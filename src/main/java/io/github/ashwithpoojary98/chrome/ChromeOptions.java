package io.github.ashwithpoojary98.chrome;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChromeOptions {

    private String binaryPath;
    private boolean headless = false;
    private List<String> arguments = new ArrayList<>();
    private Map<String, Object> experimentalOptions = new HashMap<>();
    private int debuggingPort = 0;
    private String userDataDir;
    private int windowWidth = 1280;
    private int windowHeight = 720;

    public ChromeOptions() {
    }

    public ChromeOptions setBinary(String path) {
        this.binaryPath = path;
        return this;
    }

    public String getBinary() {
        return binaryPath;
    }

    public ChromeOptions setHeadless(boolean headless) {
        this.headless = headless;
        return this;
    }

    public boolean isHeadless() {
        return headless;
    }

    public ChromeOptions addArguments(String... arguments) {
        for (String arg : arguments) {
            this.arguments.add(arg);
        }
        return this;
    }

    public ChromeOptions addArguments(List<String> arguments) {
        this.arguments.addAll(arguments);
        return this;
    }

    public List<String> getArguments() {
        return new ArrayList<>(arguments);
    }

    public ChromeOptions setExperimentalOption(String name, Object value) {
        this.experimentalOptions.put(name, value);
        return this;
    }

    public Object getExperimentalOption(String name) {
        return experimentalOptions.get(name);
    }

    public Map<String, Object> getExperimentalOptions() {
        return new HashMap<>(experimentalOptions);
    }

    public ChromeOptions setDebuggerAddress(String address) {
        return this;
    }

    public int getDebuggingPort() {
        return debuggingPort;
    }

    public ChromeOptions setDebuggingPort(int port) {
        this.debuggingPort = port;
        return this;
    }

    public String getUserDataDir() {
        return userDataDir;
    }

    public ChromeOptions setUserDataDir(String userDataDir) {
        this.userDataDir = userDataDir;
        return this;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public ChromeOptions setWindowSize(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        return this;
    }
}
