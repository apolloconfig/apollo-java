package com.ctrip.framework.apollo.metrics.collector;

import com.ctrip.framework.apollo.metrics.MetricsEvent;
import com.ctrip.framework.apollo.metrics.model.MetricsSample;
import java.util.List;

/**
 * @author Rawven
 */
public interface MetricsCollector {

  /**
   * 是否支持该指标
   */
  boolean isSupport(String tag);

  /**
   * 收集指标
   */
  void collect(MetricsEvent event);

  /**
   * 是否更新了指标样本
   */
  boolean isSamplesUpdated();

  /**
   * 导出指标
   */
  List<MetricsSample> export();

}
