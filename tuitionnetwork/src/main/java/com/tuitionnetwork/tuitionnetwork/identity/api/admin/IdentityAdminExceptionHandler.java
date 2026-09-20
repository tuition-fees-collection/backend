package com.tuitionnetwork.tuitionnetwork.identity.api.admin;

import com.tuitionnetwork.tuitionnetwork.identity.api.AuthenticationErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.tuitionnetwork.tuitionnetwork.identity.api.admin")
public class IdentityAdminExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public IdentityValidationErrorResponse invalidBody(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        String message = fieldErrors.isEmpty()
                ? "Request is invalid."
                : String.join(" ", fieldErrors.values());
        return new IdentityValidationErrorResponse("VALIDATION_ERROR", message, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AuthenticationErrorResponse unreadableBody() {
        return new AuthenticationErrorResponse("MALFORMED_JSON", "Request body is invalid.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AuthenticationErrorResponse invalidRequest(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        String message = exception.getMessage() == null || exception.getMessage().isBlank()
                ? defaultInvalidMessage(request)
                : exception.getMessage();
        return new AuthenticationErrorResponse(invalidCode(request), message);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public AuthenticationErrorResponse conflict(HttpServletRequest request) {
        if (isStaffRequest(request)) {
            return new AuthenticationErrorResponse(
                    "STAFF_CONFLICT",
                    "Staff account could not be created or updated."
            );
        }
        return new AuthenticationErrorResponse(
                "INSTITUTION_CONFLICT",
                "Institution operation could not be completed."
        );
    }

    private static String invalidCode(HttpServletRequest request) {
        return isStaffRequest(request) ? "INVALID_STAFF_REQUEST" : "INVALID_INSTITUTION_REQUEST";
    }

    private static String defaultInvalidMessage(HttpServletRequest request) {
        return isStaffRequest(request) ? "Invalid staff request." : "Invalid institution request.";
    }

    private static boolean isStaffRequest(HttpServletRequest request) {
        return request.getRequestURI().contains("/backoffice-employees");
    }
}
