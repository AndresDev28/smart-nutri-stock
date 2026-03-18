# Smart Nutri-Stock: Code Review Rules

## General Standards
- **Architecture:** Strictly follow Clean Architecture (Domain, Data, Presentation).
- **Language:** Use Kotlin with Jetpack Compose.
- **Dependency Injection:** Use Hilt for all dependencies.
- **Persistence:** Use Room for local storage.

## Testing & Quality (Mandatory)
- **TDD:** Write unit tests for business logic BEFORE implementation.
- **Coverage:** Aim for 90% coverage in the Domain layer.
- **Commits:** Use Conventional Commits (feat:, fix:, docs:, ci:, refactor:).

## Hardware Optimization (Samsung XCover7)
- **UI:** Minimum tap targets of 48dp for one-handed use.
- **Performance:** Barcode scanning must respond in <2 seconds.
- **Background:** Minimize battery drain in WorkManager tasks.

## GitHub Workflow
- No direct pushes to main (use Pull Requests).
- Ensure all CI checks (Lint and Tests) pass before merging.
