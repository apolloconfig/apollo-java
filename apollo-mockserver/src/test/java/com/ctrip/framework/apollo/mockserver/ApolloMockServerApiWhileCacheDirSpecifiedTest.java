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
package com.ctrip.framework.apollo.mockserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.core.ApolloClientSystemConsts;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.internals.LocalFileConfigRepository;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import java.io.BufferedWriter;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(EmbeddedApollo.class)
public class ApolloMockServerApiWhileCacheDirSpecifiedTest {


  @Test
  public void testLoadDefaultLocalCacheDir() throws Exception {
    String someCacheDir = "src/test/resources/config-cache";
    String someAppId = "someAppId";
    String someNamespace = "someNamespace";
    String someKey = "someKey";
    String someValue = "someValue";
    System.setProperty(ApolloClientSystemConsts.APOLLO_CACHE_DIR, someCacheDir);

    ConfigUtil configUtil = spy(new ConfigUtil());
    doReturn(someAppId).when(configUtil).getAppId();
    String defaultLocalCacheDir = ReflectionTestUtils.invokeMethod(configUtil, "getDefaultLocalCacheDir", new Object[]{});
    assertEquals(someCacheDir + "/" + someAppId, defaultLocalCacheDir);

    // LocalFileConfigRepository.CONFIG_DIR
    LocalFileConfigRepository localFileConfigRepository = new LocalFileConfigRepository(someAppId, someNamespace);
    Field FIELD_CONFIG_DIR = localFileConfigRepository.getClass().getDeclaredField("CONFIG_DIR");
    FIELD_CONFIG_DIR.setAccessible(true);
    String configDir = (String) FIELD_CONFIG_DIR.get(localFileConfigRepository);

    File someBaseDir = new File(defaultLocalCacheDir, configDir);
    someBaseDir.mkdirs();
    File file = new File(someBaseDir, String.format("%s.properties", Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR).join(
            someAppId, "default", someNamespace)));
    try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), Charsets.UTF_8)) {
      writer.write(someKey + "=" + someValue);
    }
    Config config = ConfigService.getConfig(someNamespace);
    assertEquals(someValue, config.getProperty(someKey, null));
    file.deleteOnExit();
    someBaseDir.deleteOnExit();
  }
}
