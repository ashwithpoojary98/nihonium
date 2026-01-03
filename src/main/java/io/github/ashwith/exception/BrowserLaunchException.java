package io.github.ashwith.exception;

/**
 * Thrown when the browser fails to launch.
 */
public class BrowserLaunchException extends NihoniumException {

    public BrowserLaunchException(String message) {
        super(message);
    }

    public BrowserLaunchException(String message, Throwable cause) {
        super(message, cause);
    }
}
