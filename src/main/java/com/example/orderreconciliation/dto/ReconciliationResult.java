package com.example.orderreconciliation.dto;

import com.example.orderreconciliation.model.Order;

import java.util.List;

/**
 * Holds the result of reconciliation between database and CSV sources.
 */
public class ReconciliationResult {

    private final List<Order> matchedRecords;
    private final List<Order> tableNotMatchedRecords;
    private final List<Order> csvNotMatchedRecords;

    public ReconciliationResult(List<Order> matchedRecords,
                                List<Order> tableNotMatchedRecords,
                                List<Order> csvNotMatchedRecords) {
        this.matchedRecords = matchedRecords;
        this.tableNotMatchedRecords = tableNotMatchedRecords;
        this.csvNotMatchedRecords = csvNotMatchedRecords;
    }

    public List<Order> getMatchedRecords() {
        return matchedRecords;
    }

    public List<Order> getTableNotMatchedRecords() {
        return tableNotMatchedRecords;
    }

    public List<Order> getCsvNotMatchedRecords() {
        return csvNotMatchedRecords;
    }
}
