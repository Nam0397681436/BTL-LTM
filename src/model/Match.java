package model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.ArrayList;
import java.util.Objects;

import com.google.gson.Gson;

import java.util.Collections;

public class Match {
    private int matchId; // tự tăng
    private MatchType type; // ONE_VS_ONE, MULTIPLAYER
    private String creatorId; // player_id người tạo (lưu dạng String cho an toàn DB)
    private Date startTime;
    private Date endTime;
    private ArrayList<PlayerMatch> playerMatches;

    public enum MatchType {
        ONE_VS_ONE, MULTIPLAYER
    }

    public Match() {
        this.playerMatches = new ArrayList<>();
    }

    public Match(int matchId, MatchType type, String creatorId, Date startTime, Date endTime) {
        this.matchId = matchId;
        this.type = type;
        this.creatorId = creatorId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.playerMatches = new ArrayList<>();
    }

    public void addPlayerMatch(PlayerMatch playerMatch) {
        this.playerMatches.add(playerMatch);
    }

    public void removePlayerMatch(PlayerMatch playerMatch) {
        this.playerMatches.remove(playerMatch);
    }

    public ArrayList<PlayerMatch> getPlayerMatches() {
        return playerMatches;
    }

    public void setPlayerMatches(ArrayList<PlayerMatch> playerMatches) {
        this.playerMatches = playerMatches;
    }

    public void sapXepBangDiem() {
        Collections.sort(this.playerMatches, (a, b) -> b.getScore() - a.getScore());
    }

    public static Match fromResultSet(ResultSet rs) throws SQLException {
        String t = rs.getString("type");
        MatchType mt = (t == null) ? null : MatchType.valueOf(t);
        Timestamp st = rs.getTimestamp("start_time");
        Timestamp et = rs.getTimestamp("end_time");
        return new Match(
                rs.getInt("match_id"),
                mt,
                rs.getString("creator_id"),
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

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
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

    public Player getHost() {
        return this.playerMatches.stream()
                .filter(e -> e.getPlayer().getPlayerId().equals(this.creatorId))
                .map(PlayerMatch::getPlayer)
                .findFirst()
                .orElse(null);
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

    @Override
    public int hashCode() {
        return Objects.hash(matchId);
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static Match fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, Match.class);
    }
}
