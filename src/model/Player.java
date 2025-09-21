package model;

public class Player {
    private String playerId;   // private theo yêu cầu
    private String username;
    private String nickname;
    private int totalScore;
    private int totalWins;

    public Player() {}

    public Player(String playerId, String username, String nickname, int totalScore, int totalWins) {
        this.playerId = playerId;
        this.username = username;
        this.nickname = nickname;
        this.totalScore = totalScore;
        this.totalWins = totalWins;
    }

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
}
