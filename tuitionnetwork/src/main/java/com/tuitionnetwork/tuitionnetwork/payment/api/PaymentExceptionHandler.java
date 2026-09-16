package com.tuitionnetwork.tuitionnetwork.payment.api;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import com.tuitionnetwork.tuitionnetwork.payment.domain.IllegalPaymentTransitionException;
import com.tuitionnetwork.tuitionnetwork.payment.services.PaymentAccessDeniedException;
import com.tuitionnetwork.tuitionnetwork.payment.services.Paymentvalidationexception;
@RestControllerAdvice(basePackages = "com.tuitionnetwork.tuitionnetwork.payment")
public class PaymentExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(PaymentExceptionHandler.class);
    //unauthorized access
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> forbidden(AccessDeniedException e) {
        return respond(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access denied");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadableBody(HttpMessageNotReadableException e) {
        log.warn("Unreadable request body: {}", e.getMessage());
        return respond(HttpStatus.BAD_REQUEST, "MALFORMED_JSON",
                "The request body could not be read");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> missingRoute(NoResourceFoundException e) {
        return respond(HttpStatus.NOT_FOUND, "NOT_FOUND", "Not found");
    }

    @ExceptionHandler(PaymentAccessDeniedException.class)
    ResponseEntity<ApiError> accessDenied(PaymentAccessDeniedException e) {
        log.warn("Access denied for guardian {} on resource {}",
                e.getGuardianId(), e.getResourceId());
        return respond(HttpStatus.NOT_FOUND, "NOT_FOUND", "Payment not found");
    }
    //business rule failed 
  @ExceptionHandler(Paymentvalidationexception.class)
ResponseEntity<ApiError> validation(Paymentvalidationexception e) {
    return respond(HttpStatus.UNPROCESSABLE_CONTENT, e.getCode(), e.getMessage());
}
//chage state
    @ExceptionHandler(IllegalPaymentTransitionException.class)
    ResponseEntity<ApiError> illegalTransition(IllegalPaymentTransitionException e) {
        log.error("Illegal transition on payment {}: {} -> {}",
                e.getPaymentId(), e.getFrom(), e.getTo());
        return respond(HttpStatus.CONFLICT, "INVALID_PAYMENT_STATE",
                "This payment cannot change state that way");
    }
    //click ay in same time
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ApiError> conflict(OptimisticLockingFailureException e) {
        log.warn("Concurrent modification: {}", e.getMessage());
        return respond(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "This fee was being paid at the same moment. Please try again.");
    }
    //ex as give payemnt idemocety key exist
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> integrity(DataIntegrityViolationException e) {
        log.warn("Constraint violation: {}", e.getMostSpecificCause().getMessage());
        return respond(HttpStatus.CONFLICT, "DUPLICATE_REQUEST",
                "This request was already being processed");
    }
    @ExceptionHandler(MissingRequestHeaderException.class)
    ResponseEntity<ApiError> missingHeader(MissingRequestHeaderException e) {
        return respond(HttpStatus.BAD_REQUEST, "MISSING_HEADER",
                "Required header is missing: " + e.getHeaderName());
    }
    //card holder not balnk
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException e) {
        List<String> problems = e.getBindingResult().getFieldErrors().stream()
                .map(field -> field.getField() + ": " + field.getDefaultMessage())
                .toList();
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", String.join("; ", problems));
    }
    //pass as negative payemnt
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> illegalArgument(IllegalArgumentException e) {
        return respond(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
    }
    //as make reepit for payment is not made
    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ApiError> illegalState(IllegalStateException e) {
        return respond(HttpStatus.CONFLICT, "INVALID_STATE", e.getMessage());
    }
    //runtime
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception e) {
        log.error("Unhandled exception", e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Something went wrong. Please try again.");
    }

    private ResponseEntity<ApiError> respond(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiError(code, message, Instant.now()));
    }
    public record ApiError(String code, String message, Instant timestamp) {
    }
}