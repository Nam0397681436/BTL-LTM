package model;

import java.util.Date;

import com.google.gson.Gson;

public class Player {
    private String playerId;
    private String username;
    private String nickname;
    private String password;
    private int totalScore;
    private int totalWins;
    private PlayerStatus status;

    public Player() {
    }

    public Player(String playerId, String username, String nickname, String password, int totalScore, int totalWins,
            PlayerStatus status) {
        this.playerId = playerId;
        this.username = username;
        this.nickname = nickname;
        this.password = password;
        this.totalScore = totalScore;
        this.totalWins = totalWins;
        this.status = status;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String username) {
        this.password = password;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public int getTotalWins() {
        return totalWins;
    }

    public void setTotalWins(int totalWins) {
        this.totalWins = totalWins;
    }

    public PlayerStatus getStatus() {
        return status;
    }

    public void setStatus(PlayerStatus status) {
        this.status = status;
    }

    public void updateStatus(PlayerStatus newStatus) {
        this.status = newStatus;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        String json = gson.toJson(this);
        return json;
    }

    public static Player fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, Player.class);
    }
}
