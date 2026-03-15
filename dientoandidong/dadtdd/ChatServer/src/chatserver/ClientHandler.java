import java.io.*;
import java.net.Socket;
import java.sql.*;

public class ClientHandler extends Thread {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    // ===== SQL SERVER CONFIG =====
    private static final String DB_URL =
        "jdbc:sqlserver://localhost:1433;databaseName=chat_app1;encrypt=false;trustServerCertificate=true";
    private static final String DB_USER = "sa";
    private static final String DB_PASS = "1";

    // Load JDBC Driver
    static {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

   @Override
public void run() {
    try {
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Nhận username
        username = in.readLine();
        saveUser(username);

        // Gửi lịch sử chat
        sendChatHistory();

        broadcast("🔵 " + username + " đã tham gia!");

        String message;
        while ((message = in.readLine()) != null) {

            // 🔄 CLEAR CHAT
            if (message.equals("/clear")) {
                clearChatInDB();
                broadcast("/clear");
                continue;
            }

            // ❌ DELETE 1 DÒNG (CHỈ XÓA UI)
            if (message.startsWith("/delete:")) {
                // gửi lệnh delete cho tất cả client
                synchronized (ChatServer.clients) {
                    for (ClientHandler c : ChatServer.clients) {
                        c.out.println(message);
                    }
                }
                continue; // ❗ CỰC KỲ QUAN TRỌNG
            }

            // 💬 CHAT BÌNH THƯỜNG
            saveMessage(username, message);
            broadcast(username + ": " + message);
        }

    } catch (Exception e) {
        System.out.println("❌ Client ngắt kết nối: " + username);
    } finally {
        ChatServer.clients.remove(this);
        broadcast("🔴 " + username + " đã rời chat");
    }
}


    // ================= DATABASE =================

    private void saveUser(String user) {
        String sql =
            "IF NOT EXISTS (SELECT 1 FROM users WHERE username = ?) " +
            "INSERT INTO users(username) VALUES (?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user);
            ps.setString(2, user);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveMessage(String user, String msg) {
        String sql = "INSERT INTO messages(sender, content) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user);
            ps.setString(2, msg);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ❌ XÓA TOÀN BỘ CHAT TRONG DB
    private void clearChatInDB() {
        String sql = "DELETE FROM messages";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement st = conn.createStatement()) {

            st.executeUpdate(sql);
            System.out.println("🗑️ Đã xóa toàn bộ lịch sử chat trong DB");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendChatHistory() {
        String sql = "SELECT sender, content FROM messages ORDER BY created_at";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                out.println(rs.getString("sender") + ": " +
                            rs.getString("content"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= CHAT =================

    private void broadcast(String msg) {
        synchronized (ChatServer.clients) {
            for (ClientHandler client : ChatServer.clients) {
                client.out.println(msg);
            }
        }
    }
}
