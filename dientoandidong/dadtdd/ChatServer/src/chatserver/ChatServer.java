import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ChatServer {

    // Danh sách client đang kết nối
    public static Set<ClientHandler> clients =
            Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) {

        int port = 5000;
        System.out.println("🟢 Chat Server đang chạy tại port " + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("✅ Client kết nối từ: " +
                        socket.getInetAddress().getHostAddress());

                ClientHandler client = new ClientHandler(socket);
                clients.add(client);
                client.start();
            }

        } catch (Exception e) {
            System.out.println("❌ LỖI SERVER");
            e.printStackTrace();
        }
    }
}
