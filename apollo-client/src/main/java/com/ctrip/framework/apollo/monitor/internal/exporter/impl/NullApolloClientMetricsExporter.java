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
package com.ctrip.framework.apollo.monitor.internal.exporter.impl;

import com.ctrip.framework.apollo.core.utils.DeferredLoggerFactory;
import com.ctrip.framework.apollo.monitor.internal.exporter.AbstractApolloClientMetricsExporter;
import com.ctrip.framework.apollo.monitor.internal.listener.ApolloClientMonitorEventListener;
import com.ctrip.framework.apollo.monitor.internal.exporter.ApolloClientMetricsExporter;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

/**
 * @author Rawven
 */
public class NullApolloClientMetricsExporter implements ApolloClientMetricsExporter {

  private static final Logger log = DeferredLoggerFactory.getLogger(
      NullApolloClientMetricsExporter.class);

  @Override
  public void init(List<ApolloClientMonitorEventListener> listeners, long collectPeriod) {
  }

  @Override
  public boolean isSupport(String form) {
    return false;
  }

  @Override
  public void registerOrUpdateCounterSample(String name, Map<String, String> tag,
      double incrValue) {

  }

  @Override
  public void registerOrUpdateGaugeSample(String name, Map<String, String> tag, double value) {

  }

  @Override
  public String response() {
    log.warn("No metrics exporter found, response empty string");
    return "";
  }
}
