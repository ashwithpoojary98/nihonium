package io.github.ashwith.exception;

/**
 * Thrown when a Chrome DevTools Protocol operation fails.
 */
public class CDPException extends NihoniumException {

    public CDPException(String message) {
        super(message);
    }

    public CDPException(String message, Throwable cause) {
        super(message, cause);
    }
}
