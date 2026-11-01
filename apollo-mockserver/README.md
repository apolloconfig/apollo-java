# Apollo Mock Server

## Routing MockWebServer logs to SLF4J

MockWebServer uses `java.util.logging` (JUL). The bridge is opt-in: `start()` and
`close()` do not install or release it. Add `jul-to-slf4j` to your test classpath,
since the dependency in `apollo-mockserver` is optional:

```xml
<dependency>
  <groupId>org.slf4j</groupId>
  <artifactId>jul-to-slf4j</artifactId>
  <version>1.7.21</version>
  <scope>test</scope>
</dependency>
```

Explicitly acquire and release the bridge, ensuring the server closes even if
acquisition or release throws:

```java
ApolloTestingServer server = new ApolloTestingServer();
try {
  server.start();
  server.acquireJulBridge();
  // ...
} finally {
  try {
    server.releaseJulBridge();
  } finally {
    server.close();
  }
}
```

The bridge only reconfigures the `okhttp3.mockwebserver.MockWebServer` logger,
not the JUL root logger. Each server can own at most one acquisition; the bridge
is shared until the last owner releases it. Without `jul-to-slf4j`, or when the
SLF4J backend is `slf4j-jdk14`, acquisition logs a warning and leaves JUL logging
unchanged.

A failed acquisition creates no successful owner. It restores the original JUL
state immediately if possible. If rollback also fails, its exception is attached
to the acquisition exception as a suppressed exception, and the original state
is retained. A later acquisition must complete that recovery before installing a
new bridge; it throws if recovery is still blocked.

A failed final release throws and retains the last owner's acquisition and the
original JUL state for a retry. New owners cannot acquire during incomplete
teardown; the existing owner must retry `releaseJulBridge()` after the cause of
the failure is removed. The nested `finally` above guarantees server cleanup,
but handling or retrying a reported bridge-release failure remains the caller's
responsibility. Calling `close()` does not release bridge ownership.
