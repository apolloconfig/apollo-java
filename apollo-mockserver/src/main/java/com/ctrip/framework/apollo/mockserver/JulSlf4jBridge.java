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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;

/**
 * Routes okhttp's MockWebServer logs (which go through java.util.logging directly) to
 * slf4j/logback for the lifetime of active {@link ApolloTestingServer} instances.
 *
 * <p>Unlike {@code SLF4JBridgeHandler#install()}, this only touches the JUL logger used by
 * MockWebServer, not the JUL root logger, so unrelated JUL handlers elsewhere in the JVM are
 * never removed. It also skips installing when the SLF4J binding is slf4j-jdk14, since bridging
 * JUL to an SLF4J binding that itself delegates back to JUL creates an infinite loop. Calls are
 * reference-counted so concurrently running mock servers don't uninstall the bridge out from
 * under each other.
 *
 * <p>The ownership count is only moved once the corresponding logger transition succeeded, so a
 * failed {@link #install()} leaves the bridge reusable and a failed {@link #uninstall()} can be
 * retried instead of driving the count out of sync with the JUL logger state.
 *
 * <p>Uses reflection to instantiate {@code SLF4JBridgeHandler} so the class can load even when
 * {@code jul-to-slf4j} is not on the classpath.
 */
final class JulSlf4jBridge {

  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(JulSlf4jBridge.class);
  private static final String MOCK_WEB_SERVER_LOGGER_NAME = "okhttp3.mockwebserver.MockWebServer";
  private static final String SLF4J_BRIDGE_HANDLER_CLASS = "org.slf4j.bridge.SLF4JBridgeHandler";
  private static final AtomicInteger ACTIVE = new AtomicInteger(0);

  private static Handler[] originalHandlers;
  private static boolean originalUseParentHandlers;
  private static boolean bridged;
  private static Logger mockWebServerLogger;
  private static Handler bridgeHandler;

  private JulSlf4jBridge() {}

  static synchronized void install() {
    if (ACTIVE.get() <= 0) {
      installForFirstOwner();
    }
    // Only an owner whose install succeeded consumes a slot, so a failed install cannot make
    // later servers skip their own installation.
    ACTIVE.incrementAndGet();
  }

  private static void installForFirstOwner() {
    if (!isBridgeHandlerAvailable()) {
      LOG.warn("Skipping JUL-to-SLF4J bridge for MockWebServer logs because "
          + "jul-to-slf4j is not on the classpath");
      return;
    }
    if (isJdk14Binding(LoggerFactory.getILoggerFactory().getClass().getName())) {
      LOG.warn("Skipping JUL-to-SLF4J bridge for MockWebServer logs because the SLF4J binding "
          + "is slf4j-jdk14; bridging it would create an infinite JUL<->SLF4J logging loop.");
      return;
    }

    Logger logger = Logger.getLogger(MOCK_WEB_SERVER_LOGGER_NAME);
    Handler[] handlers = logger.getHandlers();
    boolean useParentHandlers = logger.getUseParentHandlers();

    Handler handler = null;
    try {
      handler = newBridgeHandler();
      for (Handler original : handlers) {
        logger.removeHandler(original);
      }
      logger.addHandler(handler);
      logger.setUseParentHandlers(false);
    } catch (RuntimeException | Error e) {
      // Leave no half-installed bridge behind, otherwise later servers would see a bridge
      // count that no installed bridge corresponds to.
      removeHandlerQuietly(logger, handler);
      restoreHandlers(logger, handlers, useParentHandlers);
      throw e;
    }

    mockWebServerLogger = logger;
    originalHandlers = handlers;
    originalUseParentHandlers = useParentHandlers;
    bridgeHandler = handler;
    bridged = true;
  }

  private static boolean isBridgeHandlerAvailable() {
    try {
      Class.forName(SLF4J_BRIDGE_HANDLER_CLASS);
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  private static Handler newBridgeHandler() {
    try {
      return (Handler) Class.forName(SLF4J_BRIDGE_HANDLER_CLASS).newInstance();
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
      throw new IllegalStateException(
          "SLF4JBridgeHandler was available during check but not during instantiation", e);
    }
  }

  static synchronized void uninstall() {
    int active = ACTIVE.get();
    if (active <= 0) {
      // Nothing is acquired, so releasing must not drive the count below zero.
      return;
    }
    if (active > 1 || !bridged) {
      ACTIVE.decrementAndGet();
      return;
    }
    // The last owner gives up its slot only once the JUL logger is restored, so a failed
    // teardown stays retryable instead of leaving the count out of sync.
    teardown();
    ACTIVE.decrementAndGet();
  }

  private static void teardown() {
    mockWebServerLogger.removeHandler(bridgeHandler);
    restoreHandlers(mockWebServerLogger, originalHandlers, originalUseParentHandlers);
    originalHandlers = null;
    originalUseParentHandlers = false;
    mockWebServerLogger = null;
    bridgeHandler = null;
    bridged = false;
  }

  /** Restores the previously removed handlers, without ever failing the caller. */
  private static void restoreHandlers(Logger logger, Handler[] handlers,
      boolean useParentHandlers) {
    try {
      for (Handler handler : handlers) {
        if (!containsHandler(logger.getHandlers(), handler)) {
          logger.addHandler(handler);
        }
      }
      logger.setUseParentHandlers(useParentHandlers);
    } catch (RuntimeException | Error e) {
      LOG.warn("Failed to restore the JUL handlers of {}", logger.getName(), e);
    }
  }

  private static void removeHandlerQuietly(Logger logger, Handler handler) {
    if (handler == null) {
      return;
    }
    try {
      logger.removeHandler(handler);
    } catch (RuntimeException | Error e) {
      LOG.warn("Failed to remove the JUL bridge handler from {}", logger.getName(), e);
    }
  }

  private static boolean containsHandler(Handler[] handlers, Handler target) {
    for (Handler handler : handlers) {
      if (handler == target) {
        return true;
      }
    }
    return false;
  }

  /** Package-visible for testing without depending on the actual runtime SLF4J binding. */
  static boolean isJdk14Binding(String iLoggerFactoryClassName) {
    return "org.slf4j.impl.JDK14LoggerFactory".equals(iLoggerFactoryClassName)
        || "org.slf4j.jul.JDK14LoggerFactory".equals(iLoggerFactoryClassName);
  }
}
