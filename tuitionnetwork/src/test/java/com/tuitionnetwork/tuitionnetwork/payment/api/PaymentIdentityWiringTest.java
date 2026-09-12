package com.tuitionnetwork.tuitionnetwork.payment.api;

import com.tuitionnetwork.tuitionnetwork.identity.domain.ActorType;
import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthAccount;
import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;
import com.tuitionnetwork.tuitionnetwork.identity.domain.RoleCode;
import com.tuitionnetwork.tuitionnetwork.identity.infra.repository.AuthAccountRepository;
import com.tuitionnetwork.tuitionnetwork.identity.service.auth.TokenIssuer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentIdentityWiringTest {
    private static final UUID ONLINE_CHANNEL = UUID.fromString("22222222-2222-2222-2222-000000000000");

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired AuthAccountRepository accounts;
    @Autowired TokenIssuer tokens;

    private UUID guardianId;
    private UUID otherGuardianId;
    private UUID studentId;
    private UUID otherStudentId;
    private UUID institutionId;
    private UUID feeId;
    private UUID otherFeeId;
    private UUID officerId;
    private UUID payerAccountId;
    private UUID otherPayerAccountId;
    private UUID officerAccountId;
    private String payerToken;
    private String otherPayerToken;
    private String officerToken;

    @BeforeEach
    void seed() {
        guardianId = UUID.randomUUID();
        otherGuardianId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        otherStudentId = UUID.randomUUID();
        institutionId = UUID.randomUUID();
        feeId = UUID.randomUUID();
        otherFeeId = UUID.randomUUID();
        officerId = UUID.randomUUID();

        jdbc.update("INSERT INTO identity.guardian(id, national_id_hmac, national_id_encrypted, full_name, verified) VALUES (?, ?, '\\x00', 'Payer One', true)",
                guardianId, "hmac-" + guardianId);
        jdbc.update("INSERT INTO identity.guardian(id, national_id_hmac, national_id_encrypted, full_name, verified) VALUES (?, ?, '\\x00', 'Payer Two', true)",
                otherGuardianId, "hmac-" + otherGuardianId);
        jdbc.update("INSERT INTO identity.student(id, national_id_hmac, full_name) VALUES (?, ?, 'Child One')",
                studentId, "hmac-" + studentId);
        jdbc.update("INSERT INTO identity.student(id, national_id_hmac, full_name) VALUES (?, ?, 'Child Two')",
                otherStudentId, "hmac-" + otherStudentId);
        jdbc.update("INSERT INTO identity.guardian_student(guardian_id, student_id, relationship) VALUES (?, ?, 'MOTHER')",
                guardianId, studentId);
        jdbc.update("INSERT INTO identity.guardian_student(guardian_id, student_id, relationship) VALUES (?, ?, 'FATHER')",
                otherGuardianId, otherStudentId);
        jdbc.update("INSERT INTO billing.institution(id, name, type, status) VALUES (?, 'Wiring School', 'SCHOOL', 'ACTIVE')",
                institutionId);
        jdbc.update("""
                INSERT INTO billing.fee(id, student_id, institution_id, fee_type, collection_period, currency, amount, amount_paid, status)
                VALUES (?, ?, ?, 'TUITION', '2026-T1', 'EGP', 5000.00, 0.00, 'OUTSTANDING')
                """, feeId, studentId, institutionId);
        jdbc.update("""
                INSERT INTO billing.fee(id, student_id, institution_id, fee_type, collection_period, currency, amount, amount_paid, status)
                VALUES (?, ?, ?, 'BUS', '2026-T1', 'EGP', 1200.00, 0.00, 'OUTSTANDING')
                """, otherFeeId, otherStudentId, institutionId);
        jdbc.update("INSERT INTO identity.backoffice_employee(id, full_name, branch, role_id) SELECT ?, 'Wiring Officer', 'Giza', id FROM identity.role WHERE name = 'BANK_OFFICER'",
                officerId);

        payerAccountId = accounts.saveAndFlush(AuthAccount.forGuardian("wire-payer-" + guardianId, "{noop}x", guardianId)).getId();
        otherPayerAccountId = accounts.saveAndFlush(AuthAccount.forGuardian("wire-payer-" + otherGuardianId, "{noop}x", otherGuardianId)).getId();
        officerAccountId = accounts.saveAndFlush(AuthAccount.forBackofficeEmployee("wire-officer-" + officerId, "{noop}x", officerId)).getId();

        payerToken = tokens.issue(new AuthenticatedActor(payerAccountId, guardianId, ActorType.GUARDIAN, RoleCode.PAYER, null, null), 0);
        otherPayerToken = tokens.issue(new AuthenticatedActor(otherPayerAccountId, otherGuardianId, ActorType.GUARDIAN, RoleCode.PAYER, null, null), 0);
        officerToken = tokens.issue(new AuthenticatedActor(officerAccountId, officerId, ActorType.BACKOFFICE_EMPLOYEE, RoleCode.BANK_OFFICER, null, "Giza"), 0);
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM receipts.receipt WHERE payment_id IN (SELECT id FROM payments.payment WHERE guardian_id IN (?, ?))",
                guardianId, otherGuardianId);
        jdbc.update("DELETE FROM payments.payment_allocation WHERE payment_id IN (SELECT id FROM payments.payment WHERE guardian_id IN (?, ?))",
                guardianId, otherGuardianId);
        jdbc.update("DELETE FROM payments.payment_saga_step WHERE payment_id IN (SELECT id FROM payments.payment WHERE guardian_id IN (?, ?))",
                guardianId, otherGuardianId);
        jdbc.update("DELETE FROM payments.outbox_event WHERE aggregate_id IN (SELECT id FROM payments.payment WHERE guardian_id IN (?, ?))",
                guardianId, otherGuardianId);
        jdbc.update("DELETE FROM payments.payment WHERE guardian_id IN (?, ?)", guardianId, otherGuardianId);
        jdbc.update("DELETE FROM billing.fee WHERE id IN (?, ?)", feeId, otherFeeId);
        jdbc.update("DELETE FROM identity.auth_account WHERE id IN (?, ?, ?)", payerAccountId, otherPayerAccountId, officerAccountId);
        jdbc.update("DELETE FROM identity.guardian_student WHERE guardian_id IN (?, ?)", guardianId, otherGuardianId);
        jdbc.update("DELETE FROM identity.backoffice_employee WHERE id = ?", officerId);
        jdbc.update("DELETE FROM billing.institution WHERE id = ?", institutionId);
        jdbc.update("DELETE FROM identity.student WHERE id IN (?, ?)", studentId, otherStudentId);
        jdbc.update("DELETE FROM identity.guardian WHERE id IN (?, ?)", guardianId, otherGuardianId);
    }

    @Test
    void payerCreatesCardOrderFromJwtWithoutClientGuardianOrProcessor() throws Exception {
        mvc.perform(post("/api/v1/payment-orders")
                        .header("Authorization", "Bearer " + payerToken)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(studentId, feeId, "CREDIT_CARD", "500.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.amount").value(500.00))
                .andExpect(jsonPath("$.processedBy").doesNotExist());

        Integer stored = jdbc.queryForObject(
                "SELECT count(*) FROM payments.payment WHERE guardian_id = ? AND processed_by = ?",
                Integer.class, guardianId, ONLINE_CHANNEL);
        org.junit.jupiter.api.Assertions.assertEquals(1, stored);
    }

    @Test
    void payerCannotCreateCashOrPayAnotherFamily() throws Exception {
        mvc.perform(post("/api/v1/payment-orders")
                        .header("Authorization", "Bearer " + payerToken)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(studentId, feeId, "CASH", "500.00")))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/payment-orders")
                        .header("Authorization", "Bearer " + payerToken)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(otherStudentId, otherFeeId, "CREDIT_CARD", "1200.00")))
                .andExpect(status().isNotFound());
    }

    @Test
    void officerCashPaymentUsesOfficerAsProcessorAndPayerCanReadReceipt() throws Exception {
        String create = mvc.perform(post("/api/v1/payment-orders")
                        .header("Authorization", "Bearer " + officerToken)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(studentId, feeId, "CASH", "1500.00")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String paymentId = create.replaceAll(".*\"paymentId\":\"([^\"]+)\".*", "$1");

        mvc.perform(post("/api/v1/payment-orders/" + paymentId + "/pay-cash")
                        .header("Authorization", "Bearer " + otherPayerToken))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/v1/payment-orders/" + paymentId + "/pay-cash")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESSFUL"));

        mvc.perform(get("/api/v1/payment-orders/" + paymentId + "/receipt")
                        .header("Authorization", "Bearer " + payerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(1500.00))
                .andExpect(jsonPath("$.method").value("CASH"));

        mvc.perform(get("/api/v1/payment-orders")
                        .header("Authorization", "Bearer " + payerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SUCCESSFUL"));
    }

    @Test
    void unauthenticatedCreateIsUnauthorized() throws Exception {
        mvc.perform(post("/api/v1/payment-orders")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(studentId, feeId, "CASH", "100.00")))
                .andExpect(status().isUnauthorized());
    }

    private static String orderBody(UUID studentId, UUID feeId, String method, String amount) {
        return """
                {
                  "studentId": "%s",
                  "feeLines": [ { "feeId": "%s", "amountToApply": %s } ],
                  "method": "%s",
                  "paymentType": "PARTIAL",
                  "currency": "EGP"
                }
                """.formatted(studentId, feeId, amount, method);
    }
}
