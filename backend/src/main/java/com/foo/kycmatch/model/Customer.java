package com.foo.kycmatch.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Customer(
        String id,
        String name,
        String email,
        String dob,
        @JsonProperty("ssn_last4") String ssnLast4
) {}
