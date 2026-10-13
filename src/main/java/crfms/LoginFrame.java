package crfms;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;

/** Login window with a role-specific desktop workspace after authentication. */
public final class LoginFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private final transient CrfmsService service;
    private final JTextField id = new JTextField("admin", 20);
    private final JPasswordField password = new JPasswordField(20);
    public LoginFrame(CrfmsService service) {
        this.service=service;setTitle("CRFMS | Course Registration & Feedback Management System");setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);setSize(520,330);setLocationRelativeTo(null);
        JPanel root=new JPanel(new BorderLayout(12,12));root.setBorder(BorderFactory.createEmptyBorder(24,32,24,32));
        JLabel title=new JLabel("CRFMS",JLabel.CENTER);title.setFont(title.getFont().deriveFont(Font.BOLD,30f));
        JLabel subtitle=new JLabel("Course Registration & Feedback Management System",JLabel.CENTER);
        JPanel header=new JPanel(new GridLayout(2,1,2,2));header.add(title);header.add(subtitle);root.add(header,BorderLayout.NORTH);
        JPanel form=new JPanel(new GridLayout(2,2,10,10));form.add(new JLabel("User ID"));form.add(id);form.add(new JLabel("Password"));form.add(password);root.add(form,BorderLayout.CENTER);
        JButton login=new JButton("Sign in");login.addActionListener(e->authenticate());password.addActionListener(e->authenticate());JPanel bottom=new JPanel(new BorderLayout());bottom.add(login,BorderLayout.EAST);bottom.add(new JLabel("First login: admin / Admin@123"),BorderLayout.WEST);root.add(bottom,BorderLayout.SOUTH);setContentPane(root);
    }
    private void authenticate(){try{CrfmsService.User user=service.login(id.getText(),new String(password.getPassword()));if(user==null){JOptionPane.showMessageDialog(this,"Invalid user ID or password, or account inactive.","Sign in failed",JOptionPane.WARNING_MESSAGE);return;}if(user.mustChange){JPasswordField next=new JPasswordField();int choice=JOptionPane.showConfirmDialog(this,new Object[]{"Set a new password (8-64 characters, at least one letter and digit):",next},"Password change required",JOptionPane.OK_CANCEL_OPTION);if(choice!=JOptionPane.OK_OPTION)return;service.changePassword(user,new String(next.getPassword()));user=service.login(user.id,new String(next.getPassword()));}new MainFrame(service,user).setVisible(true);dispose();}catch(Exception ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Sign in error",JOptionPane.ERROR_MESSAGE);}}
}
