package io.github.ashwith.cdp.domain;

import com.google.gson.JsonObject;
import io.github.ashwith.websocket.NihoniumWebSocketClient;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class NetworkDomain {

    private final NihoniumWebSocketClient wsClient;

    public NetworkDomain(NihoniumWebSocketClient wsClient) {
        this.wsClient = wsClient;
    }

    /**
     * Enables network tracking.
     *
     * @return CompletableFuture that completes when network tracking is enabled
     */
    public CompletableFuture<JsonObject> enable() {
        return wsClient.sendCommand("Network.enable");
    }

    /**
     * Disables network tracking.
     *
     * @return CompletableFuture that completes when network tracking is disabled
     */
    public CompletableFuture<JsonObject> disable() {
        return wsClient.sendCommand("Network.disable");
    }

    /**
     * Sets user agent override.
     *
     * @param userAgent User agent string
     * @return CompletableFuture that completes when user agent is set
     */
    public CompletableFuture<JsonObject> setUserAgentOverride(String userAgent) {
        JsonObject params = new JsonObject();
        params.addProperty("userAgent", userAgent);
        return wsClient.sendCommand("Network.setUserAgentOverride", params);
    }

    /**
     * Toggles cache disabling.
     *
     * @param cacheDisabled Whether to disable cache
     * @return CompletableFuture that completes when cache setting is applied
     */
    public CompletableFuture<JsonObject> setCacheDisabled(boolean cacheDisabled) {
        JsonObject params = new JsonObject();
        params.addProperty("cacheDisabled", cacheDisabled);
        return wsClient.sendCommand("Network.setCacheDisabled", params);
    }

    /**
     * Sets extra HTTP headers.
     *
     * @param headers Headers as JSON object
     * @return CompletableFuture that completes when headers are set
     */
    public CompletableFuture<JsonObject> setExtraHTTPHeaders(JsonObject headers) {
        JsonObject params = new JsonObject();
        params.add("headers", headers);
        return wsClient.sendCommand("Network.setExtraHTTPHeaders", params);
    }

    /**
     * Gets response body for a request.
     *
     * @param requestId Request ID
     * @return CompletableFuture with response body
     */
    public CompletableFuture<JsonObject> getResponseBody(String requestId) {
        JsonObject params = new JsonObject();
        params.addProperty("requestId", requestId);
        return wsClient.sendCommand("Network.getResponseBody", params);
    }

    public void subscribeToRequestWillBeSent(Consumer<JsonObject> handler) {
        wsClient.subscribeToEvent("Network.requestWillBeSent", handler);
    }

    public void subscribeToLoadingFinished(Consumer<JsonObject> handler) {
        wsClient.subscribeToEvent("Network.loadingFinished", handler);
    }

    public void subscribeToLoadingFailed(Consumer<JsonObject> handler) {
        wsClient.subscribeToEvent("Network.loadingFailed", handler);
    }
}
