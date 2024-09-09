package com.ctrip.framework.apollo.internals;

import com.ctrip.framework.apollo.util.ConfigUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import com.ctrip.framework.apollo.Kubernetes.KubernetesManager;
import com.ctrip.framework.apollo.exceptions.ApolloConfigException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


/**
 * author: dyx1234
 */

@RunWith(MockitoJUnitRunner.class)
public class K8sConfigMapConfigRepositoryTest {

    @Mock
    private KubernetesManager kubernetesManager;

    @Mock
    private ConfigUtil configUtil;

    @InjectMocks
    private K8sConfigMapConfigRepository repository;

    @Before
    public void setUp() {
        Mockito.when(configUtil.getAppId()).thenReturn("TestApp");
        Mockito.when(configUtil.getConfigMapNamespace()).thenReturn("TestNamespace");
    }

    @Test
    public void testLoadFromK8sConfigMapSuccess() throws IOException {
        // 安排
        Map<String, String> expectedData = new HashMap<>();
        expectedData.put("key1", "c2FtcGxlIHN0cmluZw=="); // base64 编码后的 "sample string"
        Mockito.when(kubernetesManager.getFromConfigMap("TestNamespace", "TestApp"))
                .thenReturn(expectedData);

        // 执行
        Properties properties = repository.loadFromK8sConfigMap();

        // 断言
        assertEquals(1, properties.size());
        assertEquals("sample string", properties.getProperty("key1"));
    }

    @Test(expected = ApolloConfigException.class)
    public void testLoadFromK8sConfigMapFailure() throws IOException {
        // 安排
        Mockito.doThrow(new RuntimeException("Kubernetes Manager Exception"))
                .when(kubernetesManager).getFromConfigMap("TestNamespace", "TestApp");

        // 执行 & 断言 - 会抛出异常
        repository.loadFromK8sConfigMap();
    }

    @Test
    public void testOnRepositoryChange() throws IOException {
        // 安排
        Properties newProperties = new Properties();
        newProperties.setProperty("key1", "value1");

        // 执行
        repository.onRepositoryChange("TestNamespace", newProperties);

        // 断言
        assertEquals(1, repository.getConfig().size());
        assertEquals("value1", repository.getConfig().getProperty("key1"));
    }

    @Test
    public void testSyncSuccess() {
        Mockito.when(kubernetesManager.getFromConfigMap("TestNamespace", "TestApp"))
                .thenReturn(java.util.Collections.singletonMap("key", "value"));

        Properties properties = repository.getConfig();

        assertEquals(1, properties.size());
        assertEquals("value", properties.getProperty("key"));
    }

    @Test(expected = ApolloConfigException.class)
    public void testSyncFailure() {
        Mockito.when(kubernetesManager.getFromConfigMap("TestNamespace", "TestApp"))
                .thenThrow(new RuntimeException("Kubernetes Manager Exception"));

        repository.getConfig();
    }

    @Test
    public void testUpstreamRepositoryChange() {
        Properties upstreamProperties = new Properties();
        upstreamProperties.setProperty("upstreamKey", "upstreamValue");

        ConfigRepository mockUpstream = mock(ConfigRepository.class);
        when(mockUpstream.getConfig()).thenReturn(upstreamProperties);

        repository.setUpstreamRepository(mockUpstream);

        // Assuming onRepositoryChange is called upon change
        repository.onRepositoryChange("upstream", upstreamProperties);

        Properties result = repository.getConfig();

        assertEquals("upstreamValue", result.getProperty("upstreamKey"));
    }

    @Test
    public void testPersistLocalCacheFileSuccess() {
        Properties properties = new Properties();
        properties.setProperty("persistKey", "persistValue");

        repository.persistLocalCacheFile(properties);

        verify(kubernetesManager, times(1)).updateConfigMap(anyString(), anyString(), anyMap());
    }

    @Test(expected = ApolloConfigException.class)
    public void testPersistLocalCacheFileFailure() {
        Properties properties = new Properties();
        properties.setProperty("persistKey", "persistValue");

        doThrow(new RuntimeException("Kubernetes Manager Exception")).when(kubernetesManager).updateConfigMap(anyString(), anyString(), anyMap());

        repository.persistLocalCacheFile(properties);
    }
}
