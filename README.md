# Nihonium

A lightweight browser automation framework using Chrome DevTools Protocol with Playwright-like auto-wait capabilities.

## Features

- **Native CDP over WebSocket** - Direct Chrome DevTools Protocol communication
- **Playwright-like Auto-Wait** - Smart waiting for elements (visible, stable, not obscured, animations)
- **No Selenium Dependency** - Standalone lightweight framework
- **Production Ready** - No Thread.sleep(), proper resource management
- **Network Idle Detection** - Optional network monitoring
- **Chrome/Chromium Support** - Auto-detect or custom binary path

## Quick Start

```java
import io.github.ashwith.WebDriver;
import io.github.ashwith.WebElement;
import io.github.ashwith.By;
import io.github.ashwith.chrome.ChromeDriver;

public class Example {
    public static void main(String[] args) {
        WebDriver driver = new ChromeDriver();

        driver.get("https://example.com");

        WebElement heading = driver.findElement(By.tagName("h1"));
        System.out.println(heading.getText());

        driver.quit();
    }
}
```

## Advanced Configuration

```java
import io.github.ashwith.chrome.ChromeOptions;
import io.github.ashwith.wait.WaitConfig;

ChromeOptions options = new ChromeOptions()
    .setHeadless(true)
    .setWindowSize(1920, 1080);

WaitConfig waitConfig = WaitConfig.builder()
    .timeout(15000)
    .waitForVisibility(true)
    .waitForClickability(true)
    .waitForAnimations(true)
    .waitForNetworkIdle(true)
    .build();

WebDriver driver = new ChromeDriver(options, waitConfig);
```

## Auto-Wait Features

Nihonium automatically waits for elements to be:

1. **Attached** - Present in DOM
2. **Visible** - Has dimensions, not hidden
3. **Stable** - No running animations/transitions
4. **Not Obscured** - Not covered by other elements
5. **Enabled** - Not disabled
6. **Editable** - For inputs (not readonly)

## Requirements

- Java 21+
- Chrome or Chromium browser installed

## License

Apache License 2.0
