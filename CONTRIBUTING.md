# Contributing to eyeOSurveillance System

Thank you for your interest in contributing to the eyeOSurveillance System! We welcome contributions from the community.

## How to Contribute

### Reporting Bugs

If you find a bug, please create an issue with:
- A clear, descriptive title
- Steps to reproduce the issue
- Expected vs actual behavior
- Your environment (OS, Java version, etc.)
- Screenshots if applicable

### Suggesting Enhancements

We love to hear your ideas! Please create an issue with:
- A clear description of the enhancement
- Why this would be useful
- Any implementation ideas you might have

### Pull Requests

1. **Fork the repository** and create your branch from `main`
2. **Follow the coding style** used in the project
3. **Write clear commit messages** that describe your changes
4. **Add tests** for any new functionality
5. **Update documentation** as needed
6. **Ensure all tests pass** before submitting

#### Development Setup

```bash
# Clone your fork
git clone https://github.com/YOUR_USERNAME/eyeOSurveillance-System.git
cd eyeOSurveillance-System

# Install dependencies
mvn clean install

# Run tests
mvn test
```

#### Code Style Guidelines

- Follow standard Java conventions
- Use meaningful variable and method names
- Add comments for complex logic
- Keep methods focused and concise
- Write unit tests for new features

#### Commit Message Format

```
<type>: <subject>

<body>

<footer>
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

Example:
```
feat: add support for RTSP stream authentication

Added username/password parameters to RTSP configuration
to support authenticated camera streams.

Closes #123
```

### Testing

Before submitting a pull request:

```bash
# Run all tests
mvn test

# Build the project
mvn clean package
```

### Code Review Process

1. Maintainers will review your PR
2. Address any requested changes
3. Once approved, your PR will be merged

## Code of Conduct

Please note that this project is released with a [Code of Conduct](CODE_OF_CONDUCT.md). By participating in this project you agree to abide by its terms.

## Questions?

Feel free to open an issue with your question, and we'll do our best to help!

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
