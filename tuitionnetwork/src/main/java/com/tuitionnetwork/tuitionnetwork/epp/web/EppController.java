package com.tuitionnetwork.tuitionnetwork.epp.web;

import com.tuitionnetwork.tuitionnetwork.epp.domain.EppPlan;
import com.tuitionnetwork.tuitionnetwork.epp.infra.ports.bankmock.EppQuoteResponse;
import com.tuitionnetwork.tuitionnetwork.epp.service.EppPlanNotFoundException;
import com.tuitionnetwork.tuitionnetwork.epp.service.EppService;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.BankMockApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
public class EppController {

    private final EppService eppService;

    public EppController(EppService eppService) {
        this.eppService = eppService;
    }

    @GetMapping("/api/v1/epp/quotes")
    public EppQuoteResponse quotes(@RequestParam BigDecimal amount, @RequestParam String currency) {
        return eppService.getQuotes(amount, currency);
    }

    @PostMapping("/api/v1/epp/plans")
    public ResponseEntity<EppPlanView> createPlan(
            @RequestBody CreateEppPlanRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        EppPlan plan = eppService.createPlan(
                request.paymentId(), request.gatewayPaymentId(), request.tenorMonths(),
                request.productName(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(EppPlanView.from(plan));
    }

    @GetMapping("/api/v1/epp/plans/{id}")
    public EppPlanView getPlan(@PathVariable UUID id) {
        return EppPlanView.from(eppService.getPlan(id));
    }

    @GetMapping("/api/v1/epp/plans")
    public List<EppPlanView> listPlans() {
        return eppService.findAll().stream().map(EppPlanView::from).toList();
    }

    @PostMapping("/api/v1/epp/plans/{id}/cancel")
    public EppPlanView cancelPlan(@PathVariable UUID id) {
        return EppPlanView.from(eppService.cancelPlan(id));
    }

    @ExceptionHandler(EppPlanNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(EppPlanNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError("NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ApiError("BAD_REQUEST", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleConflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError("CONFLICT", e.getMessage()));
    }

    @ExceptionHandler(BankMockApiException.class)
    public ResponseEntity<ApiError> handleGatewayError(BankMockApiException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new ApiError(e.code(), e.getMessage()));
    }

    public record ApiError(String code, String message) {
    }
}
