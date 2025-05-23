package Server;

import java.io.*;
import java.net.*;
import java.util.*;

public class ClientHandler implements Runnable {

    private static final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private Socket socket;
    private String username;
    private BufferedReader in;
    private PrintWriter out;
    private GameSession gameSession;
     

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public String getUsername() {
        return username;
    }

    public PrintWriter getOut() {
        return out;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setGameSession(GameSession session) {
        this.gameSession = session;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String line;
            while ((line = in.readLine()) != null) {

                if (line.startsWith("USERNAME ")) {
                    username = line.substring(9).trim();
                    clients.add(this);
                    broadcastUserList();

                } else if (line.startsWith("INVITE ")) {
                    String target = line.substring(7).trim();
                    ClientHandler targetClient = findClientByUsername(target);
                    if (targetClient != null) {
                        targetClient.getOut().println("INVITE_FROM " + username);
                    }

                } else if (line.startsWith("INVITE_ACCEPT ")) {
                    String inviter = line.substring(14).trim();
                    ClientHandler inviterClient = findClientByUsername(inviter);
                    if (inviterClient != null) {
                        GameSession session = new GameSession(this, inviterClient);
                        this.setGameSession(session);
                        inviterClient.setGameSession(session);
                        new Thread(() -> session.start()).start();
                    }

                } else if (line.startsWith("INVITE_DECLINE ")) {
                    String inviter = line.substring(15).trim();
                    ClientHandler inviterHandler = findClientByUsername(inviter);
                    if (inviterHandler != null) {
                        inviterHandler.getOut().println("INVITE_DECLINED_BY " + username);
                    }

                } else if (line.equals("REPLAY_REQUEST") && gameSession != null) {
                    gameSession.handleReplayRequest(this);
                } else if (line.equals("REFRESH")) {
                    sendUserListToClient(this);
                } // 🌟 MOVE mesajını ilgili GameSession'a ilet
                else if (line.startsWith("MOVE") && gameSession != null) {
                    gameSession.processMove(line, this);
                } else if (line.equals("RESIGN_AND_REPLAY") && gameSession != null) {
                    gameSession.handleResignReplay(this);
                } else if (line.equals("RESIGN_AND_LOBBY") && gameSession != null) {
                    gameSession.handleResignToLobby(this);
                }

                // Diğer komutlar (REPLAY, RESIGN, vs.) buraya eklenebilir
            }

        } catch (IOException e) {
            System.out.println("❌ Client bağlantı hatası: " + e.getMessage());

        } finally {
            clients.remove(this);
            broadcastUserList();
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void sendUserListToClient(ClientHandler client) {
        StringBuilder list = new StringBuilder("USERLIST ");
        synchronized (clients) {
            for (ClientHandler c : clients) {
                if (!c.equals(client)) {
                    list.append(c.getUsername()).append(" ");
                }
            }
        }
        client.getOut().println(list.toString().trim());
    }

    private void broadcastUserList() {
        StringBuilder list = new StringBuilder("USER_LIST");
        synchronized (clients) {
            for (ClientHandler client : clients) {
                list.append(" ").append(client.getUsername());
            }
            for (ClientHandler client : clients) {
                client.getOut().println(list.toString());
            }
        }
    }

    private ClientHandler findClientByUsername(String name) {
        synchronized (clients) {
            for (ClientHandler c : clients) {
                if (c.getUsername().equals(name)) {
                    return c;
                }
            }
        }
        return null;
    }
}
