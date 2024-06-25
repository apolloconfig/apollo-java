package com.ctrip.framework.apollo.tracer.internals;

import com.ctrip.framework.apollo.metrics.Metrics;
import com.ctrip.framework.apollo.metrics.MetricsEvent;
import com.ctrip.framework.apollo.tracer.spi.MessageProducer;
import com.ctrip.framework.apollo.tracer.spi.Transaction;
import java.util.Objects;

public class MetricsMessageProducer implements MessageProducer {
    public static final String TRACER_ERROR = "Tracer.Error";
    public static final String TRACER_EVENT = "Tracer.Event";
    public static final String APOLLO_CONFIG_EXCEPTION ="ApolloConfigException";



    @Override
    public void logError(Throwable cause) {
        Metrics.push(MetricsEvent.builder().withName(cause.getMessage()).
            withTag(TRACER_ERROR).withData(cause).build());
    }

    @Override
    public void logError(String message, Throwable cause) {
        Metrics.push(MetricsEvent.builder().withName(cause.getMessage()).
            withTag(TRACER_ERROR).withData(cause).build());
    }

    @Override
    public void logEvent(String type, String name) {
        return;
    }

    @Override
    public void logEvent(String type, String name, String status,
        String nameValuePairs) {
        if(Objects.equals(type, APOLLO_CONFIG_EXCEPTION)) {
            String data = status + ":" + nameValuePairs;
            Metrics.push(MetricsEvent.builder().withName(name).withData(data)
                .withTag(TRACER_EVENT).build());
        }
    }

    @Override
    public Transaction newTransaction(String type, String name) {
        return null;
    }

}
