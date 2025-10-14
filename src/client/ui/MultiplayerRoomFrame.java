package client.ui;

import javax.swing.*;
import javax.swing.table.*;
import com.google.gson.JsonObject;

import client.ClientApp;
import client.JsonUtil;
import client.net.TcpClient;

import java.util.*;
import model.PlayerMatch;
import model.HandelMatchMulti;
import model.Player;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class MultiplayerRoomFrame extends JFrame implements ActionListener {
    private TcpClient tcp;
    private MainFrame mainFrame;
    private HandelMatchMulti match;
    private Player host;
    private Player me;
    private JTable playerTable;
    private DefaultTableModel tableModel;
    private JButton inviteButton;
    private JButton startButton;
    private JButton exitButton;
    private FindUserFrame findUserFrame;

    public MultiplayerRoomFrame(HandelMatchMulti match, Player me, TcpClient tcp, MainFrame mainFrame) {

        ClientApp.setMessageHandler(this::handleLine);
        this.match = match;
        this.me = me;
        this.tcp = tcp;
        this.mainFrame = mainFrame;
        host = match.getHost();

        setTitle("🎮 Phòng chơi nhiều người");
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));


        JLabel title = new JLabel("Phòng chờ", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        mainPanel.add(title, BorderLayout.NORTH);

        String[] columnNames = { "STT", "Người chơi" };
        tableModel = new DefaultTableModel(columnNames, 0);

        playerTable = new JTable(tableModel);
        playerTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        playerTable.setRowHeight(30);
        playerTable.setFillsViewportHeight(true);
        playerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JTableHeader header = playerTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        ((DefaultTableCellRenderer) header.getDefaultRenderer())
                .setHorizontalAlignment(SwingConstants.CENTER);

        TableColumn sttColumn = playerTable.getColumnModel().getColumn(0);
        sttColumn.setPreferredWidth(50);
        sttColumn.setMaxWidth(60);
        sttColumn.setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel cell = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                        column);
                cell.setHorizontalAlignment(SwingConstants.CENTER);
                return cell;
            }
        });

        JScrollPane scrollPane = new JScrollPane(playerTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));

        inviteButton = new JButton("Mời");
        startButton = new JButton("Bắt đầu");
        exitButton = new JButton("Thoát");

        for (JButton btn : new JButton[] { startButton, inviteButton, exitButton }) {
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setMaximumSize(new Dimension(140, 40));
            btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            if (host != null && host.getPlayerId().equals(me.getPlayerId())) {
                sidePanel.add(btn);
                sidePanel.add(Box.createRigidArea(new Dimension(0, 12)));
            } else if (btn == exitButton) {
                sidePanel.add(btn);
                sidePanel.add(Box.createRigidArea(new Dimension(0, 12)));
            }
            btn.addActionListener(this);
        }

        mainPanel.add(sidePanel, BorderLayout.EAST);

        add(mainPanel);
        updatePlayerList(new ArrayList<>(match.getPlayerMatches()));

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                exitRoom();
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source == inviteButton) {
            if (host != null) {
                if (findUserFrame == null || !findUserFrame.isShowing()) {
                    findUserFrame = new FindUserFrame(host, match, tcp, this);
                    findUserFrame.setVisible(true);
                } else {
                    findUserFrame.toFront();
                }
            }
        } else if (source == startButton) {
            if (match.getPlayerMatches().size() < 2) {
                JOptionPane.showMessageDialog(this,
                        "Cần ít nhất 2 người chơi để bắt đầu trò chơi.",
                        "Không thể bắt đầu",
                        JOptionPane.WARNING_MESSAGE);
                return;
            } else {
                var m = new JsonObject();
                m.addProperty("type", "START_GAME_MULTIPLE");
                m.addProperty("matchId", match.getMatchId());
                try {
                    tcp.send(JsonUtil.toJson(m));
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this,
                            "Gửi lệnh thất bại: " + ex.getMessage(),
                            "Lỗi mạng",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        } else if (source == exitButton) {
            System.out.println("Exiting room...");
            exitRoom();
        }
    }

    private void exitRoom() {
        var m = new JsonObject();
        m.addProperty("type", "EXIT_ROOM_MULTIPLE");
        m.addProperty("matchId", match.getMatchId());
        try {
            tcp.send(JsonUtil.toJson(m));
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Không thể thoát phòng: " + ex.getMessage(),
                    "Lỗi mạng",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void handleLine(String line) {
        try {
            var msg = JsonUtil.fromJson(line, JsonObject.class);
            String type = msg.get("type").getAsString();
            switch (type) {
                case "EXIT_ROOM_MULTIPLE_ME" -> {
                    mainFrame.reopen();
                    dispose();
                }
                case "EXIT_ROOM_MULTIPLE_HOST" -> {
                    JOptionPane.showMessageDialog(this,
                            "Chủ phòng đã thoát. Phòng sẽ bị đóng.",
                            "Phòng đã đóng",
                            JOptionPane.INFORMATION_MESSAGE);
                    mainFrame.reopen();
                    dispose();
                }
                case "EXIT_ROOM_MULTIPLE_GUEST" -> {
                    HandelMatchMulti updatedMatch = HandelMatchMulti.fromJson(msg.get("match").getAsString());
                    match = updatedMatch;
                    updatePlayerList(new ArrayList<>(updatedMatch.getPlayerMatches()));
                }
                case "ACCEPT_MULTIPLE_USERS_MATCH_INVITE" -> {
                    HandelMatchMulti updatedMatch = HandelMatchMulti.fromJson(msg.get("match").getAsString());
                    this.match = updatedMatch;
                    updatePlayerList(new ArrayList<>(updatedMatch.getPlayerMatches()));
                }
                case "DECLINE_MULTIPLE_USERS_MATCH_INVITED" -> {
                    Player fromPlayer = Player.fromJson(msg.get("fromPlayer").getAsString());
                    showInviteDecline(fromPlayer);
                }
                case "GAME_MULTIPLE_STARTED" -> {
                    try {
                        HandelMatchMulti match = HandelMatchMulti.fromJson(msg.get("match").getAsString());
                        var game = new GameWindowMultiplayerFrame(match, tcp, mainFrame, me);
                        game.setVisible(true);
                        game.startMemoryGame(match);
                        dispose();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception ignore) {}
    }

    public void updatePlayerList(ArrayList<PlayerMatch> players) {
        match.setPlayerMatches(players);
        tableModel.setRowCount(0);
        for (int i = 0; i < players.size(); i++) {
            PlayerMatch player = players.get(i);
            if (player.getPlayer().getPlayerId().equals(host.getPlayerId())) {
                tableModel.addRow(new Object[] { i + 1, player.getPlayer().getNickname() + " (Chủ phòng)" });
            } else {
                tableModel.addRow(new Object[] { i + 1, player.getPlayer().getNickname() });
            }
        }
    }

    public void showInviteDecline(Player fromPlayer) {
        JOptionPane.showMessageDialog(this,
                "'" + fromPlayer.getNickname() + "' đã từ chối lời mời của bạn.",
                "Lời mời bị từ chối",
                JOptionPane.WARNING_MESSAGE);
    }
}
