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
package com.ctrip.framework.apollo.monitor.internal.util;

import java.lang.management.ManagementFactory;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * @author Rawven
 */
public final class JMXUtil {

  public static MBeanServer mbeanServer;

  public static ObjectName register(String name, Object mbean) {
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
      throw new IllegalArgumentException(name, e);
    }
  }

  public static void unregister(String name) {
    try {
      MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
      mbeanServer.unregisterMBean(new ObjectName(name));
    } catch (JMException e) {
      throw new IllegalArgumentException(name, e);
    }
  }
}
