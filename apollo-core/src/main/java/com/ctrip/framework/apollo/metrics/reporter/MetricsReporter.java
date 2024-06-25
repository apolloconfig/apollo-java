package com.ctrip.framework.apollo.metrics.reporter;

import com.ctrip.framework.apollo.core.spi.Ordered;
import com.ctrip.framework.apollo.metrics.collector.MetricsCollector;
import com.ctrip.framework.apollo.metrics.model.CounterMetricsSample;
import com.ctrip.framework.apollo.metrics.model.GaugeMetricsSample;
import java.util.List;

public interface MetricsReporter extends Ordered {

    void init(List<MetricsCollector> collectors);

    /**
     * 用于注册Counter类型的指标
     */
    void registerCounterSample(CounterMetricsSample sample);

    /**
     * 用于注册Gauge类型的指标
     */
    void registerGaugeSample(GaugeMetricsSample<?> sample);

    /**
     * 收集的指标结果
     *
     * @return {@link String }
     */
    String response();

    @Override
    default int getOrder() {
        return 0;
    }
}
