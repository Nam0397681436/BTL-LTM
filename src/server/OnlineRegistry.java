package server;

import model.Player;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OnlineRegistry {
    private static final Map<String, Player> ONLINE = new ConcurrentHashMap<>();
    private static final Map<String, ClientHandler> SESSIONS = new ConcurrentHashMap<>();

    public static void add(Player p) {
        if (p == null || p.getPlayerId() == null) return;
        ONLINE.put(p.getPlayerId(), p);
        broadcastOnlineAdd(p);
    }

    public static void remove(String playerId) {
        if (playerId == null) return;
        ONLINE.remove(playerId);
        broadcastOnlineRemove(playerId);
        SESSIONS.remove(playerId);
    }

    public static Collection<Player> onlinePlayers() { return ONLINE.values(); }

    public static void bindSession(String playerId, ClientHandler h) {
        if (playerId != null && h != null) SESSIONS.put(playerId, h);
    }

    /* -------- broadcast -------- */

    public static void broadcastOnlineAdd(Player p) {
        var o = new com.google.gson.JsonObject();
        o.addProperty("type", "ONLINE_ADD");
        o.addProperty("playerId", p.getPlayerId());
        o.addProperty("nickname", p.getNickname());
        multicast(o, p.getPlayerId());
    }

    public static void broadcastOnlineRemove(String playerId) {
        var o = new com.google.gson.JsonObject();
        o.addProperty("type", "ONLINE_REMOVE");
        o.addProperty("playerId", playerId);
        multicast(o, playerId);
    }

    private static void multicast(com.google.gson.JsonObject o, String exceptId) {
        for (var e : SESSIONS.entrySet()) {
            if (e.getKey().equals(exceptId)) continue;
            try { e.getValue().send(o); } catch (Exception ignore) {}
        }
    }
}
