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
package com.ctrip.framework.apollo.monitor.internal.collector.internal;

import static com.ctrip.framework.apollo.monitor.internal.tracer.MessageProducerComposite.ERROR_METRICS;
import static com.ctrip.framework.apollo.monitor.internal.tracer.MessageProducerComposite.THROWABLE;

import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import com.ctrip.framework.apollo.monitor.api.ApolloExceptionMonitorApi;
import com.ctrip.framework.apollo.monitor.internal.collector.AbstractMetricsCollector;
import com.ctrip.framework.apollo.monitor.internal.model.CounterModel;
import com.ctrip.framework.apollo.monitor.internal.model.MetricsEvent;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Rawven
 */
public class DefaultApolloExceptionCollector extends AbstractMetricsCollector implements
    ApolloExceptionMonitorApi {

  public static final String EXCEPTION_NUM = "exception_num";

  private static final int MAX_EXCEPTIONS_SIZE = 25;
  private final BlockingQueue<ApolloConfigException> exceptions = new ArrayBlockingQueue<>(
      MAX_EXCEPTIONS_SIZE);

  private final AtomicInteger exceptionNum = new AtomicInteger(0);

  public DefaultApolloExceptionCollector() {
    super(ERROR_METRICS, ERROR_METRICS);
  }

  @Override
  public Integer getExceptionNum() {
    return exceptionNum.get();
  }

  @Override
  public List<String> getExceptionDetails() {
    return exceptions.stream().map(ApolloConfigException::getMessage)
        .collect(Collectors.toList());
  }

  @Override
  public void collect0(MetricsEvent event) {
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
    if (!counterSamples.containsKey(EXCEPTION_NUM)) {
      counterSamples.put(EXCEPTION_NUM, CounterModel.builder().name(EXCEPTION_NUM).value(0)
          .build());
    }
    counterSamples.get(EXCEPTION_NUM).updateValue(exceptionNum.get());
  }

}
