# Contributing to Sentinel AI Surveillance System

Thank you for your interest in contributing to Sentinel! This document provides guidelines and instructions for contributors.

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Making Changes](#making-changes)
- [Pull Request Process](#pull-request-process)
- [Coding Standards](#coding-standards)
- [Testing Guidelines](#testing-guidelines)

---

## Code of Conduct

This project adheres to professional standards. Please be respectful and constructive in all interactions.

---

## Getting Started

### Prerequisites

Before contributing, ensure you have:

- **Java JDK 21** or later
- **Maven 3.8+**
- **Docker** (for PostgreSQL and Testcontainers)
- **VLC Media Player** (64-bit) for video features
- **Git** for version control

### Fork & Clone

1. Fork the repository on GitHub
2. Clone your fork:
   ```bash
   git clone https://github.com/YOUR_USERNAME/Sentinel-AI-Surveillance-System.git
   cd Sentinel-AI-Surveillance-System
   ```
3. Add upstream remote:
   ```bash
   git remote add upstream https://github.com/ORIGINAL_OWNER/Sentinel-AI-Surveillance-System.git
   ```

---

## Development Setup

### 1. Database Setup

Start PostgreSQL with Docker:
```bash
docker run --name sentinel-db -p 5432:5432 \
  -e POSTGRES_PASSWORD=password \
  -e POSTGRES_DB=sentinel_db \
  -d postgres:15
```

### 2. Build the Project

```bash
mvn clean install
```

### 3. Run Tests

```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=AlertEngineTest

# Integration tests only
mvn test -Dtest=*IntegrationTest
```

### 4. Run the Application

```bash
mvn spring-boot:run
```

---

## Making Changes

### Branch Naming

Use descriptive branch names:
- `feature/add-motion-detection`
- `bugfix/fix-video-buffer-leak`
- `docs/update-api-documentation`
- `refactor/improve-alert-engine`

### Commit Messages

Follow conventional commits:
```
type(scope): description

[optional body]

[optional footer]
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `refactor`: Code refactoring
- `test`: Adding tests
- `chore`: Maintenance

**Examples:**
```
feat(alert): add severity-based alert filtering

fix(video): resolve buffer overflow in RTSP stream

docs(readme): update installation instructions

test(repository): add integration tests for JSONB queries
```

---

## Pull Request Process

1. **Update your fork:**
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```

2. **Create a feature branch:**
   ```bash
   git checkout -b feature/your-feature-name
   ```

3. **Make changes and test:**
   ```bash
   mvn clean test
   ```

4. **Push and create PR:**
   ```bash
   git push origin feature/your-feature-name
   ```

5. **PR Requirements:**
   - All tests pass
   - Code follows project style
   - Documentation updated (if applicable)
   - Clear description of changes

---

## Coding Standards

### Java Style

- **Formatting:** Use IDE formatter (IntelliJ/Eclipse defaults)
- **Naming:** 
  - Classes: `PascalCase`
  - Methods/Variables: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
- **Annotations:** Use Lombok where appropriate (`@Slf4j`, `@RequiredArgsConstructor`)

### Architecture Principles

1. **Fail-Safe Design:**
   ```java
   public void process(Input input) {
       // Always validate input
       if (input == null) {
           LOGGER.warn("Received null input");
           return;
       }
       
       try {
           // Main logic
       } catch (Exception e) {
           // Error isolation - don't propagate
           LOGGER.error("Processing failed", e);
       }
   }
   ```

2. **Thread Safety:**
   - Use `AtomicReference`/`AtomicLong` for shared state
   - Use `@Transactional` for database operations
   - Use `Platform.runLater()` for JavaFX updates

3. **Dependency Injection:**
   - Constructor injection preferred
   - Use `@RequiredArgsConstructor` with final fields

### Documentation

- Add Javadoc for public methods
- Include `@param`, `@return`, `@throws` where applicable
- Document complex algorithms

---

## Testing Guidelines

### Test Structure

```java
@Test
void testMethodName_givenCondition_expectedBehavior() {
    // Given
    Input input = createTestInput();
    
    // When
    Result result = service.process(input);
    
    // Then
    assertThat(result).isNotNull();
    assertThat(result.getValue()).isEqualTo(expected);
}
```

### Test Categories

| Type | Framework | Purpose |
|------|-----------|---------|
| Unit | JUnit 5 + Mockito | Isolated component testing |
| Integration | Testcontainers | Real database testing |
| E2E | Spring Boot Test | Full pipeline validation |

### Required Tests

- New features must include unit tests
- Database changes require integration tests
- Critical paths need E2E coverage

---

## Questions?

If you have questions, please:

1. Check existing documentation
2. Search existing issues
3. Open a new issue with the `question` label

---

Thank you for contributing to Sentinel! 🚀
