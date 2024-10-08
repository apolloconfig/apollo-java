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
package com.ctrip.framework.apollo.monitor.internal.listener.impl;

import static com.ctrip.framework.apollo.monitor.internal.ApolloClientMonitorConstant.METRICS_EXCEPTION_NUM;
import static com.ctrip.framework.apollo.monitor.internal.ApolloClientMonitorConstant.TAG_ERROR;
import static com.ctrip.framework.apollo.monitor.internal.ApolloClientMonitorConstant.THROWABLE;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import com.ctrip.framework.apollo.monitor.api.ApolloClientExceptionMonitorApi;
import com.ctrip.framework.apollo.monitor.internal.jmx.mbean.ApolloClientJmxExceptionMBean;
import com.ctrip.framework.apollo.monitor.internal.listener.AbstractApolloClientMonitorEventListener;
import com.ctrip.framework.apollo.monitor.internal.event.ApolloClientMonitorEvent;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Queues;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Rawven
 */
public class DefaultApolloClientExceptionApi extends
    AbstractApolloClientMonitorEventListener implements
    ApolloClientExceptionMonitorApi, ApolloClientJmxExceptionMBean {

  private final AtomicInteger exceptionCountFromStartup = new AtomicInteger(0);
  private final Queue<ApolloConfigException> exceptionsQueue;

  public DefaultApolloClientExceptionApi(ConfigUtil configUtil) {
    super(TAG_ERROR);
    int monitorExceptionQueueSize = configUtil.getMonitorExceptionQueueSize();
    EvictingQueue<ApolloConfigException> evictingQueue = EvictingQueue.create(
        monitorExceptionQueueSize);
    exceptionsQueue = Queues.synchronizedQueue(evictingQueue);
  }

  @Override
  public List<Exception> getApolloConfigExceptionList() {
    return new ArrayList<>(exceptionsQueue);
  }

  @Override
  public Integer getExceptionCountFromStartup() {
    return exceptionCountFromStartup.get();
  }

  @Override
  public void collect0(ApolloClientMonitorEvent event) {
    ApolloConfigException exception = event.getAttachmentValue(THROWABLE);
    if (exception != null) {
      exceptionsQueue.add(exception);
      exceptionCountFromStartup.incrementAndGet();
      createOrUpdateCounterSample(METRICS_EXCEPTION_NUM, 1);
    }
  }

  @Override
  public List<String> getApolloConfigExceptionDetails() {
    return exceptionsQueue.stream()
        .map(ApolloConfigException::getMessage)
        .collect(Collectors.toList());
  }
}
