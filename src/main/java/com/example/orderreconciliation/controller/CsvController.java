package com.example.orderreconciliation.controller;

import com.example.orderreconciliation.model.Order;
import com.example.orderreconciliation.service.CsvService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for CSV file upload operations.
 */
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "CSV Upload", description = "APIs for uploading order data via CSV")
public class CsvController {

    private final CsvService csvService;

    public CsvController(CsvService csvService) {
        this.csvService = csvService;
    }

    @PostMapping(value = "/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload a CSV file containing orders",
            description = "Validates the CSV file, checks all rows, detects duplicates, and writes to the configured CSV location.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "CSV processed successfully"),
                    @ApiResponse(responseCode = "400", description = "CSV validation error")
            }
    )
    public ResponseEntity<Map<String, Object>> uploadCsv(@RequestParam("file") MultipartFile file) {
        List<Order> orders = csvService.processUploadedCsv(file);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "CSV processed successfully");
        response.put("recordCount", orders.size());
        response.put("orders", orders);
        return ResponseEntity.ok(response);
    }
}
