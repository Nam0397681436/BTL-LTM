package server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dao.MatchDAO;
import dao.PlayerDAO;
import dao.PlayerMatchDAO;
import model.HandelMatchMulti;
import model.Player;
import model.PlayerMatch;
import model.PlayerStatus;
import model.RoundResult;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final BufferedReader in;
    private final BufferedWriter out;

    private final PlayerDAO dao = new PlayerDAO();
    private Player me;

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                handle(line);
            }
        } catch (IOException ignore) {
        } finally {
            try {
                if (me != null) {
                    OnlineRegistry.remove(me.getPlayerId());
                    // OnlineRegistry.unbindSession(me.getPlayerId());
                }
            } catch (Exception ignore2) {
            }
            try {
                in.close();
            } catch (Exception ignore2) {
            }
            try {
                out.close();
            } catch (Exception ignore2) {
            }
            try {
                socket.close();
            } catch (Exception ignore2) {
            }
        }
    }

    /** CHỈNH: helper check đăng nhập – đặt ở cấp class, không để trong handle() */
    private boolean allowWithoutAuth(String type) {
        return "AUTH_LOGIN".equals(type) || "AUTH_REGISTER".equals(type) || "PING".equals(type);
    }

    private void handle(String line) {
        try {
            JsonObject msg = server.JsonUtil.fromJson(line, JsonObject.class);
            String type = msg.has("type") ? msg.get("type").getAsString() : null;
            if (type == null) {
                send(err("MISSING_TYPE"));
                return;
            }

            // CHỈNH: chặn khi chưa đăng nhập cho các type cần auth
            if (!allowWithoutAuth(type) && me == null) {
                send(err("Sai tài khoản hoặc mật khẩu"));
                return;
            }

            switch (type) {
                case "PING" -> {
                    var o = new JsonObject();
                    o.addProperty("type", "PONG");
                    send(o);
                }

                /* ---------- Đăng nhập ---------- */
                case "AUTH_LOGIN" -> {
                    String username = msg.get("username").getAsString();
                    String password = msg.get("password").getAsString();

                    try {
                        Player found = dao.login(username, password);
                        if (found == null) {
                            send(err("Sai tài khoản hoặc mật khẩu"));
                            break;
                        }

                        // CHẶN ĐĂNG NHẬP TRÙNG
                        if (!OnlineRegistry.tryBindSession(found.getPlayerId(), this)) {
                            var o = new JsonObject();
                            o.addProperty("type", "AUTH_ERR");
                            o.addProperty("reason", "Tài khoản đang được đăng nhập ở nơi khác");
                            send(o);
                            break;
                        }

                        // Đăng nhập thành công
                        this.me = found;
                        OnlineRegistry.add(me); // thêm vào ONLINE + broadcast
                        OnlineRegistry.bindSession(me.getPlayerId(), this);
                        OnlineRegistry.sendOnlineSnapshotTo(me.getPlayerId());
                        OnlineRegistry.changeStatus(me.getPlayerId(), PlayerStatus.ONLINE);

                        var ok = new JsonObject();
                        ok.addProperty("type", "AUTH_OK");
                        ok.addProperty("playerId", me.getPlayerId());
                        ok.addProperty("nickname", me.getNickname());
                        send(ok);
                    } catch (SQLException ex) {
                        send(err("Lỗi CSDL: " + ex.getMessage()));
                    }
                }

                /* ---------- Đăng kí ---------- */
                case "AUTH_REGISTER" -> {
                    String u = msg.get("username").getAsString();
                    String p = msg.get("password").getAsString();
                    String n = msg.get("nickname").getAsString();
                    System.out.println("[AUTH_REGISTER] u=" + u + ", nick=" + n);
                    try {
                        this.me = dao.register(u, p, n);
                        me.setStatus(PlayerStatus.ONLINE);
                    } catch (SQLException ex) {
                        String m = ex.getMessage();
                        String reason;
                        if ("USERNAME_EXISTS".equals(m))
                            reason = "Username đã tồn tại";
                        else if ("NICKNAME_EXISTS".equals(m))
                            reason = "Nickname đã tồn tại";
                        else
                            reason = "Đăng ký thất bại";
                        send(err(reason));
                        break;
                    }
                    OnlineRegistry.add(me);
                    OnlineRegistry.changeStatus(me.getPlayerId(), PlayerStatus.ONLINE);
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
                        if (self != null && self.equals(p.getPlayerId()))
                            continue;
                        JsonObject o = new JsonObject();
                        o.addProperty("playerId", p.getPlayerId());
                        o.addProperty("nickname", p.getNickname());
                        o.addProperty("status", p.getStatus().toString() == "IN_GAME" ? "IN_GAME" : "ONLINE");
                        arr.add(o);
                        cnt++;
                    }
                    System.out.println("[LIST_PLAYERS] online=" + cnt);
                    JsonObject data = new JsonObject();
                    data.addProperty("type", "PLAYERS_LIST");
                    data.add("players", arr);
                    send(data);
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
                    JsonObject data = new JsonObject();
                    data.addProperty("type", "LEADERBOARD");
                    data.add("rows", arr);
                    send(data);
                }

                /* ---------- HISTORY ---------- */
                case "GET_HISTORY" -> {
                    int limit = msg.has("limit") ? msg.get("limit").getAsInt() : 50;
                    // me đã đảm bảo != null nhờ chặn ở đầu
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
                    JsonObject data = new JsonObject();
                    data.addProperty("type", "HISTORY");
                    data.add("rows", arr);
                    send(data);
                }

                /* ---------- LOGOUT ---------- */
                case "LOGOUT" -> {
                    if (me != null) {
                        OnlineRegistry.remove(me.getPlayerId());
                    }
                    var out = new JsonObject();
                    out.addProperty("type", "LOGOUT_OK");
                    send(out);
                    // try { socket.close(); } catch (IOException ignore) {}///
                }

                case "EXIT" -> {
                    if (me != null) {
                        OnlineRegistry.remove(me.getPlayerId());
                        OnlineRegistry.unbind(me.getPlayerId(), this);
                    }
                    try {
                        socket.close();
                    } catch (IOException ignore) {
                    }
                }

                /* ---------- THÁCH ĐẤU ---------- (dành cho Năm) */
                case "INVITE" -> {
                    String playerInvite = msg.get("PLAYER_INVITE").getAsString();
                    String targetPlayer = msg.get("PLAYER").getAsString();

                    // Kiểm tra người gửi có phải là người đang đăng nhập không
                    if (!playerInvite.equals(me.getPlayerId())) {
                        send(err("Không thể gửi thách đấu thay người khác"));
                        break;
                    }

                    // Kiểm tra đối phương có online không
                    if (!OnlineRegistry.isOnline(targetPlayer)) {
                        send(err("Đối phương không online"));
                        break;
                    }

                    // Gửi thách đấu cho đối phương
                    JsonObject challengeMsg = new JsonObject();
                    challengeMsg.addProperty("type", "INVITE");
                    challengeMsg.addProperty("PLAYER_INVITE", playerInvite);
                    challengeMsg.addProperty("PLAYER", targetPlayer);
                    challengeMsg.addProperty("inviterName", me.getNickname());

                    sendToPlayer(targetPlayer, challengeMsg);

                    // Thông báo cho người gửi rằng đã gửi thách đấu
                    JsonObject confirmMsg = new JsonObject();
                    confirmMsg.addProperty("type", "CHALLENGE_SENT");
                    confirmMsg.addProperty("targetPlayer", targetPlayer);
                    send(confirmMsg);
                }

                /* ---------- PHẢN HỒI THÁCH ĐẤU ---------- */
                case "START_GAME" -> {
                    String inviterId = msg.get("inviterId").getAsString();
                    boolean accepted = msg.get("accepted").getAsBoolean();

                    // Gửi phản hồi cho người gửi thách đấu
                    JsonObject responseMsg = new JsonObject();
                    responseMsg.addProperty("type", "START_GAME");
                    responseMsg.addProperty("inviterId", me.getPlayerId()); // ID của người chấp nhận
                    responseMsg.addProperty("accepted", accepted);
                    responseMsg.addProperty("opponentName", me.getNickname());

                    sendToPlayer(inviterId, responseMsg);
                }

                /* ---------- RỜI PHÒNG ĐẤU ---------- */
                case "LEAVE_MATCH" -> {
                    String opponentId = msg.get("opponentId").getAsString();

                    // Thông báo cho đối phương rằng người này đã rời phòng
                    JsonObject leaveMsg = new JsonObject();
                    leaveMsg.addProperty("type", "OPPONENT_LEFT");
                    leaveMsg.addProperty("playerId", me.getPlayerId());
                    leaveMsg.addProperty("playerName", me.getNickname());

                    sendToPlayer(opponentId, leaveMsg);

                    // Thông báo cho người này rằng đã rời phòng thành công
                    JsonObject confirmMsg = new JsonObject();
                    confirmMsg.addProperty("type", "LEFT_MATCH");
                    send(confirmMsg);
                }

                /* ---------- ĐẦU HÀNG ---------- */
                case "SURRENDER" -> {
                    String opponentId = msg.get("opponentId").getAsString();

                    // Thông báo cho đối phương rằng người này đã đầu hàng
                    JsonObject surrenderMsg = new JsonObject();
                    surrenderMsg.addProperty("type", "OPPONENT_SURRENDERED");
                    surrenderMsg.addProperty("playerId", me.getPlayerId());
                    surrenderMsg.addProperty("playerName", me.getNickname());

                    sendToPlayer(opponentId, surrenderMsg);

                    // Thông báo cho người này rằng đã đầu hàng
                    JsonObject confirmMsg = new JsonObject();
                    confirmMsg.addProperty("type", "SURRENDERED");
                    send(confirmMsg);
                }

                // Message milti-player game
                case "GET_ONLINE_USERS" -> {
                    System.out.println(me);
                    if (me == null) {
                        send(err("NOT_AUTH"));
                        break;
                    }
                    var players = new ArrayList<>(OnlineRegistry.getAllPlayers());
                    JsonArray arr = new JsonArray();
                    for (var p : players) {
                        if (p.getPlayerId().equals(me.getPlayerId()))
                            continue; // bỏ qua mình
                        JsonObject o = new JsonObject();
                        o.addProperty("playerId", p.getPlayerId());
                        o.addProperty("nickname", p.getNickname());
                        o.addProperty("status", p.getStatus().toString() == "IN_GAME" ? "IN_GAME" : "ONLINE");
                        arr.add(o);
                    }
                    System.out.println(arr);
                    JsonObject data = new JsonObject();
                    data.addProperty("type", "ONLINE_LIST");
                    data.add("rows", arr);
                    send(data);
                }

                case "CREATE_MULTIPLAYER_ROOM" -> {
                    if (me == null) {
                        send(err("NOT_AUTH"));
                        break;
                    }
                    HandelMatchMulti match = new HandelMatchMulti();
                    match.setType(model.Match.MatchType.MULTIPLAYER);
                    match.setCreatorId(me.getPlayerId());
                    ArrayList<PlayerMatch> pms = new ArrayList<>();
                    PlayerMatch pm = new PlayerMatch();
                    pm.setPlayer(me);
                    pm.setScore(0);
                    pms.add(pm);
                    match.setPlayerMatches(pms);
                    MatchDAO mdao = new MatchDAO();
                    mdao.create(match);
                    MatchOn.addMultiMatch(match);
                    OnlineRegistry.changeStatus(me.getPlayerId(), PlayerStatus.IN_GAME);
                    JsonObject data = new JsonObject();
                    data.addProperty("type", "CREATE_MULTIPLAYER_ROOM_ACK");
                    data.addProperty("match", match.toString());
                    data.addProperty("me", me.toString());
                    send(data);
                }

                case "INVITE_MULTIPLE_USERS_TO_MATCH" -> {
                    if (me == null) {
                        send(err("NOT_AUTH"));
                        break;
                    }
                    String toPlayerId = msg.get("toPlayerId").getAsString();
                    ClientHandler targetOut = OnlineRegistry.getHandler(toPlayerId);
                    if (targetOut != null) {
                        var m = new JsonObject();
                        m.addProperty("type", "INVITE_MULTIPLE_USERS_TO_MATCH");
                        m.addProperty("fromPlayer", me.toString());
                        m.addProperty("matchId", msg.get("matchId").getAsInt());
                        targetOut.send(m);
                    } else {
                        send(err("Người chơi không online"));
                    }
                }

                case "ACCEPT_MULTIPLE_USERS_MATCH_INVITE" -> {
                    if (me == null) {
                        send(err("NOT_AUTH"));
                        break;
                    }
                    int matchId = msg.get("matchId").getAsInt();
                    HandelMatchMulti match = MatchOn.getMultiMatch(matchId);
                    if (match == null) {
                        send(err("MATCH_NOT_FOUND"));
                        break;
                    }
                    ArrayList<PlayerMatch> players = match.getPlayerMatches();
                    boolean alreadyIn = players.stream()
                            .anyMatch(p -> p.getPlayer().getPlayerId().equals(me.getPlayerId()));
                    if (alreadyIn) {
                        send(err("ALREADY_IN_GAME"));
                        break;
                    }
                    ArrayList<PlayerMatch> pms = match.getPlayerMatches();
                    PlayerMatch pm = new PlayerMatch();
                    pm.setPlayer(me);
                    pm.setScore(0);
                    pms.add(pm);
                    match.setPlayerMatches(pms);
                    MatchOn.updateMatch(match);

                    OnlineRegistry.changeStatus(me.getPlayerId(), PlayerStatus.IN_GAME);

                    // Thông báo cho tất cả thành viên trong phòng
                    for (var player : players) {
                        Player user = player.getPlayer();
                        ClientHandler targetOut2 = OnlineRegistry.getHandler(user.getPlayerId());
                        if (targetOut2 != null) {
                            var m = new JsonObject();
                            m.addProperty("type", "ACCEPT_MULTIPLE_USERS_MATCH_INVITE");
                            m.addProperty("match", match.toString());
                            if (player.getPlayer().getPlayerId().equals(me.getPlayerId()))
                                m.addProperty("me", me.toString());
                            targetOut2.send(m);
                        }
                    }
                }

                case "DECLINE_MULTIPLE_USERS_MATCH_INVITE" -> {
                    if (me == null) {
                        send(err("NOT_AUTH"));
                        break;
                    }
                    String toPlayerId = msg.get("toPlayerId").getAsString();
                    ClientHandler targetOut = OnlineRegistry.getHandler(toPlayerId);
                    if (targetOut != null) {
                        var m = new JsonObject();
                        m.addProperty("type", "DECLINE_MULTIPLE_USERS_MATCH_INVITED");
                        m.addProperty("fromPlayer", me.toString());
                        targetOut.send(m);
                    } else {
                        send(err("Người chơi không online"));
                    }
                }

                case "EXIT_ROOM_MULTIPLE" -> {
                    if (me == null) {
                        send(err("NOT_AUTH"));
                        break;
                    }
                    System.out.println("Received EXIT_ROOM_MULTIPLE from " + me.getNickname());
                    Integer matchId = Integer.parseInt(msg.get("matchId").getAsString());
                    HandelMatchMulti match = MatchOn.getMultiMatch(matchId);
                    if (match != null) {
                        Player host = match.getHost();
                        ArrayList<PlayerMatch> players = match.getPlayerMatches();
                        OnlineRegistry.changeStatus(me.getPlayerId(), PlayerStatus.ONLINE);
                        players.removeIf(p -> p.getPlayer().getPlayerId().equals(me.getPlayerId()));
                        match.setPlayerMatches(players);
                        if (host != null && host.getPlayerId().equals(me.getPlayerId())) {
                            for (PlayerMatch player : players) {
                                Player user = player.getPlayer();
                                OnlineRegistry.changeStatus(user.getPlayerId(), PlayerStatus.ONLINE);
                                ClientHandler targetOut = OnlineRegistry.getHandler(user.getPlayerId());
                                if (targetOut != null) {
                                    var m = new JsonObject();
                                    m.addProperty("type", "EXIT_ROOM_MULTIPLE_HOST");
                                    targetOut.send(m);
                                }
                            }
                            MatchDAO matchDAO = new MatchDAO();
                            matchDAO.deletedMatch(match);
                            MatchOn.removeMatch(matchId);
                        } else {
                            for (PlayerMatch player : players) {
                                Player user = player.getPlayer();
                                ClientHandler targetOut = OnlineRegistry.getHandler(user.getPlayerId());
                                if (targetOut != null) {
                                    var m = new JsonObject();
                                    m.addProperty("type", "EXIT_ROOM_MULTIPLE_GUEST");
                                    m.addProperty("match", match.toString());
                                    targetOut.send(m);
                                }
                            }
                        }
                        var v = new JsonObject();
                        v.addProperty("type", "EXIT_ROOM_MULTIPLE_ME");
                        send(v);
                        out.flush();
                    }
                }
                case "START_GAME_MULTIPLE" -> {
                    Integer matchId = Integer.parseInt(msg.get("matchId").getAsString());
                    HandelMatchMulti match = MatchOn.getMultiMatch(matchId);
                    if (match == null) {
                        send(err("NOT_AUTH"));
                        break;
                    }
                    match.nextRound();
                    for (PlayerMatch player : match.getPlayerMatches()) {
                        Player user = player.getPlayer();
                        ClientHandler targetOut = OnlineRegistry.getHandler(user.getPlayerId());
                        if (targetOut != null) {
                            var m = new JsonObject();
                            m.addProperty("type", "GAME_MULTIPLE_STARTED");
                            m.addProperty("match", match.toString());
                            targetOut.send(m);
                        }
                    }
                }

                case "SUBMIT_MULTIPLE_ANSWERS" -> {
                    if (me == null) {
                        send(err("NOT_AUTH"));
                        break;
                    }
                    int matchId = msg.get("matchId").getAsInt();
                    String answer = msg.get("answer").getAsString();
                    HandelMatchMulti match = MatchOn.getMultiMatch(matchId);
                    if (match != null) {
                        synchronized (match) {
                            ArrayList<PlayerMatch> players = match.getPlayerMatches();
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
                                ArrayList<RoundResult> roundResults = match.calculateRoundScore();

                                // 5. Gửi kết quả cho tất cả players
                                for (PlayerMatch p : players) {
                                    ClientHandler pw = OnlineRegistry.getHandler(p.getPlayer().getPlayerId());
                                    if (pw != null) {
                                        var m = new JsonObject();
                                        m.addProperty("type", "ROUND_RESULT");
                                        m.addProperty("match", match.toString());
                                        var results = new JsonArray();
                                        for (RoundResult rr : roundResults) {
                                            results.add(rr.toString());
                                        }
                                        m.add("results", results);
                                        pw.send(m);
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
                    System.out.println("Received REQUEST_NEXT_ROUND from " + me.getNickname());
                    int matchId = msg.get("matchId").getAsInt();
                    HandelMatchMulti match = MatchOn.getMultiMatch(matchId);
                    if (match != null) {
                        synchronized (match) {
                            if (!match.hasNextRoundStarted()) {
                                ArrayList<PlayerMatch> players = match.getPlayerMatches();
                                players.stream().forEach(p -> p.setInputAnswer(null));
                                match.nextRound();
                                System.out.println("Starting next round " + match.getCurrentRound() + " with question: "
                                        + match.getQuestionCurrentRound());
                                for (PlayerMatch p : players) {
                                    ClientHandler pw = OnlineRegistry.getHandler(p.getPlayer().getPlayerId());
                                    if (pw != null) {
                                        var m = new JsonObject();
                                        m.addProperty("type", "START_NEXT_ROUND");
                                        m.addProperty("match", match.toString());
                                        pw.send(m);
                                    }
                                }
                            }
                        }
                    }
                }

                case "EXIT_GAME" -> {
                    int matchId = msg.get("matchId").getAsInt();
                    HandelMatchMulti match = MatchOn.getMultiMatch(matchId);
                    if (match != null) {
                        synchronized (match) {
                            ArrayList<PlayerMatch> players = match.getPlayerMatches();
                            PlayerMatch leavingPlayer = players.stream()
                                    .filter(p -> p.getPlayer().getPlayerId().equals(me.getPlayerId()))
                                    .findFirst()
                                    .orElse(null);

                            if (leavingPlayer != null) {
                                leavingPlayer.setStatus("TOP" + players.size());
                                PlayerMatchDAO pmDAO = new PlayerMatchDAO();
                                pmDAO.create(leavingPlayer, match.getMatchId());
                                players.removeIf(p -> p.getPlayer().getPlayerId().equals(me.getPlayerId()));
                                match.setPlayerMatches(players);
                            }
                            OnlineRegistry.changeStatus(me.getPlayerId(), PlayerStatus.ONLINE);
                            try {
                                if (players.size() > 1) {
                                    for (PlayerMatch p : players) {
                                        if (!p.getPlayer().getPlayerId().equals(me.getPlayerId())) {
                                            ClientHandler pw = OnlineRegistry.getHandler(p.getPlayer().getPlayerId());
                                            if (pw != null) {
                                                var m = new JsonObject();
                                                m.addProperty("type", "EXIT_GAME_OTHER");
                                                m.addProperty("match", match.toString());
                                                pw.send(m);
                                            }
                                        }
                                    }
                                } else if(players.size() == 1){
                                    PlayerMatch pm = players.get(0);
                                    pm.setStatus("TOP1");
                                    MatchDAO matchDAO = new MatchDAO();
                                    matchDAO.endMatch(match);
                                    PlayerMatchDAO pmDAO = new PlayerMatchDAO();
                                    pmDAO.create(pm, match.getMatchId());
                                    OnlineRegistry.changeStatus(me.getPlayerId(), PlayerStatus.ONLINE);
                                    ClientHandler pw = OnlineRegistry.getHandler(pm.getPlayer().getPlayerId());
                                    if (pw != null) {
                                        var m = new JsonObject();
                                        m.addProperty("type", "FINAL_RESULTS");
                                        m.addProperty("match", match.toString());
                                        pw.send(m);
                                    }
                                    MatchOn.removeMatch(matchId);
                                }
                                else{
                                    MatchOn.removeMatch(matchId);
                                }
                                var v = new JsonObject();
                                v.addProperty("type", "EXIT_GAME_ME");
                                send(v);
                            } catch (Exception ex) {
                                System.out.println("Error sending EXIT_GAME messages: " + ex.getMessage());
                                ex.printStackTrace();
                            }
                        }
                    } else {
                        System.out.println("Match not found for ID: " + matchId);
                    }
                }

                case "REQUEST_FINAL_RESULTS" -> {
                    int matchId = msg.get("matchId").getAsInt();
                    HandelMatchMulti match = MatchOn.getMultiMatch(matchId);
                    if (match != null) {
                        synchronized (match) {
                            ArrayList<PlayerMatch> players = match.getPlayerMatches();
                            if (match.getFinalResultsSent()) {
                                return;
                            }
                            match.setFinalResultsSent(true);
                            MatchDAO matchDAO = new MatchDAO();
                            matchDAO.endMatch(match);
                            PlayerMatchDAO pmDAO = new PlayerMatchDAO();
                            for (PlayerMatch p : players) {
                                pmDAO.create(p, match.getMatchId());
                            }
                            for (PlayerMatch p : players) {
                                ClientHandler pw = OnlineRegistry.getHandler(p.getPlayer().getPlayerId());
                                if (pw != null) {
                                    var m = new JsonObject();
                                    m.addProperty("type", "FINAL_RESULTS");
                                    m.addProperty("match", match.toString());
                                    pw.send(m);
                                }
                            }
                            // Xóa match khỏi danh sách phòng
                            MatchOn.removeMatch(matchId);
                        }
                    }
                }

                case "RELOAD_STATUS_ONLINE" -> {
                    if (me == null) {
                        send(err("NOT_AUTH"));
                        break;
                    }
                    OnlineRegistry.changeStatus(me.getPlayerId(), PlayerStatus.ONLINE);
                }
                // TODO: Handle other message types

                default -> send(err("Unknown type: " + type));
            }
        } catch (Exception e) {
            e.printStackTrace();
            send(err("SERVER_ERROR: " + e.getMessage()));
        }
        // CHÚ Ý: không unregister ở đây nữa, đã làm trong finally của run()
    }

    /* gửi JSON NDJSON */
    public synchronized void send(JsonObject o) {
        try {
            out.write(server.JsonUtil.toJson(o));
            out.write("\n");
            out.flush();
        } catch (IOException ignore) {
            ignore.printStackTrace();
        }
    }

    /* Helper method để gửi message cho người chơi cụ thể */
    private static void sendToPlayer(String playerId, JsonObject message) {
        ClientHandler handler = OnlineRegistry.getHandler(playerId);
        if (handler != null) {
            handler.send(message);
        }
    }

    private static JsonObject err(String msg) {
        var o = new JsonObject();
        o.addProperty("type", "AUTH_ERR");
        o.addProperty("reason", msg);
        return o;
    }
}
