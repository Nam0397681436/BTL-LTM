package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DAO {
    private static String url() {
        return System.getProperty(
                "DB_URL",
                "jdbc:mysql://127.0.0.1:3306/btl?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&useUnicode=true&characterEncoding=utf8"
        );
    }
    private static String user() { return System.getProperty("DB_USER", "root"); }
    private static String pass() { return System.getProperty("DB_PASS", ""); }

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(url(), user(), pass());
    }

    public Connection getConnection() throws SQLException { return get(); }
}
