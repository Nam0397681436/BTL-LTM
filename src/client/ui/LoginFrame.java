package client.ui;

import client.ClientApp;
import client.JsonUtil;
import client.net.TcpClient;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class LoginFrame extends JFrame {
    private TcpClient tcp;

    // panel login gốc & panel đăng ký nhúng 
    private JPanel loginRoot;
    private JPanel regEmbedded;

    private JTextField tfUser;
    private JPasswordField pfPass;
    private JButton btnLogin, btnGoRegister;

    private final AtomicBoolean waiting = new AtomicBoolean(false);
    private Timer timeoutTimer;

    public LoginFrame(TcpClient tcp) {
        super("Đăng nhập");
        this.tcp = tcp;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(460, 260);
        setLocationRelativeTo(null);

        renderLoginUI();                 
        ClientApp.setMessageHandler(this::handleLine); // handler mặc định
    }

    private void renderLoginUI() {
        // Kích thước đồng bộ
        final int LABEL_W = 110;
        final int FIELD_W = 220;
        final int ROW_H   = 28;
        final Dimension BTN_SIZE = new Dimension(140, 34);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 8, 8, 8);
        gc.anchor = GridBagConstraints.WEST;

        JLabel lbUser = new JLabel("Tài khoản:");
        lbUser.setPreferredSize(new Dimension(LABEL_W, ROW_H));
        JLabel lbPass = new JLabel("Mật khẩu:");
        lbPass.setPreferredSize(new Dimension(LABEL_W, ROW_H));

        tfUser = new JTextField();
        tfUser.setPreferredSize(new Dimension(FIELD_W, ROW_H));
        pfPass = new JPasswordField();
        pfPass.setPreferredSize(new Dimension(FIELD_W, ROW_H));

        btnGoRegister = new JButton("Tạo tài khoản");
        btnLogin      = new JButton("Đăng nhập");
        for (JButton b : new JButton[]{btnGoRegister, btnLogin}) {
            b.setPreferredSize(BTN_SIZE);
            b.setMinimumSize(BTN_SIZE);
            b.setMaximumSize(BTN_SIZE);
            b.setFocusPainted(false);
        }

        // Layout 2 cột: nhãn – ô nhập
        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0; form.add(lbUser, gc);
        gc.gridx = 1; gc.gridy = 0; gc.weightx = 1; form.add(tfUser, gc);

        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0; form.add(lbPass, gc);
        gc.gridx = 1; gc.gridy = 1; gc.weightx = 1; form.add(pfPass, gc);

        // Hàng nút căn giữa
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        buttons.add(btnGoRegister);
        buttons.add(btnLogin);

        gc.gridx = 0; gc.gridy = 2; gc.gridwidth = 2; gc.weightx = 1;
        form.add(buttons, gc);

        loginRoot = form;
        setContentPane(loginRoot);

        // Actions
        btnGoRegister.addActionListener(e -> switchToRegister());
        btnLogin.addActionListener(e -> doLogin());
        getRootPane().setDefaultButton(btnLogin);
    }

    private void setWaiting(boolean on) {
        waiting.set(on);
        if (btnLogin != null) btnLogin.setEnabled(!on);
        if (btnGoRegister != null) btnGoRegister.setEnabled(!on);
        setCursor(on ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
    }

    private void doLogin() {
        if (waiting.get()) return;
        String u = tfUser.getText().trim();
        String p = new String(pfPass.getPassword());
        if (u.isEmpty() || p.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đủ tài khoản/mật khẩu.");
            return;
        }

        // Kiểm tra và tái kết nối nếu cần
        if (tcp == null || !isConnectionAlive()) {
            if (!ClientApp.connectToServer()) {
                JOptionPane.showMessageDialog(this,
                        "Không thể kết nối tới server " +
                                System.getProperty("HOST", "127.0.0.1") + ":" + Integer.getInteger("PORT", 5555) +
                                "\nHãy kiểm tra server đã chạy và firewall cho phép kết nối.",
                        "Lỗi mạng", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Cập nhật tcp reference
            tcp = ClientApp.tcp();
        }

        ClientApp.setMessageHandler(this::handleLine);

        var m = new JsonObject();
        m.addProperty("type", "AUTH_LOGIN");
        m.addProperty("username", u);
        m.addProperty("password", p);

        try {
            setWaiting(true);
            if (timeoutTimer != null) timeoutTimer.stop();
            timeoutTimer = new Timer(15000, ev -> { // timeout 15 giây
                setWaiting(false);
                JOptionPane.showMessageDialog(this, "Server không phản hồi. Vui lòng thử lại.",
                        "Lỗi mạng", JOptionPane.ERROR_MESSAGE);
            });
            timeoutTimer.setRepeats(false);
            timeoutTimer.start();

            tcp.send(JsonUtil.toJson(m));
        } catch (Exception ex) {
            setWaiting(false);
            JOptionPane.showMessageDialog(this,
                    "Không thể gửi dữ liệu tới server: " + ex.getMessage(),
                    "Lỗi mạng", JOptionPane.ERROR_MESSAGE);
        }
    }
    private boolean isConnectionAlive() {
        return tcp != null && tcp.isConnected();
    }

    private void handleLine(String line) {
        try {
            var msg = JsonUtil.fromJson(line, JsonObject.class);
            String type = msg.get("type").getAsString();
            switch (type) {
                case "AUTH_ERR" -> {
                    if (timeoutTimer != null) timeoutTimer.stop();
                    setWaiting(false);
                    JOptionPane.showMessageDialog(this, msg.get("reason").getAsString(),
                            "Đăng nhập thất bại", JOptionPane.ERROR_MESSAGE);
                }
                case "AUTH_OK" -> {
                    if (timeoutTimer != null) timeoutTimer.stop();
                    setWaiting(false);
                    String myId = msg.get("playerId").getAsString();
                    String nick = msg.get("nickname").getAsString();
                    SwingUtilities.invokeLater(() -> {
                        try {
                            MainFrame main = new MainFrame(tcp, myId, nick, this);
                            ClientApp.setMessageHandler(main::handleLine);
                            main.setVisible(true);
                            this.dispose(); // Đóng LoginFrame hoàn toàn
                        } catch (Throwable t) {
                            t.printStackTrace();
                            JOptionPane.showMessageDialog(this,
                                    "Không thể mở màn chính: " + t.getMessage(),
                                    "Lỗi UI", JOptionPane.ERROR_MESSAGE);
                        }
                    });
                }
            }
        } catch (Exception ignore) {}
    }

    private void switchToRegister() {
        if (regEmbedded == null) {
            regEmbedded = RegisterFrame.createEmbedded(
                    tcp,
                    (playerId, nickname) -> {   // onSuccess: mở MainFrame  
                        try {
                            MainFrame main = new MainFrame(tcp, playerId, nickname, this);
                            ClientApp.setMessageHandler(main::handleLine);
                            main.setVisible(true);
                            this.dispose(); // Đóng LoginFrame hoàn toàn
                        } catch (Throwable t) {
                            t.printStackTrace();
                            JOptionPane.showMessageDialog(this,
                                    "Không thể mở màn chính: " + t.getMessage(),
                                    "Lỗi UI", JOptionPane.ERROR_MESSAGE);
                        }
                    },
                    this::switchBackToLogin          // onBack: quay về login
            );
        }
        setTitle("Đăng ký");
        setContentPane(regEmbedded);
        revalidate();
        repaint();
    }

    private void switchBackToLogin() {
        ClientApp.setMessageHandler(this::handleLine);
        setTitle("Đăng nhập");
        setContentPane(loginRoot);
        revalidate();
        repaint();
    }
}
