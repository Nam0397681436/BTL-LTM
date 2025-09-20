package client.ui.table;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionListener;

public class ButtonEditor extends AbstractCellEditor implements TableCellEditor {
    private final JButton button = new JButton();
    private final ActionListener onClick;

    public ButtonEditor(ActionListener onClick) {
        this.onClick = onClick;
        button.setFocusPainted(false);
        button.addActionListener(e -> {
            if (onClick != null) onClick.actionPerformed(e);
            fireEditingStopped(); // kết thúc edit sau khi click
        });
    }

    @Override public Object getCellEditorValue() { return button.getText(); }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row, int column) {
        button.setText(value == null ? "" : value.toString());
        // truyền row/column để listener biết đang bấm vào hàng nào
        button.putClientProperty("row", row);
        button.putClientProperty("column", column);
        // chỉ cho click khi text là "Thách đấu"
        button.setEnabled("Thách đấu".equals(button.getText()));
        return button;
    }
}
