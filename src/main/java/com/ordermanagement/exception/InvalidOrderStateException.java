package com.ordermanagement.exception;

public class InvalidOrderStateException extends RuntimeException {

    public InvalidOrderStateException(String message) {
        super(message);
    }

    public InvalidOrderStateException(String currentState, String targetState) {
        super(String.format("Cannot transition order from '%s' to '%s'", currentState, targetState));
    }
}
