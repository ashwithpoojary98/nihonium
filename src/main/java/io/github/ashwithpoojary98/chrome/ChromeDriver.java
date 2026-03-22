package io.github.ashwithpoojary98.chrome;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.ashwithpoojary98.By;
import io.github.ashwithpoojary98.WebDriver;
import io.github.ashwithpoojary98.WebElement;
import io.github.ashwithpoojary98.browser.BrowserLauncher;
import io.github.ashwithpoojary98.browser.BrowserOptions;
import io.github.ashwithpoojary98.browser.LaunchResult;
import io.github.ashwithpoojary98.cdp.domain.BrowserDomain;
import io.github.ashwithpoojary98.cdp.domain.CSSDomain;
import io.github.ashwithpoojary98.cdp.domain.DOMDomain;
import io.github.ashwithpoojary98.cdp.domain.InputDomain;
import io.github.ashwithpoojary98.cdp.domain.NetworkDomain;
import io.github.ashwithpoojary98.cdp.domain.PageDomain;
import io.github.ashwithpoojary98.cdp.domain.RuntimeDomain;
import io.github.ashwithpoojary98.exception.BrowserLaunchException;
import io.github.ashwithpoojary98.network.NetworkMonitor;
import io.github.ashwithpoojary98.wait.WaitConfig;
import io.github.ashwithpoojary98.websocket.NihoniumWebSocketClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * CDP-backed implementation of {@link WebDriver}.
 *
 * <p>Communicates directly with Chrome/Chromium over the DevTools Protocol WebSocket
 * — no ChromeDriver binary required.
 *
 * <h3>Window-handle model</h3>
 * <p>Nihonium uses the CDP {@code targetId} as the Selenium window handle.  This is
 * a stable, unique string (e.g. {@code "3F1A2B3C4D5E6F7G"}) that corresponds to
 * exactly one browser tab or window.
 */
public class ChromeDriver implements WebDriver {

    // ── CDP target type filter ────────────────────────────────────────────────

    private static final String TARGET_TYPE_PAGE     = "page";
    private static final String TARGET_FIELD_TYPE    = "type";
    private static final String TARGET_FIELD_ID      = "targetId";
    private static final String TARGET_INFOS_FIELD   = "targetInfos";

    // ── WebSocket URL parsing ─────────────────────────────────────────────────

    /** The path segment that precedes the targetId in a CDP WebSocket URL. */
    private static final String WS_TARGET_PATH_PREFIX = "/devtools/page/";

    // ── CDP domains ───────────────────────────────────────────────────────────

    private final PageDomain    pageDomain;
    private final DOMDomain     domDomain;
    private final RuntimeDomain runtimeDomain;
    private final InputDomain   inputDomain;
    private final NetworkDomain networkDomain;
    private final CSSDomain     cssDomain;
    private final BrowserDomain browserDomain;

    // ── Infrastructure ────────────────────────────────────────────────────────

    private final BrowserLauncher         launcher;
    private final NihoniumWebSocketClient wsClient;
    private final WaitConfig              waitConfig;
    private final NetworkMonitor          networkMonitor;

    /** CDP target ID of the page this driver is connected to. */
    private final String currentTargetId;

    private volatile boolean closed = false;

    // ── Constructors ──────────────────────────────────────────────────────────

    public ChromeDriver() {
        this(new ChromeOptions(), WaitConfig.defaultConfig());
    }

    public ChromeDriver(ChromeOptions chromeOptions) {
        this(chromeOptions, WaitConfig.defaultConfig());
    }

    public ChromeDriver(ChromeOptions chromeOptions, WaitConfig waitConfig) {
        this.waitConfig = waitConfig;

        try {
            BrowserOptions options = BrowserOptions.builder()
                    .browserType(chromeOptions.getBrowserType())
                    .browserVersion(chromeOptions.getBrowserVersion())
                    .autoDownload(chromeOptions.isAutoDownload())
                    .binaryPath(chromeOptions.getBinaryPath())
                    .headless(chromeOptions.isHeadless())
                    .windowSize(chromeOptions.getWindowWidth(), chromeOptions.getWindowHeight())
                    .addArguments(chromeOptions.getArguments())
                    .build();

            launcher = new BrowserLauncher(options);
            LaunchResult launchResult = launcher.launch();

            // Extract the targetId from the WebSocket URL so window handles work
            this.currentTargetId = extractTargetId(launchResult.webSocketUrl());

            URI wsUri = new URI(launchResult.webSocketUrl());
            wsClient = new NihoniumWebSocketClient(wsUri);
            wsClient.connectBlocking();

            boolean connected = wsClient.awaitConnection(10, TimeUnit.SECONDS);
            if (!connected) {
                throw new BrowserLaunchException("Timed out waiting for CDP WebSocket connection");
            }

            pageDomain    = new PageDomain(wsClient);
            domDomain     = new DOMDomain(wsClient);
            runtimeDomain = new RuntimeDomain(wsClient);
            inputDomain   = new InputDomain(wsClient);
            networkDomain = new NetworkDomain(wsClient);
            cssDomain     = new CSSDomain(wsClient);
            browserDomain = new BrowserDomain(wsClient);

            networkMonitor = new NetworkMonitor(networkDomain);
            if (waitConfig.isWaitForNetworkIdle()) {
                networkMonitor.enable();
            }

            pageDomain.enable().join();
            domDomain.enable().join();
            runtimeDomain.enable().join();

        } catch (BrowserLaunchException e) {
            throw e;
        } catch (Exception e) {
            cleanup();
            throw new BrowserLaunchException("Failed to initialise ChromeDriver", e);
        }
    }

    // ── WebDriver — navigation ────────────────────────────────────────────────

    @Override
    public void get(String url) {
        try {
            pageDomain.navigate(url).join();
        } catch (Exception e) {
            throw new RuntimeException("Failed to navigate to: " + url, e);
        }
    }

    @Override
    public String getCurrentUrl() {
        try {
            JsonObject result = runtimeDomain.evaluate("window.location.href", false).join();
            return result.getAsJsonObject("result").get("value").getAsString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get current URL", e);
        }
    }

    @Override
    public String getTitle() {
        try {
            JsonObject result = runtimeDomain.evaluate("document.title", false).join();
            return result.getAsJsonObject("result").get("value").getAsString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get page title", e);
        }
    }

    @Override
    public String getPageSource() {
        try {
            JsonObject result =
                    runtimeDomain.evaluate("document.documentElement.outerHTML", false).join();
            return result.getAsJsonObject("result").get("value").getAsString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get page source", e);
        }
    }

    // ── WebDriver — element finding ───────────────────────────────────────────

    @Override
    public WebElement findElement(By by) {
        return new ChromeElement(by, domDomain, runtimeDomain,
                inputDomain, cssDomain, waitConfig, networkMonitor);
    }

    @Override
    public List<WebElement> findElements(By by) {
        List<WebElement> elements = new ArrayList<>();
        elements.add(findElement(by));
        return elements;
    }

    // ── WebDriver — window handles ────────────────────────────────────────────

    /**
     * Returns the CDP target IDs of all open {@code page} targets.
     * Each ID can be passed to {@link TargetLocator#window(String)}.
     *
     * @return set of target ID strings (window handles)
     */
    @Override
    public Set<String> getWindowHandles() {
        try {
            JsonObject response = browserDomain.getTargets().join();
            JsonArray  targets  = response.getAsJsonArray(TARGET_INFOS_FIELD);
            Set<String> handles = new HashSet<>();
            for (JsonElement el : targets) {
                JsonObject target = el.getAsJsonObject();
                if (TARGET_TYPE_PAGE.equals(target.get(TARGET_FIELD_TYPE).getAsString())) {
                    handles.add(target.get(TARGET_FIELD_ID).getAsString());
                }
            }
            return handles;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get window handles", e);
        }
    }

    /**
     * Returns the CDP target ID of the page this driver is currently connected to.
     *
     * @return target ID string (window handle)
     */
    @Override
    public String getWindowHandle() {
        return currentTargetId;
    }

    // ── WebDriver — lifecycle ─────────────────────────────────────────────────

    @Override
    public void close() {
        cleanup();
    }

    @Override
    public void quit() {
        cleanup();
    }

    // ── WebDriver — sub-interfaces ────────────────────────────────────────────

    @Override
    public TargetLocator switchTo() {
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

    // ── Package-visible accessors (used by helper classes) ────────────────────

    PageDomain    getPageDomain()    { return pageDomain; }
    BrowserDomain getBrowserDomain() { return browserDomain; }
    RuntimeDomain getRuntimeDomain() { return runtimeDomain; }
    DOMDomain     getDomDomain()     { return domDomain; }
    NetworkDomain getNetworkDomain() { return networkDomain; }

    /** Returns the CDP target ID of the currently connected page. */
    String getCurrentTargetId() { return currentTargetId; }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Extracts the CDP target ID from the WebSocket debugger URL.
     *
     * <p>The URL format is {@code ws://localhost:PORT/devtools/page/TARGET_ID}.
     *
     * @param webSocketUrl WebSocket URL returned by the CDP JSON endpoint
     * @return the target ID segment
     * @throws IllegalArgumentException if the URL does not contain the expected path
     */
    private static String extractTargetId(String webSocketUrl) {
        URI uri  = URI.create(webSocketUrl);
        String path = uri.getPath();
        int idx = path.lastIndexOf(WS_TARGET_PATH_PREFIX);
        if (idx < 0) {
            throw new IllegalArgumentException(
                    "Cannot extract targetId from WebSocket URL: " + webSocketUrl);
        }
        return path.substring(idx + WS_TARGET_PATH_PREFIX.length());
    }

    private void cleanup() {
        if (closed) return;
        closed = true;

        try {
            if (wsClient != null && wsClient.isOpen()) {
                wsClient.close();
            }
        } catch (Exception ignored) { }

        try {
            if (launcher != null) {
                launcher.shutdown();
            }
        } catch (Exception ignored) { }
    }
}
