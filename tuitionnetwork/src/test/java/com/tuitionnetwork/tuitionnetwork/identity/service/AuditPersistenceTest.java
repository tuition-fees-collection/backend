package com.tuitionnetwork.tuitionnetwork.identity.service;

import com.tuitionnetwork.tuitionnetwork.identity.domain.*;
import com.tuitionnetwork.tuitionnetwork.identity.infra.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditPersistenceTest {
    @Autowired AuditService auditService;
    @Autowired AuditLogRepository repository;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void auditSurvivesOuterTransactionRollback() {
        UUID actorId = UUID.randomUUID();
        var actor = new AuthenticatedActor(UUID.randomUUID(), actorId,
                ActorType.BACKOFFICE_EMPLOYEE, RoleCode.BANK_OFFICER, null, null);
        String hmac = "a".repeat(64);
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                auditService.recordNationalIdLookup(actor, hmac);
                status.setRollbackOnly();
            });
            assertEquals(1, jdbc.queryForObject("""
                    SELECT count(*) FROM common.audit_log
                    WHERE actor_id = ? AND actor_type = 'BACKOFFICE_EMPLOYEE'
                      AND action = 'NID_LOOKUP' AND target_national_id_hmac = ?
                    """, Integer.class, actorId, hmac));
        } finally {
            jdbc.update("DELETE FROM common.audit_log WHERE actor_id = ?", actorId);
        }
    }

    @Test
    void migrationAllowsAnonymousEventsButRejectsPartialActor() {
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM public.flyway_schema_history
                WHERE version = '7' AND success = true
                """, Integer.class));
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            int before = jdbc.queryForObject("SELECT count(*) FROM common.audit_log WHERE actor_id IS NULL",
                    Integer.class);
            repository.insertUnauthenticated(AuditAction.LOGIN_FAILURE_UNKNOWN_ACCOUNT);
            assertEquals(before + 1, jdbc.queryForObject(
                    "SELECT count(*) FROM common.audit_log WHERE actor_id IS NULL", Integer.class));
            status.setRollbackOnly();
        });
        assertThrows(DataIntegrityViolationException.class, () ->
                new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                        jdbc.update("INSERT INTO common.audit_log(actor_type, action) VALUES ('GUARDIAN', 'LOGIN_SUCCESS')")));
    }
}
