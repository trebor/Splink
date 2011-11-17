package org.trebor.splink;

import java.awt.Component;

public interface View
{
  Component getViewComponent();
  ResultsListener getResultsListener();
  MessageHandler getMessageHandler();
}
