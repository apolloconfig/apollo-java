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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * @author Rawven
 * @date 2024/10/19
 */
public class DateUtil {
	 public static final DateTimeFormatter MEDIUM_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	/**
	 * Formats a LocalDateTime object to a string using the MEDIUM_FORMATTER.
	 *
	 * @param localDateTime the LocalDateTime to format, can be null
	 * @return the formatted date-time string, or null if the input is null
	 */
	public static Optional<String> formatLocalDateTime(LocalDateTime localDateTime) {
		return Optional.ofNullable(localDateTime)
				.map(dt -> dt.format(MEDIUM_FORMATTER));
	}
}
