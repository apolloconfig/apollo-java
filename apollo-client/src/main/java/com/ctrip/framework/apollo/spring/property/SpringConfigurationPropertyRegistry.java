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
package com.ctrip.framework.apollo.spring.property;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import org.springframework.beans.factory.BeanFactory;

/**
 * @author licheng
 */
public class SpringConfigurationPropertyRegistry {

  private final Map<BeanFactory, Multimap<String, String>> registry = Maps.newConcurrentMap();

  public void register(BeanFactory beanFactory, String prefix,
      String beanName) {
    registry.computeIfAbsent(beanFactory,
        k -> Multimaps.synchronizedSetMultimap(HashMultimap.create())).put(prefix, beanName);
  }

  public Collection<String> get(BeanFactory beanFactory, String key) {
    Multimap<String, String> beanNameMap = registry.get(beanFactory);
    if (beanNameMap == null) {
      return null;
    }
    // get all prefix matches beans
    Collection<String> targetCollection = new HashSet<>();
    for (String prefix : beanNameMap.keySet()) {
      if (key.startsWith(prefix)) {
        targetCollection.addAll(beanNameMap.get(prefix));
      }
    }
    return targetCollection;
  }
}
