package com.ctrip.framework.apollo.metrics;

public class MetricsEvent {
    private String name;
    private Object data;
    private String tag;

    private MetricsEvent(Builder builder) {
        this.name = builder.name;
        this.data = builder.data;
        this.tag = builder.tag;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    public Object getData() {
        return data;
    }
    public String getTag(){
        return tag;
    }

    @Override
    public String toString() {
        return "MetricsEvent{" +
            "name='" + name + '\'' +
            ", object=" + data + "tag=" + tag + "\n" +
            '}';
    }

    public static class Builder {
        private String name;
        private Object data;
        private String tag;

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withData(Object data) {
            this.data = data;
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
