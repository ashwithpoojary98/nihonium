package io.github.ashwith.wait;

public class WaitConfig {
    private final long timeoutMillis;
    private final long pollingIntervalMillis;
    private final boolean waitForVisibility;
    private final boolean waitForClickability;
    private final boolean waitForNetworkIdle;
    private final boolean waitForAnimations;
    private final int networkIdleMaxConnections;
    private final long networkIdleDurationMillis;

    private WaitConfig(Builder builder) {
        this.timeoutMillis = builder.timeoutMillis;
        this.pollingIntervalMillis = builder.pollingIntervalMillis;
        this.waitForVisibility = builder.waitForVisibility;
        this.waitForClickability = builder.waitForClickability;
        this.waitForNetworkIdle = builder.waitForNetworkIdle;
        this.waitForAnimations = builder.waitForAnimations;
        this.networkIdleMaxConnections = builder.networkIdleMaxConnections;
        this.networkIdleDurationMillis = builder.networkIdleDurationMillis;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static WaitConfig defaultConfig() {
        return builder().build();
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public long getPollingIntervalMillis() {
        return pollingIntervalMillis;
    }

    public boolean isWaitForVisibility() {
        return waitForVisibility;
    }

    public boolean isWaitForClickability() {
        return waitForClickability;
    }

    public boolean isWaitForNetworkIdle() {
        return waitForNetworkIdle;
    }

    public boolean isWaitForAnimations() {
        return waitForAnimations;
    }

    public int getNetworkIdleMaxConnections() {
        return networkIdleMaxConnections;
    }

    public long getNetworkIdleDurationMillis() {
        return networkIdleDurationMillis;
    }

    public static class Builder {
        private long timeoutMillis = 10000;
        private long pollingIntervalMillis = 100;
        private boolean waitForVisibility = true;
        private boolean waitForClickability = true;
        private boolean waitForNetworkIdle = false;
        private boolean waitForAnimations = false;
        private int networkIdleMaxConnections = 0;
        private long networkIdleDurationMillis = 500;

        public Builder timeout(long millis) {
            this.timeoutMillis = millis;
            return this;
        }

        public Builder pollingInterval(long millis) {
            this.pollingIntervalMillis = millis;
            return this;
        }

        public Builder waitForVisibility(boolean wait) {
            this.waitForVisibility = wait;
            return this;
        }

        public Builder waitForClickability(boolean wait) {
            this.waitForClickability = wait;
            return this;
        }

        public Builder waitForNetworkIdle(boolean wait) {
            this.waitForNetworkIdle = wait;
            return this;
        }

        public Builder waitForAnimations(boolean wait) {
            this.waitForAnimations = wait;
            return this;
        }

        public Builder networkIdleMaxConnections(int maxConnections) {
            this.networkIdleMaxConnections = maxConnections;
            return this;
        }

        public Builder networkIdleDuration(long millis) {
            this.networkIdleDurationMillis = millis;
            return this;
        }

        public WaitConfig build() {
            return new WaitConfig(this);
        }
    }
}
