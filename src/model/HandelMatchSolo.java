package model;

import model.PlayerMatch;
import model.Match;
import com.google.gson.JsonObject;
import java.util.Random;
import dao.PlayerMatchDAO;
import dao.MatchDAO;
public class HandelMatchSolo extends Match {
    private int roundHienTai = 0;
    private String questionRoundHienTai = "";
    private int timeHienThiQuestion;
    private int timeAnswer;
    private boolean player1Answered = false;
    private boolean player2Answered = false;
    private int countAnswer = 0;
    private int playerRealScoreExited=0;
    
    public HandelMatchSolo(){
        this.setType(MatchType.ONE_VS_ONE);
    }
    
    public synchronized void TinhDiemTranDau(JsonObject answerJson) {
        String answer = answerJson.get("answer").getAsString().toUpperCase();
        String playerId = answerJson.get("playerId").getAsString();
        int round = answerJson.get("round").getAsInt();     
        this.countAnswer++;
        System.out.println("MatchID " + this.getMatchId() + " - Round " + round + ": nguoi choi thu " + this.countAnswer + " da tra loi (Player: " + playerId + ")");
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
         bangDiem.addProperty("type","BANG_DIEM");
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
        // Chỉ tạo round mới nếu cả hai players đã trả lời round hiện tại
        if (this.countAnswer < 2 && this.roundHienTai > 0) {
            System.out.println("Không thể tạo round mới vì chưa đủ câu trả lời cho round " + roundHienTai + " (countAnswer=" + countAnswer + ")");
            return null;
        }       
        this.roundHienTai++;
        if(this.roundHienTai > 24){
            return this.ketThucTranDau();
        }
        this.questionRoundHienTai= generateQuestion(this.getLengthByRound(this.roundHienTai));
        this.timeHienThiQuestion=generateTimeShowQuestion(this.roundHienTai);       
        this.timeAnswer=generateTimeAnswer(this.roundHienTai);
        // Reset trạng thái trả lời cho round mới
        this.countAnswer = 0;
        System.out.println("Tạo round mới: " + this.roundHienTai + " với câu hỏi: " + this.questionRoundHienTai);
        JsonObject questionRound = new JsonObject();
        questionRound.addProperty("type","QUESTION");
        questionRound.addProperty("matchId",this.getMatchId());
        questionRound.addProperty("question",questionRoundHienTai);
        questionRound.addProperty("round", this.roundHienTai);
        questionRound.addProperty("time", this.timeHienThiQuestion);
        questionRound.addProperty("timeAnswer", this.timeAnswer);
        return questionRound;
    }
    // Kiểm tra xem cả hai players đã trả lời chưa
    public synchronized boolean bothPlayersAnswered() {
        return this.countAnswer == 2;
    }
    public int getCurrentRound() {
        return this.roundHienTai;
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
            return 4;
        else if (round == 2)
            return 4;
        else if (round >= 3 && round <= 4)
            return 5;
        else if (round >= 5 && round <= 7)
            return 5;
        else if (round >= 8 && round <= 10)
            return 6;
        else if (round >= 11 && round <= 13)
            return 7;
        else if (round >= 14 && round <= 15)
            return 7;
        else if (round >= 16 && round <= 17)
            return 8;
        else if (round >= 18 && round <= 19)
            return 8;
        else if (round >= 20 && round <= 21)
            return 9;
        else if (round >= 22 && round <= 24)
            return 10;
        return 4; // mặc định
    }
    public static int generateTimeAnswer(int round) {
        if (round == 1)
            return 6;
        else if (round == 2)
            return 6;
        else if (round >= 3 && round <= 4)
            return 6;
        else if (round >= 5 && round <= 7)
            return 6;
        else if (round >= 8 && round <= 10)
            return 7;
        else if (round >= 11 && round <= 13)
            return 7;
        else if (round >= 14 && round <= 15)
            return 8;
        else if (round >= 16 && round <= 17)
            return 9;
        else if (round >= 18 && round <= 19)
            return 9;
        else if (round >= 20 && round <= 21)
            return 10;
        else if (round >= 22 && round <= 24)
            return 11;
        return 6; // mặc định
    }
    public void handelPlayerExited(String playerExited) {
        for (PlayerMatch playerMatch : this.getPlayerMatches()) {
            if (playerMatch.getPlayerId().equals(playerExited)) {
                playerMatch.setStatus("LOSE");
                this.playerRealScoreExited=playerMatch.getScore();
                playerMatch.setScore(-3);
            }
        }
    }
    public JsonObject ketThucTranDau() {
        this.sapXepBangDiem();
        // Lưu điểm thực tế trước khi tính kết quả
        int player1RealScore = this.getPlayerMatches().get(0).getScore();
        int player2RealScore = this.getPlayerMatches().get(1).getScore();
        
        if (player1RealScore > player2RealScore) {
             this.getPlayerMatches().get(0).setStatus("WIN");
             this.getPlayerMatches().get(0).setScore(5);
             this.getPlayerMatches().get(1).setStatus("LOSE");
             this.getPlayerMatches().get(1).setScore(-3);
        } 
        else if (player1RealScore == player2RealScore) {
            this.getPlayerMatches().get(0).setStatus("DRAW");
            this.getPlayerMatches().get(0).setScore(0);
            this.getPlayerMatches().get(1).setScore(0);
            this.getPlayerMatches().get(1).setStatus("DRAW");
        }
        // lưu kết quả trận đấu vào database
        PlayerMatchDAO playerMatchDAO = new PlayerMatchDAO();
        for (PlayerMatch playerMatch : this.getPlayerMatches()) {
            playerMatchDAO.create(playerMatch, this.getMatchId());
        }
        // luu thong tin tran dau vao database
        MatchDAO matchDAO = new MatchDAO();
        matchDAO.endMatch(this);

        // gui ket qua trận đấu về client
        JsonObject ketQuaTranDau = new JsonObject();
        ketQuaTranDau.addProperty("type", "KETQUA_TRANDAU");
        ketQuaTranDau.addProperty("matchId", this.getMatchId());
        ketQuaTranDau.addProperty("round", this.roundHienTai);
        
        JsonObject player1 = new JsonObject();
        player1.addProperty("playerId", this.getPlayerMatches().get(0).getPlayerId());
        player1.addProperty("Nickname", this.getPlayerMatches().get(0).getPlayer().getNickname());
        player1.addProperty("score", player1RealScore);
        player1.addProperty("status", this.getPlayerMatches().get(0).getStatus());
        
        JsonObject player2 = new JsonObject();
        player2.addProperty("playerId", this.getPlayerMatches().get(1).getPlayerId());
        player2.addProperty("Nickname", this.getPlayerMatches().get(1).getPlayer().getNickname());
        player2.addProperty("score", player2RealScore < 0 ? playerRealScoreExited : player2RealScore);
        player2.addProperty("status", this.getPlayerMatches().get(1).getStatus());

        
        ketQuaTranDau.add("player1", player1);
        ketQuaTranDau.add("player2", player2);  
        return ketQuaTranDau;
    }
}
