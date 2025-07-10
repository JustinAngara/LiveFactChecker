import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class FactCheckUIUtils {

    public static final String PROMPT = """
You are a fast, precise fact-checking AI specializing in analyzing live speech transcripts for claims that can be objectively verified.

Your task:
- Analyze the transcript provided and identify **only notable claims that stand out and can be meaningfully verified** (e.g., statistics, policy claims, historical statements, legal facts).
- Ignore or skip over statements that are too vague, general, or subjective to verify.
- Do not output claims if they are unremarkable or unimportant to verify in real-time.
- If you encounter a statement that may be verifiable but lacks enough context, note it **briefly** with "Unverified" but only if it is notable.

For each notable claim:
- **Claim**: The exact quote from the transcript.
- **Rating**: One of True / Mostly True / Misleading / False / Unverified, followed by a confidence estimate in parentheses (e.g., (92%)).
- **Reason**: A short, clear explanation (1-2 sentences) referencing relevant facts or context.
- **Sources**: URLs or clear references that support your analysis.

**Output Format:**
**Claim**: [quote]
**Rating**: [Rating (Confidence%)]
**Reason**: [Explanation]
**Sources**: [Reference(s)]

Provide each claim in this format, separated by clear line breaks.

If no notable, verifiable claims are found, respond:
"No notable verifiable claims found."

Guidelines:
- Be objective, fast, and factual.
- Only include claims that stand out for real-time fact-checking.
- If a claim mixes truth and falsehood, rate as "Misleading".
- Avoid verbose analysis for "Unverified" claims; simply note them if they are notable enough to follow up later.

Begin your analysis immediately upon receiving the transcript.
""";

    private static int mouseX, mouseY;

    public static JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);

        button.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { button.setBackground(bgColor.brighter()); }
            @Override public void mouseExited(MouseEvent e) { button.setBackground(bgColor); }
        });
        return button;
    }

    public static JTextArea createLiveCaptionArea() {
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

    public static JTextArea createMainTranscriptArea() {
        JTextArea area = new JTextArea();
        area.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        area.setForeground(Color.WHITE);
        area.setBackground(new Color(40, 40, 40));
        area.setCaretColor(Color.WHITE);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        return area;
    }

    public static JTextPane createFactCheckPane() {
        JTextPane pane = new JTextPane();
        pane.setContentType("text/html");
        pane.setText("<html><body style='color:white;font-family:Segoe UI; font-size: 14pt;'>Fact checks will appear here...</body></html>");
        pane.setEditable(false);
        pane.setOpaque(false);
        pane.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        return pane;
    }

    public static JScrollPane createScrollPane(Component view) {
        JScrollPane scrollPane = new JScrollPane(view);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60), 1, true));
        customizeScrollBar(scrollPane);
        return scrollPane;
    }

    public static void customizeScrollBar(JScrollPane scrollPane) {
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

    public static JPanel createHeaderPanel(JFrame frame) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(frame.getWidth(), 40));
        header.add(createCloseButton(), BorderLayout.EAST);

        JLabel titleLabel = new JLabel("  Live Fact Checker");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);
        header.add(titleLabel, BorderLayout.WEST);

        header.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }
        });

        header.addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                frame.setLocation(e.getXOnScreen() - mouseX, e.getYOnScreen() - mouseY);
            }
        });
        return header;
    }

    public static JButton createCloseButton() {
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

    public static String formatFactCheckToHtml(String plainText) {
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

    public static String getRatingColor(String rating) {
        switch (rating.toLowerCase()) {
            case "true": case "mostly true": return "#2ECC71";
            case "false": return "#E74C3C";
            case "misleading": return "#F39C12";
            default: return "#BDC3C7";
        }
    }

    /**
     * Displays the Gemini prompt itself inside your fact-check pane for testing or tuning.
     */
    public static void displayPromptInPane(JTextPane factCheckPane) {
        String promptHtml = "<html><body style='color:white;font-family:Segoe UI; font-size: 14pt;'>" +
                PROMPT.replace("\n", "<br>") + "</body></html>";
        factCheckPane.setText(promptHtml);
    }
}
