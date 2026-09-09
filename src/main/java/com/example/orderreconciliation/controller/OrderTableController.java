package com.example.orderreconciliation.controller;

import com.example.orderreconciliation.model.Order;
import com.example.orderreconciliation.service.OrderTableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST controller for database order operations.
 */
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Order Table", description = "APIs for managing orders in the database")
public class OrderTableController {

    private final OrderTableService orderTableService;

    public OrderTableController(OrderTableService orderTableService) {
        this.orderTableService = orderTableService;
    }

    @PostMapping("/table")
    @Operation(
            summary = "Insert or update an order in the database",
            description = "If the orderId already exists, updates the record. Otherwise, inserts a new record.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "orderId": "100000000001",
                              "clientName": "ABC",
                              "stockName": "TCS",
                              "orderQty": 100.25,
                              "price": 3500.0,
                              "orderTimestamp": "2026-09-08T10:00:00",
                              "orderType": "BUY"
                            }
                            """))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Order saved successfully"),
                    @ApiResponse(responseCode = "400", description = "Validation error")
            }
    )
    public ResponseEntity<Map<String, Object>> saveOrder(@Valid @RequestBody Order order) {
        Order savedOrder = orderTableService.saveOrder(order);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Order saved successfully");
        response.put("order", savedOrder);
        return ResponseEntity.ok(response);
    }
}
