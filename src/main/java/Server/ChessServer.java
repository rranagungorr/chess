package Server;

import java.io.*;
import java.net.*;
import java.util.*;

public class ChessServer {

    private static final int PORT = 12345;

    private static final Map<String, ClientHandler> onlineUsers = new HashMap<>();
    private static final Object lock = new Object();

    public static void main(String[] args) {
        System.out.println("🟢 Satranç Sunucusu başlatılıyor...");

        try (ServerSocket serverSocket = new ServerSocket(PORT,50, InetAddress.getByName("0.0.0.0") )) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("🔌 Yeni bağlantı: " + socket.getInetAddress());
                new Thread(new ClientHandler(socket)).start();
            }
        } catch (IOException e) {
            System.out.println("❌ Sunucu hatası: " + e.getMessage());
        }
    }
}
