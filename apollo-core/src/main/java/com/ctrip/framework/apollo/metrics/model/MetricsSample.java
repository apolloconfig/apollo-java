package com.ctrip.framework.apollo.metrics.model;

import com.ctrip.framework.apollo.metrics.util.MeterType;
import java.util.Map;

/**
 * @author Rawven
 */
public class MetricsSample {

  protected String name;
  protected MeterType type;
  protected Map<String, String> tags;

  public String getName() {
    return "Apollo_" + name;
  }

  public MeterType getType() {
    return type;
  }

  public Map<String, String> getTags() {
    return tags;
  }

  public void setTag(Map<String, String> tags) {
    this.tags = tags;
  }
}
