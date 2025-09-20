package server;

import model.Player;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lưu trạng thái người chơi đang online (in-memory, thread-safe).
 */
public class OnlineRegistry {
    public enum Status { IDLE, IN_MATCH }

    private static final Map<String, Player> ONLINE = new ConcurrentHashMap<>();
    private static final Map<String, Status> STATE  = new ConcurrentHashMap<>();

    /** Đánh dấu 1 người chơi đã đăng nhập (mặc định IDLE). */
    public static void add(Player p) {
        if (p == null || p.getPlayerId() == null) return;
        ONLINE.put(p.getPlayerId(), p);
        STATE.put(p.getPlayerId(), Status.IDLE);
    }

    /** Xóa khỏi danh sách online. */
    public static void remove(String playerId) {
        if (playerId == null) return;
        ONLINE.remove(playerId);
        STATE.remove(playerId);
    }

    /** Toàn bộ người chơi đang online (không gồm offline). */
    public static Collection<Player> all() {
        return ONLINE.values();
    }

    /** Người này có đang online không? */
    public static boolean isOnline(String playerId) {
        return playerId != null && ONLINE.containsKey(playerId);
    }

    /** Trạng thái hiện tại (mặc định IDLE). */
    public static Status statusOf(String playerId) {
        return STATE.getOrDefault(playerId, Status.IDLE);
    }

    /** Cập nhật trạng thái (chỉ khi đang online). */
    public static void setStatus(String playerId, Status status) {
        if (playerId == null || status == null) return;
        if (ONLINE.containsKey(playerId)) {
            STATE.put(playerId, status);
        }
    }
}
