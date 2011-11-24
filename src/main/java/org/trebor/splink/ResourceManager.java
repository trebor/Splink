package org.trebor.splink;

import static java.lang.String.format;
import static org.trebor.splink.ResourceManager.ResourceType.*;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.openrdf.model.Namespace;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

public class ResourceManager
{
  public static final Logger log = Logger.getLogger(ResourceManager.class);

  public static final int LITERAL_STRING_INDEX = 1;
  public static final int LITERAL_SEPERATOR_INDEX = 2;
  public static final int LITERAL_TYPE_INDEX = 3;
  public static final String URI_IDENTIFIER_RE = "[a-zA-Z_0-9\\.\\-]*";
  public static final String URI_BODY_RE = "[#/a-zA-Z_0-9\\.\\-]*";
  public static final String PROTOCOL_IDENTIFIER_RE = "\\w*";
  public static final String SHORT_URI_RE = format("%s(?<!_):%s", URI_IDENTIFIER_RE, URI_IDENTIFIER_RE);
  public static final String LONG_URI_RE = format("%s://%s", PROTOCOL_IDENTIFIER_RE, URI_BODY_RE);
  public static final String LITERAL_RE = format("\"(\\p{ASCII}*)\"(@|\\^\\^)?<?(%s|%s|%s)?>?", LONG_URI_RE, SHORT_URI_RE, URI_IDENTIFIER_RE);
  public static final String BLANK_NODE_RE = format("_:%s", URI_IDENTIFIER_RE);

  private final List<Namespace> mNamespaces;

  private final ValueFactory mValueFactory;

  public enum ResourceType
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
    
    public boolean isMatch(String resource)
    {
      return mPattern.matcher(resource).matches();
    }
    
    static ResourceType establishType(String resource)
    {
      for (ResourceType type: values())
        if (type.isMatch(resource))
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

  public ResourceManager(RepositoryConnection connection) throws RepositoryException
  {
    if (connection != null)
    {
      mNamespaces = connection.getNamespaces().asList();
      mValueFactory = connection.getValueFactory();
    }
    else
    {
      mNamespaces = null;
      mValueFactory = null;
    }
  }

  
  public String shrinkResource(String longResource)
  {
    if (mNamespaces == null)
    {
      log.debug("null namespace");
      return longResource;
    }
    
    if (LONG_URI.isMatch(longResource))
    {
      URI uri = mValueFactory.createURI(longResource);
      for (Namespace namespace : mNamespaces)
        if (namespace.getName().equals(uri.getNamespace()))
          return namespace.getPrefix() + ":" + uri.getLocalName();

      return longResource;
    }
    
    if (LITERAL.isMatch(longResource))
    {
      Matcher m = LITERAL.parse(longResource);
      String string = m.group(LITERAL_STRING_INDEX);
      String seperator = m.group(LITERAL_SEPERATOR_INDEX);
      String type = m.group(LITERAL_TYPE_INDEX);
      if (seperator == null)
        return longResource;

      if (LONG_URI.isMatch(type))
        type = shrinkResource(type);

      return "\"" + string + "\"" + seperator + type;
    }

    return longResource;
  }

  public String growResource(String shortResource)
  {
    if (mNamespaces == null)
      return shortResource;
    
    if (SHORT_URI.isMatch(shortResource))
    {
      for (Namespace name : mNamespaces)
      {
        String prefix = name.getPrefix() + ":";
        if (shortResource.startsWith(prefix))
          return shortResource.replace(prefix, name.getName());
      }
    }
    else if (LITERAL.isMatch(shortResource))
    {
      Matcher m = LITERAL.parse(shortResource);
      String string = m.group(LITERAL_STRING_INDEX);
      String seperator = m.group(LITERAL_SEPERATOR_INDEX);
      String type = m.group(LITERAL_TYPE_INDEX);
      if (seperator == null)
        return shortResource;

      if (SHORT_URI.isMatch(type))
        type = "<" + growResource(type) + ">";

      return "\"" + string + "\"" + seperator + type;
    }

    return shortResource;
  }
}
