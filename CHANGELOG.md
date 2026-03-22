# Changelog

All notable changes to Nihonium are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Nihonium uses [semantic versioning](https://semver.org/).

---

## [Unreleased]

### Added
- **Auto browser download** — `BrowserManager` fetches Chrome for Testing automatically on first run.
  Cached under `~/.cache/nihonium/{browser}/{platform}/{version}/` (mirrors Selenium Manager convention).
- `BrowserType` enum — selects Chrome or Chromium.
- `Platform` enum — detects current OS/arch and maps to CfT platform identifiers.
- `ChromeOptions.setBrowserType()`, `.setBrowserVersion()`, `.setAutoDownload()`.
- `NetworkDomain` cookie API — `getCookies`, `setCookie`, `deleteCookies`, `clearBrowserCookies`.
- `ChromeManageOptions` full cookie support — `addCookie`, `deleteCookieNamed`, `deleteCookie`, `deleteAllCookies`, `getCookies`, `getCookieNamed`.
- `ChromeTargetLocator.activeElement()` — resolves focused element via CDP and returns a `WebElement`.
- Window handle support — `getWindowHandle()` returns the CDP target ID; `getWindowHandles()` returns all open page targets.
- `ChromeWindow` — real window size and position via `Browser.getWindowForTarget`.

### Changed
- All `System.out.println` / `System.err.println` replaced with SLF4J logging.
- `slf4j-simple` dependency moved to `runtime` scope (optional) so consumers can choose their own implementation.
- `maven-surefire-plugin` added with JUnit 5 configuration so tests run correctly via `mvn test`.
- `AutoWaitEngine` — replaced CPU spin-wait with `Thread.sleep` polling.
- `BrowserLauncher` — replaced CPU spin-wait for WebSocket URL with `Thread.sleep` retry loop.
- `CDPCommandManager` — all `RuntimeException` paths replaced with `CDPException`; all error output uses SLF4J.
- `BrowserDomain` — removed hardcoded `windowId=1`; now resolves real window ID via `Browser.getWindowForTarget`.
- `By.ByLinkText` / `By.ByPartialLinkText` — fixed bug where raw link text was passed as XPath; now generates `//a[normalize-space(.)='text']` with proper single-quote escaping.
- `ChromeElement.clear()` — now uses JavaScript (fires `input`/`change` events; compatible with React/Vue/Angular).
- `WaitConfig` — all default values exposed as public constants.
- `NetworkMonitor.isNetworkIdle()` — no-arg overload now delegates to `WaitConfig` constants instead of hardcoded `500`.
- `ChromeOptions.setBinary()` / `getBinary()` deprecated in favour of `setBinaryPath()` / `getBinaryPath()`.
- `BrowserDomain` deprecated methods (`maximizeCurrentWindow()` etc.) replaced with target-aware equivalents.

### Removed
- `ChromeDriverService.java` — unused service wrapper (superseded by `BrowserLauncher`).
- `IDriverService.java` — unused interface.
- `ChromeOption.java` — legacy options class (superseded by `ChromeOptions`).

### Fixed
- `ChromeTargetLocator.frame(*)` and `parentFrame()` now throw `UnsupportedOperationException` with a clear message instead of silently doing nothing.
- Test package typo `chrome.nehonium` corrected to `chrome.nihonium`.
- Tests no longer call `driver.quit()` individually — cleanup is handled by `@AfterEach`.
- README Maven Central URL corrected (`ashwith` → `ashwithpoojary98`).

---

## [1.0.0] — 2025-01-01

### Added
- Initial release: CDP-native browser automation with Selenium-compatible API.
- `ChromeDriver`, `ChromeElement`, `ChromeOptions`, `ChromeNavigation`, `ChromeWindow`.
- `By` locator strategies: id, name, className, tagName, cssSelector, xpath, linkText, partialLinkText.
- Playwright-style auto-wait engine (`AutoWaitEngine`, `WaitConfig`).
- Network idle detection (`NetworkMonitor`).
- CDP domain wrappers: `DOMDomain`, `RuntimeDomain`, `PageDomain`, `InputDomain`, `NetworkDomain`, `BrowserDomain`, `CSSDomain`.
- Maven Central publishing via Sonatype Central Portal.
