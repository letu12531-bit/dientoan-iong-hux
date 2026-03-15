import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class ChatClient extends JFrame {

    private JTextArea chatArea;
    private JTextField inputField;
    private PrintWriter out;

    public ChatClient(String username) {
        setTitle("Chat - " + username);
        setSize(500, 420);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);

        inputField = new JTextField();

        JButton sendBtn = new JButton("Gửi");
        JButton deleteBtn = new JButton("Xóa dòng");
        JButton refreshBtn = new JButton("Làm mới");

        // ===== GỬI TIN =====
        sendBtn.addActionListener(e -> {
            String text = inputField.getText().trim();
            if (!text.isEmpty()) {
                out.println(text);
                inputField.setText("");
            }
        });

        // ❌ XÓA 1 DÒNG (CẢ SERVER + CLIENT KHÁC)
        deleteBtn.addActionListener(e -> {
    int start = chatArea.getSelectionStart();
    int end = chatArea.getSelectionEnd();

    if (start == end) {
        JOptionPane.showMessageDialog(this,
                "Hãy bôi đen 1 dòng chat để xóa!");
        return;
    }

    String text = chatArea.getText();

    // xác định dòng đang chọn
    int lineStart = text.lastIndexOf("\n", start - 1) + 1;
    int lineEnd = text.indexOf("\n", end);

    if (lineEnd == -1) {
        lineEnd = text.length();
    } else {
        lineEnd++; // xóa cả ký tự xuống dòng
    }

    String fullLine = text.substring(lineStart, lineEnd).trim();

    // ✅ 1. XÓA NGAY TRÊN CLIENT HIỆN TẠI
    chatArea.replaceRange("", lineStart, lineEnd);

    // ✅ 2. GỬI LỆNH CHO SERVER ĐỂ CLIENT KHÁC XÓA
    out.println("/delete:" + fullLine);
});


        // 🔄 CLEAR TOÀN BỘ CHAT
        refreshBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Bạn có chắc muốn xóa toàn bộ lịch sử chat?",
                    "Xác nhận",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                out.println("/clear");
            }
        });

        // ===== UI =====
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(inputField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 5, 5));
        buttonPanel.add(sendBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(refreshBtn);

        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(new JScrollPane(chatArea), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        connect(username);
    }

    private void connect(String username) {
        try {
            Socket socket = new Socket("localhost", 5000);
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println(username);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );

            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {

                        // CLEAR CHAT
                        if (msg.equals("/clear")) {
                            chatArea.setText("");
                        }
                        // DELETE 1 DÒNG
                        else if (msg.startsWith("/delete:")) {
                            String line = msg.substring(8);
                            removeLine(line);
                        }
                        // CHAT BÌNH THƯỜNG
                        else {
                            chatArea.append(msg + "\n");
                        }
                    }
                } catch (Exception e) {
                    chatArea.append("❌ Mất kết nối server\n");
                }
            }).start();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Không kết nối được server!",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // ❌ XÓA CHÍNH XÁC 1 DÒNG
    private void removeLine(String line) {
        String[] lines = chatArea.getText().split("\n");
        StringBuilder sb = new StringBuilder();

        boolean removed = false;
        for (String l : lines) {
            if (!removed && l.equals(line)) {
                removed = true;
                continue;
            }
            sb.append(l).append("\n");
        }
        chatArea.setText(sb.toString());
    }

    public static void main(String[] args) {
        String username = JOptionPane.showInputDialog("Nhập tên:");
        if (username != null && !username.trim().isEmpty()) {
            new ChatClient(username).setVisible(true);
        }
    }
}
