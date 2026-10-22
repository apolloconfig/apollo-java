/*
 * Copyright 2026 Apollo Authors
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.reflect.Field;
import java.security.Permission;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.LoggingPermission;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.bridge.SLF4JBridgeHandler;

class JulSlf4jBridgeTest {

  private static final String MOCK_WEB_SERVER_LOGGER_NAME = "okhttp3.mockwebserver.MockWebServer";
  private static final boolean SECURITY_MANAGER_SUPPORTED = isSecurityManagerSupported();

  @AfterEach
  void releaseAnyBridgeOwnership() {
    // The bridge is JVM global state; make every test start from an unowned bridge even when
    // another test failed half way through.
    for (int owned = activeCount(); owned > 0; owned--) {
      JulSlf4jBridge.uninstall();
    }
  }

  @Test
  void isJdk14BindingDetectsKnownFactoryClassNames() {
    assertTrue(JulSlf4jBridge.isJdk14Binding("org.slf4j.impl.JDK14LoggerFactory"));
    assertTrue(JulSlf4jBridge.isJdk14Binding("org.slf4j.jul.JDK14LoggerFactory"));
    assertFalse(JulSlf4jBridge.isJdk14Binding("ch.qos.logback.classic.LoggerContext"));
  }

  @Test
  void installOnlyTouchesMockWebServerLoggerNotRoot() {
    Logger root = Logger.getLogger("");
    Handler[] rootHandlersBefore = root.getHandlers();

    Logger mockWebServerLogger = Logger.getLogger(MOCK_WEB_SERVER_LOGGER_NAME);
    Handler[] mwsHandlersBefore = mockWebServerLogger.getHandlers();
    boolean useParentBefore = mockWebServerLogger.getUseParentHandlers();

    JulSlf4jBridge.install();
    try {
      assertArrayEquals(rootHandlersBefore, root.getHandlers(),
          "installing the bridge must not touch the JUL root logger's handlers");
      assertTrue(hasBridgeHandler(mockWebServerLogger),
          "MockWebServer's own logger should get the bridge handler");
      assertFalse(mockWebServerLogger.getUseParentHandlers());
    } finally {
      JulSlf4jBridge.uninstall();
    }

    assertArrayEquals(rootHandlersBefore, root.getHandlers());
    assertArrayEquals(mwsHandlersBefore, mockWebServerLogger.getHandlers());
    assertEquals(useParentBefore, mockWebServerLogger.getUseParentHandlers());
  }

  @Test
  void closingSameServerTwiceDoesNotDisruptAnotherActiveServer() throws Exception {
    Logger mockWebServerLogger = Logger.getLogger(MOCK_WEB_SERVER_LOGGER_NAME);
    ApolloTestingServer serverA = new ApolloTestingServer();
    ApolloTestingServer serverB = new ApolloTestingServer();
    try {
      serverA.start();
      serverA.acquireJulBridge();
      serverB.start();
      serverB.acquireJulBridge();
      assertTrue(hasBridgeHandler(mockWebServerLogger));

      serverA.releaseJulBridge();
      serverA.releaseJulBridge();

      assertTrue(hasBridgeHandler(mockWebServerLogger),
          "bridge must remain installed while server B is still running");
    } finally {
      serverA.releaseJulBridge();
      serverA.close();
      serverB.releaseJulBridge();
      serverB.close();
    }

    assertFalse(hasBridgeHandler(mockWebServerLogger));
  }

  @Test
  void closingNeverStartedServerDoesNotDisruptActiveServer() throws Exception {
    Logger mockWebServerLogger = Logger.getLogger(MOCK_WEB_SERVER_LOGGER_NAME);
    ApolloTestingServer active = new ApolloTestingServer();
    ApolloTestingServer neverStarted = new ApolloTestingServer();
    try {
      active.start();
      active.acquireJulBridge();
      assertTrue(hasBridgeHandler(mockWebServerLogger));

      neverStarted.releaseJulBridge();

      assertTrue(hasBridgeHandler(mockWebServerLogger),
          "releasing a bridge that was never acquired must not affect the active bridge");
    } finally {
      active.releaseJulBridge();
      active.close();
      neverStarted.close();
    }

    assertFalse(hasBridgeHandler(mockWebServerLogger));
  }

  @Test
  void failedAcquisitionKeepsTheBridgeAvailableForLaterServers() {
    assumeTrue(SECURITY_MANAGER_SUPPORTED,
        "denying java.util.logging control requires the security manager API");
    Logger mockWebServerLogger = Logger.getLogger(MOCK_WEB_SERVER_LOGGER_NAME);
    assertEquals(0, activeCount(), "the bridge must start out unowned");

    ApolloTestingServer failing = new ApolloTestingServer();
    whileLoggingControlDenied(
        () -> assertThrows(SecurityException.class, failing::acquireJulBridge));

    assertEquals(0, activeCount(), "a failed acquisition must not consume the ownership");
    assertFalse(hasBridgeHandler(mockWebServerLogger),
        "a failed acquisition must not leave a half-installed bridge behind");

    ApolloTestingServer later = new ApolloTestingServer();
    later.acquireJulBridge();
    try {
      assertEquals(1, activeCount());
      assertTrue(hasBridgeHandler(mockWebServerLogger),
          "a later server must still be able to install the bridge");
    } finally {
      later.releaseJulBridge();
      later.close();
      failing.close();
    }

    assertFalse(hasBridgeHandler(mockWebServerLogger));
  }

  @Test
  void failedReleaseKeepsTheOwnershipSoThatItCanBeRetried() {
    assumeTrue(SECURITY_MANAGER_SUPPORTED,
        "denying java.util.logging control requires the security manager API");
    Logger mockWebServerLogger = Logger.getLogger(MOCK_WEB_SERVER_LOGGER_NAME);
    assertEquals(0, activeCount(), "the bridge must start out unowned");

    ApolloTestingServer server = new ApolloTestingServer();
    server.acquireJulBridge();
    try {
      assertTrue(hasBridgeHandler(mockWebServerLogger));

      whileLoggingControlDenied(
          () -> assertThrows(SecurityException.class, server::releaseJulBridge));

      assertEquals(1, activeCount(), "a failed release must keep the ownership for a retry");
      assertTrue(hasBridgeHandler(mockWebServerLogger),
          "a failed release must leave the bridge installed");

      server.releaseJulBridge();
      assertEquals(0, activeCount());
      assertFalse(hasBridgeHandler(mockWebServerLogger));

      server.releaseJulBridge();
      assertEquals(0, activeCount(), "releasing more often than acquiring must not go negative");
    } finally {
      server.releaseJulBridge();
      server.close();
    }
  }

  @Test
  void bridgeRetainsLoggerUntilMockWebServerInitializes() throws Exception {
    ApolloTestingServer server = new ApolloTestingServer();
    try {
      server.start();
      server.acquireJulBridge();

      Logger mockWebServerLogger = Logger.getLogger(MOCK_WEB_SERVER_LOGGER_NAME);
      assertSame(mockWebServerLogger, retainedMockWebServerLogger(),
          "the bridge must retain the logger MockWebServer logs to, so that GC cannot "
              + "silently detach its bridge handler");
      assertTrue(hasBridgeHandler(mockWebServerLogger),
          "the MockWebServer logger must keep its bridge handler for the bridge's lifetime");
    } finally {
      server.releaseJulBridge();
      server.close();
    }

    assertNullRetainedLogger();
  }

  @Test
  void startupFailureDoesNotInstallBridge() throws Exception {
    Logger mockWebServerLogger = Logger.getLogger(MOCK_WEB_SERVER_LOGGER_NAME);
    ApolloTestingServer server = new ApolloTestingServer();

    Field locatorField = ApolloTestingServer.class.getDeclaredField("CONFIG_SERVICE_LOCATOR");
    locatorField.setAccessible(true);
    Object originalLocator = locatorField.get(null);
    locatorField.set(null, null);
    try {
      assertThrows(IllegalStateException.class, server::start, "start() must surface failures");
      assertFalse(hasBridgeHandler(mockWebServerLogger),
          "a failed start() must not install the JUL bridge");
    } finally {
      locatorField.set(null, originalLocator);
      server.close();
    }
  }

  private static boolean hasBridgeHandler(Logger logger) {
    for (Handler handler : logger.getHandlers()) {
      if (handler instanceof SLF4JBridgeHandler) {
        return true;
      }
    }
    return false;
  }

  private static void assertNullRetainedLogger() {
    assertNull(retainedMockWebServerLogger(),
        "the bridge must release the retained logger on teardown");
  }

  private static int activeCount() {
    try {
      Field active = JulSlf4jBridge.class.getDeclaredField("ACTIVE");
      active.setAccessible(true);
      return ((AtomicInteger) active.get(null)).get();
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  private static Logger retainedMockWebServerLogger() {
    try {
      Field field = JulSlf4jBridge.class.getDeclaredField("mockWebServerLogger");
      field.setAccessible(true);
      return (Logger) field.get(null);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Runs {@code action} while {@code java.util.logging} control checks are denied, which is how a
   * JVM without permission to reconfigure JUL behaves.
   */
  private static void whileLoggingControlDenied(Runnable action) {
    DenyLoggingControlSecurityManager securityManager = new DenyLoggingControlSecurityManager();
    System.setSecurityManager(securityManager);
    try {
      securityManager.denyLoggingControl = true;
      action.run();
    } finally {
      securityManager.denyLoggingControl = false;
      System.setSecurityManager(null);
    }
  }

  private static boolean isSecurityManagerSupported() {
    try {
      System.setSecurityManager(new DenyLoggingControlSecurityManager());
      System.setSecurityManager(null);
      return true;
    } catch (Throwable t) {
      return false;
    }
  }

  /** Allows everything but {@code LoggingPermission("control")}, so JUL reconfiguration fails. */
  private static final class DenyLoggingControlSecurityManager extends SecurityManager {

    private boolean denyLoggingControl;

    @Override
    public void checkPermission(Permission permission) {
      // java.util.logging checks LoggingPermission("control") before every reconfiguration.
      if (denyLoggingControl && permission instanceof LoggingPermission
          && "control".equals(permission.getName())) {
        throw new SecurityException("denied " + permission);
      }
    }

    @Override
    public void checkPermission(Permission permission, Object context) {
      checkPermission(permission);
    }
  }
}
