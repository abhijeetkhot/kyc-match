package com.foo.kycmatch.service;

import com.foo.kycmatch.model.MatchScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NameMatcherServiceTest {

    private NameMatcherService service;

    @BeforeEach
    void setUp() {
        service = new NameMatcherService(0.20);
    }

    @Test
    void givenNameWithMiddleInitial_whenMatch_thenInitialDroppedAndExactMatch() {
        MatchScore score = service.match("James R. Sullivan", "James Sullivan");
        assertTrue(score.exactMatch(), "Middle initial should be stripped before comparison");
        assertFalse(score.fuzzyMatch());
    }

    @Test
    void givenDifferentCasing_whenMatch_thenNormalizedToExactMatch() {
        MatchScore score = service.match("Priya Nair", "PRIYA NAIR");
        assertTrue(score.exactMatch());
    }

    @Test
    void givenIdenticalNames_whenMatch_thenExactMatch() {
        MatchScore score = service.match("David Kim", "David Kim");
        assertTrue(score.exactMatch());
        assertFalse(score.fuzzyMatch());
    }

    @Test
    void givenNicknameVsFullName_whenMatch_thenNoMatch() {
        // Bob vs Robert — no nickname resolution per spec
        MatchScore score = service.match("Robert Chen", "Bob Chen");
        assertFalse(score.exactMatch());
        assertFalse(score.fuzzyMatch());
    }

    @Test
    void givenNicknameVsFullNameMichael_whenMatch_thenNoMatch() {
        // Michael vs Mike — no nickname resolution per spec
        MatchScore score = service.match("Michael Patel", "Mike Patel");
        assertFalse(score.exactMatch());
        assertFalse(score.fuzzyMatch());
    }

    @Test
    void givenVeryCloseNames_whenMatch_thenFuzzyMatch() {
        // Small typo: "Jon Smith" vs "John Smith" — distance 1, ratio = 1/10 = 0.10 < 0.20
        MatchScore score = service.match("Jon Smith", "John Smith");
        assertFalse(score.exactMatch());
        assertTrue(score.fuzzyMatch(), "Single-character typo should be within the fuzzy threshold");
    }

    @Test
    void givenFullyDifferentNames_whenMatch_thenNoMatch() {
        MatchScore score = service.match("Alice Brown", "Carlos Diaz");
        assertFalse(score.exactMatch());
        assertFalse(score.fuzzyMatch());
    }

    @Test
    void givenBothEmptyStrings_whenMatch_thenExactMatch() {
        MatchScore score = service.match("", "");
        assertTrue(score.exactMatch(), "Both empty strings normalize to equal empty strings");
        assertFalse(score.fuzzyMatch());
    }

    @Test
    void givenNameWithOnlyInitials_whenMatch_thenBothNormalizeToEmptyAndExactMatch() {
        // Single-char tokens are stripped — "A. B. C." and "X. Y." both become ""
        MatchScore score = service.match("A. B. C.", "X. Y.");
        assertTrue(score.exactMatch(), "Names consisting only of initials should both normalize to empty");
    }

    @Test
    void givenNameWithOnlyPunctuation_whenMatch_thenBothNormalizeToEmptyAndExactMatch() {
        MatchScore score = service.match("---", "...");
        assertTrue(score.exactMatch(), "Names with only punctuation strip to empty string");
    }

    @Test
    void givenMultipleMiddleInitials_whenMatch_thenAllInitialsDropped() {
        // "James A. B. Sullivan" → normalize → "james sullivan"
        MatchScore score = service.match("James A. B. Sullivan", "James Sullivan");
        assertTrue(score.exactMatch(), "Multiple middle initials should all be stripped");
    }

    @Test
    void givenNameWithNumbers_whenMatch_thenNumbersAreStripped() {
        // [^a-z ] regex removes digits before comparison
        MatchScore score = service.match("John2 Smith3", "John Smith");
        assertTrue(score.exactMatch(), "Numbers in names should be stripped before comparison");
    }

    @Test
    void givenNameWithExtraWhitespace_whenMatch_thenNormalized() {
        MatchScore score = service.match("John  Smith", "John Smith");
        assertTrue(score.exactMatch(), "Extra whitespace should be collapsed during normalization");
    }

    @Test
    void givenRatioExactlyAtThreshold_whenMatch_thenNoFuzzyMatch() {
        // Normalize: "johns" (5 chars) vs "jxhns" (5 chars)
        // Levenshtein distance=1, maxLen=5, ratio=0.20 — not strictly less than threshold
        MatchScore score = service.match("Johns", "Jxhns");
        assertFalse(score.exactMatch());
        assertFalse(score.fuzzyMatch(), "Ratio exactly at threshold (0.20) should not qualify as fuzzy");
    }

    @Test
    void givenRatioJustBelowThreshold_whenMatch_thenFuzzyMatch() {
        // Normalize: "johnss" (6 chars) vs "jxhnss" (6 chars)
        // Levenshtein distance=1, maxLen=6, ratio≈0.167 — strictly less than 0.20
        MatchScore score = service.match("Johnss", "Jxhnss");
        assertFalse(score.exactMatch());
        assertTrue(score.fuzzyMatch(), "Ratio just below threshold should qualify as fuzzy");
    }
}
