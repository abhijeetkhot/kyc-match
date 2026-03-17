package com.foo.kycmatch.service;

import com.foo.kycmatch.model.MatchScore;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.stream.Collectors;

@Service
public class NameMatcherService {

    private final double nameScoreThreshold;
    private final LevenshteinDistance levenshtein = new LevenshteinDistance();

    public NameMatcherService(@Value("${kyc.fuzzy.name-score-threshold}") double nameScoreThreshold) {
        this.nameScoreThreshold = nameScoreThreshold;
    }

    public MatchScore match(String customerName, String kycName) {
        String normalized1 = normalize(customerName);
        String normalized2 = normalize(kycName);

        if (normalized1.equals(normalized2)) {
            return new MatchScore(true, false);
        }

        int maxLen = Math.max(normalized1.length(), normalized2.length());
        if (maxLen == 0) {
            return new MatchScore(true, false);
        }

        int distance = levenshtein.apply(normalized1, normalized2);
        double ratio = (double) distance / maxLen;

        if (ratio < nameScoreThreshold) {
            return new MatchScore(false, true);
        }

        return new MatchScore(false, false);
    }

    private String normalize(String name) {
        String lower = name.toLowerCase();
        // strip all non-letter, non-space characters
        String stripped = lower.replaceAll("[^a-z ]", "");
        // split on whitespace, filter out single-char tokens (initials)
        return Arrays.stream(stripped.split("\\s+"))
                .filter(token -> token.length() > 1)
                .collect(Collectors.joining(" "))
                .trim();
    }
}
