package com.example.orderreconciliation.controller;

import com.example.orderreconciliation.model.Order;
import com.example.orderreconciliation.service.ReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for reconciliation operations between database and CSV sources.
 */
@RestController
@RequestMapping("/api/v1/reconciliation")
@Tag(name = "Reconciliation", description = "APIs for reconciling database and CSV order data")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @GetMapping("/matched")
    @Operation(
            summary = "Get matched records",
            description = "Returns records that exist in both database and CSV with all fields matching.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Matched records returned successfully")
            }
    )
    public ResponseEntity<List<Order>> getMatchedRecords() {
        return ResponseEntity.ok(reconciliationService.getMatchedRecords());
    }

    @GetMapping("/table-not-matched")
    @Operation(
            summary = "Get table-not-matched records",
            description = "Returns database records whose orderId does not exist in CSV, or exists but with different field values.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Table not-matched records returned successfully")
            }
    )
    public ResponseEntity<List<Order>> getTableNotMatchedRecords() {
        return ResponseEntity.ok(reconciliationService.getTableNotMatchedRecords());
    }

    @GetMapping("/csv-not-matched")
    @Operation(
            summary = "Get CSV-not-matched records",
            description = "Returns CSV records whose orderId does not exist in database, or exists but with different field values.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "CSV not-matched records returned successfully")
            }
    )
    public ResponseEntity<List<Order>> getCsvNotMatchedRecords() {
        return ResponseEntity.ok(reconciliationService.getCsvNotMatchedRecords());
    }
}
