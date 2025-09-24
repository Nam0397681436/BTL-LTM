package server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import model.HandelMatchMulti;
import model.HandelMatchSolo;
import model.Match;
import model.PlayerMatch;
import com.google.gson.JsonObject;
import java.util.Date;

public class MatchOn {
    public static Map<Integer, Match> matches = new ConcurrentHashMap<>();

    public static void addMatch(Integer keyMatch, String jsonObjectMatch) {
        Match match = new Match(); // Khai báo Match nhưng sẽ tạo HandelMatchSolo(nếu type là solo không sẽ là
                                   // multiplayer)

        JsonObject jsonObject = JsonUtil.fromJson(jsonObjectMatch, JsonObject.class);
        String type = jsonObject.get("type").getAsString();

        if (type.contains("SOLO")) {
            // Tạo HandelMatchSolo cho solo matches
            match = new HandelMatchSolo();
            match.setType(Match.MatchType.ONE_VS_ONE);

            PlayerMatch playerMatchInvite = new PlayerMatch();
            playerMatchInvite.setPlayerId(jsonObject.get("playerInvite").getAsString());
            match.addPlayerMatch(playerMatchInvite);

            PlayerMatch playerMatch = new PlayerMatch();
            playerMatch.setPlayerId(jsonObject.get("player").getAsString());
            match.addPlayerMatch(playerMatch);
            match.setType(Match.MatchType.ONE_VS_ONE);
        } else {
            match = new Match();
            match.setType(Match.MatchType.MULTIPLAYER);
        }
        match.setMatchId(jsonObject.get("matchId").getAsInt());
        match.setStartTime(new Date());
        matches.put(keyMatch, match);
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