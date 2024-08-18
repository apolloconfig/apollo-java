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
package com.ctrip.framework.apollo.monitor.internal.listener;

import com.ctrip.framework.apollo.monitor.internal.event.ApolloClientMonitorEvent;
import com.ctrip.framework.apollo.monitor.internal.model.CounterModel;
import com.ctrip.framework.apollo.monitor.internal.model.GaugeModel;
import com.ctrip.framework.apollo.monitor.internal.model.SampleModel;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 抽象的 Metrics 收集器 用于收集数据和导出指标样本
 *
 * @author Rawven
 */
public abstract class AbstractApolloClientMonitorEventListener implements
    ApolloClientMonitorEventListener {

  private final Map<String, CounterModel> counterSamples = Maps.newConcurrentMap();
  private final Map<String, GaugeModel> gaugeSamples = Maps.newConcurrentMap();
  private final AtomicBoolean isUpdated = new AtomicBoolean();
  private final String tag;

  public AbstractApolloClientMonitorEventListener(String tag) {
    this.tag = tag;
  }

  /**
   * Specific collection logic
   */
  protected abstract void collect0(ApolloClientMonitorEvent event);

  /**
   * Convenient for indicators that can only be obtained from the status object
   */
  protected abstract void export0();

  @Override
  public String mBeanName() {
    return tag;
  }

  @Override
  public boolean isSupport(ApolloClientMonitorEvent event) {
    return tag.equals(event.getTag());
  }

  @Override
  public void collect(ApolloClientMonitorEvent event) {
    collect0(event);
    isUpdated.set(true);
  }

  /**
   * Whether the sample data has been updated
   */
  @Override
  public boolean isMetricsSampleUpdated() {
    return isUpdated.getAndSet(false);
  }

  @Override
  public List<SampleModel> export() {
    export0();
    List<SampleModel> samples = new ArrayList<>(counterSamples.values());
    samples.addAll(gaugeSamples.values());
    return samples;
  }


  /**
   * tool method for updating indicator model
   */
  public void createOrUpdateGaugeSample(String mapKey, String metricsName, Map<String, String> tags,
      double value) {
    if (!gaugeSamples.containsKey(mapKey)) {
      GaugeModel builder = (GaugeModel) GaugeModel.create(metricsName, 0).putTags(tags);
      gaugeSamples.put(mapKey, builder);
    }
    gaugeSamples.get(mapKey).setValue(value);
  }

  /**
   * tool method for updating indicator model
   */
  public void createOrUpdateCounterSample(String mapKey, String metricsName,
      Map<String, String> tags,
      double increaseValue) {
    if (!counterSamples.containsKey(mapKey)) {
      CounterModel builder = (CounterModel) CounterModel.create(metricsName, 0).putTags(tags);
      counterSamples.put(mapKey, builder);
    }
    counterSamples.get(mapKey).increase(increaseValue);
  }

}
