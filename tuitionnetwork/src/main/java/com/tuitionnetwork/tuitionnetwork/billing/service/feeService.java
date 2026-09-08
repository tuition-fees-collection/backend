package com.tuitionnetwork.tuitionnetwork.billing.service;


@Service

public class FeeService{
    private final FeeRepository feeRepository;

    public FeeService(FeeRepository feeRepository){
        this.feeRepository=feeRepository;
    }
}