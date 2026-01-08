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

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * author : yang song
 * date   : 2026-01-08
 **/
public class ResetDeferredLoggerExtension implements BeforeEachCallback {
    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        // 清理 DeferredLogCache
        DeferredLogCache.clear();

        // 通过反射重置 DeferredLogger.state
        Field stateField = DeferredLogger.class.getDeclaredField("state");
        stateField.setAccessible(true);
        // 获取 AtomicInteger 对象
        AtomicInteger state = (AtomicInteger) stateField.get(null);
        // 只修改它的值，不替换对象
        state.set(-1);
    }
}
