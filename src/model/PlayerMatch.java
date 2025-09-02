/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

/**
 *
 * @author Admin
 */
public class PlayerMatch {
    private int Id; // tu dong tăng từ đầu 1->10000
    private int matchId; // Khóa ngoại tới Match
    private String playerId; // Khóa ngoại tới Player
    private int score; // Điểm của người chơi trong trận đấu này
    private boolean isWinner; // Người chơi có phải là người chiến thắng không
    private boolean isHost; // Có phải chủ phòng không (cho chế độ MULTIPLAYER)
    // Phương thức khởi tạo
    public PlayerMatch(int Id, int matchId, String playerId, boolean isHost) {
        this.Id = Id;
        this.matchId = matchId;
        this.playerId = playerId;
        this.score = 0;
        this.isWinner = false;
        this.isHost = isHost;
    }

    public int getMatchId() {
        return matchId;
    }
    
    public void setMatchId(int matchId) {
        this.matchId = matchId;
    }
    
    public String getPlayerId() {
        return playerId;
    }
    
    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }
    
    public int getScore() {
        return score;
    }
    
    public void setScore(int score) {
        this.score = score;
    }
    
    public boolean isWinner() {
        return isWinner;
    }
    
    public void setWinner(boolean winner) {
        isWinner = winner;
    }
    
    public boolean isHost() {
        return isHost;
    }
    
    public void setHost(boolean host) {
        isHost = host;
    }
}
