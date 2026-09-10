package com.example.orderreconciliation.controller;

import com.example.orderreconciliation.model.Order;
import com.example.orderreconciliation.service.OrderCsvService;
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

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "CSV Upload", description = "APIs for uploading order data via CSV")
public class CsvController {

    private final OrderCsvService orderCsvService;

    public CsvController(OrderCsvService orderCsvService) {
        this.orderCsvService = orderCsvService;
    }

    @PostMapping(value = "/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload a CSV file containing orders",
            description = "Validates the header, skips invalid data rows, keeps valid rows, and writes the valid rows to the backend CSV file.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "CSV processed successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid CSV header or file")
            }
    )
    public ResponseEntity<Map<String, Object>> uploadCsv(@RequestParam("file") MultipartFile file) {
        List<Order> orders = orderCsvService.uploadCsv(file);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "CSV processed successfully");
        response.put("recordCount", orders.size());
        response.put("orders", orders);
        return ResponseEntity.ok(response);
    }
}
