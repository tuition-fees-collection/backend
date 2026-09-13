package com.tuitionnetwork.tuitionnetwork.identity.service;

import com.tuitionnetwork.tuitionnetwork.identity.api.admin.*;
import java.util.*;

public interface InstitutionAdminService {
    List<InstitutionResponse> getInstitutions();
    InstitutionResponse createInstitution(CreateInstitutionRequest request);
    InstitutionResponse updateInstitution(UUID institutionId, UpdateInstitutionRequest request);
    List<InstitutionUserResponse> getInstitutionUsers(UUID institutionId);
    InstitutionUserResponse createInstitutionUser(UUID institutionId, CreateInstitutionUserRequest request);
}
