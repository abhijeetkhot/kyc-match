package com.foo.kycmatch.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Configuration
@Component
@ConfigurationProperties(prefix = "kyc")
public class AppConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    private String customersFilePath;
    private String kycFilePath;
    private String outputFilePath;
    private Fuzzy fuzzy = new Fuzzy();
    private Risk risk = new Risk();

    public String getCustomersFilePath() { return customersFilePath; }
    public void setCustomersFilePath(String customersFilePath) { this.customersFilePath = customersFilePath; }

    public String getKycFilePath() { return kycFilePath; }
    public void setKycFilePath(String kycFilePath) { this.kycFilePath = kycFilePath; }

    public String getOutputFilePath() { return outputFilePath; }
    public void setOutputFilePath(String outputFilePath) { this.outputFilePath = outputFilePath; }

    public Fuzzy getFuzzy() { return fuzzy; }
    public void setFuzzy(Fuzzy fuzzy) { this.fuzzy = fuzzy; }

    public Risk getRisk() { return risk; }
    public void setRisk(Risk risk) { this.risk = risk; }

    public static class Fuzzy {
        private double nameScoreThreshold = 0.20;
        private double emailScoreThreshold = 0.35;

        public double getNameScoreThreshold() { return nameScoreThreshold; }
        public void setNameScoreThreshold(double nameScoreThreshold) { this.nameScoreThreshold = nameScoreThreshold; }

        public double getEmailScoreThreshold() { return emailScoreThreshold; }
        public void setEmailScoreThreshold(double emailScoreThreshold) { this.emailScoreThreshold = emailScoreThreshold; }
    }

    public static class Risk {
        private List<String> suspiciousDomains = new ArrayList<>();

        public List<String> getSuspiciousDomains() { return suspiciousDomains; }
        public void setSuspiciousDomains(List<String> suspiciousDomains) { this.suspiciousDomains = suspiciousDomains; }
    }
}
