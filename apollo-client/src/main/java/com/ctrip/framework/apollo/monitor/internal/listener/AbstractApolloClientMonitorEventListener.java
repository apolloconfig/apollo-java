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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
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

  @Override
  public String getName() {
    return tag;
  }

  @Override
  public boolean isSupported(ApolloClientMonitorEvent event) {
    return tag.equals(event.getTag());
  }

  @Override
  public void collect(ApolloClientMonitorEvent event) {
    collect0(event);
    isUpdated.set(true);
  }

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
   * Specific collection logic
   */
  protected void collect0(ApolloClientMonitorEvent event) {
  }

  /**
   * Convenient for indicators that can only be obtained from the status object
   */
  protected void export0() {
  }


  /**
   * tool method for updating indicator model
   */
  public void createOrUpdateGaugeSample(String metricsName, String[] tagKeys, String[] tagValues,
      double value) {
    createOrUpdateSample(metricsName, tagKeys, tagValues, value, false);
  }

  public void createOrUpdateGaugeSample(String metricsName, double value) {
    createOrUpdateSample(metricsName, null, null, value, false);
  }

  public void createOrUpdateCounterSample(String metricsName, String[] tagKeys, String[] tagValues,
      double increaseValue) {
    createOrUpdateSample(metricsName, tagKeys, tagValues, increaseValue, true);
  }

  public void createOrUpdateCounterSample(String metricsName, double increaseValue) {
    createOrUpdateSample(metricsName, null, null, increaseValue, true);
  }

  private void createOrUpdateSample(String metricsName, String[] tagKeys, String[] tagValues,
      double value, boolean isCounter) {
    String mapKey = metricsName + (tagValues != null ? Arrays.toString(tagValues) : "");

    if (isCounter) {
      CounterModel counter = counterSamples.computeIfAbsent(mapKey,
          key -> (CounterModel) CounterModel.create(metricsName, 0)
              .putTags(getTags(tagKeys, tagValues)));
      counter.increase(value);
    } else {
      GaugeModel gauge = gaugeSamples.computeIfAbsent(mapKey,
          key -> (GaugeModel) GaugeModel.create(metricsName, 0)
              .putTags(getTags(tagKeys, tagValues)));
      gauge.setValue(value);
    }
  }

  private Map<String, String> getTags(String[] tagKeys, String[] tagValues) {
    if (tagKeys != null && tagValues != null && tagKeys.length == tagValues.length) {
      Map<String, String> tags = Maps.newHashMap();
      for (int i = 0; i < tagKeys.length; i++) {
        tags.put(tagKeys[i], tagValues[i]);
      }
      return tags;
    }
    return Collections.emptyMap();
  }

}
