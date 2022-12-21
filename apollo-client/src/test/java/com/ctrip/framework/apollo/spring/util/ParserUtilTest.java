package com.ctrip.framework.apollo.spring.util;

import static org.junit.Assert.assertEquals;

import org.springframework.expression.spel.SpelEvaluationException;

import static org.junit.Assert.assertTrue;

import com.ctrip.framework.apollo.util.parser.Parsers;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


@RunWith(JUnit4.class)
public class ParserUtilTest {

  @Test
  public void parseNameSpacesSpELValidExpressionTest() {
    String expression = "'foo,bar,test,app'.split(',')";
    List<String> expected = Arrays.asList("foo,bar,test,app".split(","));
    List<String> result = Parsers.parseNameSpacesSpEL(expression);
    assertTrue(result.size() == 4);
    assertEquals(result, expected);
  }

  @Test
  public void parseNameSpacesSpELBlankNullExpressionTest() {
    List<String> result = Parsers.parseNameSpacesSpEL("");
    assertTrue(result.size() == 0);

    result = Parsers.parseNameSpacesSpEL("");
    assertTrue(result.size() == 0);
  }

  @Test(expected = SpelEvaluationException.class)
  public void parseNameSpacesSpELInvalidExpressionTest() {
    List<String> result = Parsers.parseNameSpacesSpEL("test-wrong-exp");
    assertTrue(result.size() == 0);
  }

}
