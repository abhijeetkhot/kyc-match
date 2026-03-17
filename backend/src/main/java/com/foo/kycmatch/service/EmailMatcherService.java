package com.foo.kycmatch.service;

import com.foo.kycmatch.model.MatchScore;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class EmailMatcherService {

    private final double emailScoreThreshold;
    private final LevenshteinDistance levenshtein = new LevenshteinDistance();

    public EmailMatcherService(@Value("${kyc.fuzzy.email-score-threshold}") double emailScoreThreshold) {
        this.emailScoreThreshold = emailScoreThreshold;
    }

    public MatchScore match(String customerEmail, String kycEmail) {
        String e1 = customerEmail.toLowerCase();
        String e2 = kycEmail.toLowerCase();

        if (e1.equals(e2)) {
            return new MatchScore(true, false);
        }

        String[] parts1 = e1.split("@", 2);
        String[] parts2 = e2.split("@", 2);

        if (parts1.length < 2 || parts2.length < 2) {
            return new MatchScore(false, false);
        }

        String local1 = parts1[0];
        String domain1 = parts1[1];
        String local2 = parts2[0];
        String domain2 = parts2[1];

        // domains must match for any fuzzy consideration
        if (!domain1.equals(domain2)) {
            return new MatchScore(false, false);
        }

        // same domain, local parts differ — try token-based prefix subset match
        List<String> tokens1 = Arrays.asList(local1.split("[._]"));
        List<String> tokens2 = Arrays.asList(local2.split("[._]"));

        List<String> shorter = tokens1.size() <= tokens2.size() ? tokens1 : tokens2;
        List<String> longer  = tokens1.size() <= tokens2.size() ? tokens2 : tokens1;

        boolean prefixSubset = shorter.stream()
                .allMatch(s -> longer.stream().anyMatch(l -> l.startsWith(s)));

        if (prefixSubset) {
            return new MatchScore(false, true);
        }

        // Levenshtein fallback on local parts
        int maxLen = Math.max(local1.length(), local2.length());
        if (maxLen == 0) {
            return new MatchScore(true, false);
        }
        int distance = levenshtein.apply(local1, local2);
        double ratio = (double) distance / maxLen;

        if (ratio < emailScoreThreshold) {
            return new MatchScore(false, true);
        }

        return new MatchScore(false, false);
    }
}
