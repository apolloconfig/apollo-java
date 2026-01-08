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
package com.ctrip.framework.apollo;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.tracer.internals.MockMessageProducerManager;
import com.ctrip.framework.apollo.tracer.spi.MessageProducer;
import com.ctrip.framework.apollo.tracer.spi.Transaction;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.Callback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class BaseIntegrationTest {
  protected static final int PORT = findFreePort();
  private Server server;

  /**
   * init and start a jetty server, remember to call server.stop when the task is finished
   */
  protected Server startServerWithHandlers(ContextHandler... handlers) throws Exception {
    server = new Server(PORT);

    ContextHandlerCollection contexts = new ContextHandlerCollection();
    contexts.setHandlers(handlers);

    server.setHandler(contexts);
    server.start();

    return server;
  }


  @BeforeEach
  public void setUp() throws Exception {
    MessageProducer someProducer = mock(MessageProducer.class);
    MockMessageProducerManager.setProducer(someProducer);

    Transaction someTransaction = mock(Transaction.class);

    when(someProducer.newTransaction(anyString(), anyString())).thenReturn(someTransaction);
  }

  @AfterEach
  public void tearDown() throws Exception {
    if (server != null) {
      server.stop();
    }
  }

  protected ContextHandler mockServerHandler(final int statusCode, final String responseStr) {
    ContextHandler context = new ContextHandler("/");
    context.setHandler(new Handler.Abstract(){

        @Override
        public boolean handle(Request request, Response response, Callback callback)
            throws Exception {
            // 设置响应头
            response.setStatus(statusCode);
            response.getHeaders().put("Content-Type", "text/plain;charset=UTF-8");

            // 写入响应体
            ByteBuffer content = ByteBuffer.wrap(responseStr.getBytes(StandardCharsets.UTF_8));
            response.write(true, content, callback);

            return true; // 表示请求已处理
        }
    });
    return context;
  }

  /**
   * Returns a free port number on localhost.
   *
   * Heavily inspired from org.eclipse.jdt.launching.SocketUtil (to avoid a dependency to JDT just because of this).
   * Slightly improved with close() missing in JDT. And throws exception instead of returning -1.
   *
   * @return a free port number on localhost
   * @throws IllegalStateException if unable to find a free port
   */
  protected static int findFreePort() {
    ServerSocket socket = null;
    try {
      socket = new ServerSocket(0);
      socket.setReuseAddress(true);
      int port = socket.getLocalPort();
      try {
        socket.close();
      } catch (IOException e) {
        // Ignore IOException on close()
      }
      return port;
    } catch (IOException e) {
    } finally {
      if (socket != null) {
        try {
          socket.close();
        } catch (IOException e) {
        }
      }
    }
    throw new IllegalStateException("Could not find a free TCP/IP port to start embedded Jetty HTTP Server on");
  }
}
