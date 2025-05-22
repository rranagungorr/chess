package Server;

import java.io.*;
import java.net.*;
import java.util.*;

public class ChessServer {

    private static final int PORT = 12345;
    private static final List<Socket> waitingPlayers = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("🟢 Ana Sunucu başlatılıyor...");

        try ( ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("🔌 Yeni oyuncu bağlandı: " + socket.getInetAddress());

                synchronized (waitingPlayers) {
                    waitingPlayers.add(socket);
                    System.out.println("👥 Bekleyen oyuncu sayısı: " + waitingPlayers.size());

                    if (waitingPlayers.size() >= 2) {
                        Socket p1 = waitingPlayers.remove(0);
                        Socket p2 = waitingPlayers.remove(0);
                        new Thread(() -> startGameSession(p1, p2)).start();
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Sunucu hatası: " + e.getMessage());
        }
    }

    private static void startGameSession(Socket player0, Socket player1) {
        System.out.println("🎮 Yeni eşleşme başlatılıyor...");
        try {
            GameSession session = new GameSession(player0, player1);
            session.start();
        } catch (IOException e) {
            System.out.println("⚠ Eşleşme hatası: " + e.getMessage());
        }
    }
}
