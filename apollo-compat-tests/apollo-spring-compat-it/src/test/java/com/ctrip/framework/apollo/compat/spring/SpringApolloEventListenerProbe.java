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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

public class SpringApolloEventListenerProbe implements ApplicationListener {

  private final BlockingQueue<String> namespaces = new LinkedBlockingQueue<String>();

  @Override
  public void onApplicationEvent(ApplicationEvent event) {
    if (event instanceof ApolloConfigChangeEvent) {
      namespaces.offer(((ApolloConfigChangeEvent) event).getConfigChangeEvent().getNamespace());
    }
  }

  public String pollNamespace(long timeout, TimeUnit unit) throws InterruptedException {
    return namespaces.poll(timeout, unit);
  }
}
