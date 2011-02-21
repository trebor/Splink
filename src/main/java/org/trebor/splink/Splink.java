package org.trebor.splink;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
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
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.text.PlainDocument;

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
  private JTable mPrefix;
  private JTable mResult;
  private JLabel mOutput;
  private TableModel mPrefixTable;
  private String mQueryPrefixString;
  private Repository mRepository;
  private RepositoryConnection mConnection;
  private Map<String, String> mNameSpaceMap;
  
  enum Property
  {
    SESAME_HOST("sesame.host", String.class, "localhost"),
    SESAME_PORT("sesame.port", Integer.class, 8080),
    SESAME_REPOSITORY("sesame.repository", String.class, "test"),
    
    EDITOR_SIZE("gui.editor.size", Dimension.class, new Dimension(300, 250)), 
    EDITOR_FONT("gui.editor.font", Font.class, new Font("Courier", Font.BOLD, 18)),
    EDITOR_FONT_CLR("gui.editor.color", Color.class, Color.DARK_GRAY),
    EDITOR_TAB_SIZE("gui.editor.tabsize", Integer.class, 2),
  
    PREFIX_SIZE("gui.prefix.size", Dimension.class, new Dimension(600, 250)), 
    PREFIX_FONT("gui.prefix.font", Font.class, new Font("Courier", Font.BOLD, 18)),
    PREFIX_PREFIX_FONT_CLR("gui.prefix.prefix.color", Color.class, Color.DARK_GRAY),
    PREFIX_VALUE_FONT_CLR("gui.prefix.value.color", Color.class, Color.GRAY),
    PREFIX_COL1_WIDTH("gui.prefix.col1.width", Integer.class, 100),
    PREFIX_COL2_WIDTH("gui.prefix.col2.width", Integer.class, 500),
  
    RESULT_SIZE("gui.result.size", Dimension.class, new Dimension(900, 400)),
    RESULT_FONT("gui.result.font", Font.class, new Font("Courier", Font.BOLD, 15)),
    RESUlT_FONT_CLR("gui.result.color", Color.class, Color.DARK_GRAY);
    
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

      mRepository =
        new HTTPRepository(String.format("http://%s:%d/openrdf-sesame",
          SESAME_HOST.getString(), SESAME_PORT.getInteger()),
          SESAME_REPOSITORY.getString());
      mRepository.initialize();
      mConnection = mRepository.getConnection();
      initNameSpace();
      mEditor.setText("SELECT\n\t*\nWHERE\n{\n\t?s ?p ?o\n}");
    }
    catch (RepositoryException e)
    {
      setError(e.toString());
    }
  }

  private void initializeProperities()
  {
    mProperties = new Properties(PROPERTIES_FILE);
    Property.initialize(mProperties);
    for (Object key: mProperties.keySet())
      out.println(String.format("%s: %s", key, mProperties.get(key)));
  }  
  
  private void initNameSpace()
  {
    try
    {
      // init name-space map and a buffer to build the query prefix string
      
      mNameSpaceMap = new HashMap<String, String>();
      StringBuffer queryPrefixBuffer = new StringBuffer();
      
      // init name-space table
      
      DefaultTableModel prefixTable = new DefaultTableModel();
      prefixTable.addColumn("prefix");
      prefixTable.addColumn("value");
      
      // populate table and map
      
      RepositoryResult<Namespace> nameSpaces = mConnection.getNamespaces();
      while (nameSpaces.hasNext())
      {
        Namespace nameSpace = nameSpaces.next();
        mNameSpaceMap.put(nameSpace.getName(), nameSpace.getPrefix());
        prefixTable.addRow(new String[]{nameSpace.getPrefix(), nameSpace.getName()});
        queryPrefixBuffer.append(String.format("PREFIX %s:<%s>\n", nameSpace.getPrefix(), nameSpace.getName()));
      }

      // init master query prefix string
      
      mQueryPrefixString = queryPrefixBuffer.toString();
      
      // init master prefix table
      
      mPrefixTable = prefixTable;
      if (null != mPrefix)
      {
        mPrefix.setModel(mPrefixTable);
        mPrefix.getColumnModel().getColumn(0).setPreferredWidth(PREFIX_COL1_WIDTH.getInteger());
        mPrefix.getColumnModel().getColumn(1).setPreferredWidth(PREFIX_COL2_WIDTH.getInteger());
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
  
  // add prefix area

  TableCellRenderer mPrefixTableRenderer = new DefaultTableCellRenderer()
  {
    Color[] mPrefixRowColors = {
      Color.WHITE,
      new Color(255, 230, 220),
      Color.WHITE,
      new Color(235, 235, 235),
    };
    
    public Component getTableCellRendererComponent(JTable table,
      Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
      Component c =
        super.getTableCellRendererComponent(table, value, isSelected,
          hasFocus, row, column);

      setHorizontalAlignment(column == 0
        ? SwingConstants.CENTER
        : SwingConstants.LEFT);

      c.setForeground(column == 0
        ? PREFIX_PREFIX_FONT_CLR.getColor()
        : PREFIX_VALUE_FONT_CLR.getColor());

      c.setBackground(mPrefixRowColors[row % mPrefixRowColors.length]);

      return c;
    }
  };
  
  // add prefix area

  TableCellRenderer mResultTableRenderer = new DefaultTableCellRenderer()
  {
    Color[] mResultRowColors = {
      Color.WHITE,
      new Color(235, 235, 255),
      Color.WHITE,
      new Color(235, 235, 235),
    };
    
    public Component getTableCellRendererComponent(JTable table,
      Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
      Component c =
        super.getTableCellRendererComponent(table, value, isSelected,
          hasFocus, row, column);

      c.setBackground(mResultRowColors[row % mResultRowColors.length]);
      
      return c;
    }
  };
  
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
    mEditor.getDocument().putProperty(PlainDocument.tabSizeAttribute,
      EDITOR_TAB_SIZE.getInteger());

    mPrefix = new JTable()
    {
      public TableCellRenderer getCellRenderer(int row, int column) {
        return mPrefixTableRenderer;
      }
    };
    mPrefix.setFont(PREFIX_FONT.getFont());
    mPrefix.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
    
    // add result area

    mResult = new JTable()
    {
      public TableCellRenderer getCellRenderer(int row, int column) {
        return mResultTableRenderer;
      }
    };
    
    mResult.setFont(RESULT_FONT.getFont());
    mResult.setForeground(RESUlT_FONT_CLR.getColor());

    // add output

    mOutput = new JLabel(" ");

    // compose all the elements into the display

    final JScrollPane editScroll = new JScrollPane(mEditor);
    editScroll.setPreferredSize(EDITOR_SIZE.getDimension());

    final JScrollPane prefixScroll = new JScrollPane(mPrefix);
    prefixScroll.setPreferredSize(PREFIX_SIZE.getDimension());

    final JScrollPane resultScroll = new JScrollPane(mResult);
    resultScroll.setPreferredSize(RESULT_SIZE.getDimension());
    JSplitPane split =
      new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JSplitPane(
        JSplitPane.HORIZONTAL_SPLIT, editScroll, prefixScroll), resultScroll);
    frame.add(split, BorderLayout.CENTER);
    frame.add(mOutput, BorderLayout.SOUTH);

    // adjust tool-tip timeout

    ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);

    // save preferred window sizes when application closes

    Runtime.getRuntime().addShutdownHook(new Thread()
    {
      public void run()
      {
        EDITOR_SIZE.set(editScroll.getSize());
        PREFIX_SIZE.set(prefixScroll.getSize());
        RESULT_SIZE.set(resultScroll.getSize());
        PREFIX_COL1_WIDTH.set(mPrefix.getColumnModel().getColumn(0)
          .getWidth());
        PREFIX_COL2_WIDTH.set(mPrefix.getColumnModel().getColumn(1)
          .getWidth());
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
        performQuery(mQueryPrefixString + mEditor.getText());          
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
