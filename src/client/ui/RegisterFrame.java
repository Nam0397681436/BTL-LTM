package client.ui;

import client.JsonUtil;
import client.net.TcpClient;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.function.Consumer;

public class RegisterFrame extends JFrame {
    private final TcpClient tcp;
    private final Consumer<JsonObject> onAuthOk;
    private final Runnable onBackToLogin;

    public RegisterFrame(TcpClient tcp, Consumer<JsonObject> onAuthOk, Runnable onBackToLogin) {
        super("Đăng ký");
        this.tcp = tcp; this.onAuthOk = onAuthOk; this.onBackToLogin = onBackToLogin;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(380, 240);
        setLocationRelativeTo(null);
        render();
    }

    private void render() {
        var tfUser = new JTextField();
        var tfPass = new JPasswordField();
        var tfNick = new JTextField();

        var btnRegister = new JButton("Register");
        var btnBackLogin = new JButton("Đã có tài khoản?");

        var p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx=0; c.gridy=0; p.add(new JLabel("Username:"), c);
        c.gridx=1; c.gridy=0; p.add(tfUser, c);
        c.gridx=0; c.gridy=1; p.add(new JLabel("Password:"), c);
        c.gridx=1; c.gridy=1; p.add(tfPass, c);
        c.gridx=0; c.gridy=2; p.add(new JLabel("Nickname:"), c);
        c.gridx=1; c.gridy=2; p.add(tfNick, c);

        var rowBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rowBtn.add(btnBackLogin);
        rowBtn.add(btnRegister);
        c.gridwidth=2; c.gridx=0; c.gridy=3; p.add(rowBtn, c);

        setContentPane(p);

        btnRegister.addActionListener(e -> {
            JsonObject m = new JsonObject();
            m.addProperty("type","AUTH_REGISTER");
            m.addProperty("username", tfUser.getText().trim());
            m.addProperty("password", new String(tfPass.getPassword()));
            m.addProperty("nickname", tfNick.getText().trim());
            send(m);
        });

        btnBackLogin.addActionListener(e -> {
            dispose();
            if (onBackToLogin != null) onBackToLogin.run();
        });
    }

    private void send(JsonObject m) {
        try {
            tcp.send(JsonUtil.toJson(m));
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                this,
                "Mất kết nối đến server. Vui lòng kiểm tra server và thử lại.",
                "Lỗi mạng",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    public void handleLine(String line) {
        try {
            var msg = JsonUtil.fromJson(line, JsonObject.class);
            String type = msg.get("type").getAsString();
            switch (type) {
                case "AUTH_OK" -> SwingUtilities.invokeLater(() -> {
                    onAuthOk.accept(msg);
                    dispose();
                });
                case "AUTH_ERR" -> SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this,
                        msg.get("reason").getAsString(),
                        "Lỗi đăng ký",
                        JOptionPane.ERROR_MESSAGE)
                );
            }
        } catch (Exception ignored) {}
    }
}
