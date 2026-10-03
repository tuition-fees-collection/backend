package com.tuitionnetwork.tuitionnetwork.chatbot.api;

import com.tuitionnetwork.tuitionnetwork.chatbot.service.ChatbotModelUnavailableException;
import com.tuitionnetwork.tuitionnetwork.chatbot.service.ChatbotQueryException;
import com.tuitionnetwork.tuitionnetwork.chatbot.service.ChatbotRateLimitedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ChatbotController.class)
public class ChatbotExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ChatbotAskResponse> invalid(MethodArgumentNotValidException ignored) {
        return ResponseEntity.badRequest().body(ChatbotAskResponse.failed(
                "invalid_parameters",
                "A message is required."
        ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ChatbotAskResponse> denied(AccessDeniedException ignored) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ChatbotAskResponse.failed(
                "not_authorized",
                "You don't have access to that information."
        ));
    }

    @ExceptionHandler(ChatbotRateLimitedException.class)
    ResponseEntity<ChatbotAskResponse> limited(ChatbotRateLimitedException ignored) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(ChatbotAskResponse.failed(
                "rate_limited",
                "Too many questions. Please wait a minute and try again."
        ));
    }

    @ExceptionHandler(ChatbotModelUnavailableException.class)
    ResponseEntity<ChatbotAskResponse> unavailable(ChatbotModelUnavailableException ignored) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ChatbotAskResponse.failed(
                "llm_unavailable",
                "The assistant is unavailable right now."
        ));
    }

    @ExceptionHandler(ChatbotQueryException.class)
    ResponseEntity<ChatbotAskResponse> queryFailed(ChatbotQueryException ignored) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ChatbotAskResponse.failed(
                "internal_error",
                "Something went wrong answering that."
        ));
    }
}
