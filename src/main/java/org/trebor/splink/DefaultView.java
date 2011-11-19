package org.trebor.splink;

import java.awt.Component;

import javax.swing.JPanel;

import org.openrdf.repository.RepositoryConnection;

public class DefaultView implements View
{
  public final RepositoryConnection mConnection;
  public final JPanel mViewComponent;
  public final MessageHandler mMessageHandler;
  
  public DefaultView(Splink splink)
  {
    mConnection = splink.getConnection();
    mViewComponent = new JPanel();
    mMessageHandler = splink;
  }
  
  public Component getViewComponent()
  {
    return mViewComponent;
  }

  public ResultsListener getResultsListener()
  {
    return null;
  }

  public MessageHandler getMessageHandler()
  {
    return mMessageHandler;
  }

  public RepositoryConnection getRepositoryConnection()
  {
    return mConnection;
  }
}