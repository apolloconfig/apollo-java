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
package com.ctrip.framework.apollo.spring.annotation;

import java.lang.annotation.*;

/**
 * Use this annotation and <code>@ConfiguringProperties<code> to refresh ConfigurationProperties.
 *
 * <p>Usage example:</p>
 * <pre class="code">
 * &#064;Component
 * &#064;ConfigurationProperties(prefix = "redis.cache")
 * &#064;ApolloConfigurationPropertiesRefresh
 * public class SampleRedisConfig implements InitializingBean {
 *   private int expireSeconds;
 *   private String clusterNodes;
 *   private Map<String, String> someMap = Maps.newLinkedHashMap();
 *   private List<String> someList = Lists.newLinkedList();
 *   public void setExpireSeconds(int expireSeconds) {
 *     this.expireSeconds = expireSeconds;
 *   }
 *   public void setClusterNodes(String clusterNodes) {
 *     this.clusterNodes = clusterNodes;
 *   }
 *   public Map<String, String> getSomeMap() {
 *     return someMap;
 *   }
 *   public List<String> getSomeList() {
 *     return someList;
 *   }
 * }
 * </pre>
 *
 * Ensure <code>apollo.autoRefreshConfigurationProperties</code> should be true.<br />
 * In Spring Cloud environment, <code>org.springframework.cloud.context.config.annotation.RefreshScope<code> can replace this annotation to refresh ConfigurationProperties.
 * Before refreshing ConfigurationProperties, all validators in the bean will be checked. If they do not meet the requirements, the entire bean will stop refreshing and report an error.
 *
 * @author licheng
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface ApolloConfigurationPropertiesRefresh {

}
