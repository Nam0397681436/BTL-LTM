package server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import model.HandelMatchMulti;
import model.HandelMatchSolo;
import model.Match;
import model.PlayerMatch;
import com.google.gson.JsonObject;
import java.util.Date;
import dao.*;
import model.Player;

public class MatchOn {
    public static Map<Integer, Match> matches = new ConcurrentHashMap<>();

    public static HandelMatchSolo addMatchSolo(JsonObject jsonObject) {
            // Tạo HandelMatchSolo cho solo matches
        
        HandelMatchSolo match = new HandelMatchSolo();
        match.setType(Match.MatchType.ONE_VS_ONE);
        MatchDAO matchDAO= new MatchDAO();
        matchDAO.create(match);

        //tao du lieu trong bang PlayerMatch 
        PlayerMatch playerMatch1 = new PlayerMatch();
        playerMatch1.setPlayer(OnlineRegistry.getPlayer(jsonObject.get("p1Id").getAsString()));
        PlayerMatchDAO playerMatchDAO= new PlayerMatchDAO();
        playerMatchDAO.create(playerMatch1, match.getMatchId());
        match.addPlayerMatch(playerMatch1);

        PlayerMatch playerMatch2 = new PlayerMatch();
        playerMatch2.setPlayer(OnlineRegistry.getPlayer(jsonObject.get("p2Id").getAsString()));
        playerMatchDAO.create(playerMatch2, match.getMatchId());
        match.addPlayerMatch(playerMatch2);

        // cap nhat trang thai trong tran cho cac client
        OnlineRegistry.updateStatus(OnlineRegistry.getPlayer(jsonObject.get("p1Id").getAsString()),"in_game");
        OnlineRegistry.updateStatus(OnlineRegistry.getPlayer(jsonObject.get("p2Id").getAsString()),"in_game");
                       
        match.setStartTime(new Date());
        matches.put(match.getMatchId(), match);
        return match;
    }

    public static void removeMatch(int keyMatch) {
        matches.remove(keyMatch);
    }

    public static Match getMatch(int keyMatch) {
        return matches.get(keyMatch);
    }

    public static void addMultiMatch(Match match) {
        matches.put(match.getMatchId(), match);
    }

    public static void updateMatch(Match match) {
        matches.put(match.getMatchId(), match);
    }

    public static HandelMatchSolo getSoloMatch(int keyMatch) {
        Match match = matches.get(keyMatch);
        if (match instanceof HandelMatchSolo) {
            return (HandelMatchSolo) match;
        }
        return null;
    }

    public static HandelMatchMulti getMultiMatch(int keyMatch) {
        Match match = matches.get(keyMatch);
        if (match instanceof HandelMatchMulti) {
            return (HandelMatchMulti) match;
        }
        return null;
    }

    public static Map<Integer, HandelMatchSolo> getAllSoloMatches() {
        Map<Integer, HandelMatchSolo> soloMatches = new ConcurrentHashMap<>();
        for (Map.Entry<Integer, Match> entry : matches.entrySet()) {
            if (entry.getValue() instanceof HandelMatchSolo) {
                soloMatches.put(entry.getKey(), (HandelMatchSolo) entry.getValue());
            }
        }
        return soloMatches;
    }
}