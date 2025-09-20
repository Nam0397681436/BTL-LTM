package client;

import client.net.TcpClient;
import client.ui.HistoryFrame;
import client.ui.LoginFrame;
import client.ui.MainFrame;
import client.ui.RegisterFrame;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.io.IOException;

public class ClientApp {

    public static void main(String[] args) {
        // Cho phép cấu hình host/port khi chạy:
        //   mvn -Dexec.mainClass=client.ClientApp -DHOST=127.0.0.1 -DPORT=5555 exec:java
        final String host = System.getProperty("HOST", "127.0.0.1");
        final int port = Integer.parseInt(System.getProperty(
                "PORT", (args != null && args.length > 0) ? args[0] : "5555"));

        final TcpClient tcp = new TcpClient();

        // Mở màn Đăng nhập. Khi AUTH_OK -> mở MainFrame (truyền kèm playerId & nickname)
        SwingUtilities.invokeLater(() -> {
            LoginFrame login = new LoginFrame(tcp, (JsonObject authOk) -> {
                String nickname = authOk.get("nickname").getAsString();
                String playerId = authOk.get("playerId").getAsString();
                MainFrame main = new MainFrame(tcp, playerId, nickname);
                main.setVisible(true);
            });
            login.setVisible(true);
        });

        // Kết nối TCP và router mọi message tới các cửa sổ đang hiển thị
        try {
            tcp.connect(host, port, (line) -> SwingUtilities.invokeLater(() -> {
                for (java.awt.Window w : java.awt.Window.getWindows()) {
                    if (!w.isShowing()) continue;
                    if (w instanceof LoginFrame lf)     lf.handleLine(line);
                    if (w instanceof RegisterFrame rf) rf.handleLine(line);
                    if (w instanceof MainFrame mf)     mf.handleLine(line);
                    if (w instanceof HistoryFrame hf)  hf.handleLine(line);
                }
            }));

            // Test đường truyền: gửi PING 1 lần
            try { tcp.send("{\"type\":\"PING\"}"); } catch (IOException ignore) {}

        } catch (IOException e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(
                    null,
                    "Không thể kết nối tới server " + host + ":" + port +
                    "\nHãy kiểm tra server đã chạy và firewall cho phép kết nối.",
                    "Lỗi mạng",
                    JOptionPane.ERROR_MESSAGE
                )
            );
        }
    }
}
