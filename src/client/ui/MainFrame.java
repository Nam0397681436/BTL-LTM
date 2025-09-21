package client.ui;

import client.ClientApp;
import client.JsonUtil;
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

    // Bảng người chơi online (3 cột)
    private final DefaultTableModel playersModel =
            new DefaultTableModel(new Object[]{"PlayerId", "Player", "Action"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return c == 2; }
            };
    private JTable tblPlayers;
    private TableRowSorter<DefaultTableModel> sorter;

    // Bảng BXH
    private final DefaultTableModel lbModel =
            new DefaultTableModel(new Object[]{"Nickname", "TotalScore", "Wins"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
    private JTable tblLb;

    private final CardLayout cards = new CardLayout();
    private final JPanel center = new JPanel(cards);

    private final JComboBox<String> cbType = new JComboBox<>(new String[]{"ID", "Nickname"});
    private final JTextField tfQuery = new JTextField(12);

    public MainFrame(TcpClient tcp, String myPlayerId, String nickname) {
        super("Màn hình chính – " + nickname);
        this.tcp = tcp;
        this.myPlayerId = myPlayerId;
        this.nickname = nickname;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1024, 640);
        setLocationRelativeTo(null);

        buildUI();

        // nhận message realtime
        ClientApp.setMessageHandler(this::handleLine);
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
            @Override public boolean isCellEditable(int row, int col) {
                if (col != 2) return false;
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
            String targetId   = String.valueOf(playersModel.getValueAt(modelRow, 0));
            String targetName = String.valueOf(playersModel.getValueAt(modelRow, 1));
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

        // panel trái đẹp
        JPanel left = buildLeftPane();

        var main = new JPanel(new BorderLayout());
        main.add(top, BorderLayout.NORTH);
        main.add(left, BorderLayout.WEST);
        main.add(center, BorderLayout.CENTER);
        setContentPane(main);
        
        
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                try {
                    var m = new com.google.gson.JsonObject();
                    m.addProperty("type", "LOGOUT");
                    tcp.send(client.JsonUtil.toJson(m));
                    try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                } catch (Exception ex) {
                    // ignore
                }
                // để mặc định EXIT_ON_CLOSE sẽ đóng app
            }
        });
        
        // actions
        btnPlayers.addActionListener(e -> { cards.show(center, "players"); loadPlayers(); });
        btnLeaderboard.addActionListener(e -> { cards.show(center, "lb"); loadLeaderboard(); });
        btnLogout.addActionListener(e -> sendType("LOGOUT"));
    }
     

    /** Panel trái đã “làm đẹp” */
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
        btnHistory.addActionListener(e -> JOptionPane.showMessageDialog(this, "HistoryFrame sẽ bổ sung."));
        btnCreate.addActionListener(e -> JOptionPane.showMessageDialog(this, "ROOM_CREATE sẽ bổ sung."));
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
        btnClear.addActionListener(e -> { tfQuery.setText(""); sorter.setRowFilter(null); });

        gc.gridx=0; gc.gridy=0; gc.weightx=0; cardSearch.add(lbType, gc);
        gc.gridx=1; gc.gridy=0; gc.weightx=1; cardSearch.add(cbType, gc);
        gc.gridx=0; gc.gridy=1; gc.gridwidth=2; cardSearch.add(tfQuery, gc);
        gc.gridwidth=1; gc.gridx=0; gc.gridy=2; gc.weightx=0.5; cardSearch.add(btnSearch, gc);
        gc.gridx=1; gc.gridy=2; gc.weightx=0.5; cardSearch.add(btnClear, gc);

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
        try { Icon ico = (Icon) UIManager.get(uiKeyIcon); if (ico != null) b.setIcon(ico); } catch (Exception ignore) {}
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(new Color(210,214,222),1,true),
                BorderFactory.createEmptyBorder(8,12,8,12)));
        b.setBackground(new Color(245,247,250));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton topButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(8,16,8,16));
        return b;
    }

    private JButton dangerButton(String text) {
        JButton b = new JButton(text);
        b.setForeground(new Color(180,35,35));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(8,16,8,16));
        return b;
    }

    private TitledBorder cardBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(220,225,232),1,true),
                title, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
                uiFont().deriveFont(Font.BOLD)
        );
    }

    /** Font an toàn (không null) */
    private Font uiFont() {
        Font f = UIManager.getFont("Label.font");
        if (f == null) f = (new JLabel()).getFont();
        if (f == null) f = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        return f;
    }

    /* ================= Logic gửi/nhận ================= */

    private void applySearch() {
        String q = tfQuery.getText().trim();
        if (q.isEmpty()) { sorter.setRowFilter(null); return; }
        int col = cbType.getSelectedIndex() == 0 ? 0 : 1;
        sorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(q), col));
    }

    private void enableSingleClickButton(JTable table, int actionCol) {
        table.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row < 0 || col != actionCol) return;
                if (!table.isCellEditable(row, col)) return;
                if (table.editCellAt(row, col, e)) {
                    Component editor = table.getEditorComponent();
                    if (editor instanceof JButton b) b.doClick();
                }
            }
        });
    }

    private void invitePvp(String toUserId, String toName) {
        if (toUserId.equals(myPlayerId)) return;
        var m = new JsonObject();
        m.addProperty("type", "INVITE_PVP");
        m.addProperty("toUserId", toUserId);
        try { tcp.send(JsonUtil.toJson(m)); } catch (IOException ex) { ex.printStackTrace(); }
    }

    private void loadPlayers() {
        var m = new JsonObject(); m.addProperty("type", "LIST_PLAYERS");
        try { tcp.send(JsonUtil.toJson(m)); } catch (IOException ex) { ex.printStackTrace(); }
    }

    private void loadLeaderboard() {
        var m = new JsonObject(); m.addProperty("type", "GET_LEADERBOARD"); m.addProperty("limit", 50);
        try { tcp.send(JsonUtil.toJson(m)); } catch (IOException ex) { ex.printStackTrace(); }
    }

    private void sendType(String t) {
        var m = new JsonObject(); m.addProperty("type", t);
        try { tcp.send(JsonUtil.toJson(m)); } catch (IOException ex) { ex.printStackTrace(); }
    }

    /* ================= Router nhận message ================= */
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
                        String pid = o.get("playerId").getAsString();
                        if (pid.equals(myPlayerId)) continue;
                        String name = o.get("nickname").getAsString();
                        playersModel.addRow(new Object[]{pid, name, "Thách đấu"});
                    }
                }
                case "ONLINE_ADD" -> {
                    String pid = msg.get("playerId").getAsString();
                    if (pid.equals(myPlayerId)) break;
                    String name = msg.get("nickname").getAsString();
                    if (findRowById(pid) < 0) playersModel.addRow(new Object[]{pid, name, "Thách đấu"});
                }
                case "ONLINE_REMOVE" -> {
                    int r = findRowById(msg.get("playerId").getAsString());
                    if (r >= 0) playersModel.removeRow(r);
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
                case "LOGOUT_OK" -> {
                    JOptionPane.showMessageDialog(this, "Đã đăng xuất!");
                    System.exit(0);
                }
                case "AUTH_ERR" -> JOptionPane.showMessageDialog(this,
                        msg.get("reason").getAsString(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ignore) {}
    }

    private int findRowById(String playerId) {
        for (int i = 0; i < playersModel.getRowCount(); i++)
            if (playerId.equals(playersModel.getValueAt(i, 0))) return i;
        return -1;
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
        @Override public Component getTableCellRendererComponent(JTable table, Object value,
                                                                 boolean isSelected, boolean hasFocus,
                                                                 int row, int column) {
            setText(value == null ? "" : value.toString());
            return this;
        }
    }

    /** Editor nhớ modelRow để callback không phải lấy từ e.getSource() */
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

        @Override public Object getCellEditorValue() { return btn.getText(); }

        @Override public void actionPerformed(java.awt.event.ActionEvent e) {
            try {
                if (modelRow >= 0) onClick.accept(modelRow);
            } finally {
                fireEditingStopped();
            }
        }
    }
}
