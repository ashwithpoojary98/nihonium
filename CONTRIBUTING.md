# Contributing to Nihonium

Thank you for considering a contribution! The following guidelines will help you get started.

## Code of Conduct

Please read and follow our [Code of Conduct](CODE_OF_CONDUCT.md). We are committed to a welcoming and respectful community.

## How to Contribute

### Reporting Bugs

Open an issue using the **Bug report** template. Include a minimal reproducible example whenever possible.

### Requesting Features

Open an issue using the **Feature request** template. Describe the motivation and the API you have in mind.

### Submitting a Pull Request

1. **Fork** the repository and create a branch from `main`:
   ```bash
   git checkout -b fix/my-bug-description
   ```

2. **Make your changes** following the coding standards below.

3. **Run the build** to ensure nothing is broken:
   ```bash
   mvn clean package -DskipTests
   ```

4. **Add or update tests** for any changed behaviour.

5. **Open a pull request** against `main` using the provided PR template.

## Coding Standards

- **No hardcoded values** — declare constants (`private static final`) for every magic number or string.
- **Use SLF4J** for all logging. Never use `System.out.println` or `System.err.println`.
- **CompletableFuture** — all CDP commands must return `CompletableFuture<JsonObject>`. Callers join at the boundary, not inside domain classes.
- **Javadoc** — all public methods must have Javadoc. Use `{@code}` for code references.
- **Java 21** — the minimum language level. Records, sealed classes, and text blocks are welcome where appropriate.
- **Apache License 2.0** header is not required in individual files, but must not be removed from existing files.

## Project Structure

```
src/
  main/java/io/github/ashwithpoojary98/
    browser/        # BrowserManager, BrowserLauncher, BrowserType, Platform
    cdp/            # CDPCommandManager + domain wrappers (BrowserDomain, DOMDomain, …)
    chrome/         # ChromeDriver, ChromeElement, ChromeOptions, ChromeWindow, …
    exception/      # CDPException, BrowserLaunchException, ElementNotFoundException
    network/        # NetworkMonitor
    wait/           # AutoWaitEngine, WaitConfig
    websocket/      # NihoniumWebSocketClient
    By.java         # Locator strategies
    WebDriver.java  # Public interface
    WebElement.java # Public interface
  test/java/…       # Integration tests (require a live browser)
```

## Running Tests

Tests are integration tests that launch a real browser. They are excluded from the default CI build for non-main branches.

```bash
# Run all tests (requires Chrome or auto-download enabled)
mvn test

# Skip tests (compile/package only)
mvn package -DskipTests
```

## Releasing

Releases are automated via the GitHub Actions workflow on every push to `main`:

1. The patch version is bumped in `pom.xml`.
2. A git tag `vX.Y.Z` is created.
3. The artifact is signed with GPG and published to Maven Central.

Manual releases are not needed.
