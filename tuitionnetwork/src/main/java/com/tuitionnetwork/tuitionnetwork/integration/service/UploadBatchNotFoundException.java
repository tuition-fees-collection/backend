package com.tuitionnetwork.tuitionnetwork.integration.service;

import java.util.UUID;

public class UploadBatchNotFoundException extends RuntimeException {

    public UploadBatchNotFoundException(UUID batchId) {
        super("No such upload batch: " + batchId);
    }
}
