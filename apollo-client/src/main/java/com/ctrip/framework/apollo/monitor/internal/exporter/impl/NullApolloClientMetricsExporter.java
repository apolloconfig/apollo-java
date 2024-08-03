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

import com.ctrip.framework.apollo.monitor.internal.listener.ApolloClientMetricsEventListener;
import com.ctrip.framework.apollo.monitor.internal.exporter.ApolloClientMetricsExporter;
import com.ctrip.framework.apollo.monitor.internal.model.CounterModel;
import com.ctrip.framework.apollo.monitor.internal.model.GaugeModel;
import java.util.List;
import java.util.Map;

/**
 * @author Rawven
 */
public class NullApolloClientMetricsExporter implements ApolloClientMetricsExporter {

  @Override
  public void init(List<ApolloClientMetricsEventListener> collectors, long collectPeriod) {
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
    return "No Reporter Use";
  }
}
