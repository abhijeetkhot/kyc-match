package com.foo.kycmatch.model;

public record MatchScore(boolean exactMatch, boolean fuzzyMatch) {

    public boolean isMatch() {
        return exactMatch || fuzzyMatch;
    }
}
