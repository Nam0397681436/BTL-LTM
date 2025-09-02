/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

/**
 *
 * @author Admin
 */
import java.util.Date;

public class Player {
    private String playerId; //P001 -> 
    private String username;
    private String password;
    private String nickName;
    private int totalScore; // Tổng điểm tích lũy
    private int totalWins; // Tổng số trận thắng
    private PlayerStatus status;
    
    // Phương thức khởi tạo
    public Player(String playerId, String username, String password, String nickName) {
        this.playerId = playerId;
        this.username = username;
        this.password = password;
        this.nickName = nickName;
        this.totalScore = 0;
        this.totalWins = 0;
        this.status = PlayerStatus.ONLINE;
    }
    
    // Các phương thức getter và setter
    public String getPlayerId() {
        return playerId;
    }
    
    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getNickName() {
        return nickName;
    }
    
    public void setNickName(String nickName) {
        this.nickName = nickName;
    }
    
    public int getTotalScore() {
        return totalScore;
    }
    
    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }
    
    public int getTotalWins() {
        return totalWins;
    }
    
    public void setTotalWins(int totalWins) {
        this.totalWins = totalWins;
    }
    
    public PlayerStatus getStatus() {
        return status;
    }
    
    public void setStatus(PlayerStatus status) {
        this.status = status;
    }
}
enum PlayerStatus {
    ONLINE,
    IN_GAME
}