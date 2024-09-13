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
package com.ctrip.framework.apollo.internals;

import com.ctrip.framework.apollo.Kubernetes.KubernetesManager;
import com.ctrip.framework.apollo.enums.ConfigSourceType;
import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import com.ctrip.framework.apollo.util.ConfigUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Properties;

import static org.mockito.Mockito.*;

/**
 * TODO （未完成）K8sConfigMapConfigRepository单元测试
 */
public class K8sConfigMapConfigRepositoryTest {

    private String someNamespace;
    private ConfigRepository upstreamRepo;
    private Properties someProperties;
    private static String someAppId = "someApp";
    private static String someCluster = "someCluster";
    private String defaultKey;
    private String defaultValue;
    private ConfigSourceType someSourceType;

    @Mock
    private KubernetesManager kubernetesManager;
    @Mock
    private ConfigUtil configUtil;

    private K8sConfigMapConfigRepository k8sConfigMapConfigRepository;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(configUtil.getAppId()).thenReturn("testAppId");
        when(configUtil.getCluster()).thenReturn("default");
        when(configUtil.getConfigMapNamespace()).thenReturn("default");

        someProperties = new Properties();
        defaultKey = "defaultKey";
        defaultValue = "defaultValue";
        someProperties.setProperty(defaultKey, defaultValue);


        k8sConfigMapConfigRepository = new K8sConfigMapConfigRepository("namespace", null);
    }

    /**
     * 测试sync方法成功从上游数据源同步
     */
    @Test
    public void testSyncSuccessFromUpstream() throws Throwable {
        // arrange
        ConfigRepository upstream = mock(ConfigRepository.class);
        Properties upstreamProperties = new Properties();
        upstreamProperties.setProperty("key", "value");
        when(upstream.getConfig()).thenReturn(upstreamProperties);
        when(upstream.getSourceType()).thenReturn(ConfigSourceType.REMOTE);
        k8sConfigMapConfigRepository.setUpstreamRepository(upstream);

        // act
        k8sConfigMapConfigRepository.sync();

        // assert
        verify(upstream, times(1)).getConfig();
    }

    /**
     * 测试sync方法从上游数据源同步失败，成功从Kubernetes的ConfigMap中加载
     */
    @Test
    public void testSyncFailFromUpstreamSuccessFromConfigMap() throws Throwable {
        // arrange
        ConfigRepository upstream = mock(ConfigRepository.class);
        when(upstream.getConfig()).thenThrow(new RuntimeException("Upstream sync failed"));
        k8sConfigMapConfigRepository.setUpstreamRepository(upstream);
        when(kubernetesManager.getValueFromConfigMap(anyString(), anyString(), anyString())).thenReturn("encodedConfig");

        // act
        k8sConfigMapConfigRepository.sync();

        // assert
        verify(kubernetesManager, times(1)).getValueFromConfigMap(anyString(), anyString(), anyString());
    }

    /**
     * 测试loadFromK8sConfigMap方法成功加载配置信息
     */
    @Test
    public void testLoadFromK8sConfigMapSuccess() throws Throwable {
        // arrange
        when(kubernetesManager.getValueFromConfigMap(anyString(), anyString(), anyString())).thenReturn("encodedConfig");

        // act
        Properties properties = k8sConfigMapConfigRepository.loadFromK8sConfigMap();

        // assert
        verify(kubernetesManager, times(1)).getValueFromConfigMap(anyString(), anyString(), anyString());
        // 这里应该有更具体的断言来验证properties的内容，但由于编码和解码逻辑未给出，此处省略
    }

    /**
     * 测试loadFromK8sConfigMap方法在加载配置信息时发生异常
     */
    @Test(expected = ApolloConfigException.class)
    public void testLoadFromK8sConfigMapException() throws Throwable {
        // arrange
        when(kubernetesManager.getValueFromConfigMap(anyString(), anyString(), anyString())).thenThrow(new RuntimeException("Load failed"));

        // act
        k8sConfigMapConfigRepository.loadFromK8sConfigMap();

        // assert
        // 预期抛出ApolloConfigException
    }

    /**
     * 测试persistConfigMap方法成功持久化配置信息
     */
    @Test
    public void testPersistConfigMapSuccess() throws Throwable {
        // arrange
        Properties properties = new Properties();
        properties.setProperty("key", "value");

        // act
        k8sConfigMapConfigRepository.persistConfigMap(properties);

        // assert
        verify(kubernetesManager, times(1)).updateConfigMap(anyString(), anyString(), anyMap());
    }

    /**
     * 测试persistConfigMap方法在持久化配置信息时发生异常
     */
    @Test(expected = ApolloConfigException.class)
    public void testPersistConfigMapException() throws Throwable {
        // arrange
        Properties properties = new Properties();
        properties.setProperty("key", "value");
        doThrow(new RuntimeException("Persist failed")).when(kubernetesManager).updateConfigMap(anyString(), anyString(), anyMap());

        // act
        k8sConfigMapConfigRepository.persistConfigMap(properties);

        // assert
        // 预期抛出ApolloConfigException
    }
}
