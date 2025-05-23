package Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;

public class InviteDialog {

    private final JFrame parent;
    private final DefaultListModel<String> userModel;
    private final JList<String> userList;
    private final PrintWriter out;
    private final String username;

    public InviteDialog(JFrame parent, String username, PrintWriter out) {
        this.parent = parent;
        this.out = out;
        this.username = username;

        JDialog dialog = new JDialog(parent, "Oyuncu Davet Et", true);
        dialog.setSize(300, 400);
        dialog.setLocationRelativeTo(parent);

        userModel = new DefaultListModel<>();
        userList = new JList<>(userModel);
        JScrollPane scrollPane = new JScrollPane(userList);

        JButton inviteButton = new JButton("Davet Et");
        inviteButton.addActionListener((ActionEvent e) -> sendInvite());

        dialog.setLayout(new BorderLayout());
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(inviteButton, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void sendInvite() {
        String selected = userList.getSelectedValue();
        if (selected != null && !selected.equals(username)) {
            out.println("INVITE " + selected);
            JOptionPane.showMessageDialog(parent, selected + " kullanıcısına davet gönderildi.");
        }
    }

    public void updateUserList(String[] users) {
        SwingUtilities.invokeLater(() -> {
            userModel.clear();
            for (String user : users) {
                if (!user.equals(username)) {
                    userModel.addElement(user);
                }
            }
        });
    }
}
