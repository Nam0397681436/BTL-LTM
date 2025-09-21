package dao;

import model.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlayerDAO {

    /** Đăng nhập: thử SHA-256 trước, nếu không khớp thì thử plaintext (data cũ) và
     *  NÂNG CẤP mật khẩu về SHA-256 ngay sau lần đăng nhập thành công. */
    public Optional<Player> login(String username, String password) throws SQLException {
        final String sql = """
            SELECT player_id, username, nick_name, total_score, total_wins, password
            FROM players
            WHERE username=? AND password=?
            """;
        try (var c = DAO.get()) {
            // 1) thử SHA-256
            try (var ps = c.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, sha256(password));
                try (var rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapPlayer(rs));
                    }
                }
            }
            // 2) thử plaintext (phòng khi dữ liệu cũ chưa hash)
            try (var ps = c.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, password);
                try (var rs = ps.executeQuery()) {
                    if (rs.next()) {
                        // 3) nâng cấp về SHA-256
                        String pid = rs.getString("player_id");
                        try (var up = c.prepareStatement("UPDATE players SET password=? WHERE player_id=?")) {
                            up.setString(1, sha256(password));
                            up.setString(2, pid);
                            up.executeUpdate();
                        }
                        return Optional.of(new Player(
                                pid,
                                rs.getString("username"),
                                rs.getString("nick_name"),
                                rs.getInt("total_score"),
                                rs.getInt("total_wins")
                        ));
                    }
                }
            }
            return Optional.empty();
        }
    }

    /** Đăng ký: luôn lưu mật khẩu dạng SHA-256. Bắt trùng UNIQUE username/nickname. */
    public Player register(String username, String password, String nickname) throws SQLException {
        final String sql = """
            INSERT INTO players (player_id, username, password, nick_name, total_score, total_wins)
            VALUES (?, ?, ?, ?, 0, 0)
            """;
        String playerId = genNumericId(6); // 4–6 số tuỳ bạn, ở đây 6
        try (var c = DAO.get(); var ps = c.prepareStatement(sql)) {
            ps.setString(1, playerId);
            ps.setString(2, username.trim());
            ps.setString(3, sha256(password)); // HASH tại đây
            ps.setString(4, nickname.trim());
            ps.executeUpdate();
        } catch (SQLException ex) {
            // 23000 = vi phạm UNIQUE
            if ("23000".equals(ex.getSQLState())) {
                String msg = ex.getMessage();
                if (msg != null && msg.contains("uq_players_username"))
                    throw new SQLException("USERNAME_EXISTS");
                if (msg != null && msg.contains("uq_players_nickname"))
                    throw new SQLException("NICKNAME_EXISTS");
            }
            throw ex;
        }
        return new Player(playerId, username.trim(), nickname.trim(), 0, 0);
    }

    /** BXH */
    public List<Player> leaderboard(int limit) throws SQLException {
        final String sql = """
            SELECT player_id, username, nick_name, total_score, total_wins
            FROM players
            ORDER BY total_score DESC, total_wins DESC
            LIMIT ?
            """;
        var list = new ArrayList<Player>();
        try (var c = DAO.get(); var ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapPlayer(rs));
            }
        }
        return list;
    }

    /** Lịch sử đấu đơn giản trả mảng map */
    public List<java.util.Map<String,Object>> history(String playerId, int limit) throws SQLException {
        final String sql = """
            SELECT m.match_id   AS matchId,
                   m.type       AS mode,
                   m.start_time AS startTime,
                   m.end_time   AS endTime,
                   pm.score     AS score,
                   pm.is_winner AS isWinner
            FROM player_matches pm
            JOIN matches m ON m.match_id = pm.match_id
            WHERE pm.player_id = ?
            ORDER BY m.start_time DESC
            LIMIT ?
            """;
        var rows = new ArrayList<java.util.Map<String,Object>>();
        try (var c = DAO.get(); var ps = c.prepareStatement(sql)) {
            ps.setString(1, playerId);
            ps.setInt(2, limit);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    var m = new java.util.HashMap<String,Object>();
                    m.put("matchId",   rs.getInt("matchId"));
                    m.put("mode",      rs.getString("mode"));
                    m.put("startTime", rs.getTimestamp("startTime"));
                    m.put("endTime",   rs.getTimestamp("endTime"));
                    m.put("score",     rs.getInt("score"));
                    m.put("isWinner",  rs.getBoolean("isWinner"));
                    rows.add(m);
                }
            }
        }
        return rows;
    }

    /* ================= helpers ================= */

    private Player mapPlayer(java.sql.ResultSet rs) throws SQLException {
        return new Player(
                rs.getString("player_id"),
                rs.getString("username"),
                rs.getString("nick_name"),
                rs.getInt("total_score"),
                rs.getInt("total_wins")
        );
    }

    /** Hash SHA-256 (hex 64 ký tự) */
    private static String sha256(String s) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var sb = new StringBuilder(dig.length * 2);
            for (byte b : dig) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    /** ID 4–6 số (ở đây 6) */
    private static String genNumericId(int len) {
        var rnd = new java.security.SecureRandom();
        var sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(rnd.nextInt(10));
        return sb.toString();
    }
}
