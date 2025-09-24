package client.ui;

import java.awt.BorderLayout;
import java.io.IOException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import com.google.gson.JsonObject;

import client.ClientApp;
import client.JsonUtil;
import client.net.TcpClient;
import model.Match;
import model.Player;

public class FindUserFrame extends JFrame {
    private TcpClient tcp;
    private JTable userTable;
    private DefaultTableModel tableModel;
    private Player currentPlayer;
    private Match match;
    private MultiplayerRoomFrame host;

    public FindUserFrame(Player currentPlayer, Match match, TcpClient tcp, MultiplayerRoomFrame host) {
        // Áp dụng FlatLa

        ClientApp.setMessageHandler(this::handleLine);
        this.currentPlayer = currentPlayer;
        this.match = match;
        this.tcp = tcp;
        this.host = host;

        setTitle("🔍 Tìm người chơi");
        setSize(500, 320);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // ===== Panel tiêu đề =====
        JLabel titleLabel = new JLabel("Danh sách người chơi đang online:", SwingConstants.LEFT);
        titleLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 15));
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // ===== Bảng user =====
        String[] columnNames = { "ID", "Tên hiển thị", "Trạng thái" };
        tableModel = new DefaultTableModel(columnNames, 0);
        userTable = new JTable(tableModel);
        userTable.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14));
        userTable.setRowHeight(26);

        JScrollPane scrollPane = new JScrollPane(userTable);

        // ===== Sự kiện chọn user =====
        userTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = userTable.getSelectedRow();
                if (row >= 0) {
                    String id = String.valueOf(tableModel.getValueAt(row, 0));
                    String status = (String) tableModel.getValueAt(row, 2);
                    System.out.println("Người chơi được chọn ID: " + id + ", trạng thái: " + status);

                    if (status.equalsIgnoreCase("ONLINE")) {
                        JsonObject m = new JsonObject();
                        m.addProperty("type", "INVITE_MATCH_MULTI_USER");
                        m.addProperty("toPlayerId", id);
                        m.addProperty("matchId", match.getMatchId());
                        try {
                            tcp.send(JsonUtil.toJson(m));
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(this,
                                    "Không thể gửi lời mời: " + ex.getMessage(),
                                    "Lỗi mạng",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                        ClientApp.setMessageHandler(host::handleLine);
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "Người chơi này hiện không sẵn sàng để mời.",
                                "Không thể mời",
                                JOptionPane.WARNING_MESSAGE);
                    }
                }
            }
        });

        // ===== Content wrapper =====
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(new EmptyBorder(12, 12, 12, 12));
        wrapper.add(headerPanel, BorderLayout.NORTH);
        wrapper.add(scrollPane, BorderLayout.CENTER);

        add(wrapper);
        requestOnlineUsers();
    }

    private void requestOnlineUsers() {
        try {
            JsonObject m = new JsonObject();
            m.addProperty("type", "GET_ONLINE_USERS");
            tcp.send(JsonUtil.toJson(m));
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Không thể tải danh sách người chơi: " + e.getMessage(),
                    "Lỗi mạng",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void handleLine(String line) {
        try {
            var msg = JsonUtil.fromJson(line, JsonObject.class);
            String type = msg.get("type").getAsString();
            System.out.println("Nhận tin nhắn loại: " + type);

            switch (type) {
                case "ONLINE_LIST" -> {
                    System.out.println("Nhận danh sách ONLINE_LIST");
                    tableModel.setRowCount(0); // xoá bảng cũ
                    var usersJson = msg.getAsJsonArray("rows");
                    for (var userElem : usersJson) {
                        var userObj = userElem.getAsJsonObject();
                        String id = userObj.get("playerId").getAsString();
                        if (id.equals(currentPlayer.getPlayerId()))
                            continue; // bỏ qua chính mình
                        String nickname = userObj.get("nickname").getAsString();
                        String status = userObj.get("status").getAsString();
                        tableModel.addRow(new Object[] { id, nickname, status });
                    }
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
