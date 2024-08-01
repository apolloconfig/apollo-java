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

import com.ctrip.framework.apollo.monitor.internal.model.CounterModel;
import com.ctrip.framework.apollo.monitor.internal.model.GaugeModel;
import com.ctrip.framework.apollo.monitor.internal.model.MetricsEvent;
import com.ctrip.framework.apollo.monitor.internal.model.MetricsModel;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Arrays;
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
  private final List<String> tags;
  private final String name;

  public AbstractMetricsCollector(String name, String... tags) {
    this.name = name;
    this.tags = Arrays.asList(tags);
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public boolean isSupport(MetricsEvent event) {
    return tags.contains(event.getTag());
  }

  @Override
  public void collect(MetricsEvent event) {
    collect0(event);
    isUpdated.set(true);
  }

  @Override
  public boolean isSamplesUpdated() {
    return isUpdated.getAndSet(false);
  }

  @Override
  public List<MetricsModel> export() {
    export0();
    List<MetricsModel> samples = new ArrayList<>(counterSamples.values());
    samples.addAll(gaugeSamples.values());
    return samples;
  }

  protected abstract void collect0(MetricsEvent event);

  protected abstract void export0();
}
