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
package com.ctrip.framework.apollo.monitor.internal.collector.impl;

import static com.ctrip.framework.apollo.monitor.internal.MonitorConstant.METRICS_EXCEPTION_NUM;
import static com.ctrip.framework.apollo.monitor.internal.MonitorConstant.TAG_ERROR;
import static com.ctrip.framework.apollo.monitor.internal.MonitorConstant.THROWABLE;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import com.ctrip.framework.apollo.monitor.api.ApolloClientExceptionMonitorApi;
import com.ctrip.framework.apollo.monitor.jmx.mbean.ApolloClientJmxExceptionMBean;
import com.ctrip.framework.apollo.monitor.internal.collector.AbstractMetricsCollector;
import com.ctrip.framework.apollo.monitor.internal.event.ApolloConfigMetricsEvent;
import com.ctrip.framework.apollo.monitor.internal.model.CounterModel;
import com.ctrip.framework.apollo.util.ConfigUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Rawven
 */
public class DefaultApolloClientExceptionApi extends AbstractMetricsCollector implements
    ApolloClientExceptionMonitorApi, ApolloClientJmxExceptionMBean {


  private static final int MAX_EXCEPTIONS_SIZE = ApolloInjector.getInstance(ConfigUtil.class)
      .getMonitorExceptionSaveSize();
  private final BlockingQueue<ApolloConfigException> exceptions = new ArrayBlockingQueue<>(
      MAX_EXCEPTIONS_SIZE);

  private final AtomicInteger exceptionNum = new AtomicInteger(0);

  public DefaultApolloClientExceptionApi() {
    super(TAG_ERROR);
  }

  @Override
  public List<Exception> getApolloConfigExceptionList() {
    return new ArrayList<>(exceptions);
  }

  @Override
  public void collect0(ApolloConfigMetricsEvent event) {
    ApolloConfigException exception = event.getAttachmentValue(THROWABLE);
    if (exception != null) {
      if (exceptions.size() >= MAX_EXCEPTIONS_SIZE) {
        exceptions.poll();
      }
      exceptions.add(exception);
      exceptionNum.incrementAndGet();
    }

  }

  @Override
  public void export0() {
    if (!counterSamples.containsKey(METRICS_EXCEPTION_NUM)) {
      counterSamples.put(METRICS_EXCEPTION_NUM, CounterModel.create(METRICS_EXCEPTION_NUM, 0));
    }
    counterSamples.get(METRICS_EXCEPTION_NUM).updateValue(exceptionNum.get());
  }

  @Override
  public List<String> getApolloConfigExceptionDetails() {
    List<String> exceptionDetails = new ArrayList<>();
    for (ApolloConfigException exception : new ArrayList<>(exceptions)) {
      exceptionDetails.add(exception.getMessage());
    }
    return exceptionDetails;
  }
}
