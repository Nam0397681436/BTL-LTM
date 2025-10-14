package client.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;


public class HistoryFrame extends JDialog {

    private JTable tblHistory;
    private DefaultTableModel model;

    public HistoryFrame(Frame owner, boolean modal) {
        super(owner, "Lịch sử đấu", modal);
        initUI();
    }
    public HistoryFrame() {
        super((Frame) null, "Lịch sử đấu", true);
        initUI();
    }

    public void load(JsonArray rows) {
        model.setRowCount(0);
        for (int i = 0; i < rows.size(); i++) {
            JsonObject o = rows.get(i).getAsJsonObject();
            int matchId   = o.get("matchId").getAsInt();
            String mode   = o.get("mode").getAsString();
            String start  = o.get("startTime").isJsonNull() ? "" : o.get("startTime").getAsString();
            String end    = o.get("endTime").isJsonNull()   ? "" : o.get("endTime").getAsString();
            int score     = o.get("score").getAsInt();
            String result = o.has("result")
                    ? o.get("result").getAsString()
                    : o.get("status").getAsString();
            model.addRow(new Object[]{ matchId, mode, start, end, score, result });
        }
    }

    //Nạp dữ liệu dạng bảng sẵn. Mỗi Object[] là 1 dòng
    public void setData(List<Object[]> table) {
        model.setRowCount(0);
        for (Object[] r : table) model.addRow(r);
    }

    private void initUI() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(780, 460);
        setLocationRelativeTo(getOwner());

        model = new DefaultTableModel(
                new Object[]{"MatchId", "Chế độ", "Bắt đầu", "Kết thúc", "Điểm", "Kết quả"}, 0
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tblHistory = new JTable(model);
        tblHistory.setRowHeight(26);
        tblHistory.setAutoCreateRowSorter(true);

        tblHistory.getTableHeader().setFont(tblHistory.getFont().deriveFont(Font.BOLD));

        // Layout
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(new JScrollPane(tblHistory), BorderLayout.CENTER);

        // Nút đóng
        JButton btnClose = new JButton("Đóng");
        btnClose.addActionListener(e -> dispose());
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.add(btnClose);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }
}
