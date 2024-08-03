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
package com.ctrip.framework.apollo.monitor.internal.collector;

import com.ctrip.framework.apollo.monitor.internal.event.ApolloConfigMetricsEvent;
import com.ctrip.framework.apollo.monitor.internal.model.CounterModel;
import com.ctrip.framework.apollo.monitor.internal.model.GaugeModel;
import com.ctrip.framework.apollo.monitor.internal.model.SampleModel;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 抽象的 Metrics 收集器 用于收集和导出指标样本
 *
 * @author Rawven
 */
public abstract class AbstractMetricsCollector implements MetricsCollector {

  protected final Map<String, CounterModel> counterSamples = Maps.newHashMap();
  protected final Map<String, GaugeModel> gaugeSamples = Maps.newHashMap();
  private final AtomicBoolean isUpdated = new AtomicBoolean();
  private final String tag;

  public AbstractMetricsCollector(String tag) {
    this.tag = tag;
  }

  @Override
  public String name() {
    return tag;
  }

  @Override
  public boolean isSupport(ApolloConfigMetricsEvent event) {
    return tag.equals(event.getTag());
  }

  @Override
  public void collect(ApolloConfigMetricsEvent event) {
    collect0(event);
    isUpdated.set(true);
  }

  @Override
  public boolean isSamplesUpdated() {
    return isUpdated.getAndSet(false);
  }

  @Override
  public List<SampleModel> export() {
    export0();
    List<SampleModel> samples = new ArrayList<>(counterSamples.values());
    samples.addAll(gaugeSamples.values());
    return samples;
  }

  protected abstract void collect0(ApolloConfigMetricsEvent event);

  protected abstract void export0();
}
