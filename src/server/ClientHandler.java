package server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dao.MatchDAO;
import dao.PlayerDAO;
import dao.PlayerMatchDAO;
import model.Match;
import model.Player;
import model.PlayerMatch;
import model.RoundResult;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Xử lý 1 client TCP.
 * Chuẩn I/O của nhóm: JsonObject -> toString() -> gửi String.
 * Phía nhận: nhận String -> parse JsonObject -> xử lý.
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ObjectInputStream in; // nhận String
    private final ObjectOutputStream out; // gửi String

    private final PlayerDAO dao = new PlayerDAO();
    private Player me;

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;

        // QUAN TRỌNG: tạo ObjectOutputStream trước, flush -> rồi mới tạo
        // ObjectInputStream
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    private void broadcastOnlinePlayers() {
        try {
            JsonArray arr = new JsonArray();
            for (var p : OnlineRegistry.all()) {
                JsonObject o = new JsonObject();
                o.addProperty("playerId", p.getPlayerId());
                o.addProperty("nickname", p.getNickname());
                // o.addProperty("totalScore", p.getTotalScore());
                String status = OnlineRegistry.isOnline(p.getPlayerId())
                        ? (OnlineRegistry.statusOf(p.getPlayerId()) == OnlineRegistry.Status.IN_MATCH
                                ? "IN_MATCH"
                                : "ONLINE")
                        : "OFFLINE";
                o.addProperty("status", status);
                arr.add(o);
            }
            JsonObject out = new JsonObject();
            out.addProperty("type", "ONLINE_USERS");
            out.add("players", arr);
            var message = server.JsonUtil.toJson(out);

            for (var clientOut : OnlineRegistry.getClients()) {
                if (clientOut != null) {
                    clientOut.writeObject(message);
                    clientOut.flush();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                Object obj = in.readObject(); // nhận 1 object
                if (obj == null)
                    break;
                if (obj instanceof String line) {
                    handle(line); // parse JSON & xử lý
                }
            }
        } catch (EOFException eof) {
            // client đóng kết nối bình thường
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (me != null)
                OnlineRegistry.remove(me.getPlayerId());
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

    private void handle(String line) {
        try {
            JsonObject msg = server.JsonUtil.fromJson(line, JsonObject.class);
            String type = msg.has("type") ? msg.get("type").getAsString() : null;
            if (type == null) {
                send(err("MISSING_TYPE"));
                return;
            }

            switch (type) {
                case "PING" -> {
                    JsonObject o = new JsonObject();
                    o.addProperty("type", "PONG");
                    send(o);
                }

                // ---------------- AUTH ----------------
                case "AUTH_LOGIN" -> {
                    String u = msg.get("username").getAsString();
                    String p = msg.get("password").getAsString();
                    var op = dao.login(u, p);
                    if (op.isEmpty()) {
                        send(err("Sai username/password"));
                        break;
                    }
                    me = op.get();
                    JsonObject ok = new JsonObject();
                    OnlineRegistry.add(me, out);
                    ok.addProperty("type", "AUTH_OK");
                    ok.addProperty("playerId", me.getPlayerId());
                    ok.addProperty("nickname", me.getNickname());
                    send(ok);
                    broadcastOnlinePlayers();
                }

                case "AUTH_REGISTER" -> {
                    String u = msg.get("username").getAsString();
                    String p = msg.get("password").getAsString();
                    String n = msg.get("nickname").getAsString();
                    if (dao.usernameExists(u)) {
                        send(err("Username đã tồn tại"));
                        break;
                    }
                    me = dao.register(u, p, n);
                    OnlineRegistry.add(me, out);
                    JsonObject ok = new JsonObject();
                    ok.addProperty("type", "AUTH_OK");
                    ok.addProperty("playerId", me.getPlayerId());
                    ok.addProperty("nickname", me.getNickname());
                    send(ok);
                    broadcastOnlinePlayers();
                }

                // -------------- DANH SÁCH NGƯỜI CHƠI --------------
                case "LIST_PLAYERS" -> {
                    var all = dao.listAllPlayers();
                    JsonArray arr = new JsonArray();
                    for (var p : all) {
                        JsonObject o = new JsonObject();
                        o.addProperty("playerId", p.getPlayerId());
                        o.addProperty("nickname", p.getNickname());
                        o.addProperty("totalScore", p.getTotalScore());
                        String status = OnlineRegistry.isOnline(p.getPlayerId())
                                ? (OnlineRegistry.statusOf(p.getPlayerId()) == OnlineRegistry.Status.IN_MATCH
                                        ? "IN_MATCH"
                                        : "ONLINE")
                                : "OFFLINE";
                        o.addProperty("status", status);
                        arr.add(o);
                    }
                    JsonObject out = new JsonObject();
                    out.addProperty("type", "PLAYERS_LIST");
                    out.add("players", arr);
                    send(out);
                }

                // ------------------- BXH -------------------
                case "GET_LEADERBOARD" -> {
                    int limit = msg.has("limit") ? msg.get("limit").getAsInt() : 50;
                    var rows = dao.leaderboard(limit);
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

                // ---------------- LỊCH SỬ ----------------
                case "GET_HISTORY" -> {
                    int limit = msg.has("limit") ? msg.get("limit").getAsInt() : 50;
                    if (me == null) {
                        send(err("NOT_AUTH"));
                        break;
                    }
                    var rows = dao.history(me.getPlayerId(), limit);
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

                // ---------------- LOGOUT ----------------
                case "LOGOUT" -> {
                    if (me != null)
                        OnlineRegistry.remove(me.getPlayerId());
                    JsonObject out = new JsonObject();
                    out.addProperty("type", "LOGOUT_OK");
                    send(out);
                    try {
                        socket.close();
                    } catch (IOException ignore) {
                    }
                }

                // ------------- MỜI THÁCH ĐẤU (ACK tạm) -------------
                case "INVITE_PVP" -> {
                    JsonObject out = new JsonObject();
                    out.addProperty("type", "INVITE_ACK");
                    out.addProperty("ok", true);
                    send(out);
                }

                case "GET_ONLINE_USERS" -> {
                    if (me == null) {
                        send(err("NOT_AUTH"));
                        break;
                    }
                    var players = new ArrayList<>(OnlineRegistry.all());
                    JsonArray arr = new JsonArray();
                    for (var p : players) {
                        if (p.getPlayerId().equals(me.getPlayerId()))
                            continue; // bỏ qua mình
                        JsonObject o = new JsonObject();
                        o.addProperty("playerId", p.getPlayerId());
                        o.addProperty("nickname", p.getNickname());
                        String status = OnlineRegistry.isOnline(p.getPlayerId())
                                ? (OnlineRegistry.statusOf(p.getPlayerId()) == OnlineRegistry.Status.IN_MATCH
                                        ? "IN_MATCH"
                                        : "ONLINE")
                                : "OFFLINE";
                        o.addProperty("status", status);
                        arr.add(o);
                    }
                    JsonObject out = new JsonObject();
                    out.addProperty("type", "ONLINE_USERS");
                    out.add("players", arr);
                    send(out);
                }

                case "CREATE_MULTIPLAYER_ROOM" -> {
                    if (me == null) {
                        send(err("NOT_AUTH"));
                        break;
                    }
                    Match match = new Match();
                    match.setType(model.Match.MatchType.MULTIPLAYER);

                    List<PlayerMatch> pms = new ArrayList<>();
                    PlayerMatch pm = new PlayerMatch();
                    pm.setPlayer(me);
                    pm.setHost(true);
                    pm.setScore(0);
                    pm.setWinner(false);
                    pms.add(pm);
                    match.setPlayers(pms);

                    MatchDAO mdao = new MatchDAO();
                    mdao.create(match);

                    MatchRooms.add(match);
                    OnlineRegistry.setStatus(me.getPlayerId(), OnlineRegistry.Status.IN_MATCH);

                    JsonObject out = new JsonObject();
                    out.addProperty("type", "CREATE_MULTIPLAYER_ROOM_ACK");
                    out.addProperty("match", match.toString());
                    out.addProperty("me", me.toString());
                    send(out);
                    broadcastOnlinePlayers();
                }

                case "INVITE_USER" -> {
                    if (me == null) {
                        send(err("NOT_AUTH"));
                        break;
                    }
                    String toPlayerId = msg.get("toPlayerId").getAsString();
                    ObjectOutputStream targetOut = OnlineRegistry.clientOf(toPlayerId);
                    if (targetOut != null) {
                        var m = new JsonObject();
                        m.addProperty("type", "INVITE_USER");
                        m.addProperty("fromPlayer", me.toString());
                        m.addProperty("matchId", msg.get("matchId").getAsInt());
                        targetOut.writeObject(JsonUtil.toJson(m));
                        targetOut.flush();
                    } else {
                        send(err("Người chơi không online"));
                    }
                }

                case "INVITE_ACCEPT" -> {
                    if (me == null) {
                        send(err("NOT_AUTH"));
                        break;
                    }
                    int matchId = msg.get("matchId").getAsInt();
                    Match match = MatchRooms.get(matchId);
                    if (match == null) {
                        send(err("MATCH_NOT_FOUND"));
                        break;
                    }
                    List<PlayerMatch> players = match.getPlayers();
                    boolean alreadyIn = players.stream()
                            .anyMatch(p -> p.getPlayer().getPlayerId().equals(me.getPlayerId()));
                    if (alreadyIn) {
                        send(err("ALREADY_IN_MATCH"));
                        break;
                    }
                    List<PlayerMatch> pms = match.getPlayers();
                    PlayerMatch pm = new PlayerMatch();
                    pm.setPlayer(me);
                    pm.setHost(false);
                    pm.setScore(0);
                    pm.setWinner(false);
                    pms.add(pm);
                    match.setPlayers(pms);
                    MatchRooms.update(match);

                    OnlineRegistry.setStatus(me.getPlayerId(), OnlineRegistry.Status.IN_MATCH);

                    // Thông báo cho tất cả thành viên trong phòng
                    for (var player : players) {
                        Player user = player.getPlayer();
                        ObjectOutputStream targetOut2 = OnlineRegistry.clientOf(user.getPlayerId());
                        if (targetOut2 != null) {
                            var m = new JsonObject();
                            m.addProperty("type", "MATCH_UPDATED");
                            m.addProperty("match", match.toString());
                            if (player.getPlayer().getPlayerId().equals(me.getPlayerId()))
                                m.addProperty("me", me.toString());
                            targetOut2.writeObject(JsonUtil.toJson(m));
                            targetOut2.flush();
                        }
                    }
                    broadcastOnlinePlayers();
                }

                case "INVITE_DECLINE" -> {
                    if (me == null) {
                        send(err("NOT_AUTH"));
                        break;
                    }
                    String toPlayerId = msg.get("toPlayerId").getAsString();
                    ObjectOutputStream targetOut = OnlineRegistry.clientOf(toPlayerId);
                    if (targetOut != null) {
                        var m = new JsonObject();
                        m.addProperty("type", "INVITE_DECLINED");
                        m.addProperty("fromPlayer", me.toString());
                        targetOut.writeObject(JsonUtil.toJson(m));
                        targetOut.flush();
                    } else {
                        send(err("Người chơi không online"));
                    }
                }

                case "EXIT_GAME" -> {
                    Integer matchId = Integer.parseInt(msg.get("matchId").getAsString());
                    Match match = MatchRooms.get(matchId);
                    if (match != null) {
                        PlayerMatch host = match.getHost();
                        List<PlayerMatch> players = match.getPlayers();
                        OnlineRegistry.setStatus(me.getPlayerId(), OnlineRegistry.Status.IDLE);
                        players.removeIf(p -> p.getPlayer().getPlayerId().equals(me.getPlayerId()));
                        match.setPlayers(players);
                        if (host != null && host.getPlayer().getPlayerId().equals(me.getPlayerId())) {
                            for (PlayerMatch player : players) {
                                Player user = player.getPlayer();
                                OnlineRegistry.setStatus(user.getPlayerId(), OnlineRegistry.Status.IDLE);
                                ObjectOutputStream targetOut = OnlineRegistry.clientOf(user.getPlayerId());
                                if (targetOut != null) {
                                    var m = new JsonObject();
                                    m.addProperty("type", "EXIT_GAME_HOST");
                                    targetOut.writeObject(JsonUtil.toJson(m));
                                }
                            }
                            MatchDAO matchDAO = new MatchDAO();
                            matchDAO.deletedMatch(match);
                            MatchRooms.remove(matchId);
                        } else {
                            for (PlayerMatch player : players) {
                                Player user = player.getPlayer();
                                ObjectOutputStream targetOut = OnlineRegistry.clientOf(user.getPlayerId());
                                if (targetOut != null) {
                                    var m = new JsonObject();
                                    m.addProperty("type", "EXIT_GAME_GUEST");
                                    m.addProperty("match", match.toString());
                                    targetOut.writeObject(JsonUtil.toJson(m));
                                }
                            }
                        }
                        var v = new JsonObject();
                        v.addProperty("type", "EXIT_GAME_ME");
                        out.writeObject(JsonUtil.toJson(v));
                        out.flush();
                        broadcastOnlinePlayers();
                    }
                }
                case "START_GAME" -> {
                    Integer matchId = Integer.parseInt(msg.get("matchId").getAsString());
                    Match match = MatchRooms.get(matchId);
                    String code = GameCodeManager.createGameCode(matchId, 1);
                    if (match == null) {
                        send(err("MATCH_NOT_FOUND"));
                        break;
                    }
                    for (PlayerMatch player : match.getPlayers()) {
                        Player user = player.getPlayer();
                        ObjectOutputStream targetOut = OnlineRegistry.clientOf(user.getPlayerId());
                        if (targetOut != null) {
                            var m = new JsonObject();
                            m.addProperty("type", "GAME_STARTED");
                            m.addProperty("match", match.toString());
                            m.addProperty("code", code);
                            targetOut.writeObject(JsonUtil.toJson(m));
                            targetOut.flush();
                        }
                    }
                }

                case "SUBMIT_ANSWER" -> {
                    if (me == null) {
                        send(err("NOT_AUTH"));
                        break;
                    }
                    int matchId = msg.get("matchId").getAsInt();
                    String answer = msg.get("answer").getAsString();
                    Match match = MatchRooms.get(matchId);
                    if (match != null) {
                        synchronized (match) {
                            List<PlayerMatch> players = match.getPlayers();

                            // 1. Cập nhật input cho player hiện tại
                            players.stream()
                                    .filter(p -> p.getPlayer().getPlayerId().equals(me.getPlayerId()))
                                    .forEach(p -> {
                                        p.setInputAnswer(answer != null ? answer.toUpperCase() : "");
                                        // p.setStatus("checking");
                                    });

                            // 2. Kiểm tra xem tất cả players đã nhập chưa
                            boolean allInputReady = players.stream().allMatch(p -> p.getInputAnswer() != null);

                            if (allInputReady) {
                                // 3. Tính điểm cho từng player
                                List<RoundResult> roundResults = new ArrayList<>();
                                for (PlayerMatch p : players) {
                                    int score = GameCodeManager.calculateSimilarityScore(p.getInputAnswer(),
                                            match.getMatchId());
                                    p.setScore(p.getScore() + score);
                                    roundResults.add(new RoundResult(p.getPlayer(), p.getInputAnswer(), score));
                                }

                                // 5. Gửi kết quả cho tất cả players
                                for (PlayerMatch p : players) {
                                    ObjectOutputStream pw = OnlineRegistry.clientOf(p.getPlayer().getPlayerId());
                                    if (pw != null) {
                                        var m = new JsonObject();
                                        m.addProperty("type", "ROUND_RESULT");
                                        m.addProperty("match", match.toString());
                                        var results = new JsonArray();
                                        for (RoundResult rr : roundResults) {
                                            results.add(rr.toString());
                                        }
                                        m.add("results", results);
                                        pw.writeObject(JsonUtil.toJson(m));
                                        pw.flush();
                                    }
                                }

                                // 6. Reset input cho round tiếp theo
                                players.forEach(p -> p.setInputAnswer(null));
                            }
                        }
                    }

                }

                case "REQUEST_NEXT_ROUND" -> {
                    if (me == null) {
                        send(err("NOT_AUTH"));
                        break;
                    }
                    int matchId = msg.get("matchId").getAsInt();
                    int round = msg.get("currentRound").getAsInt();
                    Match match = MatchRooms.get(matchId);
                    if (match != null) {
                        synchronized (match) {
                            round++;
                            List<PlayerMatch> players = match.getPlayers();
                            // 1. Cập nhật trạng thái ready cho player hiện tại
                            players.stream()
                                    .forEach(p -> p.setInputAnswer(null));
                            // 3. Gửi lệnh bắt đầu round mới cho tất cả players
                            String code = GameCodeManager.createGameCode(match.getMatchId(), round);
                            for (PlayerMatch p : players) {
                                ObjectOutputStream pw = OnlineRegistry.clientOf(p.getPlayer().getPlayerId());
                                if (pw != null) {
                                    var m = new JsonObject();
                                    m.addProperty("type", "START_NEXT_ROUND");
                                    m.addProperty("code", code);
                                    m.addProperty("nextRound", round);
                                    pw.writeObject(JsonUtil.toJson(m));
                                    pw.flush();
                                }
                            }
                            // 4. Reset trạng thái ready cho round tiếp theo
                        }
                    }
                }

                case "LEAVE_GAME" -> {
                    int matchId = msg.get("matchId").getAsInt();
                    Match match = MatchRooms.get(matchId);
                    if (match != null) {
                        synchronized (match) {
                            List<PlayerMatch> players = match.getPlayers();
                            PlayerMatch leavingPlayer = players.stream()
                                    .filter(p -> p.getPlayer().getPlayerId().equals(me.getPlayerId()))
                                    .findFirst()
                                    .orElse(null);

                            if (leavingPlayer != null) {
                                PlayerMatchDAO pmDAO = new PlayerMatchDAO();
                                pmDAO.create(leavingPlayer, match.getMatchId());

                                players.removeIf(p -> p.getPlayer().getPlayerId().equals(me.getPlayerId()));
                                match.setPlayers(players);
                            }

                            OnlineRegistry.setStatus(me.getPlayerId(), OnlineRegistry.Status.IDLE);
                            try {
                                if (players.size() > 2) {
                                    for (PlayerMatch p : players) {
                                        if (!p.getPlayer().getPlayerId().equals(me.getPlayerId())) {
                                            ObjectOutputStream pw = OnlineRegistry
                                                    .clientOf(p.getPlayer().getPlayerId());
                                            if (pw != null) {
                                                var m = new JsonObject();
                                                m.addProperty("type", "LEAVE_GAME_OTHER");
                                                m.addProperty("match", match.toString());
                                                pw.writeObject(JsonUtil.toJson(m));
                                                pw.flush();
                                            }
                                        }
                                    }
                                } else {
                                    PlayerMatch pm = players.get(0);
                                    pm.setWinner(true);
                                    PlayerMatchDAO pmDAO = new PlayerMatchDAO();
                                    pmDAO.create(pm, match.getMatchId());
                                    OnlineRegistry.setStatus(pm.getPlayer().getPlayerId(), OnlineRegistry.Status.IDLE);
                                    ObjectOutputStream pw = OnlineRegistry.clientOf(pm.getPlayer().getPlayerId());
                                    if (pw != null) {
                                        var m = new JsonObject();
                                        m.addProperty("type", "FINAL_RESULTS");
                                        m.addProperty("match", match.toString());
                                        pw.writeObject(JsonUtil.toJson(m));
                                        pw.flush();
                                    }
                                    MatchRooms.remove(matchId);
                                    GameCodeManager.removeGameCode(matchId);
                                    broadcastOnlinePlayers();
                                }
                                var v = new JsonObject();
                                v.addProperty("type", "LEAVE_GAME_ME");
                                out.writeObject(JsonUtil.toJson(v));
                                out.flush();

                            } catch (IOException ex) {
                                System.out.println("Error sending LEAVE_GAME messages: " + ex.getMessage());
                                ex.printStackTrace();
                            }
                            broadcastOnlinePlayers();
                        }
                    } else {
                        System.out.println("Match not found for ID: " + matchId);
                    }
                }

                case "REQUEST_FINAL_RESULTS" -> {
                    int matchId = msg.get("matchId").getAsInt();
                    Match match = MatchRooms.get(matchId);
                    if (match != null) {
                        synchronized (match) {
                            List<PlayerMatch> players = match.getPlayers();
                            if (match.getFinalResultsSent()) {
                                return;
                            }
                            match.setFinalResultsSent(true);
                            int maxScore = players.stream()
                                    .mapToInt(PlayerMatch::getScore)
                                    .max()
                                    .orElse(0);

                            for (PlayerMatch p : players) {
                                if (p.getScore() == maxScore) {
                                    p.setWinner(true);
                                } else {
                                    p.setWinner(false);
                                }
                            }
                            PlayerMatchDAO pmDAO = new PlayerMatchDAO();
                            for (PlayerMatch p : players) {
                                pmDAO.create(p, match.getMatchId());
                                OnlineRegistry.setStatus(p.getPlayer().getPlayerId(), OnlineRegistry.Status.IDLE);
                            }
                            for (PlayerMatch p : players) {
                                ObjectOutputStream pw = OnlineRegistry.clientOf(p.getPlayer().getPlayerId());
                                if (pw != null) {
                                    var m = new JsonObject();
                                    m.addProperty("type", "FINAL_RESULTS");
                                    m.addProperty("match", match.toString());
                                    pw.writeObject(JsonUtil.toJson(m));
                                    pw.flush();
                                }
                            }
                            // Xóa match khỏi danh sách phòng
                            MatchRooms.remove(matchId);
                            GameCodeManager.removeGameCode(matchId);
                            broadcastOnlinePlayers();
                        }
                    }
                }

                default -> send(err("Unknown type: " + type));
            }
        } catch (Exception e) {
            e.printStackTrace();
            send(err("SERVER_ERROR: " + e.getMessage()));
        }
    }

    /** Gửi: JsonObject -> String -> ObjectOutputStream */
    private void send(JsonObject o) {
        try {
            out.writeObject(server.JsonUtil.toJson(o)); // gửi String JSON
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
