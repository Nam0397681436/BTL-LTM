package client.ui;

import javax.swing.*;

import client.ClientApp;
import client.net.TcpClient;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class KetQuaMatchSolo extends JFrame {
    private String player1Name, player2Name;
    private int player1Score, player2Score;
    private String player1Status, player2Status; // "WIN", "LOSE", "DRAW"
    private MainFrame mainFrame;
    private TcpClient tcp;
    private String myNickname;

    public KetQuaMatchSolo(TcpClient tcp,String player1Name, int player1Score, String player1Status,
                          String player2Name, int player2Score, String player2Status, String myNickname) {
        this.tcp = tcp;
        this.player1Name = player1Name;
        this.player1Score = player1Score;
        this.player1Status = player1Status;
        this.player2Name = player2Name;
        this.player2Score = player2Score;
        this.player2Status = player2Status;
        this.myNickname = myNickname;       
        initUI();
    }

    private void initUI() {
        setTitle("Kết quả trận đấu");
        setSize(600, 400);
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

        // Panel chính
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setOpaque(false);

        // Tiêu đề
        JLabel titleLabel = new JLabel("KẾT QUẢ TRẬN ĐẤU", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(32, 88, 110));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));

        // Panel kết quả - Layout ngang với VS ở giữa
        JPanel resultPanel = new JPanel(new BorderLayout(20, 0));
        resultPanel.setBackground(Color.WHITE);
        resultPanel.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(new Color(220, 225, 232), 2, true),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        // Player 1 Panel (bên trái)
        JPanel player1Panel = new JPanel(new BorderLayout());
        player1Panel.setBackground(Color.WHITE);
        
        // Ảnh trạng thái cho player 1
        JLabel statusImage1 = createStatusImage(player1Status);
        player1Panel.add(statusImage1, BorderLayout.NORTH);
        
        String displayName1 = player1Name.equals(myNickname) ? "You" : player1Name;
        JLabel player1Label = new JLabel(displayName1, SwingConstants.CENTER);
        player1Label.setFont(new Font("Arial", Font.BOLD, 16));
        player1Label.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0)); // Khoảng cách trên
        player1Panel.add(player1Label, BorderLayout.CENTER);
        
        // Score cho Player 1
        JLabel score1Label = new JLabel("Điểm: " + player1Score, SwingConstants.CENTER);
        score1Label.setFont(new Font("Arial", Font.BOLD, 14));
        score1Label.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0)); // Khoảng cách trên
        player1Panel.add(score1Label, BorderLayout.SOUTH);
        
        resultPanel.add(player1Panel, BorderLayout.WEST);

        // VS Panel (ở giữa)
        JPanel vsPanel = new JPanel(new BorderLayout());
        vsPanel.setBackground(Color.WHITE);
        
        JLabel vsLabel = createVsImage();
        vsPanel.add(vsLabel, BorderLayout.CENTER);
        
        resultPanel.add(vsPanel, BorderLayout.CENTER);

        // Player 2 Panel (bên phải)
        JPanel player2Panel = new JPanel(new BorderLayout());
        player2Panel.setBackground(Color.WHITE);
        
        // Ảnh trạng thái cho player 2
        JLabel statusImage2 = createStatusImage(player2Status);
        player2Panel.add(statusImage2, BorderLayout.NORTH);
        
        String displayName2 = player2Name.equals(myNickname) ? "You" : player2Name;
        JLabel player2Label = new JLabel(displayName2, SwingConstants.CENTER);
        player2Label.setFont(new Font("Arial", Font.BOLD, 16));
        player2Label.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0)); // Khoảng cách trên
        player2Panel.add(player2Label, BorderLayout.CENTER);
        
        // Score cho Player 2
        JLabel score2Label = new JLabel("Điểm: " + player2Score, SwingConstants.CENTER);
        score2Label.setFont(new Font("Arial", Font.BOLD, 14));
        score2Label.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0)); // Khoảng cách trên
        player2Panel.add(score2Label, BorderLayout.SOUTH);
        
        resultPanel.add(player2Panel, BorderLayout.EAST);

        // Nút OK
        JButton okButton = new JButton("OK");
        okButton.setFont(new Font("Arial", Font.BOLD, 14));
        okButton.setBackground(new Color(32, 88, 110));
        okButton.setForeground(Color.WHITE);
        okButton.setFocusPainted(false);
        okButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        okButton.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));
        okButton.addActionListener(e -> {
            // Quay về MainFrame
            if (mainFrame != null) {
                mainFrame.setVisible(true);
                ClientApp.setMessageHandler(mainFrame::handleLine);
            }
            dispose();
        });

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setOpaque(false);
        buttonPanel.add(okButton);

        // Thêm các component vào main panel
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        mainPanel.add(resultPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private String getStatusText(String status) {
        switch (status.toUpperCase()) {
            case "WIN": return "THẮNG";
            case "LOSE": return "THUA";
            case "DRAW": return "HÒA";
            default: return status;
        }
    }

    private Color getStatusColor(String status) {
        switch (status.toUpperCase()) {
            case "WIN": return new Color(0, 128, 0); // Xanh lá
            case "LOSE": return new Color(220, 20, 60); // Đỏ
            case "DRAW": return new Color(255, 140, 0); // Cam
            default: return Color.BLACK;
        }
    }

    private JLabel createStatusImage(String status) {
        JLabel imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(60, 60));
        
        try {
            String imagePath;
            switch (status.toUpperCase()) {
                case "WIN":
                    imagePath = "src/assets/Win.png.png";
                    break;
                case "LOSE":
                    imagePath = "src/assets/Lose.png.png";
                    break;
                case "DRAW":
                    imagePath = "src/assets/Draw.png.png";
                    break;
                default:
                    imagePath = "src/assets/Draw.png.png";
                    break;
            }
            
            ImageIcon icon = new ImageIcon(imagePath);
            // Resize ảnh về kích thước 50x50
            Image img = icon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
            imageLabel.setIcon(new ImageIcon(img));
            
        } catch (Exception e) {
            // Nếu không load được ảnh, để trống
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
            // Resize ảnh về kích thước 60x60
            Image img = icon.getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH);
            vsLabel.setIcon(new ImageIcon(img));
        } catch (Exception e) {
            // Nếu không load được ảnh, hiển thị chữ "VS"
            vsLabel.setText("VS");
            vsLabel.setFont(new Font("Arial", Font.BOLD, 24));
            vsLabel.setForeground(new Color(32, 88, 110));
        }
        
        return vsLabel;
    }

    private void handleWindowClosing() {
        // Quay về MainFrame
        if (mainFrame != null) {
            mainFrame.setVisible(true);
            ClientApp.setMessageHandler(mainFrame::handleLine);
        }
        dispose();
    }

    public void setMainFrame(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }
}