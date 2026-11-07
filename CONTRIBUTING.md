# Contributing

Thank you for your interest in contributing!

## Development Setup

### Prerequisites

- JDK 17+ (21 recommended)
- Gradle 8.9+ (via included wrapper)

### Installation

```bash
git clone https://github.com/SovaXYZ/platform-cofhe-kotlin-sdk.git
cd platform-cofhe-kotlin-sdk
./gradlew :sdk:build
```

### Running Tests

```bash
./gradlew :sdk:test                # Unit tests (MockEngine)
./gradlew :sdk:integrationTest     # Integration tests (WireMock)
./gradlew :sdk:check               # All checks (unit + integration)
./gradlew :sdk:e2eTest             # E2E tests (requires COFHE_BASE_URL)
```

## Code Style

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use `kotlinx.serialization` for JSON (not Gson/Jackson)
- Prefer `suspend` functions for async operations
- Use sealed classes for error hierarchies
- Minimal comments — code should be self-documenting

## Commit Messages

We use [Conventional Commits](https://www.conventionalcommits.org/):

- `feat:` — New feature
- `fix:` — Bug fix
- `docs:` — Documentation
- `test:` — Tests
- `refactor:` — Code refactoring
- `chore:` — Build/config changes

## Pull Request Process

1. Fork the repository
2. Create feature branch (`git checkout -b feat/amazing-feature`)
3. Commit changes (`git commit -m 'feat: add amazing feature'`)
4. Push to branch (`git push origin feat/amazing-feature`)
5. Open Pull Request

### PR Checklist

- [ ] Tests pass (`./gradlew :sdk:check`)
- [ ] No new warnings
- [ ] Documentation updated if needed
- [ ] Follows existing code patterns

## Questions?

Open an issue or start a discussion.
