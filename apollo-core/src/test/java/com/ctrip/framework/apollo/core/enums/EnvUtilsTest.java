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
package com.ctrip.framework.apollo.core.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class EnvUtilsTest {

  @Test
  public void testTransformEnv() throws Exception {
    assertEquals(Env.DEV, EnvUtils.transformEnv(Env.DEV.name()));
    assertEquals(Env.FAT, EnvUtils.transformEnv(Env.FAT.name().toLowerCase()));
    assertEquals(Env.UAT, EnvUtils.transformEnv(" " + Env.UAT.name().toUpperCase()));
    assertEquals(Env.UNKNOWN, EnvUtils.transformEnv("someInvalidEnv"));
  }

  @Test
  public void testFromString() throws Exception {
    assertEquals(Env.DEV, Env.fromString(Env.DEV.name()));
    assertEquals(Env.FAT, Env.fromString(Env.FAT.name().toLowerCase()));
    assertEquals(Env.UAT, Env.fromString(" " + Env.UAT.name().toUpperCase()));
  }

  @Test
  public void testFromInvalidString() throws Exception {
      assertThrows(IllegalArgumentException.class,()->
          Env.fromString("someInvalidEnv"));
  }

  @Test
  public void fixTypoInProductionTest() {
    Env prod = Env.fromString("PROD");
    assertEquals(prod, Env.PRO);
  }

  @Test
  public void fromBlankStringTest() {
      assertThrows(IllegalArgumentException.class,()->
    Env.fromString(""));
  }

  @Test
  public void fromSpacesStringTest() {
      assertThrows(IllegalArgumentException.class,()-> Env.fromString("    "));
  }

  @Test
  public void fromNullStringTest() {
      assertThrows(IllegalArgumentException.class,()-> Env.fromString(null));
  }

}
