package com.ctrip.framework.apollo.tracer.internals;

import com.ctrip.framework.apollo.tracer.spi.MessageProducer;
import com.ctrip.framework.apollo.tracer.spi.Transaction;
import java.util.List;

public class MessageProducerComposite implements MessageProducer {
    private List<MessageProducer> producers;

    public MessageProducerComposite(List<MessageProducer> list) {
        this.producers = list;
    }
    @Override
    public void logError(Throwable cause) {
         producers.forEach(producer -> producer.logError(cause));
    }

    @Override
    public void logError(String message, Throwable cause) {
        producers.forEach(producer -> producer.logError(message, cause));
    }

    @Override
    public void logEvent(String type, String name) {
        producers.forEach(producer -> producer.logEvent(type, name));
    }

    @Override
    public void logEvent(String type, String name, String status,
        String nameValuePairs) {
        producers.forEach(producer -> producer.logEvent(type, name, status, nameValuePairs));
    }

    @Override
    public Transaction newTransaction(String type, String name) {
        for (MessageProducer producer : producers) {
            Transaction transaction = producer.newTransaction(type, name);
            if (transaction != null) {
                return transaction;
            }
        }
        return NullMessageProducer.NULL_TRANSACTION;
    }
    public List<MessageProducer> getProducers(){
        return producers;
    }
}
