package org.trebor.splink;

public interface MessageHandler
{
  String handleMessage(String format, Object...args);
  String handleWarning(String format, Object...args);
  String handleError(String format, Object...args);
}
