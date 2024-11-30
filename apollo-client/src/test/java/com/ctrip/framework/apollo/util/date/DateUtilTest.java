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
package com.ctrip.framework.apollo.util.date;

import org.junit.Test;
import static org.junit.Assert.*;
import java.time.LocalDateTime;
import java.util.Optional;

public class DateUtilTest {

    @Test
    public void testFormatLocalDateTime_validDate() {
        // 给定一个有效的 LocalDateTime
        LocalDateTime dateTime = LocalDateTime.of(2024, 12, 1, 10, 30, 0);

        // 调用格式化方法
        Optional<String> formattedDate = DateUtil.formatLocalDateTime(dateTime);

        // 验证返回值是否为期望格式的日期字符串
        assertTrue(formattedDate.isPresent());  // 使用 isPresent() 检查 Optional 是否包含值
        assertEquals("2024-12-01 10:30:00", formattedDate.get());
    }

    @Test
    public void testFormatLocalDateTime_nullDate() {
        // 传入 null
        Optional<String> result = DateUtil.formatLocalDateTime(null);

        // 验证返回的 Optional 是否为空
        assertFalse(result.isPresent());  // 使用 isPresent() 判断 Optional 是否为空
    }
}
