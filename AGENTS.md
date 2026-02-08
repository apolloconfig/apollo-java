# Repository Guidelines

## Project Structure & Module Organization
- Multi-module Maven repository with root `pom.xml`.
- Core modules:
  - `apollo-core`: shared constants/utilities.
  - `apollo-client`: main Java client and Spring integration.
  - `apollo-client-config-data`: Spring Boot ConfigData integration.
  - `apollo-mockserver`: test/mock support.
  - `apollo-openapi`: OpenAPI client.
  - `apollo-plugin`: plugin modules.
- Tests follow standard Maven layout: `*/src/test/java` and `*/src/test/resources`.
- CI workflows are in `.github/workflows`.

## Build, Test, and Development Commands
- Build all modules: `mvn -B clean package -Dmaven.gitcommitid.skip=true`
- Run all tests: `mvn clean test`
- Run one module tests: `mvn -pl apollo-client clean test`
- Run targeted tests: `mvn -pl apollo-client -Dtest=ClassNameTest test`
- Compile only: `mvn -B clean compile -Dmaven.gitcommitid.skip=true`
- Notes:
  - This repository does **not** configure `spotless`; do not use `spotless:apply` here.
  - Some integration tests log connection warnings in local/dev environments; focus on final Maven summary.

## Coding Style & Naming Conventions
- Java style: follow existing codebase conventions (Google-style Java patterns in current files).
- Keep changes minimal and module-scoped.
- Preserve existing package/class naming conventions.
- Add/adjust tests for non-trivial behavior changes.

## Testing Guidelines
- JUnit 4 + JUnit Vintage are both used in this repo.
- For bug fixes:
  - Add a regression test first when feasible.
  - Run module-level tests for changed modules.
  - Prefer full `mvn clean test` before opening PR.

## Commit & Pull Request Guidelines
- Use Conventional Commits, e.g. `fix: ...`, `feat: ...`, `docs: ...`.
- If applicable, include issue linkage in commit message body, e.g. `Fixes #88`.
- Keep PR commits clean:
  - Require a single commit in the PR branch before review (squash locally if needed).
- Open PRs with `.github/PULL_REQUEST_TEMPLATE.md` and fill all sections with concrete content.
- PR description should include:
  - purpose/root cause
  - change summary
  - test commands actually run
- PR submission flow:
  - Push branch to personal fork remote first (for example `origin`).
  - Open PR from `<your-fork>:<branch>` to `apolloconfig/apollo-java:main`.
  - If history is rewritten for squash, update remote branch with `--force-with-lease`.

## CHANGES.md Rules
- Update `CHANGES.md` for user-visible fixes/features.
- Use bullet style consistent with existing entries.
- Entry format should be a Markdown link:
  - link text = the actual change description
  - link target = the PR URL (not issue URL)
- Example:
  - `[Fix ... detailed summary](https://github.com/apolloconfig/apollo-java/pull/123)`

## Agent Workflow Hints
- Reproduce first, then fix.
- Prefer targeted module/test runs during iteration, then run broader tests before PR.
- When creating upstream PRs:
  - use a branch in personal fork
  - base repository is `apolloconfig/apollo-java`
  - ensure PR branch is squashed to one commit
