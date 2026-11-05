# ADR 0003: Pin Gradle through the wrapper

- Status: Accepted
- Decision: Keep the Gradle Wrapper in `apps/android`, pinned to Gradle 8.14.3. Local and CI builds use `./gradlew`.
- Rationale: Depending on a local Gradle installation allows versions to drift between development and CI. Gradle 9.x has already caused a configuration error for `:core` in `settings.gradle.kts`.
- Consequences: Everyone builds with the same version after cloning. Upgrade with `./gradlew wrapper --gradle-version <version>` to update `gradle-wrapper.properties`. Set `distributionSha256Sum` to verify downloads.
