package client.net;

import java.io.*;
import java.net.Socket;

public class TcpClient {

    public interface Listener { void onMessage(String line); }

    private Socket socket;
    private ObjectOutputStream out;   // gửi String
    private ObjectInputStream in;     // nhận String

    public void connect(String host, int port, Listener listener) throws IOException {
        socket = new Socket(host, port);

        // QUAN TRỌNG: tạo ObjectOutputStream trước, flush để handshake
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());

        Thread reader = new Thread(() -> {
            try {
                while (!socket.isClosed()) {
                    Object obj = in.readObject();      // nhận Object
                    if (obj == null) break;
                    if (obj instanceof String s) {
                        if (listener != null) listener.onMessage(s);  // chuyển cho UI/router
                    }
                }
            } catch (EOFException eof) {
                // socket đóng bình thường
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                closeQuietly();
            }
        }, "TcpClient-Reader");
        reader.setDaemon(true);
        reader.start();
    }

    /** Gửi chuỗi JSON (đã toString() từ JsonObject). */
    public synchronized void send(String json) throws IOException {
        if (out == null) throw new IOException("Not connected");
        out.writeObject(json);
        out.flush();
    }

    public void close() { closeQuietly(); }

    private void closeQuietly() {
        try { if (in != null) in.close(); } catch (Exception ignored) {}
        try { if (out != null) out.close(); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
    }
}
