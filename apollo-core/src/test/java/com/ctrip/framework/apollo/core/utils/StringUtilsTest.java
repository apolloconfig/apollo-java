/*
 * Copyright 2022 Apollo Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.ctrip.framework.apollo.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StringUtilsTest {

  @Test
  public void testEqualsIgnoreCase() {
    Assertions.assertFalse(StringUtils.equalsIgnoreCase(",", "foo"));
    Assertions.assertFalse(StringUtils.equalsIgnoreCase(null, "??"));
    Assertions.assertTrue(StringUtils.equalsIgnoreCase(null, null));
    Assertions.assertTrue(StringUtils.equalsIgnoreCase("foo", "Foo"));
  }

  @Test
  public void testEquals() {
    Assertions.assertFalse(StringUtils.equals(null, ""));
    Assertions.assertTrue(StringUtils.equals(null, null));
    Assertions.assertTrue(StringUtils.equals("3", "3"));
  }

  @Test
  public void testIsBlank() {
    Assertions.assertFalse(StringUtils.isBlank("'"));
    Assertions.assertTrue(StringUtils.isBlank(""));
    Assertions.assertTrue(StringUtils.isBlank(null));
  }

  @Test
  public void testIsContainEmpty() {
    Assertions.assertFalse(StringUtils.isContainEmpty(null));
    Assertions.assertFalse(StringUtils.isContainEmpty());
    Assertions.assertFalse(StringUtils.isContainEmpty("1"));
    Assertions.assertTrue(StringUtils.isContainEmpty(new String[] {null}));
  }

  @Test
  public void testIsEmpty() {
    Assertions.assertFalse(StringUtils.isEmpty("1"));
    Assertions.assertTrue(StringUtils.isEmpty(null));
    Assertions.assertTrue(StringUtils.isEmpty(""));
  }

  @Test
  public void testIsNumeric() {
    Assertions.assertFalse(StringUtils.isNumeric(null));
    Assertions.assertFalse(StringUtils.isNumeric("'"));
    Assertions.assertTrue(StringUtils.isNumeric("1"));
  }

  @Test
  public void testStartsWithIgnoreCase() {
    Assertions.assertFalse(StringUtils.startsWithIgnoreCase("A1B2C3", "BAZ"));
    Assertions.assertFalse(StringUtils.startsWithIgnoreCase(",", "BAZ"));
    Assertions.assertTrue(StringUtils.startsWithIgnoreCase("bar", "BAR"));
  }

  @Test
  public void testStartsWith() {
    Assertions.assertFalse(StringUtils.startsWith("1234", "1a 2b 3c"));
    Assertions.assertTrue(StringUtils.startsWith("1a 2b 3c", "1a 2b 3c"));
    Assertions.assertTrue(StringUtils.startsWith(null, null));
  }

  @Test
  public void testTrim() {
    assertEquals("1234", StringUtils.trim("1234"));
    Assertions.assertNull(StringUtils.trim(null));
  }

  @Test
  public void testTrimToEmpty() {
    assertEquals("1234", StringUtils.trimToEmpty("1234"));
    assertEquals("", StringUtils.trimToEmpty(null));
  }

  @Test
  public void trimToNull() {
    Assertions.assertNull(StringUtils.trimToNull(null));
    assertEquals("foo", StringUtils.trimToNull("foo"));
  }
}
