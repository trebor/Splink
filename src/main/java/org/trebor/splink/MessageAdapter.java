package org.trebor.splink;

import static java.lang.String.format;

public class MessageAdapter implements MessageHandler
{
  public String handleMessage(String format, Object... args)
  {
    return format(format, args);
  }

  public String handleWarning(String format, Object... args)
  {
    return format(format, args);
  }

  public String handleError(String format, Object... args)
  {
    return format(format, args);
  }
}
