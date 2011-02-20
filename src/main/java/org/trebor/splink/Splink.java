package org.trebor.splink;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.plaf.DimensionUIResource;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.openrdf.model.Namespace;
import org.openrdf.model.Value;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.TupleQueryResultHandler;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.http.HTTPRepository;

import static javax.swing.KeyStroke.getKeyStroke;
import static java.awt.event.KeyEvent.*;

@SuppressWarnings("serial")
public class Splink extends JFrame
{
  private JEditorPane mEditor;
  private JTable mResult;
  private JTextArea mOutput;
  private Repository mRepository;
  private RepositoryConnection mConnection;
  private String mSesameServer = "http://localhost:8080/openrdf-sesame";
  private String mRepositoryID = "test";
  
  public static void main(String[] args)
  {
    new Splink();
  }
  
  public Splink()
  {
    try
    {
      constructFrame(getContentPane());
      pack();
      setVisible(true);
      
      mRepository = new HTTPRepository(mSesameServer, mRepositoryID);
      mRepository.initialize();
      mConnection = mRepository.getConnection();
      
      RepositoryResult<Namespace> nameSpaces = mConnection.getNamespaces();
      StringBuffer buffer = new StringBuffer();
      
      while (nameSpaces.hasNext())
      {
        Namespace nameSpace = nameSpaces.next();
        buffer.append(String.format("PREFIX %s:<%s>\n", nameSpace.getPrefix(), nameSpace.getName()));
      }
      buffer.append("\nselect * where {?s ?p ?o}\n");
      mEditor.setText(buffer.toString());
    }
    catch (RepositoryException e)
    {
      setMessage(e.toString());
    }
  }

  private void constructFrame(Container frame)
  {
    // configure frame
    
    frame.setLayout(new BorderLayout());
    
    // configure menu
    
    JMenuBar menuBar = new JMenuBar();
    setJMenuBar(menuBar);
    JMenu query = new JMenu("Query");
    menuBar.add(query);
    query.add(mSubmiteQuery);
    
    // add editor
    
    mEditor = new JEditorPane();
    //mEditor.setPreferredSize(new DimensionUIResource(500, 200));

    // add result area
    
    mResult = new JTable();
    //mResult.setPreferredSize(new DimensionUIResource(500, 200));
    
    // add output
    
    mOutput = new JTextArea(3, 1);
    mOutput.setEditable(false);
    mOutput.setWrapStyleWord(true);
    mOutput.setLineWrap(true);

    // create split pane
    
    JScrollPane editScroll = new JScrollPane(mEditor);
    editScroll.setPreferredSize(new DimensionUIResource(800, 300));
    JScrollPane resultScroll = new JScrollPane(mResult);
    resultScroll.setPreferredSize(new DimensionUIResource(800, 200));
    JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editScroll, resultScroll);
    frame.add(split, BorderLayout.CENTER);
    frame.add(new JScrollPane(mOutput), BorderLayout.SOUTH);
  }

  private void submit()
  {
    setMessage("Querying...");
    new Thread()
    {
      public void run()
      {
        performQuery(mEditor.getText());          
      }
    }.start();
  }
  
  public void performQuery(String queryString)
  {
    try
    {
      TupleQuery query = mConnection.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
      TupleQueryResult result = query.evaluate();
      
      // create the table model
      
      DefaultTableModel tm = new DefaultTableModel();
      for (String binding: result.getBindingNames())
        tm.addColumn(binding);
      
      // populate the table
      
      while (result.hasNext())
      {
        Vector<String> row = new Vector<String>();
        Iterator<Binding> rowData = result.next().iterator();
        while (rowData.hasNext())
          row.add(rowData.next().getValue().toString());

        tm.addRow(row);        
      }
      
      // update the display
      
      mResult.setModel(tm);
      setMessage("cols: %d, rows: %d", tm.getRowCount(), tm.getColumnCount());
    }
    catch (Exception e)
    {
      mResult.setModel(new DefaultTableModel());
      setMessage(e.toString());
    }
  }
  
  public void setMessage(String message, Object... args)
  {
    mOutput.setText(String.format(message, args));
    mOutput.repaint();
  }

  /**
   * Splink is derived from AbstractAction and provides a standard
   * class from which to subclass Game actions.
   */

  public abstract class SplinkAction extends AbstractAction
  {
    /**
     * Create a SplinkAction with a given name, shortcut key and description.
     * 
     * @param name name of action
     * @param key shortcut key to trigger action
     */

    public SplinkAction(String name, KeyStroke key, String description)
    {
      putValue(NAME, name);
      putValue(SHORT_DESCRIPTION, description);
      putValue(ACCELERATOR_KEY, key);
    }

    /**
     * Called when the given action is to be executed. This function must
     * be implemented by the subclass.
     * 
     * @param e action event
     */

    abstract public void actionPerformed(ActionEvent e);
  }
  
  private SplinkAction mSubmiteQuery = new SplinkAction("Submit", getKeyStroke(VK_ENTER, CTRL_MASK),  "submit sparql query")
  {
    public void actionPerformed(ActionEvent e)
    {
      submit();
    }
  };
}
