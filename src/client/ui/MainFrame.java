package client.ui;

import client.ClientApp;
import client.JsonUtil;
import client.ui.HistoryFrame;
import model.HandelMatchMulti;
import model.Player;
import client.net.TcpClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

public class MainFrame extends JFrame {
    private final TcpClient tcp;
    private final String myPlayerId;
    private final String nickname;
    private client.ui.HistoryFrame historyFrame;
    private client.ui.LoginFrame loginFrame; // Lưu instance MatchSolo hiện tại

    // Bảng người chơi online (3 cột)
    private final DefaultTableModel playersModel = new DefaultTableModel(
            new Object[] { "PlayerId", "Player", "Action" }, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return c == 2;
        }
    };
    private JTable tblPlayers;
    private TableRowSorter<DefaultTableModel> sorter;

    // Bảng BXH
    private final DefaultTableModel lbModel = new DefaultTableModel(new Object[] { "Nickname", "TotalScore", "Wins" },
            0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private JTable tblLb;

    private final CardLayout cards = new CardLayout();
    private final JPanel center = new JPanel(cards);

    private final JComboBox<String> cbType = new JComboBox<>(new String[] { "ID", "Nickname" });
    private final JTextField tfQuery = new JTextField(12);

    public MainFrame(TcpClient tcp, String myPlayerId, String nickname) {
        super("Màn hình chính – " + nickname);
        this.tcp = tcp;
        this.myPlayerId = myPlayerId;
        this.nickname = nickname;
    }

    public MainFrame(TcpClient tcp, String myPlayerId, String nickname, client.ui.LoginFrame loginFrame) {
        super("Màn hình chính – " + nickname);
        this.tcp = tcp;
        this.myPlayerId = myPlayerId;
        this.nickname = nickname;
        this.loginFrame = loginFrame;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1024, 640);
        setLocationRelativeTo(null);

        buildUI();

        // load ban đầu
        loadPlayers();
    }

    /* ===================== UI ===================== */

    private void buildUI() {
        var btnPlayers = topButton("người chơi online");
        var btnLeaderboard = topButton("BXH");
        var btnLogout = dangerButton("Đăng xuất");

        var topLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        topLeft.add(btnPlayers);
        topLeft.add(btnLeaderboard);

        var top = new JPanel(new BorderLayout());
        top.add(topLeft, BorderLayout.WEST);
        top.add(btnLogout, BorderLayout.EAST);

        // bảng người chơi
        tblPlayers = new JTable(playersModel) {
            @Override
            public boolean isCellEditable(int row, int col) {
                if (col != 2)
                    return false;
                Object v = getModel().getValueAt(row, 2);
                return v != null && "Thách đấu".equals(v.toString());
            }
        };
        tblPlayers.setRowHeight(30);
        tblPlayers.setFillsViewportHeight(true);
        tblPlayers.getTableHeader().setReorderingAllowed(false);

        // Renderer + Editor cho nút Action
        tblPlayers.getColumn("Action").setCellRenderer(new ButtonRenderer());
        tblPlayers.getColumn("Action").setCellEditor(new ButtonEditor(modelRow -> {
            String targetId = String.valueOf(playersModel.getValueAt(modelRow, 0));
            String targetName = String.valueOf(playersModel.getValueAt(modelRow, 1));
            String status = String.valueOf(playersModel.getValueAt(modelRow, 2));
            if (!"Thách đấu".equals(status))
                return;
            invitePvp(targetId, targetName);
        }));

        enableSingleClickButton(tblPlayers, 2);

        sorter = new TableRowSorter<>(playersModel);
        tblPlayers.setRowSorter(sorter);

        // bảng BXH
        tblLb = new JTable(lbModel);
        tblLb.setRowHeight(28);
        tblLb.setFillsViewportHeight(true);
        tblLb.getTableHeader().setReorderingAllowed(false);

        // center
        center.add(new JScrollPane(tblPlayers), "players");
        center.add(new JScrollPane(tblLb), "lb");
        cards.show(center, "players");

        JPanel left = buildLeftPane();

        var main = new JPanel(new BorderLayout());
        main.add(top, BorderLayout.NORTH);
        main.add(left, BorderLayout.WEST);
        main.add(center, BorderLayout.CENTER);
        setContentPane(main);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                try {
                    var m = new com.google.gson.JsonObject();
                    m.addProperty("type", "EXIT");
                    tcp.send(client.JsonUtil.toJson(m));
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException ignored) {
                    }
                } catch (Exception ex) {
                    // ignore
                }
                // để mặc định EXIT_ON_CLOSE sẽ đóng app
            }
        });

        // actions
        btnPlayers.addActionListener(e -> {
            cards.show(center, "players");
            loadPlayers();
        });
        btnLeaderboard.addActionListener(e -> {
            cards.show(center, "lb");
            loadLeaderboard();
        });
        btnLogout.addActionListener(e -> sendType("LOGOUT"));
    }

    private JPanel buildLeftPane() {
        var wrap = new JPanel();
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        wrap.setPreferredSize(new Dimension(300, 0));

        var title = new JLabel("Bảng điều khiển");
        title.setFont(uiFont().deriveFont(Font.BOLD, 15f));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        var cardActions = new JPanel();
        cardActions.setLayout(new BoxLayout(cardActions, BoxLayout.Y_AXIS));
        cardActions.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardActions.setBorder(cardBorder("Tác vụ"));

        JButton btnHistory = softButton("Xem lịch sử đấu", "FileView.directoryIcon");
        JButton btnCreate = softButton("Tạo phòng đấu", "FileView.computerIcon");
        btnHistory.addActionListener(e -> {
            var m = new com.google.gson.JsonObject();
            m.addProperty("type", "GET_HISTORY");
            m.addProperty("limit", 100);
            try {
                tcp.send(client.JsonUtil.toJson(m));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Mạng lỗi:" + ex.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
        btnCreate.addActionListener(e -> {
            var m = new JsonObject();
            System.out.println("Creating multiplayer room");
            m.addProperty("type", "CREATE_MULTIPLAYER_ROOM");
            try {
                tcp.send(JsonUtil.toJson(m));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        cardActions.add(btnHistory);
        cardActions.add(Box.createVerticalStrut(8));
        cardActions.add(btnCreate);

        var cardSearch = new JPanel(new GridBagLayout());
        cardSearch.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardSearch.setBorder(cardBorder("Tìm kiếm người chơi"));
        var gc = new GridBagConstraints();
        gc.insets = new Insets(6, 4, 6, 4);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;

        JLabel lbType = new JLabel("Theo:");
        lbType.setForeground(new Color(90, 98, 110));
        cbType.setPreferredSize(new Dimension(140, 28));
        tfQuery.setPreferredSize(new Dimension(180, 28));
        var btnSearch = softButton("Tìm kiếm", "FileView.fileIcon");
        var btnClear = softButton("Xóa lọc", "FileChooser.homeFolderIcon");

        btnSearch.addActionListener(e -> applySearch());
        btnClear.addActionListener(e -> {
            tfQuery.setText("");
            sorter.setRowFilter(null);
        });

        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 0;
        cardSearch.add(lbType, gc);
        gc.gridx = 1;
        gc.gridy = 0;
        gc.weightx = 1;
        cardSearch.add(cbType, gc);
        gc.gridx = 0;
        gc.gridy = 1;
        gc.gridwidth = 2;
        cardSearch.add(tfQuery, gc);
        gc.gridwidth = 1;
        gc.gridx = 0;
        gc.gridy = 2;
        gc.weightx = 0.5;
        cardSearch.add(btnSearch, gc);
        gc.gridx = 1;
        gc.gridy = 2;
        gc.weightx = 0.5;
        cardSearch.add(btnClear, gc);

        wrap.add(title);
        wrap.add(cardActions);
        wrap.add(Box.createVerticalStrut(12));
        wrap.add(cardSearch);
        wrap.add(Box.createVerticalGlue());
        return wrap;
    }

    /* ================= Buttons & Border helpers ================= */

    private JButton softButton(String text, String uiKeyIcon) {
        JButton b = new JButton(text);
        try {
            Icon ico = (Icon) UIManager.get(uiKeyIcon);
            if (ico != null)
                b.setIcon(ico);
        } catch (Exception ignore) {
        }
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(new Color(210, 214, 222), 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        b.setBackground(new Color(245, 247, 250));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton topButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        return b;
    }

    private JButton dangerButton(String text) {
        JButton b = new JButton(text);
        b.setForeground(new Color(180, 35, 35));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        return b;
    }

    private TitledBorder cardBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 232), 1, true),
                title, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
                uiFont().deriveFont(Font.BOLD));
    }

    /** Font an toàn (không null) */
    private Font uiFont() {
        Font f = UIManager.getFont("Label.font");
        if (f == null)
            f = (new JLabel()).getFont();
        if (f == null)
            f = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        return f;
    }

    /* ================= Logic gửi/nhận ================= */

    private void applySearch() {
        String q = tfQuery.getText().trim();
        if (q.isEmpty()) {
            sorter.setRowFilter(null);
            return;
        }
        int col = cbType.getSelectedIndex() == 0 ? 0 : 1;
        sorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(q), col));
    }

    private void enableSingleClickButton(JTable table, int actionCol) {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row < 0 || col != actionCol)
                    return;
                if (!table.isCellEditable(row, col))
                    return;
                if (table.editCellAt(row, col, e)) {
                    Component editor = table.getEditorComponent();
                    if (editor instanceof JButton b)
                        b.doClick();
                }
            }
        });
    }

    // private void invitePvp(String toUserId, String toName) {
    //     if (toUserId.equals(myPlayerId)) return;

    //     JsonObject m = new JsonObject();
    //     m.addProperty("type", "INVITE-SOLO");
    //     m.addProperty("fromId",  myPlayerId);
    //     m.addProperty("fromNick", nickname);
    //     m.addProperty("toId",    toUserId);

    //     try { tcp.send(JsonUtil.toJson(m)); }
    //     catch (IOException ex) { ex.printStackTrace(); }
    // }
    // private void showInviteDialogWithTimeout(String fromId, String fromNick) {
    //     final JDialog dialog = new JDialog(this, "Lời mời thách đấu", true);
    //     final JPanel  panel  = new JPanel(new BorderLayout(8,8));
    //     final JLabel  label  = new JLabel(fromNick + " mời bạn thách đấu (5)");
    //     label.setBorder(BorderFactory.createEmptyBorder(10,10,0,10));

    //     JButton btnAccept = new JButton("Chấp nhận");
    //     JButton btnDeny   = new JButton("Từ chối");

    //     JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    //     btns.add(btnDeny); btns.add(btnAccept);
    //     panel.add(label, BorderLayout.CENTER);
    //     panel.add(btns,  BorderLayout.SOUTH);
    //     dialog.setContentPane(panel);
    //     dialog.pack();
    //     dialog.setLocationRelativeTo(this);

    //     // Timer đếm ngược 5s
    //     // final int[] sec = {5};
    //     // Timer timer = new Timer(1000, e -> {
    //     //     sec[0]--;
    //     //     label.setText(fromNick + " mời bạn thách đấu (" + sec[0] + ")");
    //     //     if (sec[0] <= 0) {
    //     //         ((Timer)e.getSource()).stop();
    //     //         sendDenied(fromId, "timeout");
    //     //         dialog.dispose();
    //     //     }
    //     // });
    //     // timer.start();

    //     btnAccept.addActionListener(e -> {
    //         timer.stop();
    //         // Người nhận gửi START-GAME lên server (server sẽ phát cho cả 2)
    //         JsonObject m = new JsonObject();
    //         m.addProperty("type", "START-GAME");
    //         m.addProperty("p1Id", fromId);        // người mời
    //         m.addProperty("p1Nick", fromNick);
    //         m.addProperty("p2Id", myPlayerId);    // mình (người nhận)
    //         m.addProperty("p2Nick", nickname);
    //         try { tcp.send(JsonUtil.toJson(m)); } catch (IOException ex) { ex.printStackTrace(); }
    //         dialog.dispose();
    //     });

    //     btnDeny.addActionListener(e -> {
    //         timer.stop();
    //         sendDenied(fromId, "user");
    //         dialog.dispose();
    //     });

    //     dialog.setVisible(true);
    // }

    private void sendDenied(String challengerId, String reason) {
        JsonObject m = new JsonObject();
        m.addProperty("type", "DENIED");
        m.addProperty("toId", challengerId); // gửi tới người mời
        m.addProperty("byId", myPlayerId);
        m.addProperty("byNick", nickname);
        m.addProperty("reason", reason); // "timeout" | "user"
        try { tcp.send(JsonUtil.toJson(m)); } catch (IOException ex) { ex.printStackTrace(); }
    }

    private void invitePvp(String toUserId, String toName) {
        if (toUserId.equals(myPlayerId)) return;

        JsonObject m = new JsonObject();
        m.addProperty("type", "INVITE-SOLO");
        m.addProperty("fromId",  myPlayerId);
        m.addProperty("fromNick", nickname);
        m.addProperty("toId",    toUserId);

        try { tcp.send(JsonUtil.toJson(m)); }
        catch (IOException ex) { ex.printStackTrace(); }
    }

    private void showInviteDialogWithTimeout(String fromId, String fromNick) {
        final JDialog dialog = new JDialog(this, "Lời mời thách đấu", true);
        final JPanel  panel  = new JPanel(new BorderLayout(8,8));
        final JLabel  label  = new JLabel(fromNick + " mời bạn thách đấu (5)");
        label.setBorder(BorderFactory.createEmptyBorder(10,10,0,10));

        JButton btnAccept = new JButton("Chấp nhận");
        JButton btnDeny   = new JButton("Từ chối");

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.add(btnDeny); btns.add(btnAccept);
        panel.add(label, BorderLayout.CENTER);
        panel.add(btns,  BorderLayout.SOUTH);
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);

        // Timer đếm ngược 5s
        final int[] sec = {5};
        Timer timer = new Timer(1000, e -> {
            sec[0]--;
            label.setText(fromNick + " mời bạn thách đấu (" + sec[0] + ")");
            if (sec[0] <= 0) {
                ((Timer)e.getSource()).stop();
                sendDenied(fromId, "timeout");
                dialog.dispose();
            }
        });
        timer.start();

        btnAccept.addActionListener(e -> {
            timer.stop();
            // Người nhận gửi START-GAME lên server (server sẽ phát cho cả 2)
            JsonObject m = new JsonObject();
            m.addProperty("type", "START-GAME");
            m.addProperty("p1Id", fromId);        // người mời
            m.addProperty("p1Nick", fromNick);
            m.addProperty("p2Id", myPlayerId);    // mình (người nhận)
            m.addProperty("p2Nick", nickname);
            try { tcp.send(JsonUtil.toJson(m)); } catch (IOException ex) { ex.printStackTrace(); }
            dialog.dispose();
        });

        btnDeny.addActionListener(e -> {
            timer.stop();
            sendDenied(fromId, "user");
            dialog.dispose();
        });

        dialog.setVisible(true);
    }

    private void loadPlayers() {
        var m = new JsonObject();
        m.addProperty("type", "LIST_PLAYERS");
        try {
            tcp.send(JsonUtil.toJson(m));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void loadLeaderboard() {
        var m = new JsonObject();
        m.addProperty("type", "GET_LEADERBOARD");
        m.addProperty("limit", 50);
        try {
            tcp.send(JsonUtil.toJson(m));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void sendType(String t) {
        var m = new JsonObject();
        m.addProperty("type", t);
        try {
            tcp.send(JsonUtil.toJson(m));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /* ================= Router nhận message ================= */
    public void handleLine(String line) {
        System.out.println("MainFrame.handleLine() được gọi với: " + line);
        try {
            var msg = JsonUtil.fromJson(line, JsonObject.class);
            String type = msg.get("type").getAsString();
            switch (type) {
                case "PLAYERS_LIST" -> {
                    playersModel.setRowCount(0);
                    JsonArray arr = msg.getAsJsonArray("players");
                    for (var el : arr) {
                        var o = el.getAsJsonObject();
                        String pid = o.get("playerId").getAsString();
                        if (pid.equals(myPlayerId))
                            continue;
                        String name = o.get("nickname").getAsString();
                        String status = o.get("status").getAsString();
                        playersModel.addRow(new Object[] { pid, name, status.equals("IN_GAME") ? "Đang chơi" : "Thách đấu" });
                    }
                }
                case "ONLINE_LIST" -> {
                    playersModel.setRowCount(0);
                    JsonArray arr = msg.getAsJsonArray("rows");
                    for (var el : arr) {
                        var o = el.getAsJsonObject();
                        String pid = o.get("playerId").getAsString();
                        if (pid.equals(myPlayerId))
                            continue;
                        String name = o.get("nickname").getAsString();
                        String status = o.get("status").getAsString();
                        playersModel.addRow(new Object[] { pid, name, status.equals("IN_GAME") ? "Đang chơi" : "Thách đấu" });
                    }
                }
                case "ONLINE_ADD" -> {
                    String pid = msg.get("playerId").getAsString();
                    if (pid.equals(myPlayerId))
                        break;
                    String name = msg.get("nickname").getAsString();
                    if (findRowById(pid) < 0)
                        playersModel.addRow(new Object[] { pid, name, "Thách đấu" });
                }
                case "ONLINE_REMOVE" -> {
                    int r = findRowById(msg.get("playerId").getAsString());
                    if (r >= 0)
                        playersModel.removeRow(r);
                }
                case "LEADERBOARD" -> {
                    lbModel.setRowCount(0);
                    JsonArray arr = msg.getAsJsonArray("rows");
                    for (var el : arr) {
                        var o = el.getAsJsonObject();
                        lbModel.addRow(new Object[] {
                                o.get("nickname").getAsString(),
                                o.get("totalScore").getAsInt(),
                                o.get("totalWins").getAsInt()
                        });
                    }
                }

                case "HISTORY" -> {
                    com.google.gson.JsonArray rows = msg.getAsJsonArray("rows");

                    SwingUtilities.invokeLater(() -> {
                        try {
                            // 1) Tạo instance HistoryFrame đúng constructor
                            if (historyFrame == null) {
                                java.awt.Window owner = javax.swing.SwingUtilities.getWindowAncestor(this);
                                HistoryFrame hf;
                                try {
                                    // Ưu tiên ctor (Frame, boolean) nếu có (thường là JDialog sinh từ NetBeans)
                                    java.lang.reflect.Constructor<HistoryFrame> c = HistoryFrame.class
                                            .getConstructor(java.awt.Frame.class, boolean.class);
                                    java.awt.Frame f = (owner instanceof java.awt.Frame) ? (java.awt.Frame) owner
                                            : null;
                                    hf = c.newInstance(f, true);
                                } catch (NoSuchMethodException e) {
                                    // Không có ctor (Frame, boolean) -> dùng ctor rỗng
                                    hf = HistoryFrame.class.getDeclaredConstructor().newInstance();
                                }
                                historyFrame = hf;
                                historyFrame.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
                            }

                            // 2) Nạp dữ liệu: gọi đúng hàm mà HistoryFrame của bạn đang có
                            // Nếu HistoryFrame có public void load(JsonArray rows)
                            try {
                                var m = HistoryFrame.class.getMethod("load", com.google.gson.JsonArray.class);
                                m.invoke(historyFrame, rows);
                            } catch (NoSuchMethodException noLoad) {
                                // Fallback: nếu không có load(JsonArray), chuyển về List<Object[]> và gọi
                                // setData(List)
                                java.util.List<Object[]> table = new java.util.ArrayList<>();
                                for (int i = 0; i < rows.size(); i++) {
                                    var o = rows.get(i).getAsJsonObject();
                                    int matchId = o.get("matchId").getAsInt();
                                    String mode = o.get("mode").getAsString();
                                    String start = o.get("startTime").isJsonNull() ? ""
                                            : o.get("startTime").getAsString();
                                    String end = o.get("endTime").isJsonNull() ? "" : o.get("endTime").getAsString();
                                    int score = o.get("score").getAsInt();
                                    String result = o.get("status").getAsString();
                                    table.add(new Object[] { matchId, mode, start, end, score, result });
                                }
                                try {
                                    var m2 = HistoryFrame.class.getMethod("setData", java.util.List.class);
                                    m2.invoke(historyFrame, table);
                                } catch (NoSuchMethodException noSetData) {
                                    // Fallback cuối: bơm thẳng vào JTable tên tblHistory nếu có
                                    try {
                                        var fld = HistoryFrame.class.getDeclaredField("tblHistory");
                                        fld.setAccessible(true);
                                        javax.swing.JTable tbl = (javax.swing.JTable) fld.get(historyFrame);
                                        javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) tbl
                                                .getModel();
                                        model.setRowCount(0);
                                        for (Object[] r : table)
                                            model.addRow(r);
                                    } catch (NoSuchFieldException nf) {
                                        System.err.println(
                                                "[HISTORY] Không tìm thấy load(JsonArray)/setData(List) và cũng không có JTable 'tblHistory'.");
                                    }
                                }
                            }

                            // 3) Hiển thị
                            historyFrame.setLocationRelativeTo(this);
                            historyFrame.setVisible(true);
                            historyFrame.toFront();

                        } catch (Throwable t) {
                            t.printStackTrace();
                            javax.swing.JOptionPane.showMessageDialog(this,
                                    "Không thể mở lịch sử đấu: " + t.getMessage(),
                                    "Lỗi UI", javax.swing.JOptionPane.ERROR_MESSAGE);
                        }
                    });
                }
                case "LOGOUT_OK" -> {
                    JOptionPane.showMessageDialog(this, "Đã đăng xuất!");
                    // Tái kết nối trước khi mở LoginFrame
                    if (ClientApp.connectToServer()) {
                        LoginFrame lf = new LoginFrame(ClientApp.tcp());
                        lf.setVisible(true);
                        this.setVisible(false);
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "Không thể tái kết nối tới server. Vui lòng khởi động lại ứng dụng.",
                                "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
                        System.exit(0);
                    }
                }
                case "AUTH_ERR" -> {
                    String reason = msg.has("reason") ? msg.get("reason").getAsString() : "";
                    if ("INVALID_CREDENTIALS".equals(reason)) {
                        javax.swing.JOptionPane.showMessageDialog(
                                this,
                                "Sai tài khoản / mật khẩu",
                                "Đăng nhập thất bại",
                                javax.swing.JOptionPane.ERROR_MESSAGE);
                    } else {
                        javax.swing.JOptionPane.showMessageDialog(
                                this,
                                "Đăng nhập thất bại",
                                "Lỗi",
                                javax.swing.JOptionPane.ERROR_MESSAGE);
                    }
                }
                case "SERVER_ERROR" -> {
                    String m = msg.has("message") ? msg.get("message").getAsString() : "Lỗi máy chủ";
                    javax.swing.JOptionPane.showMessageDialog(
                            this, m, "Đăng nhập thất bại", javax.swing.JOptionPane.ERROR_MESSAGE);
                }
                case "INVITE-SOLO" -> {
                    // gói mời mình từ đối phương
                    String fromId   = msg.get("fromId").getAsString();
                    String fromNick = msg.get("fromNick").getAsString();
                    showInviteDialogWithTimeout(fromId, fromNick);
                }

                case "DENIED" -> {
                    // đối phương từ chối (hoặc hết giờ)
                    String byNick = msg.get("byNick").getAsString();
                    String reason = msg.has("reason") ? msg.get("reason").getAsString() : "denied";
                    JOptionPane.showMessageDialog(this,
                            byNick + " đã từ chối (" + reason + ")",
                            "Thách đấu", JOptionPane.INFORMATION_MESSAGE);
                }

                case "START-GAME" -> {
                    String p1Id   = msg.get("p1Id").getAsString();
                    String p1Nick = msg.get("p1Nick").getAsString();
                    String p2Id   = msg.get("p2Id").getAsString();
                    String p2Nick = msg.get("p2Nick").getAsString();

                    // xác định đối thủ của mình
                    String oppId   = myPlayerId.equals(p1Id) ? p2Id   : p1Id;
                    String oppNick = myPlayerId.equals(p1Id) ? p2Nick : p1Nick;
                    MatchSolo currentMatchSolo= new MatchSolo(tcp, myPlayerId, nickname, oppId, oppNick);
                    ClientApp.setMessageHandler(currentMatchSolo::handleMessage);
                    currentMatchSolo.setMainFrame(this);
                    currentMatchSolo.setVisible(true);
                    setVisible(false);       
                   
                }
                
                case "OPPONENT_LEFT" -> {
                    String playerName = msg.get("playerName").getAsString();
                    JOptionPane.showMessageDialog(this,
                            playerName + " đã rời phòng đấu.",
                            "Đối phương rời phòng",
                            JOptionPane.INFORMATION_MESSAGE);
                }
                case "OPPONENT_SURRENDERED" -> {
                    String playerName = msg.get("playerName").getAsString();
                    JOptionPane.showMessageDialog(this,
                            playerName + " đã đầu hàng. Bạn thắng!",
                            "Đối phương đầu hàng",
                            JOptionPane.INFORMATION_MESSAGE);
                }
                case "CREATE_MULTIPLAYER_ROOM_ACK" -> {
                    HandelMatchMulti match = HandelMatchMulti.fromJson(msg.get("match").getAsString());
                    Player me = Player.fromJson(msg.get("me").getAsString());
                    this.setVisible(false);
                    var game = new MultiplayerRoomFrame(match, me, tcp, this);
                    game.setVisible(true);
                }
                case "INVITE_MULTIPLE_USERS_TO_MATCH" -> {
                    Player fromPlayer = Player.fromJson(msg.get("fromPlayer").getAsString());
                    int matchId = msg.get("matchId").getAsInt();
                    System.out.println(fromPlayer);
                    int option = JOptionPane.showConfirmDialog(this,
                            "'" + fromPlayer.getNickname() + "' mời bạn chơi game!! Bạn có đồng ý không?",
                            "Lời mời thách đấu", JOptionPane.YES_NO_OPTION);
                    if (option == JOptionPane.YES_OPTION) {
                        var m = new JsonObject();
                        m.addProperty("type", "ACCEPT_MULTIPLE_USERS_MATCH_INVITE");
                        m.addProperty("matchId", matchId);
                        try {
                            tcp.send(JsonUtil.toJson(m));
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        var m = new JsonObject();
                        m.addProperty("type", "DECLINE_MULTIPLE_USERS_MATCH_INVITE");
                        m.addProperty("toPlayerId", fromPlayer.getPlayerId());
                        try {
                            tcp.send(JsonUtil.toJson(m));
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
                case "ACCEPT_MULTIPLE_USERS_MATCH_INVITE" -> {
                    HandelMatchMulti match = HandelMatchMulti.fromJson(msg.get("match").getAsString());
                    Player me = Player.fromJson(msg.get("me").getAsString());
                    this.setVisible(false);
                    var game = new MultiplayerRoomFrame(match, me, tcp, this);
                    game.setVisible(true);
                }
            }
        } catch (

        Exception ignore) {
        }
    }

    private int findRowById(String playerId) {
        for (int i = 0; i < playersModel.getRowCount(); i++)
            if (playerId.equals(playersModel.getValueAt(i, 0)))
                return i;
        return -1;
    }

    private void showChallengeDialog(String inviterId, String inviterName) {
        JDialog dialog = new JDialog(this, "Thách đấu", true);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Thông báo
        JLabel messageLabel = new JLabel("<html><div style='text-align: center;'>" +
                "<h3>Thách đấu từ " + inviterName + "</h3>" +
                "<p>Bạn có muốn chấp nhận thách đấu không?</p>" +
                "</div></html>");
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(messageLabel, BorderLayout.CENTER);

        // Panel nút bấm
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton acceptBtn = new JButton("Chấp nhận");
        JButton rejectBtn = new JButton("Từ chối");

        acceptBtn.addActionListener(e -> {
            dialog.dispose();
            sendChallengeResponse(inviterId, true);
            // openMatchFrame sẽ được gọi từ server response "START-GAME"
        });

        rejectBtn.addActionListener(e -> {
            dialog.dispose();
            sendChallengeResponse(inviterId, false);
        });

        buttonPanel.add(acceptBtn);
        buttonPanel.add(rejectBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void sendChallengeResponse(String inviterId, boolean accepted) {
        if (!accepted) {
            // Từ chối - gửi DENIED
            sendDenied(inviterId, "user");
            return;
        }
        
        // Chấp nhận - gửi START-GAME (giống như showInviteDialogWithTimeout)
        var m = new JsonObject();
        m.addProperty("type", "START-GAME");
        m.addProperty("p1Id", inviterId);        // người mời
        m.addProperty("p1Nick", "Opponent");     // tạm thời, sẽ được server xử lý
        m.addProperty("p2Id", myPlayerId);       // mình (người nhận)
        m.addProperty("p2Nick", nickname);
        try {
            tcp.send(JsonUtil.toJson(m));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void openMatchFrame(String opponentId, String opponentName) {
        MatchSolo matchSolo = new MatchSolo(tcp, myPlayerId, nickname, opponentId, opponentName);
        matchSolo.setMainFrame(this);


        matchSolo.setVisible(true);
        this.setVisible(false);  
    }


    /* ===== Renderer/Editor cho nút trong bảng ===== */

    private static class ButtonRenderer extends JButton implements TableCellRenderer {
        ButtonRenderer() {
            setOpaque(true);
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            setBackground(new Color(240, 244, 248));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int column) {
            setText(value == null ? "" : value.toString());
            return this;
        }
    }

    private static class ButtonEditor extends AbstractCellEditor
            implements TableCellEditor, java.awt.event.ActionListener {

        private final JButton btn = new JButton();
        private int modelRow = -1;
        private final java.util.function.IntConsumer onClick;

        ButtonEditor(java.util.function.IntConsumer onClick) {
            this.onClick = onClick;
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            btn.setBackground(new Color(240, 244, 248));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(this);
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table, Object value, boolean isSelected, int row, int column) {
            btn.setText(value == null ? "" : value.toString());
            this.modelRow = table.convertRowIndexToModel(row); // nhớ hàng theo MODEL
            return btn;
        }

        @Override
        public Object getCellEditorValue() {
            return btn.getText();
        }

        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            try {
                if (modelRow >= 0)
                    onClick.accept(modelRow);
            } finally {
                fireEditingStopped();
            }
        }
    }

    public void reopen() {
        this.setVisible(true);
        loadPlayers();
    }
}
