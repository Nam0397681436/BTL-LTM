package model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class PlayerMatch implements Comparable<PlayerMatch> {
    private int id; // PK auto-increment
    private Player player; // FK -> players.player_id
    private int score;
    private boolean winner;
    private boolean host;
    private String inputAnswer;

    public PlayerMatch() {
    }

    public PlayerMatch(int id, Player player, int score, boolean winner, boolean host) {
        this.id = id;
        this.player = player;
        this.score = score;
        this.winner = winner;
        this.host = host;
        this.inputAnswer = null;
    }

    // Getters & Setters
    public int getPlayerId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public boolean isWinner() {
        return winner;
    }

    public void setWinner(boolean winner) {
        this.winner = winner;
    }

    public boolean isHost() {
        return host;
    }

    public void setHost(boolean host) {
        this.host = host;
    }


    public int getId() {
        return id;
    }

    public String getInputAnswer() {
        return inputAnswer;
    }

    public void setInputAnswer(String inputAnswer) {
        this.inputAnswer = inputAnswer;
    }

    @Override
    public int compareTo(PlayerMatch other) {
        
        return Integer.compare(other.getScore(), this.getScore());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof PlayerMatch))
            return false;
        PlayerMatch that = (PlayerMatch) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PlayerMatch{" +
                "id=" + id +
                ", player=" + player +
                ", score=" + score +
                ", winner=" + winner +
                ", host=" + host +
                '}';
    }
}
