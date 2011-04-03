package org.trebor.splink;

import static java.lang.System.out;
import static org.junit.Assert.*;
import static org.openrdf.query.QueryLanguage.SPARQL;
import static org.trebor.splink.Splink.ResourceType.*;

import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.UnsupportedQueryLanguageException;
import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.query.parser.QueryParserUtil;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;
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
        put("\"hello my name is fred\"^^<http://www.w3.org/2001/XMLSchema#integer>", LITERAL);
        put("\"hello my name is \"fred\"\"^^<http://www.w3.org/2001/XMLSchema#integer>", LITERAL);
        
        put("\"1986-02-11T00:00:00-04:00\"^^<http://www.w3.org/2001/XMLSchema#dateTime>", LITERAL);
        put("\"1996-05-14T13:33:12-04:00\"^^<http://www.w3.org/2001/XMLSchema#dateTime>", LITERAL);
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
        Matcher m = LITERAL.parse(uri);
        String base = m.group(1);
        String type = m.group(2);
        if (null == type)
          type = "";
        base = base.replaceAll("\"", "\\\\\"");
        String quote = (base.contains("\n") || base.contains("\r")) ? "\"\"\"" : "\"";
        out.format("%s%s%s%s\n", quote, base, quote, type);
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
  
  @Test
  public void parseSparqlTest() throws MalformedQueryException, UnsupportedQueryLanguageException
  {
    String query = "SELECT * WHERE {?s ?p ?o}";
    ParsedQuery parsedQuery = QueryParserUtil.parseQuery(SPARQL, query, null);
    out.format("parsed query: %s\n", parsedQuery);
  }
  
  @Test
  public void writerTest()
  {
    StringWriter writer = new StringWriter();
    RDFWriter rdfWriter = null;
    out.format("formats: %d\n", RDFFormat.values().size());

    for (RDFFormat f: RDFFormat.values())
    {
      rdfWriter = Rio.createWriter(f, writer);     
      out.format("%s writer: %s\n", f, rdfWriter);
    }
  }
}
