package com.foo.kycmatch.report;

import com.foo.kycmatch.exception.ReportWriteException;
import com.foo.kycmatch.model.MatchResult;
import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@Component
public class CsvReportWriter {

    private static final Logger log = LoggerFactory.getLogger(CsvReportWriter.class);

    private static final String[] HEADER = {
            "customer_id", "kyc_verification_id", "match_status",
            "matched_fields", "diverged_fields", "reasoning",
            "risk_flag", "risk_reason"
    };

    public void write(List<MatchResult> results, String outputFilePath) {
        File outputFile = new File(outputFilePath);
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new ReportWriteException(
                    "Failed to create output directory: " + parent.getAbsolutePath(), null);
        }

        try (CSVWriter writer = new CSVWriter(new FileWriter(outputFile))) {
            writer.writeNext(HEADER);
            for (MatchResult r : results) {
                writer.writeNext(toRow(r));
            }
        } catch (IOException e) {
            throw new ReportWriteException("Failed to write CSV report to: " + outputFilePath, e);
        }
        log.info("Wrote {} rows to {}", results.size(), outputFilePath);
    }

    private String[] toRow(MatchResult r) {
        return new String[]{
                r.customerId(),
                r.kycVerificationId() != null ? r.kycVerificationId() : "",
                r.matchStatus().name().toLowerCase(),
                r.matchedFields(),
                r.divergedFields(),
                r.reasoning(),
                String.valueOf(r.riskFlag()),
                r.riskReason()
        };
    }
}
