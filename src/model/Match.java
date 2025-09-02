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

public class Match {
    private int matchId; //tu dong tang tu 10000->
    private MatchType type; // Kiểu trận đấu: ONE_VS_ONE, MULTIPLAYER
    private int creatorId; // ID của người tạo phòng (cho chế độ MULTIPLAYER)
    private Date startTime;
    private Date endTime;
    
    // Phương thức khởi tạo
    public Match(int matchId, MatchType type, int creatorId) {
        this.matchId = matchId;
        this.type = type;
        this.creatorId = creatorId;
        this.startTime = new Date();
    }
    
    // Các phương thức getter
    public int getMatchId() {
        return matchId;
    }
    
    public MatchType getType() {
        return type;
    }
    
    public int getCreatorId() {
        return creatorId;
    }
    
    public Date getStartTime() {
        return startTime;
    }
    
    public Date getEndTime() {
        return endTime;
    }
    // Các phương thức setter
    public void setMatchId(int matchId) {
        this.matchId = matchId;
    }
    
    public void setType(MatchType type) {
        this.type = type;
    }
    public void setCreatorId(int creatorId) {
        this.creatorId = creatorId;
    }
    
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }
    
    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }
}

enum MatchType {
    ONE_VS_ONE,
    MULTIPLAYER
}