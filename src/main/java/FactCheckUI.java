import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.geom.RoundRectangle2D;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

public class FactCheckUI extends JFrame {
    private final JTextArea transcriptionArea;
    private final JTextArea liveCaption;
    private final JTextPane factCheckPane;
    private String currentCaptionText = "";
    private boolean isFirstFactCheck = true;
    private boolean isSummarizing = false;
    private final List<String> transcriptList = new ArrayList<>();

    public FactCheckUI() {
        setTitle("🔍 Live Fact Checker");
        // scale width to 55% of 900px
        int scaledWidth = (int)(900 * 0.55);
        setSize(scaledWidth, 550);
        setUndecorated(true);
        setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 25, 25));
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);
        setResizable(true);  // allow vertical resizing

        // Main container with gradient background
        JPanel mainContainer = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(30,30,30), 0, getHeight(), new Color(20,20,20));
                g2.setPaint(gp);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),25,25);
                g2.dispose();
            }
        };

        // Header with drag & close
        JPanel header = FactCheckUIUtils.createHeaderPanel(this);

        // Content stacked vertically: transcript on top, fact-check below
        JPanel content = new JPanel(new GridLayout(2, 1, 0, 12));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Transcript panel (live caption + transcript)
        transcriptionArea = FactCheckUIUtils.createMainTranscriptArea();
        liveCaption = FactCheckUIUtils.createLiveCaptionArea();
        JScrollPane liveScroll = FactCheckUIUtils.createScrollPane(liveCaption);
        JScrollPane transScroll = FactCheckUIUtils.createScrollPane(transcriptionArea);
        JPanel transcriptPanel = new JPanel(new BorderLayout());
        transcriptPanel.setOpaque(false);
        transcriptPanel.add(liveScroll, BorderLayout.NORTH);
        transcriptPanel.add(transScroll, BorderLayout.CENTER);

        // Fact-check panel
        factCheckPane = FactCheckUIUtils.createFactCheckPane();
        JScrollPane factScroll = FactCheckUIUtils.createScrollPane(factCheckPane);

        content.add(transcriptPanel);
        content.add(factScroll);

        // Control buttons
        JButton startBtn = FactCheckUIUtils.createStyledButton("Start Summarize", new Color(46, 204, 113));
        JButton stopBtn  = FactCheckUIUtils.createStyledButton("Stop Summarize", new Color(231, 76, 60));
        stopBtn.setEnabled(false);
        startBtn.addActionListener(e -> beginSummarization(startBtn, stopBtn));
        stopBtn.addActionListener(e -> endSummarization(startBtn, stopBtn));
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.add(startBtn);
        buttonPanel.add(stopBtn);

        mainContainer.add(header, BorderLayout.NORTH);
        mainContainer.add(content, BorderLayout.CENTER);
        mainContainer.add(buttonPanel, BorderLayout.SOUTH);
        add(mainContainer);
        // update rounded shape on resize
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 25, 25));
            }
        });
    }

    private void beginSummarization(JButton start, JButton stop) {
        transcriptionArea.setText("");
        // keep factCheckPane content intact for next results
        transcriptList.clear();
        isSummarizing = true;
        isFirstFactCheck = true;
        start.setEnabled(false);
        stop.setEnabled(true);
    }

    private void endSummarization(JButton start, JButton stop) {
        isSummarizing = false;
        start.setEnabled(true);
        stop.setEnabled(false);
        FactCheckPopupFrame.showFactCheckPopup(transcriptList);
    }

    public void updateLiveCaption(String text) {
        SwingUtilities.invokeLater(() -> {
            if (!currentCaptionText.equals(text)) {
                currentCaptionText = text;
                liveCaption.setText(text);
                liveCaption.setCaretPosition(liveCaption.getDocument().getLength());
                if (isSummarizing) transcriptList.add("[LIVE] " + text.trim());
            }
        });
    }

    public void commitFinalTranscript(String text, String timestamp) {
        SwingUtilities.invokeLater(() -> {
            String entry = String.format("[%s] %s", timestamp, text.trim());
            transcriptionArea.append(entry + "\n\n");
            transcriptionArea.setCaretPosition(transcriptionArea.getDocument().getLength());
            liveCaption.setText("");
            currentCaptionText = "";
            if (isSummarizing) transcriptList.add(entry);
        });
    }

    public void displayFactCheckResult(String result) {
        SwingUtilities.invokeLater(() -> {
            if (result == null || result.trim().isEmpty() || result.contains("No verifiable")) return;
            String html = FactCheckUIUtils.formatFactCheckToHtml(result);
            try {
                HTMLDocument doc = (HTMLDocument) factCheckPane.getDocument();
                HTMLEditorKit kit = (HTMLEditorKit) factCheckPane.getEditorKit();
                if (isFirstFactCheck) {
                    factCheckPane.setText("<html><body style='color:white;font-family:Segoe UI; font-size:14pt;'>" + html + "</body></html>");
                    isFirstFactCheck = false;
                } else {
                    kit.insertHTML(doc, doc.getLength(), "<br><hr><br>" + html, 0, 0, null);
                }
                factCheckPane.setCaretPosition(doc.getLength());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FactCheckUI().setVisible(true));
    }
}
