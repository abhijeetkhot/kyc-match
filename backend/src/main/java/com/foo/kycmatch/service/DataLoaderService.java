package com.foo.kycmatch.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foo.kycmatch.exception.DataLoadException;
import com.foo.kycmatch.model.Customer;
import com.foo.kycmatch.model.KycRecord;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class DataLoaderService {

    private final ObjectMapper objectMapper;

    public DataLoaderService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<Customer> loadCustomers(String filePath) {
        try {
            var resource = new ClassPathResource(filePath);
            return objectMapper.readValue(resource.getInputStream(), new TypeReference<List<Customer>>() {});
        } catch (IOException e) {
            throw new DataLoadException("Failed to load customers from: " + filePath, e);
        }
    }

    public List<KycRecord> loadKycRecords(String filePath) {
        try {
            var resource = new ClassPathResource(filePath);
            return objectMapper.readValue(resource.getInputStream(), new TypeReference<List<KycRecord>>() {});
        } catch (IOException e) {
            throw new DataLoadException("Failed to load KYC records from: " + filePath, e);
        }
    }
}
