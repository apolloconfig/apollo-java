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
package com.ctrip.framework.apollo.monitor.stress;

import static com.ctrip.framework.apollo.monitor.internal.ApolloClientMonitorConstant.*;

import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.monitor.internal.event.ApolloClientMonitorEventFactory;
import com.ctrip.framework.apollo.monitor.internal.event.ApolloClientMonitorEventPublisher;
import com.github.noconnor.junitperf.JUnitPerfRule;
import com.github.noconnor.junitperf.JUnitPerfTest;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@Ignore("Stress test")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ApolloClientMonitorStressTest.class)
public class ApolloClientMonitorStressTest {

  @Rule
  public JUnitPerfRule perfTestRule = new JUnitPerfRule();

  @Test
  @JUnitPerfTest(threads = 25, durationMs = 10000, warmUpMs = 1000, maxExecutionsPerSecond = 1000)
  public void testConfigMonitor() {
    System.out.println("abcdeft");
    ConfigService.getConfigMonitor().getExporterData();
  }

  @Test
  @JUnitPerfTest(threads = 50, durationMs = 10000, warmUpMs = 1000, maxExecutionsPerSecond = 1000)
  public void testPublishEvent() {
    ApolloClientMonitorEventPublisher.publish(
        ApolloClientMonitorEventFactory.getInstance()
            .createEvent(APOLLO_CLIENT_NAMESPACE_USAGE)
            .putAttachment(NAMESPACE, "application"));
  }
}
