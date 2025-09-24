package client;

import client.net.TcpClient;
import java.util.function.Consumer;
import javax.swing.*;


public class ClientApp {

    private static TcpClient TCP;
    private static volatile Consumer<String> messageHandler; // handler hiện tại của UI

    /** Cho UI (MainFrame...) đăng ký nơi nhận message từ server. */
    public static void setMessageHandler(Consumer<String> handler) {
        messageHandler = handler;
    }

    /** Cho UI truy cập TcpClient nếu cần (gửi thủ công). */
    public static TcpClient tcp() {
        return TCP;
    }

    public static void main(String[] args) {
        // 1) Kết nối TCP
        TCP = new client.net.TcpClient();
        try {
            TCP.connect();
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    null,
                    "Không thể kết nối tới server " +
                            System.getProperty("HOST", "26.239.82.76") + ":" + Integer.getInteger("PORT", 5555) +
                            "\nHãy kiểm tra server đã chạy và firewall cho phép kết nối.",
                    "Lỗi mạng", JOptionPane.ERROR_MESSAGE
            ));
            return;
        }

        // 2) Mở UI đăng nhập
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                new client.ui.LoginFrame(TCP).setVisible(true);
            }
});

        // 3) Luồng đọc nền: nhận NDJSON và chuyển cho UI hiện tại
        Thread rx = new Thread(() -> {
            try {
                String line;
                while ((line = TCP.readLine()) != null) {
                    // Đẩy về UI thread để thao tác Swing an toàn
                    final String msg = line;
                    Consumer<String> h = messageHandler;
                    if (h != null) {
                        SwingUtilities.invokeLater(() -> {
                            try { h.accept(msg); } catch (Exception ignore) {}
                        });
                    } else {
                        // Chưa có handler (ví dụ đang ở Login/Register) -> bỏ qua hoặc log
                        System.out.println("[RECV(no-handler)] " + msg);
                    }
                }
            } catch (Exception e) {
                System.out.println("[RX] socket closed: " + e.getMessage());
            } finally {
                try { TCP.close(); } catch (Exception ignore) {}
                // Thông báo nhẹ cho người dùng nếu đang ở MainFrame
                SwingUtilities.invokeLater(() -> {
                });
            }
        }, "tcp-reader");
        rx.setDaemon(true);
        rx.start();

        // 4) Đóng gói dọn dẹp khi app thoát
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { TCP.close(); } catch (Exception ignore) {}
        }));
    }
}
