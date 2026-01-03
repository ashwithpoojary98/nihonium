package io.github.ashwith.cdp.domain;

import com.google.gson.JsonObject;
import io.github.ashwith.websocket.NihoniumWebSocketClient;

import java.util.concurrent.CompletableFuture;

public class BrowserDomain {

    private final NihoniumWebSocketClient wsClient;

    public BrowserDomain(NihoniumWebSocketClient wsClient) {
        this.wsClient = wsClient;
    }

    public CompletableFuture<JsonObject> getWindowForTarget(String targetId) {
        JsonObject params = new JsonObject();
        params.addProperty("targetId", targetId);
        return wsClient.sendCommand("Browser.getWindowForTarget", params);
    }

    public CompletableFuture<JsonObject> setWindowBounds(int windowId, JsonObject bounds) {
        JsonObject params = new JsonObject();
        params.addProperty("windowId", windowId);
        params.add("bounds", bounds);
        return wsClient.sendCommand("Browser.setWindowBounds", params);
    }

    public CompletableFuture<JsonObject> getWindowBounds(int windowId) {
        JsonObject params = new JsonObject();
        params.addProperty("windowId", windowId);
        return wsClient.sendCommand("Browser.getWindowBounds", params);
    }

    public CompletableFuture<JsonObject> maximizeCurrentWindow() {
        return wsClient.sendCommand("Browser.getVersion")
                .thenCompose(version -> {
                    JsonObject bounds = new JsonObject();
                    bounds.addProperty("windowState", "maximized");
                    return setWindowBounds(1, bounds);
                });
    }

    public CompletableFuture<JsonObject> minimizeCurrentWindow() {
        return wsClient.sendCommand("Browser.getVersion")
                .thenCompose(version -> {
                    JsonObject bounds = new JsonObject();
                    bounds.addProperty("windowState", "minimized");
                    return setWindowBounds(1, bounds);
                });
    }

    public CompletableFuture<JsonObject> fullscreenCurrentWindow() {
        return wsClient.sendCommand("Browser.getVersion")
                .thenCompose(version -> {
                    JsonObject bounds = new JsonObject();
                    bounds.addProperty("windowState", "fullscreen");
                    return setWindowBounds(1, bounds);
                });
    }

    public CompletableFuture<JsonObject> openNewWindow() {
        JsonObject params = new JsonObject();
        params.addProperty("url", "about:blank");
        return wsClient.sendCommand("Target.createTarget", params);
    }

    public CompletableFuture<JsonObject> openNewTabInCurrentContext() {
        JsonObject params = new JsonObject();
        params.addProperty("url", "about:blank");
        return wsClient.sendCommand("Target.createTarget", params);
    }

    public CompletableFuture<JsonObject> getVersion() {
        return wsClient.sendCommand("Browser.getVersion");
    }
}
