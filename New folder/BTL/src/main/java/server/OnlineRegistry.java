package server;

import model.Player;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OnlineRegistry {
    public enum Status { IDLE, IN_MATCH }

    private static final Map<String, Player> ONLINE = new ConcurrentHashMap<>();
    private static final Map<String, Status> STATE  = new ConcurrentHashMap<>();

    public static void add(Player p) { ONLINE.put(p.playerId, p); STATE.put(p.playerId, Status.IDLE); }
    public static void remove(String playerId) { ONLINE.remove(playerId); STATE.remove(playerId); }
    public static Collection<Player> all() { return ONLINE.values(); }
    public static boolean isOnline(String playerId) { return ONLINE.containsKey(playerId); }
    public static Status statusOf(String playerId) { return STATE.getOrDefault(playerId, Status.IDLE); }
    public static void setStatus(String playerId, Status s) { if (ONLINE.containsKey(playerId)) STATE.put(playerId, s); }
}
