package com.ctrip.framework.apollo.metrics;

/**
 * metrics constant
 *
 * @author Rawven
 */
public class MetricsConstant {

  public static final String NAMESPACE = "namespace";
  public static final String APOLLO_CONFIG_EXCEPTION = "ApolloConfigException";

  public static final String TRACER = "Tracer";
  public static final String TRACER_ERROR = TRACER + ".Error";
  public static final String TRACER_EVENT = TRACER + ".Event";
  public static final String THROWABLE = TRACER + ".throwable";
  public static final String STATUS = TRACER + ".status";
  public static final String NAME_VALUE_PAIRS = TRACER + ".nameValuePairs";
}
