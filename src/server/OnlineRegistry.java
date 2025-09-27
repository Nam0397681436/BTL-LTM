package server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import model.Player;
import model.PlayerStatus;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OnlineRegistry {

    // key = playerId
    private static final Map<String, Player> ONLINE = new ConcurrentHashMap<>();
    private static final Map<String, ClientHandler> SESSIONS = new ConcurrentHashMap<>();

    /* ================== Session mapping ================== */

    public static Collection<Player> getAllPlayers() {
        return ONLINE.values();
    }
    public static Player getPlayer(String playerId){
        return ONLINE.get(playerId);
    }

    public static List<ClientHandler> getClients() {
        return new ArrayList<>(SESSIONS.values());
    }

    public static void bindSession(String playerId, ClientHandler h) {
        if (playerId != null && h != null) {
            SESSIONS.put(playerId, h);
        }
    }

    // Player có đang có session hoạt động không?
    public static boolean isOnline(String playerId) {
        if (playerId == null)
            return false;
        return SESSIONS.containsKey(playerId);
    }

    public static synchronized boolean tryBindSession(String playerId, ClientHandler h) {
        if (playerId == null || h == null)
            return false;
        return SESSIONS.putIfAbsent(playerId, h) == null;
    }

    /** Giải phóng phiên, chỉ xóa nếu đúng handler đang nắm (tránh xóa nhầm). */
    public static void unbind(String playerId, ClientHandler h) {
        if (playerId == null || h == null)
            return;
        SESSIONS.computeIfPresent(playerId, (k, v) -> (v == h) ? null : v);
    }

    /** Lấy ClientHandler của một player cụ thể */
    public static ClientHandler getHandler(String playerId) {
        System.out.println("Getting handler for playerId: " + playerId);
        if (playerId == null)
            return null;
        return SESSIONS.get(playerId);
    }

    /* ================== Online set ================== */

    /** Thêm (hoặc cập nhật) 1 người chơi là online và thông báo những người khác */
    public static void add(Player p) {
        if (p == null || p.getPlayerId() == null)
            return;
        ONLINE.put(p.getPlayerId(), p);
        broadcastOnlineAdd(p);
    }

    /** Xoá một người chơi khỏi online và thông báo những người khác */
    public static void remove(String playerId) {
        if (playerId == null)
            return;
        ONLINE.remove(playerId);
        broadcastOnlineRemove(playerId);
        SESSIONS.remove(playerId);
    }

    /** Danh sách Player đang online (snapshot an toàn) */
    public static Collection<Player> onlinePlayers() {
        return new ArrayList<>(ONLINE.values());
    }

    /**
     * Cập nhật trạng thái của player:
     * Sau đó broadcast toàn bộ danh sách online hiện tại cho tất cả client.
     */

    public static void changeStatus(String playerId, PlayerStatus status) {
        if (playerId == null || status == null)
            return;
        Player p = ONLINE.get(playerId);
        if (p != null) {
            p.setStatus(status);
            broadcastOnlineList();
        }
    }

    public static void updateStatus(Player player, String status) {
        if (player == null || player.getPlayerId() == null)
            return;
        switch (status) {
            case "online":
                add(player);
                break;

            case "offline":
                remove(player.getPlayerId());
                break;

            case "in_game":
                Player p = ONLINE.get(player.getPlayerId());
                if (p != null) {
                    p.setStatus(PlayerStatus.IN_GAME);
                }
                break;

            default:
                System.err.println("Unknown status: " + status);
        }
        broadcastOnlineList();
    }

    /* ================== Broadcast helpers ================== */

    /**
     * Gửi snapshot toàn bộ danh sách ONLINE hiện tại cho một client cụ thể (loại
     * bản thân khỏi danh sách).
     */
    public static void sendOnlineSnapshotTo(String targetPlayerId) {
        ClientHandler target = SESSIONS.get(targetPlayerId);
        if (target == null)
            return;

        JsonObject o = buildOnlineListPayload(targetPlayerId); // exclude self
        try {
            target.send(o);
        } catch (Exception ignore) {
        }
    }

    /** Broadcast snapshot ONLINE cho tất cả client (mỗi người tự loại bản thân). */
    public static void broadcastOnlineList() {
        for (String pid : SESSIONS.keySet()) {
            ClientHandler h = SESSIONS.get(pid);
            if (h == null)
                continue;
            JsonObject o = buildOnlineListPayload(pid); // exclude pid
            try {
                h.send(o);
            } catch (Exception ignore) {
            }
        }
    }

    /** Thông báo 1 người vừa online cho tất cả những người khác */
    public static void broadcastOnlineAdd(Player p) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "ONLINE_ADD");
        o.addProperty("playerId", p.getPlayerId());
        o.addProperty("nickname", p.getNickname());
        multicast(o, p.getPlayerId());
    }

    /** Thông báo 1 người vừa offline cho tất cả những người khác */
    public static void broadcastOnlineRemove(String playerId) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "ONLINE_REMOVE");
        o.addProperty("playerId", playerId);
        multicast(o, playerId);
    }

    /** Gửi cho tất cả session trừ người có id = exceptId */
    private static void multicast(JsonObject o, String exceptId) {
        for (var e : SESSIONS.entrySet()) {
            if (e.getKey().equals(exceptId))
                continue;
            try {
                e.getValue().send(o);
            } catch (Exception ignore) {
            }
        }
    }

    /* ================== Payload builders ================== */

    /** Tạo payload ONLINE_LIST; excludeId: loại người này khỏi danh sách */
    private static JsonObject buildOnlineListPayload(String excludeId) {
        JsonArray arr = new JsonArray();
        for (Player p : ONLINE.values()) {
            if (excludeId != null && excludeId.equals(p.getPlayerId()))
                continue; // loại bản thân
            JsonObject row = new JsonObject();
            row.addProperty("playerId", p.getPlayerId());
            row.addProperty("nickname", p.getNickname());
            row.addProperty("status", p.getStatus().toString() == "IN_GAME" ? "IN_GAME" : "ONLINE");
            arr.add(row);
        }
        JsonObject out = new JsonObject();
        out.addProperty("type", "ONLINE_LIST");
        out.add("rows", arr);
        return out;
    }
}
