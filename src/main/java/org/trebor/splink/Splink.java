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
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import javax.swing.border.LineBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import javax.swing.undo.UndoManager;

import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.algebra.Slice;
import org.openrdf.query.algebra.helpers.QueryModelVisitorBase;
import org.openrdf.query.parser.ParsedBooleanQuery;
import org.openrdf.query.parser.ParsedGraphQuery;
import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.query.parser.ParsedTupleQuery;
import org.openrdf.query.parser.QueryParserUtil;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.http.HTTPRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;

import static org.openrdf.query.QueryLanguage.*;
import static org.trebor.splink.Splink.Property.*;
import static org.trebor.splink.Splink.ResourceType.*;
import static javax.swing.KeyStroke.getKeyStroke;
import static java.awt.event.KeyEvent.*;
import static java.lang.String.format;
import static java.lang.System.out;
import static java.lang.Math.max;

@SuppressWarnings("serial")
public class Splink extends JFrame
{
  public static final String QUERY_REPO_NAME_DESCRIPTION = "SELECT ?name ?label WHERE {?_ sys:repositoryID ?name. ?_ rdfs:label ?label}";
  public static final String DEFAULT_QUERY = "SELECT\n\t*\nWHERE\n{\n\t?s ?p ?o\n}";
  public static final String PROPERTIES_FILE = System.getProperty("user.home") + File.separator + ".splink";
  public static final String QUERY_NAME_KEY_BASE = "query.name.";
  public static final String QUERY_VALUE_KEY_BASE = "query.value.";
  public static final String URI_IDENTIFIER_RE = "[a-zA-Z_0-9\\.\\-]*";
  public static final String PROTOCOL_IDENTIFIER_RE = "\\w*";
  public static final String SHORT_URI_RE = format("%s(?<!_):%s", URI_IDENTIFIER_RE, URI_IDENTIFIER_RE);
  public static final String LONG_URI_RE = format("%s://.*", PROTOCOL_IDENTIFIER_RE);
  public static final String LITERAL_RE = format("\"(\\p{ASCII}*)\"((@|\\^\\^)(%s|%s|<%s>))?", URI_IDENTIFIER_RE, SHORT_URI_RE, LONG_URI_RE);
  public static final String BLANK_NODE_RE = format("_:%s", URI_IDENTIFIER_RE);
  public static final int NO_QUERY_LIMIT = Integer.MIN_VALUE;
  

  private int mQueryLimit;
  private UndoManager mCurrentUndoManagaer;
  private Properties mProperties;
  private JScrollPane mPrefixScroll;
  private JScrollPane mContextScroll;
  private JTabbedPane mEditorTab;
  private JTable mPrefix;
  private JTable mContext;
  private JScrollPane mResultArea;
  private JTable mResult;
  private JTextArea mErrorText;
  private JMenu mRepositoryListMenu;
  private JLabel mStatusBar;
  private Map<String, String> mRepositoryList;
  private JCheckBoxMenuItem mShowLongUriCbmi;
  private JCheckBoxMenuItem mShowInferredCbmi;
  private TableModel mPrefixTable;
  private TableModel mContextTable;
  private String mQueryPrefixString;
  private Repository mRepository;
  private RepositoryConnection mConnection;
  private Map<String, String> mNameSpaceMap;
  private Map<String, Pattern> mPrefixRegex;
  private Stack<String> mQueryStack;
  private String mLastQuery;
  private JPopupMenu mTablePopupMenu;
  private JTable mPopupTable;
  private int mPopupTableRow;
  private int mPopupTableColumn;
  private Map<JEditorPane, UndoManager> mEditorUndoManagerMap;
  private StringBuffer mKillRing = new StringBuffer();
  
  enum ResourceType
  {    
    SHORT_URI("^" + SHORT_URI_RE + "$"),
    LONG_URI("^" + LONG_URI_RE + "$"),
    BLANK_NODE("^" + BLANK_NODE_RE + "$"),
    LITERAL(LITERAL_RE);
    
    private final Pattern mPattern;
    
    ResourceType(String regex)
    {
      mPattern = Pattern.compile(regex);
    }
    
    public boolean isMatch(String uri)
    {
      return mPattern.matcher(uri).matches();
    }
    
    static ResourceType establishType(String uri)
    {
      for (ResourceType type: values())
        if (type.isMatch(uri))
          return type;
      
      return null;
    }
    
    public Matcher parse(String resource)
    {
      Matcher matcher = mPattern.matcher(resource);
      matcher.matches();
      return matcher;
    }
  }
  
  enum Property
  {
    SESAME_HOST("sesame.host", String.class, "localhost"),
    SESAME_PORT("sesame.port", Integer.class, 8080),
    SESAME_REPOSITORY("sesame.repository", String.class, "SYSTEM"),
    
    QUERY_RESULT_LIIMT("query.result.limit", Integer.class, 100),

    EDITOR_SIZE("gui.editor.size", Dimension.class, new Dimension(300, 250)), 
    EDITOR_FONT("gui.editor.font", Font.class, new Font("Courier", Font.BOLD, 18)),
    EDITOR_FONT_CLR("gui.editor.color", Color.class, Color.DARK_GRAY),
    EDITOR_TAB_SIZE("gui.editor.tabsize", Integer.class, 2),
    EDITOR_CURRENT_QUERY("gui.editor.current-query", Integer.class, 0),
    
    PREFIX_SIZE("gui.prefix.size", Dimension.class, new Dimension(600, 125)), 
    PREFIX_FONT("gui.prefix.font", Font.class, new Font("Courier", Font.BOLD, 18)),
    PREFIX_PREFIX_FONT_CLR("gui.prefix.prefix.color", Color.class, Color.DARK_GRAY),
    PREFIX_VALUE_FONT_CLR("gui.prefix.value.color", Color.class, Color.GRAY),
    PREFIX_COL1_WIDTH("gui.prefix.col1.width", Integer.class, 100),
    PREFIX_COL2_WIDTH("gui.prefix.col2.width", Integer.class, 500),

    CONTEXT_SIZE("gui.context.size", Dimension.class, new Dimension(600, 125)), 
    CONTEXT_FONT("gui.context.font", Font.class, new Font("Courier", Font.BOLD, 18)),
    CONTEXT_FONT_CLR("gui.context.prefix.color", Color.class, Color.DARK_GRAY),
    
    ERROR_FONT("gui.error.font", Font.class, new Font("Courier", Font.BOLD, 15)),
    ERROR_FONT_CLR("gui.error.color", Color.class, Color.RED.darker().darker()),
    
    TABLE_HEADER_FONT("gui.table.header.font", Font.class, new Font("Courier", Font.BOLD, 20)),
    TABLE_HEADER_FOREGROUND_CLR("gui.table.header.foreground", Color.class, new Color(30, 30, 30)),
    TABLE_HEADER_BACKGROUND_CLR("gui.table.header.background", Color.class, new Color(230, 230, 230)),
    
    RESULT_SIZE("gui.result.size", Dimension.class, new Dimension(900, 400)),
    RESULT_FONT("gui.result.font", Font.class, new Font("Courier", Font.BOLD, 15)),
    RESULT_FONT_CLR("gui.result.color", Color.class, Color.DARK_GRAY),
    RESULT_MESSAGE_CLR("gui.result.message-color", Color.class, Color.LIGHT_GRAY),
    
    OPTION_SHOW_LONG_URI("option.show.longuri", Boolean.class, false),
    OPTION_SHOW_INFERRED("option.show.inferred", Boolean.class, true);
    
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
    constructUi(getContentPane());
    initializeRepositoryList();
    initializeRepository(SESAME_REPOSITORY.getString());
  }

  private void initializeRepositoryList()
  {
    initializeRepository("SYSTEM");
    
    // get repo list

    performQuery(mQueryPrefixString + QUERY_REPO_NAME_DESCRIPTION, 
      false, false, new QueryResultsProcessor()
    {
      public int process(TupleQueryResult result)
        throws QueryEvaluationException
      {
        mRepositoryList = new HashMap<String, String>();
        String nameColumn = result.getBindingNames().get(0);
        String labelColumn = result.getBindingNames().get(1);
        while (result.hasNext())
        {
          BindingSet row = result.next();
          mRepositoryList.put(row.getValue(nameColumn)
            .stringValue(), row.getValue(labelColumn).stringValue());
        }


        boolean found = false;
        ButtonGroup radioButtonGroup = new ButtonGroup();
        mRepositoryListMenu.removeAll();
        for (final String repositoryName : mRepositoryList.keySet())
        {
          JRadioButtonMenuItem button =
            new JRadioButtonMenuItem(new AbstractAction(repositoryName)
            {
              public void actionPerformed(ActionEvent arg0)
              {
                initializeRepository(repositoryName);
                SESAME_REPOSITORY.set(repositoryName);
              }
            });

          button.setToolTipText(mRepositoryList.get(repositoryName));
          
          if (SESAME_REPOSITORY.getString().equals(repositoryName))
          {
            button.setSelected(true);
            found = true;
          }

          radioButtonGroup.add(button);
          mRepositoryListMenu.add(button);
        }

        // if the repository in the properties is not in the store, default
        // to one in the store

        if (!found)
          SESAME_REPOSITORY.set(mRepositoryList.get(0));

        return mRepositoryList.size();
      }
      
      public int process(GraphQueryResult result)
      {
        throw new UnsupportedOperationException();
      }

      public boolean process(boolean result)
      {
        throw new UnsupportedOperationException();
      }
    });
  }
  
  private void initializeRepository(String repositoryName)
  {
    try
    {
      mRepository =
        new HTTPRepository(String.format("http://%s:%d/openrdf-sesame",
          SESAME_HOST.getString(), SESAME_PORT.getInteger()), repositoryName);

      mRepository.initialize();
      mConnection = mRepository.getConnection();
      initializePrefixes();
      initalizeContext(repositoryName);
      
      // set frame title
      
      setTitle(String.format("http://%s:%d/openrdf-sesame/%s (%d)",
        SESAME_HOST.getString(), SESAME_PORT.getInteger(),
        repositoryName, mConnection.size()));
      
      setResultAreaMessage(repositoryName + " is ready!", 100);
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

    // initialize the queries which are being edited
    
    for (Object key: mProperties.keySet())
      debugMessage("%s: %s", key, mProperties.get(key));
  }  
  
  private void initalizeContext(final String repositoryName)
  {
    new Thread()
    {
      public void run()
      {
        try
        {
          String message =
            format("initializing %s context...", repositoryName);
          setMessage(message);
          JLabel info = new JLabel(message);
          info.setFont(CONTEXT_FONT.getFont());
          info.setForeground(CONTEXT_FONT_CLR.getColor());
          info.setHorizontalAlignment(JLabel.CENTER);
          info.setHorizontalAlignment(JLabel.CENTER);
          mContextScroll.setViewportView(info);

          // create a table model

          DefaultTableModel contextTable = new DefaultTableModel()
          {
            public boolean isCellEditable(int row, int col)
            {
              return false;
            }
          };
          contextTable.addColumn("context");

          // get the context values

          RepositoryResult<Resource> context = mConnection.getContextIDs();

          while (context.hasNext())
          {
            String contextUri = context.next().toString();

            if (!mShowLongUriCbmi.getState())
              contextUri = shortUri(contextUri);

            contextTable.addRow(new String[]
            {
              contextUri
            });
          }

          mContextTable = contextTable;
          if (null != mContext)
          {
            mContext.setModel(mContextTable);
            mContext.getColumnModel().getColumn(0)
              .setHeaderRenderer(mTableHeaderRenderer);
          }

          mContextScroll.setViewportView(mContext);
          setMessage("initialized context.");
        }
        catch (RepositoryException e)
        {
          e.printStackTrace();
        }
      }
    }.start();
  }

  private void initializePrefixes()
  {
    try
    {
      setMessage("initializing namespace...");

      // init name-space map and a buffer to build the query prefix string

      mNameSpaceMap = new HashMap<String, String>();
      mPrefixRegex = new HashMap<String, Pattern>();
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
        mPrefixRegex.put(
          nameSpace.getName(),
          Pattern.compile(String.format("^%s(%s)$",
            Pattern.quote(nameSpace.getName()), Splink.URI_IDENTIFIER_RE)));
        prefixTable.addRow(new String[]
        {
          nameSpace.getPrefix(), nameSpace.getName()
        });
        queryPrefixBuffer.append(String.format("PREFIX %s:<%s>\n",
          nameSpace.getPrefix(), nameSpace.getName()));
      }

      // init master query prefix string

      mQueryPrefixString = queryPrefixBuffer.toString();

      // init master prefix table

      mPrefixTable = prefixTable;
      if (null != mPrefix)
      {
        mPrefix.setModel(mPrefixTable);
        TableColumn prefixCol = mPrefix.getColumnModel().getColumn(0);
        TableColumn valueCol = mPrefix.getColumnModel().getColumn(1);

        prefixCol.setPreferredWidth(PREFIX_COL1_WIDTH.getInteger());
        prefixCol.setHeaderRenderer(mTableHeaderRenderer);
        valueCol.setPreferredWidth(PREFIX_COL2_WIDTH.getInteger());
        valueCol.setHeaderRenderer(mTableHeaderRenderer);
        adjustTablesColumns(mPrefix);
      }

      setMessage("initialized namespace.");
    }
    catch (Exception e)
    {
      setError(e);
    }
  }

  public String shortUri(String longUri)
  {
    if (LONG_URI.isMatch(longUri))
    {
      URI uri = mConnection.getValueFactory().createURI(longUri);
      String prefix = mNameSpaceMap.get(uri.getNamespace());
      return null != prefix ? prefix + uri.getLocalName() : longUri;
    }
    
    return longUri;
  }

  public String longUri(String shortUri)
  {
    for (String name: mNameSpaceMap.keySet())
    {
      String perfix = mNameSpaceMap.get(name);
      if (shortUri.startsWith(perfix))
        return shortUri.replace(perfix, name);
    }
    
    return shortUri;
  }
  
  // add prefix table renderer

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
  
  // add context table render 

  TableCellRenderer mContextTableRenderer = new DefaultTableCellRenderer()
  {
    Color[] mContextRowColors = {
      Color.WHITE,
      new Color(220, 230, 255),
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

      c.setForeground(CONTEXT_FONT_CLR.getColor());
      c.setBackground(mContextRowColors[row % mContextRowColors.length]);
      
      return c;
    }
  };
  
  // add prefix area

  TableCellRenderer mResultTableRenderer = new DefaultTableCellRenderer()
  {
    Color[] mResultRowColors = {
      Color.WHITE,
      new Color(255, 215, 255),
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
  
  private void constructUi(Container frame)
  {
    // configure frame

    frame.setLayout(new BorderLayout());

    // construct gui elements 

    constructMenus();
    constructEditorArea();
    constructPrefixArea();
    constructContextArea();
    constructErrorTextArea();
    constructResultTable();

    // create status bar

    mStatusBar = new JLabel(" ");

    // create the table mouse listener

    MouseListener ml = new MouseAdapter()
    {
      public void mouseClicked(MouseEvent e)
      {
        handleMouseClick(e);
      }            
    };

    mResult.addMouseListener(ml);
    mPrefix.addMouseListener(ml);
    mContext.addMouseListener(ml);

    // composit the gui frame
    
    constructFrame(frame);

    // adjust tool-tip timeout

    ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);

    // attach shutdown hook

    Runtime.getRuntime().addShutdownHook(new Thread()
    {
      public void run()
      {
        shutdownHook();
      }
    });

    // update the enabled state of our actions

    updateEnabled();

    // make frame visible

    pack();
    setVisible(true);

    // request focus for current editor

    getCurrentEditor().requestFocus();
  }

  private int getEditorCount()
  {
    return mEditorTab.getTabCount();
  }

  private JEditorPane getCurrentEditor()
  {
    return mEditorTab.getTabCount() == 0
      ? null
      : getEditor(mEditorTab.getSelectedIndex());
  }
  
  private int getEditorIndex()
  {
    return mEditorTab.getSelectedIndex();
  }

  @SuppressWarnings("unused")
  private JEditorPane getEditor()
  {
    return getEditor(getEditorIndex());
  }

  private JEditorPane getEditor(int index)
  {
    if (index >= mEditorTab.getTabCount())
      return null;
    JScrollPane scroll = (JScrollPane)mEditorTab.getComponent(index);
    return (JEditorPane)scroll.getViewport().getView();
  }

  private void setEditor(int index)
  {
    mEditorTab.setSelectedIndex(index);
  }
  
  private void constructResultTable()
  {
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
    
    // look for popup menu mouse events
    
    mResult.addMouseListener(new PopupListener(mResult));
  }

  private void constructErrorTextArea()
  {
    // create error text area

    mErrorText = new JTextArea();
    mErrorText.setFont(ERROR_FONT.getFont());
    mErrorText.setForeground(ERROR_FONT_CLR.getColor());
    mErrorText.setEditable(false);
    mErrorText.getDocument().putProperty(PlainDocument.tabSizeAttribute,
      EDITOR_TAB_SIZE.getInteger());
  }

  private void constructPrefixArea()
  {
    // create prefix table

    mPrefix = new JTable()
    {
      public TableCellRenderer getCellRenderer(int row, int column)
      {
        return mPrefixTableRenderer;
      }
    };
    mPrefix.setFont(PREFIX_FONT.getFont());
    mPrefix.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
    
    // look for popup menu mouse events
    
    mPrefix.addMouseListener(new PopupListener(mPrefix));
  }

  class PopupListener extends MouseAdapter
  {
    private final JTable mTable;

    public PopupListener(JTable table)
    {
      mTable = table;
    }

    public void mousePressed(MouseEvent e)
    {
      showPopup(e);
    }

    public void mouseReleased(MouseEvent e)
    {
      showPopup(e);
    }

    private void showPopup(MouseEvent e)
    {
      if (e.isPopupTrigger())
      {
        mPopupTableRow = mTable.rowAtPoint(e.getPoint());
        mPopupTableColumn = mTable.columnAtPoint(e.getPoint());
        mPopupTable = mTable;
        mTablePopupMenu.show(e.getComponent(), e.getX(), e.getY());
      }
    }
  }
  
  private void constructContextArea()
  {
    // create context table

    mContext = new JTable()
    {
      public TableCellRenderer getCellRenderer(int row, int column)
      {
        return mContextTableRenderer;
      }
    };
    mContext.setFont(CONTEXT_FONT.getFont());
    mContext.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
    
    // look for popup menu mouse events
    
    mContext.addMouseListener(new PopupListener(mContext));
  }

  private void constructFrame(Container frame)
  {
    // compose all the elements into the display

    mEditorTab.setPreferredSize(EDITOR_SIZE.getDimension());

    mPrefixScroll = new JScrollPane(mPrefix);
    mPrefixScroll.setPreferredSize(PREFIX_SIZE.getDimension());

    mContextScroll = new JScrollPane(mContext);
    mContextScroll.setPreferredSize(CONTEXT_SIZE.getDimension());
    mContextScroll.setAlignmentY(Component.CENTER_ALIGNMENT);

    mResultArea = new JScrollPane(mResult);
    mResultArea.setPreferredSize(RESULT_SIZE.getDimension());
    
    JSplitPane contextPrefixSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mContextScroll, mPrefixScroll);
    contextPrefixSplit.setBorder(null);
    
    JSplitPane topSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mEditorTab, contextPrefixSplit);
    topSplit.setBorder(null);
    
    JSplitPane masterSplit =new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSplit, mResultArea);
    masterSplit.setBorder(null);
    
    frame.add(masterSplit, BorderLayout.CENTER);
    frame.add(mStatusBar, BorderLayout.SOUTH);
  }

  private void constructEditorArea()
  {
    // create the editor tabbed pane

    mEditorTab = new JTabbedPane();
    mEditorTab.addChangeListener(new ChangeListener()
    {
      public void stateChanged(ChangeEvent arg0)
      {
        mCurrentUndoManagaer = mEditorUndoManagerMap.get(getCurrentEditor());
        updateEnabled();
      }
    });

    // add editors

    boolean done = false;
    for (int i = 0; !done; ++i)
    {
      String queryName = mProperties.getProperty(QUERY_NAME_KEY_BASE + i);
      String queryValue = mProperties.getProperty(QUERY_VALUE_KEY_BASE + i);

      if (null != queryName && null != queryValue)
        addEditor(queryName, queryValue);
      else
        done = true;
    }

    // if no editors create a new one

    if (0 == getEditorCount())
      addNewEditor();

    // be sure we select the last editor being edited at

    setEditor(EDITOR_CURRENT_QUERY.getInteger());
  }
  
  private void constructMenus()
  {
    JMenuBar menuBar = new JMenuBar();
    setJMenuBar(menuBar);

    // file menu

    JMenu fileMenu = new JMenu("File");
    menuBar.add(fileMenu);
    fileMenu.add(mSave);

    // edit menu

    JMenu editMenu = new JMenu("Edit");
    menuBar.add(editMenu);
    editMenu.add(mCopyQuery);
    editMenu.addSeparator();

    // store menu

    JMenu storeMenu = new JMenu("Store");
    menuBar.add(storeMenu);
    storeMenu.add(mReloadNameSpace);
    mRepositoryListMenu = new JMenu("Repositories");
    storeMenu.add(mRepositoryListMenu);

    // query menu

    JMenu queryMenu = new JMenu("Query");
    menuBar.add(queryMenu);
    queryMenu.add(mSubmiteQuery);
    queryMenu.add(mPreviousQuery);
    queryMenu.addSeparator();
    queryMenu.add(mNewQueryTab);
    queryMenu.add(mQueryRemoveTab);
    queryMenu.add(mQueryLeft);
    queryMenu.add(mQueryRight);

    // add the query limit menu

    JMenu limitMenu = new JMenu("Result Size");
    queryMenu.addSeparator();
    queryMenu.add(limitMenu);
    
    ButtonGroup limitButtonGroup = new ButtonGroup();
    for (QueryLimitAction limit : mQuerylimitActions)
    {
      JRadioButtonMenuItem button = new JRadioButtonMenuItem(limit);
      limitButtonGroup.add(button);
      limitMenu.add(button);
      if (limit.getLimit() == QUERY_RESULT_LIIMT.getInteger())
        button.doClick();
    }

    if (limitButtonGroup.getSelection() == null)
    {
      JRadioButtonMenuItem button =
        new JRadioButtonMenuItem(new QueryLimitAction(
          QUERY_RESULT_LIIMT.getInteger(), getKeyStroke(VK_5, META_MASK)));
      limitButtonGroup.add(button);
      limitMenu.add(button);
      button.doClick();
    }

    // options menu

    JMenu optionMenu = new JMenu("Options");
    menuBar.add(optionMenu);
    optionMenu.add(mShowLongUriCbmi = new JCheckBoxMenuItem(mShowLongUri));
    mShowLongUriCbmi.setSelected(OPTION_SHOW_LONG_URI.getBoolean());
    optionMenu.add(mShowInferredCbmi =
      new JCheckBoxMenuItem(mShowInferred));
    mShowInferredCbmi.setSelected(OPTION_SHOW_INFERRED.getBoolean());

    // create the table popup menu

    mTablePopupMenu = new JPopupMenu();
    mTablePopupMenu.add(new JMenuItem(mCopyTableCell));
    mTablePopupMenu.add(new JMenuItem(mInsertCellValue));
  }

  private void handleMouseClick(MouseEvent e)
  {
    if (e.getClickCount() == 2)
    {
      JTable target = (JTable)e.getSource();
      int row = target.getSelectedRow();
      int column = target.getSelectedColumn();
      if (target == mResult)
        inspectResource(target.getModel().getValueAt(row, column)
          .toString());
      if (target == mPrefix)
        inspectPrefix(target.getModel().getValueAt(row, 0).toString() +
          ":");
      if (target == mContext)
        inspectContext(target.getModel().getValueAt(row, 0).toString());
    }
  }
  
  private void addEditor(String name, String query)
  {
    // create and configure the editor
    
    JEditorPane editor = new JEditorPane();    
    editor.setFont(EDITOR_FONT.getFont());
    editor.setForeground(EDITOR_FONT_CLR.getColor());
    editor.getDocument().putProperty(PlainDocument.tabSizeAttribute,
      EDITOR_TAB_SIZE.getInteger());

    // get the action map and input map
    
    ActionMap actionMap = editor.getActionMap();
    InputMap inputMap = editor.getInputMap();
    
    // add undo and redo
    
    editor.getActionMap().put(mUndoAction.getDescription(), mUndoAction);
    editor.getInputMap().put(mUndoAction.getKeyStroke(), mUndoAction.getDescription());
    editor.getActionMap().put(mRedoAction.getDescription(), mRedoAction);
    editor.getInputMap().put(mRedoAction.getKeyStroke(), mRedoAction.getDescription());

    // add emacs options
    
    actionMap.put(mEditKill.getDescription(), mEditKill);
    inputMap.put(mEditKill.getKeyStroke(), mEditKill.getDescription());
    editor.getInputMap().put(getKeyStroke(VK_Y, CTRL_MASK), DefaultEditorKit.pasteAction);
    editor.getInputMap().put(getKeyStroke(VK_SLASH, CTRL_MASK), mUndoAction.getDescription());
    
    editor.addCaretListener(new CaretListener()
    {
      private int mDot = Integer.MIN_VALUE;
      private int mMark = Integer.MIN_VALUE;
      
      public void caretUpdate(CaretEvent event)
      {
        if (event.getDot() != mDot ||  event.getMark() != mMark)
          mKillRing = new StringBuffer();

        mDot = event.getDot();
        mMark = event.getMark();
      }
    });

    // set the editor text
    
    editor.setText(query);
    
    // add the undo manager
    
    mCurrentUndoManagaer = new UndoManager()
    {
      @Override
      public void undoableEditHappened(UndoableEditEvent e)
      {
        super.undoableEditHappened(e);
        updateEnabled();
      }
    };
    editor.getDocument().addUndoableEditListener(mCurrentUndoManagaer);

    if (null == mEditorUndoManagerMap)
      mEditorUndoManagerMap = new HashMap<JEditorPane, UndoManager>();
    mEditorUndoManagerMap.put(editor, mCurrentUndoManagaer);
    
    // handle adding the editor to the ui
    
    JScrollPane scroll = new JScrollPane(editor);
    mEditorTab.add(scroll);
    mEditorTab.setSelectedComponent(scroll);
    int index = mEditorTab.getSelectedIndex();
    mEditorTab.setTitleAt(index, name);
    updateEnabled();
  }

  public void setResultAreaMessage(String message, float size)
  {
    JLabel resultComponent = new JLabel(message);
    resultComponent.setFont(RESULT_FONT.getFont().deriveFont(size));
    resultComponent.setForeground(RESULT_MESSAGE_CLR.getColor());
    resultComponent.setVerticalAlignment(JLabel.CENTER);
    resultComponent.setHorizontalAlignment(JLabel.CENTER);
    setResultComponent(resultComponent);
  }
  
  public void setResultComponent(Component c)
  {
    if (c instanceof JTable)
      adjustTablesColumns((JTable)c);
    mResultArea.setViewportView(c);
  }
  
  public void adjustTablesColumns(JTable table)
  {
    TableColumnModel columnModel = table.getColumnModel();
    TableModel model = table.getModel();
    int count = model.getColumnCount();
    
    for (int c = 0; c < count; ++c)
    {
      TableColumn column = columnModel.getColumn(c);

      int width =
        establishStringWidth(
          column.getHeaderValue().toString(),
          column.getHeaderRenderer()
            .getTableCellRendererComponent(table, null, false, false, 0, c)
            .getFont());

      for (int r = 0; r < model.getRowCount(); ++r)
      {
        Object text = model.getValueAt(r, c);
        if (null != text)
          width =
            max(width, establishStringWidth(text.toString(), table.getFont()));
      }

      column.setPreferredWidth(width);
    }
  }

  class SplinkResource
  {
    private final ResourceType mType;
    private final String mValue;
    
    public SplinkResource(String value)
    {
      mValue = value;
      mType = establishType(getValue());
    }

    public String getValue()
    {
      return mValue;
    }

    public ResourceType getType()
    {
      return mType;
    }
    
    public String getCanonical()
    {
      switch (mType)
      {
      case LONG_URI:
        return "<" + getValue() + ">";
      case LITERAL:
        Matcher m = LITERAL.parse(getValue());
        String base = m.group(1);
        String type = m.group(2);
        if (null == type)
          type = "";
        base = base.replaceAll("\"", "\\\\\"");
        String quote = (base.contains("\n") || base.contains("\r")) ? "\"\"\"" : "\"";
        return format("%s%s%s%s\n", quote, base, quote, type);
      }

      return getValue();
    }
    
    public String toString()
    {
      return getCanonical();
    }
  }
  
  
  private void inspectResource(String resourceString)
  {
    SplinkResource resource = new SplinkResource(resourceString);

    switch (resource.getType())
    {
    case BLANK_NODE:
      setError("sorry you can't inspect blank nodes like:"
        + "\n\n   %s\n\nif you now how " + "to make a query which CAN\n"
        + "ispect such nodes context the splink\n"
        + "developers at github.com.", resource);
      break;
    case LITERAL:
      String query =
        format("SELECT * " + "WHERE { ?subject ?predicate ?object "
          + "FILTER ( ?subject = %s || ?object = %s)}",
          resource.getCanonical(), resource.getCanonical());
      submitQuery(query, true, true);
      break;
    default:
      submitQuery("DESCRIBE " + resource, true, true);
    }
  }
  
  private void inspectPrefix(String prefix)
  {
    String query = format(
      "construct {?s ?p ?o}"  +
      "where { ?s ?p ?o. " +
      "  filter(regex(str(?s), str(%s)) || regex(str(?o), str(%s)))}",
      prefix, prefix);
    submitQuery(query, true, true);
  }

  private void inspectContext(String context)
  {
    SplinkResource resource = new SplinkResource(context);
    switch (resource.getType())
    {
    case SHORT_URI:
    case LONG_URI:
      String query = String.format(
        "SELECT * " +
        "FROM %s " +
        "WHERE { ?subject ?predicate ?object }",
        resource); 
      submitQuery(query, true, true);
      break;
    }
  }
  
  
  private void submitQuery()
  {
    submitQuery(getCurrentQuery(), true, true);
  }
  
  private String getCurrentQuery()
  {
    JEditorPane editor = getCurrentEditor();
    return editor.getText();
  }

  private void pushQuery(String query)
  {
    if (null == mQueryStack)
      mQueryStack = new Stack<String>();

    if (null != mLastQuery)
      mQueryStack.push(mLastQuery);
    
    mLastQuery = query;
    
    updateEnabled();
  }
  
  private void popQuery()
  {
    if (null != mQueryStack && !mQueryStack.isEmpty())
      submitQuery(mLastQuery = mQueryStack.pop(), false, false);

    updateEnabled();
  }
  
  private void submitQuery(final String query, final boolean appendPrefix,
    boolean pushQuery)
  {
    final String fullQuery = appendPrefix
      ? mQueryPrefixString + query
      : query;
    
    if (pushQuery)
      pushQuery(fullQuery);
          
    new Thread()
    {
      public void run()
      {
        boolean submitEnabled = mSubmiteQuery.isEnabled();
        boolean previousEnabled = mPreviousQuery.isEnabled();

        mSubmiteQuery.setEnabled(false);
        mPreviousQuery.setEnabled(false);

        performQuery(fullQuery, mShowInferredCbmi.isSelected(), true,
          mDefaultResultsProcessor);

        mSubmiteQuery.setEnabled(submitEnabled);
        mPreviousQuery.setEnabled(previousEnabled);
      }
    }.start();
  }
  
  private Integer establishStringWidth(String string, Font font)
  {
    FontMetrics metrics = getFontMetrics(font);
    return metrics.stringWidth(" " + string);
  }

  public interface QueryResultsProcessor
  {
    int process(TupleQueryResult result) throws QueryEvaluationException;

    int process(GraphQueryResult result) throws QueryEvaluationException;
    
    boolean process(boolean result);
  }
  
  private QueryResultsProcessor mDefaultResultsProcessor =
    new QueryResultsProcessor()
    {
      public int process(TupleQueryResult result)
        throws QueryEvaluationException
      {
        // create the table model

        DefaultTableModel tm = new DefaultTableModel()
        {
          public boolean isCellEditable(int row, int column)
          {
            return false;
          }
        };

        // add columnds to table

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
            if (!mShowLongUriCbmi.getState())
              uri = shortUri(uri);
            row[columnMap.get(binding)] = uri;
          }

          tm.addRow(row);
        }

        // update the display

        mResult.setModel(tm);
        TableColumnModel columnModel = mResult.getColumnModel();
        for (int i = 0; i < tm.getColumnCount(); ++i)
          columnModel.getColumn(i).setHeaderRenderer(mTableHeaderRenderer);
        setResultComponent(mResult);

        // return row count

        return tm.getRowCount();
      }
      
      public int process(GraphQueryResult result)
        throws QueryEvaluationException
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

        for (String name : new String[]{"subject", "predicate", "object"}) 
          tm.addColumn(name);

        // populate the table

        while (result.hasNext())
        {
          Vector<String> row = new Vector<String>();
          Statement rowData = result.next();
          if (mShowLongUriCbmi.getState())
          {
            row.add(rowData.getSubject().toString());
            row.add(rowData.getPredicate().toString());
            row.add(rowData.getObject().toString());
          }
          else
          {
            row.add(shortUri(rowData.getSubject().toString()));
            row.add(shortUri(rowData.getPredicate().toString()));
            row.add(shortUri(rowData.getObject().toString()));
          }

          tm.addRow(row);
        }

        // update the display

        mResult.setModel(tm);
        for (int i = 0; i < tm.getColumnCount(); ++i)
          mResult.getColumnModel().getColumn(i).setHeaderRenderer(mTableHeaderRenderer);
        setResultComponent(mResult);

        // return row count

        return tm.getRowCount();
      }

      public boolean process(boolean result)
      {
        setResultAreaMessage(format("%b", result).toUpperCase(), 150);
        return result;
      }
    };
  
  public void performQuery(String queryString, boolean includeInffered,
    boolean limitResults, QueryResultsProcessor resultProcessor)
  {
    try
    {
      ParsedQuery parsedQuery =
        QueryParserUtil.parseQuery(SPARQL, queryString, null);

      // if there is a query limit, apply it

      final AtomicInteger actualLimit = new AtomicInteger(NO_QUERY_LIMIT);
      if (limitResults && mQueryLimit != NO_QUERY_LIMIT &&
        !(parsedQuery instanceof ParsedGraphQuery))
      {
        final AtomicBoolean hasLimit = new AtomicBoolean(false);
        parsedQuery.getTupleExpr().visit(
          new QueryModelVisitorBase<Exception>()
          {
            public void meet(Slice node) throws Exception
            {
              actualLimit.set(node.getLimit());
              hasLimit.set(true);
            }
          });

        if (!hasLimit.get())
        {
          queryString += "\nLIMIT " + mQueryLimit;
          actualLimit.set(mQueryLimit);
        }
      }

      debugMessage(queryString);

      // register start time of expression

      long startTime = System.currentTimeMillis();

      // if this is a slice create, an ask query

      if (parsedQuery instanceof ParsedBooleanQuery)
      {
        String message = "Asking...";
        setMessage(message);
        setResultAreaMessage(message, 50);

        BooleanQuery query =
          mConnection.prepareBooleanQuery(QueryLanguage.SPARQL, queryString);
        query.setIncludeInferred(includeInffered);
        boolean result = query.evaluate();
        resultProcessor.process(result);
        setMessage("seconds: %2.2f, result: %b",
          (System.currentTimeMillis() - startTime) / 1000.f, result);
      }

      // if this is a projection, create an tuple query

      else if (parsedQuery instanceof ParsedTupleQuery)
      {
        String message = format("Querying%s...", actualLimit.get() == NO_QUERY_LIMIT
          ? " (no limit)"
          : " with limit " + actualLimit.get());
        setMessage(message);
        setResultAreaMessage(message, 50);
        
        TupleQuery query =
          mConnection.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        query.setIncludeInferred(includeInffered);
        TupleQueryResult result = query.evaluate();
        int rows = resultProcessor.process(result);
        int columns = result.getBindingNames().size();
        setMessage("seconds: %2.2f, cols: %d, rows: %d%s",
          (System.currentTimeMillis() - startTime) / 1000.f, columns, rows,
          rows == actualLimit.get()
            ? " (limited)"
            : "");
      }

      // if this is a reduced, create an graph query

      else if (parsedQuery instanceof ParsedGraphQuery)
      {
        String message = format("Describing%s...", actualLimit.get() == NO_QUERY_LIMIT
          ? " (no limit)"
          : " with limit " + actualLimit.get());
        setMessage(message);
        setResultAreaMessage(message, 50);
        GraphQuery query =
          mConnection.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
        query.setIncludeInferred(includeInffered);
        int rows = resultProcessor.process(query.evaluate());
        //exportGraph(query.evaluate());
        setMessage("seconds: %2.2f, cols: %d, rows: %d %s",
          (System.currentTimeMillis() - startTime) / 1000.f, 3, rows,
          rows == actualLimit.get()
            ? " (limited)"
            : "");
      }
      else
      {
        setError("Unknown query type: " + parsedQuery);
      }
    }
    catch (Exception e)
    {
      setError(e, "------ query ------\n\n%s\n\n-------------------\n",
        queryString);
    }
  }

  private void exportGraph(GraphQueryResult result)
  {
    try
    {
      StringWriter stringWriter = new StringWriter();
      RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TURTLE, stringWriter);
      
      rdfWriter.startRDF();
      Map<String, String> nameSpace = result.getNamespaces();
      for (String prefix: nameSpace.keySet())
        rdfWriter.handleNamespace(prefix, nameSpace.get(prefix));
      
      while (result.hasNext())
        rdfWriter.handleStatement(result.next());
      rdfWriter.endRDF();
      
      debugMessage(stringWriter.toString());
    }
    catch (QueryEvaluationException e)
    {
      setError(e);
    }
    catch (RDFHandlerException e)
    {
      setError(e);
    }
  }

  public void debugMessage(String message, Object... args)
  {
    out.format(message + "\n", args);
    if (null != mStatusBar)
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
    if (null != mStatusBar)
    {
      String toolTip =
        fullMessage.replaceAll("<", "&lt;").replaceAll(">", "&gt;")
          .replaceAll("\n", "<br>");
      mStatusBar.setToolTipText("<html>" + toolTip + "</html>");
      mStatusBar.setText(fullMessage);
      mStatusBar.setForeground(color);
      mStatusBar.repaint();
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
    
    public String getDescription()
    {
      return (String)getValue(SHORT_DESCRIPTION);
    }

    public KeyStroke getKeyStroke()
    {
      return (KeyStroke)getValue(ACCELERATOR_KEY);
    }
  }

  class QueryLimitAction extends SplinkAction
  {
    private final int mLimit;
    
    public QueryLimitAction(int limit, KeyStroke key)
    {
      super(limit == NO_QUERY_LIMIT
        ? "Unlimited"
        : "Limit " + limit, key, limit == NO_QUERY_LIMIT
        ? "return an unlimited number of rows"
        : "limit query results to " + limit + " rows");
      mLimit = limit;
    }

    public void actionPerformed(ActionEvent arg0)
    {
      mQueryLimit = mLimit;
      if (getLimit() == NO_QUERY_LIMIT)
        setMessage("query results size unlimited");
      else
        setMessage("query results limited to %d rows", getLimit());
      updateEnabled();
    }
    
    public int getLimit()
    {
      return mLimit;
    }
  }

  private QueryLimitAction [] mQuerylimitActions =
    {
      new QueryLimitAction(NO_QUERY_LIMIT, getKeyStroke(VK_0, META_MASK)),
      new QueryLimitAction(10, getKeyStroke(VK_1, META_MASK)),
      new QueryLimitAction(100, getKeyStroke(VK_2, META_MASK)),
      new QueryLimitAction(1000, getKeyStroke(VK_3, META_MASK)),
      new QueryLimitAction(10000, getKeyStroke(VK_4, META_MASK)),
    };
  
  private SplinkAction mSubmiteQuery = new SplinkAction("Submit", getKeyStroke(VK_ENTER, CTRL_MASK),  "perform query in current editor")
  {
    public void actionPerformed(ActionEvent e)
    {
      submitQuery();
    }
  };
  
  private SplinkAction mPreviousQuery = new SplinkAction("Submit Previous", getKeyStroke(VK_BACK_SPACE, CTRL_MASK),  "perform prevousely submited query (back up)")
  {
    {
      setEnabled(false);
    }
    
    public void actionPerformed(ActionEvent e)
    {
      popQuery();
    }
  };
  
  private SplinkAction mShowLongUri = new SplinkAction("Long URI", getKeyStroke(VK_L, META_MASK),  "show result URIs in long form")
  {
    public void actionPerformed(ActionEvent e)
    {
      OPTION_SHOW_LONG_URI.set(mShowLongUriCbmi.isSelected());
      if (null != mLastQuery)
        submitQuery(mLastQuery, false, false);
    }
  };
  
  private SplinkAction mShowInferred = new SplinkAction("Include Inferred", getKeyStroke(VK_I, META_MASK),  "return results which include inferred tripples")
  {
    public void actionPerformed(ActionEvent e)
    {
      OPTION_SHOW_INFERRED.set(mShowInferredCbmi.isSelected());
      if (null != mLastQuery)
        submitQuery(mLastQuery, false, false);
    }
  };  

  private SplinkAction mReloadNameSpace = new SplinkAction("Reinitialize Connection", getKeyStroke(VK_N, META_MASK),  "refresh repository list, prefixes and contexts")
  {
    public void actionPerformed(ActionEvent e)
    {
      initializeRepositoryList();
      initializeRepository(SESAME_REPOSITORY.getString());
    }
  };

  private SplinkAction mNewQueryTab = new SplinkAction("New Query", getKeyStroke(VK_T, META_MASK),  "create a new query editor")
  {
    public void actionPerformed(ActionEvent e)
    {
      addNewEditor();
    }
  };  
  
  private SplinkAction mQueryLeft = new SplinkAction("Left Query", getKeyStroke(VK_LEFT, META_MASK + ALT_MASK),  "select query editor to the left")
  {
    public void actionPerformed(ActionEvent e)
    {
      setEditor(getEditorIndex() - 1);
      updateEnabled();
    }
  };

  private SplinkAction mCopyTableCell = new SplinkAction("Copy Cell Contents", null,  "copy cell contents to the system clipboard")
  {
    public void actionPerformed(ActionEvent e)
    {
      StringSelection ss = new StringSelection(mPopupTable.getValueAt(mPopupTableRow, mPopupTableColumn).toString());
      getToolkit().getSystemClipboard().setContents(ss, null);
    }
  };

  private SplinkAction mInsertCellValue = new SplinkAction("Insert Cell Value", null,  "insert table cell value into current query editor")
  {
    public void actionPerformed(ActionEvent e)
    {
      try
      {
        String value =
          mPopupTable.getValueAt(mPopupTableRow, mPopupTableColumn)
            .toString();

        SplinkResource resource = new SplinkResource(value);
        
        getCurrentEditor().getDocument().insertString(
          getCurrentEditor().getCaretPosition(), resource.getCanonical(), null);
      }
      catch (BadLocationException e1)
      {
        setError(e1);
      }
    }
  };
  
  private SplinkAction mEditKill = new SplinkAction("Kill", getKeyStroke(VK_K, CTRL_MASK),  "cut the text to the end of the line and put it in the clipboard") 
  {
    public void actionPerformed(ActionEvent e)
    {
      try
      {
        // get editor and document
        
        JEditorPane editor = (JEditorPane)e.getSource();
        Document doc = editor.getDocument();
        
        // get the text at the caret
        
        int caret = editor.getCaretPosition();
        String text = doc.getText(caret, doc.getLength() - caret);
        
        // find the next carrage return
        
        int end = text.indexOf("\n");
        
        // if none then assem end of document
        
        if (-1 == end)
          end = text.length();
        
        // if no length and more text assuem at end of line
        
        if (end == 0 && text.length() > 0)
           end = 1;

        // stow text and kill it
        
        mKillRing.append(text.substring(0, end));
        doc.remove(caret, end);
        
        StringSelection ss = new StringSelection(mKillRing.toString());
        getToolkit().getSystemClipboard().setContents(ss, null);
      }
      catch (BadLocationException e1)
      {
        setError(e1);
      }
    }
  };

  private SplinkAction mQueryRight = new SplinkAction("Right Query",
    getKeyStroke(VK_RIGHT, META_MASK + ALT_MASK),
    "select query editor to the right")
  {
    public void actionPerformed(ActionEvent e)
    {
      setEditor(getEditorIndex() + 1);
      updateEnabled();
    }
  };

    
  private SplinkAction mQueryRemoveTab =
    new SplinkAction("Remove Current Query", null,
      "delete current query editor (not undoable!)")
    {
      public void actionPerformed(ActionEvent e)
      {
        mEditorTab.remove(mEditorTab.getSelectedIndex());
        if (mEditorTab.getTabCount() == 0)
          addNewEditor();
        updateEnabled();
      }
    };

  private SplinkAction mCopyQuery = new SplinkAction("Copy Query",
    getKeyStroke(VK_C, META_MASK + SHIFT_MASK),
    "copy the query from the current editor, with the prefix statements prepended, to system clipboard")
  {
    public void actionPerformed(ActionEvent e)
    {
      StringSelection ss =
        new StringSelection(mQueryPrefixString + "\n" + getCurrentQuery());
      getToolkit().getSystemClipboard().setContents(ss, null);
    }
  };
  
  private SplinkAction mSave = new SplinkAction("Save", getKeyStroke(VK_S,
    META_MASK),
    "save the state of all of the current query editors and frame sizes")
  {
    public void actionPerformed(ActionEvent e)
    {
      shutdownHook();
    }
  };

  private SplinkAction mUndoAction = new SplinkAction("Undo", getKeyStroke(
    VK_Z, META_MASK), "undo edit")
  {
    public void actionPerformed(ActionEvent e)
    {
      mCurrentUndoManagaer.undo();
      updateEnabled();
    }
  };
  
  private SplinkAction mRedoAction = new SplinkAction("Redo", getKeyStroke(VK_Z,
    META_MASK + SHIFT_MASK),
    "redo edit")
  {
    public void actionPerformed(ActionEvent e)
    {
      mCurrentUndoManagaer.redo();
      updateEnabled();
    }
  };
  
  protected void updateEnabled()
  {
    if (null != mEditorTab)
    {
      int selected = mEditorTab.getSelectedIndex();
      int count = mEditorTab.getTabCount();

      mPreviousQuery
        .setEnabled(null != mQueryStack && !mQueryStack.isEmpty());

      mQueryRemoveTab.setEnabled(count > 0);
      mQueryLeft.setEnabled(selected > 0);
      mQueryRight.setEnabled(selected < count - 1);

      mUndoAction.setEnabled(null != mCurrentUndoManagaer &&
        mCurrentUndoManagaer.canUndo());
      mRedoAction.setEnabled(null != mCurrentUndoManagaer &&
        mCurrentUndoManagaer.canRedo());
    }
  }

  private void addNewEditor()
  {
    addEditor(NameGenerator.getName(2, NameGenerator.INITIAL_CASE_SPACE), DEFAULT_QUERY);
  }

  private void shutdownHook()
  {
    // save frame sizes
    
    EDITOR_SIZE.set(mEditorTab.getSize());
    PREFIX_SIZE.set(mPrefixScroll.getSize());
    CONTEXT_SIZE.set(mContextScroll.getSize());
    RESULT_SIZE.set(mResultArea.getSize());
    PREFIX_COL1_WIDTH.set(mPrefix.getColumnModel().getColumn(0)
      .getWidth());
    PREFIX_COL2_WIDTH.set(mPrefix.getColumnModel().getColumn(1)
      .getWidth());
    
    QUERY_RESULT_LIIMT.set(mQueryLimit);

    // expunge old edior state
    
    boolean done = false;
    for (int i = 0; !done; ++i)
    {
      String queryNameKey = QUERY_NAME_KEY_BASE + i;
      String queryValueKey = QUERY_VALUE_KEY_BASE + i;

      done = true;
      
      if (null != mProperties.getProperty(queryNameKey))
      {
        mProperties.remove(queryNameKey);
        done = false;
      }
      
      if (null != mProperties.getProperty(queryValueKey))
      {
        mProperties.remove(queryValueKey);
        done = false;
      }
    } 

    // store the query editors

    for (int i = 0; i < getEditorCount(); ++i)
    {
      JEditorPane editor = getEditor(i);
      mProperties.setProperty(QUERY_NAME_KEY_BASE + i, mEditorTab.getTitleAt(i));
      mProperties.setProperty(QUERY_VALUE_KEY_BASE + i, editor.getText());
    }
    
    EDITOR_CURRENT_QUERY.set(mEditorTab.getSelectedIndex());
  }
}


