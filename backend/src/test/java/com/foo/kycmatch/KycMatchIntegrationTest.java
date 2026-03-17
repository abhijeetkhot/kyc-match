package com.foo.kycmatch;

import com.foo.kycmatch.config.AppConfig;
import com.foo.kycmatch.model.Customer;
import com.foo.kycmatch.model.KycRecord;
import com.foo.kycmatch.model.MatchResult;
import com.foo.kycmatch.model.MatchStatus;
import com.foo.kycmatch.service.DataLoaderService;
import com.foo.kycmatch.service.MatchingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class KycMatchIntegrationTest {

    @Autowired AppConfig config;
    @Autowired DataLoaderService dataLoader;
    @Autowired MatchingService matchingService;

    private List<MatchResult> results;

    @BeforeEach
    void runPipeline() {
        List<Customer> customers = dataLoader.loadCustomers(config.getCustomersFilePath());
        List<KycRecord> kycRecords = dataLoader.loadKycRecords(config.getKycFilePath());
        results = matchingService.match(customers, kycRecords);
    }

    @Test
    void pipelineProducesOneResultPerCustomer() {
        assertEquals(7, results.size());
    }

    @Test
    void mc001_confirmedMatchNoRisk() {
        MatchResult r = find("MC-001");
        assertEquals(MatchStatus.CONFIRMED_MATCH, r.matchStatus());
        assertFalse(r.riskFlag());
    }

    @Test
    void mc004_noMatchSsnMismatch() {
        MatchResult r = find("MC-004");
        assertEquals(MatchStatus.NO_MATCH, r.matchStatus());
        assertTrue(r.riskReason().contains("SSN_MISMATCH"));
    }

    @Test
    void mc007_needsReviewSuspiciousDomain() {
        MatchResult r = find("MC-007");
        assertEquals(MatchStatus.NEEDS_REVIEW, r.matchStatus());
        assertTrue(r.riskReason().contains("SUSPICIOUS_EMAIL_DOMAIN"));
    }

    private MatchResult find(String id) {
        return results.stream().filter(r -> r.customerId().equals(id)).findFirst().orElseThrow();
    }
}
