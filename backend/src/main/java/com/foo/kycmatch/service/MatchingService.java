package com.foo.kycmatch.service;

import com.foo.kycmatch.model.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MatchingService {

    private final NameMatcherService nameMatcher;
    private final EmailMatcherService emailMatcher;
    private final RiskEvaluatorService riskEvaluator;

    public MatchingService(NameMatcherService nameMatcher,
                           EmailMatcherService emailMatcher,
                           RiskEvaluatorService riskEvaluator) {
        this.nameMatcher = nameMatcher;
        this.emailMatcher = emailMatcher;
        this.riskEvaluator = riskEvaluator;
    }

    public List<MatchResult> match(List<Customer> customers, List<KycRecord> kycRecords) {
        List<MatchResult> results = new ArrayList<>();

        for (Customer customer : customers) {
            results.add(matchCustomer(customer, kycRecords));
        }

        return results;
    }

    private MatchResult matchCustomer(Customer customer, List<KycRecord> kycRecords) {
        // Hard gate 1: SSN
        List<KycRecord> ssnCandidates = kycRecords.stream()
                .filter(k -> k.ssnLast4().equals(customer.ssnLast4()))
                .toList();

        if (ssnCandidates.isEmpty()) {
            List<String> risks = riskEvaluator.evaluateNoSsnMatch();
            return new MatchResult(
                    customer.id(), null, MatchStatus.NO_MATCH,
                    "", "ssn_last4",
                    "No KYC record found with matching SSN",
                    true, String.join("|", risks)
            );
        }

        // Hard gate 2: DOB
        List<KycRecord> dobCandidates = ssnCandidates.stream()
                .filter(k -> k.dob().equals(customer.dob()))
                .toList();

        if (dobCandidates.isEmpty()) {
            KycRecord ssnMatch = ssnCandidates.get(0);
            List<String> risks = riskEvaluator.evaluateDobMismatch();
            return new MatchResult(
                    customer.id(), ssnMatch.verificationId(), MatchStatus.NO_MATCH,
                    "ssn_last4", "dob",
                    "SSN matched but DOB did not match",
                    true, String.join("|", risks)
            );
        }

        // Score soft fields against each candidate that passed hard gates; take best result
        MatchResult best = null;
        for (KycRecord kyc : dobCandidates) {
            MatchResult candidate = scoreCandidate(customer, kyc);
            if (best == null || candidate.matchStatus().ordinal() < best.matchStatus().ordinal()) {
                best = candidate;
            }
        }
        return best;
    }

    private MatchResult scoreCandidate(Customer customer, KycRecord kyc) {
        MatchScore nameScore  = nameMatcher.match(customer.name(), kyc.fullName());
        MatchScore emailScore = emailMatcher.match(customer.email(), kyc.email());

        MatchStatus status = classify(nameScore, emailScore);

        List<String> matchedFields  = new ArrayList<>(List.of("ssn_last4", "dob"));
        List<String> divergedFields = new ArrayList<>();

        if (nameScore.exactMatch()) {
            matchedFields.add("name");
        } else {
            divergedFields.add("name");
        }

        if (emailScore.exactMatch()) {
            matchedFields.add("email");
        } else {
            divergedFields.add("email");
        }

        String reasoning = buildReasoning(nameScore, emailScore, status);

        List<String> risks = riskEvaluator.evaluate(customer, kyc, status, emailScore);
        boolean riskFlag = !risks.isEmpty();

        return new MatchResult(
                customer.id(),
                kyc.verificationId(),
                status,
                String.join("|", matchedFields),
                String.join("|", divergedFields),
                reasoning,
                riskFlag,
                String.join("|", risks)
        );
    }

    private MatchStatus classify(MatchScore nameScore, MatchScore emailScore) {
        if (nameScore.exactMatch() && emailScore.exactMatch()) {
            return MatchStatus.CONFIRMED_MATCH;
        }
        if (nameScore.isMatch() && emailScore.isMatch()) {
            return MatchStatus.LIKELY_MATCH;
        }
        return MatchStatus.NEEDS_REVIEW;
    }

    private String buildReasoning(MatchScore nameScore, MatchScore emailScore, MatchStatus status) {
        StringBuilder sb = new StringBuilder();
        sb.append("SSN and DOB matched. ");

        if (nameScore.exactMatch()) {
            sb.append("Name: exact match. ");
        } else if (nameScore.fuzzyMatch()) {
            sb.append("Name: fuzzy match. ");
        } else {
            sb.append("Name: no match. ");
        }

        if (emailScore.exactMatch()) {
            sb.append("Email: exact match. ");
        } else if (emailScore.fuzzyMatch()) {
            sb.append("Email: fuzzy match. ");
        } else {
            sb.append("Email: no match. ");
        }

        sb.append("Classification: ").append(status.name().toLowerCase().replace('_', ' ')).append(".");
        return sb.toString().trim();
    }
}
