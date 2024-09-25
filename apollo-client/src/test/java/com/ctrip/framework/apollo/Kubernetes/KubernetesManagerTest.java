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
package com.ctrip.framework.apollo.Kubernetes;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesManagerTest {

    @Mock
    private CoreV1Api coreV1Api;
    @Mock
    private ApiClient client;

    @InjectMocks
    private KubernetesManager kubernetesManager;

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
     * 测试 createConfigMap 传入空的命名空间，抛出异常
     */
    @Test
    public void testCreateConfigMapEmptyNamespace() throws Exception {
        // arrange
        String namespace = "";
        String name = "testConfigMap";
        Map<String, String> data = new HashMap<>();
        data.put("key", "value");

        // act
        assertThrows("create config map failed due to null parameter", IllegalArgumentException.class, () -> {
            kubernetesManager.createConfigMap(namespace, name, data);
        });

        // assert
        verify(coreV1Api, never()).createNamespacedConfigMap(anyString(), any(V1ConfigMap.class), isNull(), isNull(), isNull(),isNull());
    }

    /**
     * 测试 createConfigMap 传入空的配置名，抛出异常
     */
    @Test
    public void testCreateConfigMapEmptyName() throws Exception {
        // arrange
        String namespace = "default";
        String name = "";
        Map<String, String> data = new HashMap<>();
        data.put("key", "value");

        // act
        assertThrows("create config map failed due to null parameter", IllegalArgumentException.class, () -> {
            kubernetesManager.createConfigMap(namespace, name, data);
        });

        // assert
        verify(coreV1Api, never()).createNamespacedConfigMap(anyString(), any(V1ConfigMap.class), isNull(), isNull(), isNull(),isNull());
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
     * 测试loadFromConfigMap方法在正常情况下的行为
     */
    @Test
    public void testLoadFromConfigMapSuccess() throws Exception {
        // arrange
        String namespace = "TestNamespace";
        String name = "TestName";
        V1ConfigMap configMap = new V1ConfigMap();
        configMap.putDataItem(name, "TestValue");
        when(coreV1Api.readNamespacedConfigMap(name, namespace, null)).thenReturn(configMap);

        // act
        String result = kubernetesManager.loadFromConfigMap(namespace, name);

        // assert
        assertEquals("TestValue", result);
    }

    /**
     * 测试loadFromConfigMap方法在抛出异常时的行为
     */
    @Test
    public void testLoadFromConfigMapFailure() throws Exception {
        // arrange
        String namespace = "TestNamespace";
        String name = "TestName";
        when(coreV1Api.readNamespacedConfigMap(name, namespace, null)).thenThrow(new ApiException("Kubernetes Manager Exception"));

        assertThrows(String.format("get config map failed, configMapNamespace: %s, name: %s", namespace, name), RuntimeException.class, () -> {
            kubernetesManager.loadFromConfigMap(namespace, name);
        });

    }

    /**
     * 测试loadFromConfigMap方法在ConfigMap不存在时的行为
     */
    @Test
    public void testLoadFromConfigMapConfigMapNotFound() throws Exception {
        // arrange
        String namespace = "TestNamespace";
        String name = "TestName";
        when(coreV1Api.readNamespacedConfigMap(name, namespace, null)).thenReturn(null);

        // act
        assertThrows(String.format("get config map failed, configMapNamespace: %s, name: %s", namespace, name), RuntimeException.class, () -> {
            kubernetesManager.loadFromConfigMap(namespace, name);
        });

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
        String configMapNamespace = "default";
        String name = "testConfigMap";
        Map<String, String> data = new HashMap<>();
        data.put("key", "value");
        V1ConfigMap configMap = new V1ConfigMap();
        configMap.metadata(new V1ObjectMeta().name(name).namespace(configMapNamespace));
        configMap.data(data);

        when(coreV1Api.replaceNamespacedConfigMap(name, configMapNamespace, configMap, null, null, null, "fieldManagerValue")).thenReturn(configMap);

        // act
        String result = kubernetesManager.updateConfigMap(configMapNamespace, name, data);

        // assert
        assert result.equals(name);
    }

    /**
     * 测试ConfigMap存在的情况
     */
    @Test
    public void testCheckConfigMapExistWhenConfigMapExists() throws Exception {
        // arrange
        String namespace = "default";
        String name = "testConfigMap";
        when(coreV1Api.readNamespacedConfigMap(name, namespace, null)).thenReturn(new V1ConfigMap());

        // act
        boolean result = kubernetesManager.checkConfigMapExist(namespace, name);

        // assert
        assertTrue(result);
    }

    /**
     * 测试ConfigMap不存在的情况
     */
    @Test
    public void testCheckConfigMapExistWhenConfigMapDoesNotExist() throws Exception {
        // arrange
        String namespace = "default";
        String name = "testConfigMap";
        doThrow(new RuntimeException("ConfigMap not found")).when(coreV1Api).readNamespacedConfigMap(name, namespace, null);

        // act
        boolean result = kubernetesManager.checkConfigMapExist(namespace, name);

        // assert
        assertFalse(result);
    }

}
