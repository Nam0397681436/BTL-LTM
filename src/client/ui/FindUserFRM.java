package client.ui;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.List;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import com.google.gson.JsonObject;

import client.JsonUtil;
import client.net.TcpClient;
import model.Match;
import model.Player;
import server.OnlineRegistry;

public class FindUserFRM extends JFrame {
    private TcpClient tcp;
    private JTable userTable;
    private DefaultTableModel tableModel;
    private Player currentPlayer;
    private Match match;

    public FindUserFRM(Player currentPlayer, Match match, TcpClient tcp) {
        this.currentPlayer = currentPlayer;
        this.match = match;
        this.tcp = tcp;

        setTitle("Find User");
        setSize(500, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Panel tiêu đề
        JLabel titleLabel = new JLabel("List user online:", SwingConstants.LEFT);
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // Bảng user
        String[] columnNames = { "ID", "Nickname", "Status" };
        tableModel = new DefaultTableModel(columnNames, 0);
        userTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(userTable);

        // Khi chọn user -> callback
        userTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = userTable.getSelectedRow();
                if (row >= 0) {
                    int id = (int) tableModel.getValueAt(row, 0);
                    String nickname = (String) tableModel.getValueAt(row, 1);
                    String status = (String) tableModel.getValueAt(row, 2);
                    if (status.equals("ONLINE")) {
                        JsonObject m = new JsonObject();
                        m.addProperty("type", "INVITE_USER");
                        m.addProperty("toPlayerId", id);
                        m.addProperty("matchId", match.getMatchId());
                        try {
                            tcp.send(JsonUtil.toJson(m));
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(this, "Failed to send message: " + ex.getMessage(), "Network Error", JOptionPane.ERROR_MESSAGE);
                        }
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(this, "User is not available for invite.");
                    }
                }
            }
        });

        // Content wrapper
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(new EmptyBorder(10, 10, 10, 10));
        wrapper.add(headerPanel, BorderLayout.NORTH);
        wrapper.add(scrollPane, BorderLayout.CENTER);

        add(wrapper);
        // Đăng ký listener từ Client
        // Client.addOnlineUserListener(players -> {
        //     SwingUtilities.invokeLater(() -> updateUserList(players));
        // });

        // Gửi yêu cầu lên server
        requestOnlineUsers();
    }

    private void requestOnlineUsers() {
        try {
            JsonObject m = new JsonObject();
            m.addProperty("type", "GET_ONLINE_USERS");
            tcp.send(JsonUtil.toJson(m));
            // Server sẽ response với danh sách user online
            // updateUserList sẽ được gọi khi nhận response từ server
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleLine(String line) {
        try {
            var msg = JsonUtil.fromJson(line, JsonObject.class);
            String type = msg.get("type").getAsString();
            switch (type) {
                case "ONLINE_USERS" -> {
                    tableModel.setRowCount(0); // clear bảng
                    var usersJson = msg.getAsJsonArray("players");
                    for (var userElem : usersJson) {
                        var userObj = userElem.getAsJsonObject();
                        int id = userObj.get("playerId").getAsInt();
                        if(id == Integer.parseInt(currentPlayer.getPlayerId()))
                            continue;
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
