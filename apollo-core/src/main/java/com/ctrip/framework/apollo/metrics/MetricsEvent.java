package com.ctrip.framework.apollo.metrics;

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
    return (T) attachments.get(key);
  }

  public String getTag() {
    return tag;
  }

  @Override
  public String toString() {
    return "MetricsEvent{" +
        "name='" + name + '\'' +
        ", attachments=" + attachments.toString() +
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

    // 构建 MetricsEvent 对象
    public MetricsEvent build() {
      return new MetricsEvent(this);
    }
  }
}
