package com.example.orderreconciliation.exception;

/**
 * Thrown when an order fails business validation.
 */
public class InvalidOrderException extends RuntimeException {

    public InvalidOrderException(String message) {
        super(message);
    }
}
