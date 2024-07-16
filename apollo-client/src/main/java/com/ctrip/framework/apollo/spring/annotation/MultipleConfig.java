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
 * @author Terry.Lam This annotation is used as a supplement to @EnableApolloConfig to fill in
 * multiple appid
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface MultipleConfig {

  /**
   * multiple appId
   * @return
   */
  String appId();


  String[] namespaces();

  /**
   * eg. ${apollo.multiple.shop.secret}
   * @return
   */
  String secret() default "";
}
