package Server;

import java.io.*;

public class GameSession {

    private final ClientHandler player0;
    private final ClientHandler player1;

    private final PrintWriter out0, out1;
    private final BufferedReader in0, in1;

    private int turn = 1; // PLAYER_1 (beyaz) başlar

    private boolean player0WantsReplay = false;
    private boolean player1WantsReplay = false;

    public GameSession(ClientHandler player0, ClientHandler player1) throws IOException {
        this.player0 = player0;
        this.player1 = player1;

        this.out0 = player0.getOut();
        this.out1 = player1.getOut();

        this.in0 = new BufferedReader(new InputStreamReader(player0.getSocket().getInputStream()));
        this.in1 = new BufferedReader(new InputStreamReader(player1.getSocket().getInputStream()));
    }

    public void start() {
        sendInitialMessages();

        new Thread(() -> handlePlayer(in0, out0, out1, 0)).start();
        new Thread(() -> handlePlayer(in1, out1, out0, 1)).start();
    }

    private void sendInitialMessages() {
        out0.println("CONNECTED:PLAYER_0");
        out1.println("CONNECTED:PLAYER_1");
        startNewGame();
    }

    private void startNewGame() {
        turn = 1;
        out0.println("START");
        out1.println("START");
        out0.println("TURN " + turn);
        out1.println("TURN " + turn);
        player0WantsReplay = false;
        player1WantsReplay = false;
        System.out.println("✅ Yeni oyun başlatıldı.");
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
                        ownOut.println(input);
                        otherOut.println(input);

                        if (promotedPiece != null) {
                            ownOut.println("PROMOTED " + promotedPiece);
                            otherOut.println("PROMOTED " + promotedPiece);
                        }

                        turn = 1 - turn;
                        out0.println("TURN " + turn);
                        out1.println("TURN " + turn);
                    } else {
                        ownOut.println("❌ Sıra sizde değil!");
                    }

                } else if (input.equals("REPLAY_REQUEST")) {
                    System.out.println("🔁 PLAYER_" + playerId + " tekrar oynamak istiyor.");

                    if (playerId == 0 && !player0WantsReplay) {
                        player0WantsReplay = true;
                    } else if (playerId == 1 && !player1WantsReplay) {
                        player1WantsReplay = true;
                    }

                    if (player0WantsReplay && player1WantsReplay) {
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

                    if (playerId == 0 && !player0WantsReplay) {
                        player0WantsReplay = true;
                        out1.println("RESIGN_REPLAY_REQUEST");
                    } else if (playerId == 1 && !player1WantsReplay) {
                        player1WantsReplay = true;
                        out0.println("RESIGN_REPLAY_REQUEST");
                    }

                    if (player0WantsReplay && player1WantsReplay) {
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

    public synchronized void processMove(String move, ClientHandler sender) {
        int playerId = sender == player0 ? 0 : 1;
        PrintWriter ownOut = sender.getOut();
        PrintWriter otherOut = (sender == player0 ? player1.getOut() : player0.getOut());

        String[] parts = move.split(" ");
        if (parts.length < 5) {
            return;
        }

        int fromRow = Integer.parseInt(parts[1]);
        int fromCol = Integer.parseInt(parts[2]);
        int toRow = Integer.parseInt(parts[3]);
        int toCol = Integer.parseInt(parts[4]);

        boolean isCastling = parts.length > 5 && parts[5].equals("CASTLE");
        String promotedPiece = (parts.length > 5 && !parts[5].equals("CASTLE")) ? parts[5] : null;

        if (playerId == turn) {
            ownOut.println(move);
            otherOut.println(move);

            if (promotedPiece != null) {
                ownOut.println("PROMOTED " + promotedPiece);
                otherOut.println("PROMOTED " + promotedPiece);
            }

            turn = 1 - turn;
            out0.println("TURN " + turn);
            out1.println("TURN " + turn);
        } else {
            ownOut.println("❌ Sıra sizde değil!");
        }
    }

    public synchronized void handleResignReplay(ClientHandler sender) {
        int playerId = sender == player0 ? 0 : 1;

        if (playerId == 0 && !player0WantsReplay) {
            player0WantsReplay = true;
            player1.getOut().println("RESIGN_REPLAY_REQUEST");
        } else if (playerId == 1 && !player1WantsReplay) {
            player1WantsReplay = true;
            player0.getOut().println("RESIGN_REPLAY_REQUEST");
        }

        if (player0WantsReplay && player1WantsReplay) {
            startNewGame();
        }
    }

    public synchronized void handleReplayRequest(ClientHandler sender) {
        int playerId = sender == player0 ? 0 : 1;

        if (playerId == 0 && !player0WantsReplay) {
            player0WantsReplay = true;
        } else if (playerId == 1 && !player1WantsReplay) {
            player1WantsReplay = true;
        }

        if (player0WantsReplay && player1WantsReplay) {
            startNewGame();
        }
    }

    public synchronized void handleResignToLobby(ClientHandler sender) {
        int playerId = sender == player0 ? 0 : 1;
        ClientHandler winner = (playerId == 0) ? player1 : player0;
        ClientHandler loser = sender;

        winner.getOut().println("WIN_BY_RESIGN " + winner.getUsername());
        loser.getOut().println("LOSE_BY_RESIGN " + winner.getUsername());

        winner.getOut().println("REPLAY_DECLINE");
        loser.getOut().println("REPLAY_DECLINE");

        System.out.println("🏁 " + loser.getUsername() + " oyunu bıraktı. Kazanan: " + winner.getUsername());
    }

}
