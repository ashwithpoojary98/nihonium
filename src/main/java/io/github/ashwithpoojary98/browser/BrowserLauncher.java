package io.github.ashwithpoojary98.browser;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Manages the lifecycle of Chrome/Chromium browser processes.
 * <p>
 * This class is responsible for:
 * - Finding Chrome/Chromium installation
 * - Launching the browser with appropriate flags
 * - Extracting the WebSocket debugger URL
 * - Managing browser process lifecycle
 */
public class BrowserLauncher {

    private Process browserProcess;
    private final BrowserOptions options;
    private final Gson gson;

    /**
     * Creates a new BrowserLauncher with default options.
     */
    public BrowserLauncher() {
        this(BrowserOptions.builder().build());
    }

    /**
     * Creates a new BrowserLauncher with specified options.
     *
     * @param options Browser launch options
     */
    public BrowserLauncher(BrowserOptions options) {
        this.options = options;
        this.gson = new Gson();
    }

    /**
     * Launches the browser and returns the launch result.
     *
     * @return LaunchResult containing the process and WebSocket URL
     * @throws IOException if launch fails
     */
    public LaunchResult launch() throws IOException {
        String chromePath = findChromePath();
        int port = options.getDebuggingPort();
        if (port == 0) {
            port = findAvailablePort();
        }

        List<String> commandLine = buildCommandLine(chromePath, port);

        ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
        processBuilder.redirectErrorStream(true);

        browserProcess = processBuilder.start();

        if (!browserProcess.isAlive()) {
            throw new IOException("Browser process failed to start");
        }

        String webSocketUrl = extractWebSocketDebuggerUrl(port);

        return new LaunchResult(browserProcess, webSocketUrl);
    }

    /**
     * Finds the Chrome/Chromium binary path.
     *
     * @return Path to the browser binary
     * @throws IOException if browser is not found
     */
    private String findChromePath() throws IOException {
        if (options.getBinaryPath() != null) {
            Path path = Paths.get(options.getBinaryPath());
            if (Files.exists(path)) {
                return options.getBinaryPath();
            }
            throw new IOException("Specified browser binary not found: " + options.getBinaryPath());
        }

        String envPath = System.getenv("CHROME_PATH");
        if (envPath != null) {
            Path path = Paths.get(envPath);
            if (Files.exists(path)) {
                return envPath;
            }
        }


        // Auto-detect based on OS
        String os = System.getProperty("os.name").toLowerCase();

        List<String> possiblePaths = new ArrayList<>();

        if (os.contains("win")) {
            // Windows paths
            possiblePaths.add("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe");
            possiblePaths.add("C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe");
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null) {
                possiblePaths.add(localAppData + "\\Google\\Chrome\\Application\\chrome.exe");
            }
            possiblePaths.add("C:\\Program Files\\Chromium\\Application\\chrome.exe");
        } else if (os.contains("mac")) {
            // macOS paths
            possiblePaths.add("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
            possiblePaths.add("/Applications/Chromium.app/Contents/MacOS/Chromium");
            String homeDir = System.getProperty("user.home");
            possiblePaths.add(homeDir + "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
        } else {
            // Linux paths
            possiblePaths.add("/usr/bin/google-chrome");
            possiblePaths.add("/usr/bin/google-chrome-stable");
            possiblePaths.add("/usr/bin/chromium");
            possiblePaths.add("/usr/bin/chromium-browser");
            possiblePaths.add("/snap/bin/chromium");
        }

        // Find first existing path
        for (String path : possiblePaths) {
            if (Files.exists(Paths.get(path))) {
                return path;
            }
        }

        throw new IOException(
                "Chrome/Chromium not found. Please install Chrome or set CHROME_PATH environment variable. " +
                        "Searched paths: " + possiblePaths
        );
    }

    /**
     * Builds the command line for launching the browser.
     *
     * @param chromePath Path to the browser binary
     * @param port       Remote debugging port
     * @return List of command line arguments
     */
    private List<String> buildCommandLine(String chromePath, int port) throws IOException {
        List<String> command = new ArrayList<>();
        command.add(chromePath);

        // Remote debugging
        command.add("--remote-debugging-port=" + port);

        // Headless mode
        if (options.isHeadless()) {
            command.add("--headless=new");
        }

        // Window size
        command.add("--window-size=" + options.getWindowWidth() + "," + options.getWindowHeight());

        // User data directory
        if (options.getUserDataDir() != null) {
            command.add("--user-data-dir=" + options.getUserDataDir());
        } else {
            // Create temporary profile directory
            Path tempDir = Files.createTempDirectory("nihonium-chrome-profile-");
            command.add("--user-data-dir=" + tempDir.toAbsolutePath());
        }

        // Disable first run prompts
        command.add("--no-first-run");
        command.add("--no-default-browser-check");

        // Add user-specified arguments
        command.addAll(options.getArguments());

        // Add initial page (about:blank)
        command.add("about:blank");

        return command;
    }

    /**
     * Extracts the WebSocket debugger URL from the browser.
     * This method gets a page-level target, not the browser-level endpoint.
     *
     * @param port Remote debugging port
     * @return WebSocket debugger URL for a page target
     * @throws IOException if extraction fails
     */
    private String extractWebSocketDebuggerUrl(int port) throws IOException {
        // Get list of targets (pages)
        String jsonUrl = "http://localhost:" + port + "/json";

        // Retry a few times in case browser hasn't fully started
        int maxRetries = 10;
        for (int i = 0; i < maxRetries; i++) {
            try {
                URL url = new URL(jsonUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(1000);
                conn.setReadTimeout(1000);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(conn.getInputStream())
                    );
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    // Parse JSON array of targets
                    JsonArray targets = gson.fromJson(response.toString(), JsonArray.class);

                    // Find first page target
                    for (int j = 0; j < targets.size(); j++) {
                        JsonObject target = targets.get(j).getAsJsonObject();
                        String type = target.get("type").getAsString();

                        if ("page".equals(type)) {
                            String wsUrl = target.get("webSocketDebuggerUrl").getAsString();
                            return wsUrl;
                        }
                    }

                    // If no page target found, throw error
                    throw new IOException("No page target found in browser targets");
                }
            } catch (IOException e) {
                if (i < maxRetries - 1) {
                    long deadline = System.currentTimeMillis() + 500;
                    while (System.currentTimeMillis() < deadline) {
                        if (Thread.interrupted()) {
                            Thread.currentThread().interrupt();
                            throw new IOException("Interrupted while waiting for browser", e);
                        }
                    }
                } else {
                    throw new IOException("Failed to get WebSocket debugger URL after " + maxRetries + " retries", e);
                }
            }
        }

        throw new IOException("Failed to extract WebSocket debugger URL");
    }

    /**
     * Finds an available port for remote debugging.
     *
     * @return An available port number
     * @throws IOException if no port is available
     */
    private int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    /**
     * Shuts down the browser process.
     */
    public void shutdown() {
        if (browserProcess != null && browserProcess.isAlive()) {
            browserProcess.destroy();

            try {
                boolean exited = browserProcess.waitFor(5, TimeUnit.SECONDS);
                if (!exited) {
                    browserProcess.destroyForcibly();
                    browserProcess.waitFor(2, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                browserProcess.destroyForcibly();
            }
        }
    }

    /**
     * Checks if the browser process is running.
     *
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return browserProcess != null && browserProcess.isAlive();
    }

    /**
     * Gets the browser process.
     *
     * @return The browser process, or null if not started
     */
    public Process getProcess() {
        return browserProcess;
    }
}
