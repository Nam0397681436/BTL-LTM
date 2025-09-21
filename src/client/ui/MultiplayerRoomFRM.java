package client.ui;

import javax.swing.*;
import javax.swing.table.*;

import com.google.gson.JsonObject;

import client.JsonUtil;
import client.net.TcpClient;

import java.util.*;
import model.Match;
import model.PlayerMatch;
import server.MatchRooms;
import model.Player;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class MultiplayerRoomFRM extends JFrame implements ActionListener {
    private TcpClient tcp;
    private MainFrame mainFrame;
    private Match match;
    private PlayerMatch host;
    private Player me;
    private JTable playerTable;
    private DefaultTableModel tableModel;
    private JButton inviteButton;
    private JButton startButton;
    private JButton exitButton;
    private FindUserFRM findUserFRM;

    public MultiplayerRoomFRM(Match match, Player me, TcpClient tcp, MainFrame mainFrame) {
        this.match = match;
        this.me = me;
        this.tcp = tcp;
        this.mainFrame = mainFrame;
        host = match.getHost();

        setTitle("Multiplayer Room");
        setSize(600, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // ===== Main Panel =====
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ===== Title =====
        JLabel title = new JLabel("Lobby", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        mainPanel.add(title, BorderLayout.NORTH);

        // ===== Player Table =====
        String[] columnNames = { "No.", "Player Name" };
        tableModel = new DefaultTableModel(columnNames, 0);

        playerTable = new JTable(tableModel);
        playerTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        playerTable.setRowHeight(28);

        // Show grid
        playerTable.setShowGrid(true);
        playerTable.setGridColor(Color.GRAY);

        // ===== Adjust No. column =====
        TableColumn sttColumn = playerTable.getColumnModel().getColumn(0);
        sttColumn.setPreferredWidth(40); // narrow width
        sttColumn.setMaxWidth(50); // cannot expand too wide
        sttColumn.setMinWidth(35); // minimum size
        sttColumn.setResizable(false); // fixed size

        sttColumn.setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus,
                    int row, int column) {
                JLabel cell = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                        column);
                cell.setFont(new Font("Segoe UI", Font.PLAIN, 12)); // smaller font
                cell.setHorizontalAlignment(SwingConstants.CENTER); // center align
                return cell;
            }
        });

        JScrollPane scrollPane = new JScrollPane(playerTable);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // ===== Side Panel with Buttons =====
        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        inviteButton = new JButton("Invite");
        startButton = new JButton("Start");
        exitButton = new JButton("Exit");

        for (JButton btn : new JButton[] { startButton, inviteButton, exitButton }) {
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setMaximumSize(new Dimension(120, 35));
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            if (host != null && host.getPlayer().getPlayerId().equals(me.getPlayerId())) {
                sidePanel.add(btn);
                sidePanel.add(Box.createRigidArea(new Dimension(0, 10)));
            } else if (btn == exitButton) {
                sidePanel.add(btn);
                sidePanel.add(Box.createRigidArea(new Dimension(0, 10)));
            }
            btn.addActionListener(this);
        }

        mainPanel.add(sidePanel, BorderLayout.EAST);

        add(mainPanel);
        updatePlayerList(new ArrayList<>(match.getPlayers()));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source == inviteButton) {
            if (host != null) {
                if (findUserFRM == null || !findUserFRM.isShowing()) {
                    findUserFRM = new FindUserFRM(host.getPlayer(), match, tcp);
                    findUserFRM.setVisible(true);
                } else {
                    findUserFRM.toFront();
                }
            }
        } else if (source == startButton) {
            if (match.getPlayers().size() < 2) {
                JOptionPane.showMessageDialog(this, "At least 2 players are required to start the game.",
                        "Cannot Start Game", JOptionPane.WARNING_MESSAGE);
                return;
            } else {
                var m = new JsonObject();
                m.addProperty("type", "START_GAME");
                m.addProperty("matchId", match.getMatchId());
                try {
                    tcp.send(JsonUtil.toJson(m));
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Failed to send message: " +
                            ex.getMessage(), "Network Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        } else if (source == exitButton) {
            dispose();
            mainFrame.setVisible(true);
            var m = new JsonObject();
            m.addProperty("type", "EXIT_GAME");
            m.addProperty("matchId", match.getMatchId());
            try {
                tcp.send(JsonUtil.toJson(m));
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to send message: " + ex.getMessage(), "Network Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void handleLine(String line) {
        try {
            var msg = JsonUtil.fromJson(line, JsonObject.class);
            String type = msg.get("type").getAsString();
            switch (type) {
                case "EXIT_GAME_ME" -> {
                    dispose();
                    mainFrame.setVisible(true);
                }

                case "EXIT_GAME_HOST" -> {
                    dispose();
                    mainFrame.setVisible(true);
                    JOptionPane.showMessageDialog(this,
                            "The host has exited the game. The room will be closed.",
                            "Room Closed",
                            JOptionPane.INFORMATION_MESSAGE);
                }

                case "EXIT_GAME_GUEST" -> {
                    Match updatedMatch = Match.fromJson(msg.get("match").getAsString());
                    match = updatedMatch;
                    updatePlayerList(new ArrayList<>(updatedMatch.getPlayers()));
                }

                case "MATCH_UPDATED" -> {
                    Match updatedMatch = Match.fromJson(msg.get("match").getAsString());
                    match = updatedMatch;
                    updatePlayerList(new ArrayList<>(updatedMatch.getPlayers()));
                }

                case "INVITE_DECLINED" -> {
                    Player fromPlayer = Player.fromJson(msg.get("fromPlayer").getAsString());
                    showInviteDecline(fromPlayer);
                }

                case "GAME_STARTED" -> {
                    try {
                        String code = msg.get("code").getAsString();
                        var game = new GameWindowMultiplayerFRM(match, tcp, mainFrame);
                        game.setVisible(true);
                        game.startMemoryGame(code, 1);
                        dispose();
                    } catch (Exception e) {
                        System.out.println("ERROR in GAME_STARTED: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception ignore) {
        }
    }

    public void updatePlayerList(ArrayList<PlayerMatch> players) {
        match.setPlayers(players);
        tableModel.setRowCount(0); // Clear existing rows
        for (int i = 0; i < players.size(); i++) {
            PlayerMatch player = players.get(i);
            if (player.isHost()) {
                tableModel.addRow(new Object[] { i + 1, player.getPlayer().getNickname() + " (Host)" });
            } else {
                tableModel.addRow(new Object[] { i + 1, player.getPlayer().getNickname() });
            }
        }
    }

    public void showInviteDecline(Player fromPlayer) {
        JOptionPane.showMessageDialog(this,
                "'" + fromPlayer.getNickname() + "' đã từ chối lời mời chơi của bạn.",
                "Lời mời bị từ chối",
                JOptionPane.WARNING_MESSAGE);
    }
}
