import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class FactCheckUI extends JFrame {
    private final JTextArea transcriptionArea;
    private final JTextArea liveCaption;
    private final JTextPane factCheckPane;
    private String currentCaptionText = "";
    private boolean isFirstFactCheck = true;
    private int mouseX, mouseY;


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

        // Wrap the liveCaption JTextArea in its own JScrollPane to make it scrollable.
        JScrollPane liveCaptionScroll = createScrollPane(liveCaption);
        FontMetrics metrics = liveCaption.getFontMetrics(liveCaption.getFont());
        int lineHeight = metrics.getHeight();
        int borderHeight = liveCaption.getInsets().top + liveCaption.getInsets().bottom;
        int desiredHeight = (lineHeight * 3) + borderHeight;
        liveCaptionScroll.setPreferredSize(new Dimension(0, desiredHeight));

        JScrollPane transcriptionScroll = createScrollPane(transcriptionArea);
        transcriptionPanel.add(liveCaptionScroll, BorderLayout.NORTH);
        transcriptionPanel.add(transcriptionScroll, BorderLayout.CENTER);

        factCheckPane = createFactCheckPane();
        JScrollPane factCheckScroll = createScrollPane(factCheckPane);

        contentPanel.add(transcriptionPanel);
        contentPanel.add(factCheckScroll);

        mainContainer.add(header, BorderLayout.NORTH);
        mainContainer.add(contentPanel, BorderLayout.CENTER);
        add(mainContainer);
    }

    /**
     * **MODIFICATION**: This method now accepts a timestamp and prepends it to the text.
     */
    public void commitFinalTranscript(String text, String timestamp) {
        SwingUtilities.invokeLater(() -> {
            // Prepend the timestamp to the committed text.
            transcriptionArea.append(String.format("[%s] %s\n\n", timestamp, text));
            transcriptionArea.setCaretPosition(transcriptionArea.getDocument().getLength());
            liveCaption.setText("");
            currentCaptionText = "";
        });
    }

    //<editor-fold desc="Unchanged and Other Methods">
    private JTextArea createLiveCaptionArea() {
        JTextArea area = new JTextArea("Listening...");
        Font captionFont = new Font("Segoe UI", Font.ITALIC, 20);
        area.setFont(captionFont);
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

    public void updateLiveCaption(String text) {
        SwingUtilities.invokeLater(() -> {
            if (!currentCaptionText.equals(text)) {
                currentCaptionText = text;
                liveCaption.setText(text);
                liveCaption.setCaretPosition(liveCaption.getDocument().getLength());
            }
        });
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
            @Override
            public void mousePressed(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }
        });

        header.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int newX = e.getXOnScreen();
                int newY = e.getYOnScreen();
                setLocation(newX - mouseX, newY - mouseY);
            }
        });

        return header;
    }

    public void displayFactCheckResult(String result) {
        SwingUtilities.invokeLater(() -> {
            if (result == null || result.trim().isEmpty() || result.contains("No verifiable claims found.")) {
                return;
            }

            String styledHtmlResult = formatFactCheckToHtml(result);

            if (isFirstFactCheck) {
                String htmlDocument = "<html><body style='color:white; font-family:Segoe UI; font-size: 14pt;'>"
                        + styledHtmlResult
                        + "</body></html>";
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
            case "true":
            case "mostly true":
                return "#2ECC71"; // Green
            case "false":
                return "#E74C3C"; // Red
            case "misleading":
                return "#F39C12"; // Orange
            case "unverified":
            default:
                return "#BDC3C7"; // Gray
        }
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
    //</editor-fold>
}