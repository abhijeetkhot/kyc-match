package com.foo.kycmatch.service;

import com.foo.kycmatch.config.AppConfig;
import com.foo.kycmatch.model.Customer;
import com.foo.kycmatch.model.KycRecord;
import com.foo.kycmatch.model.MatchScore;
import com.foo.kycmatch.model.MatchStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RiskEvaluatorService {

    private final List<String> suspiciousDomains;

    public RiskEvaluatorService(AppConfig appConfig) {
        this.suspiciousDomains = appConfig.getRisk().getSuspiciousDomains();
    }

    /**
     * Evaluate risk for records that passed the SSN + DOB hard gates and received a classification.
     * <p>
     * NEEDS_REVIEW_STATUS applies only when the match is NEEDS_REVIEW and the email did not exactly
     * match. A name-only divergence (e.g. Bob vs Robert) with an exact email match is not considered
     * elevated risk by itself.
     */
    public List<String> evaluate(Customer customer, KycRecord kycRecord, MatchStatus status, MatchScore emailScore) {
        List<String> risks = new ArrayList<>();

        if ("flagged".equalsIgnoreCase(kycRecord.status())) {
            risks.add("KYC_STATUS_FLAGGED");
        }

        String kycDomain = extractDomain(kycRecord.email());
        if (suspiciousDomains.contains(kycDomain)) {
            risks.add("SUSPICIOUS_EMAIL_DOMAIN");
        }

        String customerDomain = extractDomain(customer.email());
        if (!customerDomain.equals(kycDomain)) {
            risks.add("EMAIL_DOMAIN_DIVERGENCE");
        }

        if (status == MatchStatus.NEEDS_REVIEW && !emailScore.exactMatch()) {
            risks.add("NEEDS_REVIEW_STATUS");
        }

        return risks;
    }

    /** Risk for the SSN hard-gate failure: no KYC record shares the customer's SSN. */
    public List<String> evaluateNoSsnMatch() {
        return List.of("SSN_MISMATCH");
    }

    /** Risk for the DOB hard-gate failure: SSN matched but DOB did not. */
    public List<String> evaluateDobMismatch() {
        return List.of("DOB_MISMATCH");
    }

    private String extractDomain(String email) {
        if (email == null) return "";
        int at = email.lastIndexOf('@');
        return at >= 0 ? email.substring(at + 1).toLowerCase() : "";
    }
}
