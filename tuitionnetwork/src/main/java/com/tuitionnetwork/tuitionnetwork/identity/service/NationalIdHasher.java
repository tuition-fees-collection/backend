package com.tuitionnetwork.tuitionnetwork.identity.service;

import com.tuitionnetwork.tuitionnetwork.identity.domain.NationalId;

public interface NationalIdHasher {

    String hash(NationalId nationalId);
}
