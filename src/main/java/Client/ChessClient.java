package Client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class ChessClient extends JFrame {

    private JTextField ipField = new JTextField("51.21.170.158", 15);
    private JTextArea logArea;
    private PrintWriter out;
    private int playerId = -1;
    private int currentTurn = 0;
    private boolean connected = false;
    private ChessBoardPanel boardPanel;

    
    // Lobi arayüzü
    private JPanel loginPanel;
    private JPanel lobbyPanel;
    private JPanel gamePanel;
    private JLabel statusLabel;
    private JPanel userListPanel;

    private JButton startGameButton;

    private String username = "";

    public void setCurrentTurn(int turn) {
        this.currentTurn = turn;
    }

    public ChessClient() {
        setTitle("Chess Game");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new CardLayout());

        initLoginPanel();
        initLobbyPanel();
        initGamePanel();

        add(loginPanel, "Login");
        add(lobbyPanel, "Lobby");
        add(gamePanel, "Game");

        showLogin();
        setVisible(true);
    }

    private JTextField usernameField;

    private void initLoginPanel() {
        loginPanel = new JPanel() {
            private final Image backgroundImage = new ImageIcon(getClass().getResource("/pieces/chesslogin.jpeg")).getImage();

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            }
        };
        loginPanel.setLayout(new GridBagLayout()); // Ortalamak için

        JPanel containerPanel = new JPanel();
        containerPanel.setOpaque(false);
        containerPanel.setLayout(new BoxLayout(containerPanel, BoxLayout.Y_AXIS));
        containerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Başlık - CHESS GAME
        JLabel title = new JLabel("CHESS GAME");
        title.setFont(new Font("Serif", Font.BOLD, 60));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 40, 0));

        // Kullanıcı Adı Etiketi
        JLabel userLabel = new JLabel("Kullanıcı Adı:");
        userLabel.setFont(new Font("SansSerif", Font.BOLD, 18)); // Kalın yazı
        userLabel.setForeground(Color.WHITE);
        userLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Kullanıcı Adı Alanı
        usernameField = new JTextField();
        usernameField.setMaximumSize(new Dimension(250, 35));
        usernameField.setFont(new Font("SansSerif", Font.PLAIN, 16));
        usernameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        usernameField.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Başlat Butonu
        JButton startButton = new JButton("Oyuna Başla");
        startButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        startButton.setBackground(new Color(51, 25, 0)); // Kahverengi
        startButton.setForeground(Color.WHITE);
        startButton.setFocusPainted(false);
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        startButton.setMaximumSize(new Dimension(180, 45));
        startButton.setMargin(new Insets(10, 10, 10, 10));
        startButton.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Dikey boşluklarla ekle
        containerPanel.add(title);
        containerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        containerPanel.add(userLabel);
        containerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        containerPanel.add(usernameField);
        containerPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        containerPanel.add(startButton);

        // Ortaya ekle
        loginPanel.add(containerPanel);

        // Buton İşlevi
        startButton.addActionListener(e -> {
            username = usernameField.getText().trim(); // ✅ Global değişkene ata
            if (!username.isEmpty()) {
                log("👤 Kullanıcı adı: " + username);
                connect(); // 🔄 SUNUCUYA BAĞLAN
                showLobby(); // 🔄 LOBİYİ GÖSTER
            } else {
                JOptionPane.showMessageDialog(this, "Lütfen bir kullanıcı adı girin.");
            }
        });
    }

    public void showLogin() {
        getContentPane().removeAll();
        getContentPane().add(loginPanel);
        revalidate();
        repaint();
    }

    private boolean gameOver = false;

    public void setGameOver(boolean over) {
        this.gameOver = over;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void sendReplayRequest() {
        if (out != null) {
            out.println("REPLAY_REQUEST");
        }
    }

    public void sendReplayDecline() {
        if (out != null) {
            out.println("REPLAY_DECLINE");
        }
    }

    private DefaultListModel<String> userModel = new DefaultListModel<>();
    private JList<String> userList = new JList<>(userModel);

    private void initLobbyPanel() {
        lobbyPanel = new JPanel() {
            private final Image backgroundImage = new ImageIcon(getClass().getResource("/pieces/chesslogin.jpeg")).getImage();

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            }
        };
        lobbyPanel.setLayout(new GridBagLayout());

        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BorderLayout());
        contentPanel.setPreferredSize(new Dimension(400, 500));

        // Başlık
        JLabel listLabel = new JLabel("Aktif Oyuncular", SwingConstants.CENTER);
        listLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        listLabel.setForeground(Color.WHITE);

        // Durum etiketi
        statusLabel = new JLabel("Bağlantı durumu", SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 🧱 Üst grup: başlık + bağlantı durumu
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);
        topPanel.add(listLabel);
        topPanel.add(statusLabel);

        contentPanel.add(topPanel, BorderLayout.NORTH);

        // Kullanıcı listesi paneli
        userListPanel = new JPanel();
        userListPanel.setLayout(new BoxLayout(userListPanel, BoxLayout.Y_AXIS));
        userListPanel.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(userListPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        contentPanel.add(scrollPane, BorderLayout.CENTER);

        // 🔄 Refresh butonu
        JButton refreshButton = new JButton("↻");
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setBackground(new Color(70, 130, 180));
        refreshButton.setOpaque(true);
        refreshButton.setBorderPainted(false);
        refreshButton.setFont(new Font("SansSerif", Font.BOLD, 20));
        refreshButton.setPreferredSize(new Dimension(50, 40));
        refreshButton.addActionListener(e -> {
            if (out != null) {
                out.println("REFRESH");
            } else {
                JOptionPane.showMessageDialog(this, "Önce sunucuya bağlanın.");
            }
        });

        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.add(refreshButton);
        contentPanel.add(bottomPanel, BorderLayout.SOUTH);

        lobbyPanel.add(contentPanel);
    }

    public void showLobby() {
        getContentPane().removeAll();
        getContentPane().add(lobbyPanel);
        revalidate();
        repaint();
    }

    private void initGamePanel() {
        boardPanel = new ChessBoardPanel(this);
        logArea = new JTextArea(4, 30);
        logArea.setEditable(false);

        gamePanel = new JPanel(new BorderLayout());
        gamePanel.add(boardPanel, BorderLayout.CENTER);
        gamePanel.add(new JScrollPane(logArea), BorderLayout.SOUTH);
    }

    public void showGame() {
        JPanel gamePanel = new JPanel(new BorderLayout());
        gamePanel.add(boardPanel, BorderLayout.CENTER);
        gamePanel.add(new JScrollPane(logArea), BorderLayout.SOUTH);

        getContentPane().removeAll();
        getContentPane().add(gamePanel);
        revalidate();
        repaint();
    }

    private void connect() {
        try {
            Socket socket = new Socket(ipField.getText(), 12345);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            connected = true;
            out.println("USERNAME " + username);
            statusLabel.setText("Bağlandı. Eşleşme bekleniyor...");
            log("Sunucuya bağlandı.");

            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        System.out.println("🔔 Gelen mesaj: " + msg);

                        if (msg.startsWith("CONNECTED:PLAYER_")) {
                            playerId = Integer.parseInt(msg.split("_")[1]);
                            log("Ben PLAYER_" + playerId);
                        } else if (msg.startsWith("START")) {
                            SwingUtilities.invokeLater(() -> {
                                boardPanel = new ChessBoardPanel(this);
                                logArea = new JTextArea(4, 30);
                                logArea.setEditable(false);

                                JPanel gamePanel = new JPanel(new BorderLayout());
                                gamePanel.add(boardPanel, BorderLayout.CENTER);
                                gamePanel.add(new JScrollPane(logArea), BorderLayout.SOUTH);

                                setContentPane(gamePanel);
                                revalidate();
                                repaint();

                                log("🎯 Eşleşme tamamlandı! Oyun başlıyor...");
                            });
                        } else if (msg.startsWith("MOVE")) {
                        String[] parts = msg.split(" ");
                        int fromRow = Integer.parseInt(parts[1]);
                        int fromCol = Integer.parseInt(parts[2]);
                        int toRow = Integer.parseInt(parts[3]);
                        int toCol = Integer.parseInt(parts[4]);

                        boolean isCastling = false;
                        String promotedPiece = null;

                        if (parts.length > 5) {
                            if (parts[5].equals("CASTLE")) {
                                isCastling = true;
                            } else {
                                promotedPiece = parts[5];
                                if (parts.length > 6 && parts[6].equals("CASTLE")) {
                                    isCastling = true;
                                }
                            }
                        }

                        if (boardPanel != null) {
                            boardPanel.movePiece(fromRow, fromCol, toRow, toCol, promotedPiece, isCastling);
                        }

                    } else if (msg.startsWith("TURN")) {
                        int newTurn = Integer.parseInt(msg.split(" ")[1]);
                        setCurrentTurn(newTurn);
                        log("Sıra PLAYER_" + newTurn + "'da");

                        SwingUtilities.invokeLater(() -> {
                            boolean whiteToMove = (newTurn == 1);
                            if (!boardPanel.hasAnyValidMove(whiteToMove)) {
                                boardPanel.disableInput();
                                setGameOver(true);

                                String resultMessage = boardPanel.isKingInCheck(whiteToMove)
                                        ? "♚ ŞAH MAT!\n" + (whiteToMove ? "Siyah" : "Beyaz") + " kazandı!"
                                        : "🤝 PAT!\nOyun berabere.";

                                log(resultMessage);

                                int choice = JOptionPane.showConfirmDialog(
                                        boardPanel,
                                        resultMessage + "\nYeniden oynamak ister misiniz?",
                                        "Oyun Bitti",
                                        JOptionPane.YES_NO_OPTION
                                );

                                if (choice == JOptionPane.YES_OPTION) {
                                    sendReplayRequest();
                                } else {
                                    sendReplayDecline();
                                }
                            }
                        });

                    } else if (msg.startsWith("REPLAY_DECLINE")) {
                            log("❌ Eşleşme reddedildi. Lobiye dönülüyor...");
                            SwingUtilities.invokeLater(() -> {
                                showLobby();
                            });
                        } else if (msg.startsWith("WIN_BY_RESIGN ")) {
    String winnerName = msg.substring(15).trim();
    JOptionPane.showMessageDialog(
        boardPanel,
        "🎉 Tebrikler! Rakibiniz oyunu bıraktı. Kazandınız!",
        "Oyun Bitti",
        JOptionPane.INFORMATION_MESSAGE
    );
    showLobby();
} else if (msg.startsWith("LOSE_BY_RESIGN ")) {
    String winnerName = msg.substring(16).trim();
    JOptionPane.showMessageDialog(
        boardPanel,
        "☠ Rakip oyunu kazandı (" + winnerName + "), siz oyunu bıraktınız.",
        "Oyun Bitti",
        JOptionPane.INFORMATION_MESSAGE
    );
    showLobby();
}
 else if (msg.equals("RESIGN_REPLAY_REQUEST")) {
                            int choice = JOptionPane.showConfirmDialog(
                                    boardPanel,
                                    "Rakibiniz oyunu bıraktı. Yeni bir oyun başlatmak ister misiniz?",
                                    "Yeni Oyun Teklifi",
                                    JOptionPane.YES_NO_OPTION
                            );

                            if (choice == JOptionPane.YES_OPTION) {
                                sendReplayRequest();
                            } else {
                                sendReplayDecline();
                            }
                        } else if (msg.startsWith("INVITE_FROM ")) {
                            String from = msg.substring(12).trim();
                            int response = JOptionPane.showConfirmDialog(this,
                                    from + " size oyun daveti gönderdi. Kabul etmek ister misiniz?",
                                    "Oyun Daveti", JOptionPane.YES_NO_OPTION);

                            if (response == JOptionPane.YES_OPTION) {
                                out.println("INVITE_ACCEPT " + from);
                            } else {
                                out.println("INVITE_DECLINE " + from);
                            }
                        } else if (msg.startsWith("USERLIST ")) {
                            final String list = msg.substring(9).trim();
                            SwingUtilities.invokeLater(() -> {
                                userListPanel.removeAll();
                                String[] users = list.split(" ");
                                for (String user : users) {
                                    if (!user.equals(username)) {
                                        JPanel row = new JPanel(new BorderLayout());
                                        row.setOpaque(false);
                                        row.setMaximumSize(new Dimension(350, 40));

                                        JLabel nameLabel = new JLabel(user);
                                        nameLabel.setForeground(Color.WHITE);
                                        nameLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

                                        JButton inviteBtn = new JButton("Davet Et");
                                        inviteBtn.addActionListener(ev -> {
                                            out.println("INVITE " + user);
                                            log("📩 " + user + " kullanıcısına davet gönderildi.");
                                        });

                                        row.add(nameLabel, BorderLayout.CENTER);
                                        row.add(inviteBtn, BorderLayout.EAST);
                                        userListPanel.add(row);
                                    }
                                }
                                userListPanel.revalidate();
                                userListPanel.repaint();
                            });
                        } else if (msg.startsWith("INVITE_DECLINED_BY ")) {
                            String from = msg.substring(20).trim();
                            JOptionPane.showMessageDialog(this, from + " oyun davetini reddetti.");
                        } else {
                            log("Sunucudan gelen: " + msg);
                        }
                    }
                } catch (IOException ex) {
                    log("Bağlantı kesildi.");
                }
            }).start();

        } catch (IOException e) {
            statusLabel.setText("❌ Sunucuya bağlanılamadı: " + e.getMessage());
            log("Bağlantı hatası: " + e.getMessage());
        }
    }

    // ✅ GÜNCELLENMİŞ SEND MOVE (CASTLING DESTEKLİ)
    public void sendMove(int fromRow, int fromCol, int toRow, int toCol, String promotedPiece, boolean isCastling) {
        if (out != null) {
            StringBuilder move = new StringBuilder();
            move.append("MOVE ")
                    .append(fromRow).append(" ")
                    .append(fromCol).append(" ")
                    .append(toRow).append(" ")
                    .append(toCol);

            if (promotedPiece != null) {
                move.append(" ").append(promotedPiece);
            }

            if (isCastling) {
                move.append(" CASTLE");
            }

            out.println(move.toString());
        }
    }

    public void sendResign() {
        if (out != null) {
            out.println("RESIGN");
        }
    }

    public void sendResignWithReplay() {
        if (out != null) {
            out.println("RESIGN_AND_REPLAY");
        }
    }

    public void sendResignToLobby() {
        if (out != null) {
            out.println("RESIGN_AND_LOBBY");
        }
    }

    public void log(String text) {
        logArea.append(text + "\n");
    }

    public int getPlayerId() {
        return playerId;
    }

    public int getCurrentTurn() {
        return currentTurn;
    }

    public boolean isConnected() {
        return connected;
    }

    public static void main(String[] args) {
        new ChessClient();
    }
}
