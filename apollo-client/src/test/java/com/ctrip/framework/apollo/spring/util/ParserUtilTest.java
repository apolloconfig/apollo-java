package com.ctrip.framework.apollo.spring.util;


import static org.junit.Assert.assertTrue;

import com.ctrip.framework.apollo.util.parser.Parsers;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


@RunWith(JUnit4.class)
public class ParserUtilTest {

  @Test
  public void parseNameSpacesSpELValidExpressionForArrayTest() {
    final String expression = "#{'foo,bar,test,app'.split(',')}";
    Set<String> result = Parsers.parseNameSpacesSpEL(expression);
    assertTrue(result.size() == 4);
    assertTrue(result.contains("foo"));
    assertTrue(result.contains("bar"));
    assertTrue(result.contains("test"));
    assertTrue(result.contains("app"));
  }

  @Test
  public void parseNameSpacesSpELBlankNullExpressionTest() {
    Set<String> result = Parsers.parseNameSpacesSpEL("");
    assertTrue(result.size() == 0);

    result = Parsers.parseNameSpacesSpEL("");
    assertTrue(result.size() == 0);
  }

  @Test
  public void parseNameSpacesSpELInvalidExpressionTest() {
    Set<String> result = Parsers.parseNameSpacesSpEL("#{foo");
    assertTrue(result.size() == 1);
    assertTrue(result.contains("#{foo"));
  }

}