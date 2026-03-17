package com.foo.kycmatch.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foo.kycmatch.exception.DataLoadException;
import com.foo.kycmatch.model.Customer;
import com.foo.kycmatch.model.KycRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class DataLoaderService {

    private static final Logger log = LoggerFactory.getLogger(DataLoaderService.class);

    private final ObjectMapper objectMapper;

    public DataLoaderService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<Customer> loadCustomers(String filePath) {
        try {
            var resource = new ClassPathResource(filePath);
            List<Customer> list = objectMapper.readValue(resource.getInputStream(), new TypeReference<List<Customer>>() {});
            log.info("Loaded {} customers from {}", list.size(), filePath);
            return list;
        } catch (IOException e) {
            throw new DataLoadException("Failed to load customers from: " + filePath, e);
        }
    }

    public List<KycRecord> loadKycRecords(String filePath) {
        try {
            var resource = new ClassPathResource(filePath);
            List<KycRecord> list = objectMapper.readValue(resource.getInputStream(), new TypeReference<List<KycRecord>>() {});
            log.info("Loaded {} KYC records from {}", list.size(), filePath);
            return list;
        } catch (IOException e) {
            throw new DataLoadException("Failed to load KYC records from: " + filePath, e);
        }
    }
}
