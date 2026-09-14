package com.tuitionnetwork.tuitionnetwork.billing.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
public record FeeSearchRequest(
             
    @NotBlank(message="National ID is required")
    @Pattern(
        regexp="\\d{14}",
        message="National ID must be a 14-digit number"
    )
    String nationalId



){

}

    
