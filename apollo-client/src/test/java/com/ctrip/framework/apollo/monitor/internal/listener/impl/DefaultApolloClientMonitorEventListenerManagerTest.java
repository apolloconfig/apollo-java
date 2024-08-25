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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.ctrip.framework.apollo.monitor.internal.listener.ApolloClientMonitorEventListener;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefaultApolloClientMonitorEventListenerManagerTest {

    private DefaultApolloClientMonitorEventListenerManager manager;

    @Before
    public void setUp() {
        manager = new DefaultApolloClientMonitorEventListenerManager();
    }

    @Test
    public void testInitialCollectors() {
        List<ApolloClientMonitorEventListener> collectors = manager.getCollectors();
        assertNotNull(collectors);
        assertTrue(collectors.isEmpty()); // 初始状态应该为空列表
    }

    @Test
    public void testSetCollectors() {
        ApolloClientMonitorEventListener mockListener = mock(ApolloClientMonitorEventListener.class);
        List<ApolloClientMonitorEventListener> newCollectors = new ArrayList<>();
        newCollectors.add(mockListener);

        manager.setCollectors(newCollectors);
        List<ApolloClientMonitorEventListener> collectors = manager.getCollectors();

        assertNotNull(collectors);
        assertEquals(1, collectors.size());
        assertEquals(mockListener, collectors.get(0)); // 验证设置的监听器
    }

    @Test
    public void testSetEmptyCollectors() {
        manager.setCollectors(Collections.emptyList());
        List<ApolloClientMonitorEventListener> collectors = manager.getCollectors();

        assertNotNull(collectors);
        assertTrue(collectors.isEmpty()); // 设置为空列表后应该为空
    }
}
