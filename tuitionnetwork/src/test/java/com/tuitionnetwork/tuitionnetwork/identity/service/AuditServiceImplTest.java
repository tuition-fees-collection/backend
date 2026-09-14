package com.tuitionnetwork.tuitionnetwork.identity.service;

import com.tuitionnetwork.tuitionnetwork.identity.domain.*;
import com.tuitionnetwork.tuitionnetwork.identity.infra.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.mockito.Mockito.*;

class AuditServiceImplTest {
    private final AuditLogRepository repository = mock(AuditLogRepository.class);
    private final AuditService service = new AuditServiceImpl(repository);
    private final AuthenticatedActor actor = new AuthenticatedActor(UUID.randomUUID(), UUID.randomUUID(),
            ActorType.BACKOFFICE_EMPLOYEE, RoleCode.BANK_OFFICER, null, null);

    @Test
    void delegatesAuthenticatedEvent() {
        service.recordAuthenticated(AuditAction.LOGIN_SUCCESS, actor);
        verify(repository).insertAuthenticated(AuditAction.LOGIN_SUCCESS, actor);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void delegatesNationalIdLookup() {
        String hmac = "a".repeat(64);
        service.recordNationalIdLookup(actor, hmac);
        verify(repository).insertNationalIdLookup(actor, hmac);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void delegatesUnauthenticatedEvent() {
        service.recordUnauthenticated(AuditAction.LOGIN_FAILURE_UNKNOWN_ACCOUNT);
        verify(repository).insertUnauthenticated(AuditAction.LOGIN_FAILURE_UNKNOWN_ACCOUNT);
        verifyNoMoreInteractions(repository);
    }
}
