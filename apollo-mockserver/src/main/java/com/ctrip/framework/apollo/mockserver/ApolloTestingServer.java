/*
 * Copyright 2023 Apollo Authors
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
package com.ctrip.framework.apollo.mockserver;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.core.ApolloClientSystemConsts;
import com.ctrip.framework.apollo.core.dto.ApolloConfig;
import com.ctrip.framework.apollo.core.dto.ApolloConfigNotification;
import com.ctrip.framework.apollo.core.utils.ResourceUtils;
import com.ctrip.framework.apollo.internals.ConfigServiceLocator;
import com.ctrip.framework.apollo.internals.LocalFileConfigRepository;
import com.ctrip.framework.apollo.internals.RemoteConfigLongPollService;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class ApolloTestingServer implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(ApolloTestingServer.class);
    private static final Type notificationType = new TypeToken<List<ApolloConfigNotification>>() {
    }.getType();

    private static final String DEFAULT_APP_ID = "someAppId";
    private static Method CONFIG_SERVICE_LOCATOR_CLEAR;
    private static Method CONFIG_SERVICE_RESET;
    private static Method REMOTE_CONFIG_LONG_POLL_STOP;
    private static ConfigServiceLocator CONFIG_SERVICE_LOCATOR;

    private static ConfigUtil CONFIG_UTIL;

    private static Method RESOURCES_UTILS_CLEAR;
    private static ResourceUtils RESOURCES_UTILS;

    private static final Gson GSON = new Gson();
    private final Map<String, Map<String, Map<String, String>>> addedOrModifiedPropertiesOfAppAndNamespace =
        Maps.newConcurrentMap();
    private final Map<String, Map<String, Set<String>>> deletedKeysOfAppAndNamespace =
        Maps.newConcurrentMap();

    private MockWebServer server;

    private boolean started;

    private boolean closed;

    static {
        try {
            System.setProperty("apollo.longPollingInitialDelayInMills", "0");
            CONFIG_SERVICE_LOCATOR = ApolloInjector.getInstance(ConfigServiceLocator.class);
            CONFIG_SERVICE_LOCATOR_CLEAR = ConfigServiceLocator.class.getDeclaredMethod("initConfigServices");
            CONFIG_SERVICE_LOCATOR_CLEAR.setAccessible(true);
            CONFIG_SERVICE_RESET = ConfigService.class.getDeclaredMethod("reset");
            CONFIG_SERVICE_RESET.setAccessible(true);
            REMOTE_CONFIG_LONG_POLL_STOP =
                RemoteConfigLongPollService.class.getDeclaredMethod("stopLongPollingRefresh");
            REMOTE_CONFIG_LONG_POLL_STOP.setAccessible(true);

            CONFIG_UTIL = ApolloInjector.getInstance(ConfigUtil.class);

            RESOURCES_UTILS = ApolloInjector.getInstance(ResourceUtils.class);
            RESOURCES_UTILS_CLEAR = ResourceUtils.class.getDeclaredMethod("loadConfigFileFromDefaultSearchLocations",
                    new Class[] {String.class});
            RESOURCES_UTILS_CLEAR.setAccessible(true);
        } catch (NoSuchMethodException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void start() throws IOException {
        clearForStart();
        server = new MockWebServer();
        final Dispatcher dispatcher = new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (request.getPath().startsWith("/notifications/v2")) {
                    String notifications = request.getRequestUrl().queryParameter("notifications");
                    return new MockResponse().setResponseCode(200).setBody(mockLongPollBody(notifications));
                }
                if (request.getPath().startsWith("/configs")) {
                    List<String> pathSegments = request.getRequestUrl().pathSegments();
                    // appId and cluster might be used in the future
                    String appId = pathSegments.get(1);
                    String cluster = pathSegments.get(2);
                    String namespace = pathSegments.get(3);
                    return new MockResponse().setResponseCode(200).setBody(loadConfigFor(appId, namespace));
                }
                return new MockResponse().setResponseCode(404);
            }
        };

        server.setDispatcher(dispatcher);
        server.start();

        mockConfigServiceUrl("http://localhost:" + server.getPort());
        started = true;
    }

    public void close() {
        try {
            clearForClose();
            server.close();
        } catch (Exception e) {
            logger.error("stop apollo server error", e);
        } finally {
            closed = true;
        }
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean isStarted() {
        return started;
    }

    private void clearForStart() {
        resetApolloClientState(false);
        resetOverriddenProperties();
    }

    private void clearForClose() {
        resetApolloClientState(true);
        resetOverriddenProperties();
    }

    private void mockConfigServiceUrl(String url) {
        System.setProperty(ApolloClientSystemConsts.APOLLO_CONFIG_SERVICE, url);

        try {
            CONFIG_SERVICE_LOCATOR_CLEAR.invoke(CONFIG_SERVICE_LOCATOR);
        } catch (Exception e) {
            throw new IllegalStateException("Invoke config service locator clear failed.", e);
        }
    }

    private String loadConfigFor(String appId, String namespace) {
        final Properties prop = loadPropertiesOfNamespace(appId, namespace);
        Map<String, String> configurations = Maps.newHashMap();
        for (String propertyName : prop.stringPropertyNames()) {
            configurations.put(propertyName, prop.getProperty(propertyName));
        }
        ApolloConfig apolloConfig = new ApolloConfig(appId, "someCluster", namespace, "someReleaseKey");

        Map<String, String> mergedConfigurations = mergeOverriddenProperties(appId, namespace, configurations);
        apolloConfig.setConfigurations(mergedConfigurations);
        return GSON.toJson(apolloConfig);
    }

    private Properties loadPropertiesOfNamespace(String appId, String namespace) {
        String appSpecificFilename = String.format("mockdata-%s-%s.properties", appId, namespace);
        Properties appSpecific = loadPropertiesFromResource(appSpecificFilename, appId, namespace);
        if (appSpecific != null) {
            return appSpecific;
        }

        String filename = String.format("mockdata-%s.properties", namespace);
        Properties genericProperties = loadPropertiesFromResource(filename, appId, namespace);
        if (genericProperties != null) {
            return genericProperties;
        }
        return new LocalFileConfigRepository(appId, namespace).getConfig();
    }

    private Properties loadPropertiesFromResource(String filename, String appId, String namespace) {
        Object mockdataPropertiesExists = null;
        try {
            mockdataPropertiesExists = RESOURCES_UTILS_CLEAR.invoke(RESOURCES_UTILS, filename);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.error("invoke resources util locator clear failed.", e);
        }
        if (!Objects.isNull(mockdataPropertiesExists)) {
            logger.debug("load appId [{}] namespace [{}] from {}", appId, namespace, filename);
            return ResourceUtils.readConfigFile(filename, new Properties());
        }
        return null;
    }

    private String mockLongPollBody(String notificationsStr) {
        List<ApolloConfigNotification> oldNotifications = GSON.fromJson(notificationsStr, notificationType);
        List<ApolloConfigNotification> newNotifications = new ArrayList<>();
        for (ApolloConfigNotification notification : oldNotifications) {
            newNotifications
                    .add(new ApolloConfigNotification(notification.getNamespaceName(), notification.getNotificationId() + 1));
        }
        return GSON.toJson(newNotifications);
    }

    /**
     * 合并用户对namespace的修改
     */
    private Map<String, String> mergeOverriddenProperties(String appId, String namespace,
        Map<String, String> configurations) {
        Map<String, Map<String, String>> addedOrModifiedPropertiesOfNamespace =
            addedOrModifiedPropertiesOfAppAndNamespace.get(appId);
        if (addedOrModifiedPropertiesOfNamespace != null
            && addedOrModifiedPropertiesOfNamespace.containsKey(namespace)) {
            configurations.putAll(addedOrModifiedPropertiesOfNamespace.get(namespace));
        }
        Map<String, Set<String>> deletedKeysOfNamespace = deletedKeysOfAppAndNamespace.get(appId);
        if (deletedKeysOfNamespace != null && deletedKeysOfNamespace.containsKey(namespace)) {
            for (String k : deletedKeysOfNamespace.get(namespace)) {
                configurations.remove(k);
            }
        }
        return configurations;
    }

    /**
     * Add new property or update existed property
     */
    public void addOrModifyProperty(String namespace, String someKey, String someValue) {
        addOrModifyProperty(DEFAULT_APP_ID, namespace, someKey, someValue);
    }

    /**
     * Add new property or update existed property for the specified appId and namespace.
     */
    public void addOrModifyProperty(String appId, String namespace, String someKey, String someValue) {
        Map<String, Map<String, String>> addedOrModifiedPropertiesOfNamespace =
            addedOrModifiedPropertiesOfAppAndNamespace.computeIfAbsent(appId, key -> Maps.newConcurrentMap());
        if (addedOrModifiedPropertiesOfNamespace.containsKey(namespace)) {
            addedOrModifiedPropertiesOfNamespace.get(namespace).put(someKey, someValue);
            return;
        }
        Map<String, String> properties = Maps.newConcurrentMap();
        properties.put(someKey, someValue);
        addedOrModifiedPropertiesOfNamespace.put(namespace, properties);
    }

    /**
     * Delete existed property
     */
    public void deleteProperty(String namespace, String someKey) {
        deleteProperty(DEFAULT_APP_ID, namespace, someKey);
    }

    /**
     * Delete existed property for the specified appId and namespace.
     */
    public void deleteProperty(String appId, String namespace, String someKey) {
        Map<String, Set<String>> deletedKeysOfNamespace =
            deletedKeysOfAppAndNamespace.computeIfAbsent(appId, key -> Maps.newConcurrentMap());
        if (deletedKeysOfNamespace.containsKey(namespace)) {
            deletedKeysOfNamespace.get(namespace).add(someKey);
            return;
        }
        Set<String> keys = Sets.newConcurrentHashSet();
        keys.add(someKey);
        deletedKeysOfNamespace.put(namespace, keys);
    }

    /**
     * reset overridden properties
     */
    public void resetOverriddenProperties() {
        addedOrModifiedPropertiesOfAppAndNamespace.clear();
        deletedKeysOfAppAndNamespace.clear();
    }

    private void resetApolloClientState(boolean stopLongPolling) {
        try {
            RemoteConfigLongPollService longPollService =
                ApolloInjector.getInstance(RemoteConfigLongPollService.class);
            if (stopLongPolling) {
                REMOTE_CONFIG_LONG_POLL_STOP.invoke(longPollService);
            } else {
                prepareLongPollingService();
            }
            clearLongPollingState(longPollService);
            CONFIG_SERVICE_RESET.invoke(null);
        } catch (Throwable ex) {
            logger.warn("reset apollo client state failed.", ex);
        }
    }

    private static void prepareLongPollingService() throws Exception {
        RemoteConfigLongPollService longPollService =
            ApolloInjector.getInstance(RemoteConfigLongPollService.class);
        AtomicBoolean stopped = (AtomicBoolean) getLongPollField(longPollService, "m_longPollingStopped");
        stopped.set(false);
    }

    @SuppressWarnings("unchecked")
    private static void clearLongPollingState(RemoteConfigLongPollService longPollService) throws Exception {
        ((Map<String, Boolean>) getLongPollField(longPollService, "m_longPollStarted")).clear();
        ((Map<String, ?>) getLongPollField(longPollService, "m_longPollNamespaces")).clear();
        ((Table<String, String, Long>) getLongPollField(longPollService, "m_notifications")).clear();
        ((Map<String, ?>) getLongPollField(longPollService, "m_remoteNotificationMessages")).clear();
    }

    private static Object getLongPollField(RemoteConfigLongPollService longPollService, String fieldName)
        throws Exception {
        java.lang.reflect.Field field = RemoteConfigLongPollService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(longPollService);
    }
}
