package Client;

import javax.swing.*;

public class LobbyFrame extends JFrame {

    public LobbyFrame() {
        setTitle("Satranç Lobi");
        setSize(300, 150);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JButton joinBtn = new JButton("Oyuna Gir");
        joinBtn.addActionListener(e -> {
            new ChessClient();  // Oyunu başlat
            dispose();          // Lobi ekranını kapat
        });

        add(joinBtn);
        setVisible(true);
    }

    public static void main(String[] args) {
        new LobbyFrame();  // Başlangıç olarak bu çalıştırılacak
    }
}
