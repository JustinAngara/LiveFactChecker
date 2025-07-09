import com.google.api.gax.rpc.ClientStream;
import com.google.api.gax.rpc.ResponseObserver;
import com.google.api.gax.rpc.StreamController;
import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;

import javax.sound.sampled.*;
import javax.swing.Timer;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class Transcription {
    private static final int SAMPLE_RATE = 16000;
    private static final int BUFFER_SIZE = 4096;
    private static final int PAUSE_TIMEOUT_MS = 800;
    private static final int FACT_CHECK_BATCH_SIZE = 3;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final FactCheckUI ui;
    private final AtomicBoolean isRecording = new AtomicBoolean(false);
    private final ExecutorService audioExecutor = Executors.newSingleThreadExecutor();
    private TargetDataLine systemAudioLine;
    private final StringBuilder currentLineBuilder = new StringBuilder();
    private final Timer pauseCommitTimer;
    private final StringBuilder factCheckBatchBuilder = new StringBuilder();
    private int finalizedLineCount = 0;

    // --- MODIFICATION: Volatile boolean to manage the active stream state ---
    private volatile boolean streamActive;

    public Transcription(FactCheckUI ui) {
        this.ui = ui;
        this.pauseCommitTimer = new Timer(PAUSE_TIMEOUT_MS, e -> finalizeLine());
        this.pauseCommitTimer.setRepeats(false);
    }

    public void start() {
        isRecording.set(true);
        audioExecutor.submit(this::beginAudioStreaming);
    }

    public void stop() {
        // Signal all loops to terminate
        isRecording.set(false);
        streamActive = false;

        if (pauseCommitTimer.isRunning()) {
            pauseCommitTimer.stop();
        }
        // Fact-check any remaining lines
        if (factCheckBatchBuilder.length() > 0) {
            System.out.println("Sending final batch on stop.");
            triggerFactCheckBatch();
        }

        // The audio line and executor shutdown are handled in the streaming method's finally block
        audioExecutor.shutdownNow();
    }

    /**
     * **UPDATED**: This method now contains a loop to automatically restart the Google Speech stream.
     */
    private void beginAudioStreaming() {
        try {
            systemAudioLine = findAndPrepareAudioLine();
            systemAudioLine.start();

            byte[] buffer = new byte[BUFFER_SIZE];

            // Main loop to keep the transcription running
            while (isRecording.get()) {
                // This inner loop manages a single stream lifecycle
                streamActive = true;
                System.out.println("Attempting to start a new transcription stream...");

                try (SpeechClient client = SpeechClient.create()) {
                    ClientStream<StreamingRecognizeRequest> clientStream = client.streamingRecognizeCallable().splitCall(createResponseObserver());

                    StreamingRecognitionConfig streamingConfig = StreamingRecognitionConfig.newBuilder()
                            .setConfig(RecognitionConfig.newBuilder()
                                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                                    .setLanguageCode("en-US")
                                    .setSampleRateHertz(SAMPLE_RATE)
                                    .setEnableAutomaticPunctuation(true)
                                    .build())
                            .setInterimResults(true)
                            .build();
                    clientStream.send(StreamingRecognizeRequest.newBuilder().setStreamingConfig(streamingConfig).build());

                    System.out.println("✅ New transcription stream started successfully.");

                    // Feed audio to the stream as long as it's active
                    while (isRecording.get() && streamActive) {
                        int bytesRead = systemAudioLine.read(buffer, 0, buffer.length);
                        if (bytesRead > 0) {
                            clientStream.send(StreamingRecognizeRequest.newBuilder()
                                    .setAudioContent(ByteString.copyFrom(buffer, 0, bytesRead))
                                    .build());
                        }
                    }
                    clientStream.closeSend();

                } catch (Exception e) {
                    System.err.println("Error during stream lifecycle: " + e.getMessage());
                }

                if (isRecording.get()) {
                    System.out.println("Stream ended. Restarting in 1 second...");
                    Thread.sleep(1000); // Brief pause before restarting
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            ui.displayFactCheckResult("Fatal Error: Could not start audio transcription.");
        } finally {
            if (systemAudioLine != null) {
                systemAudioLine.stop();
                systemAudioLine.close();
            }
        }
    }

    /**
     * **UPDATED**: This observer now controls the streamActive flag to signal when a stream ends.
     */
    private ResponseObserver<StreamingRecognizeResponse> createResponseObserver() {
        return new ResponseObserver<>() {
            @Override
            public void onStart(StreamController controller) {}

            @Override
            public void onResponse(StreamingRecognizeResponse response) {
                if (response.getResultsList().isEmpty()) return;

                StreamingRecognitionResult result = response.getResults(0);
                if (result.getAlternativesList().isEmpty()) return;

                String transcript = result.getAlternatives(0).getTranscript().trim();

                if (result.getIsFinal()) {
                    pauseCommitTimer.stop();
                    currentLineBuilder.append(transcript).append(" ");
                    long sentenceCount = currentLineBuilder.toString().chars().filter(c -> c == '.' || c == '?' || c == '!').count();

                    if (sentenceCount >= 2) {
                        finalizeLine();
                    } else {
                        ui.updateLiveCaption(currentLineBuilder.toString());
                        pauseCommitTimer.restart();
                    }
                } else {
                    String previewText = currentLineBuilder.toString() + transcript;
                    ui.updateLiveCaption(previewText);
                }
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Stream error: " + t.getMessage());
                streamActive = false; // Signal that the stream has died
            }

            @Override
            public void onComplete() {
                System.out.println("Stream completed gracefully.");
                streamActive = false; // Signal that the stream has died
            }
        };
    }

    private void finalizeLine() {
        pauseCommitTimer.stop();
        String lineToCommit = currentLineBuilder.toString().trim();

        if (!lineToCommit.isEmpty()) {
            String timestamp = LocalTime.now().format(TIME_FORMATTER);
            ui.commitFinalTranscript(lineToCommit, timestamp);
            factCheckBatchBuilder.append(lineToCommit).append(" ");
            finalizedLineCount++;
            if (finalizedLineCount >= FACT_CHECK_BATCH_SIZE) {
                triggerFactCheckBatch();
            }
        }
        currentLineBuilder.setLength(0);
    }

    //<editor-fold desc="Unchanged Helper Methods">
    private AudioFormat getDesiredFormat() {
        return new AudioFormat(16000.0F, 16, 1, true, false);
    }

    private void triggerFactCheckBatch() {
        String batchToFactCheck = factCheckBatchBuilder.toString().trim();
        if (!batchToFactCheck.isEmpty()) {
            System.out.println("Sending batch of " + finalizedLineCount + " lines for fact-check.");
            GeminiAPI.callFactCheckAPIAsync(batchToFactCheck)
                    .thenAccept(ui::displayFactCheckResult)
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        ui.displayFactCheckResult("Error during fact-check: " + ex.getMessage());
                        return null;
                    });
        }
        factCheckBatchBuilder.setLength(0);
        finalizedLineCount = 0;
    }

    public TargetDataLine findAndPrepareAudioLine() throws LineUnavailableException {
        AudioFormat format = getDesiredFormat();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            if (mixerInfo.getName().toLowerCase().contains("stereo mix")) {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (mixer.isLineSupported(info)) {
                    try {
                        TargetDataLine targetLine = (TargetDataLine) mixer.getLine(info);
                        targetLine.open(format);
                        System.out.println("✅ Using Stereo Mix for system audio capture.");
                        return targetLine;
                    } catch (LineUnavailableException e) {
                        System.err.println("Stereo Mix was found but is busy or unavailable: " + e.getMessage());
                    }
                }
            }
        }
        throw new LineUnavailableException("❌ Stereo Mix not found or is unavailable. Please enable it in your system's sound settings.");
    }
    //</editor-fold>
}