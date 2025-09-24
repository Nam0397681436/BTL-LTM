package model;

import java.util.Objects;

import com.google.gson.Gson;

public class PlayerMatch implements Comparable<PlayerMatch> {
    private int id;
    private Player player;
    private int score;
    private String inputAnswer;
    private String playerId;
    private String status; // "WINNER" hoặc "LOSER"

    public PlayerMatch() {
    }

    public PlayerMatch(int id, Player player, int score, boolean winner) {
        this.id = id;
        this.player = player;
        this.score = score;
        this.inputAnswer = null;
    }

    // Getters & Setters
    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static PlayerMatch fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, PlayerMatch.class);
    }
}
