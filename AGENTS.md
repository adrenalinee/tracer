# Repository Guidelines

## Project Structure & Module Organization
This repository is a multi-module Gradle library for Spring-based tracing. Core logic lives in `tracer-core/src/main/kotlin`; Spring integrations are split by runtime:
- `tracer-webmvc`: servlet stack support, plus the only current automated tests in `src/test/kotlin`
- `tracer-webflux`: reactive server tracing
- `tracer-webclient`: outbound `WebClient` tracing
- `tracer-task`: Spring Task integration

Shared docs are in the root `README.md` and module-specific `README.md` files. Logback appender resources live under `tracer-core/src/main/resources/malibu/tracer/logback`.

## Build, Test, and Development Commands
- `./gradlew build`: compile all modules, run tests, and assemble jars
- `./gradlew test`: run the current JUnit 5 test suite across all modules
- `./gradlew :tracer-webmvc:test`: run the concrete integration tests that exist today
- `./gradlew publishToMavenLocal`: build and publish snapshot artifacts locally for sample apps

Use Java 21 toolchains; Gradle is configured through the checked-in wrapper.

## Coding Style & Naming Conventions
Follow Kotlin official style (`kotlin.code.style=official`) with 4-space indentation. Keep package names lowercase (`malibu.tracer.webmvc`) and use `PascalCase` for classes, `camelCase` for functions and properties, and `UPPER_SNAKE_CASE` for constants. Prefer descriptive type names ending in their role, such as `TracerWebMvcConfiguration` or `TraceIdGenerator`.

Keep modules focused: shared abstractions belong in `tracer-core`, while transport-specific behavior stays in the corresponding integration module.

## Testing Guidelines
Tests use JUnit 5, Spring Boot Test, AssertJ, and Mockito Kotlin. Name test files `*Test.kt` and keep scenario-oriented method names such as `getTest` or `postTest`. Add tests in the same module as the behavior you changed; for web request logging, mirror the existing `tracer-webmvc/src/test/kotlin/malibu/tracer/test` patterns. There is no enforced coverage gate, but new features and bug fixes should include regression tests where practical.

## Commit & Pull Request Guidelines
Recent history favors short, imperative summaries, often in Korean, for example: `spring 버전 최신으로 변경` or `Harden trace payload handling and modernize build`. Keep commits narrowly scoped and descriptive.

PRs should include:
- a concise problem/solution summary
- linked issue or context when available
- test evidence (`./gradlew test`, module-specific task, or explanation if no tests apply)
- sample logs or configuration snippets when behavior visible to users changes

## Configuration Notes
Publishing uses GitHub Packages credentials from `USERNAME` / `TOKEN` or Gradle properties `gpr.user` / `gpr.key`. Do not commit secrets or local publishing credentials.
