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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Rawven
 */
public class DefaultApolloClientExceptionApi extends
    AbstractApolloClientMonitorEventListener implements
    ApolloClientExceptionMonitorApi, ApolloClientJmxExceptionMBean {

  private final AtomicInteger exceptionNum = new AtomicInteger(0);
  private int monitorExceptionQueueSize;
  private BlockingQueue<ApolloConfigException> exceptions;

  public DefaultApolloClientExceptionApi() {
    super(TAG_ERROR);
    monitorExceptionQueueSize = ApolloInjector.getInstance(ConfigUtil.class)
        .getMonitorExceptionQueueSize();
    if(monitorExceptionQueueSize <= 0){
      monitorExceptionQueueSize = 25;
    }
    exceptions = new ArrayBlockingQueue<>(
        monitorExceptionQueueSize);
  }

  @Override
  public List<Exception> getApolloConfigExceptionList() {
    return new ArrayList<>(exceptions);
  }

  @Override
  public void collect0(ApolloClientMonitorEvent event) {
    ApolloConfigException exception = event.getAttachmentValue(THROWABLE);
    if (exception != null) {
      addExceptionToQueue(exception);
      exceptionNum.incrementAndGet();
      createOrUpdateCounterSample(METRICS_EXCEPTION_NUM, METRICS_EXCEPTION_NUM,
          Collections.emptyMap(),
          1);
    }
  }

  private void addExceptionToQueue(ApolloConfigException exception) {
    if (exceptions.size() >= monitorExceptionQueueSize) {
      exceptions.poll();
    }
    exceptions.add(exception);
  }

  @Override
  public void export0() {
  }

  @Override
  public List<String> getApolloConfigExceptionDetails() {
    return exceptions.stream()
        .map(ApolloConfigException::getMessage)
        .collect(Collectors.toList());
  }
}
