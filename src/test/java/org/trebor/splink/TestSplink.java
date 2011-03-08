package org.trebor.splink;

import static org.junit.Assert.*;
import static org.trebor.splink.Splink.ResourceType.*;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
import org.trebor.splink.Splink.ResourceType;

public class TestSplink
{
  @Test
  public void testUri()
  {
    Map<String, ResourceType> uriExamples = new HashMap<String, ResourceType>();

    uriExamples.put("foo:bar#baz", null);
    uriExamples.put("foo:bar/baz", null);
    
    uriExamples.put("foo:bar", SHORT_URI);
    uriExamples.put("foo:bar.baz", SHORT_URI);
    uriExamples.put("foo_bar:baz_qux", SHORT_URI);
    
    uriExamples.put("_:node15pf8hastx4", BLANK_NODE);
    uriExamples.put("_:x", BLANK_NODE);
    
    uriExamples.put("http://www.google.com/foo#bar", LONG_URI);
    uriExamples.put("file://foo.txt", LONG_URI);
    uriExamples.put("file://foo/bar.txt", LONG_URI);
    uriExamples.put("http://www.google.com/foo", LONG_URI);
    
    uriExamples.put("\"hello my name is fred\"", LITERAL);
    uriExamples.put("\"hello my name is fred\"@en", LITERAL);
    uriExamples.put("\"hello my name is fred\"@en_gb", LITERAL);
    uriExamples.put("\"hello my name is fred\"^^xsd:integer", LITERAL);
    uriExamples.put("\"hello my name is fred\"^^http://www.w3.org/2001/XMLSchema#integer", LITERAL);
    
    for (String uri: uriExamples.keySet())
      assertEquals(uri, uriExamples.get(uri), ResourceType.establishType(uri));
  }
  
  @Test
  public void shortenUri()
  {
    String prefix1 = "http://cs.com/foo/";
    //String prefix2 = "http://cs.com/foo#";
    String longUri1 = "http://cs.com/foo/bar";
    String longUri2 = "http://cs.com/foo/bar/baz";
    
    Pattern p = Pattern.compile(String.format("^%s(%s)$", Pattern.quote(prefix1), Splink.URI_IDENTIFIER_RE));

    Matcher m1 = p.matcher(longUri1);
    m1.find();
    assertEquals(1, m1.groupCount());
    assertEquals(longUri1, "bar", m1.group(1));
    
    Matcher m2 = p.matcher(longUri2);
    assertFalse(m2.find());

    assertTrue(longUri1, p.matcher(longUri1).matches());
    assertFalse(longUri2, p.matcher(longUri2).matches());
  }
}
