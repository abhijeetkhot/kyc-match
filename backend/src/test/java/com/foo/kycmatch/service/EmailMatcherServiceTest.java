package com.foo.kycmatch.service;

import com.foo.kycmatch.model.MatchScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailMatcherServiceTest {

    private EmailMatcherService service;

    @BeforeEach
    void setUp() {
        service = new EmailMatcherService(0.35);
    }

    @Test
    void givenIdenticalEmails_whenMatch_thenExactMatch() {
        MatchScore score = service.match("jsullivan@gmail.com", "jsullivan@gmail.com");
        assertTrue(score.exactMatch());
        assertFalse(score.fuzzyMatch());
    }

    @Test
    void givenCaseDifference_whenMatch_thenExactMatch() {
        MatchScore score = service.match("JsulLivan@Gmail.COM", "jsullivan@gmail.com");
        assertTrue(score.exactMatch());
    }

    @Test
    void givenLocalPartPrefixSubset_whenMatch_thenFuzzyMatch() {
        // priya.n is a prefix-subset of priya.nair
        MatchScore score = service.match("priya.nair@outlook.com", "priya.n@outlook.com");
        assertFalse(score.exactMatch());
        assertTrue(score.fuzzyMatch(), "Token prefix subset should be a fuzzy match");
    }

    @Test
    void givenDomainDivergence_whenMatch_thenNoMatch() {
        MatchScore score = service.match("user@gmail.com", "user@yahoo.com");
        assertFalse(score.exactMatch());
        assertFalse(score.fuzzyMatch());
    }

    @Test
    void givenSameDomainVeryDifferentLocal_whenMatch_thenNoMatch() {
        // dkim.work vs david.kim.personal — well outside Levenshtein threshold
        MatchScore score = service.match("dkim.work@gmail.com", "david.kim.personal@gmail.com");
        assertFalse(score.exactMatch());
        assertFalse(score.fuzzyMatch());
    }

    @Test
    void givenScamIoDomain_whenDomainsDiffer_thenNoMatch() {
        MatchScore score = service.match("m.patel@gmail.com", "mpatel_fake@scam.io");
        assertFalse(score.exactMatch());
        assertFalse(score.fuzzyMatch());
    }

    @Test
    void givenEmailMissingAtSymbol_whenMatch_thenNoMatch() {
        MatchScore score = service.match("nodomain.com", "user@gmail.com");
        assertFalse(score.exactMatch());
        assertFalse(score.fuzzyMatch(), "Email without @ symbol should never match");
    }

    @Test
    void givenBothEmailsMissingAtSymbol_whenMatch_thenNoMatch() {
        MatchScore score = service.match("nodomain.com", "alsonodomain.com");
        assertFalse(score.exactMatch());
        assertFalse(score.fuzzyMatch(), "Emails without @ should not match even if similar");
    }

    @Test
    void givenTokenPrefixSubset_whenMatch_thenFuzzyMatch() {
        // j.smith → tokens ["j","smith"]; john.smith → tokens ["john","smith"]
        // shorter=["j","smith"], "john".startsWith("j")=true, "smith".startsWith("smith")=true
        MatchScore score = service.match("j.smith@gmail.com", "john.smith@gmail.com");
        assertFalse(score.exactMatch());
        assertTrue(score.fuzzyMatch(), "Token prefix subset should qualify as fuzzy match");
    }

    @Test
    void givenSameDomainSmallLevenshteinDiff_whenMatch_thenFuzzyMatch() {
        // local "userx" vs "usery": distance=1, maxLen=5, ratio=0.20 < 0.35
        MatchScore score = service.match("userx@gmail.com", "usery@gmail.com");
        assertFalse(score.exactMatch());
        assertTrue(score.fuzzyMatch(), "Small Levenshtein difference on same domain should be fuzzy");
    }

    @Test
    void givenLevenshteinRatioExactlyAtEmailThreshold_whenMatch_thenNoFuzzyMatch() {
        // local1="aaaaaaaaaaaaaaaaaaaa" (20 a's), local2="aaaaaaaaaaaaabbbbbbb" (13 a's + 7 b's)
        // distance=7, maxLen=20, ratio=0.35 — not strictly less than threshold
        MatchScore score = service.match(
                "aaaaaaaaaaaaaaaaaaaa@gmail.com",
                "aaaaaaaaaaaaabbbbbbb@gmail.com");
        assertFalse(score.exactMatch());
        assertFalse(score.fuzzyMatch(), "Levenshtein ratio exactly at threshold should not qualify as fuzzy");
    }

    @Test
    void givenUpperCaseDomainInOneEmail_whenMatch_thenNormalizedAndExactMatch() {
        // Both emails are lowercased before comparison
        MatchScore score = service.match("user@Gmail.COM", "user@gmail.com");
        assertTrue(score.exactMatch(), "Domain case difference should be normalized before comparison");
    }
}
