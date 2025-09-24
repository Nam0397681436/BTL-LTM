package model;
import com.google.gson.Gson;

public class RoundResult {
    private Player user;
    private String userAnswer;
    private int score;

    public RoundResult(Player user,String userAnswer, int score) {
        this.user = user;
        this.userAnswer = userAnswer;
        this.score = score;
    }

    public Player getUser() {
        return user;
    }

    public void setUser(Player user) {
        this.user = user;
    }

    public String getUserAnswer() {
        return userAnswer;
    }

    public void setUserAnswer(String userAnswer) {
        this.userAnswer = userAnswer;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static RoundResult fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, RoundResult.class);
    }
}
