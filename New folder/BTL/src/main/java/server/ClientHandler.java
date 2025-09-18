package server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dao.PlayerDAO;
import model.Player;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ObjectInputStream in;     // nhận String
    private final ObjectOutputStream out;   // gửi String

    private final PlayerDAO dao = new PlayerDAO();
    private Player me;

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;

        // QUAN TRỌNG: tạo OOS trước, flush, rồi OIS (match với client)
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in  = new ObjectInputStream(socket.getInputStream());
    }

    @Override public void run() {
        try {
            while (true) {
                Object obj = in.readObject();     // nhận chuỗi JSON
                if (obj == null) break;
                if (obj instanceof String line) {
                    handle(line);                 // parse ngược lại -> JsonObject
                }
            }
        } catch (EOFException eof) {
            // client đóng kết nối
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (me != null) OnlineRegistry.remove(me.playerId);
            try { in.close(); } catch (Exception ignore) {}
            try { out.close(); } catch (Exception ignore) {}
            try { socket.close(); } catch (Exception ignore) {}
        }
    }

    private void handle(String line) {
        try {
            JsonObject msg = server.JsonUtil.fromJson(line, JsonObject.class);
            String type = msg.has("type") ? msg.get("type").getAsString() : null;
            if (type == null) { send(err("MISSING_TYPE")); return; }

            switch (type) {
                case "PING" -> {
                    JsonObject o = new JsonObject();
                    o.addProperty("type", "PONG");
                    send(o);
                }

                // ---------- AUTH ----------
                case "AUTH_LOGIN" -> {
                    String u = msg.get("username").getAsString();
                    String p = msg.get("password").getAsString();
                    var op = dao.login(u, p);
                    if (op.isEmpty()) { send(err("Sai username/password")); break; }
                    me = op.get();
                    OnlineRegistry.add(me);
                    JsonObject ok = new JsonObject();
                    ok.addProperty("type", "AUTH_OK");
                    ok.addProperty("playerId", me.playerId);
                    ok.addProperty("nickname", me.nickname);
                    send(ok);
                }

                case "AUTH_REGISTER" -> {
                    String u = msg.get("username").getAsString();
                    String p = msg.get("password").getAsString();
                    String n = msg.get("nickname").getAsString();
                    if (dao.usernameExists(u)) { send(err("Username đã tồn tại")); break; }
                    me = dao.register(u, p, n);
                    OnlineRegistry.add(me);
                    JsonObject ok = new JsonObject();
                    ok.addProperty("type", "AUTH_OK");
                    ok.addProperty("playerId", me.playerId);
                    ok.addProperty("nickname", me.nickname);
                    send(ok);
                }

                // ---------- DANH SÁCH NGƯỜI CHƠI ----------
                case "LIST_PLAYERS" -> {
                    var all = dao.listAllPlayers();
                    JsonArray arr = new JsonArray();
                    for (var p : all) {
                        JsonObject o = new JsonObject();
                        o.addProperty("playerId", p.playerId);
                        o.addProperty("nickname", p.nickname);
                        o.addProperty("totalScore", p.totalScore);
                        String status = OnlineRegistry.isOnline(p.playerId)
                                ? (OnlineRegistry.statusOf(p.playerId) == OnlineRegistry.Status.IN_MATCH ? "IN_MATCH" : "ONLINE")
                                : "OFFLINE";
                        o.addProperty("status", status);
                        arr.add(o);
                    }
                    JsonObject out = new JsonObject();
                    out.addProperty("type", "PLAYERS_LIST");
                    out.add("players", arr);
                    send(out);
                }

                // ---------- BXH ----------
                case "GET_LEADERBOARD" -> {
                    int limit = msg.has("limit") ? msg.get("limit").getAsInt() : 50;
                    var rows = dao.leaderboard(limit);
                    JsonArray arr = new JsonArray();
                    for (var p : rows) {
                        JsonObject o = new JsonObject();
                        o.addProperty("nickname", p.nickname);
                        o.addProperty("totalScore", p.totalScore);
                        o.addProperty("totalWins", p.totalWins);
                        arr.add(o);
                    }
                    JsonObject out = new JsonObject();
                    out.addProperty("type", "LEADERBOARD");
                    out.add("rows", arr);
                    send(out);
                }

                // ---------- LỊCH SỬ ----------
                case "GET_HISTORY" -> {
                    int limit = msg.has("limit") ? msg.get("limit").getAsInt() : 50;
                    if (me == null) { send(err("NOT_AUTH")); break; }
                    var rows = dao.history(me.playerId, limit);
                    JsonArray arr = new JsonArray();
                    for (var r : rows) {
                        JsonObject o = new JsonObject();
                        o.addProperty("matchId", ((Number) r.get("matchId")).intValue());
                        o.addProperty("mode", String.valueOf(r.get("mode")));
                        o.addProperty("startTime", String.valueOf(r.get("startTime")));
                        o.addProperty("endTime", String.valueOf(r.get("endTime")));
                        o.addProperty("score", ((Number) r.get("score")).intValue());
                        o.addProperty("result", ((Boolean) r.get("isWinner")) ? "WIN" : "LOSE");
                        arr.add(o);
                    }
                    JsonObject out = new JsonObject();
                    out.addProperty("type", "HISTORY");
                    out.add("rows", arr);
                    send(out);
                }

                // ---------- LOGOUT ----------
                case "LOGOUT" -> {
                    if (me != null) OnlineRegistry.remove(me.playerId);
                    JsonObject out = new JsonObject();
                    out.addProperty("type", "LOGOUT_OK");
                    send(out);
                    try { socket.close(); } catch (IOException ignore) {}
                }

                // ---------- MỜI THÁCH ĐẤU (gửi theo chuẩn JSON String) ----------
                case "INVITE_PVP" -> {
                    // TODO: triển khai chuyển tiếp lời mời tới đối thủ (khi có registry session/socket mapping)
                    JsonObject out = new JsonObject();
                    out.addProperty("type", "INVITE_ACK");
                    out.addProperty("ok", true);
                    send(out);
                }

                default -> send(err("Unknown type: " + type));
            }
        } catch (Exception e) {
            e.printStackTrace();
            send(err("SERVER_ERROR: " + e.getMessage()));
        }
    }

    /** Gửi chuỗi JSON (JsonObject -> String) */
    private void send(JsonObject o) {
        try {
            out.writeObject(server.JsonUtil.toJson(o));  // gửi String
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static JsonObject err(String msg) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "AUTH_ERR");
        o.addProperty("reason", msg);
        return o;
    }
}
