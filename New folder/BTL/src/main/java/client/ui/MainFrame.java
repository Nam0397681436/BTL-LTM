package client.ui;

import client.JsonUtil;
import client.net.TcpClient;
import client.ui.table.ButtonEditor;
import client.ui.table.ButtonRenderer;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

public class MainFrame extends JFrame {
    private final TcpClient tcp;
    private final String myPlayerId;
    private final String nickname;

    // Bảng người chơi: PlayerId, Player, Status, Action
    private final DefaultTableModel playersModel =
            new DefaultTableModel(new Object[]{"PlayerId","Player","Status","Action"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return c == 3; }
            };

    // Bảng BXH
    private final DefaultTableModel lbModel =
            new DefaultTableModel(new Object[]{"Nickname","TotalScore","Wins"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };

    private JTable tblPlayers;
    private JTable tblLb;
    private final CardLayout cards = new CardLayout();
    private final JPanel center = new JPanel(cards);
    private HistoryFrame historyFrame;

    public MainFrame(TcpClient tcp, String myPlayerId, String nickname) {
        super("Màn hình chính – " + nickname);
        this.tcp = tcp; this.myPlayerId = myPlayerId; this.nickname = nickname;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 560);
        setLocationRelativeTo(null);

        buildUI();
        loadPlayers(); // mặc định hiện tab người chơi
    }

    private void buildUI() {
        // TOP: tabs + Logout
        var btnPlayers = new JButton("người chơi online");
        var btnLeaderboard = new JButton("BXH");
        var btnLogout = new JButton("Logout");

        var topLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        topLeft.add(btnPlayers); topLeft.add(btnLeaderboard);

        var top = new JPanel(new BorderLayout());
        top.add(topLeft, BorderLayout.WEST);
        top.add(btnLogout, BorderLayout.EAST);

        // LEFT: các nút chức năng
        var btnHistory = new JButton("Xem lịch sử đấu");
        var btnCreateRoom = new JButton("Tạo phòng đấu");
        var left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        left.add(btnHistory); left.add(Box.createVerticalStrut(8)); left.add(btnCreateRoom);

        // CENTER: cards (Players / Leaderboard)
        tblPlayers = new JTable(playersModel) {
            @Override public boolean isCellEditable(int row, int col) {
                // chỉ cho bấm ở cột Action khi text là "Thách đấu"
                if (col != 3) return false;
                Object v = getModel().getValueAt(row, 3);
                return v != null && "Thách đấu".equals(v.toString());
            }
        };
        tblPlayers.setRowHeight(28);
        tblPlayers.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        // renderer/editor cho cột Action
        tblPlayers.getColumn("Action").setCellRenderer(new ButtonRenderer());
        tblPlayers.getColumn("Action").setCellEditor(new ButtonEditor(e -> {
            // Lắng nghe click của nút trong ô
            int row = (int) ((JButton)e.getSource()).getClientProperty("row");
            String targetId   = String.valueOf(playersModel.getValueAt(row, 0));
            String targetName = String.valueOf(playersModel.getValueAt(row, 1));
            invitePvp(targetId, targetName);
        }));

        // Single-click để bấm nút ngay, không cần click 2 lần
        enableSingleClickButton(tblPlayers, 3);

        // chỉnh width để hiện trọn chữ
        resizePlayersColumns();

        // BXH
        tblLb = new JTable(lbModel);
        tblLb.setRowHeight(28);

        center.add(new JScrollPane(tblPlayers), "players");
        center.add(new JScrollPane(tblLb), "lb");

        // Root layout
        var main = new JPanel(new BorderLayout());
        main.add(top, BorderLayout.NORTH);
        main.add(left, BorderLayout.WEST);
        main.add(center, BorderLayout.CENTER);
        setContentPane(main);

        // Actions
        btnPlayers.addActionListener(e -> { cards.show(center, "players"); loadPlayers(); });
        btnLeaderboard.addActionListener(e -> { cards.show(center, "lb"); loadLeaderboard(); });
        btnLogout.addActionListener(e -> sendType("LOGOUT"));
        btnHistory.addActionListener(e -> {
            if (historyFrame == null || !historyFrame.isShowing()) {
                historyFrame = new HistoryFrame(tcp);
                historyFrame.setVisible(true);
            }
            historyFrame.load();
        });
        btnCreateRoom.addActionListener(e ->
                JOptionPane.showMessageDialog(this, "Tạo phòng (ROOM_CREATE) sẽ được bổ sung.",
                        "Thông báo", JOptionPane.INFORMATION_MESSAGE));
    }

    /** Bấm 1 lần là hoạt động nút trong cột Action */
    private void enableSingleClickButton(JTable table, int actionCol) {
        table.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row < 0 || col != actionCol) return;
                if (!table.isCellEditable(row, col)) return;

                if (table.editCellAt(row, col, e)) {
                    Component editor = table.getEditorComponent();
                    if (editor instanceof JButton b) {
                        b.putClientProperty("row", row);
                        b.putClientProperty("column", col);
                        b.doClick(); // kích hoạt ngay
                    }
                }
            }
        });
    }

    // ====== width columns để Action luôn hiển thị đủ ======
    private void resizePlayersColumns() {
        var cm = tblPlayers.getColumnModel();

        // PlayerId gọn để nhường chỗ
        cm.getColumn(0).setMinWidth(90);
        cm.getColumn(0).setPreferredWidth(120);
        cm.getColumn(0).setMaxWidth(160);

        // Status đủ "Trong trận"
        int wStatus = textWidth(tblPlayers, "Trong trận") + 30;
        cm.getColumn(2).setMinWidth(wStatus);
        cm.getColumn(2).setPreferredWidth(wStatus);
        cm.getColumn(2).setMaxWidth(wStatus + 20);

        // Action đủ "Thách đấu"
        int wAction = textWidth(tblPlayers, "Thách đấu") + 40;
        cm.getColumn(3).setMinWidth(wAction);
        cm.getColumn(3).setPreferredWidth(wAction);
        cm.getColumn(3).setMaxWidth(wAction + 30);
    }
    private static int textWidth(JTable t, String s) {
        FontMetrics fm = t.getFontMetrics(t.getFont());
        return fm.stringWidth(s);
    }

    // ====== gửi gói tin ======
    private void invitePvp(String toUserId, String toName) {
        if (toUserId.equals(myPlayerId)) return;
        var m = new JsonObject();
        m.addProperty("type","INVITE_PVP");
        m.addProperty("toUserId", toUserId);
        try {
            tcp.send(JsonUtil.toJson(m));
            JOptionPane.showMessageDialog(this,
                    "Đã gửi lời mời thách đấu tới " + toName + " (" + toUserId + ").",
                    "Thách đấu", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) { ex.printStackTrace(); }
    }
    private void loadPlayers() {
        var m = new JsonObject(); m.addProperty("type","LIST_PLAYERS");
        try { tcp.send(JsonUtil.toJson(m)); } catch (IOException ex) { ex.printStackTrace(); }
    }
    private void loadLeaderboard() {
        var m = new JsonObject(); m.addProperty("type","GET_LEADERBOARD"); m.addProperty("limit", 50);
        try { tcp.send(JsonUtil.toJson(m)); } catch (IOException ex) { ex.printStackTrace(); }
    }
    private void sendType(String t) {
        var m = new JsonObject(); m.addProperty("type", t);
        try { tcp.send(JsonUtil.toJson(m)); } catch (IOException ex) { ex.printStackTrace(); }
    }

    /** Router: nhận dữ liệu server trả về */
    public void handleLine(String line) {
        try {
            var msg = JsonUtil.fromJson(line, JsonObject.class);
            String type = msg.get("type").getAsString();
            switch (type) {
                case "PLAYERS_LIST" -> {
                    playersModel.setRowCount(0);
                    JsonArray arr = msg.getAsJsonArray("players");
                    for (var el : arr) {
                        var o = el.getAsJsonObject();
                        String pid  = o.get("playerId").getAsString();
                        String name = o.get("nickname").getAsString();
                        String st   = o.get("status").getAsString(); // ONLINE, IN_MATCH, OFFLINE
                        String actionText =
                                pid.equals(myPlayerId) ? "Bạn" :
                                "ONLINE".equals(st)    ? "Thách đấu" :
                                "IN_MATCH".equals(st)  ? "Trong trận" : "Offline";
                        playersModel.addRow(new Object[]{ pid, name, labelStatus(st), actionText });
                    }
                    resizePlayersColumns();
                }
                case "LEADERBOARD" -> {
                    lbModel.setRowCount(0);
                    JsonArray arr = msg.getAsJsonArray("rows");
                    for (var el : arr) {
                        var o = el.getAsJsonObject();
                        lbModel.addRow(new Object[]{
                                o.get("nickname").getAsString(),
                                o.get("totalScore").getAsInt(),
                                o.get("totalWins").getAsInt()
                        });
                    }
                }
                case "HISTORY" -> {
                    if (historyFrame != null && historyFrame.isShowing()) {
                        historyFrame.handleLine(line);
                    }
                }
                case "LOGOUT_OK" -> {
                    JOptionPane.showMessageDialog(this, "Đã đăng xuất!");
                    System.exit(0);
                }
            }
        } catch (Exception ignore) {}
    }

    private static String labelStatus(String st) {
        return switch (st) {
            case "ONLINE"   -> "Online";
            case "IN_MATCH" -> "Trong trận";
            case "OFFLINE"  -> "Offline";
            default -> st;
        };
    }
}
