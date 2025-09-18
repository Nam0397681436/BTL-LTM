package model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class PlayerMatch {
    public int id;             // PK
    public int matchId;        // FK -> matches.match_id
    public String playerId;    // FK -> players.player_id
    public int score;
    public boolean winner;
    public boolean host;

    public PlayerMatch() {}

    public PlayerMatch(int id, int matchId, String playerId, int score, boolean winner, boolean host) {
        this.id = id;
        this.matchId = matchId;
        this.playerId = playerId;
        this.score = score;
        this.winner = winner;
        this.host = host;
    }

    public static PlayerMatch fromResultSet(ResultSet rs) throws SQLException {
        return new PlayerMatch(
                rs.getInt("id"),
                rs.getInt("match_id"),
                rs.getString("player_id"),
                rs.getInt("score"),
                rs.getInt("is_winner") == 1,
                rs.getInt("is_host") == 1
        );
    }

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
                ", host=" + host +
                '}';
    }
}
