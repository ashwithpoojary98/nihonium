package io.github.ashwith.wait;

import io.github.ashwith.By;
import io.github.ashwith.exception.TimeoutException;
import io.github.ashwith.network.NetworkMonitor;

import java.util.function.Supplier;

public class AutoWaitEngine {

    private final ElementWaitConditions conditions;
    private final WaitConfig config;
    private final NetworkMonitor networkMonitor;

    public AutoWaitEngine(ElementWaitConditions conditions, WaitConfig config, NetworkMonitor networkMonitor) {
        this.conditions = conditions;
        this.config = config;
        this.networkMonitor = networkMonitor;
    }

    public void waitForElement(By locator) {
        waitForCondition(
                () -> conditions.isPresent(locator),
                "Element not found: " + locator
        );
    }

    public void waitForElementVisible(By locator) {
        waitForElement(locator);

        if (config.isWaitForVisibility()) {
            waitForCondition(
                    () -> conditions.isVisible(locator),
                    "Element not visible: " + locator
            );
        }
    }

    public void waitForElementClickable(By locator) {
        waitForElementVisible(locator);

        if (config.isWaitForClickability()) {
            waitForCondition(
                    () -> conditions.isClickable(locator),
                    "Element not clickable: " + locator
            );
        }

        if (config.isWaitForNetworkIdle()) {
            waitForNetworkIdle();
        }
    }

    public void waitForElementInteractable(By locator) {
        waitForElementVisible(locator);

        if (config.isWaitForClickability()) {
            waitForCondition(
                    () -> conditions.isEditable(locator),
                    "Element not editable: " + locator
            );
        }

        if (config.isWaitForNetworkIdle()) {
            waitForNetworkIdle();
        }
    }

    public void waitForNetworkIdle() {
        if (networkMonitor != null) {
            waitForCondition(
                    () -> networkMonitor.isNetworkIdle(
                            config.getNetworkIdleMaxConnections(),
                            config.getNetworkIdleDurationMillis()
                    ),
                    "Network not idle after " + config.getTimeoutMillis() + "ms"
            );
        }
    }

    private void waitForCondition(Supplier<Boolean> condition, String timeoutMessage) {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + config.getTimeoutMillis();

        while (System.currentTimeMillis() < endTime) {
            try {
                if (condition.get()) {
                    return;
                }
            } catch (Exception e) {
            }

            long pollDeadline = System.currentTimeMillis() + config.getPollingIntervalMillis();
            while (System.currentTimeMillis() < pollDeadline) {
                if (Thread.interrupted()) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Wait interrupted");
                }
            }
        }

        throw new TimeoutException(timeoutMessage + " after " + config.getTimeoutMillis() + "ms");
    }
}
