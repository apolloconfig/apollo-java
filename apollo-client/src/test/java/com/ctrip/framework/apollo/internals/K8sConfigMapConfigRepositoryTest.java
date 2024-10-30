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

import com.ctrip.framework.apollo.build.MockInjector;
import com.ctrip.framework.apollo.enums.ConfigSourceType;
import com.ctrip.framework.apollo.kubernetes.KubernetesManager;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.ctrip.framework.apollo.util.escape.EscapeUtil;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class K8sConfigMapConfigRepositoryTest {
    private static String someAppId = "someApp";
    private static String someCluster = "someCluster";
    private String someNamespace = "default";
    private static final String someConfigmapName = "apollo-configcache-someApp";

    private static final String defaultKey = "defaultKey";
    private static final String defaultValue = "defaultValue";
    private static final String defaultJsonValue = "{\"id\":123,\"name\":\"John Doe\",\"email\":\"john.doe@example.com\"}";

    private ConfigRepository upstreamRepo;
    private Properties someProperties;
    private ConfigSourceType someSourceType = ConfigSourceType.LOCAL;
    private V1ConfigMap configMap;
    private Map<String, String> data;
    private KubernetesManager kubernetesManager;
    private K8sConfigMapConfigRepository k8sConfigMapConfigRepository;


    @Before
    public void setUp() {
        // mock configUtil
        MockInjector.setInstance(ConfigUtil.class, new MockConfigUtil());
        // mock kubernetesManager
        kubernetesManager = mock(KubernetesManager.class);
        MockInjector.setInstance(KubernetesManager.class, kubernetesManager);

        // mock upstream
        someProperties = new Properties();
        someProperties.setProperty(defaultKey, defaultValue);
        upstreamRepo = mock(ConfigRepository.class);
        when(upstreamRepo.getConfig()).thenReturn(someProperties);
        when(upstreamRepo.getSourceType()).thenReturn(someSourceType);

        // make configmap
        data = new HashMap<>();
        data.put(defaultKey, defaultJsonValue);
        configMap = new V1ConfigMap()
                .metadata(new V1ObjectMeta().name(someAppId).namespace(someNamespace))
                .data(data);

        k8sConfigMapConfigRepository = new K8sConfigMapConfigRepository(someNamespace, upstreamRepo);
    }

    // TODO 直接mock manager中的参数

    /**
     * 测试setConfigMapKey方法，当cluster和namespace都为正常值时
     */
    @Test
    public void testSetConfigMapKeyUnderNormalConditions() throws Throwable {
        // arrange
        String cluster = "testCluster";
        String namespace = "test_Namespace_1";
        String escapedKey = "testCluster___test__Namespace__1";

        // act
        ReflectionTestUtils.invokeMethod(k8sConfigMapConfigRepository, "setConfigMapKey", cluster, namespace);

        // assert
        String expectedConfigMapKey = EscapeUtil.createConfigMapKey(cluster, namespace);
        assertEquals(escapedKey, ReflectionTestUtils.getField(k8sConfigMapConfigRepository, "configMapKey"));
        assertEquals(expectedConfigMapKey, ReflectionTestUtils.getField(k8sConfigMapConfigRepository, "configMapKey"));
    }

//    @Test
//    public void testSetConfigMapKey() {
//        when(kubernetesManager.createConfigMap(anyString(), anyString(), any())).thenReturn("someAppId");
//        k8sConfigMapConfigRepository.setConfigMapKey(someCluster, someNamespace);
//        assertEquals(someCluster +"-"+ someNamespace, k8sConfigMapConfigRepository.getConfigMapKey());
//    }
//
//    @Test
//    public void testSetConfigMapName() {
//        k8sConfigMapConfigRepository.setConfigMapName(someAppId, false);
//        assertEquals(someConfigmapName, k8sConfigMapConfigRepository.getConfigMapName());
//    }

    /**
     * 测试sync方法成功从上游数据源同步
     */
    @Test
    public void testSyncSuccessFromUpstream() throws Throwable {
        // arrange
        k8sConfigMapConfigRepository.setUpstreamRepository(upstreamRepo);

        // act
        k8sConfigMapConfigRepository.sync();

        // assert
        verify(upstreamRepo, times(1)).getConfig();
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
        when(kubernetesManager.getValueFromConfigMap(anyString(), anyString(), anyString())).thenReturn(data.get(defaultKey));

        // act
        k8sConfigMapConfigRepository.sync();

        // assert
        verify(kubernetesManager, times(1)).getValueFromConfigMap(anyString(), anyString(), anyString());
    }

    @Test
    public void testGetConfig() {
        // Arrange
        Properties expectedProperties = new Properties();
        expectedProperties.setProperty(defaultKey, defaultValue);
        when(upstreamRepo.getConfig()).thenReturn(expectedProperties);
        // Act
        Properties actualProperties = k8sConfigMapConfigRepository.getConfig();
        // Assert
        assertNotNull(actualProperties);
        assertEquals(defaultValue, actualProperties.getProperty(defaultKey));
    }

    @Test
    public void testPersistConfigMap() throws ApiException {
        // Arrange
        Properties properties = new Properties();
        properties.setProperty(defaultKey, defaultValue);
        // Act
        k8sConfigMapConfigRepository.persistConfigMap(properties);
        // Assert
        verify(kubernetesManager, times(1)).updateConfigMap(anyString(), anyString(), anyMap());
    }

    @Test
    public void testOnRepositoryChange() throws ApiException {
        // Arrange
        Properties newProperties = new Properties();
        newProperties.setProperty(defaultKey, defaultValue);
        // Act
        k8sConfigMapConfigRepository.onRepositoryChange(someNamespace, newProperties);
        // Assert
        verify(kubernetesManager, times(1)).updateConfigMap(anyString(), anyString(), anyMap());
    }

    @Test
    public void testLoadFromK8sConfigMapSuccess() {
        when(kubernetesManager.getValueFromConfigMap(anyString(), anyString(), anyString())).thenReturn(defaultJsonValue);

        Properties properties = k8sConfigMapConfigRepository.loadFromK8sConfigMap();

        assertNotNull(properties);
    }

    public static class MockConfigUtil extends ConfigUtil {
        @Override
        public String getAppId() {
            return someAppId;
        }

        @Override
        public String getCluster() {
            return someCluster;
        }
    }

}
