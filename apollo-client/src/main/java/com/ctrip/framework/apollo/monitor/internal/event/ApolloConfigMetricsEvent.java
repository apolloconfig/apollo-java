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
package com.ctrip.framework.apollo.monitor.internal.event;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Rawven
 */
public class ApolloConfigMetricsEvent {

  private String name;
  private String tag;
  private Map<String, Object> attachments;

  public ApolloConfigMetricsEvent(String name, String tag, Map<String, Object> attachments) {
    this.name = name;
    this.tag = tag;
    this.attachments = attachments != null ? new HashMap<>(attachments) : new HashMap<>();
  }


  public void reset(String name) {
    this.name = name;
    this.tag = null;
    this.attachments.clear();
  }

  public ApolloConfigMetricsEvent withTag(String tag) {
    this.tag = tag;
    return this;
  }

  public ApolloConfigMetricsEvent putAttachment(String key, Object value) {
    this.attachments.put(key, value);
    return this;
  }

  public void push() {
    ApolloConfigMetricsEventPusher.push(this);
    ApolloConfigMetricsEventPool.getInstance().returnEvent(this);
  }

  public String getName() {
    return name;
  }

  @SuppressWarnings("unchecked")
  public <T> T getAttachmentValue(String key) {
    Object value = attachments.get(key);
    if (value == null) {
      return null;
    }
    try {
      return (T) value;
    } catch (ClassCastException e) {
      throw new IllegalArgumentException("Value for key " + key + " is not of expected type", e);
    }
  }

  public String getTag() {
    return tag;
  }

  @Override
  public String toString() {
    return "MetricsEvent{" +
        "name='" + name + '\'' +
        ", attachments=" + attachments +
        ", tag='" + tag + '\'' +
        '}';
  }

}