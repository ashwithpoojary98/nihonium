package io.github.ashwith.exception;

/**
 * Thrown when an element cannot be found in the DOM.
 */
public class ElementNotFoundException extends NihoniumException {

    public ElementNotFoundException(String message) {
        super(message);
    }

    public ElementNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
