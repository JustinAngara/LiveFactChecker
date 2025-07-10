import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;

public class FactCheckPopupFrame extends JFrame {
    private JEditorPane messagePane;
    private static final String PROMPT = "You are a fast, precise fact-checking AI specializing in analyzing live speech transcripts for claims that can be objectively verified.\n"
            + "\n"
            + "Your task:\n"
            + "- Analyze the transcript provided and identify **only notable claims that stand out and can be meaningfully verified** (e.g., statistics, policy claims, historical statements, legal facts).\n"
            + "- Ignore or skip over statements that are too vague, general, or subjective to verify.\n"
            + "- Do not output claims if they are unremarkable or unimportant to verify in real-time.\n"
            + "- If you encounter a statement that may be verifiable but lacks enough context, note it **briefly** with \"Unverified\" but only if it is notable.\n"
            + "\n"
            + "For each notable claim:\n"
            + "- **Claim**: The exact quote from the transcript.\n"
            + "- **Rating**: One of True / Mostly True / Misleading / False / Unverified, followed by a confidence estimate in parentheses (e.g., (92%)).\n"
            + "- **Reason**: A short, clear explanation (1-2 sentences) referencing relevant facts or context.\n"
            + "- **Sources**: URLs or clear references that support your analysis.\n"
            + "- **Final Grade**: Provide an overall grade (A, B, C, D, or F) summarizing the transcript’s verifiability.\n"
            + "\n"
            + "**Output Format:**\n"
            + "**Claim**: [quote]\n"
            + "**Rating**: [Rating (Confidence%)]\n"
            + "**Reason**: [Explanation]\n"
            + "**Sources**: [Reference(s)]\n"
            + "**Final Grade**: [Letter]\n"
            + "\n"
            + "Provide each claim in this format, separated by clear line breaks.\n"
            + "\n"
            + "If no notable, verifiable claims are found, respond:\n"
            + "\"No notable verifiable claims found.\"\n"
            + "\n"
            + "Guidelines:\n"
            + "- Be objective, fast, and factual.\n"
            + "- Only include claims that stand out for real-time fact-checking.\n"
            + "- If a claim mixes truth and falsehood, rate as \"Misleading\".\n"
            + "- Avoid verbose analysis for \"Unverified\" claims; simply note them if they are notable enough to follow up later.\n"
            + "\n"
            + "Begin your analysis immediately upon receiving the transcript.\n";

    public FactCheckPopupFrame(List<String> transcriptList) {
        setTitle("Fact Check Result");
        setSize(700, 600);
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Dark theme
        Color backgroundColor = new Color(30, 30, 30);
        getContentPane().setBackground(backgroundColor);

        // Prepare message pane with HTML styling
        messagePane = new JEditorPane();
        messagePane.setContentType("text/html");
        messagePane.setEditable(false);
        messagePane.setBackground(backgroundColor);
        messagePane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Extract and format transcript
        String factCheckOutput = callGeminiAndFormat(transcriptList);
        messagePane.setText(factCheckOutput);
        messagePane.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(messagePane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(backgroundColor);

        add(scrollPane, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.setFocusPainted(false);
        closeButton.setBackground(new Color(50, 50, 50));
        closeButton.setForeground(new Color(220, 220, 220));
        closeButton.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        closeButton.addActionListener(e -> dispose());

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(backgroundColor);
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private String callGeminiAndFormat(List<String> transcriptList) {
        String previous = "";
        LinkedHashSet<String> lines = new LinkedHashSet<>();
        for (String raw : transcriptList) {
            String c = raw.trim();
            if (c.startsWith("[LIVE CAPTION]")) c = c.substring(14).trim();
            else if (c.matches("^\\[\\d{2}:\\d{2}:\\d{2}]\\s.*"))
                c = c.replaceFirst("^\\[\\d{2}:\\d{2}:\\d{2}]\\s*", "");
            if (c.isEmpty() || c.equalsIgnoreCase(previous)) continue;
            if (!previous.isEmpty() && c.startsWith(previous)) {
                lines.remove(previous);
            }
            lines.add(c);
            previous = c;
        }
        if (lines.isEmpty()) {
            return toHtml("No finalized captions available yet.");
        }

        StringBuilder rawSb = new StringBuilder();
        for (String ln : lines) rawSb.append(ln).append("\n\n");

        String fullPrompt = PROMPT + "\nTranscript:\n" + rawSb.toString().trim();
        String result     = GeminiAPI.callFactCheckAPIAsync(fullPrompt).join();

        return toHtml(result);
    }

    private String toHtml(String text) {
        // Escape HTML
        String escaped = text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");

        // Convert double newlines first
        escaped = escaped.replaceAll("\\r?\\n\\r?\\n", "<br/><br/>");
        // Then convert remaining single newlines
        escaped = escaped.replaceAll("\\r?\\n", "<br/>");

        // Highlight keywords
        escaped = escaped.replaceAll("\\*\\*(Claim|Rating|Reason|Sources|Final Grade)\\*\\*:",
                "<span style='color:#FFA500;font-weight:bold;'>$1:</span>");

        return "<html><body style='background-color:#1e1e1e;color:#dcdcdc;"
                + "font-family:Sans-Serif;font-size:14px;'>" + escaped + "</body></html>";
    }

    public static void showFactCheckPopup(List<String> transcriptList) {
        SwingUtilities.invokeLater(() -> {
            FactCheckPopupFrame popup = new FactCheckPopupFrame(transcriptList);
            popup.setVisible(true);
        });
    }
}
