package org.testcontainers.containers.exceptions;

public class CartridgeTopologyException extends TarantoolContainerException {

    static final String errorMsg = "Failed to change the app topology";

    public CartridgeTopologyException(String message) {
        super(message);
    }

    public CartridgeTopologyException(Throwable cause) {
        super(errorMsg, cause);
    }
}
