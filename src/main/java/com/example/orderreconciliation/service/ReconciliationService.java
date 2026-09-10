package com.example.orderreconciliation.service;

import com.example.orderreconciliation.dto.ReconciliationResult;
import com.example.orderreconciliation.model.Order;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reconciles database Source A with backend CSV Source B using HashMap and HashSet.
 */
@Service
public class ReconciliationService {

    private final OrderTableService orderTableService;
    private final OrderCsvService orderCsvService;

    public ReconciliationService(OrderTableService orderTableService,
                                 OrderCsvService orderCsvService) {
        this.orderTableService = orderTableService;
        this.orderCsvService = orderCsvService;
    }

    public List<Order> getMatchedRecords() {
        return reconcile().getMatchedRecords();
    }

    public List<Order> getTableNotMatchedRecords() {
        return reconcile().getTableNotMatchedRecords();
    }

    public List<Order> getCsvNotMatchedRecords() {
        return reconcile().getCsvNotMatchedRecords();
    }

    /**
     * 1. Read DB and CSV.
     * 2. Put both sources into HashMaps keyed by orderId.
     * 3. Build one HashSet containing the union of all orderIds.
     * 4. Iterate once over the HashSet and compare records.
     */
    public ReconciliationResult reconcile() {
        List<Order> tableOrders = orderTableService.getAllOrders();
        List<Order> csvOrders = orderCsvService.getAllOrders();

        Map<String, Order> tableMap = buildOrderMap(tableOrders);
        Map<String, Order> csvMap = buildOrderMap(csvOrders);

        Set<String> allOrderIds = new HashSet<>();
        allOrderIds.addAll(tableMap.keySet());
        allOrderIds.addAll(csvMap.keySet());

        List<Order> matchedRecords = new ArrayList<>();
        List<Order> tableNotMatchedRecords = new ArrayList<>();
        List<Order> csvNotMatchedRecords = new ArrayList<>();

        for (String orderId : allOrderIds) {
            Order tableOrder = tableMap.get(orderId);
            Order csvOrder = csvMap.get(orderId);

            if (tableOrder != null && csvOrder != null) {
                if (compareAllFields(tableOrder, csvOrder)) {
                    matchedRecords.add(tableOrder);
                } else {
                    tableNotMatchedRecords.add(tableOrder);
                    csvNotMatchedRecords.add(csvOrder);
                }
            } else if (tableOrder != null) {
                tableNotMatchedRecords.add(tableOrder);
            } else {
                csvNotMatchedRecords.add(csvOrder);
            }
        }

        return new ReconciliationResult(
                matchedRecords,
                tableNotMatchedRecords,
                csvNotMatchedRecords
        );
    }

    private Map<String, Order> buildOrderMap(List<Order> orders) {
        Map<String, Order> map = new HashMap<>();
        for (Order order : orders) {
            map.put(order.getOrderId(), order);
        }
        return map;
    }

    private boolean compareAllFields(Order tableOrder, Order csvOrder) {
        return tableOrder.getOrderId().equals(csvOrder.getOrderId())
                && tableOrder.getClientName().equals(csvOrder.getClientName())
                && tableOrder.getStockName().equals(csvOrder.getStockName())
                && tableOrder.getOrderQty().compareTo(csvOrder.getOrderQty()) == 0
                && tableOrder.getPrice().compareTo(csvOrder.getPrice()) == 0
                && tableOrder.getOrderTimestamp().equals(csvOrder.getOrderTimestamp())
                && tableOrder.getOrderType() == csvOrder.getOrderType();
    }
}
