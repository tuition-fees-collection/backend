package com.tuitionnetwork.tuitionnetwork.identity.service;

import com.tuitionnetwork.tuitionnetwork.identity.domain.AuditAction;
import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;
import com.tuitionnetwork.tuitionnetwork.identity.infra.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditServiceImpl
        implements AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditServiceImpl(
            AuditLogRepository auditLogRepository
    ) {
        this.auditLogRepository =
                auditLogRepository;
    }

    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void recordAuthenticated(
            AuditAction action,
            AuthenticatedActor actor
    ) {
        auditLogRepository
                .insertAuthenticated(
                        action,
                        actor
                );
    }

    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void recordNationalIdLookup(
            AuthenticatedActor actor,
            String nationalIdHmac
    ) {
        auditLogRepository
                .insertNationalIdLookup(
                        actor,
                        nationalIdHmac
                );
    }

    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void recordUnauthenticated(
            AuditAction action
    ) {
        auditLogRepository
                .insertUnauthenticated(
                        action
                );
    }
}
