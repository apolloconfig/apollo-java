///*
// * Copyright 2022 Apollo Authors
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// * http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// *
// */
//package com.ctrip.framework.apollo.internals;
//
//import com.ctrip.framework.apollo.Kubernetes.KubernetesManager;
//import com.ctrip.framework.apollo.build.MockInjector;
//import com.ctrip.framework.apollo.enums.ConfigSourceType;
//import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
//import com.ctrip.framework.apollo.util.ConfigUtil;
//import com.ctrip.framework.apollo.util.factory.PropertiesFactory;
//import io.kubernetes.client.openapi.ApiClient;
//import io.kubernetes.client.openapi.apis.CoreV1Api;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.mockito.invocation.InvocationOnMock;
//import org.mockito.stubbing.Answer;
//
//import java.util.Base64;
//import java.util.Properties;
//
//import static org.junit.Assert.assertEquals;
//import static org.mockito.Mockito.*;
//import static org.springframework.test.util.ReflectionTestUtils.setField;
//
//public class K8sConfigMapConfigRepositoryTest {
//    private String someNamespace;
//    private ConfigRepository upstreamRepo;
//    private Properties someProperties;
//    private static String someAppId = "someApp";
//    private static String someCluster = "someCluster";
//    private String defaultKey;
//    private String defaultValue;
//    private ConfigSourceType someSourceType;
//
//    @Mock
//    private CoreV1Api coreV1Api;
//    @Mock
//    private ApiClient client;
//
//    @InjectMocks
//    private KubernetesManager kubernetesManager;
//    @Mock
//    private ConfigUtil configUtil;
//
//    @Before
//    public void setUp() {
//        someNamespace = "someName";
//
//        // 初始化上游数据源
//        someProperties = new Properties();
//        defaultKey = "defaultKey";
//        defaultValue = "defaultValue";
//        someProperties.setProperty(defaultKey, defaultValue);
//        someSourceType = ConfigSourceType.LOCAL;
//        upstreamRepo = mock(ConfigRepository.class);
//        when(upstreamRepo.getConfig()).thenReturn(someProperties);
//        when(upstreamRepo.getSourceType()).thenReturn(someSourceType);
//
//        // mock configutil类
//        MockitoAnnotations.initMocks(this);
//        when(configUtil.getAppId()).thenReturn("testAppId");
//        when(configUtil.getCluster()).thenReturn("default");
//        when(configUtil.getConfigMapNamespace()).thenReturn("default");
//
//        MockInjector.setInstance(ConfigUtil.class, new MockConfigUtil());
//        PropertiesFactory propertiesFactory = mock(PropertiesFactory.class);
//        when(propertiesFactory.getPropertiesInstance()).thenAnswer(new Answer<Properties>() {
//            @Override
//            public Properties answer(InvocationOnMock invocation) {
//                return new Properties();
//            }
//        });
//        MockInjector.setInstance(PropertiesFactory.class, propertiesFactory);
//    }
//
//    @After
//    public void tearDown() throws Exception {
//        MockInjector.reset();
//    }
//
//    @Test(expected = ApolloConfigException.class)
//    public void testConstructorWithNullNamespace() {
//        new K8sConfigMapConfigRepository(null);
//    }
//
//    @Test
//    public void testSetConfigMapKey() {
//        K8sConfigMapConfigRepository repo = new K8sConfigMapConfigRepository(someNamespace);
//        repo.setConfigMapKey(someCluster, someNamespace);
//        assertEquals(someCluster + someNamespace, repo.getConfigMapKey());
//    }
//
//    @Test
//    public void testSetConfigMapName() {
//        K8sConfigMapConfigRepository repo = new K8sConfigMapConfigRepository(someNamespace);
//        repo.setConfigMapName(someAppId, false);
//        assertEquals(someAppId, repo.getConfigMapName());
//    }
//
//    @Test(expected = ApolloConfigException.class)
//    public void testSetConfigMapNameWithNullAppId() {
//        K8sConfigMapConfigRepository repo = new K8sConfigMapConfigRepository(someNamespace);
//        repo.setConfigMapName(null, false);
//    }
//
//
//    @Test
//    public void testOnRepositoryChange() throws Exception {
//        RepositoryChangeListener someListener = mock(RepositoryChangeListener.class);
//
//        // 创建一个 LocalFileConfigRepository 实例，作为上游仓库
//        LocalFileConfigRepository upstreamRepo = mock(LocalFileConfigRepository.class);
//        when(upstreamRepo.getSourceType()).thenReturn(ConfigSourceType.LOCAL);
//
//        // 创建一个模拟的 KubernetesManager
//        KubernetesManager mockKubernetesManager = mock(KubernetesManager.class);
//        when(mockKubernetesManager.checkConfigMapExist(anyString(), anyString())).thenReturn(true);
//        doNothing().when(mockKubernetesManager).createConfigMap(anyString(), anyString(), any());
//
//        K8sConfigMapConfigRepository k8sConfigMapConfigRepository = new K8sConfigMapConfigRepository("someNamespace", upstreamRepo);
//        k8sConfigMapConfigRepository.initialize();
//
//        // 设置本地缓存目录并添加监听器
//        k8sConfigMapConfigRepository.addChangeListener(someListener);
//        k8sConfigMapConfigRepository.getConfig();
//
//        Properties anotherProperties = new Properties();
//        anotherProperties.put("anotherKey", "anotherValue");
//
//        ConfigSourceType anotherSourceType = ConfigSourceType.LOCAL;
//        when(upstreamRepo.getSourceType()).thenReturn(anotherSourceType);
//
//        // 调用 onRepositoryChange 方法，模拟仓库配置发生变化
//        k8sConfigMapConfigRepository.onRepositoryChange("someNamespace", anotherProperties);
//
//        // 使用 ArgumentCaptor 捕获监听器的调用参数
//        final ArgumentCaptor<Properties> captor = ArgumentCaptor.forClass(Properties.class);
//        verify(someListener, times(1)).onRepositoryChange(eq("someNamespace"), captor.capture());
//
//        // 断言捕获的配置与 anotherProperties 相同
//        assertEquals(anotherProperties, captor.getValue());
//        // 断言 K8sConfigMapConfigRepository 的源类型更新为 anotherSourceType
//        assertEquals(anotherSourceType, k8sConfigMapConfigRepository.getSourceType());
//    }
//
//    /**
//     * 测试persistConfigMap方法成功持久化配置信息
//     */
//    @Test
//    public void testPersistConfigMap() {
//        K8sConfigMapConfigRepository repo = new K8sConfigMapConfigRepository(someNamespace);
//        doNothing().when(kubernetesManager).updateConfigMap(anyString(), anyString(), anyMap());
//        repo.persistConfigMap(someProperties);
//        verify(kubernetesManager, times(1)).updateConfigMap(anyString(), anyString(), anyMap());
//    }
//
//    /**
//     * 测试sync方法成功从上游数据源同步
//     */
//    @Test
//    public void testSyncSuccessFromUpstream() throws Throwable {
//        K8sConfigMapConfigRepository repo = new K8sConfigMapConfigRepository(someNamespace);
//
//        // arrange
//        ConfigRepository upstream = mock(ConfigRepository.class);
//        Properties upstreamProperties = new Properties();
//        upstreamProperties.setProperty("key", "value");
//        when(upstream.getConfig()).thenReturn(upstreamProperties);
//        when(upstream.getSourceType()).thenReturn(ConfigSourceType.REMOTE);
//        repo.setUpstreamRepository(upstream);
//
////        // mock KubernetesManager
////        when(kubernetesManager.createConfigMap(anyString(), anyString(), anyMap()))
////                .thenReturn(true);
////        setField(repo, "kubernetesManager", kubernetesManager);
//
//        // act
//        repo.sync();
//
//        // assert
//        verify(upstream, times(1)).getConfig();
//    }
//
//    @Test
//    public void testSyncFromUpstreamWithFileStorage() throws Exception {
//        K8sConfigMapConfigRepository repo = new K8sConfigMapConfigRepository(someNamespace);
//
//
//        Properties upstreamProperties = new Properties();
//        upstreamProperties.setProperty("key1", "value1");
//
//        when(upstreamRepo.getConfig()).thenReturn(upstreamProperties);
//        when(upstreamRepo.getSourceType()).thenReturn(ConfigSourceType.LOCAL);
//
//        repo.sync();
//
//        Properties config = repo.getConfig();
//        assertEquals("value1", config.getProperty("key1"));
//        assertEquals(ConfigSourceType.LOCAL, repo.getSourceType());
//    }
//
//    @Test
//    public void testSyncFromUpstreamWithRemote() throws Exception {
//        K8sConfigMapConfigRepository repo = new K8sConfigMapConfigRepository(someNamespace);
//
//        Properties upstreamProperties = new Properties();
//        upstreamProperties.setProperty("key2", "value2");
//
//        when(upstreamRepo.getConfig()).thenReturn(upstreamProperties);
//        when(upstreamRepo.getSourceType()).thenReturn(ConfigSourceType.REMOTE);
//
//        repo.sync();
//
//        Properties config = repo.getConfig();
//        assertEquals("value2", config.getProperty("key2"));
//        assertEquals(ConfigSourceType.REMOTE, repo.getSourceType());
//    }
//
//    @Test
//    public void testSyncFromK8s() throws Exception {
//        K8sConfigMapConfigRepository repo = new K8sConfigMapConfigRepository(someNamespace);
//
//        Properties k8sProperties = new Properties();
//        k8sProperties.setProperty("key3", "value3");
//
//        when(kubernetesManager.getValueFromConfigMap(anyString(), anyString(), anyString()))
//                .thenReturn(Base64.getEncoder().encodeToString("{\"key3\":\"value3\"}".getBytes()));
//
//        repo.sync();
//
//        Properties config = repo.getConfig();
//        assertEquals("value3", config.getProperty("key3"));
//        assertEquals(ConfigSourceType.CONFIGMAP, repo.getSourceType());
//    }
//
//
//    /**
//     * 测试sync方法从上游数据源同步失败，成功从Kubernetes的ConfigMap中加载
//     */
//    @Test
//    public void testSyncFailFromUpstreamSuccessFromConfigMap() throws Throwable {
//        K8sConfigMapConfigRepository repo = new K8sConfigMapConfigRepository(someNamespace);
//        // arrange
//        ConfigRepository upstream = mock(ConfigRepository.class);
//        when(upstream.getConfig()).thenThrow(new RuntimeException("Upstream sync failed"));
//        repo.setUpstreamRepository(upstream);
//        when(kubernetesManager.getValueFromConfigMap(anyString(), anyString(), anyString())).thenReturn("encodedConfig");
//
//        // act
//        repo.sync();
//
//        // assert
//        verify(kubernetesManager, times(1)).getValueFromConfigMap(anyString(), anyString(), anyString());
//    }
//
//
//    public static class MockConfigUtil extends ConfigUtil {
//        @Override
//        public String getAppId() {
//            return someAppId;
//        }
//
//        @Override
//        public String getCluster() {
//            return someCluster;
//        }
//    }
//
//}
