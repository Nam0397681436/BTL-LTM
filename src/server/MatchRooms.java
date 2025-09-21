package server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import model.Match;
public class MatchRooms {

    private static final Map<Integer, Match> MATCHES = new ConcurrentHashMap<>();

    public static void add(Match m) {
        if (m == null)  
            return;
        MATCHES.put(m.getMatchId(), m);
    }

    public static void remove(int matchId) {
        MATCHES.remove(matchId);
    }

    public static Match get(int matchId) {
        return MATCHES.get(matchId);
    }

    public static void update(Match m) {
        if (m == null)
            return;
        MATCHES.put(m.getMatchId(), m);
    }
}
