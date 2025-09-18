package client.ui;

import client.JsonUtil;
import client.net.TcpClient;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.function.Consumer;

public class LoginFrame extends JFrame {
    private final TcpClient tcp;
    private final Consumer<JsonObject> onAuthOk;

    public LoginFrame(TcpClient tcp, Consumer<JsonObject> onAuthOk) {
        super("Đăng nhập");
        this.tcp = tcp; this.onAuthOk = onAuthOk;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(360, 200);
        setLocationRelativeTo(null);
        render();
    }

    private void render() {
        var tfUser = new JTextField();
        var tfPass = new JPasswordField();

        var btnLogin = new JButton("Login");
        var btnGoRegister = new JButton("Tạo tài khoản");

        var p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx=0; c.gridy=0; p.add(new JLabel("Username:"), c);
        c.gridx=1; c.gridy=0; p.add(tfUser, c);
        c.gridx=0; c.gridy=1; p.add(new JLabel("Password:"), c);
        c.gridx=1; c.gridy=1; p.add(tfPass, c);

        var rowBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rowBtn.add(btnGoRegister);
        rowBtn.add(btnLogin);
        c.gridwidth = 2; c.gridx=0; c.gridy=2; p.add(rowBtn, c);

        setContentPane(p);

        btnLogin.addActionListener(e -> {
            JsonObject m = new JsonObject();
            m.addProperty("type","AUTH_LOGIN");
            m.addProperty("username", tfUser.getText().trim());
            m.addProperty("password", new String(tfPass.getPassword()));
            send(m);
        });

        btnGoRegister.addActionListener(e -> {
            var reg = new RegisterFrame(tcp, onAuthOk, () -> this.setVisible(true));
            reg.setVisible(true);
            this.setVisible(false);
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
                        "Lỗi đăng nhập",
                        JOptionPane.ERROR_MESSAGE)
                );
            }
        } catch (Exception ignored) {}
    }
}
