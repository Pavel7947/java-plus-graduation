package ru.practicum.ewm.exception;

public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(Throwable cause) {
        super(cause);
    }

    public ServiceUnavailableException() {
    }
}
