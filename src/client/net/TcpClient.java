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
        socket = new Socket(Proxy.NO_PROXY); // tránh SOCKS proxy
        socket.connect(new InetSocketAddress(host, port), 4000);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    public synchronized void send(String jsonLine) throws IOException {
        if (socket == null || socket.isClosed())
            throw new IOException("Not connected");
        out.write(jsonLine);
        out.write("\n"); // NDJSON
        out.flush();
    }

    public String readLine() throws IOException {
        if (socket == null)
            throw new IOException("Not connected");
        return in.readLine();
    }

    public void close() {
        try {
            in.close();
        } catch (Exception ignore) {
        }
        try {
            out.close();
        } catch (Exception ignore) {
        }
        try {
            socket.close();
        } catch (Exception ignore) {
        }
    }
}
