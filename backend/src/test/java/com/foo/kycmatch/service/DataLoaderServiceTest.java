package com.foo.kycmatch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foo.kycmatch.exception.DataLoadException;
import com.foo.kycmatch.model.Customer;
import com.foo.kycmatch.model.KycRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DataLoaderServiceTest {

    private DataLoaderService service;

    @BeforeEach
    void setUp() {
        service = new DataLoaderService(new ObjectMapper());
    }

    @Test
    void givenValidCustomersFile_whenLoadCustomers_thenReturnsAllRecords() {
        List<Customer> customers = service.loadCustomers("data/customers.json");
        assertEquals(7, customers.size());
        assertTrue(customers.stream().anyMatch(c -> c.id().equals("MC-001")),
                "Should contain MC-001");
    }

    @Test
    void givenValidKycFile_whenLoadKycRecords_thenReturnsAllRecords() {
        List<KycRecord> records = service.loadKycRecords("data/kyc_verified.json");
        assertEquals(7, records.size());
        assertTrue(records.stream().anyMatch(k -> k.verificationId().equals("KYC-A")),
                "Should contain KYC-A");
    }

    @Test
    void givenNonExistentFile_whenLoadCustomers_thenThrowsDataLoadException() {
        DataLoadException ex = assertThrows(DataLoadException.class,
                () -> service.loadCustomers("data/nonexistent.json"));
        assertTrue(ex.getMessage().contains("data/nonexistent.json"),
                "Exception message should contain the problematic path");
    }

    @Test
    void givenNonExistentFile_whenLoadKycRecords_thenThrowsDataLoadException() {
        assertThrows(DataLoadException.class,
                () -> service.loadKycRecords("data/nonexistent_kyc.json"));
    }

    @Test
    void givenMalformedJson_whenLoadCustomers_thenThrowsDataLoadException() {
        // data/malformed_customers.json is in src/test/resources with invalid JSON content
        assertThrows(DataLoadException.class,
                () -> service.loadCustomers("data/malformed_customers.json"));
    }
}
