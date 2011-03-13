package org.trebor.splink;

import static java.lang.System.out;
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
  @SuppressWarnings("serial")
  public static final Map<String, ResourceType> RESOURCE_EXAMPLES =
    new HashMap<String, ResourceType>()
    {
      {
        put("foo:bar#baz", null);
        put("foo:bar/baz", null);

        put("foo:bar", SHORT_URI);
        put("foo:bar.baz", SHORT_URI);
        put("foo_bar:baz_qux", SHORT_URI);

        put("_:node15pf8hastx4", BLANK_NODE);
        put("_:x", BLANK_NODE);

        put("http://www.google.com/foo#bar", LONG_URI);
        put("file://foo.txt", LONG_URI);
        put("file://foo/bar.txt", LONG_URI);
        put("http://www.google.com/foo", LONG_URI);

        put("\"hello my name is fred\"", LITERAL);
        put("\"hello my name is fred\"@en", LITERAL);
        put("\"hello my name is fred\"@en_gb", LITERAL);
        put("\"hello my name is fred\"^^xsd:integer", LITERAL);
        put("\"hello my name is fred\"^^http://www.w3.org/2001/XMLSchema#integer", LITERAL);
        put("\"hello my name is \"fred\"\"^^http://www.w3.org/2001/XMLSchema#integer", LITERAL);
        put("\"foo\nbar\"", LITERAL);
      }
    };
  
  @Test
  public void testUri()
  {
    for (String uri: RESOURCE_EXAMPLES.keySet())
      assertEquals(uri, RESOURCE_EXAMPLES.get(uri), ResourceType.establishType(uri));
  }

  @Test
  public void testParseLiteral()
  {
    out.format("start\n");
    
    for (String uri: RESOURCE_EXAMPLES.keySet())
    {
      if (RESOURCE_EXAMPLES.get(uri) == LITERAL)
      {
        System.out.println(String.format(uri));
        Matcher m = LITERAL.parse(uri);
        assertTrue(m.matches());
        for (int i = 0; i <  m.groupCount(); ++i)
          out.format("  %d: %s\n", i, m.group(i + 1));
        
        out.format("\"\"\"%s\"\"\"%s\n", m.group(1).replaceAll("\"", "\\\\\""), m.group(2) == null ? "" : m.group(2));
      }
    }
    
    out.format("end\n");
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
