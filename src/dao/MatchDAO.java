package dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import model.Match;

public class MatchDAO extends DAO {
    public MatchDAO() {
        super();
    }

    public void create(Match match) {
        String sql1 = "INSERT INTO matches (type, creator_id) VALUES (?, ?)";
        try {
            var con = getConnection();
            try (PreparedStatement stmt = con.prepareStatement(sql1, PreparedStatement.RETURN_GENERATED_KEYS)) {
                // Thêm trận mới
                stmt.setString(1, match.getType().toString());
                stmt.setString(2, match.getHost().getPlayerId());
                stmt.executeUpdate();
                // Lấy id vừa insert
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        match.setMatchId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deletedMatch(Match match) {
        String sql = "DELETE FROM matches WHERE match_id = ?";
        System.out.println("Deleting match ID: " + match.getMatchId());
        try {
            var con = getConnection();
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setInt(1, match.getMatchId());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void endMatch(Match match) {
        String sql = "UPDATE matches SET end_time = ? WHERE match_id = ?";
        try {
            var con = getConnection();
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                stmt.setInt(2, match.getMatchId());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // public void updateMatch(Match match) {
    // if (match.isFinished())
    // return;
    // match.setFinished(true);

    // String sqlMatch = "INSERT INTO player_matches (match_id, user_id, status,
    // points, role) VALUES (?, ?, ?, ?, ?)";
    // // String sqlUpdateUser = "UPDATE users SET total_points = total_points + ?,
    // total_wins = total_wins + ? WHERE id = ?";
    // // String sqlUpdateMatches = "UPDATE users SET total_matches = total_matches
    // + 1 WHERE id = ?";

    // try {
    // con.setAutoCommit(false);

    // // === 1. Luôn lưu player_matches ===
    // try (PreparedStatement stmt = con.prepareStatement(sqlMatch)) {
    // for (PlayerMatch player : match.getPlayers()) {
    // stmt.setInt(1, match.getId());
    // stmt.setInt(2, player.getPlayer().getId());
    // stmt.setString(3, player.getStatus());
    // stmt.setInt(4, player.getPoints());
    // stmt.setString(5, player.getRole());
    // stmt.addBatch();
    // }
    // stmt.executeBatch();
    // }

    // // // === 2. Luôn tăng total_matches ===
    // // try (PreparedStatement stmt = con.prepareStatement(sqlUpdateMatches)) {
    // // for (Player player : match.getPlayers()) {
    // // stmt.setInt(1, player.getPlayer().getId());
    // // stmt.addBatch();
    // // }
    // // stmt.executeBatch();
    // // }

    // // // === 3. Chỉ PvP mới update điểm & thắng ===
    // // if ("PVP".equalsIgnoreCase(match.getType())) {
    // // for (Player player : match.getPlayers()) {
    // // updateUserStatsPvP(sqlUpdateUser, player.getPlayer().getId(),
    // player.getStatus());
    // // }
    // // }

    // con.commit();
    // } catch (SQLException e) {
    // try {
    // con.rollback();
    // } catch (SQLException ex) {
    // ex.printStackTrace();
    // }
    // e.printStackTrace();
    // } finally {
    // try {
    // con.setAutoCommit(true);
    // } catch (SQLException e) {
    // e.printStackTrace();
    // }
    // }
    // }

}
