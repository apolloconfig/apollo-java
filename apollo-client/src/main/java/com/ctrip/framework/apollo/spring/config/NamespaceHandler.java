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

import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.core.Ordered;
import org.springframework.util.SystemPropertyUtils;
import org.w3c.dom.Element;

import com.ctrip.framework.apollo.core.ConfigConsts;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class NamespaceHandler extends NamespaceHandlerSupport {


  public static final String NAMESPACE_DELIMITER = ",";
  private static final Splitter NAMESPACE_SPLITTER = Splitter.on(NAMESPACE_DELIMITER).omitEmptyStrings()
      .trimResults();

  @Override
  public void init() {
    registerBeanDefinitionParser("config", new BeanParser());
  }

  static class BeanParser extends AbstractSingleBeanDefinitionParser {

    @Override
    protected Class<?> getBeanClass(Element element) {
      return ConfigPropertySourcesProcessor.class;
    }

    @Override
    protected boolean shouldGenerateId() {
      return true;
    }

    private String resolveNamespaces(Element element) {
      String namespaces = element.getAttribute("namespaces");
      if (Strings.isNullOrEmpty(namespaces)) {
        //default to application
        return ConfigConsts.NAMESPACE_APPLICATION;
      }
      return SystemPropertyUtils.resolvePlaceholders(namespaces);
    }

    @Override
    protected void doParse(Element element, BeanDefinitionBuilder builder) {
      String namespaces = this.resolveNamespaces(element);

      int order = Ordered.LOWEST_PRECEDENCE;
      String orderAttribute = element.getAttribute("order");

      if (!Strings.isNullOrEmpty(orderAttribute)) {
        try {
          order = Integer.parseInt(orderAttribute);
        } catch (Throwable ex) {
          throw new IllegalArgumentException(
              String.format("Invalid order: %s for namespaces: %s", orderAttribute, namespaces));
        }
      }
      PropertySourcesProcessor.addNamespaces(NAMESPACE_SPLITTER.splitToList(namespaces), order);
    }
  }

  /**
   * @param namespaces - comma separated string of namespaces
   * @return List of namespaces
   */
  public static List<String> parseCommaSeparatedNamespaces(String namespaces) {

    if (namespaces == null) {
      return Collections.emptyList();
    }

    return NAMESPACE_SPLITTER.splitToList(namespaces);
  }
}
