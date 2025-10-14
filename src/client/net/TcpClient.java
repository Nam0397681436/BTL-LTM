package client.net;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class TcpClient {
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;

    public void connect() throws IOException {
        String host = System.getProperty("HOST", "127.0.0.1");
        int port = Integer.getInteger("PORT", 5555);
        System.out.println("[CONNECT] host=" + host + " port=" + port);
        
        // Đóng kết nối cũ nếu có
        close();
        
        socket = new Socket(Proxy.NO_PROXY); // tránh SOCKS proxy
        //socket.setSoTimeout(30000); // Timeout 30 giây cho read
        socket.connect(new InetSocketAddress(host, port), 10000); // Timeout 10 giây cho connect
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        
        System.out.println("[CONNECT] Connected successfully");
    }

    public synchronized void send(String jsonLine) throws IOException {
        if (socket == null || socket.isClosed())
            throw new IOException("Not connected");
        out.write(jsonLine);
        out.write("\n");
        out.flush();
    }

    public String readLine() throws IOException {
        if (socket == null)
            throw new IOException("Not connected");
        return in.readLine();
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected();
    }

    public void close() {
        try {
            if (in != null) in.close();
        } catch (Exception ignore) {
        }
        try {
            if (out != null) out.close();
        } catch (Exception ignore) {
        }
        try {
            if (socket != null) socket.close();
        } catch (Exception ignore) {
        }
        in = null;
        out = null;
        socket = null;
    }
}
