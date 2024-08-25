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
package com.ctrip.framework.apollo.monitor.internal.listener.impl;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import com.ctrip.framework.apollo.monitor.internal.listener.impl.DefaultApolloClientThreadPoolApi.ApolloThreadPoolInfo;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class DefaultApolloClientThreadPoolApiTest {

    private DefaultApolloClientThreadPoolApi threadPoolApi;
    private ThreadPoolExecutor remoteConfigExecutor;
    private ThreadPoolExecutor abstractConfigExecutor;
    private ThreadPoolExecutor abstractConfigFileExecutor;

    @Before
    public void setUp() {
        remoteConfigExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
        abstractConfigExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
        abstractConfigFileExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);

        threadPoolApi = new DefaultApolloClientThreadPoolApi(
                remoteConfigExecutor,
                abstractConfigExecutor,
                abstractConfigFileExecutor
        );
    }

    @SneakyThrows
    @Test
    public void testExportThreadPoolMetrics() {
        remoteConfigExecutor.execute(() -> {});
        remoteConfigExecutor.execute(() -> {});
        // 等待任务执行完成
        Thread.sleep(200); 
        threadPoolApi.export0();

        ApolloThreadPoolInfo info = threadPoolApi.getRemoteConfigRepositoryThreadPoolInfo();
        assertEquals(0, info.getQueueSize());
        assertEquals(2, info.getCompletedTaskCount());
        assertEquals(2, info.getPoolSize());
    }

    @Test
    public void testGetThreadPoolInfo() {
        assertNotNull(threadPoolApi.getThreadPoolInfo());
        assertEquals(3, threadPoolApi.getThreadPoolInfo().size());
    }

    @Test
    public void testMetricsSampleUpdated() {
        assertTrue(threadPoolApi.isMetricsSampleUpdated());
    }

    @Test
    public void testGetAbstractConfigThreadPoolInfo() {
        ApolloThreadPoolInfo info = threadPoolApi.getAbstractConfigThreadPoolInfo();
        assertNotNull(info);
    }

    @Test
    public void testGetAbstractConfigFileThreadPoolInfo() {
        ApolloThreadPoolInfo info = threadPoolApi.getAbstractConfigFileThreadPoolInfo();
        assertNotNull(info);
    }
}
