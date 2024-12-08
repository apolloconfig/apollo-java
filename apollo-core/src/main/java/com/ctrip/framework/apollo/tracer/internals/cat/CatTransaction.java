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
package com.ctrip.framework.apollo.tracer.internals.cat;

import com.ctrip.framework.apollo.tracer.spi.Transaction;

import java.lang.reflect.Method;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class CatTransaction implements Transaction {

  private com.dianping.cat.message.Transaction catTransaction;

  public CatTransaction(com.dianping.cat.message.Transaction catTransaction) {
    this.catTransaction = catTransaction;
  }

  @Override
  public void setStatus(String status) {
      catTransaction.setStatus(status);
  }

  @Override
  public void setStatus(Throwable status) {
    catTransaction.setStatus(status);
  }

  @Override
  public void addData(String key, Object value) {
    catTransaction.addData(key, value);
  }

  @Override
  public void complete() {
    catTransaction.complete();
  }
}