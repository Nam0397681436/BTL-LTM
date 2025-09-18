package client.ui.table;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class ButtonRenderer extends JButton implements TableCellRenderer {
    public ButtonRenderer() {
        setOpaque(true);
        setFocusable(false);
        setHorizontalAlignment(SwingConstants.CENTER);
        setMargin(new Insets(2, 10, 2, 10)); // padding cho chữ không bị cụt
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        String text = value == null ? "" : value.toString();
        setText(text);
        // chỉ enable khi là "Thách đấu"
        setEnabled("Thách đấu".equals(text));
        return this;
    }
}
