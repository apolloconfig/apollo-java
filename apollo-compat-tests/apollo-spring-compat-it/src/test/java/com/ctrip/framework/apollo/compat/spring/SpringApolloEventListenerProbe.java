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
package com.ctrip.framework.apollo.compat.spring;

import com.ctrip.framework.apollo.spring.events.ApolloConfigChangeEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

public class SpringApolloEventListenerProbe implements ApplicationListener {

  private final Set<String> namespaces = Collections.synchronizedSet(new HashSet<String>());

  @Override
  public void onApplicationEvent(ApplicationEvent event) {
    if (event instanceof ApolloConfigChangeEvent) {
      namespaces.add(((ApolloConfigChangeEvent) event).getConfigChangeEvent().getNamespace());
    }
  }

  public boolean hasNamespace(String namespace) {
    return namespaces.contains(namespace);
  }
}
