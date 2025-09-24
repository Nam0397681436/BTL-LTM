package client.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import com.google.gson.JsonObject;

import client.ClientApp;
import client.JsonUtil;
import client.net.TcpClient;
import model.PlayerMatch;
import model.RoundResult;
import model.HandelMatchMulti;
import model.Player;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.List;

public class GameWindowMultiplayerFrame extends JFrame implements ActionListener {
    private Player me;
    private TcpClient tcp;
    private MainFrame mainFrame;
    private HandelMatchMulti match;
    private JPanel gamePanel;
    private JLabel roundLabel, scoreLabel, gameLabel, timerLabel;
    private JTextField inputField;
    private JButton submitButton;

    private JTable leaderboardTable;
    private DefaultTableModel leaderboardModel;
    private JButton exitButton;

    private JTextArea chatArea;
    private JTextField chatInput;
    private JButton sendButton;

    private String generatedString;
    private int round = 0;
    private int score = 0;
    private boolean alreadySubmitted = false;
    private int timeLeft;

    private Timer memorizeTimer;
    private Timer inputTimer;
    private Timer resultTimer;

    public GameWindowMultiplayerFrame(HandelMatchMulti match, TcpClient tcp, MainFrame mainFrame, Player me) {
        ClientApp.setMessageHandler(this::handleLine);
        this.match = match;
        this.tcp = tcp;
        this.mainFrame = mainFrame;
        this.me = me;

        setTitle("🎮 Trò chơi nhiều người");
        setSize(950, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // ===== Main Layout =====
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        // ===== Left Panel (Game Area) =====
        gamePanel = new JPanel(new BorderLayout(10, 10));
        gamePanel.setBackground(Color.BLACK);
        gamePanel.setPreferredSize(new Dimension(650, 600));
        gamePanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Round + Score + Timer
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        roundLabel = new JLabel("Vòng: 1", SwingConstants.LEFT);
        roundLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        roundLabel.setForeground(Color.WHITE);

        scoreLabel = new JLabel("Điểm: 0", SwingConstants.CENTER);
        scoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        scoreLabel.setForeground(Color.CYAN);

        timerLabel = new JLabel("Thời gian: --", SwingConstants.RIGHT);
        timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        timerLabel.setForeground(Color.YELLOW);

        topPanel.add(roundLabel, BorderLayout.WEST);
        topPanel.add(scoreLabel, BorderLayout.CENTER);
        topPanel.add(timerLabel, BorderLayout.EAST);

        gamePanel.add(topPanel, BorderLayout.NORTH);

        // Game display (center)
        gameLabel = new JLabel("", SwingConstants.CENTER);
        gameLabel.setForeground(Color.WHITE);
        gameLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        gamePanel.add(gameLabel, BorderLayout.CENTER);

        // Input Panel (bottom)
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        inputField = new JTextField();
        submitButton = new JButton("Xác nhận");
        inputField.setEnabled(false);
        submitButton.setEnabled(false);

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(submitButton, BorderLayout.EAST);

        gamePanel.add(inputPanel, BorderLayout.SOUTH);

        add(gamePanel, BorderLayout.CENTER);

        // ===== Right Panel with Tabs =====
        JTabbedPane rightTabs = new JTabbedPane();
        rightTabs.setPreferredSize(new Dimension(280, 600));

        // --- Tab 1: Leaderboard ---
        JPanel leaderboardPanel = new JPanel(new BorderLayout(5, 5));
        leaderboardPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        String[] columnNames = { "Hạng", "Người chơi", "Điểm" };
        leaderboardModel = new DefaultTableModel(columnNames, 0);

        leaderboardTable = new JTable(leaderboardModel);
        leaderboardTable.setRowHeight(28);
        leaderboardTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        leaderboardTable.setShowGrid(true);
        leaderboardTable.setGridColor(Color.GRAY);

        JScrollPane scrollPane = new JScrollPane(leaderboardTable);
        leaderboardPanel.add(scrollPane, BorderLayout.CENTER);

        exitButton = new JButton("Thoát trận");
        exitButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        exitButton.addActionListener(this);
        JPanel exitPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        exitPanel.add(exitButton);
        leaderboardPanel.add(exitPanel, BorderLayout.SOUTH);

        rightTabs.addTab("Bảng xếp hạng", leaderboardPanel);

        // --- Tab 2: Chat (giữ nguyên nếu sau này muốn bật) ---
        // (Mã chat ban đầu của bạn bị comment — mình giữ comment để không thay đổi logic)
        // JPanel chatPanel = new JPanel(new BorderLayout(5, 5));
        // chatPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        // chatArea = new JTextArea();
        // chatArea.setEditable(false);
        // chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        // JScrollPane chatScroll = new JScrollPane(chatArea);
        // JPanel inputChatPanel = new JPanel(new BorderLayout(5, 5));
        // chatInput = new JTextField();
        // sendButton = new JButton("Send");
        // inputChatPanel.add(chatInput, BorderLayout.CENTER);
        // inputChatPanel.add(sendButton, BorderLayout.EAST);
        // chatPanel.add(chatScroll, BorderLayout.CENTER);
        // chatPanel.add(inputChatPanel, BorderLayout.SOUTH);
        // rightTabs.addTab("Chat", chatPanel);

        add(rightTabs, BorderLayout.EAST);
        updateRanking(match);

        // ===== Đóng cửa sổ: hỏi trước khi rời =====
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int confirm = JOptionPane.showConfirmDialog(
                        GameWindowMultiplayerFrame.this,
                        "Bạn có chắc muốn thoát trận không?",
                        "Xác nhận thoát",
                        JOptionPane.YES_NO_OPTION);

                if (confirm == JOptionPane.YES_OPTION) {
                    try {
                        var m = new JsonObject();
                        m.addProperty("type", "LEAVE_GAME");
                        m.addProperty("matchId", match.getMatchId());
                        tcp.send(JsonUtil.toJson(m));
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        // Đăng ký action cho nút Xác nhận (giữ nguyên cách bạn làm)
        submitButton.addActionListener(e -> checkAnswer());

        // nếu có sendButton kích hoạt (nếu bạn bật chat), cũng giữ logic gửi
        if (sendButton != null) {
            sendButton.addActionListener(this);
        }
    }

    public void handleLine(String line) {
        SwingUtilities.invokeLater(() -> {
            try {
                var msg = JsonUtil.fromJson(line, JsonObject.class);
                String type = msg.get("type").getAsString();
                switch (type) {
                    case "ROUND_RESULT" -> {
                        HandelMatchMulti updatedMatch = HandelMatchMulti.fromJson(msg.get("match").getAsString());
                        var resultsJson = msg.getAsJsonArray("results");
                        List<RoundResult> roundResults = resultsJson != null ? resultsJson
                                .asList()
                                .stream()
                                .map(e -> RoundResult.fromJson(e.getAsString()))
                                .toList()
                                : List.of();
                        showRoundResult(updatedMatch, roundResults);
                    }

                    case "START_NEXT_ROUND" -> {
                        HandelMatchMulti updatedMatch = HandelMatchMulti.fromJson(msg.get("match").getAsString());
                        System.out.println("Received START_NEXT_ROUND for round " + updatedMatch.getCurrentRound());
                        startMemoryGame(updatedMatch);
                    }

                    case "LEAVE_GAME_ME" -> {
                        System.out.println("Player chose to leave the game.");
                        mainFrame.reopen();
                        dispose();
                    }

                    case "LEAVE_GAME_OTHER" -> {
                        HandelMatchMulti updatedMatch = HandelMatchMulti.fromJson(msg.get("match").getAsString());
                        match = updatedMatch;
                        updateRanking(updatedMatch);
                    }

                    case "FINAL_RESULTS" -> {
                        HandelMatchMulti finalMatch = HandelMatchMulti.fromJson(msg.get("match").getAsString());
                        showFinalResults(finalMatch);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        try {
            if (source == sendButton) {
                String input = chatInput.getText().trim();
                if (!input.isEmpty()) {
                    var m = new JsonObject();
                    m.addProperty("type", "SEND_INPUT");
                    m.addProperty("matchId", match.getMatchId());
                    m.addProperty("input", input);
                    chatInput.setText("");
                    tcp.send(JsonUtil.toJson(m));
                }
            } else if (source == exitButton) {
                int confirm = JOptionPane.showConfirmDialog(
                        GameWindowMultiplayerFrame.this,
                        "Bạn có chắc muốn thoát trận không?",
                        "Xác nhận thoát",
                        JOptionPane.YES_NO_OPTION);

                if (confirm == JOptionPane.YES_OPTION) {
                    try {
                        var m = new JsonObject();
                        m.addProperty("type", "LEAVE_GAME");
                        m.addProperty("matchId", match.getMatchId());
                        tcp.send(JsonUtil.toJson(m));
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void startMemoryGame(HandelMatchMulti match) {
        this.match = match;
        this.round = match.getCurrentRound();
        generatedString = match.getQuestionCurrentRound();
        gameLabel.setText(generatedString);
        roundLabel.setText("Vòng: " + round);
        alreadySubmitted = false;
        inputField.setText("");
        // Show phase (the original logic)
        if (memorizeTimer != null)
            memorizeTimer.stop();
        if (inputTimer != null)
            inputTimer.stop();

        timeLeft = match.getTimeShowQuestion();
        updateTimerLabel("Hiển thị");

        memorizeTimer = new Timer(1000, e -> {
            timeLeft--;
            updateTimerLabel("Hiển thị");
            if (timeLeft <= 0) {
                memorizeTimer.stop();
                startInputPhase();
            }
        });
        memorizeTimer.start();
    }

    private void startInputPhase() {
        gameLabel.setText("Hãy nhập lại chuỗi!");
        inputField.setEnabled(true);
        submitButton.setEnabled(true);

        // Stop timers cũ
        if (inputTimer != null)
            inputTimer.stop();

        timeLeft = 7;
        updateTimerLabel("Nhập");

        inputTimer = new Timer(1000, e -> {
            timeLeft--;
            updateTimerLabel("Nhập");
            if (timeLeft <= 0) {
                inputTimer.stop();
                checkAnswer(); // auto check nếu hết giờ
            }
        });
        inputTimer.start();
    }

    private void checkAnswer() {
        if (alreadySubmitted)
            return; // tránh gửi nhiều lần
        alreadySubmitted = true;
        String userInput = inputField.getText().trim().toUpperCase();
        userInput = userInput.isEmpty() ? "" : userInput;
        try {
            var m = new JsonObject();
            m.addProperty("type", "SUBMIT_ANSWER");
            m.addProperty("matchId", match.getMatchId());
            m.addProperty("round", round);
            m.addProperty("answer", userInput);
            tcp.send(JsonUtil.toJson(m));
        } catch (IOException e) {
            e.printStackTrace();
        }
        inputField.setEnabled(false);
        submitButton.setEnabled(false);
    }

    public void showRoundResult(HandelMatchMulti match, List<RoundResult> roundResults) {
        if (resultTimer != null)
            resultTimer.stop();
        if (memorizeTimer != null)
            memorizeTimer.stop();
        this.match = match; // cập nhật match mới nhất
        updateRanking(match);
        this.scoreLabel.setText("Điểm: " + match.getPlayerMatches().stream()
                .filter(pm -> pm.getPlayer().getPlayerId().equals(me.getPlayerId()))
                .findFirst()
                .map(PlayerMatch::getScore)
                .orElse(0));
        // Tạo text hiển thị
        StringBuilder resultText = new StringBuilder();
        resultText.append("Kết quả vòng ").append(round).append(":\n");
        resultText.append("Đáp án đúng: ").append(generatedString).append("\n\n");
        for (RoundResult rr : roundResults) {
            resultText.append(rr.getUser().getNickname())
                    .append(" - Trả lời: ")
                    .append(rr.getUserAnswer() != null ? rr.getUserAnswer() : "Không nhập")
                    .append(" (Điểm: ")
                    .append(rr.getScore())
                    .append(")\n");
        }

        // Tạo JDialog tùy chỉnh
        JDialog dialog = new JDialog(this, "Kết quả vòng", true);
        dialog.setSize(450, 350);
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE); // KHÔNG cho đóng bằng X

        // Vô hiệu hóa Alt+F4 hoặc đóng bằng cửa sổ
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                // Không làm gì
            }
        });

        // Panel chính
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextArea textArea = new JTextArea(resultText.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textArea.setBackground(new Color(240, 240, 240));
        textArea.setMargin(new Insets(10, 10, 10, 10));

        JLabel timerLabel = new JLabel("Đóng sau 5 giây...", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        timerLabel.setForeground(Color.RED);

        panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
        panel.add(timerLabel, BorderLayout.SOUTH);

        dialog.add(panel);

        // Countdown Timer
        final int[] timeLeft = { 5 }; // 5 giây
        resultTimer = new Timer(1000, e -> {
            timeLeft[0]--;
            if (timeLeft[0] <= 0) {
                ((Timer) e.getSource()).stop();
                dialog.dispose();
                if (round < 24) {
                    try {
                        var m = new JsonObject();
                        m.addProperty("type", "REQUEST_NEXT_ROUND");
                        m.addProperty("matchId", match.getMatchId());
                        m.addProperty("currentRound", round);
                        tcp.send(JsonUtil.toJson(m));
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                } else {
                    var m = new JsonObject();
                    m.addProperty("type", "REQUEST_FINAL_RESULTS");
                    m.addProperty("matchId", match.getMatchId());
                    try {
                        tcp.send(JsonUtil.toJson(m));
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            } else {
                timerLabel.setText("Đóng sau " + timeLeft[0] + " giây...");
            }
        });
        resultTimer.start();

        dialog.setVisible(true);
    }

    private void showFinalResults(HandelMatchMulti match) {
        if (resultTimer != null)
            resultTimer.stop();
        if (memorizeTimer != null)
            memorizeTimer.stop();
        String[] columnNames = { "Hạng", "Người chơi", "Điểm", "Trạng thái" };
        List<PlayerMatch> players = match.getPlayerMatches();

        // Sắp xếp theo điểm giảm dần
        players.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));

        Object[][] data = new Object[players.size()][4];
        for (int i = 0; i < players.size(); i++) {
            PlayerMatch p = players.get(i);
            data[i][0] = (i + 1);
            data[i][1] = p.getPlayer().getNickname();
            data[i][2] = p.getScore();
            data[i][3] = p.getStatus();
        }

        JTable table = new JTable(data, columnNames);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));

        JScrollPane scrollPane = new JScrollPane(table);

        int result = JOptionPane.showOptionDialog(this,
                scrollPane,
                "🏆 Kết quả cuối cùng",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                new Object[] { "Đóng" },
                "Đóng");

        if (result == 0 || result == JOptionPane.CLOSED_OPTION) {
            var m = new JsonObject();
            m.addProperty("type", "RELOAD_STATUS_ONLINE");
            try {
                tcp.send(JsonUtil.toJson(m));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            mainFrame.reopen();
            this.dispose();
        }
    }

    public void updateRanking(HandelMatchMulti match) {
        List<PlayerMatch> players = match.getPlayerMatches();
        // Sort theo điểm số giảm dần (cao nhất lên đầu)
        players.sort(null);
        leaderboardModel.setRowCount(0);
        int i = 1; // Clear existing rows
        for (PlayerMatch player : players) {
            leaderboardModel.addRow(
                    new Object[] { i++, player.getPlayer().getUsername(), player.getScore() });
        }
    }

    public void showWaitingForOpponentInput() {
        JOptionPane.showMessageDialog(this,
                "Đang chờ đối thủ nhập...",
                "Thông báo",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateTimerLabel(String phase) {
        timerLabel.setText("Thời gian " + phase + ": " + timeLeft + "s");
    }
}
