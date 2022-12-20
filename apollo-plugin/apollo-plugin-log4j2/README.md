# Apollo Log4j2

This module could let you integrate log4j2 with apollo easily.
You could create a namespace `log42.xml`, then log4j2 will load the configuration from it.

## How to use it?

There are several steps that need to do:

1. Add `apollo-plugin-log4j2` dependency with `log4j2` dependency
   ```xml
   <dependency>
       <groupId>com.ctrip.framework.apollo</groupId>
       <artifactId>apollo-plugin-log4j2</artifactId>
   </dependency>
   <!-- log4j2 dependency -->
   <dependency>
      <groupId>org.apache.logging.log4j</groupId>
      <artifactId>log4j-api</artifactId>
   </dependency>
   <dependency>
      <groupId>org.apache.logging.log4j</groupId>
      <artifactId>log4j-core</artifactId>
   </dependency>
   ```
2. Create a new namespace `log4j2.xml` (namespace format must be XML) in your apollo application
3. Add system properties `apollo.log4j2.enabled` or set env variable `APOLLO_LOG4J2_ENABLED` when you run java application
   ```bash
   -Dapollo.log4j2.enabled=true
   ```
   ```bash
   APOLLO_LOG4J2_ENABLED=true
   ```
4. Now run the java application, then it could load log4j2 content from Apollo

## Notice

By default, log4j2 will load the configuration from the classpath.
This module only affects when you set the system properties `apollo.log4j2.enabled=true` or set the env variable `APOLLO_LOG4J2_ENABLED =true` to enable it. 
