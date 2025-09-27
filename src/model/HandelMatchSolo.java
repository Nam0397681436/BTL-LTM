package model;

import model.PlayerMatch;
import model.Match;
import java.util.Date;
import com.google.gson.JsonObject;

import client.JsonUtil;
import server.JsonUtil;
import java.util.Random;

public class HandelMatchSolo extends Match {
    private int roundHienTai = 0;
    private String questionRoundHienTai = "";
    private int timeHienThiQuestion;  
    public HandelMatchSolo(){
        this.setType(MatchType.ONE_VS_ONE);
    }

    public void TinhDiemTranDau(JsonObject answerJson) {
        String answer = answerJson.get("answer").getAsString().toLowerCase();
        String playerId = answerJson.get("playerId").getAsString();
        int round = answerJson.get("round").getAsInt();
        int i = 0;
        int totalPoint = 0;
        while (i < answer.length()) {
            if (answer.charAt(i) == questionRoundHienTai.charAt(i)) {
                totalPoint++;
            } else {
                break;
            }
            i++;
        }
        for (PlayerMatch playerMatch : this.getPlayerMatches()) {
            if (playerMatch.getPlayerId().equals(playerId)) {
                int score=playerMatch.getScore()+totalPoint;
                playerMatch.setScore(score);
            }
        }
        this.sapXepBangDiem();
    }
    public JsonObject bangDiemHienTai(){
         JsonObject bangDiem= new JsonObject();
         bangDiem.addProperty("type","BANGDIEM");
         bangDiem.addProperty("matchId",this.getMatchId());
         bangDiem.addProperty("round",this.roundHienTai);
         
         JsonObject player1= new JsonObject();
         player1.addProperty("playerId",this.getPlayerMatches().get(0).getPlayerId());
         player1.addProperty("Nickname",this.getPlayerMatches().get(0).getPlayer().getNickname());
         player1.addProperty("point",this.getPlayerMatches().get(0).getScore());
         
         JsonObject player2= new JsonObject();
         player2.addProperty("playerId",this.getPlayerMatches().get(1).getPlayerId());
         player2.addProperty("Nickname",this.getPlayerMatches().get(1).getPlayer().getNickname());
         player2.addProperty("point",this.getPlayerMatches().get(1).getScore());
         
         bangDiem.add("player1",player1);
         bangDiem.add("player2",player2);         
         return bangDiem;    
    }

    public JsonObject getQuestionRound() {
        this.roundHienTai++;
        this.questionRoundHienTai= generateQuestion(getLengthByRound(this.roundHienTai));
        this.timeHienThiQuestion=generateTimeShowQuestion(this.roundHienTai);
        JsonObject questionRound = new JsonObject();
        questionRound.addProperty("type","QUESTION");
        questionRound.addProperty("matchId",this.getMatchId());
        questionRound.addProperty("question",questionRoundHienTai);
        questionRound.addProperty("round", this.roundHienTai);
        questionRound.addProperty("time", this.timeHienThiQuestion);
        return questionRound;
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
     public static int getLengthByRound(int round) {
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

    public static int generateTimeShowQuestion(int round) {
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
