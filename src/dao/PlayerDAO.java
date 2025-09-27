package dao;

import java.sql.*;
import java.util.*;
import model.Player;

/**
 * - Sinh player_id 6 chữ số (000001, 000002, ...) trong transaction.
 * - Đăng ký, đăng nhập, tra cứu, cập nhật điểm/thắng.
 */
public class PlayerDAO {

    private Connection getConn() throws SQLException {
        return DAO.get();
    }

    /* ======================= Helper mapping ======================= */

    private Player map(ResultSet rs) throws SQLException {
        if (rs == null) return null;
        Player p = new Player();
        p.setPlayerId(rs.getString("player_id"));
        p.setUsername(rs.getString("username"));
        p.setPassword(rs.getString("password"));
        p.setNickname(rs.getString("nick_name"));
        p.setTotalScore(rs.getInt("total_score"));
        p.setTotalWins(rs.getInt("total_wins"));
        return p;
    }

    /**
     * Sinh id tiếp theo dạng 6 chữ số trong 1 transaction.
     * Ví dụ: MAX('000135') -> 135 + 1 -> LPAD -> '000136'
     */
    private String nextPlayerId(Connection c) throws SQLException {
        String sql = """
            SELECT LPAD(IFNULL(MAX(CAST(player_id AS UNSIGNED)), 0) + 1, 6, '0') AS next_id
            FROM players
        """;
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getString(1);
        }
    }
    /* ======================= BXH ======================= */
    public List<model.Player> getLeaderboard(int limit) throws SQLException {
        String sql = """
                     SELECT player_id, username, password, nick_name, total_score, total_wins
                     FROM players
                     ORDER BY total_score DESC, total_wins DESC, player_id ASC
                     LIMIT ?
                     """;
        List<model.Player> list = new ArrayList<>();
        try(Connection c = getConn();
        PreparedStatement ps = c.prepareStatement(sql)){
            ps.setInt(1, limit);
            try(ResultSet rs = ps.executeQuery()){
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }
    /* ======================= Lịch sử đấu ======================= */
    public List<Map<String,Object>> getHistory(String playerId, int limit) throws SQLException {
        String sql = """
            SELECT m.match_id,
                   m.type        AS mode,
                   m.start_time,
                   m.end_time,
                   pm.score,
                   pm.status AS result
            FROM player_matches pm
            JOIN matches m ON m.match_id = pm.match_id
            WHERE pm.player_id = ?
            ORDER BY m.start_time DESC, m.match_id DESC
            LIMIT ?
        """;
        List<Map<String,Object>> out = new ArrayList<>();
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, playerId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String,Object> row = new HashMap<>();
                    row.put("matchId",   rs.getInt("match_id"));
                    row.put("mode",      rs.getString("mode")); // ONE_VS_ONE / MULTIPLAYER
                    row.put("startTime", rs.getTimestamp("start_time"));
                    row.put("endTime",   rs.getTimestamp("end_time"));
                    row.put("score",     rs.getInt("score"));
                    row.put("status",  rs.getString("result"));
                    out.add(row);
                }
            }
        }
        return out;
    }    
    /* ======================= Public APIs ======================= */

    /**
     * Đăng ký tài khoản mới.
     * @throws SQLException "USERNAME_EXISTS" hoặc "NICKNAME_EXISTS" nếu trùng.
     */
    public Player register(String username, String password, String nickname) throws SQLException {
        try (Connection c = getConn()) {
            // Tăng cô lập để hạn chế đua ID
            int oldIso = c.getTransactionIsolation();
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            c.setAutoCommit(false);

            try {
                // 1) Sinh id
                String newId = nextPlayerId(c);

                // 2) Insert
                String sql = """
                    INSERT INTO players (player_id, username, password, nick_name, total_score, total_wins)
                    VALUES (?, ?, ?, ?, 0, 0)
                """;
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, newId);
                    ps.setString(2, username);
                    ps.setString(3, password);
                    ps.setString(4, nickname);
                    ps.executeUpdate();
                }

                // 3) Lấy lại row để trả về đầy đủ
                Player created = getById(c, newId);

                c.commit();
                c.setTransactionIsolation(oldIso);
                return created;

            } catch (SQLException ex) {
                c.rollback();

                // Chuẩn hóa thông báo vi phạm UNIQUE -> ném code để server hiển thị đẹp
                String msg = ex.getMessage();
                if (msg != null) {
                    String lower = msg.toLowerCase();
                    if (lower.contains("uq_players_username") || lower.contains("username"))
                        throw new SQLException("USERNAME_EXISTS");
                    if (lower.contains("uq_players_nickname") || lower.contains("nick_name"))
                        throw new SQLException("NICKNAME_EXISTS");
                    if (lower.contains("duplicate") && lower.contains("nick"))
                        throw new SQLException("NICKNAME_EXISTS");
                    if (lower.contains("duplicate") && lower.contains("user"))
                        throw new SQLException("USERNAME_EXISTS");
                }
                throw ex;
            } finally {
                try { c.setAutoCommit(true); } catch (Exception ignore) {}
            }
        }
    }

    /**
     * Đăng nhập: đúng username/password trả về Player, sai trả về null.
     */
    public Player login(String username, String password) throws SQLException {
        String sql = """
            SELECT player_id, username, password, nick_name, total_score, total_wins
            FROM players
            WHERE username = ? AND password = ?
            LIMIT 1
        """;
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
                return null;
            }
        }
    }

    /**
     * Lấy người chơi theo ID.
     */
    public Player getById(String playerId) throws SQLException {
        try (Connection c = getConn()) {
            return getById(c, playerId);
        }
    }

    private Player getById(Connection c, String playerId) throws SQLException {
        String sql = """
            SELECT player_id, username, password, nick_name, total_score, total_wins
            FROM players
            WHERE player_id = ?
            LIMIT 1
        """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
                return null;
            }
        }
    }

    /**
     * Lấy người chơi theo username.
     */
    public Player getByUsername(String username) throws SQLException {
        String sql = """
            SELECT player_id, username, password, nick_name, total_score, total_wins
            FROM players
            WHERE username = ?
            LIMIT 1
        """;
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
                return null;
            }
        }
    }
    
    /**
     * Cộng điểm/thắng sau khi kết thúc trận.
     */
    public void addScoreAndWins(String playerId, int scoreDelta, int winDelta) throws SQLException {
        String sql = """
            UPDATE players
            SET total_score = total_score + ?, total_wins = total_wins + ?
            WHERE player_id = ?
        """;
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, scoreDelta);
            ps.setInt(2, winDelta);
            ps.setString(3, playerId);
            ps.executeUpdate();
        }
    }

    /* ================== Status Player ==================
    */
    public void updateStatusNoop(String playerId, String status) throws SQLException {
        try (Connection ignored = getConn()) {
            // intentionally empty
        }
    }
}
