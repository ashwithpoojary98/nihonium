package io.github.ashwith.chrome;

import io.github.ashwith.browser.BrowserLauncher;
import io.github.ashwith.browser.BrowserOptions;
import io.github.ashwith.browser.LaunchResult;
import io.github.ashwith.exception.BrowserLaunchException;

import java.io.IOException;

public class ChromeDriverService implements IDriverService {

    private BrowserLauncher launcher;
    private LaunchResult launchResult;


    @Override
    public synchronized void start() {
        if (isRunning()) {
            return;
        }

        BrowserOptions options = BrowserOptions.builder()
                .headless(false)
                .windowSize(1280, 720)
                .build();

        launcher = new BrowserLauncher(options);
        try {
            launchResult = launcher.launch();
        } catch (IOException e) {
            launcher = null;
            throw new BrowserLaunchException("Failed to start ChromeDriverService", e);
        }
    }

    @Override
    public synchronized void stop() {
        if (launcher != null) {
            try {
                launcher.shutdown();
            } finally {
                launcher = null;
                launchResult = null;
            }
        }
    }

    @Override
    public synchronized boolean isRunning() {
        return launcher != null && launcher.isRunning();
    }

    @Override
    public synchronized String getUrl() {
        if (launchResult == null) {
            return "";
        }
        String url = launchResult.webSocketUrl();
        return url == null ? "" : url;
    }
}
