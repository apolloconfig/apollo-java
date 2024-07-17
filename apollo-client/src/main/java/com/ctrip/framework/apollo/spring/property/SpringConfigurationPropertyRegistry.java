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
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

/**
 * @author licheng
 */
public class SpringConfigurationPropertyRegistry {

  private final Map<AutowireCapableBeanFactory, Multimap<String, String>> registry = Maps.newConcurrentMap();
  private final Object LOCK = new Object();

  public void register(AutowireCapableBeanFactory beanFactory, String prefix,
      String beanName) {
    if (!registry.containsKey(beanFactory)) {
      synchronized (LOCK) {
        if (!registry.containsKey(beanFactory)) {
          // ensure no repeat bean names in same prefix
          registry.put(beanFactory, Multimaps.synchronizedSetMultimap(HashMultimap.create()));
        }
      }
    }
    registry.get(beanFactory).put(prefix, beanName);
  }

  public Collection<String> get(AutowireCapableBeanFactory beanFactory, String key) {
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

  public void refresh(AutowireCapableBeanFactory beanFactory, String beanName) {
    Object bean = beanFactory.getBean(beanName);
    beanFactory.destroyBean(bean);
    beanFactory.initializeBean(bean, beanName);
  }

}
