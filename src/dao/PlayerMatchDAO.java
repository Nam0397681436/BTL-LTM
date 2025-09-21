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
        String sql = "INSERT INTO player_match (player_id, match_id, score, is_winner,is_host) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = getConnection();) {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, Integer.parseInt(pm.getPlayer().getPlayerId()));
            stmt.setInt(2, matchId);
            stmt.setInt(3, pm.getScore());
            stmt.setBoolean(4, pm.isWinner());
            stmt.setBoolean(5, pm.isHost());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
