package com.tuitionnetwork.tuitionnetwork.identity.service;

public class StudentNotFoundException extends RuntimeException {

    public StudentNotFoundException() {
        super("No student was found for the supplied national ID");
    }
}
