package com.ctrip.framework.apollo.internals;

public interface ConfigMonitorMBean {

    String getAppId();

    String getCluster();

    String getEnv();

    String getNamespace404();

    String getNamespaceTimeout();

    int getExceptionNum();

    String getNamespaceUsed();

    String getNamespaceUsedTime();

    String getDataWithCurrentMonitoringSystemFormat();
    //more method....
}
