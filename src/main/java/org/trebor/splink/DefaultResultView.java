package org.trebor.splink;

import static java.lang.String.format;

import static org.trebor.splink.MessageHandler.Type.*;
import static org.trebor.splink.Splink.Property.RESULT_FONT;
import static org.trebor.splink.Splink.Property.RESULT_FONT_CLR;
import static org.trebor.splink.Splink.Property.TABLE_HEADER_BACKGROUND_CLR;
import static org.trebor.splink.Splink.Property.TABLE_HEADER_FONT;
import static org.trebor.splink.Splink.Property.TABLE_HEADER_FOREGROUND_CLR;

import java.awt.Color;
import java.awt.Component;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.openrdf.model.Statement;
import org.openrdf.query.Binding;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;

@SuppressWarnings("serial")
public class DefaultResultView extends JPanel implements View,
  ResultsListener
{
  public static final Color RESULT_ROW_HIGHLIGHT = new Color(255, 215, 255);

  private final JTable mResult;

  TableCellRenderer mTableHeaderRenderer = new DefaultTableCellRenderer()
  {
    {
      setHorizontalAlignment(CENTER);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table,
      Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
      JLabel header = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      
      header.setFont(TABLE_HEADER_FONT.getFont());
      header.setForeground(TABLE_HEADER_FOREGROUND_CLR.getColor());
      Color background = TABLE_HEADER_BACKGROUND_CLR.getColor();
      header.setBackground(background);
      Color c = new Color(background.getRed() - 15, background.getGreen() - 15, background.getBlue() - 15);
      header.setBorder(new LineBorder(c, 1, true));

      return header;
    }
  };  
  
  private final TableCellRenderer mResultTableRenderer;

  public DefaultResultView(Splink splink)
  {
    mResultTableRenderer =
      new RowStripeTableCellRenderer(RESULT_ROW_HIGHLIGHT);

    // create result table

    mResult = new JTable()
    {
      public TableCellRenderer getCellRenderer(int row, int column)
      {
        return mResultTableRenderer;
      }
    };

    mResult.setFont(RESULT_FONT.getFont());
    mResult.setForeground(RESULT_FONT_CLR.getColor());
    mResult.setSelectionForeground(RESULT_FONT_CLR.getColor());
    mResult.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

    // table.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    // table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    // table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

    // look for popup menu mouse events

    mResult.addMouseListener(splink.new PopupListener(mResult));
  }

  public Component getViewComponent()
  {
    return this;
  }

  public ResultsListener getResultsListener()
  {
    return this;
  }

  public int onTuple(TupleQueryResult result, Splink splink) throws QueryEvaluationException
  {
    // create the table model

    DefaultTableModel tm = new DefaultTableModel()
    {
      public boolean isCellEditable(int row, int column)
      {
        return false;
      }
    };

    // add columns to table

    Map<String, Integer> columnMap = new HashMap<String, Integer>();
    for (String binding : result.getBindingNames())
    {
      columnMap.put(binding, tm.getColumnCount());
      tm.addColumn(binding);
    }

    // populate the table

    while (result.hasNext())
    {
      String[] row = new String[columnMap.size()];
      Iterator<Binding> rowData = result.next().iterator();
      while (rowData.hasNext())
      {
        Binding rowBinding = rowData.next();
        String binding = rowBinding.getName();
        String uri = rowBinding.getValue().toString();
        if (!splink.showShortUris())
          uri = splink.shortUri(uri);
        row[columnMap.get(binding)] = uri;
      }

      tm.addRow(row);
    }

    // update the display

    mResult.setModel(tm);
    TableColumnModel columnModel = mResult.getColumnModel();
    for (int i = 0; i < tm.getColumnCount(); ++i)
      columnModel.getColumn(i).setHeaderRenderer(mTableHeaderRenderer);
    splink.setResultComponent(mResult);

    // return row count

    return tm.getRowCount();
  }

  public int onGraph(GraphQueryResult result, Splink splink) throws QueryEvaluationException
  {
    // create the table model

    DefaultTableModel tm = new DefaultTableModel()
    {
      public boolean isCellEditable(int row, int column)
      {
        return false;
      }
    };

    // add columns

    for (String name : new String[]
    {
      "subject", "predicate", "object"
    })
      tm.addColumn(name);

    // populate the table

    while (result.hasNext())
    {
      Vector<String> row = new Vector<String>();
      Statement rowData = result.next();
      if (!splink.showShortUris())
      {
        row.add(rowData.getSubject().toString());
        row.add(rowData.getPredicate().toString());
        row.add(rowData.getObject().toString());
      }
      else
      {
        row.add(splink.shortUri(rowData.getSubject().toString()));
        row.add(splink.shortUri(rowData.getPredicate().toString()));
        row.add(splink.shortUri(rowData.getObject().toString()));
      }

      tm.addRow(row);
    }

    // update the display

    mResult.setModel(tm);
    for (int i = 0; i < tm.getColumnCount(); ++i)
      mResult.getColumnModel().getColumn(i)
        .setHeaderRenderer(mTableHeaderRenderer);
    splink.setResultComponent(mResult);

    // return row count

    return tm.getRowCount();
  }

  public boolean onBoolean(boolean result, Splink splink)
  {
    splink.handleMessage(SPLASH, format("%b", result).toUpperCase());
    return result;
  }
}
