package model;

import model.PlayerMatch;
import model.Match;
import java.util.Date;
import com.google.gson.JsonObject;

import client.JsonUtil;

import java.util.Random;

public class HandelMatchSolo extends Match {
    private Match match;
    private int roundHienTai = 0;
    private String questionRoundHienTai = "";
    private int timeHienThiQuestion;

    public void TinhDiemTranDau(String answerObject, String questionRound) {
        JsonObject answerJson = JsonUtil.fromJson(answerObject, JsonObject.class);
        String answer = answerJson.get("answer").getAsString().toLowerCase();
        String playerId = answerJson.get("playerId").getAsString();
        String matchId = answerJson.get("matchId").getAsString();
        String round = answerJson.get("round").getAsString();
        String question = questionRound.toLowerCase();
        int i = 0;
        int totalPoint = 0;
        while (i < answer.length()) {
            if (answer.charAt(i) == question.charAt(i)) {
                totalPoint++;
            } else {
                break;
            }
            i++;
        }
        for (PlayerMatch playerMatch : this.match.getPlayerMatches()) {
            if (playerMatch.getPlayerId().equals(playerId)) {
                playerMatch.setScore(totalPoint);
            }
        }
        this.match.sapXepBangDiem();
    }

    public String getQuestionRoundTiepTheo() {
        this.roundHienTai++;
        JsonObject questionRound = new JsonObject();
        questionRound.addProperty("question", "dtfhtfh");
        questionRound.addProperty("round", this.roundHienTai);
        questionRound.addProperty("time", this.timeHienThiQuestion);
        questionRound.addProperty("question", this.generateQuestion(10));
        return questionRound.toString();
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

    // public void ketThucTranDau() {
    //     this.match.sapXepBangDiem();
    //     if (this.match.getPlayerMatches().get(0).getScore() > this.match.getPlayerMatches().get(1).getScore()) {
    //         this.match.getPlayerMatches().get(0).setWinner(true);
    //     } else if (this.match.getPlayerMatches().get(0).getScore() < this.match.getPlayerMatches().get(1).getScore()) {
    //         this.match.getPlayerMatches().get(1).setWinner(true);
    //     } else {
    //         this.match.getPlayerMatches().get(0).setWinner(false);
    //         this.match.getPlayerMatches().get(1).setWinner(false);
    //     }
    //     this.match.setEndTime(new Date());
    // }
}
