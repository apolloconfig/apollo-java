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
package com.ctrip.framework.apollo.monitor.internal.model;

import java.util.HashMap;
import java.util.Map;

/**
 * metrics event model
 *
 * @author Rawven
 */
public class MetricsEvent {

  private final String name;
  private final String tag;
  private final Map<String, Object> attachments;

  private MetricsEvent(Builder builder) {
    this.name = builder.name;
    this.attachments = builder.attachment;
    this.tag = builder.tag;
  }

  public static Builder builder() {
    return new Builder();
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

  public static class Builder {

    private final Map<String, Object> attachment = new HashMap<>(3);
    private String name;
    private String tag;

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder putAttachment(String k, Object v) {
      this.attachment.put(k, v);
      return this;
    }

    public Builder withTag(String tag) {
      this.tag = tag;
      return this;
    }

    public void push() {
      MetricsEventPusher.push(this.build());
    }

    public MetricsEvent build() {
      return new MetricsEvent(this);
    }
  }
}
