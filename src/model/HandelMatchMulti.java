package model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class HandelMatchMulti extends Match {
    private int currentRound = 0;
    private String questionCurrentRound = "";
    private int timeShowQuestion;
    private boolean isFinalResult = false;
    private boolean nextRoundStarted = false;

    public ArrayList<RoundResult> calculateRoundScore() {
        this.nextRoundStarted = false;
        ArrayList<RoundResult> roundResults = new ArrayList<>();
        ArrayList<PlayerMatch> playerMatchs = this.getPlayerMatches();

        for (PlayerMatch p : playerMatchs) {
            int score = caculateScore(p.getInputAnswer());
            p.setScore(p.getScore() + score);
            roundResults.add(new RoundResult(p.getPlayer(), p.getInputAnswer(), score));
        }

        List<PlayerMatch> sorted = new ArrayList<>(playerMatchs);
        sorted.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        int rank = 1;
        for (PlayerMatch p : sorted) {
            p.setStatus("TOP" + rank);
            rank++;
        }

        return roundResults;
    }

    public boolean hasNextRoundStarted() {
        return this.nextRoundStarted;
    }

    private int caculateScore(String answer) {
        int i = 0;
        int totalPoint = 0;
        while (i < answer.length()) {
            if (answer.charAt(i) == questionCurrentRound.charAt(i)) {
                totalPoint++;
            } else {
                break;
            }
            i++;
        }
        return totalPoint;
    }

    public boolean getFinalResultsSent() {
        return this.isFinalResult;
    }

    public void setFinalResultsSent(boolean isFinalResult) {
        this.isFinalResult = isFinalResult;
    }

    public void nextRound() {
        if (this.nextRoundStarted)
            return;
        this.nextRoundStarted = true;
        this.currentRound++;
        this.questionCurrentRound = generateQuestion(getLengthByRound(currentRound));
        this.timeShowQuestion = generateTimeShowQuestion(currentRound);
    }

    public String getQuestionCurrentRound() {
        return questionCurrentRound;
    }

    public int getTimeShowQuestion() {
        return timeShowQuestion;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public String generateQuestion(int length) {
        Random random = new Random();
        String aphal = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        String question = "";
        for (int i = 0; i < length; i++) {
            question += aphal.charAt(random.nextInt(aphal.length()));
        }
        return question;
    }

    private static int getLengthByRound(int round) {
        if (round == 1)
            return 4;
        else if (round == 2)
            return 5;
        else if (round >= 3 && round <= 4)
            return 6;
        else if (round >= 5 && round <= 7)
            return 7;
        else if (round >= 8 && round <= 10)
            return 8;
        else if (round >= 11 && round <= 13)
            return 9;
        else if (round >= 14 && round <= 15)
            return 10;
        else if (round >= 16 && round <= 17)
            return 11;
        else if (round >= 18 && round <= 19)
            return 12;
        else if (round >= 20 && round <= 21)
            return 13;
        else if (round >= 22 && round <= 24)
            return 14;
        return 4; // mặc định
    }

    private static int generateTimeShowQuestion(int round) {
        if (round == 1)
            return 3;
        else if (round == 2)
            return 3;
        else if (round >= 3 && round <= 4)
            return 4;
        else if (round >= 5 && round <= 7)
            return 4;
        else if (round >= 8 && round <= 10)
            return 5;
        else if (round >= 11 && round <= 13)
            return 6;
        else if (round >= 14 && round <= 15)
            return 6;
        else if (round >= 16 && round <= 17)
            return 7;
        else if (round >= 18 && round <= 19)
            return 7;
        else if (round >= 20 && round <= 21)
            return 8;
        else if (round >= 22 && round <= 24)
            return 9;
        return 3; // mặc định
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static HandelMatchMulti fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, HandelMatchMulti.class);
    }
}
