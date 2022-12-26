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
package com.ctrip.framework.apollo.spring.config;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class NameSpaceHandlerTest {

  @Test
  public void testParseCommaSeparatedNamespacesForValidCase() {
    final String NAMESPACES = "app1,app2,app3";
    List<String> namespaces = NamespaceHandler.parseCommaSeparatedNamespaces(NAMESPACES);
    assertEquals(namespaces.size(), 3);
    assertEquals(namespaces, Arrays.asList(NAMESPACES.split(",")));
  }

  @Test
  public void testParseCommaSeparatedNamespacesForInValidCase() {
    final String NAMESPACES = "";
    List<String> namespaces = NamespaceHandler.parseCommaSeparatedNamespaces(NAMESPACES);
    assertEquals(namespaces.size(), 0);

    namespaces = NamespaceHandler.parseCommaSeparatedNamespaces(null);
    assertEquals(namespaces.size(), 0);
  }

}
