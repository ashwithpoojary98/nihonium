package io.github.ashwithpoojary98.chrome;

public interface IDriverService {

    void start();

    void stop();

    boolean isRunning();

    String getUrl();
}
