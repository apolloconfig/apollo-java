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
package com.ctrip.framework.apollo.tracer.spi;

import com.ctrip.framework.apollo.core.spi.Ordered;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public interface MessageProducer extends Ordered {
  /**
   * Log an error.
   *
   * @param cause root cause exception
   */
  void logError(Throwable cause);

  /**
   * Log an error.
   *
   * @param cause root cause exception
   */
  void logError(String message, Throwable cause);

  /**
   * Log an event in one shot with SUCCESS status.
   *
   * @param type event type
   * @param name event name
   */
  void logEvent(String type, String name);

  /**
   * Log an event in one shot.
   *
   * @param type           event type
   * @param name           event name
   * @param status         "0" means success, otherwise means error code
   * @param nameValuePairs name value pairs in the format of "a=1&b=2&..."
   */
  void logEvent(String type, String name, String status, String nameValuePairs);


  /**
   * log metrics for count
   *
   * @param name        metrics name
   */
  default void logMetricsForCount(String name) {
      //do nothing
  }

  /**
   * Create a new transaction with given type and name.
   *
   * @param type transaction type
   * @param name transaction name
   */
  Transaction newTransaction(String type, String name);


  @Override
  default int getOrder() {
    return 0;
  }
}
