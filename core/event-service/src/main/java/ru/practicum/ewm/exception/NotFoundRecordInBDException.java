package ru.practicum.ewm.exception;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NotFoundRecordInBDException extends RuntimeException {
    public NotFoundRecordInBDException(String message) {
        super(message);
    }
}
