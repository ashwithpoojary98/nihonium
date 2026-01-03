package io.github.ashwith.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.ashwith.cdp.CDPCommandManager;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * WebSocket client for Chrome DevTools Protocol (CDP) communication.
 *
 * This class manages the WebSocket connection to Chrome/Chromium and handles
 * sending CDP commands and receiving responses/events.
 */
public class NihoniumWebSocketClient extends WebSocketClient {

    private final CDPCommandManager commandManager;
    private final Gson gson;
    private final CountDownLatch connectionLatch;
    private volatile boolean connected;
    private volatile Exception connectionError;

    /**
     * Creates a new NihoniumWebSocketClient.
     *
     * @param serverUri The WebSocket URI (e.g., ws://localhost:9222/devtools/page/...)
     */
    public NihoniumWebSocketClient(URI serverUri) {
        super(serverUri);
        this.commandManager = new CDPCommandManager();
        this.gson = new Gson();
        this.connectionLatch = new CountDownLatch(1);
        this.connected = false;
    }

    /**
     * Creates a new NihoniumWebSocketClient with custom CDPCommandManager.
     *
     * @param serverUri The WebSocket URI
     * @param commandManager Custom CDPCommandManager instance
     */
    public NihoniumWebSocketClient(URI serverUri, CDPCommandManager commandManager) {
        super(serverUri);
        this.commandManager = commandManager;
        this.gson = new Gson();
        this.connectionLatch = new CountDownLatch(1);
        this.connected = false;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        connected = true;
        connectionLatch.countDown();
        System.out.println("CDP WebSocket connection established");
    }

    @Override
    public void onMessage(String message) {
        try {
            JsonObject jsonMessage = gson.fromJson(message, JsonObject.class);
            commandManager.handleMessage(jsonMessage);
        } catch (Exception e) {
            System.err.println("Error handling CDP message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        connected = false;
        String initiator = remote ? "remote" : "local";
        System.out.println("CDP WebSocket connection closed by " + initiator + ". Code: " + code + ", Reason: " + reason);

        // Clear all pending commands
        commandManager.clear();
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("CDP WebSocket error: " + ex.getMessage());
        ex.printStackTrace();
        connectionError = ex;
        connectionLatch.countDown();
    }

    /**
     * Waits for the WebSocket connection to be established.
     *
     * @param timeout Timeout duration
     * @param unit Time unit
     * @return true if connected, false if timeout
     * @throws InterruptedException if the wait is interrupted
     */
    public boolean awaitConnection(long timeout, TimeUnit unit) throws InterruptedException {
        boolean result = connectionLatch.await(timeout, unit);
        if (connectionError != null) {
            throw new RuntimeException("Failed to connect to CDP", connectionError);
        }
        return result && connected;
    }

    /**
     * Sends a CDP command and returns a CompletableFuture with the result.
     *
     * @param method The CDP method name (e.g., "Page.navigate")
     * @param params The command parameters
     * @return CompletableFuture that completes with the response
     */
    public CompletableFuture<JsonObject> sendCommand(String method, JsonObject params) {
        if (!connected) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("WebSocket is not connected")
            );
        }

        long commandId = commandManager.nextCommandId();
        CompletableFuture<JsonObject> future = commandManager.registerCommand(commandId);

        JsonObject command = new JsonObject();
        command.addProperty("id", commandId);
        command.addProperty("method", method);
        if (params != null) {
            command.add("params", params);
        }

        String commandJson = gson.toJson(command);
        send(commandJson);

        return future;
    }

    /**
     * Sends a CDP command without parameters.
     *
     * @param method The CDP method name
     * @return CompletableFuture that completes with the response
     */
    public CompletableFuture<JsonObject> sendCommand(String method) {
        return sendCommand(method, null);
    }

    /**
     * Subscribes to a CDP event.
     *
     * @param eventName The event name (e.g., "Page.frameNavigated")
     * @param handler The handler to be called when the event occurs
     */
    public void subscribeToEvent(String eventName, Consumer<JsonObject> handler) {
        commandManager.subscribe(eventName, handler);
    }

    /**
     * Unsubscribes from a CDP event.
     *
     * @param eventName The event name
     * @param handler The handler to remove
     */
    public void unsubscribeFromEvent(String eventName, Consumer<JsonObject> handler) {
        commandManager.unsubscribe(eventName, handler);
    }

    /**
     * Unsubscribes all handlers for a specific event.
     *
     * @param eventName The event name
     */
    public void unsubscribeAllFromEvent(String eventName) {
        commandManager.unsubscribeAll(eventName);
    }

    /**
     * Checks if the WebSocket is currently connected.
     *
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return connected && !isClosed();
    }

    /**
     * Gets the CDPCommandManager instance.
     *
     * @return The CDPCommandManager
     */
    public CDPCommandManager getCommandManager() {
        return commandManager;
    }

    /**
     * Closes the WebSocket connection and cleans up resources.
     */
    @Override
    public void close() {
        commandManager.clear();
        super.close();
    }
}
