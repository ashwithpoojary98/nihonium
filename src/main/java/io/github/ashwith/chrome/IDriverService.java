package io.github.ashwith.chrome;

public interface IDriverService {

    void start();

    void stop();

    boolean isRunning();

    String getUrl();
}
