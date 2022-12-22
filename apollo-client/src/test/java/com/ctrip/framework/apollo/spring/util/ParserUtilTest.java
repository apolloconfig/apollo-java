/*
 * Copyright 2022 Apollo Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package com.ctrip.framework.apollo.spring.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
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
  public void parseNameSpacesSpELValidListExpressionTest() {
    String expression = "'foo,bar,test,app'.split(',')";
    List<String> expected = Arrays.asList("foo,bar,test,app".split(","));
    List<String> result = Parsers.parseNameSpacesSpEL(expression);
    assertTrue(result.size() == 4);
    assertEquals(result, expected);
  }

  @Test
  public void parseNameSpacesSpELValidStringExpressionTest() {
    String expression = "'application'";
    List<String> expected = new ArrayList<>();
    expected.add("application");
    List<String> result = Parsers.parseNameSpacesSpEL(expression);
    assertTrue(result.size() == 1);
    assertEquals(result, expected);
  }

  @Test
  public void parseNameSpacesSpELBlankNullExpressionTest() {
    List<String> result = Parsers.parseNameSpacesSpEL("");
    assertTrue(result.size() == 0);

    result = Parsers.parseNameSpacesSpEL(null);
    assertTrue(result.size() == 0);
  }


  @Test(expected = SpelEvaluationException.class)
  public void parseNameSpacesSpELInvalidExpressionTest() {
    List<String> result = Parsers.parseNameSpacesSpEL("test-wrong-exp");
    assertTrue(result.size() == 0);
  }

}
