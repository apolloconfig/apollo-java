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
package com.ctrip.framework.apollo.tracer.internals;

import com.ctrip.framework.apollo.core.utils.ClassLoaderUtil;
import com.ctrip.framework.apollo.metrics.Metrics;
import com.ctrip.framework.apollo.tracer.internals.cat.CatMessageProducer;
import com.ctrip.framework.apollo.tracer.internals.cat.CatNames;
import com.ctrip.framework.apollo.tracer.spi.MessageProducer;
import com.ctrip.framework.apollo.tracer.spi.MessageProducerManager;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class DefaultMessageProducerManager implements MessageProducerManager {
    private static MessageProducer producer = new MessageProducerComposite(new ArrayList<>());

    public DefaultMessageProducerManager() {
        List<MessageProducer> producers = new ArrayList<>();
        if(Metrics.isMetricsEnabled()){
            producers.add(new MetricsMessageProducer());
        }
        if (ClassLoaderUtil.isClassPresent(CatNames.CAT_CLASS)) {
            producers.add(new CatMessageProducer());
        }
        if (producers.isEmpty()) {
            producer = new NullMessageProducer();
        }else {
            producer = new MessageProducerComposite(producers);
        }
    }
    @Override
    public MessageProducer getProducer() {
        return producer;
    }
}
