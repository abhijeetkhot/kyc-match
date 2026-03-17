package com.foo.kycmatch.service;

import com.foo.kycmatch.config.AppConfig;
import com.foo.kycmatch.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RiskEvaluatorServiceTest {

    private RiskEvaluatorService service;

    @BeforeEach
    void setUp() {
        AppConfig config = new AppConfig();
        config.getRisk().setSuspiciousDomains(List.of("scam.io", "tempmail.com", "mailinator.com"));
        service = new RiskEvaluatorService(config);
    }

    @Test
    void givenFlaggedKycStatus_whenEvaluate_thenKycStatusFlaggedRisk() {
        KycRecord kyc = new KycRecord("KYC-D", "Amanda Torres", "amandatorres@gmail.com",
                "1995-01-30", "6612", "flagged");
        Customer customer = new Customer("MC-004", "Amanda J. Torres", "amandatorres@gmail.com",
                "1995-01-30", "6612");

        List<String> risks = service.evaluate(customer, kyc, MatchStatus.CONFIRMED_MATCH,
                new MatchScore(true, false));
        assertTrue(risks.contains("KYC_STATUS_FLAGGED"));
    }

    @Test
    void givenSuspiciousDomain_whenEvaluate_thenSuspiciousEmailDomainRisk() {
        KycRecord kyc = new KycRecord("KYC-G", "Mike Patel", "mpatel_fake@scam.io",
                "1990-12-01", "5543", "verified");
        Customer customer = new Customer("MC-007", "Michael Patel", "m.patel@gmail.com",
                "1990-12-01", "5543");

        List<String> risks = service.evaluate(customer, kyc, MatchStatus.NEEDS_REVIEW,
                new MatchScore(false, false));
        assertTrue(risks.contains("SUSPICIOUS_EMAIL_DOMAIN"));
    }

    @Test
    void givenDomainDivergence_whenEvaluate_thenEmailDomainDivergenceRisk() {
        KycRecord kyc = new KycRecord("KYC-G", "Mike Patel", "mpatel_fake@scam.io",
                "1990-12-01", "5543", "verified");
        Customer customer = new Customer("MC-007", "Michael Patel", "m.patel@gmail.com",
                "1990-12-01", "5543");

        List<String> risks = service.evaluate(customer, kyc, MatchStatus.NEEDS_REVIEW,
                new MatchScore(false, false));
        assertTrue(risks.contains("EMAIL_DOMAIN_DIVERGENCE"));
    }

    @Test
    void givenNeedsReviewWithEmailDivergence_whenEvaluate_thenNeedsReviewStatusRisk() {
        KycRecord kyc = new KycRecord("KYC-E", "David Kim", "david.kim.personal@gmail.com",
                "1979-08-15", "9901", "verified");
        Customer customer = new Customer("MC-005", "David Kim", "dkim.work@gmail.com",
                "1979-08-15", "9901");

        List<String> risks = service.evaluate(customer, kyc, MatchStatus.NEEDS_REVIEW,
                new MatchScore(false, false));
        assertTrue(risks.contains("NEEDS_REVIEW_STATUS"));
    }

    @Test
    void givenNeedsReviewWithExactEmailMatch_whenEvaluate_thenNoNeedsReviewStatusRisk() {
        // MC-003 case: name diverges (Bob vs Robert) but email exact — no elevated risk
        KycRecord kyc = new KycRecord("KYC-C", "Bob Chen", "rchen88@yahoo.com",
                "1988-11-22", "7754", "verified");
        Customer customer = new Customer("MC-003", "Robert Chen", "rchen88@yahoo.com",
                "1988-11-22", "7754");

        List<String> risks = service.evaluate(customer, kyc, MatchStatus.NEEDS_REVIEW,
                new MatchScore(true, false));
        assertFalse(risks.contains("NEEDS_REVIEW_STATUS"),
                "Name-only divergence with exact email should not trigger NEEDS_REVIEW_STATUS");
        assertTrue(risks.isEmpty(), "Clean record with name-only divergence should have no risk flags");
    }

    @Test
    void givenCleanRecord_whenEvaluate_thenNoRisk() {
        KycRecord kyc = new KycRecord("KYC-A", "James Sullivan", "jsullivan@gmail.com",
                "1985-03-12", "4821", "verified");
        Customer customer = new Customer("MC-001", "James R. Sullivan", "jsullivan@gmail.com",
                "1985-03-12", "4821");

        List<String> risks = service.evaluate(customer, kyc, MatchStatus.CONFIRMED_MATCH,
                new MatchScore(true, false));
        assertTrue(risks.isEmpty());
    }

    @Test
    void givenNoSsnMatch_whenEvaluateNoSsnMatch_thenSsnMismatchRisk() {
        List<String> risks = service.evaluateNoSsnMatch();
        assertEquals(List.of("SSN_MISMATCH"), risks);
    }

    @Test
    void givenDobMismatch_whenEvaluateDobMismatch_thenDobMismatchRisk() {
        List<String> risks = service.evaluateDobMismatch();
        assertEquals(List.of("DOB_MISMATCH"), risks);
    }

    @Test
    void givenAllFourRiskConditions_whenEvaluate_thenAllFourRisksPresent() {
        // flagged + suspicious domain + domain divergence + NEEDS_REVIEW with non-exact email
        KycRecord kyc = new KycRecord("KYC-X", "John Doe", "jdoe@scam.io",
                "1990-01-01", "1234", "flagged");
        Customer customer = new Customer("MC-X", "John Doe", "jdoe@gmail.com",
                "1990-01-01", "1234");

        List<String> risks = service.evaluate(customer, kyc, MatchStatus.NEEDS_REVIEW,
                new MatchScore(false, false));

        assertTrue(risks.contains("KYC_STATUS_FLAGGED"));
        assertTrue(risks.contains("SUSPICIOUS_EMAIL_DOMAIN"));
        assertTrue(risks.contains("EMAIL_DOMAIN_DIVERGENCE"));
        assertTrue(risks.contains("NEEDS_REVIEW_STATUS"));
        assertEquals(4, risks.size());
    }

    @Test
    void givenNullKycEmail_whenEvaluate_thenNoCrash() {
        // extractDomain handles null — should not throw NullPointerException
        KycRecord kyc = new KycRecord("KYC-X", "John Doe", null,
                "1990-01-01", "1234", "verified");
        Customer customer = new Customer("MC-X", "John Doe", "jdoe@gmail.com",
                "1990-01-01", "1234");

        assertDoesNotThrow(() ->
                service.evaluate(customer, kyc, MatchStatus.CONFIRMED_MATCH,
                        new MatchScore(true, false)));
    }

    @Test
    void givenConfirmedMatchWithNonExactEmail_whenEvaluate_thenNoNeedsReviewStatusRisk() {
        KycRecord kyc = new KycRecord("KYC-X", "John Doe", "jdoe@gmail.com",
                "1990-01-01", "1234", "verified");
        Customer customer = new Customer("MC-X", "John Doe", "jdoe@gmail.com",
                "1990-01-01", "1234");

        List<String> risks = service.evaluate(customer, kyc, MatchStatus.CONFIRMED_MATCH,
                new MatchScore(false, true));

        assertFalse(risks.contains("NEEDS_REVIEW_STATUS"),
                "CONFIRMED_MATCH should never trigger NEEDS_REVIEW_STATUS");
    }

    @Test
    void givenLikelyMatchWithNonExactEmail_whenEvaluate_thenNoNeedsReviewStatusRisk() {
        KycRecord kyc = new KycRecord("KYC-X", "John Doe", "jdoe@gmail.com",
                "1990-01-01", "1234", "verified");
        Customer customer = new Customer("MC-X", "John Doe", "jdoe@gmail.com",
                "1990-01-01", "1234");

        List<String> risks = service.evaluate(customer, kyc, MatchStatus.LIKELY_MATCH,
                new MatchScore(false, true));

        assertFalse(risks.contains("NEEDS_REVIEW_STATUS"),
                "LIKELY_MATCH should not trigger NEEDS_REVIEW_STATUS");
    }

    @Test
    void givenKycStatusUppercaseFlagged_whenEvaluate_thenKycStatusFlaggedRisk() {
        KycRecord kyc = new KycRecord("KYC-X", "John Doe", "jdoe@gmail.com",
                "1990-01-01", "1234", "FLAGGED");
        Customer customer = new Customer("MC-X", "John Doe", "jdoe@gmail.com",
                "1990-01-01", "1234");

        List<String> risks = service.evaluate(customer, kyc, MatchStatus.CONFIRMED_MATCH,
                new MatchScore(true, false));

        assertTrue(risks.contains("KYC_STATUS_FLAGGED"),
                "KYC status check should be case-insensitive: 'FLAGGED' should trigger the flag");
    }

    @Test
    void givenKycStatusMixedCaseFlagged_whenEvaluate_thenKycStatusFlaggedRisk() {
        KycRecord kyc = new KycRecord("KYC-X", "John Doe", "jdoe@gmail.com",
                "1990-01-01", "1234", "Flagged");
        Customer customer = new Customer("MC-X", "John Doe", "jdoe@gmail.com",
                "1990-01-01", "1234");

        List<String> risks = service.evaluate(customer, kyc, MatchStatus.CONFIRMED_MATCH,
                new MatchScore(true, false));

        assertTrue(risks.contains("KYC_STATUS_FLAGGED"),
                "KYC status check should be case-insensitive: 'Flagged' should trigger the flag");
    }

    @Test
    void givenEmptySuspiciousDomainsConfig_whenEvaluate_thenNoSuspiciousDomainRisk() {
        AppConfig emptyConfig = new AppConfig();
        emptyConfig.getRisk().setSuspiciousDomains(List.of());
        RiskEvaluatorService serviceWithEmptyDomains = new RiskEvaluatorService(emptyConfig);

        KycRecord kyc = new KycRecord("KYC-X", "John Doe", "jdoe@scam.io",
                "1990-01-01", "1234", "verified");
        Customer customer = new Customer("MC-X", "John Doe", "jdoe@scam.io",
                "1990-01-01", "1234");

        List<String> risks = serviceWithEmptyDomains.evaluate(customer, kyc,
                MatchStatus.CONFIRMED_MATCH, new MatchScore(true, false));

        assertFalse(risks.contains("SUSPICIOUS_EMAIL_DOMAIN"),
                "No suspicious domain risk when config list is empty");
    }

    @Test
    void givenSameEmailDomain_whenEvaluate_thenNoEmailDomainDivergenceRisk() {
        KycRecord kyc = new KycRecord("KYC-X", "John Doe", "jdoe.alt@gmail.com",
                "1990-01-01", "1234", "verified");
        Customer customer = new Customer("MC-X", "John Doe", "jdoe@gmail.com",
                "1990-01-01", "1234");

        List<String> risks = service.evaluate(customer, kyc, MatchStatus.LIKELY_MATCH,
                new MatchScore(false, true));

        assertFalse(risks.contains("EMAIL_DOMAIN_DIVERGENCE"),
                "Same email domain should not trigger EMAIL_DOMAIN_DIVERGENCE");
    }
}
