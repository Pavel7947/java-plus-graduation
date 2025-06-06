package ru.practicum.ewm.controller;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.practicum.ewm.dto.ErrorResponse;
import ru.practicum.ewm.exception.DuplicateException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ServiceUnavailableException;
import ru.practicum.ewm.exception.ValidationException;

@RestControllerAdvice
@Slf4j
public class ErrorHandler {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse handleMethodArgumentNotValidExceptions(MethodArgumentNotValidException e) {
        String message = e.getMessage();
        log.debug("Получен статус 400 BAD_REQUEST {}", message, e);
        return new ErrorResponse(message);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleServiceUnavailable(ServiceUnavailableException e) {
        log.debug("Получен статус 503 SERVICE-UNAVAILABLE {}", e.getMessage(), e);
        return new ErrorResponse("Запрос не был обработан из-за того что сервер перегружен. Попробуйте еще раз");
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ErrorResponse handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String message = e.getMessage();
        log.debug("Получен статус 400 BAD_REQUEST {}", message, e);
        return new ErrorResponse(message);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public ErrorResponse handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getMessage();
        log.debug("Получен статус 400 BAD_REQUEST {}", message, e);
        return new ErrorResponse(message);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicate(final DuplicateException e) {
        log.debug("Получен статус 409 CONFLICT {}", e.getMessage(), e);
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(final NotFoundException e) {
        log.debug("Получен статус 404 NOT_FOUND {}", e.getMessage(), e);
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(final ValidationException e) {
        return new ErrorResponse(e.getMessage());
    }


    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ErrorResponse handleAllExceptions(Exception e) {
        log.debug("Получен статус 500 INTERNAL_SERVER_ERROR {}", e.getMessage(), e);
        return new ErrorResponse("Ой у нас чтото сломалось :)");
    }

}
