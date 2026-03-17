package com.foo.kycmatch.model;

public record MatchResult(
        String customerId,
        String kycVerificationId,
        MatchStatus matchStatus,
        String matchedFields,
        String divergedFields,
        String reasoning,
        boolean riskFlag,
        String riskReason
) {}
