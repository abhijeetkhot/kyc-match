package com.foo.kycmatch.service;

import com.foo.kycmatch.config.AppConfig;
import com.foo.kycmatch.model.Customer;
import com.foo.kycmatch.model.KycRecord;
import com.foo.kycmatch.model.MatchResult;
import com.foo.kycmatch.model.MatchStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MatchingServiceTest {

    private MatchingService matchingService;

    private static final List<Customer> CUSTOMERS = List.of(
            new Customer("MC-001", "James R. Sullivan",  "jsullivan@gmail.com",       "1985-03-12", "4821"),
            new Customer("MC-002", "Priya Nair",         "priya.nair@outlook.com",    "1991-07-04", "3309"),
            new Customer("MC-003", "Robert Chen",        "rchen88@yahoo.com",         "1988-11-22", "7754"),
            new Customer("MC-004", "Amanda J. Torres",   "amandatorres@gmail.com",    "1995-01-30", "6612"),
            new Customer("MC-005", "David Kim",          "dkim.work@gmail.com",       "1979-08-15", "9901"),
            new Customer("MC-006", "Sarah E. Johnson",   "sejohnson@proton.me",       "1983-05-09", "2278"),
            new Customer("MC-007", "Michael Patel",      "m.patel@gmail.com",         "1990-12-01", "5543")
    );

    private static final List<KycRecord> KYC_RECORDS = List.of(
            new KycRecord("KYC-A", "James Sullivan",   "jsullivan@gmail.com",          "1985-03-12", "4821", "verified"),
            new KycRecord("KYC-B", "Priya Nair",       "priya.n@outlook.com",          "1991-07-04", "3309", "verified"),
            new KycRecord("KYC-C", "Bob Chen",         "rchen88@yahoo.com",            "1988-11-22", "7754", "verified"),
            new KycRecord("KYC-D", "Amanda Torres",    "amandatorres@gmail.com",       "1995-01-30", "0000", "flagged"),
            new KycRecord("KYC-E", "David Kim",        "david.kim.personal@gmail.com", "1979-08-15", "9901", "verified"),
            new KycRecord("KYC-F", "Sarah Johnson",    "sejohnson@proton.me",          "1990-05-09", "2278", "verified"),
            new KycRecord("KYC-G", "Mike Patel",       "mpatel_fake@scam.io",          "1990-12-01", "5543", "verified")
    );

    @BeforeEach
    void setUp() {
        AppConfig config = new AppConfig();
        config.getFuzzy().setNameScoreThreshold(0.20);
        config.getFuzzy().setEmailScoreThreshold(0.35);
        config.getRisk().setSuspiciousDomains(List.of("scam.io", "tempmail.com", "mailinator.com"));

        NameMatcherService  nameMatcher  = new NameMatcherService(config.getFuzzy().getNameScoreThreshold());
        EmailMatcherService emailMatcher = new EmailMatcherService(config.getFuzzy().getEmailScoreThreshold());
        RiskEvaluatorService riskEvaluator = new RiskEvaluatorService(config);

        matchingService = new MatchingService(nameMatcher, emailMatcher, riskEvaluator);
    }

    @Test
    void givenMC001andKYCA_whenMatch_thenConfirmedMatchNoRisk() {
        MatchResult result = matchSingle("MC-001");
        assertEquals(MatchStatus.CONFIRMED_MATCH, result.matchStatus());
        assertFalse(result.riskFlag());
        assertEquals("KYC-A", result.kycVerificationId());
    }

    @Test
    void givenMC002andKYCB_whenMatch_thenLikelyMatchNoRisk() {
        MatchResult result = matchSingle("MC-002");
        assertEquals(MatchStatus.LIKELY_MATCH, result.matchStatus());
        assertFalse(result.riskFlag());
        assertEquals("KYC-B", result.kycVerificationId());
    }

    @Test
    void givenMC003andKYCC_whenMatch_thenNeedsReviewNoRisk() {
        // Bob ≠ Robert, no nickname resolution; email exact — no elevated risk
        MatchResult result = matchSingle("MC-003");
        assertEquals(MatchStatus.NEEDS_REVIEW, result.matchStatus());
        assertFalse(result.riskFlag(), "Name-only divergence with exact email should not set risk flag");
        assertEquals("KYC-C", result.kycVerificationId());
    }

    @Test
    void givenMC004andKYCD_whenMatch_thenNoMatchWithRisk() {
        // SSN mismatch — KYC-D has SSN 0000
        MatchResult result = matchSingle("MC-004");
        assertEquals(MatchStatus.NO_MATCH, result.matchStatus());
        assertTrue(result.riskFlag());
        assertNull(result.kycVerificationId());
    }

    @Test
    void givenMC005andKYCE_whenMatch_thenNeedsReviewWithRisk() {
        // email local parts differ significantly (dkim.work vs david.kim.personal)
        MatchResult result = matchSingle("MC-005");
        assertEquals(MatchStatus.NEEDS_REVIEW, result.matchStatus());
        assertTrue(result.riskFlag());
        assertEquals("KYC-E", result.kycVerificationId());
    }

    @Test
    void givenMC006andKYCF_whenMatch_thenNoMatchWithRisk() {
        // DOB mismatch: 1983-05-09 vs 1990-05-09
        MatchResult result = matchSingle("MC-006");
        assertEquals(MatchStatus.NO_MATCH, result.matchStatus());
        assertTrue(result.riskFlag());
        assertEquals("KYC-F", result.kycVerificationId());
    }

    @Test
    void givenMC007andKYCG_whenMatch_thenNeedsReviewWithRisk() {
        // Mike ≠ Michael, email domain scam.io is suspicious
        MatchResult result = matchSingle("MC-007");
        assertEquals(MatchStatus.NEEDS_REVIEW, result.matchStatus());
        assertTrue(result.riskFlag());
        assertEquals("KYC-G", result.kycVerificationId());
        assertTrue(result.riskReason().contains("SUSPICIOUS_EMAIL_DOMAIN"));
        assertTrue(result.riskReason().contains("EMAIL_DOMAIN_DIVERGENCE"));
    }

    @Test
    void givenEmptyCustomerList_whenMatch_thenEmptyResults() {
        List<MatchResult> results = matchingService.match(List.of(), KYC_RECORDS);
        assertTrue(results.isEmpty(), "Empty customer list should produce empty results");
    }

    @Test
    void givenEmptyKycList_whenMatch_thenAllCustomersGetNoMatchWithSsnMismatch() {
        List<MatchResult> results = matchingService.match(CUSTOMERS, List.of());
        assertEquals(CUSTOMERS.size(), results.size());
        assertTrue(results.stream().allMatch(r -> r.matchStatus() == MatchStatus.NO_MATCH),
                "All customers should get NO_MATCH when KYC list is empty");
        assertTrue(results.stream().allMatch(r -> r.riskReason().contains("SSN_MISMATCH")),
                "All should carry SSN_MISMATCH risk");
        assertTrue(results.stream().allMatch(r -> r.kycVerificationId() == null),
                "kycVerificationId should be null when no KYC record found");
    }

    @Test
    void givenMultipleKycCandidatesPassingHardGates_whenMatch_thenBestStatusChosen() {
        Customer customer = new Customer("MC-X", "John Smith", "john@gmail.com", "1990-01-01", "1234");
        KycRecord exactMatch = new KycRecord("KYC-EXACT", "John Smith", "john@gmail.com",
                "1990-01-01", "1234", "verified");
        KycRecord poorMatch = new KycRecord("KYC-POOR", "Alice Brown", "notjohn@gmail.com",
                "1990-01-01", "1234", "verified");

        List<MatchResult> results = matchingService.match(List.of(customer), List.of(exactMatch, poorMatch));

        assertEquals(1, results.size());
        assertEquals(MatchStatus.CONFIRMED_MATCH, results.get(0).matchStatus(),
                "Best matching KYC candidate should be selected");
        assertEquals("KYC-EXACT", results.get(0).kycVerificationId());
    }

    @Test
    void givenConfirmedMatch_whenMatch_thenMatchedFieldsContainsAllFour() {
        MatchResult result = matchSingle("MC-001"); // MC-001 → KYC-A: all four fields match
        String matched = result.matchedFields();
        assertTrue(matched.contains("ssn_last4"));
        assertTrue(matched.contains("dob"));
        assertTrue(matched.contains("name"));
        assertTrue(matched.contains("email"));
        assertTrue(result.divergedFields().isEmpty(),
                "divergedFields should be empty for a confirmed match");
    }

    @Test
    void givenNoSsnMatch_whenMatch_thenKycVerificationIdIsNullAndDivergedFieldContainsSsn() {
        MatchResult result = matchSingle("MC-004"); // MC-004 has no matching SSN in KYC list
        assertNull(result.kycVerificationId(), "No KYC id when SSN gate fails");
        assertTrue(result.divergedFields().contains("ssn_last4"),
                "divergedFields should contain ssn_last4 on SSN mismatch");
    }

    @Test
    void givenDobMismatch_whenMatch_thenReasoningMentionsDob() {
        MatchResult result = matchSingle("MC-006"); // MC-006 vs KYC-F: SSN matches, DOB differs
        assertTrue(result.reasoning().toLowerCase().contains("dob"),
                "Reasoning should mention DOB when DOB gate fails");
    }

    private MatchResult matchSingle(String customerId) {
        Customer customer = CUSTOMERS.stream()
                .filter(c -> c.id().equals(customerId))
                .findFirst()
                .orElseThrow();
        List<MatchResult> results = matchingService.match(List.of(customer), KYC_RECORDS);
        assertEquals(1, results.size());
        return results.get(0);
    }
}
