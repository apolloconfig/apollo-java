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
import com.ctrip.framework.apollo.monitor.internal.collector.MetricsCollector;
import com.ctrip.framework.apollo.monitor.internal.model.CounterModel;
import com.ctrip.framework.apollo.monitor.internal.model.GaugeModel;
import java.util.List;

/**
 * @author Rawven
 */
public interface MetricsExporter extends Ordered {

  /**
   * init method
   */
  void init(List<MetricsCollector> collectors, long collectPeriod);

  /**
   * is support
   *
   * @param form form
   */
  boolean isSupport(String form);

  /**
   * used to register Counter type metrics
   */
  void registerCounterSample(CounterModel sample);

  /**
   * used to register Gauge type metrics
   */
  void registerGaugeSample(GaugeModel<?> sample);

  /**
   * result of the collect metrics
   */
  String response();

  @Override
  default int getOrder() {
    return 0;
  }
}
