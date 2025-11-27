import Domain.Library;
import Mapper.CommandMapper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import com.formdev.flatlaf.FlatLightLaf;
// oder für Dark:
// import com.formdev.flatlaf.FlatDarkLaf;

public class LibraryManagerUI extends JFrame {

    private final JTextField fileField;
    private final JTextArea outputArea;
    private final JLabel statusLabel;

    public LibraryManagerUI() {
        super("Library Manager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null); // center on screen

        // ===== Root content panel with padding =====
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(new EmptyBorder(12, 12, 12, 12));
        setContentPane(content);

        // ===== Header =====
        JLabel titleLabel = new JLabel("Library Manager");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20f));

        JLabel subtitleLabel = new JLabel("Select a command script file (e.g. library_manager.txt) and run it.");
        subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(Font.PLAIN, 12f));
        subtitleLabel.setForeground(Color.DARK_GRAY);

        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBorder(new EmptyBorder(0, 0, 8, 0));
        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(subtitleLabel);

        // ===== File chooser row =====
        fileField = new JTextField();
        fileField.setPreferredSize(new Dimension(400, 28));

        JButton browseButton = new JButton("Browse…");
        JButton runButton = new JButton("Run");

        JPanel filePanel = new JPanel(new BorderLayout(8, 0));
        filePanel.add(fileField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.add(browseButton);
        buttonPanel.add(runButton);

        filePanel.add(buttonPanel, BorderLayout.EAST);

        JPanel northPanel = new JPanel(new BorderLayout(0, 8));
        northPanel.add(headerPanel, BorderLayout.NORTH);
        northPanel.add(filePanel, BorderLayout.SOUTH);

        content.add(northPanel, BorderLayout.NORTH);

        // ===== Output area =====
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        outputArea.setLineWrap(false);

        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(new TitledBorder("Output"));

        content.add(scrollPane, BorderLayout.CENTER);

        // ===== Status bar =====
        statusLabel = new JLabel("Ready.");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 11f));
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(new EmptyBorder(4, 0, 0, 0));
        statusPanel.add(statusLabel, BorderLayout.WEST);

        content.add(statusPanel, BorderLayout.SOUTH);

        // ===== Actions =====
        browseButton.addActionListener(e -> chooseFile());
        runButton.addActionListener(e -> runScript());
    }

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            fileField.setText(chooser.getSelectedFile().getAbsolutePath());
            statusLabel.setText("File selected: " + chooser.getSelectedFile().getName());
        }
    }

    private void runScript() {
        String path = fileField.getText().trim();
        if (path.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please select a file first.",
                    "Info",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        File file = new File(path);
        if (!file.isFile()) {
            JOptionPane.showMessageDialog(this,
                    "The selected file does not exist.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        statusLabel.setText("Running script...");
        outputArea.setText("");

        try {
            String output = executeScriptFile(file);
            outputArea.setText(output);
            outputArea.setCaretPosition(0); // scroll to top
            statusLabel.setText("Finished. Lines processed: " + output.lines().count());
        } catch (IOException ex) {
            statusLabel.setText("Error while running script.");
            JOptionPane.showMessageDialog(this,
                    "Error while reading the file:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private String executeScriptFile(File file) throws IOException {
        Library library = new Library();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintStream ps = new PrintStream(baos, true, "UTF-8");
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            CommandMapper mapper = new CommandMapper(library, ps);
            String line;
            while ((line = reader.readLine()) != null) {
                mapper.processLine(line);
            }
        }

        return baos.toString("UTF-8");
    }

    public static void main(String[] args) {
        try {
            FlatLightLaf.setup(); // or FlatDarkLaf.setup();
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf: " + ex.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            LibraryManagerUI ui = new LibraryManagerUI();
            ui.setVisible(true);
        });
    }

}