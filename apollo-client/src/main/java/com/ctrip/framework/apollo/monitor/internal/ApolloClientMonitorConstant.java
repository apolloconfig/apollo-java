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
package com.ctrip.framework.apollo.monitor.internal;

/**
 * @author Rawven
 */
public class ApolloClientMonitorConstant {

  /**
   * common
   */
  public static final String MBEAN_NAME = "apollo.client.monitor:type=";
  public static final String NAMESPACE = "namespace";
  public static final String TIMESTAMP = "timestamp";
  public static final String THROWABLE = "throwable";
  public static final String NAMESPACE_RELEASE_KEY = "releaseKey";
  public static final String ENV = "env";
  public static final String VERSION = "version";
  public static final String META_FRESH = "metaFreshTime";
  public static final String CONFIG_SERVICE_URL = "configServiceUrl";

  /**
   * tracer
   */
  public static final String APOLLO_CLIENT_CONFIGCHANGES = "Apollo.Client.ConfigChanges";
  public static final String APOLLO_CONFIG_EXCEPTION = "ApolloConfigException";
  public static final String APOLLO_META_SERVICE = "Apollo.MetaService";
  public static final String APOLLO_CONFIG_SERVICES = "Apollo.Config.Services";
  public static final String APOLLO_CLIENT_VERSION = "Apollo.Client.Version";
  public static final String APOLLO_CONFIGSERVICE = "Apollo.ConfigService";
  public static final String APOLLO_CONFIGSERVICE_HELP_STR = "periodicRefresh: ";
  public static final String APOLLO_CLIENT_CONFIGS = "Apollo.Client.Configs.";
  public static final String APOLLO_CLIENT_CONFIGMETA = "Apollo.Client.ConfigMeta";
  public static final String APOLLO_CLIENT_NAMESPACE_NOT_FOUND = "Apollo.Client.NamespaceNotFound";
  public static final String APOLLO_CLIENT_NAMESPACE_TIMEOUT = "Apollo.Client.NamespaceTimeout";
  public static final String APOLLO_CLIENT_NAMESPACE_USAGE = "Apollo.Client.NamespaceUsage";
  public static final String APOLLO_CLIENT_NAMESPACE_FIRST_LOAD_SPEND = "Apollo.Client.NamespaceFirstLoadSpendTime";

  /**
   * listener tag
   */
  public static final String TAG_ERROR = "ErrorMonitor";
  public static final String TAG_NAMESPACE = "NamespaceMonitor";
  public static final String TAG_BOOTSTRAP = "BootstrapMonitor";
  public static final String TAG_THREAD_POOL = "ThreadPoolMonitor";

  /**
   * metrics
   */
  public static final String METRICS_NAMESPACE_LATEST_UPDATE_TIME = "apollo_client_namespace_latest_update_time";
  public static final String METRICS_NAMESPACE_ITEM_NUM = "apollo_client_namespace_item_num";
  public static final String METRICS_EXCEPTION_NUM = "apollo_client_exception_num";
  public static final String METRICS_NAMESPACE_FIRST_LOAD_SPEND = "apollo_client_namespace_first_load_time_spend_in_ms";
  public static final String METRICS_NAMESPACE_USAGE = "apollo_client_namespace_usage";
  public static final String METRICS_NAMESPACE_NOT_FOUND = "apollo_client_namespace_not_found";
  public static final String METRICS_NAMESPACE_TIMEOUT = "apollo_client_namespace_timeout";
  public static final String METRICS_THREAD_POOL_NAME = "thread_pool_name";
  public static final String METRICS_THREAD_POOL_ACTIVE_TASK_COUNT = "apollo_client_thread_pool_active_task_count";
  public static final String METRICS_THREAD_POOL_QUEUE_SIZE = "apollo_client_thread_pool_queue_size";
  public static final String METRICS_THREAD_POOL_COMPLETED_TASK_COUNT = "apollo_client_thread_pool_completed_task_count";
  public static final String METRICS_THREAD_POOL_POOL_SIZE = "apollo_client_thread_pool_pool_size";
  public static final String METRICS_THREAD_POOL_TOTAL_TASK_COUNT = "apollo_client_thread_pool_total_task_count";
  public static final String METRICS_THREAD_POOL_CORE_POOL_SIZE = "apollo_client_thread_pool_core_pool_size";
  public static final String METRICS_THREAD_POOL_MAXIMUM_POOL_SIZE = "apollo_client_thread_pool_maximum_pool_size";
  public static final String METRICS_THREAD_POOL_LARGEST_POOL_SIZE = "apollo_client_thread_pool_largest_pool_size";
  public static final String METRICS_THREAD_POOL_QUEUE_REMAINING_CAPACITY = "apollo_client_thread_pool_queue_remaining_capacity";
}
