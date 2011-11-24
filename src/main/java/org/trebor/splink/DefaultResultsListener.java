package org.trebor.splink;

import static java.lang.String.format;
import static org.trebor.splink.MessageHandler.Type.SPLASH;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.apache.log4j.Logger;
import org.openrdf.model.Statement;
import org.openrdf.query.Binding;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryException;

public class DefaultResultsListener implements ResultsListener
{
  public static final Logger log = Logger.getLogger(DefaultResultsListener.class);

  private Splink mSplink;
  private ResourceManager mResourceManager;
  private JTable mResultTable;
  TableCellRenderer mTableHeaderRenderer;
  
  public DefaultResultsListener(Splink splink, JTable resultTable, TableCellRenderer tableHeaderRenderer) throws RepositoryException
  {
    mSplink = splink;
    mResourceManager = new ResourceManager(splink.getConnection());
    mResultTable = resultTable;
    mTableHeaderRenderer = tableHeaderRenderer;
  }

  public int onTuple(TupleQueryResult result)
    throws QueryEvaluationException
  {
    // create the table model

    log.debug("mark 1");
    @SuppressWarnings("serial")
    DefaultTableModel tm = new DefaultTableModel()
    {
      public boolean isCellEditable(int row, int column)
      {
        return false;
      }
    };

    // add columnds to table

    log.debug("mark 2");
    Map<String, Integer> columnMap = new HashMap<String, Integer>();
    for (String binding : result.getBindingNames())
    {
      columnMap.put(binding, tm.getColumnCount());
      tm.addColumn(binding);
    }

    // populate the table

    log.debug("mark 3");
    while (result.hasNext())
    {
      String[] row = new String[columnMap.size()];
      Iterator<Binding> rowData = result.next().iterator();
      while (rowData.hasNext())
      {
        Binding rowBinding = rowData.next();
        String binding = rowBinding.getName();
        //String uri = rowBinding.getValue().toString();
        String uri = adjustResource(rowBinding.getValue().toString());
        row[columnMap.get(binding)] = uri;
      }

      tm.addRow(row);
    }

    // update the display

    log.debug("mark 4");
    mResultTable.setModel(tm);
    TableColumnModel columnModel = mResultTable.getColumnModel();
    for (int i = 0; i < tm.getColumnCount(); ++i)
      columnModel.getColumn(i).setHeaderRenderer(mTableHeaderRenderer);
    mSplink.setResultComponent(mResultTable);

    log.debug("mark 5");
    // return row count

    return tm.getRowCount();
  }

  public String adjustResource(String resource)
  {
    return mSplink.showLongUri()
      ? resource
      : mResourceManager.shrinkResource(resource);
  }
  
  public int onGraph(GraphQueryResult result) throws QueryEvaluationException
  {
    // create the table model

    @SuppressWarnings("serial")
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
      row.add(adjustResource(rowData.getSubject().toString()));
      row.add(adjustResource(rowData.getPredicate().toString()));
      row.add(adjustResource(rowData.getObject().toString()));
      tm.addRow(row);
    }

    // update the display

    mResultTable.setModel(tm);
    for (int i = 0; i < tm.getColumnCount(); ++i)
      mResultTable.getColumnModel().getColumn(i)
        .setHeaderRenderer(mTableHeaderRenderer);
    mSplink.setResultComponent(mResultTable);

    // return row count

    return tm.getRowCount();
  }

  public boolean onBoolean(boolean result)
  {
    mSplink.handleMessage(SPLASH, format("%b", result).toUpperCase());
    return result;
  }

  public void onUpdate()
  {
    mSplink.handleMessage(SPLASH, "update performed");
  }
}