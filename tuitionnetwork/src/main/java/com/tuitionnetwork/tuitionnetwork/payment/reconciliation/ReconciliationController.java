package com.tuitionnetwork.tuitionnetwork.payment.reconciliation;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
@RestController
@RequestMapping("/api/v1/admin/gateway-checks")
@SecurityRequirement(name = "bearerAuth")
public class ReconciliationController {
    private final GatewayStatusCheckService checks;
    private final GatewayCheckStore store;
    public ReconciliationController(GatewayStatusCheckService checks, GatewayCheckStore store) {
        this.checks = checks;
        this.store = store;
    }
    @PostMapping("/run")
    public GatewayCheckRecords.Run runNow() {
        return checks.sweep();
    }
    @GetMapping
    public List<GatewayCheckRecords.Run> recent() {
        return store.recentRuns(10);
    }
    @GetMapping("/{runId}/items")
    public List<GatewayCheckRecords.Item> itemsFor(@PathVariable UUID runId) {
        return store.itemsForRun(runId);
    }
    @GetMapping("/payments/{paymentId}")
    public List<GatewayCheckRecords.Item> historyFor(@PathVariable UUID paymentId) {
        return store.itemsForPayment(paymentId);
    }
}
