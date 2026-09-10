package com.example.orderreconciliation.service;

import com.example.orderreconciliation.dto.ReconciliationResult;
import com.example.orderreconciliation.enums.OrderType;
import com.example.orderreconciliation.model.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private OrderTableService orderTableService;

    @Mock
    private OrderCsvService orderCsvService;

    @InjectMocks
    private ReconciliationService reconciliationService;

    @Test
    void matchingRecordsGoToMatched() {
        Order db = order("100000000001", "ABC", "TCS", "100.25", "3500.0", OrderType.BUY);
        Order csv = order("100000000001", "ABC", "TCS", "100.25", "3500.0", OrderType.BUY);
        when(orderTableService.getAllOrders()).thenReturn(List.of(db));
        when(orderCsvService.getAllOrders()).thenReturn(List.of(csv));

        ReconciliationResult result = reconciliationService.reconcile();

        assertEquals(1, result.getMatchedRecords().size());
        assertEquals(0, result.getTableNotMatchedRecords().size());
        assertEquals(0, result.getCsvNotMatchedRecords().size());
    }

    @Test
    void dbOnlyRecordGoesToTableNotMatched() {
        Order db = order("100000000001", "ABC", "TCS", "100.25", "3500.0", OrderType.BUY);
        when(orderTableService.getAllOrders()).thenReturn(List.of(db));
        when(orderCsvService.getAllOrders()).thenReturn(List.of());

        ReconciliationResult result = reconciliationService.reconcile();

        assertEquals(1, result.getTableNotMatchedRecords().size());
        assertEquals(0, result.getCsvNotMatchedRecords().size());
    }

    @Test
    void csvOnlyRecordGoesToCsvNotMatched() {
        Order csv = order("100000000001", "ABC", "TCS", "100.25", "3500.0", OrderType.BUY);
        when(orderTableService.getAllOrders()).thenReturn(List.of());
        when(orderCsvService.getAllOrders()).thenReturn(List.of(csv));

        ReconciliationResult result = reconciliationService.reconcile();

        assertEquals(0, result.getTableNotMatchedRecords().size());
        assertEquals(1, result.getCsvNotMatchedRecords().size());
    }

    @Test
    void fieldMismatchGoesToBothNotMatchedLists() {
        Order db = order("100000000001", "ABC", "TCS", "100.25", "3500.0", OrderType.BUY);
        Order csv = order("100000000001", "ABC", "TCS", "100.25", "4000.0", OrderType.BUY);
        when(orderTableService.getAllOrders()).thenReturn(List.of(db));
        when(orderCsvService.getAllOrders()).thenReturn(List.of(csv));

        ReconciliationResult result = reconciliationService.reconcile();

        assertEquals(0, result.getMatchedRecords().size());
        assertEquals(1, result.getTableNotMatchedRecords().size());
        assertEquals(1, result.getCsvNotMatchedRecords().size());
        assertEquals("3500.0", result.getTableNotMatchedRecords().get(0).getPrice().toPlainString());
        assertEquals("4000.0", result.getCsvNotMatchedRecords().get(0).getPrice().toPlainString());
    }

    private Order order(String id, String client, String stock, String qty, String price, OrderType type) {
        return new Order(id, client, stock, new BigDecimal(qty), new BigDecimal(price),
                LocalDateTime.of(2026, 9, 8, 10, 0), type);
    }
}
