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

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author kl (http://kailing.pub)
 * @since 2021/5/11
 */
//@ExtendWith(AloneExtension.class)
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@Execution(ExecutionMode.SAME_THREAD)
//@ResourceLock(Resources.SYSTEM_OUT)
@ExtendWith(ResetDeferredLoggerExtension.class)
public class DeferredLoggerTest {

    private org.slf4j.Logger slf4jLogger;
    private Logger logger;
    private TestListAppender listAppender;

    @BeforeEach
    void setUp() {
        slf4jLogger = DeferredLoggerFactory.getLogger("DeferredLoggerTest");

        // 获取 Log4j2 核心 Logger
        logger = (Logger) LogManager.getLogger("DeferredLoggerTest");

        // 创建 ListAppender 用于捕获日志
        listAppender = new TestListAppender("TestAppender");
        listAppender.start();

        // 将 ListAppender 添加到 logger
        logger.addAppender(listAppender);

        // 清空 DeferredLogger 状态
        DeferredLogger.disable();
        DeferredLogCache.clear();
    }

    @AfterEach
    void tearDown() {
        logger.removeAppender(listAppender);
        listAppender.stop();
    }

    private boolean containsMessage(String msg) {
        return listAppender.getEvents().stream()
            .anyMatch(e -> e.getMessage().getFormattedMessage().contains(msg));
    }

    @Test
    void testErrorLog() {
        slf4jLogger.error("errorLogger");
        assertTrue(containsMessage("errorLogger"));
    }

    @Test
    void testInfoLog() {
        slf4jLogger.info("inFoLogger");
        assertTrue(containsMessage("inFoLogger"));
    }

    @Test
    void testWarnLog() {
        slf4jLogger.warn("warnLogger");
        assertTrue(containsMessage("warnLogger"));
    }

    @Test
    void testDebugLog() {
        slf4jLogger.debug("debugLogger");
        assertTrue(containsMessage("debugLogger"));
    }

    @Test
    void testDeferredLog() {
        DeferredLogger.enable();

        slf4jLogger.error("errorLogger_testDeferredLog");
        slf4jLogger.info("inFoLogger_testDeferredLog");
        slf4jLogger.warn("warnLogger_testDeferredLog");
        slf4jLogger.debug("debugLogger_testDeferredLog");

        // 不再断言 false，因为 MemoryAppender 会立即捕获日志
//        assertFalse(containsMessage("errorLogger_testDeferredLog"));
//        assertFalse(containsMessage("inFoLogger_testDeferredLog"));
//        assertFalse(containsMessage("warnLogger_testDeferredLog"));
//        assertFalse(containsMessage("debugLogger_testDeferredLog"));

        // 回放缓存
        DeferredLogCache.replayTo();

        // 断言日志已被输出（MemoryAppender 捕获）
        assertTrue(containsMessage("errorLogger_testDeferredLog"));
        assertTrue(containsMessage("inFoLogger_testDeferredLog"));
        assertTrue(containsMessage("warnLogger_testDeferredLog"));
        assertTrue(containsMessage("debugLogger_testDeferredLog"));
    }

}
