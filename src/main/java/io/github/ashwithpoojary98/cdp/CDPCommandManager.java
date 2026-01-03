package io.github.ashwithpoojary98.cdp;

import com.google.gson.JsonObject;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Manages Chrome DevTools Protocol (CDP) command/response correlation and event routing.
 * <p>
 * This class is responsible for:
 * - Generating unique command IDs
 * - Correlating CDP responses with their requests
 * - Managing event subscriptions
 * - Handling timeouts for pending commands
 */
public class CDPCommandManager {

    private final AtomicLong commandIdGenerator = new AtomicLong(0);
    private final ConcurrentHashMap<Long, CompletableFuture<JsonObject>> pendingCommands = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<Consumer<JsonObject>>> eventSubscribers = new ConcurrentHashMap<>();
    private final long defaultTimeoutSeconds;

    /**
     * Creates a new CDPCommandManager with default timeout of 30 seconds.
     */
    public CDPCommandManager() {
        this(30);
    }

    /**
     * Creates a new CDPCommandManager with specified timeout.
     *
     * @param defaultTimeoutSeconds Default timeout for commands in seconds
     */
    public CDPCommandManager(long defaultTimeoutSeconds) {
        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
    }

    /**
     * Generates the next unique command ID.
     *
     * @return Next command ID
     */
    public long nextCommandId() {
        return commandIdGenerator.incrementAndGet();
    }

    /**
     * Registers a command and returns a CompletableFuture that will be completed
     * when the response is received.
     *
     * @param id Command ID
     * @return CompletableFuture that completes with the response
     */
    public CompletableFuture<JsonObject> registerCommand(long id) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        pendingCommands.put(id, future);

        future.orTimeout(defaultTimeoutSeconds, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    pendingCommands.remove(id);
                    throw new RuntimeException("CDP command timeout (ID: " + id + ")", ex);
                });

        return future;
    }

    /**
     * Handles a CDP response message by completing the corresponding future.
     *
     * @param message The response message from CDP
     */
    public void handleResponse(JsonObject message) {
        if (!message.has("id")) {
            return;
        }

        long id = message.get("id").getAsLong();
        CompletableFuture<JsonObject> future = pendingCommands.remove(id);

        if (future != null) {
            if (message.has("error")) {
                JsonObject error = message.getAsJsonObject("error");
                String errorMessage = error.has("message")
                        ? error.get("message").getAsString()
                        : "Unknown CDP error";
                int errorCode = error.has("code")
                        ? error.get("code").getAsInt()
                        : -1;

                future.completeExceptionally(
                        new RuntimeException("CDP Error (code: " + errorCode + "): " + errorMessage)
                );
            } else {
                JsonObject result = message.has("result")
                        ? message.getAsJsonObject("result")
                        : new JsonObject();
                future.complete(result);
            }
        }
    }

    /**
     * Handles a CDP event message by notifying all subscribers.
     *
     * @param message The event message from CDP
     */
    public void handleEvent(JsonObject message) {
        if (!message.has("method")) {
            return;
        }

        String method = message.get("method").getAsString();
        JsonObject params = message.has("params")
                ? message.getAsJsonObject("params")
                : new JsonObject();

        List<Consumer<JsonObject>> handlers = eventSubscribers.get(method);
        if (handlers != null) {
            handlers.forEach(handler ->
                    CompletableFuture.runAsync(() -> {
                        try {
                            handler.accept(params);
                        } catch (Exception e) {
                            System.err.println("Error in event handler for " + method + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    })
            );
        }
    }

    /**
     * Handles an incoming CDP message (either response or event).
     *
     * @param message The CDP message
     */
    public void handleMessage(JsonObject message) {
        if (message.has("id")) {
            handleResponse(message);
        } else if (message.has("method")) {
            handleEvent(message);
        }
    }

    /**
     * Subscribes to a CDP event.
     *
     * @param eventName The event name (e.g., "Page.frameNavigated")
     * @param handler   The handler to be called when the event occurs
     */
    public void subscribe(String eventName, Consumer<JsonObject> handler) {
        eventSubscribers.computeIfAbsent(eventName, k -> new CopyOnWriteArrayList<>())
                .add(handler);
    }

    /**
     * Unsubscribes from a CDP event.
     *
     * @param eventName The event name
     * @param handler   The handler to remove
     */
    public void unsubscribe(String eventName, Consumer<JsonObject> handler) {
        List<Consumer<JsonObject>> handlers = eventSubscribers.get(eventName);
        if (handlers != null) {
            handlers.remove(handler);
            if (handlers.isEmpty()) {
                eventSubscribers.remove(eventName);
            }
        }
    }

    /**
     * Unsubscribes all handlers for a specific event.
     *
     * @param eventName The event name
     */
    public void unsubscribeAll(String eventName) {
        eventSubscribers.remove(eventName);
    }

    /**
     * Clears all pending commands and event subscriptions.
     * This should be called when closing the connection.
     */
    public void clear() {
        pendingCommands.values().forEach(future ->
                future.completeExceptionally(new RuntimeException("Connection closed"))
        );
        pendingCommands.clear();
        eventSubscribers.clear();
    }

    /**
     * Gets the number of pending commands.
     *
     * @return Number of pending commands
     */
    public int getPendingCommandCount() {
        return pendingCommands.size();
    }

    /**
     * Gets the number of event subscriptions.
     *
     * @return Number of event subscriptions
     */
    public int getEventSubscriptionCount() {
        return eventSubscribers.size();
    }
}
