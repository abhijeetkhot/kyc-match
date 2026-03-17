package com.foo.kycmatch.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KycRecord(
        @JsonProperty("verification_id") String verificationId,
        @JsonProperty("full_name") String fullName,
        String email,
        String dob,
        @JsonProperty("ssn_last4") String ssnLast4,
        String status
) {}
