package com.example.orderreconciliation.service;

import com.example.orderreconciliation.dto.ReconciliationResult;
import com.example.orderreconciliation.enums.OrderType;
import com.example.orderreconciliation.model.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private OrderTableService orderTableService;

    @Mock
    private CsvService csvService;

    @InjectMocks
    private ReconciliationService reconciliationService;

    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2026, 9, 8, 10, 0, 0);

    private Order createOrder(String orderId, String clientName, String stockName,
                              BigDecimal orderQty, BigDecimal price,
                              LocalDateTime timestamp, OrderType orderType) {
        return new Order(orderId, clientName, stockName, orderQty, price, timestamp, orderType);
    }

    // --- Test A: Both records exist and all fields match ---

    @Test
    @DisplayName("A. Both records exist and all fields match - should be MATCHED")
    void reconcile_bothExistAllFieldsMatch() {
        Order tableOrder = createOrder("100000000001", "ABC", "TCS",
                new BigDecimal("100.25"), new BigDecimal("3500.0"), TIMESTAMP, OrderType.BUY);
        Order csvOrder = createOrder("100000000001", "ABC", "TCS",
                new BigDecimal("100.25"), new BigDecimal("3500.0"), TIMESTAMP, OrderType.BUY);

        when(orderTableService.getAllOrders()).thenReturn(List.of(tableOrder));
        when(csvService.getAllOrders()).thenReturn(List.of(csvOrder));

        ReconciliationResult result = reconciliationService.reconcile();

        assertEquals(1, result.getMatchedRecords().size());
        assertEquals(0, result.getTableNotMatchedRecords().size());
        assertEquals(0, result.getCsvNotMatchedRecords().size());
    }

    // --- Test B: Record exists only in DB ---

    @Test
    @DisplayName("B. Record exists only in DB - should be TABLE NOT MATCHED")
    void reconcile_recordOnlyInDb() {
        Order tableOrder = createOrder("100000000001", "ABC", "TCS",
                new BigDecimal("100.25"), new BigDecimal("3500.0"), TIMESTAMP, OrderType.BUY);

        when(orderTableService.getAllOrders()).thenReturn(List.of(tableOrder));
        when(csvService.getAllOrders()).thenReturn(Collections.emptyList());

        ReconciliationResult result = reconciliationService.reconcile();

        assertEquals(0, result.getMatchedRecords().size());
        assertEquals(1, result.getTableNotMatchedRecords().size());
        assertEquals(0, result.getCsvNotMatchedRecords().size());
    }

    // --- Test C: Record exists only in CSV ---

    @Test
    @DisplayName("C. Record exists only in CSV - should be CSV NOT MATCHED")
    void reconcile_recordOnlyInCsv() {
        Order csvOrder = createOrder("100000000001", "ABC", "TCS",
                new BigDecimal("100.25"), new BigDecimal("3500.0"), TIMESTAMP, OrderType.BUY);

        when(orderTableService.getAllOrders()).thenReturn(Collections.emptyList());
        when(csvService.getAllOrders()).thenReturn(List.of(csvOrder));

        ReconciliationResult result = reconciliationService.reconcile();

        assertEquals(0, result.getMatchedRecords().size());
        assertEquals(0, result.getTableNotMatchedRecords().size());
        assertEquals(1, result.getCsvNotMatchedRecords().size());
    }

    // --- Test D: Same orderId but different quantity ---

    @Test
    @DisplayName("D. Same orderId but different quantity - both should be NOT MATCHED")
    void reconcile_sameOrderIdDifferentQuantity() {
        Order tableOrder = createOrder("100000000001", "ABC", "TCS",
                new BigDecimal("100.25"), new BigDecimal("3500.0"), TIMESTAMP, OrderType.BUY);
        Order csvOrder = createOrder("100000000001", "ABC", "TCS",
                new BigDecimal("200.50"), new BigDecimal("3500.0"), TIMESTAMP, OrderType.BUY);

        when(orderTableService.getAllOrders()).thenReturn(List.of(tableOrder));
        when(csvService.getAllOrders()).thenReturn(List.of(csvOrder));

        ReconciliationResult result = reconciliationService.reconcile();

        assertEquals(0, result.getMatchedRecords().size());
        assertEquals(1, result.getTableNotMatchedRecords().size());
        assertEquals(1, result.getCsvNotMatchedRecords().size());
    }

    // --- Test E: Same orderId but different price ---

    @Test
    @DisplayName("E. Same orderId but different price - both should be NOT MATCHED")
    void reconcile_sameOrderIdDifferentPrice() {
        Order tableOrder = createOrder("100000000001", "ABC", "TCS",
                new BigDecimal("100.25"), new BigDecimal("3500.0"), TIMESTAMP, OrderType.BUY);
        Order csvOrder = createOrder("100000000001", "ABC", "TCS",
                new BigDecimal("100.25"), new BigDecimal("4000.0"), TIMESTAMP, OrderType.BUY);

        when(orderTableService.getAllOrders()).thenReturn(List.of(tableOrder));
        when(csvService.getAllOrders()).thenReturn(List.of(csvOrder));

        ReconciliationResult result = reconciliationService.reconcile();

        assertEquals(0, result.getMatchedRecords().size());
        assertEquals(1, result.getTableNotMatchedRecords().size());
        assertEquals(1, result.getCsvNotMatchedRecords().size());
    }

    // --- Test F: Same orderId but different clientName ---

    @Test
    @DisplayName("F. Same orderId but different clientName - both should be NOT MATCHED")
    void reconcile_sameOrderIdDifferentClientName() {
        Order tableOrder = createOrder("100000000001", "ABC", "TCS",
                new BigDecimal("100.25"), new BigDecimal("3500.0"), TIMESTAMP, OrderType.BUY);
        Order csvOrder = createOrder("100000000001", "XYZ", "TCS",
                new BigDecimal("100.25"), new BigDecimal("3500.0"), TIMESTAMP, OrderType.BUY);

        when(orderTableService.getAllOrders()).thenReturn(List.of(tableOrder));
        when(csvService.getAllOrders()).thenReturn(List.of(csvOrder));

        ReconciliationResult result = reconciliationService.reconcile();

        assertEquals(0, result.getMatchedRecords().size());
        assertEquals(1, result.getTableNotMatchedRecords().size());
        assertEquals(1, result.getCsvNotMatchedRecords().size());
    }

    // --- Test G: Empty DB and non-empty CSV ---

    @Test
    @DisplayName("G. Empty DB and non-empty CSV - all CSV records should be CSV NOT MATCHED")
    void reconcile_emptyDbNonEmptyCsv() {
        Order csvOrder1 = createOrder("100000000001", "ABC", "TCS",
                new BigDecimal("100.25"), new BigDecimal("3500.0"), TIMESTAMP, OrderType.BUY);
        Order csvOrder2 = createOrder("100000000002", "XYZ", "INFY",
                new BigDecimal("200.50"), new BigDecimal("1800.0"), TIMESTAMP, OrderType.SELL);

        when(orderTableService.getAllOrders()).thenReturn(Collections.emptyList());
        when(csvService.getAllOrders()).thenReturn(List.of(csvOrder1, csvOrder2));

        ReconciliationResult result = reconciliationService.reconcile();

        assertEquals(0, result.getMatchedRecords().size());
        assertEquals(0, result.getTableNotMatchedRecords().size());
        assertEquals(2, result.getCsvNotMatchedRecords().size());
    }

    // --- Test H: Non-empty DB and empty CSV ---

    @Test
    @DisplayName("H. Non-empty DB and empty CSV - all DB records should be TABLE NOT MATCHED")
    void reconcile_nonEmptyDbEmptyCsv() {
        Order tableOrder1 = createOrder("100000000001", "ABC", "TCS",
                new BigDecimal("100.25"), new BigDecimal("3500.0"), TIMESTAMP, OrderType.BUY);
        Order tableOrder2 = createOrder("100000000002", "XYZ", "INFY",
                new BigDecimal("200.50"), new BigDecimal("1800.0"), TIMESTAMP, OrderType.SELL);

        when(orderTableService.getAllOrders()).thenReturn(List.of(tableOrder1, tableOrder2));
        when(csvService.getAllOrders()).thenReturn(Collections.emptyList());

        ReconciliationResult result = reconciliationService.reconcile();

        assertEquals(0, result.getMatchedRecords().size());
        assertEquals(2, result.getTableNotMatchedRecords().size());
        assertEquals(0, result.getCsvNotMatchedRecords().size());
    }

    // --- Test I: Both sources empty ---

    @Test
    @DisplayName("I. Both sources empty - all lists should be empty")
    void reconcile_bothEmpty() {
        when(orderTableService.getAllOrders()).thenReturn(Collections.emptyList());
        when(csvService.getAllOrders()).thenReturn(Collections.emptyList());

        ReconciliationResult result = reconciliationService.reconcile();

        assertEquals(0, result.getMatchedRecords().size());
        assertEquals(0, result.getTableNotMatchedRecords().size());
        assertEquals(0, result.getCsvNotMatchedRecords().size());
    }

    // --- Test J: Multiple records with mixed results ---

    @Test
    @DisplayName("J. Multiple records with mixed results")
    void reconcile_mixedResults() {
        LocalDateTime ts1 = LocalDateTime.of(2026, 9, 8, 10, 0, 0);
        LocalDateTime ts2 = LocalDateTime.of(2026, 9, 8, 10, 5, 0);
        LocalDateTime ts3 = LocalDateTime.of(2026, 9, 8, 10, 10, 0);
        LocalDateTime ts4 = LocalDateTime.of(2026, 9, 8, 10, 15, 0);
        LocalDateTime ts5 = LocalDateTime.of(2026, 9, 8, 10, 20, 0);

        // Order 1: exists in both, all fields match -> MATCHED
        Order tableOrder1 = createOrder("100000000001", "ABC", "TCS",
                new BigDecimal("100.25"), new BigDecimal("3500.0"), ts1, OrderType.BUY);
        Order csvOrder1 = createOrder("100000000001", "ABC", "TCS",
                new BigDecimal("100.25"), new BigDecimal("3500.0"), ts1, OrderType.BUY);

        // Order 2: exists only in DB -> TABLE NOT MATCHED
        Order tableOrder2 = createOrder("100000000002", "XYZ", "INFY",
                new BigDecimal("200.50"), new BigDecimal("1800.0"), ts2, OrderType.SELL);

        // Order 3: exists only in CSV -> CSV NOT MATCHED
        Order csvOrder3 = createOrder("100000000003", "DEF", "WIPRO",
                new BigDecimal("50.00"), new BigDecimal("400.5"), ts3, OrderType.BUY);

        // Order 4: exists in both, different quantity -> both NOT MATCHED
        Order tableOrder4 = createOrder("100000000004", "GHI", "RELIANCE",
                new BigDecimal("75.50"), new BigDecimal("2500.0"), ts4, OrderType.SELL);
        Order csvOrder4 = createOrder("100000000004", "GHI", "RELIANCE",
                new BigDecimal("80.00"), new BigDecimal("2500.0"), ts4, OrderType.SELL);

        // Order 5: exists in both, different price -> both NOT MATCHED
        Order tableOrder5 = createOrder("100000000005", "JKL", "HDFC",
                new BigDecimal("300.00"), new BigDecimal("1600.0"), ts5, OrderType.BUY);
        Order csvOrder5 = createOrder("100000000005", "JKL", "HDFC",
                new BigDecimal("300.00"), new BigDecimal("1700.0"), ts5, OrderType.BUY);

        List<Order> tableOrders = List.of(tableOrder1, tableOrder2, tableOrder4, tableOrder5);
        List<Order> csvOrders = List.of(csvOrder1, csvOrder3, csvOrder4, csvOrder5);

        when(orderTableService.getAllOrders()).thenReturn(tableOrders);
        when(csvService.getAllOrders()).thenReturn(csvOrders);

        ReconciliationResult result = reconciliationService.reconcile();

        // 1 matched (order 1)
        assertEquals(1, result.getMatchedRecords().size());

        // 3 table not matched: order 2 (DB only), order 4 (diff qty), order 5 (diff price)
        assertEquals(3, result.getTableNotMatchedRecords().size());

        // 3 CSV not matched: order 3 (CSV only), order 4 (diff qty), order 5 (diff price)
        assertEquals(3, result.getCsvNotMatchedRecords().size());
    }

    // --- Test: BigDecimal scale difference should still match ---

    @Test
    @DisplayName("BigDecimal scale difference (3500.0 vs 3500) should still match")
    void reconcile_bigDecimalScaleDifference() {
        Order tableOrder = createOrder("100000000001", "ABC", "TCS",
                new BigDecimal("100.25"), new BigDecimal("3500.0"), TIMESTAMP, OrderType.BUY);
        // CSV might parse 3500 without decimal
        Order csvOrder = createOrder("100000000001", "ABC", "TCS",
                new BigDecimal("100.25"), new BigDecimal("3500"), TIMESTAMP, OrderType.BUY);

        when(orderTableService.getAllOrders()).thenReturn(List.of(tableOrder));
        when(csvService.getAllOrders()).thenReturn(List.of(csvOrder));

        ReconciliationResult result = reconciliationService.reconcile();

        assertEquals(1, result.getMatchedRecords().size());
        assertEquals(0, result.getTableNotMatchedRecords().size());
        assertEquals(0, result.getCsvNotMatchedRecords().size());
    }

    // --- Test: getMatchedRecords convenience method ---

    @Test
    @DisplayName("getMatchedRecords delegates to reconcile()")
    void getMatchedRecords_delegatesToReconcile() {
        Order tableOrder = createOrder("100000000001", "ABC", "TCS",
                new BigDecimal("100.25"), new BigDecimal("3500.0"), TIMESTAMP, OrderType.BUY);
        Order csvOrder = createOrder("100000000001", "ABC", "TCS",
                new BigDecimal("100.25"), new BigDecimal("3500.0"), TIMESTAMP, OrderType.BUY);

        when(orderTableService.getAllOrders()).thenReturn(List.of(tableOrder));
        when(csvService.getAllOrders()).thenReturn(List.of(csvOrder));

        List<Order> matched = reconciliationService.getMatchedRecords();

        assertNotNull(matched);
        assertEquals(1, matched.size());
    }

    // --- Test: getTableNotMatchedRecords convenience method ---

    @Test
    @DisplayName("getTableNotMatchedRecords delegates to reconcile()")
    void getTableNotMatchedRecords_delegatesToReconcile() {
        Order tableOrder = createOrder("100000000001", "ABC", "TCS",
                new BigDecimal("100.25"), new BigDecimal("3500.0"), TIMESTAMP, OrderType.BUY);

        when(orderTableService.getAllOrders()).thenReturn(List.of(tableOrder));
        when(csvService.getAllOrders()).thenReturn(Collections.emptyList());

        List<Order> notMatched = reconciliationService.getTableNotMatchedRecords();

        assertNotNull(notMatched);
        assertEquals(1, notMatched.size());
    }

    // --- Test: getCsvNotMatchedRecords convenience method ---

    @Test
    @DisplayName("getCsvNotMatchedRecords delegates to reconcile()")
    void getCsvNotMatchedRecords_delegatesToReconcile() {
        Order csvOrder = createOrder("100000000001", "ABC", "TCS",
                new BigDecimal("100.25"), new BigDecimal("3500.0"), TIMESTAMP, OrderType.BUY);

        when(orderTableService.getAllOrders()).thenReturn(Collections.emptyList());
        when(csvService.getAllOrders()).thenReturn(List.of(csvOrder));

        List<Order> notMatched = reconciliationService.getCsvNotMatchedRecords();

        assertNotNull(notMatched);
        assertEquals(1, notMatched.size());
    }

    // --- Test: Different orderType should not match ---

    @Test
    @DisplayName("Same orderId but different orderType should not match")
    void reconcile_sameOrderIdDifferentOrderType() {
        Order tableOrder = createOrder("100000000001", "ABC", "TCS",
                new BigDecimal("100.25"), new BigDecimal("3500.0"), TIMESTAMP, OrderType.BUY);
        Order csvOrder = createOrder("100000000001", "ABC", "TCS",
                new BigDecimal("100.25"), new BigDecimal("3500.0"), TIMESTAMP, OrderType.SELL);

        when(orderTableService.getAllOrders()).thenReturn(List.of(tableOrder));
        when(csvService.getAllOrders()).thenReturn(List.of(csvOrder));

        ReconciliationResult result = reconciliationService.reconcile();

        assertEquals(0, result.getMatchedRecords().size());
        assertEquals(1, result.getTableNotMatchedRecords().size());
        assertEquals(1, result.getCsvNotMatchedRecords().size());
    }

    // --- Test: Different stockName should not match ---

    @Test
    @DisplayName("Same orderId but different stockName should not match")
    void reconcile_sameOrderIdDifferentStockName() {
        Order tableOrder = createOrder("100000000001", "ABC", "TCS",
                new BigDecimal("100.25"), new BigDecimal("3500.0"), TIMESTAMP, OrderType.BUY);
        Order csvOrder = createOrder("100000000001", "ABC", "INFY",
                new BigDecimal("100.25"), new BigDecimal("3500.0"), TIMESTAMP, OrderType.BUY);

        when(orderTableService.getAllOrders()).thenReturn(List.of(tableOrder));
        when(csvService.getAllOrders()).thenReturn(List.of(csvOrder));

        ReconciliationResult result = reconciliationService.reconcile();

        assertEquals(0, result.getMatchedRecords().size());
        assertEquals(1, result.getTableNotMatchedRecords().size());
        assertEquals(1, result.getCsvNotMatchedRecords().size());
    }

    // --- Test: Different timestamp should not match ---

    @Test
    @DisplayName("Same orderId but different timestamp should not match")
    void reconcile_sameOrderIdDifferentTimestamp() {
        Order tableOrder = createOrder("100000000001", "ABC", "TCS",
                new BigDecimal("100.25"), new BigDecimal("3500.0"),
                LocalDateTime.of(2026, 9, 8, 10, 0, 0), OrderType.BUY);
        Order csvOrder = createOrder("100000000001", "ABC", "TCS",
                new BigDecimal("100.25"), new BigDecimal("3500.0"),
                LocalDateTime.of(2026, 9, 8, 11, 0, 0), OrderType.BUY);

        when(orderTableService.getAllOrders()).thenReturn(List.of(tableOrder));
        when(csvService.getAllOrders()).thenReturn(List.of(csvOrder));

        ReconciliationResult result = reconciliationService.reconcile();

        assertEquals(0, result.getMatchedRecords().size());
        assertEquals(1, result.getTableNotMatchedRecords().size());
        assertEquals(1, result.getCsvNotMatchedRecords().size());
    }
}
