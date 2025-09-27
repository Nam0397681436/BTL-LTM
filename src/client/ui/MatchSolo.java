package client.ui;


import java.awt.*;
import javax.swing.*;
import com.google.gson.JsonObject;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import client.net.TcpClient;
import model.Player;

public class MatchSolo extends JFrame {
    private JTable pointTable;
    private JButton btnSend, btnExit, btnSkip;
    private JLabel roundLabel, questionLabel, timeLabel;
    private JLabel timeStartRound;
    private JTextField answerField;
    private Player _player;
    private String answerRound;
    private TcpClient tcp;
    private int timeShowQuestion;
    private String myPlayerId,myNickname,opponentId,opponentName;
    private MainFrame mainFrame;
    private String[][] dataPoint;
    private String[] columnsNamePoint={"Tên","Điểm"};
    public MatchSolo(TcpClient tcp, String myPlayerId, String myNickname, String opponentId, String opponentName) {
        this.tcp = tcp;
        this.myPlayerId = myPlayerId;
        this.myNickname = myNickname;
        this.opponentId = opponentId;
        this.opponentName = opponentName;
        dataPoint = new String[][]{
            {"Nguyễn Văn A","10"},
            {"Noname","7"}
        };
        initUI();
    }
 
    public MatchSolo() {
        this.tcp = null;
        dataPoint = new String[][]{
            {"Nguyễn Văn A","10"},
            {"Noname","7"}
        };
        initUI();
    }
    public void handleMessage(Object obj){
         if (obj instanceof JsonObject msg) {
            String type = msg.get("type").getAsString();
            switch (type) {
                case "QUESTION":
                    String question = msg.get("question").getAsString();
                    String round = String.valueOf(msg.get("round").getAsInt());
                    this.timeShowQuestion=msg.get("time").getAsInt();                    
                    this.timeStartRound= new JLabel("5s", SwingConstants.CENTER);
                    setTimeDown(timeStartRound,5, ()->{
                        timeStartRound.setText("");
                        questionLabel.setText(question);
                        roundLabel.setText("Round " + round);
                    },"startRound");
                    break;
                case "BXH":
                    String answer = msg.get("answer").getAsString();
                    answerField.setText(answer);
                    break;
                case "BANG_DIEM":
                    JsonObject player1 = msg.get("player1").getAsJsonObject();
                    JsonObject player2 = msg.get("player2").getAsJsonObject();
                    String nickname1 = player1.get("Nickname").getAsString();
                    String nickname2 = player2.get("Nickname").getAsString();
                    String point1 = player1.get("point").getAsString();
                    String point2 = player2.get("point").getAsString();
                    dataPoint[0][0] = nickname1;
                    dataPoint[0][1] = point1;
                    dataPoint[1][0] = nickname2;
                    dataPoint[1][1] = point2;
                    break;
                default:
                    break;
            }
        }
    }
    public void setMainFrame(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }
    
    private void initUI() {
        setTitle("Match Solo");
        setSize(1024, 640);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(245, 247, 250));

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

        roundLabel = new JLabel("Round 1", SwingConstants.CENTER);
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
            if (!answer.trim().isEmpty()) {
                // Gửi câu trả lời tới server (nếu có client)
                if (tcp != null) {
                    try {
                        tcp.send("ANSWER:" + answer);
                    } catch (java.io.IOException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(this,
                            "Lỗi gửi câu trả lời: " + ex.getMessage(),
                            "Lỗi mạng", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    // Test mode: chỉ hiển thị câu trả lời
                    System.out.println("Câu trả lời: " + answer);
                }
                answerField.setText("");
                allowAnswer(false);
            }
        });
        
        // Khởi tạo trạng thái ban đầu
        allowAnswer(false);
        answerPanel.add(answerLabel);
        answerPanel.add(answerField);
        answerPanel.add(Box.createHorizontalStrut(10));
        answerPanel.add(btnSend);
        // không cho nhập và gửi lúc kí tự xuất hiện

        // Label thời gian bên trên ô nhập
        JLabel answerTimeLabel = new JLabel("", SwingConstants.CENTER);
        answerTimeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        answerTimeLabel.setOpaque(true);
        answerTimeLabel.setForeground(Color.WHITE);
        answerTimeLabel.setBackground(new Color(32, 88, 110));
        answerTimeLabel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
        answerTimeLabel.setMaximumSize(new Dimension(50, 30));
        answerTimeLabel.setPreferredSize(new Dimension(50, 30));
        answerTimeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        setTimeDown(timeLabel,7, ()->{
            timeLabel.setText("");
            setTimeDown(answerTimeLabel,7, ()->answerTimeLabel.setText(""),"answer");
            allowAnswer(true);
        },"question");
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
        Timer t = new Timer(1000,e ->{
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
        t.setInitialDelay(0);
        t.start();
    }
    public class ThoatTranDauListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            // Tạm thời không làm gì cả
            int choice=JOptionPane.showOptionDialog(
                 null,
                 "Bạn có chắc chắn muốn thoát trận không?\n Nếu đồng ý, bạn sẽ thua trận",
                 "Thoát trận",
                 JOptionPane.YES_NO_OPTION,
                 JOptionPane.QUESTION_MESSAGE,
                 null,
                 new Object[]{"Thoát trận","Quay lại"},
                 "Quay lại"
               );
            if(choice==JOptionPane.YES_OPTION){
                //client.send("EXIT_MATCH");
            }    
            System.out.println("Nút Thoát trận được nhấn - chưa xử lý");
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Chạy không cần client
            //new MatchSoloFrm().setVisible(true);
        });
    }
}


