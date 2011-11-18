package org.trebor.splink;

import java.awt.Component;

import org.openrdf.repository.RepositoryConnection;

public interface View
{
  Component getViewComponent();
  ResultsListener getResultsListener();
  MessageHandler getMessageHandler();
  RepositoryConnection getRepositoryConnection();
}
