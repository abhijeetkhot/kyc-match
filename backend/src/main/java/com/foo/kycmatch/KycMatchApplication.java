package com.foo.kycmatch;

import com.foo.kycmatch.config.AppConfig;
import com.foo.kycmatch.model.Customer;
import com.foo.kycmatch.model.KycRecord;
import com.foo.kycmatch.model.MatchResult;
import com.foo.kycmatch.report.CsvReportWriter;
import com.foo.kycmatch.service.DataLoaderService;
import com.foo.kycmatch.service.MatchingService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;

@SpringBootApplication
public class KycMatchApplication implements CommandLineRunner {

    private final AppConfig config;
    private final DataLoaderService dataLoader;
    private final MatchingService matchingService;
    private final CsvReportWriter csvReportWriter;

    public KycMatchApplication(AppConfig config,
                               DataLoaderService dataLoader,
                               MatchingService matchingService,
                               CsvReportWriter csvReportWriter) {
        this.config = config;
        this.dataLoader = dataLoader;
        this.matchingService = matchingService;
        this.csvReportWriter = csvReportWriter;
    }

    public static void main(String[] args) {
        SpringApplication.run(KycMatchApplication.class, args);
    }

    @Override
    public void run(String... args) {
        List<Customer> customers = dataLoader.loadCustomers(config.getCustomersFilePath());
        List<KycRecord> kycRecords = dataLoader.loadKycRecords(config.getKycFilePath());

        System.out.printf("Loaded %d customers and %d KYC records%n", customers.size(), kycRecords.size());

        List<MatchResult> results = matchingService.match(customers, kycRecords);

        csvReportWriter.write(results, config.getOutputFilePath());

        System.out.printf("Report written to: %s (%d rows)%n", config.getOutputFilePath(), results.size());
    }
}
