package client.ui;

import javax.swing.*;
import com.google.gson.JsonObject;

import client.ClientApp;
import client.net.TcpClient;
import client.JsonUtil;            

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class KetQuaMatchSolo extends JFrame {
    private String player1Name, player2Name;
    private int player1Score, player2Score;
    private String player1Status, player2Status; // "WIN", "LOSE", "DRAW"
    private MainFrame mainFrame;
    private TcpClient tcp;
    private String myNickname;

    // THÊM 2 FIELD ID
    private String myPlayerId;
    private String opponentId;

    // THÊM 2 THAM SỐ ID VÀO CONSTRUCTOR
    public KetQuaMatchSolo(
            TcpClient tcp,
            String player1Name, int player1Score, String player1Status,
            String player2Name, int player2Score, String player2Status,
            String myNickname,
            String myPlayerId, String opponentId) {

        this.tcp = tcp;
        this.player1Name = player1Name;
        this.player1Score = player1Score;
        this.player1Status = player1Status;
        this.player2Name = player2Name;
        this.player2Score = player2Score;
        this.player2Status = player2Status;
        this.myNickname = myNickname;

        this.myPlayerId = myPlayerId;       
        this.opponentId = opponentId;      

        initUI();
    }

    private void initUI() {
        setTitle("Kết quả trận đấu");
        setSize(600, 400);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(245, 247, 250));
        
        // Đăng ký với MainFrame để có thể được đóng khi cần
        MainFrame.setCurrentKetQuaFrame(this);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleWindowClosing();
            }
        });

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("KẾT QUẢ TRẬN ĐẤU", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(32, 88, 110));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));

        JPanel resultPanel = new JPanel(new BorderLayout(20, 0));
        resultPanel.setBackground(Color.WHITE);
        resultPanel.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(new Color(220, 225, 232), 2, true),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JPanel player1Panel = new JPanel(new BorderLayout());
        player1Panel.setBackground(Color.WHITE);
        JLabel statusImage1 = createStatusImage(player1Status);
        player1Panel.add(statusImage1, BorderLayout.NORTH);
        String displayName1 = player1Name.equals(myNickname) ? "You" : player1Name;
        JLabel player1Label = new JLabel(displayName1, SwingConstants.CENTER);
        player1Label.setFont(new Font("Arial", Font.BOLD, 16));
        player1Label.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        player1Panel.add(player1Label, BorderLayout.CENTER);
        JLabel score1Label = new JLabel("Điểm: " + player1Score, SwingConstants.CENTER);
        score1Label.setFont(new Font("Arial", Font.BOLD, 14));
        score1Label.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        player1Panel.add(score1Label, BorderLayout.SOUTH);
        resultPanel.add(player1Panel, BorderLayout.WEST);

        JPanel vsPanel = new JPanel(new BorderLayout());
        vsPanel.setBackground(Color.WHITE);
        JLabel vsLabel = createVsImage();
        vsPanel.add(vsLabel, BorderLayout.CENTER);
        resultPanel.add(vsPanel, BorderLayout.CENTER);

        JPanel player2Panel = new JPanel(new BorderLayout());
        player2Panel.setBackground(Color.WHITE);
        JLabel statusImage2 = createStatusImage(player2Status);
        player2Panel.add(statusImage2, BorderLayout.NORTH);
        String displayName2 = player2Name.equals(myNickname) ? "You" : player2Name;
        JLabel player2Label = new JLabel(displayName2, SwingConstants.CENTER);
        player2Label.setFont(new Font("Arial", Font.BOLD, 16));
        player2Label.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        player2Panel.add(player2Label, BorderLayout.CENTER);
        JLabel score2Label = new JLabel("Điểm: " + player2Score, SwingConstants.CENTER);
        score2Label.setFont(new Font("Arial", Font.BOLD, 14));
        score2Label.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        player2Panel.add(score2Label, BorderLayout.SOUTH);
        resultPanel.add(player2Panel, BorderLayout.EAST);

        // --- BOTTOM BUTTONS ---
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));

        JButton okButton = new JButton("OK");
        okButton.setFont(new Font("Arial", Font.BOLD, 14));
        okButton.setBackground(new Color(32, 88, 110));
        okButton.setForeground(Color.WHITE);
        okButton.setFocusPainted(false);
        okButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        okButton.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));
        okButton.addActionListener(e -> {
            if (mainFrame != null) {
                mainFrame.setVisible(true);
                //ClientApp.setMessageHandler(mainFrame::handleLine);
            }
            dispose();
        });
        bottom.add(okButton);

        JButton rematchBtn = new JButton("Thách đấu lại");
        rematchBtn.setFont(new Font("Arial", Font.BOLD, 14));
        rematchBtn.setBackground(new Color(23, 130, 55));
        rematchBtn.setForeground(Color.WHITE);
        rematchBtn.setFocusPainted(false);
        rematchBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        rematchBtn.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));
        rematchBtn.addActionListener(e -> {
            if (mainFrame != null) {
                mainFrame.setVisible(true);
                ClientApp.setMessageHandler(mainFrame::handleLine);
            }
            try {
                JsonObject m = new JsonObject();
                m.addProperty("type", "INVITE-SOLO");
                m.addProperty("fromId",   myPlayerId);
                m.addProperty("fromNick", myNickname);
                m.addProperty("toId",     opponentId);
                tcp.send(JsonUtil.toJson(m));
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Không thể gửi lời mời thách đấu lại",
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
            MainFrame.clearCurrentKetQuaFrame(); // Clear reference khi đóng
            dispose();
        });
        bottom.add(rematchBtn);           

        // GẮN VÀO MAIN PANEL
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        mainPanel.add(resultPanel, BorderLayout.CENTER);
        mainPanel.add(bottom, BorderLayout.SOUTH);  

        setContentPane(mainPanel); // hoặc add(mainPanel);
    }

    private JLabel createStatusImage(String status) {
        JLabel imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(60, 60));
        try {
            String imagePath;
            switch (status.toUpperCase()) {
                case "WIN":  imagePath = "src/assets/Win.png.png";  break;
                case "LOSE": imagePath = "src/assets/Lose.png.png"; break;
                case "DRAW": imagePath = "src/assets/Draw.png.png"; break;
                default:     imagePath = "src/assets/Draw.png.png"; break;
            }
            ImageIcon icon = new ImageIcon(imagePath);
            Image img = icon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
            imageLabel.setIcon(new ImageIcon(img));
        } catch (Exception e) {
            System.err.println("Không thể load ảnh: " + e.getMessage());
        }
        return imageLabel;
    }

    private JLabel createVsImage() {
        JLabel vsLabel = new JLabel();
        vsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        vsLabel.setPreferredSize(new Dimension(80, 80));
        try {
            ImageIcon icon = new ImageIcon("src/assets/vs.png");
            Image img = icon.getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH);
            vsLabel.setIcon(new ImageIcon(img));
        } catch (Exception e) {
            vsLabel.setText("VS");
            vsLabel.setFont(new Font("Arial", Font.BOLD, 24));
            vsLabel.setForeground(new Color(32, 88, 110));
        }
        return vsLabel;
    }

    private void handleWindowClosing() {
        if (mainFrame != null) {
            mainFrame.setVisible(true);
            ClientApp.setMessageHandler(mainFrame::handleLine);
        }
        MainFrame.clearCurrentKetQuaFrame(); // Clear reference khi đóng
        dispose();
    }

    public void setMainFrame(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }
}
