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
package com.ctrip.framework.foundation.internals;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ctrip.framework.apollo.core.spi.Ordered;
import java.util.ServiceConfigurationError;
import org.junit.jupiter.api.Test;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class ServiceBootstrapTest {
  @Test
  public void loadFirstSuccessfully() throws Exception {
    Interface1 service = ServiceBootstrap.loadFirst(Interface1.class);
      assertInstanceOf(Interface1Impl.class, service);
  }

  @Test
  public void loadFirstWithNoServiceFileDefined() throws Exception {
      assertThrows(IllegalStateException.class,()->
          ServiceBootstrap.loadFirst(Interface2.class));
  }

  @Test
  public void loadFirstWithServiceFileButNoServiceImpl() throws Exception {
      assertThrows(IllegalStateException.class,()-> ServiceBootstrap.loadFirst(Interface3.class));
  }

  @Test
  public void loadFirstWithWrongServiceImpl() throws Exception {
      assertThrows(ServiceConfigurationError.class,()-> ServiceBootstrap.loadFirst(Interface4.class));
  }

  @Test
  public void loadFirstWithServiceImplNotExists() throws Exception {
      assertThrows(ServiceConfigurationError.class,()-> ServiceBootstrap.loadFirst(Interface5.class));
  }

  @Test
  public void loadAllWithServiceFileButNoServiceImpl() {
    assertFalse(ServiceBootstrap.loadAll(Interface7.class).hasNext());
  }

  @Test
  public void loadPrimarySuccessfully() {
    Interface6 service = ServiceBootstrap.loadPrimary(Interface6.class);
      assertInstanceOf(Interface6Impl1.class, service);
  }

  @Test
  public void loadPrimaryWithServiceFileButNoServiceImpl() {
      assertThrows(IllegalStateException.class,()-> ServiceBootstrap.loadPrimary(Interface7.class));
  }

  @Test
  public void loadAllOrderedWithServiceFileButNoServiceImpl() {
    assertTrue(ServiceBootstrap.loadAllOrdered(Interface7.class).isEmpty());
  }

  interface Interface1 {
  }

  public static class Interface1Impl implements Interface1 {
  }

  interface Interface2 {
  }

  interface Interface3 {
  }

  interface Interface4 {
  }

  interface Interface5 {
  }

  interface Interface6 extends Ordered {
  }

  public static class Interface6Impl1 implements Interface6 {
    @Override
    public int getOrder() {
      return 1;
    }
  }

  public static class Interface6Impl2 implements Interface6 {
    @Override
    public int getOrder() {
      return 2;
    }
  }

  interface Interface7 extends Ordered {
  }
}
