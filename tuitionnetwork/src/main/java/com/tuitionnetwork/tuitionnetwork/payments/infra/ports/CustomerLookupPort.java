package com.tuitionnetwork.tuitionnetwork.payments.infra.ports;

import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.CustomerResponse;

public interface CustomerLookupPort {

    CustomerResponse findByNationalId(String nationalId);
}
