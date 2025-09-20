package client.ui;

import client.JsonUtil;
import client.net.TcpClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;

public class HistoryFrame extends JFrame {
    private final TcpClient tcp;
    private final DefaultTableModel model =
            new DefaultTableModel(new Object[]{"MatchId","Mode","Start","End","Score","Kết quả"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };

    public HistoryFrame(TcpClient tcp) {
        super("Lịch sử đấu");
        this.tcp = tcp;
        setSize(700, 420);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        var btnReload = new JButton("Lịch sử đấu");
        btnReload.addActionListener(e -> load());
        add(btnReload, BorderLayout.NORTH);
        add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);
    }

    public void load() {
        JsonObject m = new JsonObject();
        m.addProperty("type", "GET_HISTORY");
        m.addProperty("limit", 50);
        try { tcp.send(JsonUtil.toJson(m)); } catch (IOException ex) { ex.printStackTrace(); }
    }

    public void handleLine(String line) {
        try {
            var msg = JsonUtil.fromJson(line, JsonObject.class);
            if (!"HISTORY".equals(msg.get("type").getAsString())) return;
            model.setRowCount(0);
            JsonArray arr = msg.getAsJsonArray("rows");
            for (var el : arr) {
                var o = el.getAsJsonObject();
                model.addRow(new Object[]{
                        o.get("matchId").getAsInt(),
                        o.get("mode").getAsString(),
                        o.get("startTime").getAsString(),
                        o.get("endTime").getAsString(),
                        o.get("score").getAsInt(),
                        o.get("result").getAsString()
                });
            }
        } catch (Exception ignore) {}
    }
}
