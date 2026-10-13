package crfms;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.nio.file.Path;

/** Desktop application entry point. */
public final class Main {
    private Main() { }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                Database db = new Database(Path.of("data"));
                CrfmsService service = new CrfmsService(db);
                for (String arg : args) if (arg.equals("--demo")) service.seedDemo();
                new LoginFrame(service).setVisible(true);
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(null,
                    "CRFMS could not start.\n" + ex.getMessage() + "\n\nCheck that the SQLite JDBC driver downloaded correctly.",
                    "Startup error", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
