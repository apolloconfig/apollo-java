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
package com.ctrip.framework.apollo.monitor.internal.jmx;

import static com.ctrip.framework.apollo.monitor.internal.ApolloClientMonitorConstant.MBEAN_NAME;

import com.ctrip.framework.apollo.core.utils.DeferredLoggerFactory;
import java.lang.management.ManagementFactory;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.slf4j.Logger;

/**
 * @author Rawven
 */
public final class ApolloClientJmxMBeanRegister {

  private static final Logger logger = DeferredLoggerFactory.getLogger(
      ApolloClientJmxMBeanRegister.class);
  private static MBeanServer mbeanServer;
  private static ObjectName ERROR_OBJECT_NAME;

  static {
    try {
      ERROR_OBJECT_NAME = new ObjectName(MBEAN_NAME + "error");
    } catch (MalformedObjectNameException e) {
      logger.warn("MalformedObjectNameException during static initialization.", e);
    }
  }

  public static synchronized void setMBeanServer(MBeanServer mbeanServer) {
    ApolloClientJmxMBeanRegister.mbeanServer = mbeanServer;
  }

  public static synchronized ObjectName register(String name, Object mbean) {
    try {
      ObjectName objectName = new ObjectName(name);

      if (mbeanServer == null) {
        mbeanServer = ManagementFactory.getPlatformMBeanServer();
      }

      if (mbeanServer.isRegistered(objectName)) {
        mbeanServer.unregisterMBean(objectName);
      }
      mbeanServer.registerMBean(mbean, objectName);

      return objectName;
    } catch (JMException e) {
      logger.error("Register JMX MBean failed.", e);
      return ERROR_OBJECT_NAME;
    }
  }

  public static synchronized void unregister(String name) {
    try {
      MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
      mbeanServer.unregisterMBean(new ObjectName(name));
    } catch (JMException e) {
      logger.error("Unregister JMX MBean failed.", e);
    }
  }
}
