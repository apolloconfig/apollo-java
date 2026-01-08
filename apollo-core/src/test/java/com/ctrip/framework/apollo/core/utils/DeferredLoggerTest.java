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
package com.ctrip.framework.apollo.core.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ctrip.framework.test.tools.AloneExtension;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;

/**
 * @author kl (http://kailing.pub)
 * @since 2021/5/11
 */
@ExtendWith(AloneExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DeferredLoggerTest {

  private static ByteArrayOutputStream outContent;
  private static Logger logger = null;
  private static PrintStream printStream;

  @BeforeAll
  public static void init() throws NoSuchFieldException, IllegalAccessException {
    DeferredLoggerTest.outContent = new ByteArrayOutputStream();
    DeferredLoggerTest.printStream = new PrintStream(DeferredLoggerTest.outContent);
    System.setOut(DeferredLoggerTest.printStream);
    DeferredLoggerTest.logger = DeferredLoggerFactory.getLogger("DeferredLoggerTest");
  }

  @Test
  public void testErrorLog() {
    DeferredLoggerTest.logger.error("errorLogger");
    assertTrue(DeferredLoggerTest.outContent.toString().contains("errorLogger"));
  }

  @Test
  public void testInfoLog() {
    DeferredLoggerTest.logger.info("inFoLogger");
    assertTrue(DeferredLoggerTest.outContent.toString().contains("inFoLogger"));
  }

  @Test
  public void testWarnLog() {
    DeferredLoggerTest.logger.warn("warnLogger");
    assertTrue(DeferredLoggerTest.outContent.toString().contains("warnLogger"));
  }

  @Test
  public void testDebugLog() {
    DeferredLoggerTest.logger.warn("debugLogger");
    assertTrue(DeferredLoggerTest.outContent.toString().contains("debugLogger"));
  }

  @Test
  public void testDeferredLog() {
    DeferredLogger.enable();

    DeferredLoggerTest.logger.error("errorLogger_testDeferredLog");
    DeferredLoggerTest.logger.info("inFoLogger_testDeferredLog");
    DeferredLoggerTest.logger.warn("warnLogger_testDeferredLog");
    DeferredLoggerTest.logger.debug("debugLogger_testDeferredLog");

    assertFalse(DeferredLoggerTest.outContent.toString().contains("errorLogger_testDeferredLog"));
    assertFalse(DeferredLoggerTest.outContent.toString().contains("inFoLogger_testDeferredLog"));
    assertFalse(DeferredLoggerTest.outContent.toString().contains("warnLogger_testDeferredLog"));
    assertFalse(DeferredLoggerTest.outContent.toString().contains("debugLogger_testDeferredLog"));

    DeferredLogCache.replayTo();

    assertTrue(DeferredLoggerTest.outContent.toString().contains("errorLogger_testDeferredLog"));
    assertTrue(DeferredLoggerTest.outContent.toString().contains("inFoLogger_testDeferredLog"));
    assertTrue(DeferredLoggerTest.outContent.toString().contains("warnLogger_testDeferredLog"));
    assertTrue(DeferredLoggerTest.outContent.toString().contains("debugLogger_testDeferredLog"));

  }

}
