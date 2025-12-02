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
package com.ctrip.framework.apollo.config.data.util;

import java.lang.reflect.Method;
import java.util.function.Supplier;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.util.ClassUtils;

/**
 * Helper class to provide compatibility between Spring Boot 3.x and 4.x for bootstrap context
 * operations.
 * <p>
 * In Spring Boot 4.0, the bootstrap-related classes were moved from
 * {@code org.springframework.boot} to {@code org.springframework.boot.bootstrap}.
 *
 * @author vdisk <vdisk@foxmail.com>
 */
public class BootstrapRegistryHelper {

  private static final boolean SPRING_BOOT_4_PRESENT;
  private static final Method GET_BOOTSTRAP_CONTEXT_METHOD;
  private static final Method REGISTER_IF_ABSENT_METHOD;
  private static final Method INSTANCE_SUPPLIER_OF_METHOD;
  private static final Method INSTANCE_SUPPLIER_FROM_METHOD;
  private static final Method GET_METHOD;
  private static final Method GET_OR_ELSE_METHOD;

  static {
    ClassLoader classLoader = BootstrapRegistryHelper.class.getClassLoader();
    SPRING_BOOT_4_PRESENT = ClassUtils.isPresent(
        "org.springframework.boot.bootstrap.ConfigurableBootstrapContext", classLoader);

    try {
      GET_BOOTSTRAP_CONTEXT_METHOD = ApplicationStartingEvent.class.getMethod("getBootstrapContext");

      Class<?> bootstrapRegistryClass;
      Class<?> bootstrapContextClass;
      if (SPRING_BOOT_4_PRESENT) {
        bootstrapRegistryClass = ClassUtils.forName(
            "org.springframework.boot.bootstrap.BootstrapRegistry", classLoader);
        bootstrapContextClass = ClassUtils.forName(
            "org.springframework.boot.bootstrap.BootstrapContext", classLoader);
      } else {
        bootstrapRegistryClass = ClassUtils.forName(
            "org.springframework.boot.BootstrapRegistry", classLoader);
        bootstrapContextClass = ClassUtils.forName(
            "org.springframework.boot.BootstrapContext", classLoader);
      }

      Class<?> instanceSupplierClass = findInnerClass(bootstrapRegistryClass, "InstanceSupplier");
      REGISTER_IF_ABSENT_METHOD = bootstrapRegistryClass.getMethod("registerIfAbsent",
          Class.class, instanceSupplierClass);
      INSTANCE_SUPPLIER_OF_METHOD = instanceSupplierClass.getMethod("of", Object.class);
      INSTANCE_SUPPLIER_FROM_METHOD = instanceSupplierClass.getMethod("from", Supplier.class);
      GET_METHOD = bootstrapContextClass.getMethod("get", Class.class);
      GET_OR_ELSE_METHOD = bootstrapContextClass.getMethod("getOrElse", Class.class, Object.class);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to initialize BootstrapRegistryHelper", e);
    }
  }

  private static Class<?> findInnerClass(Class<?> outerClass, String innerClassName) {
    for (Class<?> innerClass : outerClass.getDeclaredClasses()) {
      if (innerClass.getSimpleName().equals(innerClassName)) {
        return innerClass;
      }
    }
    throw new IllegalStateException(
        "Cannot find inner class " + innerClassName + " in " + outerClass.getName());
  }

  /**
   * Get the bootstrap context from an ApplicationStartingEvent using reflection to support both
   * Spring Boot 3.x and 4.x.
   *
   * @param event the ApplicationStartingEvent
   * @return the bootstrap context (either from old or new package)
   */
  public static Object getBootstrapContext(ApplicationStartingEvent event) {
    try {
      return GET_BOOTSTRAP_CONTEXT_METHOD.invoke(event);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to invoke getBootstrapContext on event", e);
    }
  }

  /**
   * Register an instance if absent in the bootstrap context using reflection.
   *
   * @param bootstrapContext the bootstrap context
   * @param type             the type to register
   * @param instance         the instance to register
   * @param <T>              the type parameter
   */
  public static <T> void registerIfAbsent(Object bootstrapContext, Class<T> type, T instance) {
    try {
      Object wrappedSupplier = INSTANCE_SUPPLIER_OF_METHOD.invoke(null, instance);
      REGISTER_IF_ABSENT_METHOD.invoke(bootstrapContext, type, wrappedSupplier);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to register instance in bootstrap context", e);
    }
  }

  /**
   * Register an instance supplier if absent in the bootstrap context using reflection.
   *
   * @param bootstrapContext the bootstrap context
   * @param type             the type to register
   * @param instanceSupplier supplier for the instance
   * @param <T>              the type parameter
   */
  public static <T> void registerIfAbsentFromSupplier(Object bootstrapContext, Class<T> type,
      Supplier<T> instanceSupplier) {
    try {
      Object wrappedSupplier = INSTANCE_SUPPLIER_FROM_METHOD.invoke(null, instanceSupplier);
      REGISTER_IF_ABSENT_METHOD.invoke(bootstrapContext, type, wrappedSupplier);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to register instance in bootstrap context", e);
    }
  }

  /**
   * Get an instance from the bootstrap context using reflection.
   *
   * @param bootstrapContext the bootstrap context
   * @param type             the type to get
   * @param <T>              the type parameter
   * @return the instance
   */
  @SuppressWarnings("unchecked")
  public static <T> T get(Object bootstrapContext, Class<T> type) {
    try {
      return (T) GET_METHOD.invoke(bootstrapContext, type);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to get instance from bootstrap context", e);
    }
  }

  /**
   * Get an instance from the bootstrap context or return a default value.
   *
   * @param bootstrapContext the bootstrap context
   * @param type             the type to get
   * @param defaultValue     the default value
   * @param <T>              the type parameter
   * @return the instance or default value
   */
  @SuppressWarnings("unchecked")
  public static <T> T getOrElse(Object bootstrapContext, Class<T> type, T defaultValue) {
    try {
      return (T) GET_OR_ELSE_METHOD.invoke(bootstrapContext, type, defaultValue);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to get instance from bootstrap context", e);
    }
  }

  /**
   * Check if Spring Boot 4.x is present.
   *
   * @return true if Spring Boot 4.x classes are available
   */
  public static boolean isSpringBoot4Present() {
    return SPRING_BOOT_4_PRESENT;
  }
}
