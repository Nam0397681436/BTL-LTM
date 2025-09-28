package server;

import dao.DAO;
import java.net.ServerSocket;
import java.util.concurrent.Executors;

public class ServerMain {
    public static void main(String[] args) throws Exception {
        System.out.println("[BOOT] starting server...");
        // In cấu hình để tự kiểm tra khi lỗi Access denied
        System.out.println("[DBCONF] url=" + System.getProperty("DB_URL"));
        System.out.println("[DBCONF] user=" + System.getProperty("DB_USER"));
        System.out.println("[DBCONF] pass.len=" + (System.getProperty("DB_PASS") == null ? 0 : System.getProperty("DB_PASS").length()));

        try (var c = DAO.get()) {
            System.out.println("DB OK: " + c.getMetaData().getURL());
        } catch (Exception e) {
            System.err.println("DB FAIL: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        int port = Integer.getInteger("PORT", 5555);
        try (ServerSocket ss = new ServerSocket(port)) {
            System.out.println("Server started on 0.0.0.0:" + port);
            var pool = Executors.newCachedThreadPool();
            while (true) pool.submit(new ClientHandler(ss.accept()));
        }
    }
}
