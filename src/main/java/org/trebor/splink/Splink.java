// The MIT License
//
// Copyright (c) 2011 Robert B. Harrs (trebor@trebor.org)
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
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
import javax.swing.JTextArea;
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
import static java.lang.String.format;

@SuppressWarnings("serial")
public class Splink extends JFrame
{
  public static final String PROPERTIES_FILE = System.getProperty("user.home") + File.separator + ".splink";

  private Properties mProperties;
  private JEditorPane mEditor;
  private JTable mPrefix;
  private JScrollPane mResultArea;
  private JTable mResult;
  private JTextArea mErrorText;
  private JLabel mOutput;
  private TableModel mPrefixTable;
  private String mQueryPrefixString;
  private Repository mRepository;
  private RepositoryConnection mConnection;
  private Map<String, String> mNameSpaceMap;
  private Stack<String> mQueryStack;
  private String mLastQuery;

  
  
  enum Property
  {
    SESAME_HOST("sesame.host", String.class, "localhost"),
    SESAME_PORT("sesame.port", Integer.class, 8080),
    SESAME_REPOSITORY("sesame.repository", String.class, "test"),

    CURRENT_QUERY("current.query", String.class, "SELECT\n\t*\nWHERE\n{\n\t?s ?p ?o\n}"),
    
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

    ERROR_FONT("gui.error.font", Font.class, new Font("Courier", Font.BOLD, 15)),
    ERROR_FONT_CLR("gui.error.color", Color.class, Color.RED.darker().darker()),
    
    RESULT_SIZE("gui.result.size", Dimension.class, new Dimension(900, 400)),
    RESULT_FONT("gui.result.font", Font.class, new Font("Courier", Font.BOLD, 15)),
    RESULT_FONT_CLR("gui.result.color", Color.class, Color.DARK_GRAY);
    
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
    initializeProperities();
    constructFrame(getContentPane());
    connectToRepository();
  }

  private void connectToRepository()
  {
    try
    {
      mRepository =
        new HTTPRepository(String.format("http://%s:%d/openrdf-sesame",
          SESAME_HOST.getString(), SESAME_PORT.getInteger()),
          SESAME_REPOSITORY.getString());
      
      mRepository.initialize();
      mConnection = mRepository.getConnection();
      initializeNameSpace();
    }
    catch (RepositoryException e)
    {
      setError(e);
    }
  }

  private void initializeProperities()
  {
    mProperties = new Properties(PROPERTIES_FILE);
    Property.initialize(mProperties);
    for (Object key: mProperties.keySet())
      debugMessage("%s: %s", key, mProperties.get(key));
  }  
  
  private void initializeNameSpace()
  {
    try
    {
      // init name-space map and a buffer to build the query prefix string
      
      mNameSpaceMap = new HashMap<String, String>();
      StringBuffer queryPrefixBuffer = new StringBuffer();
      
      // init name-space table
      
      DefaultTableModel prefixTable = new DefaultTableModel()
      {
        public boolean isCellEditable(int row, int col)
        {
          return false;
        }         
      };
      prefixTable.addColumn("prefix");
      prefixTable.addColumn("value");
      
      // populate table and map
      
      RepositoryResult<Namespace> nameSpaces = mConnection.getNamespaces();
      while (nameSpaces.hasNext())
      {
        Namespace nameSpace = nameSpaces.next();
        mNameSpaceMap.put(nameSpace.getName(), nameSpace.getPrefix() + ":");
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
      setError(e);
    }
  }
  
  private String shortUri(String longUri)
  {
    for (String name: mNameSpaceMap.keySet())
      if (longUri.startsWith(name))
        return longUri.replace(name, mNameSpaceMap.get(name));
    
    return longUri;
  }

  private String longUri(String shortUri)
  {
    for (String name: mNameSpaceMap.keySet())
    {
      String perfix = mNameSpaceMap.get(name);
      if (shortUri.startsWith(perfix))
        return shortUri.replace(perfix, name);
    }
    
    return shortUri;
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

      if (c instanceof JTextArea)
        ((JTextArea)c).setEditable(false);
      
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

      if (c instanceof JTextArea)
        ((JTextArea)c).setEditable(false);

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
    query.add(mPreviousQuery);

    // add editor

    mEditor = new JEditorPane();
    mEditor.setFont(EDITOR_FONT.getFont());
    mEditor.setForeground(EDITOR_FONT_CLR.getColor());
    mEditor.getDocument().putProperty(PlainDocument.tabSizeAttribute,
      EDITOR_TAB_SIZE.getInteger());
    mEditor.setText(CURRENT_QUERY.getString());
    
    // create prefix table
    
    mPrefix = new JTable()
    {
      public TableCellRenderer getCellRenderer(int row, int column) {
        return mPrefixTableRenderer;
      }
    };
    mPrefix.setFont(PREFIX_FONT.getFont());
    mPrefix.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
    
    // create error text area
    
    mErrorText = new JTextArea();
    mErrorText.setFont(ERROR_FONT.getFont());
    mErrorText.setForeground(ERROR_FONT_CLR.getColor());
    mErrorText.setEditable(false);
    
    // create result table

    mResult = new JTable()
    {
      public TableCellRenderer getCellRenderer(int row, int column) {
        return mResultTableRenderer;
      }
    };
    mResult.addMouseListener(new MouseAdapter()
    {
      public void mouseClicked(MouseEvent e)
      {
        if (e.getClickCount() == 2) {
          JTable target = (JTable)e.getSource();
          int row = target.getSelectedRow();
          int column = target.getSelectedColumn();
          inspectResource(target.getModel().getValueAt(row, column).toString());
        }
      }
    });
    
    mResult.setFont(RESULT_FONT.getFont());
    mResult.setForeground(RESULT_FONT_CLR.getColor());
    mResult.setSelectionForeground(RESULT_FONT_CLR.getColor());

    // add output

    mOutput = new JLabel(" ");

    // compose all the elements into the display

    final JScrollPane editScroll = new JScrollPane(mEditor);
    editScroll.setPreferredSize(EDITOR_SIZE.getDimension());

    final JScrollPane prefixScroll = new JScrollPane(mPrefix);
    prefixScroll.setPreferredSize(PREFIX_SIZE.getDimension());

    mResultArea = new JScrollPane(mResult);
    mResultArea.setPreferredSize(RESULT_SIZE.getDimension());
    JSplitPane split =
      new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JSplitPane(
        JSplitPane.HORIZONTAL_SPLIT, editScroll, prefixScroll), mResultArea);
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
        RESULT_SIZE.set(mResultArea.getSize());
        PREFIX_COL1_WIDTH.set(mPrefix.getColumnModel().getColumn(0)
          .getWidth());
        PREFIX_COL2_WIDTH.set(mPrefix.getColumnModel().getColumn(1)
          .getWidth());
        CURRENT_QUERY.set(mEditor.getText());
      }
    });
    
    // set frame title
    
    setTitle(String.format("http://%s:%d/openrdf-sesame/%s",
      SESAME_HOST.getString(), SESAME_PORT.getInteger(),
      SESAME_REPOSITORY.getString()));

    // make frame visible
    
    pack();
    setVisible(true);
  }

  public void setResultComponent(Component c)
  {
    mResultArea.setViewportView(c);
  }
  
  private void inspectResource(String uri)
  {
    String longUri = longUri(uri);
    String query = String.format(
      "SELECT * " +
      "WHERE { ?subject ?predicate ?object " +
      "FILTER (?subject = <%s> || ?predicate = <%s> || ?object = <%s>) }", longUri, longUri, longUri);
    submitQuery(query, true, true);
  }
  
  private void submitQuery()
  {
    submitQuery(mEditor.getText(), true, true);
  }

  private void pushQuery(String query)
  {
    if (null == mQueryStack)
      mQueryStack = new Stack<String>();

    if (null != mLastQuery)
      mQueryStack.push(mLastQuery);
    
    mLastQuery = query;
    
    mPreviousQuery.setEnabled(!mQueryStack.isEmpty());
  }
  
  private void popQuery()
  {
    if (null != mQueryStack && !mQueryStack.isEmpty())
      submitQuery(mLastQuery = mQueryStack.pop(), false, false);

    mPreviousQuery.setEnabled(!mQueryStack.isEmpty());
  }
  
  private void submitQuery(final String query, final boolean appendPrefix,
    boolean record)
  {
    final String fullQuery = appendPrefix
      ? mQueryPrefixString + query
      : query;
    
    if (record)
      pushQuery(fullQuery);
          
    setMessage("Querying...");
    new Thread()
    {
      public void run()
      {
        boolean submitEnabled = mSubmiteQuery.isEnabled();
        boolean previousEnabled = mPreviousQuery.isEnabled();

        mSubmiteQuery.setEnabled(false);
        mPreviousQuery.setEnabled(false);

        debugMessage(fullQuery);
        performQuery(fullQuery);
        
        mSubmiteQuery.setEnabled(submitEnabled);
        mPreviousQuery.setEnabled(previousEnabled);
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
      
      DefaultTableModel tm = new DefaultTableModel()
      {
        public boolean isCellEditable(int row, int column)
        {
          return false;
        }
      };
      
      for (String binding: result.getBindingNames())
        tm.addColumn(binding);
      
      // populate the table
      
      while (result.hasNext())
      {
        Vector<String> row = new Vector<String>();
        Iterator<Binding> rowData = result.next().iterator();
        while (rowData.hasNext())
          row.add(shortUri(rowData.next().getValue().toString()));
        tm.addRow(row);
      }
      
      // update the display

      mResult.setModel(tm);
      setResultComponent(mResult);
      setMessage("seconds: %2.2f cols: %d, rows: %d", (System.currentTimeMillis() - startTime) / 1000.f, tm.getColumnCount(), tm.getRowCount());
    }
    catch (Exception e)
    {
      setError(e, "------ query ------\n\n%s\n\n-------------------\n", queryString);
    }
  }

  public void debugMessage(String message, Object... args)
  {
    System.out.println(format(message, args));
    setMessage(Color.BLUE, message, args);
  }
  
  public void setError(Exception e, String message, Object... args)
  {
    StringWriter writer = new StringWriter();
    PrintWriter pw = new PrintWriter(writer);
    e.printStackTrace(pw);
    setError("%s\n%s", format(message, args), writer.getBuffer().toString());
  }

  public void setError(Exception e)
  {
    setError(e, "");
  }

  public void setError(String message, Object... args)
  {
    mErrorText.setText(format(message, args));
    setResultComponent(mErrorText);
    setMessage(ERROR_FONT_CLR.getColor(), "Error!");
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
    if (null != mOutput)
    {
      String toolTip =
        fullMessage.replaceAll("<", "&lt;").replaceAll(">", "&gt;")
          .replaceAll("\n", "<br>");
      mOutput.setToolTipText("<html>" + toolTip + "</html>");
      mOutput.setText(fullMessage);
      mOutput.setForeground(color);
      mOutput.repaint();
    }
    else
      System.out.println(fullMessage);
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
      submitQuery();
    }
  };
  
  private SplinkAction mPreviousQuery = new SplinkAction("Previous", getKeyStroke(VK_BACK_SPACE, CTRL_MASK),  "perform previous query")
  {
    {
      setEnabled(false);
    }
    
    public void actionPerformed(ActionEvent e)
    {
      popQuery();
    }
  };
}
