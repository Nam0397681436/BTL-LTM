package model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

import com.google.gson.Gson;

public class Match {
    private int matchId; // tự tăng
    private MatchType type; // ONE_VS_ONE, MULTIPLAYER
    private Date startTime;
    private Date endTime;
    private List<PlayerMatch> players; // những người chơi tham gia trận này
    private boolean finalResultsSent; // Đánh dấu đã gửi kết quả cuối cùng

    public enum MatchType {
        ONE_VS_ONE, MULTIPLAYER
    }

    public Match() {
    }

    public Match(int matchId, MatchType type, Date startTime, Date endTime) {
        this.matchId = matchId;
        this.type = type;
        this.startTime = startTime;
        this.endTime = endTime;
        this.finalResultsSent = false;
    }

    public static Match fromResultSet(ResultSet rs) throws SQLException {
        String t = rs.getString("type");
        MatchType mt = (t == null) ? null : MatchType.valueOf(t);
        Timestamp st = rs.getTimestamp("start_time");
        Timestamp et = rs.getTimestamp("end_time");
        return new Match(
                rs.getInt("match_id"),
                mt,
                st == null ? null : new Date(st.getTime()),
                et == null ? null : new Date(et.getTime()));
    }

    // Getters & Setters
    public int getMatchId() {
        return matchId;
    }

    public void setMatchId(int matchId) {
        this.matchId = matchId;
    }

    public MatchType getType() {
        return type;
    }

    public void setType(MatchType type) {
        this.type = type;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public List<PlayerMatch> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerMatch> players) {
        this.players = players;
    }

    public boolean getFinalResultsSent() {
        return finalResultsSent;
    }
    public void setFinalResultsSent(boolean finalResultsSent) {
        this.finalResultsSent = finalResultsSent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Match))
            return false;
        Match match = (Match) o;
        return matchId == match.matchId;
    }

    public PlayerMatch getHost() {
        if (players != null && !players.isEmpty()) {
            for (PlayerMatch player : players) {
                if (player.isHost()) {
                    return player;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        String json = gson.toJson(this);
        return json;
    }

    public static Match fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, Match.class);
    }
}
