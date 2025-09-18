package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DAO {
    private static final String URL  =
        "jdbc:mysql://127.0.0.1:3306/game_service?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = System.getProperty("DB_USER", "game_user");
    private static final String PASS = System.getProperty("DB_PASS", "game123!");

    static {
        try { Class.forName("com.mysql.cj.jdbc.Driver"); }
        catch (ClassNotFoundException e) { throw new RuntimeException("Missing MySQL driver", e); }
    }

    /** Dùng ở mọi nơi (ServerMain, kiểm tra DB, v.v.) */
    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    /** Dùng trong các DAO con (PlayerDAO) */
    protected Connection getConnection() throws SQLException {
        return get();
    }

    /** SHA-256 tiện cho password */
    protected static String sha256(String s) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(out.length * 2);
            for (byte b : out) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
