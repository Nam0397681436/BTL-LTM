package model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class PlayerMatch {
    private int id;             // PK auto-increment
    private int matchId;        // FK -> matches.match_id
    private String playerId;    // FK -> players.player_id
    private int score;
    private String winner;


    public PlayerMatch() {}

    public PlayerMatch(int id, int matchId, String playerId, int score, String winner) {
        this.id = id;
        this.matchId = matchId;
        this.playerId = playerId;
        this.score = score;
        this.winner = winner;
    }

    public static PlayerMatch fromResultSet(ResultSet rs) throws SQLException {
        return new PlayerMatch(
                rs.getInt("id"),
                rs.getInt("match_id"),
                rs.getString("player_id"),
                rs.getInt("score"),
                rs.getString("is_winner")
        );
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getMatchId() { return matchId; }
    public void setMatchId(int matchId) { this.matchId = matchId; }
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public String isWinner() { return winner; }
    public void setWinner(String winner) { this.winner = winner; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerMatch)) return false;
        PlayerMatch that = (PlayerMatch) o;
        return id == that.id;
    }
    @Override public int hashCode() { return Objects.hash(id); }

    @Override public String toString() {
        return "PlayerMatch{" +
                "id=" + id +
                ", matchId=" + matchId +
                ", playerId='" + playerId + '\'' +
                ", score=" + score +
                ", winner=" + winner +
                '}';
    }
}
