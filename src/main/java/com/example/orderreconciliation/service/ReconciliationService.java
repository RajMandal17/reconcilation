package com.example.orderreconciliation.service;

import com.example.orderreconciliation.dto.ReconciliationResult;
import com.example.orderreconciliation.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Core reconciliation service that compares database records (Source A) with
 * CSV records (Source B) using HashMap and HashSet for O(n) performance.
 *
 * The reconciliation is performed once, and all three result categories
 * (matched, table-not-matched, csv-not-matched) are derived from a single pass.
 */
@Service
public class ReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);

    private final OrderTableService orderTableService;
    private final CsvService csvService;

    public ReconciliationService(OrderTableService orderTableService, CsvService csvService) {
        this.orderTableService = orderTableService;
        this.csvService = csvService;
    }

    /**
     * Returns only matched records (exist in both sources with all fields equal).
     */
    public List<Order> getMatchedRecords() {
        return reconcile().getMatchedRecords();
    }

    /**
     * Returns table-not-matched records:
     * - DB records whose orderId does not exist in CSV
     * - DB records whose orderId exists in CSV but one or more fields differ
     */
    public List<Order> getTableNotMatchedRecords() {
        return reconcile().getTableNotMatchedRecords();
    }

    /**
     * Returns CSV-not-matched records:
     * - CSV records whose orderId does not exist in DB
     * - CSV records whose orderId exists in DB but one or more fields differ
     */
    public List<Order> getCsvNotMatchedRecords() {
        return reconcile().getCsvNotMatchedRecords();
    }

    /**
     * Performs reconciliation between database and CSV sources.
     *
     * Algorithm:
     * 1. Read all DB orders -> build HashMap keyed by orderId
     * 2. Read all CSV orders -> build HashMap keyed by orderId
     * 3. Create a HashSet union of all orderIds from both sources
     * 4. Single iteration over the union set with O(1) HashMap lookups
     * 5. Classify each orderId into matched, table-not-matched, or csv-not-matched
     */
    public ReconciliationResult reconcile() {
        log.info("Reconciliation started.");

        // Step 1: Read all orders from both sources
        List<Order> tableOrders = orderTableService.getAllOrders();
        List<Order> csvOrders = csvService.getAllOrders();

        log.info("Database records: {}. CSV records: {}.", tableOrders.size(), csvOrders.size());

        // Step 2: Build HashMaps keyed by orderId
        Map<String, Order> tableMap = buildOrderMap(tableOrders);
        Map<String, Order> csvMap = buildOrderMap(csvOrders);

        // Step 3: Create union of all orderIds
        Set<String> allOrderIds = new HashSet<>();
        allOrderIds.addAll(tableMap.keySet());
        allOrderIds.addAll(csvMap.keySet());

        // Step 4: Single iteration - classify each orderId
        List<Order> matchedRecords = new ArrayList<>();
        List<Order> tableNotMatchedRecords = new ArrayList<>();
        List<Order> csvNotMatchedRecords = new ArrayList<>();

        for (String orderId : allOrderIds) {
            Order tableOrder = tableMap.get(orderId);
            Order csvOrder = csvMap.get(orderId);

            if (tableOrder != null && csvOrder != null) {
                // orderId exists in both sources - compare all fields
                if (compareAllFields(tableOrder, csvOrder)) {
                    matchedRecords.add(tableOrder);
                } else {
                    // Same orderId but fields differ - both go to not-matched
                    tableNotMatchedRecords.add(tableOrder);
                    csvNotMatchedRecords.add(csvOrder);
                }
            } else if (tableOrder != null) {
                // orderId exists only in DB
                tableNotMatchedRecords.add(tableOrder);
            } else {
                // orderId exists only in CSV
                csvNotMatchedRecords.add(csvOrder);
            }
        }

        log.info("Reconciliation completed. Matched: {}, Table not matched: {}, CSV not matched: {}.",
                matchedRecords.size(), tableNotMatchedRecords.size(), csvNotMatchedRecords.size());

        return new ReconciliationResult(matchedRecords, tableNotMatchedRecords, csvNotMatchedRecords);
    }

    /**
     * Builds a HashMap from a list of orders, keyed by orderId.
     */
    private Map<String, Order> buildOrderMap(List<Order> orders) {
        Map<String, Order> map = new HashMap<>();
        for (Order order : orders) {
            map.put(order.getOrderId(), order);
        }
        return map;
    }

    /**
     * Compares ALL business fields of two Order objects.
     * Uses compareTo() for BigDecimal to handle scale differences.
     *
     * Returns true only if every field matches.
     */
    private boolean compareAllFields(Order tableOrder, Order csvOrder) {
        if (!tableOrder.getOrderId().equals(csvOrder.getOrderId())) {
            return false;
        }
        if (!tableOrder.getClientName().equals(csvOrder.getClientName())) {
            return false;
        }
        if (!tableOrder.getStockName().equals(csvOrder.getStockName())) {
            return false;
        }
        // Use compareTo for BigDecimal to handle scale differences (e.g., 100.0 vs 100.00)
        if (tableOrder.getOrderQty().compareTo(csvOrder.getOrderQty()) != 0) {
            return false;
        }
        if (tableOrder.getPrice().compareTo(csvOrder.getPrice()) != 0) {
            return false;
        }
        if (!tableOrder.getOrderTimestamp().equals(csvOrder.getOrderTimestamp())) {
            return false;
        }
        if (tableOrder.getOrderType() != csvOrder.getOrderType()) {
            return false;
        }
        return true;
    }
}
