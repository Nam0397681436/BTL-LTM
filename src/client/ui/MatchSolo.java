package client.ui;


import java.awt.*;
import javax.swing.*;
import com.google.gson.JsonObject;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import client.ClientApp;
import client.JsonUtil;
import client.net.TcpClient;
import java.io.IOException;
import model.Player;

public class MatchSolo extends JFrame {
    private JTable pointTable;
    private JButton btnSend, btnExit, btnSkip;
    private JLabel roundLabel, questionLabel, timeLabel,timeStartRound, answerTimeLabel;
    private JTextField answerField;
    private Player _player;
    private String answerRound,question,myPlayerId,myNickname,opponentId,opponentName;
    private TcpClient tcp;
    private int timeShowQuestion,timeAnswer;
    private MainFrame mainFrame;
    private String[][] dataPoint;
    private int matchId;
    private int currentRound;
    private String[] columnsNamePoint={"Tên","Điểm"};
    private boolean checkSendAnswer=false;
    private Timer timerTimeout;
    public MatchSolo(TcpClient tcp, String myPlayerId, String myNickname, String opponentId, String opponentName, boolean isHost) {
        this.tcp = tcp;
        this.myPlayerId = myPlayerId;
        this.myNickname = myNickname;
        this.opponentId = opponentId;
        this.opponentName = opponentName;
        this.timeShowQuestion = 5; // Khởi tạo giá trị mặc định
        dataPoint = new String[][]{
            {"You","0"},
            {opponentName,"0"}
        };
        
        // người mờ mới gửi START-GAME-SOLO
        if (isHost) {
            JsonObject startGameMsg= new JsonObject();
            startGameMsg.addProperty("type","START-GAME-SOLO");
            startGameMsg.addProperty("p1Id", myPlayerId);
            startGameMsg.addProperty("p1Nick", myNickname);
            startGameMsg.addProperty("p2Id", opponentId);
            startGameMsg.addProperty("p2Nick", opponentName);
            this.sendMessage(startGameMsg);
        }
        
        initUI();
    }
    
    public void setMainFrame(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }
    
    public void sendMessage(JsonObject msg){
        String result=JsonUtil.toJson(msg);
        try {
            tcp.send(result);
            System.out.println("Gửi message thành công: " + msg.get("type").getAsString());
        } catch (IOException ex) {
            ex.printStackTrace();
            System.out.println("Lỗi kết nối khi gửi message: " + ex.getMessage());
        }
    }
            
    public void handleMessage(String result){
        JsonObject obj= client.JsonUtil.fromJson(result,JsonObject.class);
        System.out.println("MatchSolo.handleMessage() được gọi với: " + obj);
         if (obj != null) {
            String type = obj.get("type").getAsString();
            switch (type) {
                case "QUESTION":
                    System.out.println("Nhận được câu hỏi: " + obj.toString());
                    // Dừng timer cũ trước khi bắt đầu round mới
                    if (timerTimeout != null) {
                        timerTimeout.stop();
                        System.out.println("Đã dừng timer cũ cho round mới");
                    }
                    // Clear các label thời gian
                    timeLabel.setText("");
                    answerTimeLabel.setText("");
                    questionLabel.setText("");
                    
                    // Reset trạng thái cho round mới
                    checkSendAnswer = false;
                    
                    this.question = obj.get("question").getAsString();
                    this.currentRound = obj.get("round").getAsInt();
                    this.timeShowQuestion=obj.get("time").getAsInt();
                    this.matchId = obj.get("matchId").getAsInt();
                    this.timeAnswer=obj.get("timeAnswer").getAsInt()   ;
                    // Hiển thị đếm ngược trước khi hiển thị câu hỏi
                    showCountdownBeforeQuestion(String.valueOf(this.currentRound), question);
                    break;
                case "KETQUA_TRANDAU":
                    JsonObject player1Score = obj.get("player1").getAsJsonObject();
                    JsonObject player2Score = obj.get("player2").getAsJsonObject();
                    String nickname1Score = player1Score.get("Nickname").getAsString();
                    String nickname2Score = player2Score.get("Nickname").getAsString();
                    int score1 = player1Score.get("score").getAsInt();
                    int score2 = player2Score.get("score").getAsInt();   
                    String status1 = player1Score.get("status").getAsString();
                    String status2 = player2Score.get("status").getAsString();
                     // nhay sang giao dien ket qua tran dau
                    SwingUtilities.invokeLater(() -> {
                        // Tạo và hiển thị giao diện kết quả
                        KetQuaMatchSolo ketQuaFrame = new KetQuaMatchSolo(
                            tcp,nickname1Score, score1, status1,nickname2Score, score2, status2, myNickname
                        );
                        ketQuaFrame.setMainFrame(mainFrame);
                        ketQuaFrame.setVisible(true);
                        dispose();
                    });
                    break;
                case "BANG_DIEM":
                    JsonObject player1 = obj.get("player1").getAsJsonObject();
                    JsonObject player2 = obj.get("player2").getAsJsonObject();
                    String nickname1 = player1.get("Nickname").getAsString();
                    String nickname2 = player2.get("Nickname").getAsString();
                    String point1 = player1.get("point").getAsString();
                    String point2 = player2.get("point").getAsString();
                    dataPoint[0][0] = nickname1.equals(myNickname) ? "You" : nickname1;
                    dataPoint[0][1] = point1;
                    dataPoint[1][0] = nickname2.equals(myNickname) ? "You" : nickname2;
                    dataPoint[1][1] = point2;
                    if (timerTimeout != null) {
                        timerTimeout.stop();
                    }
                    // Cập nhật bảng điểm trên UI
                    SwingUtilities.invokeLater(() -> {
                        pointTable.repaint();
                    });

                    break;
                default:
                    break;
            }
        }
    }
    
    private void showCountdownBeforeQuestion(String round, String question) {
        SwingUtilities.invokeLater(() -> {
            roundLabel.setText("Round " + round);
            // checkSendAnswer đã được reset ở trên khi nhận QUESTION message
            
            // Sử dụng lại hàm setTimeDown để đếm ngược 4 giây
            setTimeDownWithText(timeLabel, 4, () -> {
                // Sau khi đếm ngược xong, hiển thị câu hỏi thực tế
                SwingUtilities.invokeLater(() -> {
                    questionLabel.setText(question);
                    System.out.println("Hiển thị câu hỏi: " + question);
                });
                // Bắt đầu timer cho câu hỏi (thời gian để trả lời)
                setTimeDown(timeLabel, timeShowQuestion, () -> {
                    timeLabel.setText("");
                    // Sau khi hết thời gian hiển thị câu hỏi, bắt đầu timer trả lời
                    allowAnswer(true);
                    setTimeDown(answerTimeLabel, timeAnswer, () -> {
                        answerTimeLabel.setText("");
                        // Nếu chưa gửi câu trả lời thì tự động gửi
                        if (!checkSendAnswer) {
                            // Gửi câu trả lời rỗng khi timeout
                            JsonObject timeoutAnswer = new JsonObject();
                            timeoutAnswer.addProperty("type", "ANSWER_PLAYER_SOLO");
                            timeoutAnswer.addProperty("answer", "");
                            timeoutAnswer.addProperty("playerId", myPlayerId);
                            timeoutAnswer.addProperty("opponentId", opponentId);
                            timeoutAnswer.addProperty("matchId", matchId);
                            timeoutAnswer.addProperty("round", currentRound);
                            checkSendAnswer = true; 
                            sendMessage(timeoutAnswer);
                        }
                        allowAnswer(false);
                    }, "answer");
                }, "question");
            }, "Câu hỏi vòng " + round + " sẽ hiển thị sau ");
        });
    }
    
    // Hàm mới để đếm ngược với text
    public void setTimeDownWithText(JLabel timeLabel, int second, Runnable onFinish, String prefixText) {
        final int[] timeDown = {second};
        Timer t = new Timer(1000, e -> {
            timeDown[0] = Math.max(0, timeDown[0] - 1);
            
            if (timeDown[0] > 0) {
                SwingUtilities.invokeLater(() -> {
                    timeLabel.setText(timeDown[0] + "s");
                    questionLabel.setText(prefixText + timeDown[0] + " giây");
                });
            } else {
                SwingUtilities.invokeLater(() -> {
                    timeLabel.setText("");
                });
                ((Timer) e.getSource()).stop();
                onFinish.run();
            }
        });
        t.setInitialDelay(0);
        t.start();
    }
    
    private void initUI() {
        setTitle("Match Solo"+myPlayerId);
        setSize(1024, 640);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(245, 247, 250));
        
        // Thêm WindowListener để xử lý việc đóng cửa sổ
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleWindowClosing();
            }
        });

        // Nút Thoát trận ở góc trên cùng bên trái
        this.btnExit = new JButton("Thoát trận");
        btnExit.setFont(new Font("Arial", Font.BOLD, 13));
        btnExit.setBackground(new Color(32, 88, 110));
        btnExit.setForeground(Color.WHITE);
        btnExit.setFocusPainted(false);
        btnExit.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnExit.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        btnExit.addActionListener(new ThoatTranDauListener());

        // Panel chứa nút Thoát trận căn phải trên cùng
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 0, 12));
        topPanel.add(btnExit);

        // tạo bảng điểm
        JPanel pointPanel = new JPanel();
        pointPanel.setBackground(new Color(255, 255, 255));
        pointPanel.setLayout(new BorderLayout(8, 8));
        pointPanel.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(new Color(220, 225, 232), 1, true),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        JLabel pointLabel = new JLabel("Bảng điểm");
        pointLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        pointTable = new JTable(dataPoint, columnsNamePoint);
        pointTable.setFillsViewportHeight(true);
        pointTable.setRowHeight(28);
        pointTable.setShowGrid(true);
        pointTable.setGridColor(new Color(230, 234, 240));
        pointTable.getColumnModel().getColumn(0).setPreferredWidth(130);
        pointTable.getColumnModel().getColumn(1).setPreferredWidth(70);
        JScrollPane scrollPane = new JScrollPane(pointTable);
        scrollPane.setPreferredSize(new Dimension(240, 160));
        pointLabel.setFont(new Font("Arial", Font.BOLD, 16));
        pointPanel.add(pointLabel, BorderLayout.NORTH);
        pointPanel.add(scrollPane, BorderLayout.CENTER);
        pointPanel.setBounds(20, 20, 200, 200);
        pointTable.setEnabled(false);
        // tạo phần câu hỏi và trả lời
        JPanel questionPanel = new JPanel();
        questionPanel.setBackground(new Color(255, 255, 255));
        questionPanel.setLayout(new BoxLayout(questionPanel, BoxLayout.Y_AXIS));
        questionPanel.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(new Color(220, 225, 232), 1, true),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));

        roundLabel = new JLabel(String.valueOf(this.currentRound), SwingConstants.CENTER);
        roundLabel.setFont(new Font("Arial", Font.BOLD, 18));
        roundLabel.setOpaque(true);
        roundLabel.setForeground(new Color(32, 88, 110));
        roundLabel.setBackground(new Color(240, 246, 250));
        roundLabel.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(new Color(210, 220, 230), 1, true),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        roundLabel.setMaximumSize(new Dimension(260, 38));
        roundLabel.setPreferredSize(new Dimension(260, 38));
        roundLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Label câu hỏi và label thời gian bên phải
        JPanel questionRowPanel = new JPanel(new GridBagLayout());
        questionRowPanel.setBackground(new Color(255, 255, 255));
        questionRowPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        GridBagConstraints qgc = new GridBagConstraints();
        qgc.insets = new Insets(0, 0, 0, 12);
        qgc.fill = GridBagConstraints.HORIZONTAL;
        qgc.weightx = 1.0;

        // Label câu hỏi
        questionLabel = new JLabel("", SwingConstants.CENTER);
        questionLabel.setFont(new Font("Arial", Font.BOLD, 18));
        questionLabel.setOpaque(true);
        questionLabel.setForeground(Color.WHITE);
        questionLabel.setBackground(new Color(32, 88, 110));
        questionLabel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        questionLabel.setMaximumSize(new Dimension(Short.MAX_VALUE, 44));
        questionLabel.setPreferredSize(new Dimension(520, 44));
        questionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        questionRowPanel.add(questionLabel, qgc);

        // Label thời gian
        this.timeLabel = new JLabel("5s", SwingConstants.CENTER);
        timeLabel.setFont(new Font("Arial", Font.BOLD, 16));
        timeLabel.setOpaque(true);
        timeLabel.setForeground(Color.WHITE);
        timeLabel.setBackground(new Color(32, 88, 110));
        timeLabel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
        timeLabel.setMaximumSize(new Dimension(60, 40));
        timeLabel.setPreferredSize(new Dimension(60, 40));
        timeLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        GridBagConstraints tgc = new GridBagConstraints();
        tgc.insets = new Insets(0, 0, 0, 0);
        tgc.fill = GridBagConstraints.NONE;
        tgc.anchor = GridBagConstraints.EAST;
        tgc.weightx = 0;
        questionRowPanel.add(timeLabel, tgc);

        // Thêm khoảng cách phía trên và giữa 2 label
        questionPanel.add(Box.createVerticalStrut(20)); // Dịch xuống dưới
        questionPanel.add(roundLabel);
        questionPanel.add(Box.createVerticalStrut(10)); // Tách xa nhau (giảm từ 30 xuống 15)
        questionPanel.add(questionRowPanel); // Thay vì questionLabel

        // Thêm ô nhập dữ liệu và nút gửi
        JPanel answerPanel = new JPanel(new GridBagLayout());
        answerPanel.setBackground(new Color(255, 255, 255));
        answerPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        JLabel answerLabel = new JLabel("Câu trả lời: ");
        answerLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        this.answerField = new JTextField();
        answerField.setPreferredSize(new Dimension(360, 32));
        answerField.setMaximumSize(new Dimension(360, 32));
        this.btnSend = new JButton("Gửi");
        btnSend.setBackground(new Color(32, 88, 110));
        btnSend.setForeground(Color.WHITE);
        btnSend.setFont(new Font("Arial", Font.BOLD, 14));
        btnSend.setFocusPainted(false);
        btnSend.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        btnSend.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        GridBagConstraints agc = new GridBagConstraints();
        agc.insets = new Insets(0, 0, 0, 8);
        agc.gridx = 0; agc.gridy = 0; agc.weightx = 0; agc.fill = GridBagConstraints.NONE;
        answerPanel.add(answerLabel, agc);
        agc.insets = new Insets(0, 0, 0, 8);
        agc.gridx = 1; agc.gridy = 0; agc.weightx = 1; agc.fill = GridBagConstraints.HORIZONTAL;
        answerPanel.add(answerField, agc);
        agc.insets = new Insets(0, 0, 0, 0);
        agc.gridx = 2; agc.gridy = 0; agc.weightx = 0; agc.fill = GridBagConstraints.NONE;
        answerPanel.add(btnSend, agc);

        btnSend.addActionListener(e -> {
            // Xử lý khi nhấn nút Gửi
            String answer = answerField.getText();
            // Cho phép gửi cả câu trả lời rỗng
            if (tcp != null) {
                JsonObject answerMsg = new JsonObject();
                answerMsg.addProperty("type", "ANSWER_PLAYER_SOLO");
                answerMsg.addProperty("answer", answer);
                answerMsg.addProperty("playerId", myPlayerId);
                answerMsg.addProperty("opponentId", opponentId);
                answerMsg.addProperty("matchId", matchId);
                answerMsg.addProperty("round", currentRound);
                checkSendAnswer = true; 
                sendMessage(answerMsg);                                                  
            }           
            answerField.setText("");
            allowAnswer(false);                
        });
        
        // Khởi tạo trạng thái ban đầu
        allowAnswer(false);
        answerPanel.add(answerLabel);
        answerPanel.add(answerField);
        answerPanel.add(Box.createHorizontalStrut(10));
        answerPanel.add(btnSend);
        // không cho nhập và gửi lúc kí tự xuất hiện

        // Label thời gian bên trên ô nhập
        this.answerTimeLabel = new JLabel("", SwingConstants.CENTER);
        answerTimeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        answerTimeLabel.setOpaque(true);
        answerTimeLabel.setForeground(Color.WHITE);
        answerTimeLabel.setBackground(new Color(32, 88, 110));
        answerTimeLabel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
        answerTimeLabel.setMaximumSize(new Dimension(50, 30));
        answerTimeLabel.setPreferredSize(new Dimension(50, 30));
        answerTimeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Timer sẽ được bắt đầu khi nhận câu hỏi từ server
        // Thêm vào questionPanel
        questionPanel.add(Box.createVerticalStrut(16));
        questionPanel.add(answerTimeLabel);
        questionPanel.add(answerPanel);
        
        // gộp 2 panel
        JPanel mainPanel = new JPanel(new BorderLayout(16, 0));
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        mainPanel.add(pointPanel, BorderLayout.WEST);
        mainPanel.add(questionPanel, BorderLayout.CENTER);

        // Thêm topPanel vào phía trên cùng của frame
        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
    }
    private void allowAnswer(boolean allow){
        answerField.setEditable(allow);
        btnSend.setEnabled(allow);

    }
    public void setTimeDown(JLabel timeLabel,int second, Runnable onFinish,String type){
        final int [] timeDown={second};
        timerTimeout = new Timer(1000,e ->{
             timeDown[0]=Math.max(0,timeDown[0]-1);
             timeLabel.setText(String.valueOf(timeDown[0]));
             if(timeDown[0]==0){
                ((javax.swing.Timer) e.getSource()).stop();        
                onFinish.run(); 
                if(type.equals("answer")){
                    answerRound=answerField.getText();           
                    answerField.setText("");
                }
                else if(type.equals("question")){          
                    questionLabel.setText("");
                }          
             }
        });
        timerTimeout.setInitialDelay(0);
        timerTimeout.start();
    }
    
    private void handleWindowClosing() {
        int choice = JOptionPane.showOptionDialog(
            this,
            "Bạn có chắc chắn muốn đóng cửa sổ không?\nTrận đấu sẽ tiếp tục nếu bạn đóng cửa sổ",
            "Đóng cửa sổ",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            new Object[]{"Đóng cửa sổ", "Quay lại"},
            "Quay lại"
        );
        
        if (choice == JOptionPane.YES_OPTION) { 
            JsonObject leaveMsg = new JsonObject();
            leaveMsg.addProperty("type", "EXIT_MATCH_SOLO");
            leaveMsg.addProperty("playerId", opponentId);
            leaveMsg.addProperty("playerExited", myPlayerId);
            leaveMsg.addProperty("matchId", matchId);
            sendMessage(leaveMsg);
            // Đóng cửa sổ MatchSolo
            dispose();
        }
    }
    
    public class ThoatTranDauListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            int choice = JOptionPane.showOptionDialog(
            MatchSolo.this,
            "Bạn có chắc chắn muốn thoát trận không?\nTrận đấu sẽ tiếp tục nếu bạn thoát trận",
            "Thoát trận",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            new Object[]{"Thoát trận", "Quay lại"},
            "Quay lại"
        );
            if (choice == JOptionPane.YES_OPTION) {
                JsonObject exitMsg = new JsonObject();
                exitMsg.addProperty("type", "LEAVE_MATCH_SOLO");
                exitMsg.addProperty("playerLeaved", myPlayerId);
                exitMsg.addProperty("playerId", opponentId);
                exitMsg.addProperty("matchId", matchId);
                sendMessage(exitMsg);           
            }
        }
    }
}


