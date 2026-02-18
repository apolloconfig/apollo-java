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
package com.ctrip.framework.apollo.config.data.importer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;
import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.config.Profiles;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
public class ApolloConfigDataLocationResolverTest {

  private final DeferredLogFactory logFactory = destination -> destination.get();

  @Test
  public void testResolveDefaultNamespaceWhenLocationWithoutNamespace() {
    ApolloConfigDataLocationResolver resolver = new ApolloConfigDataLocationResolver(logFactory);
    ConfigDataLocation location = ConfigDataLocation.of("apollo://");
    ConfigDataLocationResolverContext context = null;
    Profiles profiles = null;

    List<ApolloConfigDataResource> resources =
        resolver.resolveProfileSpecific(context, location, profiles);

    assertEquals(1, resources.size());
    assertEquals("application", resources.get(0).getNamespace());
  }

  @Test
  public void testResolveExplicitNamespace() {
    ApolloConfigDataLocationResolver resolver = new ApolloConfigDataLocationResolver(logFactory);
    ConfigDataLocation location = ConfigDataLocation.of("apollo://TEST1.apollo");
    ConfigDataLocationResolverContext context = null;
    Profiles profiles = null;

    List<ApolloConfigDataResource> resources =
        resolver.resolveProfileSpecific(context, location, profiles);

    assertEquals(1, resources.size());
    assertEquals("TEST1.apollo", resources.get(0).getNamespace());
  }

  @Test
  public void testOrderAndResolvable() {
    ApolloConfigDataLocationResolver resolver = new ApolloConfigDataLocationResolver(logFactory);

    assertEquals(Ordered.HIGHEST_PRECEDENCE + 100, resolver.getOrder());
    assertTrue(resolver.isResolvable(null, ConfigDataLocation.of("apollo://application")));
  }
}
