package com.foo.kycmatch.report;

import com.foo.kycmatch.model.MatchResult;
import com.foo.kycmatch.model.MatchStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvReportWriterTest {

    private final CsvReportWriter writer = new CsvReportWriter();

    @Test
    void givenResults_whenWrite_thenRowCountMatchesPlusHeader(@TempDir Path tempDir) throws IOException {
        List<MatchResult> results = sampleResults();
        Path output = tempDir.resolve("report.csv");

        writer.write(results, output.toString());

        List<String> lines = Files.readAllLines(output);
        // 1 header + n data rows
        assertEquals(results.size() + 1, lines.size());
    }

    @Test
    void givenResults_whenWrite_thenHeaderPresentAsFirstLine(@TempDir Path tempDir) throws IOException {
        Path output = tempDir.resolve("report.csv");
        writer.write(sampleResults(), output.toString());

        String firstLine = Files.readAllLines(output).get(0);
        assertTrue(firstLine.contains("customer_id"));
        assertTrue(firstLine.contains("match_status"));
        assertTrue(firstLine.contains("risk_flag"));
    }

    @Test
    void givenReasoningWithComma_whenWrite_thenFieldIsProperlyQuoted(@TempDir Path tempDir) throws IOException {
        String reasoningWithComma = "SSN and DOB matched, name diverged, email exact.";
        MatchResult result = new MatchResult(
                "MC-003", "KYC-C", MatchStatus.NEEDS_REVIEW,
                "ssn_last4|dob|email", "name",
                reasoningWithComma,
                false, ""
        );
        Path output = tempDir.resolve("report.csv");
        writer.write(List.of(result), output.toString());

        String content = Files.readString(output);
        // opencsv wraps fields containing commas in double-quotes
        assertTrue(content.contains("\"" + reasoningWithComma + "\""),
                "Field with comma must be wrapped in double quotes");
    }

    @Test
    void givenNoMatchResult_whenWrite_thenEmptyKycVerificationIdWritten(@TempDir Path tempDir) throws IOException {
        MatchResult noMatch = new MatchResult(
                "MC-004", null, MatchStatus.NO_MATCH,
                "", "ssn_last4",
                "No KYC record found with matching SSN",
                true, "SSN_MISMATCH"
        );
        Path output = tempDir.resolve("report.csv");
        writer.write(List.of(noMatch), output.toString());

        List<String> lines = Files.readAllLines(output);
        assertEquals(2, lines.size());
        // data row should not contain "null"
        assertFalse(lines.get(1).contains("null"));
    }

    private List<MatchResult> sampleResults() {
        return List.of(
                new MatchResult("MC-001", "KYC-A", MatchStatus.CONFIRMED_MATCH,
                        "ssn_last4|dob|name|email", "",
                        "SSN and DOB matched. Name: exact match. Email: exact match. Classification: confirmed match.",
                        false, ""),
                new MatchResult("MC-002", "KYC-B", MatchStatus.LIKELY_MATCH,
                        "ssn_last4|dob|name", "email",
                        "SSN and DOB matched. Name: exact match. Email: fuzzy match. Classification: likely match.",
                        false, ""),
                new MatchResult("MC-004", null, MatchStatus.NO_MATCH,
                        "", "ssn_last4",
                        "No KYC record found with matching SSN",
                        true, "SSN_MISMATCH")
        );
    }
}
