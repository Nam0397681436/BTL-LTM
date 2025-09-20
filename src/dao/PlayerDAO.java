package dao;

import model.Player;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class PlayerDAO extends DAO {

    // ID chỉ gồm CHỮ SỐ (mặc định 6). Đổi 4/5 nếu bạn muốn ngắn hơn.
    private static final int ID_LENGTH    = 6;
    private static final int MAX_ATTEMPTS = 25;

    // ---------- AUTH ----------
    public boolean usernameExists(String username) throws SQLException {
        try (var c = getConnection();
             var ps = c.prepareStatement("SELECT 1 FROM players WHERE username=? LIMIT 1")) {
            ps.setString(1, username);
            try (var rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public Optional<Player> login(String username, String password) throws SQLException {
        String sql = """
            SELECT player_id, username, nick_name, total_score, total_wins
            FROM players WHERE username=? AND password=?
            """;
        try (var c = getConnection(); var ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, sha256(password));
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Player(
                            rs.getString(1), rs.getString(2), rs.getString(3),
                            rs.getInt(4), rs.getInt(5)
                    ));
                }
                return Optional.empty();
            }
        }
    }

    public Player register(String username, String password, String nickname) throws SQLException {
        try (var c = getConnection()) {
            String playerId = generateNumericId(c);
            try (var ps = c.prepareStatement(
                    "INSERT INTO players(player_id, username, password, nick_name, total_score, total_wins) " +
                    "VALUES (?, ?, ?, ?, 0, 0)")) {
                ps.setString(1, playerId);
                ps.setString(2, username);
                ps.setString(3, sha256(password));
                ps.setString(4, nickname);
                ps.executeUpdate();
            }
            return new Player(playerId, username, nickname, 0, 0);
        }
    }

    // ---------- PHỤC VỤ UI ----------
    public List<Player> leaderboard(int limit) throws SQLException {
        String sql = """
            SELECT player_id, username, nick_name, total_score, total_wins
            FROM players ORDER BY total_score DESC, total_wins DESC
            LIMIT ?
            """;
        try (var c = getConnection(); var ps = c.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, limit));
            try (var rs = ps.executeQuery()) {
                var list = new ArrayList<Player>();
                while (rs.next()) {
                    list.add(new Player(
                            rs.getString(1), rs.getString(2), rs.getString(3),
                            rs.getInt(4), rs.getInt(5)
                    ));
                }
                return list;
            }
        }
    }

    /** Lấy tất cả người chơi để ghép trạng thái (ONLINE/OFFLINE/IN_MATCH) ở server. */
    public List<Player> listAllPlayers() throws SQLException {
        String sql = """
            SELECT player_id, username, nick_name, total_score, total_wins
            FROM players ORDER BY nick_name ASC
            """;
        try (var c = getConnection(); var ps = c.prepareStatement(sql); var rs = ps.executeQuery()) {
            var list = new ArrayList<Player>();
            while (rs.next()) {
                list.add(new Player(
                        rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getInt(4), rs.getInt(5)
                ));
            }
            return list;
        }
    }

    /** Lịch sử đấu của 1 người chơi. */
    public List<Map<String, Object>> history(String playerId, int limit) throws SQLException {
        String sql = """
            SELECT m.match_id, m.type, m.start_time, m.end_time, pm.score, pm.is_winner
            FROM matches m
            JOIN player_matches pm ON pm.match_id = m.match_id
            WHERE pm.player_id = ?
            ORDER BY m.start_time DESC
            LIMIT ?
            """;
        try (var c = getConnection(); var ps = c.prepareStatement(sql)) {
            ps.setString(1, playerId);
            ps.setInt(2, Math.max(1, limit));
            try (var rs = ps.executeQuery()) {
                var rows = new ArrayList<Map<String, Object>>();
                while (rs.next()) {
                    var r = new LinkedHashMap<String, Object>();
                    r.put("matchId",   rs.getInt("match_id"));
                    r.put("mode",      rs.getString("type"));
                    r.put("startTime", rs.getTimestamp("start_time"));
                    r.put("endTime",   rs.getTimestamp("end_time"));
                    r.put("score",     rs.getInt("score"));
                    r.put("isWinner",  rs.getInt("is_winner") == 1);
                    rows.add(r);
                }
                return rows;
            }
        }
    }

    // ---------- ID số ngẫu nhiên ----------
    private String generateNumericId(Connection c) throws SQLException {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            String id = genDigits(ID_LENGTH, true);
            if (!existsById(c, id)) return id;
        }
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            String id = genDigits(ID_LENGTH, false);
            if (!existsById(c, id)) return id;
        }
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            String id = genDigits(ID_LENGTH + 1, true);
            if (!existsById(c, id)) return id;
        }
        throw new SQLException("Cannot generate unique player_id");
    }
    private static String genDigits(int length, boolean noLeadingZero) {
        var r = ThreadLocalRandom.current();
        var sb = new StringBuilder(length);
        if (noLeadingZero) sb.append(r.nextInt(1, 10)); else sb.append(r.nextInt(10));
        while (sb.length() < length) sb.append(r.nextInt(10));
        return sb.toString();
    }
    private static boolean existsById(Connection c, String id) throws SQLException {
        try (var ps = c.prepareStatement("SELECT 1 FROM players WHERE player_id=? LIMIT 1")) {
            ps.setString(1, id);
            try (var rs = ps.executeQuery()) { return rs.next(); }
        }
    }
}
