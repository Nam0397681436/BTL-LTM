package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import model.PlayerMatch;

public class PlayerMatchDAO extends DAO {
    public PlayerMatchDAO() {
        super();
    }

    public void create(PlayerMatch pm, int matchId) {
        String sql = "INSERT INTO player_matches (player_id, match_id, score, status) VALUES (?, ?, ?, ?)";
        try (Connection connection = getConnection();) {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, pm.getPlayer().getPlayerId());
            stmt.setInt(2, matchId);
            stmt.setInt(3, pm.getScore());
            stmt.setString(4, pm.getStatus());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
