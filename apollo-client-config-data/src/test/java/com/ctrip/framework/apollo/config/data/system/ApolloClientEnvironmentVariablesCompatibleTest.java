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
package com.ctrip.framework.apollo.config.data.system;

import com.ctrip.framework.apollo.core.ApolloClientSystemConsts;
import com.ctrip.framework.apollo.spring.boot.ApolloApplicationContextInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
//@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = ApolloClientPropertyCompatibleTestConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class ApolloClientEnvironmentVariablesCompatibleTest {

  @Autowired
  private ConfigurableEnvironment environment;

    /**
     * ⚠️ 在 Spring Context 初始化前执行
     */
    @DynamicPropertySource
    static void registerApolloEnv(DynamicPropertyRegistry registry) {
        registry.add(
            ApolloClientSystemConsts.APOLLO_CACHE_DIR,
            () -> "test-2/cacheDir"
        );
        registry.add(
            ApolloClientSystemConsts.APOLLO_ACCESS_KEY_SECRET,
            () -> "test-2-secret"
        );
        registry.add(
            ApolloClientSystemConsts.APOLLO_CONFIG_SERVICE,
            () -> "https://test-2-config-service"
        );
    }

  @Test
  public void testEnvironmentVariablesCompatible() throws Exception {
//    SystemLambda.withEnvironmentVariable(
//        ApolloClientSystemConsts.DEPRECATED_APOLLO_CACHE_DIR_ENVIRONMENT_VARIABLES,
//        "test-2/cacheDir")
//        .and(ApolloClientSystemConsts.DEPRECATED_APOLLO_ACCESS_KEY_SECRET_ENVIRONMENT_VARIABLES,
//            "test-2-secret")
//        .and(ApolloClientSystemConsts.DEPRECATED_APOLLO_CONFIG_SERVICE_ENVIRONMENT_VARIABLES,
//            "https://test-2-config-service")
//        .execute(() -> {
//          assertEquals("test-2/cacheDir",
//              this.environment.getProperty(ApolloClientSystemConsts.APOLLO_CACHE_DIR));
//          assertEquals("test-2-secret",
//              this.environment.getProperty(ApolloClientSystemConsts.APOLLO_ACCESS_KEY_SECRET));
//          assertEquals("https://test-2-config-service",
//              this.environment.getProperty(ApolloClientSystemConsts.APOLLO_CONFIG_SERVICE));
//        });
      Assertions.assertEquals(
          "test-2/cacheDir",
          environment.getProperty(ApolloClientSystemConsts.APOLLO_CACHE_DIR)
      );
      Assertions.assertEquals(
          "test-2-secret",
          environment.getProperty(ApolloClientSystemConsts.APOLLO_ACCESS_KEY_SECRET)
      );
      Assertions.assertEquals(
          "https://test-2-config-service",
          environment.getProperty(ApolloClientSystemConsts.APOLLO_CONFIG_SERVICE)
      );
  }

  @AfterEach
  public void clearProperty() {
    for (String propertyName : ApolloApplicationContextInitializer.APOLLO_SYSTEM_PROPERTIES) {
      System.clearProperty(propertyName);
    }
  }
}
