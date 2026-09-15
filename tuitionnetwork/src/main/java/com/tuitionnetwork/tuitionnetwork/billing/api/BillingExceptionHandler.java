package com.tuitionnetwork.tuitionnetwork.billing.api;

import com.tuitionnetwork.tuitionnetwork.billing.service.FeeNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class BillingExceptionHandler {

    @ExceptionHandler(FeeNotFoundException.class)
    ResponseEntity<ApiError> feeNotFound(FeeNotFoundException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiError(
                        "NOT_FOUND",
                        e.getMessage(),
                        Instant.now()
                ));
    }

    public record ApiError(
            String code,
            String message,
            Instant timestamp
    ) {
    }
}