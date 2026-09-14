package com.tuitionnetwork.tuitionnetwork.identity.service;

import com.tuitionnetwork.tuitionnetwork.identity.domain.NationalId;

public interface NationalIdEncryptor {

    byte[] encrypt(NationalId nationalId);
}
