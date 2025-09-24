package client.ui;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.List;

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
        ClientApp.setMessageHandler(this::handleLine);
        this.currentPlayer = currentPlayer;
        this.match = match;
        this.tcp = tcp;
        this.host = host;

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
                    String id = String.valueOf(tableModel.getValueAt(row, 0));
                    String status = (String) tableModel.getValueAt(row, 2);
                    System.out.println("Selected user ID: " + id + ", status: " + status);
                    if (status.equals("ONLINE")) {
                        JsonObject m = new JsonObject();
                        m.addProperty("type", "INVITE_MATCH_MULTI_USER");
                        m.addProperty("toPlayerId", id);
                        m.addProperty("matchId", match.getMatchId());
                        try {
                            tcp.send(JsonUtil.toJson(m));
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(this, "Failed to send message: " + ex.getMessage(), "Network Error", JOptionPane.ERROR_MESSAGE);
                        }
                        ClientApp.setMessageHandler(host::handleLine);
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
        requestOnlineUsers();
    }

    private void requestOnlineUsers() {
        try {
            JsonObject m = new JsonObject();
            m.addProperty("type", "GET_ONLINE_USERS");
            tcp.send(JsonUtil.toJson(m));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleLine(String line) {
        try {
            var msg = JsonUtil.fromJson(line, JsonObject.class);
            String type = msg.get("type").getAsString();
            System.out.println("Received message of type: " + type);
            switch (type) {
                case "ONLINE_LIST" -> {
                    System.out.println("Received ONLINE_LIST");
                    tableModel.setRowCount(0); // clear bảng
                    var usersJson = msg.getAsJsonArray("rows");
                    for (var userElem : usersJson) {
                        var userObj = userElem.getAsJsonObject();
                        String id = userObj.get("playerId").getAsString();
                        if(id.equals(currentPlayer.getPlayerId()))
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
