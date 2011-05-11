package org.trebor.splink;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableCellRenderer;

@SuppressWarnings("serial")
public class RowStripeTableCellRenderer extends DefaultTableCellRenderer
{
  private final Color[] mResultRowColors;

  public RowStripeTableCellRenderer(Color highlight)
  {
    this(highlight, new Color(235, 235, 235), Color.WHITE);
  }
  
  public RowStripeTableCellRenderer(Color highlight, Color bland,
    Color background)
  {
    this(new Color[]
    {
      background, highlight, background, bland,
    });
  }

  public RowStripeTableCellRenderer(Color[] resultRowColors)
  {
    mResultRowColors = resultRowColors;
  }

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value,
    boolean isSelected, boolean hasFocus, int row, int column)
  {
    Component c =
      super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
        row, column);

    if (c instanceof JTextArea)
      ((JTextArea)c).setEditable(false);
    
    c.setBackground(mResultRowColors[row % mResultRowColors.length]);

    return c;
  }
}
