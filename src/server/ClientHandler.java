package server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dao.PlayerDAO;
import model.Player;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final BufferedReader in;
    private final BufferedWriter out;

    private final PlayerDAO dao = new PlayerDAO();
    private Player me;

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    @Override public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) handle(line);
        } catch (IOException ignore) {
        } finally {
            if (me != null) OnlineRegistry.remove(me.getPlayerId());
            try { in.close(); }  catch (Exception ignore2) {}
            try { out.close(); } catch (Exception ignore2) {}
            try { socket.close(); } catch (Exception ignore2) {}
        }
    }

    private void handle(String line) {
        try {
            JsonObject msg = server.JsonUtil.fromJson(line, JsonObject.class);
            String type = msg.has("type") ? msg.get("type").getAsString() : null;
            if (type == null) { send(err("MISSING_TYPE")); return; }

            switch (type) {
                case "PING" -> { var o = new JsonObject(); o.addProperty("type","PONG"); send(o); }

                /* ---------- Đăng nhập ---------- */
                case "AUTH_LOGIN" -> {
                    String u = msg.get("username").getAsString();
                    String p = msg.get("password").getAsString();
                    System.out.println("[AUTH_LOGIN] u=" + u);
                    model.Player op = dao.login(u, p);
                    if (p == null || p.isBlank() || u == null || u.isBlank()) { send(err("Sai username hoặc mật khẩu")); break; }
                    me = op;
                    OnlineRegistry.add(me);
                    OnlineRegistry.bindSession(me.getPlayerId(), this);
                    var ok = new JsonObject();
                    ok.addProperty("type", "AUTH_OK");
                    ok.addProperty("playerId", me.getPlayerId());
                    ok.addProperty("nickname", me.getNickname());
                    send(ok);
                }
                /* ---------- Đăng kí ---------- */
                case "AUTH_REGISTER" -> {
                    String u = msg.get("username").getAsString();
                    String p = msg.get("password").getAsString();
                    String n = msg.get("nickname").getAsString();
                    System.out.println("[AUTH_REGISTER] u=" + u + ", nick=" + n);
                    try {
                        me = dao.register(u, p, n);
                    } catch (SQLException ex) {
                        String m = ex.getMessage();
                        String reason;
                        if ("USERNAME_EXISTS".equals(m))       reason = "Username đã tồn tại";
                        else if ("NICKNAME_EXISTS".equals(m))  reason = "Nickname đã tồn tại";
                        else                                   reason = "Đăng ký thất bại";
                        send(err(reason));
                        break;
                    }
                    OnlineRegistry.add(me);
                    OnlineRegistry.bindSession(me.getPlayerId(), this);
                    var ok = new JsonObject();
                    ok.addProperty("type", "AUTH_OK");
                    ok.addProperty("playerId", me.getPlayerId());
                    ok.addProperty("nickname", me.getNickname());
                    send(ok);
                }

                /* ---------- CHỈ TRẢ NGƯỜI ĐANG ONLINE ---------- */
                case "LIST_PLAYERS" -> {
                    JsonArray arr = new JsonArray();
                    String self = (me == null) ? null : me.getPlayerId();
                    int cnt = 0;
                    for (Player p : OnlineRegistry.onlinePlayers()) {
                        if (self != null && self.equals(p.getPlayerId())) continue;
                        JsonObject o = new JsonObject();
                        o.addProperty("playerId", p.getPlayerId());
                        o.addProperty("nickname", p.getNickname());
                        arr.add(o);
                        cnt++;
                    }
                    System.out.println("[LIST_PLAYERS] online=" + cnt);
                    JsonObject out = new JsonObject();
                    out.addProperty("type", "PLAYERS_LIST");
                    out.add("players", arr);
                    send(out);
                }

                /* ---------- BXH ---------- */
                case "GET_LEADERBOARD" -> {
                    int limit = msg.has("limit") ? msg.get("limit").getAsInt() : 50;
                    var rows = dao.getLeaderboard(limit);
                    JsonArray arr = new JsonArray();
                    for (var p : rows) {
                        JsonObject o = new JsonObject();
                        o.addProperty("nickname", p.getNickname());
                        o.addProperty("totalScore", p.getTotalScore());
                        o.addProperty("totalWins", p.getTotalWins());
                        arr.add(o);
                    }
                    JsonObject out = new JsonObject();
                    out.addProperty("type", "LEADERBOARD");
                    out.add("rows", arr);
                    send(out);
                }

                /* ---------- HISTORY ---------- */
                case "GET_HISTORY" -> {
                    int limit = msg.has("limit") ? msg.get("limit").getAsInt() : 50;
                    if (me == null) { send(err("NOT_AUTH")); break; }
                    
                    var rows = dao.getHistory(me.getPlayerId(), limit);
                    var arr = new com.google.gson.JsonArray();

                    for (var r : rows) {
                        JsonObject o = new JsonObject();
                        o.addProperty("matchId", ((Number) r.get("matchId")).intValue());
                        o.addProperty("mode", String.valueOf(r.get("mode")));
                        o.addProperty("startTime", String.valueOf(r.get("startTime")));
                        o.addProperty("endTime", String.valueOf(r.get("endTime")));
                        o.addProperty("score", ((Number) r.get("score")).intValue());
                        
                        String result = String.valueOf(r.get("isWinner"));
                        o.addProperty("result", result);
                        o.addProperty("isWinner", "WIN".equalsIgnoreCase(result));
                        
                        arr.add(o);
                    }
                    JsonObject out = new JsonObject();
                    out.addProperty("type", "HISTORY");
                    out.add("rows", arr);
                    send(out);
                }

                /* ---------- LOGOUT ---------- */
                case "LOGOUT" -> {
                    if (me != null) OnlineRegistry.remove(me.getPlayerId());
                    var out = new JsonObject(); out.addProperty("type","LOGOUT_OK"); send(out);
                    try { socket.close(); } catch (IOException ignore) {}
                }

                default -> send(err("Unknown type: " + type));
            }
        } catch (Exception e) {
            e.printStackTrace();
            send(err("SERVER_ERROR: " + e.getMessage()));
        }
    }

    /* gửi JSON NDJSON */
    public synchronized void send(JsonObject o) {
        try {
            out.write(server.JsonUtil.toJson(o));
            out.write("\n");
            out.flush();
        } catch (IOException ignore) {}
    }

    private static JsonObject err(String msg) {
        var o = new JsonObject();
        o.addProperty("type", "AUTH_ERR");
        o.addProperty("reason", msg);
        return o;
    }
}
