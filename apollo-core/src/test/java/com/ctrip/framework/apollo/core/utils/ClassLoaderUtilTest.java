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
package com.ctrip.framework.apollo.core.utils;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

public class ClassLoaderUtilTest {
  private static boolean shouldFailInInitialization = false;

  @Test
  public void testGetClassLoader() {
    assertNotNull(ClassLoaderUtil.getLoader());
  }

  @Test
  public void testResolveClassPathWithFileUrl() throws Exception {
    Path tempDir = Files.createTempDirectory("apollo class path");
    try {
      ClassLoader classLoader = classLoaderReturning(tempDir.toUri().toURL());

      assertEquals(URLDecoder.decode(tempDir.toUri().toURL().getPath(), "utf-8"),
          ClassLoaderUtil.resolveClassPath(classLoader, "fallback"));
    } finally {
      Files.deleteIfExists(tempDir);
    }
  }

  @Test
  public void testResolveClassPathFallsBackForNestedJarUrl() throws Exception {
    String fallback = "/tmp/fallback";
    URL nestedJarUrl = createUrl("jar:nested:/tmp/apollo-app.jar/!BOOT-INF/classes/!/");
    ClassLoader classLoader = classLoaderReturning(nestedJarUrl);

    assertEquals(fallback, ClassLoaderUtil.resolveClassPath(classLoader, fallback));
  }

  @Test
  public void testResolveClassPathPreservesWindowsStyleFileUrlFormat() throws Exception {
    URL windowsFileUrl = new URL("file:/C:/Program%20Files/apollo/classes/");
    ClassLoader classLoader = classLoaderReturning(windowsFileUrl);

    assertEquals("/C:/Program Files/apollo/classes/",
        ClassLoaderUtil.resolveClassPath(classLoader, "fallback"));
  }

  @Test
  public void testGetClassPathFallsBackToUserDirForNestedJarUrl() throws Exception {
    String expectedClassPath = System.getProperty("user.dir");
    ClassLoader contextClassLoader =
        classLoaderReturning(createUrl("jar:nested:/tmp/apollo-app.jar/!BOOT-INF/classes/!/"));

    assertEquals(expectedClassPath, isolatedClassPath(contextClassLoader));
  }

  @Test
  public void testGetClassPathFallsBackToUserDirWhenLookupFails() throws Exception {
    String expectedClassPath = System.getProperty("user.dir");
    ClassLoader contextClassLoader = new ClassLoader(null) {
      @Override
      public URL getResource(String name) {
        throw new RuntimeException("lookup failed");
      }
    };

    assertEquals(expectedClassPath, isolatedClassPath(contextClassLoader));
  }

  @Test
  public void testIsClassPresent() {
    assertTrue(ClassLoaderUtil.isClassPresent("java.lang.String"));
  }

  @Test
  public void testIsClassPresentWithClassNotFound() {
    assertFalse(ClassLoaderUtil.isClassPresent("java.lang.StringNotFound"));
  }

  @Test
  public void testIsClassPresentWithLinkageError() {
    shouldFailInInitialization = true;
    assertFalse(ClassLoaderUtil.isClassPresent(ClassWithInitializationError.class.getName()));
  }

  public static class ClassWithInitializationError {
    static {
      if (ClassLoaderUtilTest.shouldFailInInitialization) {
        throw new RuntimeException("Some initialization exception");
      }
    }
  }

  private ClassLoader classLoaderReturning(URL resource) {
    return new ClassLoader(null) {
      @Override
      public URL getResource(String name) {
        return resource;
      }
    };
  }

  private URL createUrl(String spec) throws Exception {
    return new URL(null, spec, new URLStreamHandler() {
      @Override
      protected URLConnection openConnection(URL url) {
        throw new UnsupportedOperationException();
      }
    });
  }

  private String isolatedClassPath(ClassLoader contextClassLoader) throws Exception {
    ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(contextClassLoader);
      Class<?> isolatedClassLoaderUtil = newIsolatedClassLoader().loadClass(
          ClassLoaderUtil.class.getName());
      Method getClassPath = isolatedClassLoaderUtil.getMethod("getClassPath");
      return (String) getClassPath.invoke(null);
    } finally {
      Thread.currentThread().setContextClassLoader(originalClassLoader);
    }
  }

  private ClassLoader newIsolatedClassLoader() throws IOException {
    String className = ClassLoaderUtil.class.getName();
    String classFile = className.replace('.', '/') + ".class";
    byte[] classBytes = readClassBytes(classFile);

    return new ClassLoader(ClassLoaderUtil.class.getClassLoader()) {
      @Override
      protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (!className.equals(name)) {
          return super.loadClass(name, resolve);
        }

        synchronized (getClassLoadingLock(name)) {
          Class<?> loadedClass = findLoadedClass(name);
          if (loadedClass == null) {
            loadedClass = defineClass(name, classBytes, 0, classBytes.length);
          }
          if (resolve) {
            resolveClass(loadedClass);
          }
          return loadedClass;
        }
      }
    };
  }

  private byte[] readClassBytes(String classFile) throws IOException {
    try (InputStream inputStream = ClassLoaderUtil.class.getClassLoader().getResourceAsStream(
        classFile);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      assertNotNull(inputStream);

      byte[] buffer = new byte[1024];
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, bytesRead);
      }
      return outputStream.toByteArray();
    }
  }
}
