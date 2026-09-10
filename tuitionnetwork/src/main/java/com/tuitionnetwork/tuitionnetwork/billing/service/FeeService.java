package com.tuitionnetwork.tuitionnetwork.billing.service;
import org.springframework.stereotype.Service;

import com.tuitionnetwork.tuitionnetwork.billing.infra.FeeRepository;


@Service

public class FeeService{
    private final FeeRepository feeRepository;

    public FeeService(FeeRepository feeRepository){
        this.feeRepository=feeRepository;
    }
}