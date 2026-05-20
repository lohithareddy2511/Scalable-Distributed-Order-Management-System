package com.ordermanagement.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String productName, int requested, int available) {
        super(String.format("Insufficient stock for product '%s'. Requested: %d, Available: %d",
                productName, requested, available));
    }

    public InsufficientStockException(String message) {
        super(message);
    }
}
