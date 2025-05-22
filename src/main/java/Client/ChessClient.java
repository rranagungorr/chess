package Client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class ChessClient extends JFrame {

    private JTextField ipField;
    private JTextArea logArea;
    private PrintWriter out;
    private int playerId = -1;
    private int currentTurn = 0;
    private boolean connected = false;
    private ChessBoardPanel boardPanel;

    // Lobi arayüzü
    private JPanel lobbyPanel;
    private JPanel gamePanel;
    private JLabel statusLabel;
    private JButton startGameButton;

    public ChessClient() {
        setTitle("Chess Client");
        setSize(600, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new CardLayout());

        initLobbyPanel();
        initGamePanel();

        add(lobbyPanel, "Lobby");
        add(boardPanel, "Game");

        showLobby();
        setVisible(true);
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

    private void initLobbyPanel() {
        lobbyPanel = new JPanel(new BorderLayout());

        JPanel top = new JPanel();
        ipField = new JTextField("127.0.0.1", 15);
        startGameButton = new JButton("Oyuna Gir");

        top.add(new JLabel("Sunucu IP:"));
        top.add(ipField);
        top.add(startGameButton);

        statusLabel = new JLabel("Oyuncu bekleniyor...");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        lobbyPanel.add(top, BorderLayout.NORTH);
        lobbyPanel.add(statusLabel, BorderLayout.CENTER);

        startGameButton.addActionListener(e -> connect());
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
                            currentTurn = Integer.parseInt(msg.split(" ")[1]);
                            log("Sıra PLAYER_" + currentTurn + "'da");

                            SwingUtilities.invokeLater(() -> {
                                boolean whiteToMove = (currentTurn == 1);

                                if (!boardPanel.hasAnyValidMove(whiteToMove)) {
                                    boardPanel.disableInput();
                                    setGameOver(true);

                                    String resultMessage;
                                    if (boardPanel.isKingInCheck(whiteToMove)) {
                                        resultMessage = "♚ ŞAH MAT!\n" + (whiteToMove ? "Siyah" : "Beyaz") + " kazandı!";
                                        log(resultMessage);
                                    } else {
                                        resultMessage = "🤝 PAT!\nOyun berabere.";
                                        log(resultMessage);
                                    }

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
                        } else if (msg.startsWith("WIN")) {
                            int winner = Integer.parseInt(msg.split(" ")[1]);
                            String message = (winner == playerId)
                                    ? "🎉 Tebrikler, rakibin oyunu bıraktı. Kazandınız!"
                                    : "☠ Rakip oyunu kazandı, siz terk ettiniz.";

                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(boardPanel, message, "Oyun Bitti", JOptionPane.INFORMATION_MESSAGE);
                                showLobby();
                            });
                        } else if (msg.equals("RESIGN_REPLAY_REQUEST")) {
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
