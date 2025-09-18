package model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class Player {
    public String playerId;
    public String username;
    public String nickname;
    public int totalScore;
    public int totalWins;

    public Player() {}

    public Player(String playerId, String username, String nickname, int totalScore, int totalWins) {
        this.playerId = playerId;
        this.username = username;
        this.nickname = nickname;
        this.totalScore = totalScore;
        this.totalWins = totalWins;
    }

    public static Player fromResultSet(ResultSet rs) throws SQLException {
        return new Player(
                rs.getString("player_id"),
                rs.getString("username"),
                rs.getString("nick_name"),
                rs.getInt("total_score"),
                rs.getInt("total_wins")
        );
    }

    // Getters/Setters (nếu bạn thích dùng kiểu property)
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }
    public int getTotalWins() { return totalWins; }
    public void setTotalWins(int totalWins) { this.totalWins = totalWins; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Player)) return false;
        Player player = (Player) o;
        return Objects.equals(playerId, player.playerId);
    }
    @Override public int hashCode() { return Objects.hash(playerId); }

    @Override public String toString() {
        return "Player{" +
                "playerId='" + playerId + '\'' +
                ", username='" + username + '\'' +
                ", nickname='" + nickname + '\'' +
                ", totalScore=" + totalScore +
                ", totalWins=" + totalWins +
                '}';
    }
}
