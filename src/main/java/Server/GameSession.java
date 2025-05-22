package Server;

import java.io.*;
import java.net.*;

public class GameSession {

    private final Socket player0Socket;
    private final Socket player1Socket;
    private PrintWriter out0, out1;
    private BufferedReader in0, in1;
    private int turn = 1; // PLAYER_1 (beyaz) başlar
    private int replayVotes = 0;

    public GameSession(Socket player0Socket, Socket player1Socket) throws IOException {
        this.player0Socket = player0Socket;
        this.player1Socket = player1Socket;

        out0 = new PrintWriter(player0Socket.getOutputStream(), true);
        out1 = new PrintWriter(player1Socket.getOutputStream(), true);
        in0 = new BufferedReader(new InputStreamReader(player0Socket.getInputStream()));
        in1 = new BufferedReader(new InputStreamReader(player1Socket.getInputStream()));
    }

    public void start() {
        sendInitialMessages();

        new Thread(() -> handlePlayer(in0, out0, out1, 0)).start();
        new Thread(() -> handlePlayer(in1, out1, out0, 1)).start();
    }

    private void sendInitialMessages() {
        out0.println("CONNECTED:PLAYER_0");
        out1.println("CONNECTED:PLAYER_1");
        startNewGame();  // başlatma mesajı burada
    }

    private void startNewGame() {
        turn = 1;
        replayVotes = 0;
        out0.println("START");
        out1.println("START");
        out0.println("TURN " + turn);
        out1.println("TURN " + turn);
        System.out.println("✅ Yeni oyun başlatıldı.");
    }

    private PrintWriter getOtherOut(int playerId) {
        return playerId == 0 ? out1 : out0;
    }

    private void handlePlayer(BufferedReader in, PrintWriter ownOut, PrintWriter otherOut, int playerId) {
        try {
            String input;
            while ((input = in.readLine()) != null) {
                if (input.startsWith("MOVE")) {
                    String[] parts = input.split(" ");
                    int fromRow = Integer.parseInt(parts[1]);
                    int fromCol = Integer.parseInt(parts[2]);
                    int toRow = Integer.parseInt(parts[3]);
                    int toCol = Integer.parseInt(parts[4]);

                    boolean isCastling = parts.length > 5 && parts[5].equals("CASTLE");
                    String promotedPiece = (parts.length > 5 && !parts[5].equals("CASTLE")) ? parts[5] : null;

                    if (playerId == turn) {
                        System.out.println("♟ Hamle: " + input);
                        ownOut.println(input);
                        otherOut.println(input);

                        if (promotedPiece != null) {
                            ownOut.println("PROMOTED " + promotedPiece);
                            otherOut.println("PROMOTED " + promotedPiece);
                        }

                        turn = 1 - turn;
                        ownOut.println("TURN " + turn);
                        otherOut.println("TURN " + turn);
                    } else {
                        ownOut.println("❌ Sıra sizde değil!");
                    }
                } else if (input.equals("REPLAY_REQUEST")) {
                    System.out.println("🔁 PLAYER_" + playerId + " tekrar oynamak istiyor.");
                    replayVotes++;

                    if (replayVotes == 2) {
                        startNewGame();
                    }
                } else if (input.equals("REPLAY_DECLINE")) {
                    System.out.println("❌ PLAYER_" + playerId + " tekrar oynamayı reddetti.");
                    out0.println("REPLAY_DECLINE");
                    out1.println("REPLAY_DECLINE");
                } else if (input.equals("RESIGN")) {
                    System.out.println("❌ PLAYER_" + playerId + " oyunu terk etti.");
                    String winMessage = "WIN " + (1 - playerId);
                    out0.println(winMessage);
                    out1.println(winMessage);
                    break;
                } else if (input.equals("RESIGN_AND_REPLAY")) {
                    System.out.println("PLAYER_" + playerId + " terk etti ve tekrar oynamak istiyor.");
                    replayVotes++;  // EKLENDİ 🔥
                    getOtherOut(playerId).println("RESIGN_REPLAY_REQUEST");

                    if (replayVotes == 2) {
                        startNewGame();
                    }
                } else if (input.equals("RESIGN_AND_LOBBY")) {
                    System.out.println("PLAYER_" + playerId + " terk etti ve lobiye dönmek istiyor.");
                    out0.println("REPLAY_DECLINE");
                    out1.println("REPLAY_DECLINE");
                }
            }
        } catch (IOException e) {
            System.out.println("⚠ Bağlantı kesildi: PLAYER_" + playerId);
        }
    }
}
