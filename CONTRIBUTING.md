# Contributing to Apollo Java

Thanks for contributing to [apolloconfig/apollo-java](https://github.com/apolloconfig/apollo-java)! This
guide covers the project-specific steps needed before opening a pull request. The default branch of this
repository is `main`.

For general guidance on writing a good pull request description, see the
[Apollo server contributing guide](https://github.com/apolloconfig/apollo/blob/master/CONTRIBUTING.md) — note
that it documents the `apollo` (server) repository, so its build/format commands do not apply here.

## Building and testing

```bash
mvn clean test
```

## Code style (Spotless)

This repository is split across **two independent Maven reactors**:

- the root [`pom.xml`](pom.xml), which aggregates the regular client modules
  (`apollo-core`, `apollo-client`, `apollo-client-config-data`, `apollo-mockserver`, `apollo-openapi`,
  `apollo-plugin`);
- [`apollo-compat-tests/pom.xml`](apollo-compat-tests/pom.xml), which separately aggregates the
  compatibility-test modules (`apollo-api-compat-it`, `apollo-spring-compat-it`,
  `apollo-spring-boot-compat-it`).

The root POM's `<modules>` list does **not** include `apollo-compat-tests`, so running Spotless from the
repository root never touches the compatibility-test modules, and vice versa. CI checks both reactors as
separate steps, so you must format and check **both** before pushing — running only the root command will
pass locally and still fail CI.

Format your code:

```bash
mvn -B spotless:apply -Dmaven.gitcommitid.skip=true
mvn -B -f apollo-compat-tests/pom.xml spotless:apply -Dmaven.gitcommitid.skip=true
```

Verify formatting (this is what CI runs):

```bash
mvn -B spotless:check -Dmaven.gitcommitid.skip=true
mvn -B -f apollo-compat-tests/pom.xml spotless:check -Dmaven.gitcommitid.skip=true
```

## Updating the changelog

Update the [`CHANGES` log](CHANGES.md) to describe your change.
