package com.example.orderreconciliation.service;

import com.example.orderreconciliation.csv.CsvFileHandler;
import com.example.orderreconciliation.exception.InvalidCsvException;
import com.example.orderreconciliation.model.Order;
import com.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.util.List;

/**
 * Service handling CSV upload, validation, and write operations.
 */
@Service
public class CsvService {

    private static final Logger log = LoggerFactory.getLogger(CsvService.class);

    private final CsvFileHandler csvFileHandler;

    public CsvService(CsvFileHandler csvFileHandler) {
        this.csvFileHandler = csvFileHandler;
    }

    /**
     * Processes an uploaded CSV file:
     * 1. Validates file presence and extension
     * 2. Reads and parses CSV content
     * 3. Validates all rows (header, fields, duplicates)
     * 4. Writes validated orders to the configured CSV file using temp file strategy
     */
    public List<Order> processUploadedCsv(MultipartFile file) {
        validateFile(file);

        log.info("CSV upload started. File: {}, Size: {} bytes", file.getOriginalFilename(), file.getSize());

        List<String[]> allRows;
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            allRows = reader.readAll();
        } catch (Exception e) {
            throw new InvalidCsvException("Failed to read uploaded CSV file: " + e.getMessage());
        }

        // Parse and validate all rows (header, fields, duplicates)
        List<Order> orders = csvFileHandler.parseAndValidate(allRows);

        log.info("CSV validation completed. {} valid records found.", orders.size());

        // Write validated orders to the CSV file
        csvFileHandler.writeOrders(orders);

        log.info("CSV file updated successfully with {} records.", orders.size());

        return orders;
    }

    /**
     * Reads all orders from the existing CSV file.
     */
    public List<Order> getAllOrders() {
        return csvFileHandler.readOrders();
    }

    /**
     * Validates the uploaded file is present and has a .csv extension.
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidCsvException("CSV file is required and must not be empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".csv")) {
            throw new InvalidCsvException("File must have a .csv extension. Received: " + originalFilename);
        }
    }
}
