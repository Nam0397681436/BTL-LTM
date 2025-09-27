package client.ui;

import client.net.TcpClient;
import client.JsonUtil;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class MatchSolo extends JFrame {
    private final TcpClient tcp;
    private final String myPlayerId;
    private final String myNickname;
    private final String opponentId;
    private final String opponentName;
    private MainFrame mainFrame;

    public MatchSolo(TcpClient tcp, String myPlayerId, String myNickname, String opponentId, String opponentName) {
        super("Phòng Solo - " + myNickname + " vs " + opponentName);
        this.tcp = tcp;
        this.myPlayerId = myPlayerId;
        this.myNickname = myNickname;
        this.opponentId = opponentId;
        this.opponentName = opponentName;

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        buildUI();
        setupWindowListener();
    }

    private void buildUI() {
        // Tạo giao diện trắng trơn hoàn toàn
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // Khu vực chính hoàn toàn trắng
        JPanel gameArea = new JPanel(new BorderLayout());
        gameArea.setBackground(Color.WHITE);

        // Không có gì cả - chỉ là màu trắng
        setContentPane(mainPanel);
    }

    private void setupWindowListener() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                backToMainFrame();
            }
        });
    }

    private void backToMainFrame() {
        // Gửi thông báo rời phòng
        var m = new JsonObject();
        m.addProperty("type", "LEAVE_MATCH");
        m.addProperty("opponentId", opponentId);
        try { 
            tcp.send(JsonUtil.toJson(m)); 
        } catch (IOException ex) { 
            ex.printStackTrace(); 
        }

        // Quay lại MainFrame
        if (mainFrame != null) {
            client.ClientApp.setMessageHandler(mainFrame::handleLine);
            mainFrame.reopen();
        }
        this.dispose();
    }

    private void surrender() {
        int result = JOptionPane.showConfirmDialog(this, 
                "Bạn có chắc chắn muốn đầu hàng?", 
                "Xác nhận đầu hàng", 
                JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            // Gửi thông báo đầu hàng
            var m = new JsonObject();
            m.addProperty("type", "SURRENDER");
            m.addProperty("opponentId", opponentId);
            try { 
                tcp.send(JsonUtil.toJson(m)); 
            } catch (IOException ex) { 
                ex.printStackTrace(); 
            }
            
            backToMainFrame();
        }
    }

    public void setMainFrame(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }
    
    // Xử lý message từ server
    public void handleMessage(com.google.gson.JsonObject msg) {
        String type = msg.get("type").getAsString();
        switch (type) {
            case "START-GAME": {
                // Khởi tạo trận đấu solo
                String p1Id = msg.get("p1Id").getAsString();
                String p1Nick = msg.get("p1Nick").getAsString();
                String p2Id = msg.get("p2Id").getAsString();
                String p2Nick = msg.get("p2Nick").getAsString();
                startGame(p1Id, p1Nick, p2Id, p2Nick);
                break;
            }

            case "LEFT_MATCH": {
                JOptionPane.showMessageDialog(this, 
                    "Bạn đã rời phòng đấu.", 
                    "Rời phòng", 
                    JOptionPane.INFORMATION_MESSAGE);
                backToMainFrame();
                break;
            }

            case "SURRENDERED": {
                JOptionPane.showMessageDialog(this, 
                    "Bạn đã đầu hàng.", 
                    "Đầu hàng", 
                    JOptionPane.INFORMATION_MESSAGE);
                backToMainFrame();
                break;
            }

            case "OPPONENT_LEFT": {
                String playerName = msg.get("playerName").getAsString();
                JOptionPane.showMessageDialog(this, 
                    playerName + " đã rời phòng đấu. Bạn thắng!", 
                    "Đối phương rời phòng", 
                    JOptionPane.INFORMATION_MESSAGE);
                backToMainFrame();
                break;
            }

            case "OPPONENT_SURRENDERED": {
                String playerName = msg.get("playerName").getAsString();
                JOptionPane.showMessageDialog(this, 
                    playerName + " đã đầu hàng. Bạn thắng!", 
                    "Đối phương đầu hàng", 
                    JOptionPane.INFORMATION_MESSAGE);
                backToMainFrame();
                break;
            }

            default: {
                // Xử lý nếu có thông điệp không rõ
                System.out.println("Unknown message type: " + type);
                break;
            }
        }
    }

    private void startGame(String p1Id, String p1Nick, String p2Id, String p2Nick) {
        System.out.println("Starting game between " + p1Nick + " and " + p2Nick);
        
        // Mở giao diện phòng thách đấu nếu cần
        JOptionPane.showMessageDialog(this, 
            p1Nick + " vs " + p2Nick + " - Trận đấu bắt đầu!", 
            "Trận đấu bắt đầu", 
            JOptionPane.INFORMATION_MESSAGE);
        
        ;
    }
}
