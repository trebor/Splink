package org.trebor.splink;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

import static java.lang.String.format;

@SuppressWarnings("serial")
public class MaximumSizeMessageHandlerPanel extends JPanel implements MessageHandler
{
  private String mMessage;
  private Color mColor;
  private final Color mErrorColor;
  private final Color mWarningColor;
  private final Color mMessageColor;
  
  public MaximumSizeMessageHandlerPanel()
  {
    this(Color.white, Color.red, Color.yellow, Color.black);
  }

  public MaximumSizeMessageHandlerPanel(Color background, Color error, Color warning, Color message)
  {
    this.setBackground(background);
    mErrorColor = error;
    mWarningColor = warning;
    mMessageColor = message;
  }
  
  public void paint(Graphics graphics)
  {
    super.paint(graphics);
    Graphics2D g = (Graphics2D)graphics;
    g.setColor(mColor);
    g.drawString(mMessage, 0, 0);
  }

  private String setMessage(Color color, String format, Object... args)
  {
    mMessage = format(format, args);
    mColor = color;
    repaint();
    return mMessage;
  }
  
  public String handleMessage(String format, Object... args)
  {
    return setMessage(mMessageColor, format, args);
  }

  public String handleWarning(String format, Object... args)
  {
    return setMessage(mWarningColor, format, args);
  }

  public String handleError(String format, Object... args)
  {
    return setMessage(mErrorColor, format, args);
  }

  public String handleError(Exception exception, String format,
    Object... args)
  {
    return setMessage(mErrorColor, exception.getMessage() + format, args);
  }

  public String handleError(Exception exception)
  {
    return setMessage(mErrorColor, exception.getMessage());
  }
}
