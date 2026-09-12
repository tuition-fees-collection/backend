package com.tuitionnetwork.tuitionnetwork.identity.infra.repository;

import com.tuitionnetwork.tuitionnetwork.identity.domain.AuditAction;
import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AuditLogRepository {

    private final JdbcClient jdbcClient;

    public AuditLogRepository(
            JdbcClient jdbcClient
    ) {
        this.jdbcClient = jdbcClient;
    }

    public void insertAuthenticated(
            AuditAction action,
            AuthenticatedActor actor
    ) {

        jdbcClient.sql("""
                INSERT INTO common.audit_log (
                    actor_type,
                    actor_id,
                    action
                )
                VALUES (
                    :actorType,
                    :actorId,
                    :action
                )
                """)
                .param(
                        "actorType",
                        actor.actorType().name()
                )
                .param(
                        "actorId",
                        actor.actorId()
                )
                .param(
                        "action",
                        action.name()
                )
                .update();
    }

    public void insertNationalIdLookup(
            AuthenticatedActor actor,
            String nationalIdHmac
    ) {

        jdbcClient.sql("""
                INSERT INTO common.audit_log (
                    actor_type,
                    actor_id,
                    action,
                    target_national_id_hmac
                )
                VALUES (
                    :actorType,
                    :actorId,
                    :action,
                    :nationalIdHmac
                )
                """)
                .param(
                        "actorType",
                        actor.actorType().name()
                )
                .param(
                        "actorId",
                        actor.actorId()
                )
                .param(
                        "action",
                        AuditAction.NID_LOOKUP.name()
                )
                .param(
                        "nationalIdHmac",
                        nationalIdHmac
                )
                .update();
    }

    public void insertUnauthenticated(
            AuditAction action
    ) {

        jdbcClient.sql("""
                INSERT INTO common.audit_log (
                    action
                )
                VALUES (
                    :action
                )
                """)
                .param(
                        "action",
                        action.name()
                )
                .update();
    }
}
