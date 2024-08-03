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
package com.ctrip.framework.apollo.monitor.internal.exporter;

import com.ctrip.framework.apollo.core.spi.Ordered;
import com.ctrip.framework.apollo.monitor.internal.listener.ApolloClientMetricsEventListener;
import com.ctrip.framework.apollo.monitor.internal.model.CounterModel;
import com.ctrip.framework.apollo.monitor.internal.model.GaugeModel;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Rawven
 */
public interface ApolloClientMetricsExporter extends Ordered {

  /**
   * init method
   */
  void init(List<ApolloClientMetricsEventListener> collectors, long collectPeriod);

  /**
   * Used to access custom monitoring systems
   */
  boolean isSupport(String form);

  /**
   * register or update counter sample
   */
  void registerOrUpdateCounterSample(String name, Map<String, String> tag, double incrValue);


  /**
   * register or update gauge sample
   */
  void registerOrUpdateGaugeSample(String name, Map<String, String> tag, double value);

  /**
   * result of the collect metrics
   */
  String response();

  @Override
  default int getOrder() {
    return 0;
  }
}
