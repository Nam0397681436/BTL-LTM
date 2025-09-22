package model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class PlayerMatch {
    private int id;             // PK auto-increment
    private int matchId;        // FK -> matches.match_id
    private String playerId;    // FK -> players.player_id
    private int score=0;
    private boolean winner;
    private boolean host;

    public PlayerMatch() {}

    public PlayerMatch(int id, int matchId, String playerId) {
        this.id = id;
        this.matchId = matchId;
        this.playerId = playerId;
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
    public boolean isWinner() { return winner; }
    public void setWinner(boolean winner) { this.winner = winner; }
    public boolean isHost() { return host; }
    public void setHost(boolean host) { this.host = host; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerMatch)) return false;
        PlayerMatch that = (PlayerMatch) o;
        return id == that.id;
    }
   
}
