package client.ui;

import client.ClientApp;
import client.JsonUtil;
import client.net.TcpClient;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class RegisterFrame extends JFrame {
    private final TcpClient tcp;
    private JTextField tfUser, tfNick;
    private JPasswordField pfPass;
    private JButton btnHave, btnReg;

    private final AtomicBoolean waiting = new AtomicBoolean(false);
    private Timer timeoutTimer;

    public RegisterFrame(TcpClient tcp) {
        super("Đăng ký");
        this.tcp = tcp;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(460, 260);
        setLocationRelativeTo(null);

        render();                              
        ClientApp.setMessageHandler(this::handleLine);
    }

    /* ==================== UI mới cho màn Đăng ký ==================== */
    private void render() {
        final int LABEL_W = 110;
        final int FIELD_W = 220;
        final int ROW_H   = 28;
        final Dimension BTN_SIZE = new Dimension(140, 34);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 8, 8, 8);
        gc.anchor = GridBagConstraints.WEST;

        JLabel lbUser = new JLabel("Username:");
        lbUser.setPreferredSize(new Dimension(LABEL_W, ROW_H));
        JLabel lbPass = new JLabel("Password:");
        lbPass.setPreferredSize(new Dimension(LABEL_W, ROW_H));
        JLabel lbNick = new JLabel("Nickname:");
        lbNick.setPreferredSize(new Dimension(LABEL_W, ROW_H));

        tfUser = new JTextField();      tfUser.setPreferredSize(new Dimension(FIELD_W, ROW_H));
        pfPass = new JPasswordField();  pfPass.setPreferredSize(new Dimension(FIELD_W, ROW_H));
        tfNick = new JTextField();      tfNick.setPreferredSize(new Dimension(FIELD_W, ROW_H));

        btnHave = new JButton("Đã có tài khoản?");
        btnReg  = new JButton("Đăng ký");
        for (JButton b : new JButton[]{btnHave, btnReg}) {
            b.setPreferredSize(BTN_SIZE);
            b.setMinimumSize(BTN_SIZE);
            b.setMaximumSize(BTN_SIZE);
            b.setFocusPainted(false);
        }

        gc.gridx=0; gc.gridy=0; gc.weightx=0; form.add(lbUser, gc);
        gc.gridx=1; gc.gridy=0; gc.weightx=1; form.add(tfUser, gc);

        gc.gridx=0; gc.gridy=1; gc.weightx=0; form.add(lbPass, gc);
        gc.gridx=1; gc.gridy=1; gc.weightx=1; form.add(pfPass, gc);

        gc.gridx=0; gc.gridy=2; gc.weightx=0; form.add(lbNick, gc);
        gc.gridx=1; gc.gridy=2; gc.weightx=1; form.add(tfNick, gc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        buttons.add(btnHave);
        buttons.add(btnReg);

        gc.gridx=0; gc.gridy=3; gc.gridwidth=2; gc.weightx=1;
        form.add(buttons, gc);

        setContentPane(form);

        btnHave.addActionListener(e -> dispose());
        btnReg.addActionListener(e -> doRegister());
        getRootPane().setDefaultButton(btnReg);
    }

    private void setWaiting(boolean on) {
        waiting.set(on);
        if (btnHave != null) btnHave.setEnabled(!on);
        if (btnReg  != null) btnReg.setEnabled(!on);
        setCursor(on ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
    }

    private void doRegister() {
        if (waiting.get()) return;

        String u = tfUser.getText().trim();
        String p = new String(pfPass.getPassword());
        String n = tfNick.getText().trim();
        if (u.isEmpty() || p.isEmpty() || n.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đủ thông tin.");
            return;
        }

        ClientApp.setMessageHandler(this::handleLine);

        var m = new JsonObject();
        m.addProperty("type","AUTH_REGISTER");
        m.addProperty("username", u);
        m.addProperty("password", p);
        m.addProperty("nickname", n);

        try {
            setWaiting(true);
            if (timeoutTimer != null) timeoutTimer.stop();
            timeoutTimer = new Timer(8000, ev -> {
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
                    "Mất kết nối đến server. Vui lòng thử lại.",
                    "Lỗi mạng", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleLine(String line) {
        try {
            var msg = JsonUtil.fromJson(line, JsonObject.class);
            String type = msg.get("type").getAsString();
            switch (type) {
                case "AUTH_ERR" -> {
                    if (timeoutTimer != null) timeoutTimer.stop();
                    setWaiting(false);
                    JOptionPane.showMessageDialog(this,
                            msg.get("reason").getAsString(),
                            "Đăng ký thất bại", JOptionPane.ERROR_MESSAGE);
                }
                case "AUTH_OK" -> {
                    if (timeoutTimer != null) timeoutTimer.stop();
                    setWaiting(false);
                    JOptionPane.showMessageDialog(this, "Đăng ký thành công! Bạn đã được đăng nhập.");
                }
            }
        } catch (Exception ignore) {}
    }

    /* =================== Panel đăng ký nhúng  =================== */
    public static JPanel createEmbedded(
            TcpClient tcp,
            java.util.function.BiConsumer<String,String> onSuccess,
            Runnable onBack
    ) {
        return new EmbeddedPanel(tcp, onSuccess, onBack);
    }

    private static final class EmbeddedPanel extends JPanel {
        private final TcpClient tcp;
        private final java.util.function.BiConsumer<String,String> onSuccess;
        private final Runnable onBack;

        private final JTextField tfUser = new JTextField(18);
        private final JPasswordField pfPass = new JPasswordField(18);
        private final JTextField tfNick = new JTextField(18);
        private final JButton btnBack = new JButton("Đã có tài khoản?");
        private final JButton btnReg  = new JButton("Đăng ký");

        private final AtomicBoolean waiting = new AtomicBoolean(false);
        private Timer timeoutTimer;

        EmbeddedPanel(TcpClient tcp,
                      java.util.function.BiConsumer<String,String> onSuccess,
                      Runnable onBack) {
            super(new GridBagLayout());
            this.tcp = tcp; this.onSuccess = onSuccess; this.onBack = onBack;

            final int LABEL_W = 110, FIELD_W = 220, ROW_H = 28;
            final Dimension BTN_SIZE = new Dimension(140, 34);

            setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
            GridBagConstraints gc = new GridBagConstraints();
            gc.insets = new Insets(8, 8, 8, 8);
            gc.anchor = GridBagConstraints.WEST;

            JLabel lbUser = new JLabel("Username:"); lbUser.setPreferredSize(new Dimension(LABEL_W, ROW_H));
            JLabel lbPass = new JLabel("Password:"); lbPass.setPreferredSize(new Dimension(LABEL_W, ROW_H));
            JLabel lbNick = new JLabel("Nickname:"); lbNick.setPreferredSize(new Dimension(LABEL_W, ROW_H));

            tfUser.setPreferredSize(new Dimension(FIELD_W, ROW_H));
            pfPass.setPreferredSize(new Dimension(FIELD_W, ROW_H));
            tfNick.setPreferredSize(new Dimension(FIELD_W, ROW_H));

            for (JButton b : new JButton[]{btnBack, btnReg}) {
                b.setPreferredSize(BTN_SIZE);
                b.setMinimumSize(BTN_SIZE);
                b.setMaximumSize(BTN_SIZE);
                b.setFocusPainted(false);
            }

            gc.gridx=0; gc.gridy=0; add(lbUser, gc);
            gc.gridx=1; gc.gridy=0; add(tfUser, gc);
            gc.gridx=0; gc.gridy=1; add(lbPass, gc);
            gc.gridx=1; gc.gridy=1; add(pfPass, gc);
            gc.gridx=0; gc.gridy=2; add(lbNick, gc);
            gc.gridx=1; gc.gridy=2; add(tfNick, gc);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
            buttons.add(btnBack);
            buttons.add(btnReg);

            gc.gridx=0; gc.gridy=3; gc.gridwidth=2; add(buttons, gc);

            btnBack.addActionListener(e -> { if (!waiting.get()) onBack.run(); });
            btnReg.addActionListener(e -> submit());
            ClientApp.setMessageHandler(this::handleLine);

            // Enter = Đăng ký
            SwingUtilities.getWindowAncestor(this);
            getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ENTER"), "SUBMIT");
            getActionMap().put("SUBMIT", new AbstractAction() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { submit(); }
            });
        }

        private void setWaiting(boolean on) {
            waiting.set(on);
            btnBack.setEnabled(!on);
            btnReg.setEnabled(!on);
            setCursor(on ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
        }

        private void submit() {
            if (waiting.get()) return;
            String u = tfUser.getText().trim();
            String p = new String(pfPass.getPassword());
            String n = tfNick.getText().trim();
            if (u.isEmpty() || p.isEmpty() || n.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đủ thông tin.");
                return;
            }

            ClientApp.setMessageHandler(this::handleLine);

            var m = new JsonObject();
            m.addProperty("type","AUTH_REGISTER");
            m.addProperty("username", u);
            m.addProperty("password", p);
            m.addProperty("nickname", n);

            try {
                setWaiting(true);
                if (timeoutTimer != null) timeoutTimer.stop();
                timeoutTimer = new Timer(8000, ev -> {
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
                        "Mất kết nối đến server. Vui lòng thử lại.",
                        "Lỗi mạng", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void handleLine(String line) {
            try {
                var msg = JsonUtil.fromJson(line, JsonObject.class);
                String type = msg.get("type").getAsString();
                switch (type) {
                    case "AUTH_ERR" -> {
                        if (timeoutTimer != null) timeoutTimer.stop();
                        setWaiting(false);
                        JOptionPane.showMessageDialog(this,
                                msg.get("reason").getAsString(),
                                "Đăng ký thất bại", JOptionPane.ERROR_MESSAGE);
                    }
                    case "AUTH_OK" -> {
                        if (timeoutTimer != null) timeoutTimer.stop();
                        setWaiting(false);
                        String myId = msg.get("playerId").getAsString();
                        String nick = msg.get("nickname").getAsString();
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(this, "Đăng ký thành công! Bạn đã được đăng nhập.");
                            onSuccess.accept(myId, nick);
                        });
                    }
                }
            } catch (Exception ignore) {}
        }
    }
}
