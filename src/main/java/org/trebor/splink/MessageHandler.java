package org.trebor.splink;

public interface MessageHandler
{
  public enum Type
  {
    SPLASH(true, false),
    STATUS(false, true),
    BOTH(true, true);
    
    private final boolean mSplash;
    private final boolean mStatus;

    Type(boolean splash, boolean status)
    {
      mSplash = splash;
      mStatus = status;
    }

    public boolean isSplash()
    {
      return mSplash;
    }

    public boolean isStatus()
    {
      return mStatus;
    }
  }
  
  String handleMessage(Type type, String format, Object...args);
  String handleWarning(Type type, String format, Object...args);
  String handleError(Type type, String format, Object...args);
  String handleError(Type type, Exception exception, String format, Object...args);
  String handleError(Type type, Exception exception);
}
