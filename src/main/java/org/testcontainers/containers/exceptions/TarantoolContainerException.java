package org.testcontainers.containers.exceptions;

/**
 * Base class for Tarantool runtime exceptions
 *
 * @author Artyom Dubinin
 */
public abstract class TarantoolContainerException extends RuntimeException {
    public TarantoolContainerException(String message) {
        super(message);
    }

    public TarantoolContainerException() {
        super();
    }

    public TarantoolContainerException(Throwable cause) {
        super(cause);
    }

    public TarantoolContainerException(String message, Throwable cause) {
        super(message, cause);
    }
}
