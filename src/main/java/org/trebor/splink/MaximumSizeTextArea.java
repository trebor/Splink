package org.trebor.splink;

import java.awt.Dimension;
import java.awt.Shape;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JLabel;


@SuppressWarnings("serial")
public class MaximumSizeTextArea extends JLabel
{
  private String mOriginalString;
  
  @Override
  public void setText(String string)
  {
    mOriginalString = string;
    string = string.replace("\n", "<p>");
    string = "<html><center>" + string + "</center></html>";
    super.setText(string);
    resizeFont();
  }

  public String getOriginalText()
  {
    return mOriginalString;
  }
  
  public MaximumSizeTextArea(String content)
  {
    super(content, JLabel.CENTER);
    mOriginalString = content;
    
    addComponentListener(new ComponentAdapter()
    {
      public void componentResized(ComponentEvent e)
      {
        resizeFont();
      }
    });
  }

  private void resizeFont()
  {
    if (getParent() != null)
    {
      Dimension textBounds = getUI().getPreferredSize(this);
      Shape frameBounds = getParent().getBounds();
      
      float fontSize = getFont().getSize2D();

      float newWidth =
        (float)(frameBounds.getBounds().getWidth() / textBounds.getWidth() * fontSize);
      float newHeight =
        (float)(frameBounds.getBounds().getHeight() / textBounds.getHeight() * fontSize);

      setFont(getFont().deriveFont(Math.min(newWidth, newHeight) * 0.9f));
    }
  }
}
