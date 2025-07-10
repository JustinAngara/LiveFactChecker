import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

public class FactCheckUI extends JFrame {
    private final JTextArea transcriptionArea;
    private final JTextArea liveCaption;
    private final JTextPane factCheckPane;
    private String currentCaptionText = "";
    private boolean isFirstFactCheck = true;
    private int mouseX, mouseY;

    private boolean isSummarizing = false;
    private final List<String> transcriptList = new ArrayList<>();

    public FactCheckUI() {
        setTitle("🔍 Live Fact Checker");
        setSize(900, 550);
        setUndecorated(true);
        setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 25, 25));
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);

        JPanel mainContainer = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gradient = new GradientPaint(0, 0, new Color(30, 30, 30), 0, getHeight(), new Color(20, 20, 20));
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);
                g2d.setColor(new Color(100, 149, 237, 40));
                g2d.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 23, 23);
                g2d.setColor(new Color(100, 149, 237));
                g2d.setStroke(new BasicStroke(2f));
                g2d.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 25, 25);
                g2d.dispose();
            }
        };

        JPanel header = createHeaderPanel();
        JPanel contentPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel transcriptionPanel = new JPanel(new BorderLayout());
        transcriptionPanel.setOpaque(false);
        transcriptionArea = createMainTranscriptArea();
        liveCaption = createLiveCaptionArea();

        JScrollPane liveCaptionScroll = createScrollPane(liveCaption);
        JScrollPane transcriptionScroll = createScrollPane(transcriptionArea);

        transcriptionPanel.add(liveCaptionScroll, BorderLayout.NORTH);
        transcriptionPanel.add(transcriptionScroll, BorderLayout.CENTER);

        factCheckPane = createFactCheckPane();
        JScrollPane factCheckScroll = createScrollPane(factCheckPane);

        contentPanel.add(transcriptionPanel);
        contentPanel.add(factCheckScroll);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);

        JButton startSummarizeButton = createStyledButton("Start Summarize", new Color(46, 204, 113)); // green
        JButton stopSummarizeButton = createStyledButton("Stop Summarize", new Color(231, 76, 60));   // red

        // Initially, Stop should be disabled since you haven't started yet
        stopSummarizeButton.setEnabled(false);

        startSummarizeButton.addActionListener(e -> {
            transcriptList.clear();
            isSummarizing = true;
            startSummarizeButton.setEnabled(false);  // disable start
            stopSummarizeButton.setEnabled(true);    // enable stop
            JOptionPane.showMessageDialog(this, "Summarization started.");
        });

        stopSummarizeButton.addActionListener(e -> {
            isSummarizing = false;
            stopSummarizeButton.setEnabled(false);   // disable stop
            startSummarizeButton.setEnabled(true);   // enable start
            JOptionPane.showMessageDialog(this, "Summarization stopped. Lines collected: " + transcriptList.size());
            transcriptList.forEach(System.out::println);
        });


        buttonPanel.add(startSummarizeButton);
        buttonPanel.add(stopSummarizeButton);

        mainContainer.add(header, BorderLayout.NORTH);
        mainContainer.add(contentPanel, BorderLayout.CENTER);
        mainContainer.add(buttonPanel, BorderLayout.SOUTH);
        add(mainContainer);
    }

    // Modern styled button creator
    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.brighter());
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });

        return button;
    }

    public void updateLiveCaption(String text) {
        SwingUtilities.invokeLater(() -> {
            if (!currentCaptionText.equals(text)) {
                currentCaptionText = text;
                liveCaption.setText(text);
                liveCaption.setCaretPosition(liveCaption.getDocument().getLength());
            }
        });
    }

    public void commitFinalTranscript(String text, String timestamp) {
        SwingUtilities.invokeLater(() -> {
            String entry = String.format("[%s] %s\n\n", timestamp, text);
            transcriptionArea.append(entry);
            transcriptionArea.setCaretPosition(transcriptionArea.getDocument().getLength());
            liveCaption.setText("");
            currentCaptionText = "";
            if (isSummarizing) {
                transcriptList.add(entry.trim());
            }
        });
    }

    public void displayFactCheckResult(String result) {
        SwingUtilities.invokeLater(() -> {
            if (result == null || result.trim().isEmpty() || result.contains("No verifiable claims found.")) return;
            String styledHtmlResult = formatFactCheckToHtml(result);
            if (isFirstFactCheck) {
                String htmlDocument = "<html><body style='color:white; font-family:Segoe UI; font-size: 14pt;'>" + styledHtmlResult + "</body></html>";
                factCheckPane.setText(htmlDocument);
                isFirstFactCheck = false;
            } else {
                try {
                    HTMLDocument doc = (HTMLDocument) factCheckPane.getDocument();
                    HTMLEditorKit editorKit = (HTMLEditorKit) factCheckPane.getEditorKit();
                    String separator = "<br><hr style='border-color: #444;'><br>";
                    editorKit.insertHTML(doc, doc.getLength(), separator + styledHtmlResult, 0, 0, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            factCheckPane.setCaretPosition(factCheckPane.getDocument().getLength());
        });
    }

    private String formatFactCheckToHtml(String plainText) {
        StringBuilder htmlBuilder = new StringBuilder();
        String[] lines = plainText.split("\n");
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            String formattedLine = "<p style='margin-bottom: 8px;'>";
            if (line.startsWith("**Claim**:")) {
                formattedLine += line.replace("**Claim**:", "<strong style='color: #85C1E9;'>Claim:</strong>");
            } else if (line.startsWith("**Rating**:")) {
                String ratingValue = line.substring(line.indexOf(":") + 1).trim();
                String ratingColor = getRatingColor(ratingValue);
                formattedLine += "<strong style='color: #85C1E9;'>Rating:</strong> <strong style='color:" + ratingColor + ";'>" + ratingValue + "</strong>";
            } else if (line.startsWith("**Reason**:")) {
                formattedLine += line.replace("**Reason**:", "<strong style='color: #85C1E9;'>Reason:</strong>");
            } else if (line.startsWith("**Sources**:")) {
                formattedLine += line.replace("**Sources**:", "<strong style='color: #85C1E9;'>Sources:</strong>");
            } else {
                formattedLine += line;
            }
            formattedLine += "</p>";
            htmlBuilder.append(formattedLine);
        }
        return htmlBuilder.toString();
    }

    private String getRatingColor(String rating) {
        switch (rating.toLowerCase()) {
            case "true": case "mostly true": return "#2ECC71";
            case "false": return "#E74C3C";
            case "misleading": return "#F39C12";
            default: return "#BDC3C7";
        }
    }

    private JTextArea createLiveCaptionArea() {
        JTextArea area = new JTextArea("Listening...");
        area.setFont(new Font("Segoe UI", Font.ITALIC, 20));
        area.setForeground(new Color(173, 216, 230));
        area.setBackground(new Color(35, 35, 35));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        Border padding = BorderFactory.createEmptyBorder(10, 14, 10, 14);
        Border line = BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(60, 60, 60));
        area.setBorder(BorderFactory.createCompoundBorder(line, padding));
        return area;
    }

    private JTextArea createMainTranscriptArea() {
        JTextArea area = new JTextArea();
        area.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        area.setForeground(Color.WHITE);
        area.setBackground(new Color(40, 40, 40, 230));
        area.setCaretColor(Color.WHITE);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        return area;
    }

    private JTextPane createFactCheckPane() {
        JTextPane pane = new JTextPane();
        pane.setContentType("text/html");
        pane.setText("<html><body style='color:white;font-family:Segoe UI; font-size: 14pt;'>Fact checks will appear here...</body></html>");
        pane.setEditable(false);
        pane.setOpaque(false);
        pane.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        return pane;
    }

    private JScrollPane createScrollPane(Component view) {
        JScrollPane scrollPane = new JScrollPane(view);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60), 1, true));
        customizeScrollBar(scrollPane);
        return scrollPane;
    }

    private void customizeScrollBar(JScrollPane scrollPane) {
        JScrollBar bar = scrollPane.getVerticalScrollBar();
        bar.setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                this.thumbColor = new Color(100, 149, 237, 180);
                this.trackColor = new Color(30, 30, 30, 180);
            }
            @Override protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
            @Override protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
            private JButton createZeroButton() {
                JButton btn = new JButton();
                btn.setPreferredSize(new Dimension(0, 0));
                return btn;
            }
        });
        bar.setPreferredSize(new Dimension(8, Integer.MAX_VALUE));
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(getWidth(), 40));
        header.add(createCloseButton(), BorderLayout.EAST);
        JLabel titleLabel = new JLabel("  Live Fact Checker");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);
        header.add(titleLabel, BorderLayout.WEST);
        header.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { mouseX = e.getX(); mouseY = e.getY(); }
        });
        header.addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseDragged(MouseEvent e) { setLocation(e.getXOnScreen() - mouseX, e.getYOnScreen() - mouseY); }
        });
        return header;
    }

    private JButton createCloseButton() {
        JButton closeBtn = new JButton("×");
        closeBtn.setFont(new Font("Segoe UI Symbol", Font.BOLD, 20));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setOpaque(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setBorder(BorderFactory.createEmptyBorder(0, 15, 5, 15));
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { closeBtn.setForeground(new Color(231, 76, 60)); }
            @Override public void mouseExited(MouseEvent e) { closeBtn.setForeground(Color.WHITE); }
        });
        closeBtn.addActionListener(e -> System.exit(0));
        return closeBtn;
    }
}
