public class RealTimeTranscription {

    private final Transcription transcription;
    private final FactCheckUI factCheckUI;

    public RealTimeTranscription() {
        this.factCheckUI = new FactCheckUI();
        this.transcription = new Transcription(factCheckUI); // prints raw transcription
    }

    public void start() {
        factCheckUI.setVisible(true);
        transcription.start();
    }

    public void stop() {
        transcription.stop();
        factCheckUI.dispose();
    }
}
