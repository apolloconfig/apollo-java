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
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.LoggingPermission;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

  @ParameterizedTest
  @CsvSource({"removeHandler, 1", "removeHandler, 2", "addHandler, 1", "setUseParentHandlers, 1"})
  void failedAcquisitionRestoresEveryMutationStage(String method, int occurrence) {
    assumeTrue(SECURITY_MANAGER_SUPPORTED);
    try (LoggerState state = new LoggerState()) {
      ApolloTestingServer failing = new ApolloTestingServer();
      whileLoggingControlFails(() -> {
        SecurityException failure =
            assertThrows(SecurityException.class, failing::acquireJulBridge);
        assertTrue(failure.getMessage().contains(method));
        assertEquals(0, failure.getSuppressed().length, "immediate rollback should succeed");
      }, new LoggingFailure(method, occurrence, false));

      assertEquals(0, activeCount());
      assertOwnership(failing, false);
      failing.releaseJulBridge();
      assertRestored(state);
      assertFreshAcquisitionWorks(state);
    }
  }

  @ParameterizedTest
  @CsvSource({"removeHandler, 1", "addHandler, 1", "addHandler, 2", "setUseParentHandlers, 1"})
  void failedFinalReleaseRetainsRecoveryAtEveryMutationStage(String method, int occurrence) {
    assumeTrue(SECURITY_MANAGER_SUPPORTED);
    try (LoggerState state = new LoggerState()) {
      ApolloTestingServer owner = new ApolloTestingServer();
      ApolloTestingServer later = new ApolloTestingServer();
      owner.acquireJulBridge();

      whileLoggingControlFails(() -> {
        assertThrows(SecurityException.class, owner::releaseJulBridge);
        assertEquals(1, activeCount());
        assertOwnership(owner, true);
        assertRecoveryState(state);
        if ("removeHandler".equals(method)) {
          assertTrue(hasBridgeHandler(state.logger));
        } else {
          assertFalse(hasBridgeHandler(state.logger));
          if ("addHandler".equals(method)) {
            assertEquals(occurrence - 1, state.logger.getHandlers().length,
                "earlier handler restoration must succeed before the targeted failure");
          }
        }
        assertFalse(state.logger.getUseParentHandlers());

        assertThrows(SecurityException.class, owner::releaseJulBridge,
            "retry must report restoration that is still blocked");
        assertThrows(IllegalStateException.class, later::acquireJulBridge,
            "a new owner must not acknowledge a partially torn-down bridge");
        assertOwnership(later, false);
        assertEquals(1, activeCount());
        assertRecoveryState(state);
      }, new LoggingFailure(method, occurrence, true));

      owner.releaseJulBridge();
      assertOwnership(owner, false);
      assertRestored(state);
      owner.releaseJulBridge();
      assertEquals(0, activeCount(), "repeated release must remain a no-op");
      assertFreshAcquisitionWorks(state);
    }
  }

  @ParameterizedTest
  @CsvSource({"setUseParentHandlers, 1, removeHandler, 3", "setUseParentHandlers, 1, addHandler, 2",
      "removeHandler, 2, addHandler, 1"})
  void failedAcquisitionRollbackRetainsBaselineForLaterRecovery(String acquisitionMethod,
      int acquisitionOccurrence, String rollbackMethod, int rollbackOccurrence) {
    assumeTrue(SECURITY_MANAGER_SUPPORTED);
    try (LoggerState state = new LoggerState()) {
      ApolloTestingServer failing = new ApolloTestingServer();
      whileLoggingControlFails(() -> {
        SecurityException failure =
            assertThrows(SecurityException.class, failing::acquireJulBridge);
        assertTrue(failure.getMessage().contains(acquisitionMethod));
        assertEquals(1, failure.getSuppressed().length,
            "rollback failure must be observable without hiding the acquisition failure");
        assertTrue(failure.getSuppressed()[0].getMessage().contains(rollbackMethod));
        assertEquals(0, activeCount());
        assertOwnership(failing, false);
        assertRecoveryState(state);
      }, new LoggingFailure(acquisitionMethod, acquisitionOccurrence, false),
          new LoggingFailure(rollbackMethod, rollbackOccurrence, false));

      failing.releaseJulBridge();
      assertEquals(0, activeCount(), "failed acquisition creates no owner to release");
      assertRecoveryState(state);
      assertFreshAcquisitionWorks(state);
    }
  }

  @Test
  void acquisitionMustRecoverPendingRollbackBeforeCreatingAnOwner() {
    assumeTrue(SECURITY_MANAGER_SUPPORTED);
    try (LoggerState state = new LoggerState()) {
      ApolloTestingServer failing = new ApolloTestingServer();
      ApolloTestingServer later = new ApolloTestingServer();
      whileLoggingControlFails(() -> {
        SecurityException failure =
            assertThrows(SecurityException.class, failing::acquireJulBridge);
        assertEquals(1, failure.getSuppressed().length);
        assertTrue(hasBridgeHandler(state.logger), "rollback removal is deliberately blocked");

        assertThrows(SecurityException.class, later::acquireJulBridge,
            "pending rollback must be recovered before a new installation");
        assertOwnership(failing, false);
        assertOwnership(later, false);
        assertEquals(0, activeCount());
        assertRecoveryState(state);
      }, new LoggingFailure("setUseParentHandlers", 1, false),
          new LoggingFailure("removeHandler", 3, true));

      later.acquireJulBridge();
      assertOwnership(later, true);
      assertEquals(1, activeCount());
      assertEquals(1, state.logger.getHandlers().length,
          "the leftover bridge must not be adopted as an original handler");
      assertTrue(hasBridgeHandler(state.logger));
      later.releaseJulBridge();
      later.releaseJulBridge();
      assertRestored(state);
      assertFreshAcquisitionWorks(state);
    }
  }

  @Test
  void finalReleasePreservesAddedHandlersWithoutDuplicatingOriginals() {
    try (LoggerState state = new LoggerState()) {
      ApolloTestingServer server = new ApolloTestingServer();
      server.acquireJulBridge();
      CaptureHandler added = new CaptureHandler();
      state.logger.addHandler(added);
      state.logger.addHandler(state.first);

      server.releaseJulBridge();
      assertEquals(0, activeCount());
      Handler[] restored = state.logger.getHandlers();
      assertEquals(3, restored.length);
      assertSame(added, restored[0]);
      assertSame(state.first, restored[1]);
      assertSame(state.second, restored[2]);
      assertTrue(state.logger.getUseParentHandlers());
      assertNullRetainedLogger();
      state.logger.info("record after restoring handlers");
      assertEquals(1, state.first.records);
      assertEquals(1, state.second.records);
      assertEquals(1, added.records);
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

  private static void assertRecoveryState(LoggerState state) {
    assertSame(state.logger, retainedMockWebServerLogger());
    assertArrayEquals(new Handler[] {state.first, state.second},
        (Handler[]) retainedField("originalHandlers"));
    assertEquals(true, retainedField("originalUseParentHandlers"));
  }

  private static void assertOwnership(ApolloTestingServer server, boolean owned) {
    try {
      Field field = ApolloTestingServer.class.getDeclaredField("bridgeAcquired");
      field.setAccessible(true);
      assertEquals(owned, field.getBoolean(server));
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  private static void assertRestored(LoggerState state) {
    assertEquals(0, activeCount());
    Handler[] handlers = state.logger.getHandlers();
    assertEquals(2, handlers.length);
    assertTrue(handlers[0] == state.first || handlers[1] == state.first);
    assertTrue(handlers[0] == state.second || handlers[1] == state.second);
    assertTrue(state.logger.getUseParentHandlers());
    assertFalse(hasBridgeHandler(state.logger));
    assertNullRetainedLogger();
    assertNull(retainedField("originalHandlers"));
    assertNull(retainedField("bridgeHandler"));

    int firstRecords = state.first.records;
    int secondRecords = state.second.records;
    state.logger.info("record after recovering JUL state");
    assertEquals(firstRecords + 1, state.first.records);
    assertEquals(secondRecords + 1, state.second.records);
  }

  private static void assertFreshAcquisitionWorks(LoggerState state) {
    ApolloTestingServer fresh = new ApolloTestingServer();
    fresh.acquireJulBridge();
    fresh.acquireJulBridge();
    assertEquals(1, activeCount());
    assertEquals(1, state.logger.getHandlers().length);
    assertTrue(hasBridgeHandler(state.logger));
    fresh.releaseJulBridge();
    fresh.releaseJulBridge();
    assertOwnership(fresh, false);
    assertRestored(state);
  }

  private static Object retainedField(String name) {
    try {
      Field field = JulSlf4jBridge.class.getDeclaredField(name);
      field.setAccessible(true);
      return field.get(null);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
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
    SecurityManager original = System.getSecurityManager();
    System.setSecurityManager(securityManager);
    try {
      securityManager.denyLoggingControl = true;
      action.run();
    } finally {
      securityManager.denyLoggingControl = false;
      System.setSecurityManager(original);
    }
  }

  private static void whileLoggingControlFails(Runnable action, LoggingFailure... failures) {
    DenyLoggingControlSecurityManager securityManager =
        new DenyLoggingControlSecurityManager(failures);
    SecurityManager original = System.getSecurityManager();
    System.setSecurityManager(securityManager);
    try {
      securityManager.denyLoggingControl = true;
      action.run();
    } finally {
      securityManager.denyLoggingControl = false;
      System.setSecurityManager(original);
    }
  }

  private static boolean isSecurityManagerSupported() {
    SecurityManager original = System.getSecurityManager();
    try {
      System.setSecurityManager(new DenyLoggingControlSecurityManager());
      System.setSecurityManager(original);
      return true;
    } catch (UnsupportedOperationException | SecurityException e) {
      return false;
    }
  }

  private static final class LoggerState implements AutoCloseable {

    private final Logger logger = Logger.getLogger(MOCK_WEB_SERVER_LOGGER_NAME);
    private final Handler[] handlers = logger.getHandlers();
    private final boolean useParentHandlers = logger.getUseParentHandlers();
    private final Level level = logger.getLevel();
    private final CaptureHandler first = new CaptureHandler();
    private final CaptureHandler second = new CaptureHandler();

    private LoggerState() {
      for (Handler handler : handlers) {
        logger.removeHandler(handler);
      }
      logger.addHandler(first);
      logger.addHandler(second);
      logger.setUseParentHandlers(true);
      logger.setLevel(Level.ALL);
    }

    @Override
    public void close() {
      try {
        // An acquisition can leave pending rollback without a successful owner. Recover it
        // through the public transition rather than clearing the bridge's private state.
        if (activeCount() == 0 && retainedMockWebServerLogger() != null) {
          JulSlf4jBridge.install();
        }
        while (activeCount() > 0) {
          JulSlf4jBridge.uninstall();
        }
      } finally {
        for (Handler handler : logger.getHandlers()) {
          logger.removeHandler(handler);
        }
        for (Handler handler : handlers) {
          logger.addHandler(handler);
        }
        logger.setUseParentHandlers(useParentHandlers);
        logger.setLevel(level);
      }
    }
  }

  private static final class CaptureHandler extends Handler {

    private int records;

    @Override
    public void publish(LogRecord record) {
      records++;
    }

    @Override
    public void flush() {}

    @Override
    public void close() {}
  }

  private static final class LoggingFailure {

    private final String method;
    private final int occurrence;
    private final boolean persistent;
    private int calls;

    private LoggingFailure(String method, int occurrence, boolean persistent) {
      this.method = method;
      this.occurrence = occurrence;
      this.persistent = persistent;
    }

    private void check(String mutation) {
      if (method.equals(mutation)) {
        calls++;
        if (calls == occurrence || persistent && calls > occurrence) {
          throw new SecurityException("denied Logger." + method + " at call " + calls);
        }
      }
    }
  }

  /** Can deny all logging control, or only a chosen occurrence of a JUL mutation. */
  private static final class DenyLoggingControlSecurityManager extends SecurityManager {

    private final LoggingFailure[] failures;
    private boolean denyLoggingControl;

    private DenyLoggingControlSecurityManager(LoggingFailure... failures) {
      this.failures = failures;
    }

    @Override
    public void checkPermission(Permission permission) {
      if (denyLoggingControl && permission instanceof LoggingPermission
          && "control".equals(permission.getName())) {
        if (failures.length == 0) {
          throw new SecurityException("denied " + permission);
        }
        for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
          if (Logger.class.getName().equals(frame.getClassName())
              && ("removeHandler".equals(frame.getMethodName())
                  || "addHandler".equals(frame.getMethodName())
                  || "setUseParentHandlers".equals(frame.getMethodName()))) {
            for (LoggingFailure failure : failures) {
              failure.check(frame.getMethodName());
            }
            return;
          }
        }
      }
    }

    @Override
    public void checkPermission(Permission permission, Object context) {
      checkPermission(permission);
    }
  }
}
