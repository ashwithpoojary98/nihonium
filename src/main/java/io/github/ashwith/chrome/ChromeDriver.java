package io.github.ashwith.chrome;

import com.google.gson.JsonObject;
import io.github.ashwith.By;
import io.github.ashwith.WebDriver;
import io.github.ashwith.WebElement;
import io.github.ashwith.browser.BrowserLauncher;
import io.github.ashwith.browser.BrowserOptions;
import io.github.ashwith.browser.LaunchResult;
import io.github.ashwith.cdp.domain.BrowserDomain;
import io.github.ashwith.cdp.domain.CSSDomain;
import io.github.ashwith.cdp.domain.DOMDomain;
import io.github.ashwith.cdp.domain.InputDomain;
import io.github.ashwith.cdp.domain.NetworkDomain;
import io.github.ashwith.cdp.domain.PageDomain;
import io.github.ashwith.cdp.domain.RuntimeDomain;
import io.github.ashwith.exception.BrowserLaunchException;
import io.github.ashwith.network.NetworkMonitor;
import io.github.ashwith.wait.WaitConfig;
import io.github.ashwith.websocket.NihoniumWebSocketClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ChromeDriver implements WebDriver {
    private ChromeOptions chromeOptions;
    private final BrowserLauncher launcher;
    private final NihoniumWebSocketClient wsClient;
    private final WaitConfig waitConfig;
    private final NetworkMonitor networkMonitor;

    // CDP Domain APIs
    private final PageDomain pageDomain;
    private final DOMDomain domDomain;
    private final RuntimeDomain runtimeDomain;
    private final InputDomain inputDomain;
    private final NetworkDomain networkDomain;
    private final CSSDomain cssDomain;
    private final BrowserDomain browserDomain;

    private volatile boolean closed = false;
    private String currentFrameId;

    public ChromeDriver() {
        this(new ChromeOptions(), WaitConfig.defaultConfig());
    }

    public ChromeDriver(ChromeOptions chromeOptions) {
        this(chromeOptions, WaitConfig.defaultConfig());
    }

    public ChromeDriver(ChromeOptions chromeOptions, WaitConfig waitConfig) {
        this.chromeOptions = chromeOptions;
        this.waitConfig = waitConfig;

        try {
            BrowserOptions options = BrowserOptions.builder()
                    .headless(chromeOptions.isHeadless())
                    .windowSize(chromeOptions.getWindowWidth(), chromeOptions.getWindowHeight())
                    .build();
            launcher = new BrowserLauncher(options);
            LaunchResult launchResult = launcher.launch();

            URI wsUri = new URI(launchResult.webSocketUrl());
            wsClient = new NihoniumWebSocketClient(wsUri);
            wsClient.connectBlocking();

            boolean connected = wsClient.awaitConnection(10, TimeUnit.SECONDS);
            if (!connected) {
                throw new BrowserLaunchException("Failed to connect to CDP WebSocket");
            }

            pageDomain = new PageDomain(wsClient);
            domDomain = new DOMDomain(wsClient);
            runtimeDomain = new RuntimeDomain(wsClient);
            inputDomain = new InputDomain(wsClient);
            networkDomain = new NetworkDomain(wsClient);
            cssDomain = new CSSDomain(wsClient);
            browserDomain = new BrowserDomain(wsClient);

            networkMonitor = new NetworkMonitor(networkDomain);
            if (waitConfig.isWaitForNetworkIdle()) {
                networkMonitor.enable();
            }

            pageDomain.enable().join();
            domDomain.enable().join();
            runtimeDomain.enable().join();

        } catch (Exception e) {
            cleanup();
            throw new BrowserLaunchException("Failed to initialize ChromeDriver", e);
        }
    }

    @Override
    public void get(String url) {
        try {
            pageDomain.navigate(url).join();
        } catch (Exception e) {
            throw new RuntimeException("Failed to navigate to URL: " + url, e);
        }
    }

    @Override
    public String getCurrentUrl() {
        try {
            String script = "window.location.href";
            JsonObject result = runtimeDomain.evaluate(script, false).join();
            return result.getAsJsonObject("result").get("value").getAsString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get current URL", e);
        }
    }

    @Override
    public String getTitle() {
        try {
            String script = "document.title";
            JsonObject result = runtimeDomain.evaluate(script, false).join();
            return result.getAsJsonObject("result").get("value").getAsString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get page title", e);
        }
    }

    @Override
    public List<WebElement> findElements(By by) {
        List<WebElement> elements = new ArrayList<>();
        elements.add(new ChromeElement(by, domDomain, runtimeDomain, inputDomain, cssDomain, waitConfig, networkMonitor));
        return elements;
    }

    @Override
    public WebElement findElement(By by) {
        return new ChromeElement(by, domDomain, runtimeDomain, inputDomain, cssDomain, waitConfig, networkMonitor);
    }

    @Override
    public String getPageSource() {
        try {
            String script = "document.documentElement.outerHTML";
            JsonObject result = runtimeDomain.evaluate(script, false).join();
            return result.getAsJsonObject("result").get("value").getAsString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get page source", e);
        }
    }

    @Override
    public void close() {
        cleanup();
    }

    @Override
    public void quit() {
        cleanup();
    }

    @Override
    public Set<String> getWindowHandles() {
        return new HashSet<>();
    }

    @Override
    public String getWindowHandle() {
        return "main-window";
    }

    @Override
    public WebDriver.TargetLocator switchTo() {
        return new ChromeTargetLocator(this);
    }

    @Override
    public Navigation navigate() {
        return new ChromeNavigation(this);
    }

    @Override
    public Options manage() {
        return new ChromeManageOptions(this);
    }

    public PageDomain getPageDomain() {
        return pageDomain;
    }

    public BrowserDomain getBrowserDomain() {
        return browserDomain;
    }

    public RuntimeDomain getRuntimeDomain() {
        return runtimeDomain;
    }

    private void cleanup() {
        if (closed) return;
        closed = true;

        try {
            if (wsClient != null && wsClient.isOpen()) {
                wsClient.close();
            }
        } catch (Exception e) {
            // Ignore
        }

        try {
            if (launcher != null) {
                launcher.shutdown();
            }
        } catch (Exception e) {
            // Ignore
        }
    }
}
