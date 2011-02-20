package org.trebor.splink;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ToolTipManager;
import javax.swing.table.DefaultTableModel;

import org.openrdf.model.Namespace;
import org.openrdf.query.Binding;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.http.HTTPRepository;

import static org.trebor.splink.Splink.Property.*;
import static javax.swing.KeyStroke.getKeyStroke;
import static java.awt.event.KeyEvent.*;
import static java.lang.System.out;

@SuppressWarnings("serial")
public class Splink extends JFrame
{
  public static final String PROPERTIES_FILE = System.getProperty("user.home") + File.separator + ".splink";

  private Properties mProperties;
  private JEditorPane mEditor;
  private JTable mResult;
  private JLabel mOutput;
  private Repository mRepository;
  private RepositoryConnection mConnection;
  private String mSesameServer = "http://localhost:8080/openrdf-sesame";
  private String mRepositoryID = "test";
  private Map<String, String> mNameSpaceMap;
  
  public static void main(String[] args)
  {
    System.setProperty("apple.laf.useScreenMenuBar", "true");
    new Splink();
  }
  
  public Splink()
  {
    try
    {
      initializeProperities();
      constructFrame(getContentPane());
      pack();
      setVisible(true);
      
      mRepository = new HTTPRepository(mSesameServer, mRepositoryID);
      mRepository.initialize();
      mConnection = mRepository.getConnection();
      initNameSpace();
      
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
      setError(e.toString());
    }
  }

  enum Property
  {
    EDITOR_SIZE("gui.editor.size", Dimension.class, new Dimension(800, 300)), 
    EDITOR_FONT("gui.editor.font", Font.class, new Font("Courier", Font.BOLD, 18)),
    EDITOR_FONT_CLR("gui.editor.color", Color.class, Color.DARK_GRAY),
    RESULT_SIZE("gui.result.size", Dimension.class, new Dimension(800, 200)),
    RESULT_FONT("gui.result.font", Font.class, new Font("Courier", Font.BOLD, 15)),
    RESUlT_FONT_CLR("gui.result.color", Color.class, Color.GRAY);
    
    final private String mName;
    final private Class<?> mType;
    final private Object mDefaultValue;
    private Properties mProperties;

    Property(String name, Class<?> type, Object defaultValue)
    {
      mName = name;
      mType = type;
      mDefaultValue = defaultValue;
    }

    public static void initialize(Properties properties)
    {
      for (Property property : values())
      {
        property.mProperties = properties;
        if (!properties.containsKey(property.mName))
          property.set(property.mDefaultValue);
      }
    }
    
    public void set(Object value)
    {
      try
      {
        mProperties.getClass().getMethod("set", String.class, mType)
          .invoke(mProperties, mName, value);
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
    
    public Font getFont()
    {
      return mProperties.getFont(mName);
    }
    
    public Dimension getDimension()
    {
      return mProperties.getDimension(mName);
    }
    
    public Point getPoint()
    {
      return mProperties.getPoint(mName);
    }
    
    public Rectangle getRectangle()
    {
      return mProperties.getRectangle(mName);
    }

    public Color getColor()
    {
      return mProperties.getColor(mName);
    }

    public String getString()
    {
      return mProperties.getString(mName);
    }
    
    public double getDouble()
    {
      return mProperties.getDouble(mName);
    }

    public int getInteger()
    {
      return mProperties.getInteger(mName);
    }
    
    public boolean getBoolean()
    {
      return mProperties.getBoolean(mName);
    }
  }
  
  private void initializeProperities()
  {
    mProperties = new Properties(PROPERTIES_FILE);
    Property.initialize(mProperties);
    out.println(EDITOR_SIZE.getDimension());
    out.println(EDITOR_FONT.getFont());
    
  }

  private void initNameSpace()
  {
    try
    {
      mNameSpaceMap = new HashMap<String, String>();
      RepositoryResult<Namespace> nameSpaces = mConnection.getNamespaces();
      while (nameSpaces.hasNext())
      {
        Namespace nameSpace = nameSpaces.next();
        mNameSpaceMap.put(nameSpace.getName(), nameSpace.getPrefix());
      }
    }
    catch (Exception e)
    {
      setError(e.toString());
    }
  }
  
  private String convertUri(String uri)
  {
    for (String name: mNameSpaceMap.keySet())
      if (uri.startsWith(name))
        return uri.replace(name, mNameSpaceMap.get(name) + ":");
    
    return uri;
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
    mEditor.setFont(EDITOR_FONT.getFont());
    mEditor.setForeground(EDITOR_FONT_CLR.getColor());
    
    // add result area

    mResult = new JTable();
    mResult.setFont(RESULT_FONT.getFont());
    mResult.setForeground(RESUlT_FONT_CLR.getColor());

    // add output

    mOutput = new JLabel(" ");

    // create split pane

    final JScrollPane editScroll = new JScrollPane(mEditor);
    editScroll.setPreferredSize(EDITOR_SIZE.getDimension());
    final JScrollPane resultScroll = new JScrollPane(mResult);
    resultScroll.setPreferredSize(RESULT_SIZE.getDimension());
    JSplitPane split =
      new JSplitPane(JSplitPane.VERTICAL_SPLIT, editScroll, resultScroll);
    frame.add(split, BorderLayout.CENTER);
    frame.add(mOutput, BorderLayout.SOUTH);

    // adjust tool-tip timeout

    ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);

    // save preferred window sizes when application closes
    
    Runtime.getRuntime().addShutdownHook(new Thread()
    {
      public void run()
      {
        System.out.println("save!");
        
        EDITOR_SIZE.set(editScroll.getSize());
        RESULT_SIZE.set(editScroll.getSize());
      }
    });
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
      long startTime = System.currentTimeMillis();
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
          row.add(convertUri(rowData.next().getValue().toString()));
        tm.addRow(row);
      }
      
      // update the display
      
      mResult.setModel(tm);
      setMessage("seconds: %2.2f cols: %d, rows: %d", (System.currentTimeMillis() - startTime) / 1000.f, tm.getColumnCount(), tm.getRowCount());
    }
    catch (Exception e)
    {
      mResult.setModel(new DefaultTableModel());
      setError(e.toString());
    }
  }

  public void setError(String message, Object... args)
  {
    setMessage(Color.RED, message, args);
  }
  
  public void setMessage(String message, Object... args)
  {
    setMessage(Color.GREEN.darker().darker().darker(), message, args);
  }
  
  public void setWarning(String message, Object... args)
  {
    setMessage(Color.YELLOW, message, args);
  }
  
  public void setMessage(Color color, String message, Object... args)
  {
    String fullMessage = String.format(message, args);
    String toolTip =
      fullMessage.replaceAll("<", "&lt;").replaceAll(">", "&gt;")
        .replaceAll("\n", "<br>");
    mOutput.setToolTipText("<html>" + toolTip + "</html>");
    mOutput.setText(fullMessage);
    mOutput.setForeground(color);
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
