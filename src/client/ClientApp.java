package client;

import client.net.TcpClient;
import java.util.function.Consumer;
import javax.swing.*;

public class ClientApp {

    private static TcpClient TCP;
    private static volatile Consumer<String> messageHandler; // handler hiện tại của UI
    private static Thread rxThread; // Luồng đọc từ server

    /** Cho UI (MainFrame...) đăng ký nơi nhận message từ server. */
    public static void setMessageHandler(Consumer<String> handler) {
        messageHandler = handler;
    }

    /** Cho UI truy cập TcpClient nếu cần (gửi thủ công). */
    public static TcpClient tcp() {
        return TCP;
    }

    /** Tạo kết nối mới hoặc tái kết nối */
    public static boolean connectToServer() {
        try {
            // Đóng kết nối cũ nếu có
            if (TCP != null) {
                TCP.close();
            }
            
            // Tạo kết nối mới
            TCP = new client.net.TcpClient();
            TCP.connect();
            
            // Khởi động lại luồng đọc nếu cần
            startReaderThread();
            
            return true;
        } catch (Exception e) {
            System.err.println("[CONNECT] Failed to connect: " + e.getMessage());
            return false;
        }
    }

    /** Khởi động luồng đọc từ server */
    private static void startReaderThread() {
        if (rxThread != null && rxThread.isAlive()) {
            return; // Luồng đã chạy
        }
        
        rxThread = new Thread(() -> {
            try {
                String line;
                while ((line = TCP.readLine()) != null) {
                    // Đẩy về UI thread để thao tác Swing an toàn
                    final String msg = line;
                    Consumer<String> h = messageHandler;
                    System.out.println("ClientApp nhận message: " + msg + ", có handler: " + (h != null));
                    if (h != null) {
                        SwingUtilities.invokeLater(() -> {
                            try {
                                h.accept(msg);
                            } catch (Exception ignore) {
                                System.out.println("Lỗi trong message handler: " + ignore.getMessage());
                            }
                        });
                    } else {
                        // Chưa có handler (ví dụ đang ở Login/Register) -> bỏ qua hoặc log
                        System.out.println("[RECV(no-handler)] " + msg);
                    }
                }
            } catch (Exception e) {
                System.out.println("[RX] socket closed: " + e.getMessage());
            } finally {
                try {
                    if (TCP != null) {
                        TCP.close();
                    }
                } 
                catch (Exception ignore) {
                }
            }
        }, "tcp-reader");
        rxThread.setDaemon(true);
        rxThread.start();
    }

    public static void main(String[] args) {
        // 1) Kết nối TCP
        if (!connectToServer()) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    null,
                    "Không thể kết nối tới server " +
                            System.getProperty("HOST", "127.0.0.1") + ":" + Integer.getInteger("PORT", 5555) +
                            "\nHãy kiểm tra server đã chạy và firewall cho phép kết nối.",
                    "Lỗi mạng", JOptionPane.ERROR_MESSAGE));
            return;
        }

        // 2) Mở UI đăng nhập
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new client.ui.LoginFrame(TCP).setVisible(true);
            }
        });

        // 3) Đóng gói dọn dẹp khi app thoát
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (TCP != null) {
                    TCP.close();
                }
            } catch (Exception ignore) {
            }
        }));
    }
}
