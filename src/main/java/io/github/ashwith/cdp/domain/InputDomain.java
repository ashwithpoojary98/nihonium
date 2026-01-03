package io.github.ashwith.cdp.domain;

import com.google.gson.JsonObject;
import io.github.ashwith.websocket.NihoniumWebSocketClient;

import java.util.concurrent.CompletableFuture;

/**
 * Wrapper for the CDP Input domain.
 * Provides methods for simulating mouse and keyboard input.
 */
public class InputDomain {

    private final NihoniumWebSocketClient wsClient;

    public InputDomain(NihoniumWebSocketClient wsClient) {
        this.wsClient = wsClient;
    }

    /**
     * Dispatches a mouse event.
     *
     * @param type Event type (mousePressed, mouseReleased, mouseMoved, mouseWheel)
     * @param x X coordinate
     * @param y Y coordinate
     * @param button Mouse button (left, right, middle, back, forward)
     * @param clickCount Click count (for mousePressed/mouseReleased)
     * @return CompletableFuture that completes when event is dispatched
     */
    public CompletableFuture<JsonObject> dispatchMouseEvent(String type, double x, double y, String button, int clickCount) {
        JsonObject params = new JsonObject();
        params.addProperty("type", type);
        params.addProperty("x", x);
        params.addProperty("y", y);

        if (button != null) {
            params.addProperty("button", button);
        }

        if (clickCount > 0) {
            params.addProperty("clickCount", clickCount);
        }

        return wsClient.sendCommand("Input.dispatchMouseEvent", params);
    }

    /**
     * Dispatches a mouse move event.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return CompletableFuture that completes when event is dispatched
     */
    public CompletableFuture<JsonObject> mouseMove(double x, double y) {
        return dispatchMouseEvent("mouseMoved", x, y, null, 0);
    }

    /**
     * Dispatches a mouse click event (press + release).
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param button Mouse button
     * @return CompletableFuture that completes when both events are dispatched
     */
    public CompletableFuture<JsonObject> click(double x, double y, String button) {
        return mouseMove(x, y)
            .thenCompose(v -> dispatchMouseEvent("mousePressed", x, y, button, 1))
            .thenCompose(v -> dispatchMouseEvent("mouseReleased", x, y, button, 1));
    }

    /**
     * Dispatches a left mouse click event.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return CompletableFuture that completes when click is dispatched
     */
    public CompletableFuture<JsonObject> click(double x, double y) {
        return click(x, y, "left");
    }

    /**
     * Dispatches a double click event.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return CompletableFuture that completes when double click is dispatched
     */
    public CompletableFuture<JsonObject> doubleClick(double x, double y) {
        return mouseMove(x, y)
            .thenCompose(v -> dispatchMouseEvent("mousePressed", x, y, "left", 1))
            .thenCompose(v -> dispatchMouseEvent("mouseReleased", x, y, "left", 1))
            .thenCompose(v -> dispatchMouseEvent("mousePressed", x, y, "left", 2))
            .thenCompose(v -> dispatchMouseEvent("mouseReleased", x, y, "left", 2));
    }

    /**
     * Dispatches a keyboard event.
     *
     * @param type Event type (keyDown, keyUp, char)
     * @param key Key value
     * @param code Key code
     * @param modifiers Modifier keys bitmask
     * @return CompletableFuture that completes when event is dispatched
     */
    public CompletableFuture<JsonObject> dispatchKeyEvent(String type, String key, String code, int modifiers) {
        JsonObject params = new JsonObject();
        params.addProperty("type", type);

        if (key != null) {
            params.addProperty("key", key);
        }

        if (code != null) {
            params.addProperty("code", code);
        }

        if (modifiers > 0) {
            params.addProperty("modifiers", modifiers);
        }

        return wsClient.sendCommand("Input.dispatchKeyEvent", params);
    }

    /**
     * Dispatches a key press (keyDown + keyUp).
     *
     * @param key Key value
     * @return CompletableFuture that completes when key press is dispatched
     */
    public CompletableFuture<JsonObject> pressKey(String key) {
        return dispatchKeyEvent("keyDown", key, null, 0)
            .thenCompose(v -> dispatchKeyEvent("keyUp", key, null, 0));
    }

    /**
     * Inserts text by simulating keypress events for each character.
     *
     * @param text Text to insert
     * @return CompletableFuture that completes when text is inserted
     */
    public CompletableFuture<JsonObject> insertText(String text) {
        JsonObject params = new JsonObject();
        params.addProperty("text", text);
        return wsClient.sendCommand("Input.insertText", params);
    }

    /**
     * Types text character by character.
     *
     * @param text Text to type
     * @return CompletableFuture that completes when text is typed
     */
    public CompletableFuture<Void> typeText(String text) {
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);

        for (char c : text.toCharArray()) {
            String key = String.valueOf(c);
            future = future.thenCompose(v -> dispatchKeyEvent("keyDown", key, null, 0))
                          .thenCompose(v -> dispatchKeyEvent("keyUp", key, null, 0))
                          .thenApply(v -> null);
        }

        return future;
    }
}
