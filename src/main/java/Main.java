import javax.swing.*;

public class Main{
    public static void main(String[] args) {
        GeminiAPI.loadEnvFromFile("C:\\Users\\justi\\IdeaProjects\\DesktopAI\\.env");
        RealTimeTranscription transcriber = new RealTimeTranscription();
        Runtime.getRuntime().addShutdownHook(new Thread(transcriber::stop));
        transcriber.start();
    }

}