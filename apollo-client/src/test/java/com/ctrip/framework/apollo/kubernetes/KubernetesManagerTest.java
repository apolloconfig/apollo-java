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
package com.ctrip.framework.apollo.kubernetes;

import com.ctrip.framework.apollo.build.MockInjector;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class KubernetesManagerTest {

    private CoreV1Api coreV1Api;
    private KubernetesManager kubernetesManager;

    @Before
    public void setUp() {
        coreV1Api = mock(CoreV1Api.class);
        kubernetesManager = new KubernetesManager(coreV1Api);

        MockInjector.setInstance(KubernetesManager.class, kubernetesManager);
        MockInjector.setInstance(CoreV1Api.class, coreV1Api);
    }

    /**
     * 测试 createConfigMap 成功创建配置
     */
    @Test
    public void testCreateConfigMapSuccess() throws Exception {
        // arrange
        String namespace = "default";
        String name = "testConfigMap";
        Map<String, String> data = new HashMap<>();
        data.put("key", "value");
        V1ConfigMap configMap = new V1ConfigMap()
                .metadata(new V1ObjectMeta().name(name).namespace(namespace))
                .data(data);

        when(coreV1Api.createNamespacedConfigMap(eq(namespace), eq(configMap), isNull(), isNull(), isNull(),isNull())).thenReturn(configMap);

        // act
        String result = kubernetesManager.createConfigMap(namespace, name, data);

        // assert
        verify(coreV1Api, times(1)).createNamespacedConfigMap(eq(namespace), any(V1ConfigMap.class), isNull(), isNull(), isNull(),isNull());
        assert name.equals(result);
    }

    /**
     * 测试 createConfigMap 传入 null 作为数据，正常执行
     */
    @Test
    public void testCreateConfigMapNullData() throws Exception {
        // arrange
        String namespace = "default";
        String name = "testConfigMap";
        Map<String, String> data = null;

        // act
        String result = kubernetesManager.createConfigMap(namespace, name, data);

        // assert
        verify(coreV1Api, times(1)).createNamespacedConfigMap(eq(namespace), any(V1ConfigMap.class), isNull(), isNull(), isNull(),isNull());
        assert name.equals(result);
    }

    /**
     * 测试getValueFromConfigMap方法，当ConfigMap存在且包含指定key时返回正确的value
     */
    @Test
    public void testGetValueFromConfigMapReturnsValue() throws Exception {
        // arrange
        String namespace = "default";
        String name = "testConfigMap";
        String key = "testKey";
        String expectedValue = "testValue";
        V1ConfigMap configMap = new V1ConfigMap();
        configMap.putDataItem(key, expectedValue);

        when(coreV1Api.readNamespacedConfigMap(name, namespace, null)).thenReturn(configMap);

        // act
        String actualValue = kubernetesManager.getValueFromConfigMap(namespace, name, key);

        // assert
        assertEquals(expectedValue, actualValue);
    }

    /**
     * 测试getValueFromConfigMap方法，当ConfigMap不存在指定key时返回null
     */
    @Test
    public void testGetValueFromConfigMapKeyNotFound() throws Exception {
        // arrange
        String namespace = "default";
        String name = "testConfigMap";
        String key = "nonExistingKey";
        V1ConfigMap configMap = new V1ConfigMap();
        when(coreV1Api.readNamespacedConfigMap(name, namespace, null)).thenReturn(configMap);

        // act
        String actualValue = kubernetesManager.getValueFromConfigMap(namespace, name, key);

        // assert
        assertNull(actualValue);
    }

    /**
     * 测试updateConfigMap成功的情况
     */
    @Test
    public void testUpdateConfigMapSuccess() throws Exception {
        // arrange
        String namespace = "default";
        String name = "testConfigMap";
        Map<String, String> data = new HashMap<>();
        data.put("key", "value");
        V1ConfigMap configMap = new V1ConfigMap();
        configMap.metadata(new V1ObjectMeta().name(name).namespace(namespace));
        configMap.data(data);

        when(coreV1Api.readNamespacedConfigMap(name, namespace, null)).thenReturn(configMap);
        when(coreV1Api.replaceNamespacedConfigMap(name, namespace, configMap, null, null, null, null)).thenReturn(configMap);

        // act
        Boolean success = kubernetesManager.updateConfigMap(namespace, name, data);

        // assert
        assertTrue(success);
    }

    /**
     * 测试ConfigMap存在时，checkConfigMapExist方法返回true
     */
    @Test
    public void testCheckConfigMapExistWhenConfigMapExists() throws Exception {
        // arrange
        String namespace = "default";
        String name = "testConfigMap";

        // 创建一个模拟的 V1ConfigMap 实例
        V1ConfigMap mockConfigMap = new V1ConfigMap();
        mockConfigMap.setMetadata(new V1ObjectMeta().name(name).namespace(namespace));
        doReturn(mockConfigMap).when(coreV1Api).readNamespacedConfigMap(name, namespace, null);

        // act
        boolean result = kubernetesManager.checkConfigMapExist(namespace, name);

        // assert
        assertEquals(true, result);
    }

    /**
     * 测试ConfigMap不存在的情况，返回false
     */
    @Test
    public void testCheckConfigMapExistWhenConfigMapDoesNotExist() throws Exception {
        // arrange
        String namespace = "default";
        String name = "testConfigMap";
        doThrow(new ApiException("ConfigMap not exist")).when(coreV1Api).readNamespacedConfigMap(name, namespace, null);

        // act
        boolean result = kubernetesManager.checkConfigMapExist(namespace, name);

        // assert
        assertFalse(result);
    }

    /**
     * 测试参数k8sNamespace和configMapName都为空时，checkConfigMapExist方法返回false
     */
    @Test
    public void testCheckConfigMapExistWithEmptyNamespaceAndName() {
        // arrange
        String namespace = "";
        String name = "";

        // act
        boolean result = kubernetesManager.checkConfigMapExist(namespace, name);

        // assert
        assertFalse(result);
    }

}
