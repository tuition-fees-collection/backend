package com.tuitionnetwork.tuitionnetwork.identity.service;

import com.tuitionnetwork.tuitionnetwork.identity.api.admin.BackofficeEmployeeResponse;
import com.tuitionnetwork.tuitionnetwork.identity.api.admin.ChangeBackofficeRoleRequest;
import com.tuitionnetwork.tuitionnetwork.identity.api.admin.CreateBackofficeEmployeeRequest;

import java.util.List;
import java.util.UUID;

public interface BackofficeAdminService {

    List<BackofficeEmployeeResponse> getAll();

    BackofficeEmployeeResponse create(
            CreateBackofficeEmployeeRequest request
    );

    void changeRole(
            UUID employeeId,
            ChangeBackofficeRoleRequest request
    );
}
