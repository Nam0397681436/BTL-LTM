package server;

import dao.DAO;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {
    public static void main(String[] args) throws Exception {
        // Kiểm tra DB trước khi mở port
        try (var c = DAO.get()) {
            System.out.println("DB OK: " + c.getMetaData().getURL());
        } catch (Exception e) {
            System.err.println("DB FAIL: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        int port = Integer.parseInt(System.getProperty("PORT", "5555"));
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Server listening on " + port);
            while (true) {
                Socket s = server.accept();
                new Thread(new ClientHandler(s), "ClientHandler").start();
            }
        }
    }
}
