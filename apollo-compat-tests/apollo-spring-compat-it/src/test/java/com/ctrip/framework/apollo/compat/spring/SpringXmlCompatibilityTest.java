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
package com.ctrip.framework.apollo.compat.spring;

import static org.junit.Assert.assertEquals;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.mockserver.EmbeddedApollo;
import java.util.Properties;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/spring/apollo-context.xml")
public class SpringXmlCompatibilityTest {

  @ClassRule
  public static final EmbeddedApollo EMBEDDED_APOLLO = new EmbeddedApollo();

  @Autowired
  private SpringXmlBean xmlBean;

  @BeforeClass
  public static void beforeClass() throws Exception {
    SpringCompatibilityTestSupport.beforeClass(EMBEDDED_APOLLO);
  }

  @AfterClass
  public static void afterClass() throws Exception {
    SpringCompatibilityTestSupport.afterClass();
  }

  @Test
  public void shouldSupportXmlConfig() throws Exception {
    assertEquals(5099, xmlBean.getTimeout());
    assertEquals(51, xmlBean.getBatch());
    assertEquals("from-public", xmlBean.getPublicKey());
    assertEquals("from-yaml", xmlBean.getYamlMarker());

    Config applicationConfig = ConfigService.getConfig("application");
    Properties applicationProperties =
        SpringCompatibilityTestSupport.copyConfigProperties(applicationConfig);
    applicationProperties.setProperty("compat.xml.timeout", "5199");
    applicationProperties.setProperty("compat.xml.batch", "61");
    SpringCompatibilityTestSupport.applyConfigChange(applicationConfig, "application",
        applicationProperties);

    Config publicConfig = ConfigService.getConfig("TEST1.apollo");
    Properties publicProperties = SpringCompatibilityTestSupport.copyConfigProperties(publicConfig);
    publicProperties.setProperty("public.key", "from-public-xml-updated");
    SpringCompatibilityTestSupport.applyConfigChange(publicConfig, "TEST1.apollo", publicProperties);

    Config yamlConfig = ConfigService.getConfig("application.yaml");
    Properties yamlProperties = SpringCompatibilityTestSupport.copyConfigProperties(yamlConfig);
    yamlProperties.setProperty("yaml.marker", "from-yaml-xml-updated");
    SpringCompatibilityTestSupport.applyConfigChange(yamlConfig, "application.yaml", yamlProperties);

    SpringCompatibilityTestSupport.waitForCondition("xml timeout should be updated",
        () -> xmlBean.getTimeout() == 5199);
    SpringCompatibilityTestSupport.waitForCondition("xml batch should be updated",
        () -> xmlBean.getBatch() == 61);
    SpringCompatibilityTestSupport.waitForCondition("xml public key should be updated",
        () -> "from-public-xml-updated".equals(xmlBean.getPublicKey()));
    SpringCompatibilityTestSupport.waitForCondition("xml yaml marker should be updated",
        () -> "from-yaml-xml-updated".equals(xmlBean.getYamlMarker()));
  }
}
