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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class DateUtilTest {

    @Test
    public void testFormatLocalDateTime_validDate() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 12, 1, 10, 30, 0);
        
        Optional<String> formattedDate = DateUtil.formatLocalDateTime(dateTime);

        assertTrue(formattedDate.isPresent()); 
        assertEquals("2024-12-01 10:30:00", formattedDate.get());
    }

    @Test
    public void testFormatLocalDateTime_nullDate() {
        Optional<String> result = DateUtil.formatLocalDateTime(null);
        
        assertFalse(result.isPresent()); 
    }
}
