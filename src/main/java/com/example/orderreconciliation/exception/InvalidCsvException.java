package com.example.orderreconciliation.exception;

/**
 * Thrown when a CSV file fails validation (bad header, malformed rows, etc).
 */
public class InvalidCsvException extends RuntimeException {

    public InvalidCsvException(String message) {
        super(message);
    }
}
