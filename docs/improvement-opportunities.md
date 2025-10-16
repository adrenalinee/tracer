# Modernization Opportunities

## Dependency Baseline
- The root `build.gradle.kts` pins Kotlin to `1.7.22` and imports the Spring Boot `3.0.5` and Spring Cloud `2022.0.2` BOMs, which are now out of general support. Upgrading to Kotlin 1.9+, Spring Boot 3.2+, and the matching Spring Cloud release would unlock the latest bug fixes, CVE patches, and GraalVM/native-image improvements while keeping the project within the currently supported Spring ecosystem.

## Concurrency and Robustness in Registries
- `TraceDataCreatorRegistry` stores protocol-specific creators inside a shared `mutableMapOf` without any synchronization. Because the registry is effectively a process-wide singleton, concurrent registrations or lookups can race under high load. Switching to thread-safe collections (e.g., `ConcurrentHashMap`) or applying explicit synchronization would harden runtime behavior.
- The same file still carries a `TODO` note about handling `null` protocols when looking up creators, so addressing that edge case will prevent `null` pointers and give clearer fallbacks for protocol-agnostic creators.

## Request/Response Body Guardrails
- Both the WebMVC `TracerFilter` and WebFlux `TracerWebFilter` include TODO comments about reading entire request bodies when the controller hasn’t consumed them yet. Implementing configurable size limits or streaming safeguards before buffering the body will prevent excessive memory use and protect the tracer from very large payloads.
