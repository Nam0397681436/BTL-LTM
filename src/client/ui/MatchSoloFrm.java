package client.ui;


import java.awt.*;
import javax.swing.*;
import com.google.gson.JsonObject;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import client.net.TcpClient;
import model.Player;

public class MatchSoloFrm extends JFrame {
    private JTable pointTable;
    private JButton btnSend, btnExit, btnSkip;
    private JLabel roundLabel, questionLabel, timeLabel;
    private JLabel timeStartRound;
    private JTextField answerField;
    private Player _player;
    private String answerRound;
    private TcpClient client;
    private String[][] dataPoint;
    private String[] columnsNamePoint={"Tên","Điểm"};
    public MatchSoloFrm(TcpClient client) {
        this.client = client;
        dataPoint = new String[][]{
            {"Nguyễn Văn A","10"},
            {"Noname","7"}
        };
        initUI();
    }
    
    public MatchSoloFrm() {
        this.client = null;
        dataPoint = new String[][]{
            {"Nguyễn Văn A","10"},
            {"Noname","7"}
        };
        initUI();
    }
    public void ReceiveMsg(Object obj){
         if (obj instanceof JsonObject msg) {
            String type = msg.get("type").getAsString();
            switch (type) {
                case "QUESTION":
                    String question = msg.get("question").getAsString();
                    String round = String.valueOf(msg.get("round").getAsInt());
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
    
    private void initUI() {
        setTitle("Match Solo");
        setSize(700, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(190, 190, 190));

        // Nút Thoát trận ở góc trên cùng bên trái
        this.btnExit = new JButton("Thoát trận");
        btnExit.setFont(new Font("Arial", Font.PLAIN, 13));
        btnExit.setBackground(new Color(32, 88, 110));
        btnExit.setForeground(Color.WHITE);
        btnExit.addActionListener(new ThoatTranDauListener());

        // Panel chứa nút Thoát trận căn phải trên cùng
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        topPanel.setOpaque(false);
        topPanel.add(btnExit);

        // tạo bảng điểm
        JPanel pointPanel = new JPanel();
        pointPanel.setBackground(new Color(220, 220, 220));
        pointPanel.setLayout(new BorderLayout());
        JLabel pointLabel = new JLabel("Bảng điểm");
    
        pointTable = new JTable(dataPoint, columnsNamePoint);
        pointTable.getColumnModel().getColumn(0).setPreferredWidth(130);
        pointTable.getColumnModel().getColumn(1).setPreferredWidth(70);
        JScrollPane scrollPane = new JScrollPane(pointTable);
        scrollPane.setPreferredSize(new Dimension(200, 150));
        pointLabel.setFont(new Font("Arial", Font.BOLD, 16));
        pointPanel.add(pointLabel, BorderLayout.NORTH);
        pointPanel.add(scrollPane, BorderLayout.CENTER);
        pointPanel.setBounds(20, 20, 200, 200);
        pointTable.setEnabled(false);
        // tạo phần câu hỏi và trả lời
        JPanel questionPanel = new JPanel();
        questionPanel.setBackground(new Color(220, 220, 220));
        questionPanel.setLayout(new BoxLayout(questionPanel, BoxLayout.Y_AXIS));

        roundLabel = new JLabel("Round 1", SwingConstants.CENTER);
        roundLabel.setFont(new Font("Arial", Font.BOLD, 18));
        roundLabel.setOpaque(false);
        roundLabel.setForeground(Color.BLACK);
        roundLabel.setBackground(new Color(32, 88, 110));
        roundLabel.setMaximumSize(new Dimension(220, 40));
        roundLabel.setPreferredSize(new Dimension(220, 40));
        roundLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Label câu hỏi và label thời gian bên phải
        JPanel questionRowPanel = new JPanel();
        questionRowPanel.setLayout(new BoxLayout(questionRowPanel, BoxLayout.X_AXIS));
        questionRowPanel.setBackground(new Color(220, 220, 220));

        // Label câu hỏi
        questionLabel = new JLabel("fsaf", SwingConstants.CENTER);
        questionLabel.setFont(new Font("Arial", Font.BOLD, 18));
        questionLabel.setOpaque(true);
        questionLabel.setForeground(Color.WHITE);
        questionLabel.setBackground(new Color(32, 88, 110));
        questionLabel.setMaximumSize(new Dimension(220, 40));
        questionLabel.setPreferredSize(new Dimension(220, 40));
        questionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

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

        // Thêm label vào panel ngang
        questionRowPanel.add(questionLabel);
        questionRowPanel.add(Box.createHorizontalStrut(20)); // khoảng cách giữa câu hỏi và thời gian
        questionRowPanel.add(timeLabel);

        // Thêm khoảng cách phía trên và giữa 2 label
        questionPanel.add(Box.createVerticalStrut(20)); // Dịch xuống dưới
        questionPanel.add(roundLabel);
        questionPanel.add(Box.createVerticalStrut(10)); // Tách xa nhau (giảm từ 30 xuống 15)
        questionPanel.add(questionRowPanel); // Thay vì questionLabel

        // Thêm ô nhập dữ liệu và nút gửi
        JPanel answerPanel = new JPanel();
        answerPanel.setBackground(new Color(220, 220, 220));
        answerPanel.setLayout(new BoxLayout(answerPanel, BoxLayout.X_AXIS));
        JLabel answerLabel = new JLabel("Câu trả lời: ");
        answerLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        this.answerField = new JTextField();
        answerField.setMaximumSize(new Dimension(150, 30));
        this.btnSend = new JButton("Gửi");
        btnSend.setBackground(new Color(32, 88, 110));
        btnSend.setForeground(Color.WHITE);
        btnSend.setFont(new Font("Arial", Font.BOLD, 14));
        btnSend.addActionListener(e -> {
            // Xử lý khi nhấn nút Gửi
            String answer = answerField.getText();
            if (!answer.trim().isEmpty()) {
                // Gửi câu trả lời tới server (nếu có client)
                if (client != null) {
                    try {
                        client.send("ANSWER:" + answer);
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
        questionPanel.add(Box.createVerticalStrut(20)); // Tách với label trên
        questionPanel.add(answerTimeLabel);             // Thêm label thời gian bên trên
        questionPanel.add(answerPanel);
        
        // gộp 2 panel
        JPanel mainPanel = new JPanel(new BorderLayout());
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
            new MatchSoloFrm().setVisible(true);
        });
    }
}


