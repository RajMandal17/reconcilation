package com.example.orderreconciliation.csv;

import com.example.orderreconciliation.enums.OrderType;
import com.example.orderreconciliation.exception.InvalidCsvException;
import com.example.orderreconciliation.model.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvFileHandlerTest {

    @TempDir
    Path tempDir;

    private CsvFileHandler csvFileHandler;
    private String csvFilePath;

    @BeforeEach
    void setUp() {
        csvFilePath = tempDir.resolve("test_orders.csv").toString();
        csvFileHandler = new CsvFileHandler(csvFilePath);
    }

    @Test
    @DisplayName("Should return empty list when CSV file does not exist")
    void readOrders_fileNotExists() {
        List<Order> orders = csvFileHandler.readOrders();
        assertNotNull(orders);
        assertTrue(orders.isEmpty());
    }

    @Test
    @DisplayName("Should read valid CSV file successfully")
    void readOrders_validFile() throws IOException {
        writeTestCsv(
                "orderId,clientName,stockName,orderQty,price,orderTimestamp,orderType\n" +
                "100000000001,ABC,TCS,100.25,3500.0,2026-09-08T10:00:00,BUY\n"
        );

        List<Order> orders = csvFileHandler.readOrders();

        assertEquals(1, orders.size());
        Order order = orders.get(0);
        assertEquals("100000000001", order.getOrderId());
        assertEquals("ABC", order.getClientName());
        assertEquals("TCS", order.getStockName());
        assertEquals(new BigDecimal("100.25"), order.getOrderQty());
        assertEquals(new BigDecimal("3500.0"), order.getPrice());
        assertEquals(LocalDateTime.of(2026, 9, 8, 10, 0, 0), order.getOrderTimestamp());
        assertEquals(OrderType.BUY, order.getOrderType());
    }

    @Test
    @DisplayName("Should throw on invalid header")
    void readOrders_invalidHeader() throws IOException {
        writeTestCsv("col1,col2,col3,col4,col5,col6,col7\n" +
                "100000000001,ABC,TCS,100.25,3500.0,2026-09-08T10:00:00,BUY\n");

        assertThrows(InvalidCsvException.class, () -> csvFileHandler.readOrders());
    }

    @Test
    @DisplayName("Should throw on invalid row data - bad orderId")
    void parseAndValidate_invalidOrderId() {
        List<String[]> rows = List.of(
                new String[]{"orderId", "clientName", "stockName", "orderQty", "price", "orderTimestamp", "orderType"},
                new String[]{"12345", "ABC", "TCS", "100.25", "3500.0", "2026-09-08T10:00:00", "BUY"}
        );

        assertThrows(InvalidCsvException.class, () -> csvFileHandler.parseAndValidate(rows));
    }

    @Test
    @DisplayName("Should throw on malformed decimal - orderQty")
    void parseAndValidate_malformedDecimal() {
        List<String[]> rows = List.of(
                new String[]{"orderId", "clientName", "stockName", "orderQty", "price", "orderTimestamp", "orderType"},
                new String[]{"100000000001", "ABC", "TCS", "abc", "3500.0", "2026-09-08T10:00:00", "BUY"}
        );

        assertThrows(InvalidCsvException.class, () -> csvFileHandler.parseAndValidate(rows));
    }

    @Test
    @DisplayName("Should throw on malformed timestamp")
    void parseAndValidate_malformedTimestamp() {
        List<String[]> rows = List.of(
                new String[]{"orderId", "clientName", "stockName", "orderQty", "price", "orderTimestamp", "orderType"},
                new String[]{"100000000001", "ABC", "TCS", "100.25", "3500.0", "not-a-date", "BUY"}
        );

        assertThrows(InvalidCsvException.class, () -> csvFileHandler.parseAndValidate(rows));
    }

    @Test
    @DisplayName("Should throw on duplicate orderId within CSV")
    void parseAndValidate_duplicateOrderId() {
        List<String[]> rows = List.of(
                new String[]{"orderId", "clientName", "stockName", "orderQty", "price", "orderTimestamp", "orderType"},
                new String[]{"100000000001", "ABC", "TCS", "100.25", "3500.0", "2026-09-08T10:00:00", "BUY"},
                new String[]{"100000000001", "XYZ", "INFY", "200.50", "1800.0", "2026-09-08T10:05:00", "SELL"}
        );

        InvalidCsvException ex = assertThrows(InvalidCsvException.class,
                () -> csvFileHandler.parseAndValidate(rows));
        assertTrue(ex.getMessage().contains("duplicate orderId"));
    }

    @Test
    @DisplayName("Should throw on empty CSV content")
    void parseAndValidate_emptyContent() {
        assertThrows(InvalidCsvException.class, () -> csvFileHandler.parseAndValidate(List.of()));
    }

    @Test
    @DisplayName("Should throw on negative orderQty")
    void parseAndValidate_negativeQuantity() {
        List<String[]> rows = List.of(
                new String[]{"orderId", "clientName", "stockName", "orderQty", "price", "orderTimestamp", "orderType"},
                new String[]{"100000000001", "ABC", "TCS", "-10.00", "3500.0", "2026-09-08T10:00:00", "BUY"}
        );

        assertThrows(InvalidCsvException.class, () -> csvFileHandler.parseAndValidate(rows));
    }

    @Test
    @DisplayName("Should throw on invalid orderType")
    void parseAndValidate_invalidOrderType() {
        List<String[]> rows = List.of(
                new String[]{"orderId", "clientName", "stockName", "orderQty", "price", "orderTimestamp", "orderType"},
                new String[]{"100000000001", "ABC", "TCS", "100.25", "3500.0", "2026-09-08T10:00:00", "HOLD"}
        );

        assertThrows(InvalidCsvException.class, () -> csvFileHandler.parseAndValidate(rows));
    }

    @Test
    @DisplayName("Should write orders to CSV file successfully")
    void writeOrders_success() {
        Order order = new Order("100000000001", "ABC", "TCS",
                new BigDecimal("100.25"), new BigDecimal("3500.0"),
                LocalDateTime.of(2026, 9, 8, 10, 0, 0), OrderType.BUY);

        csvFileHandler.writeOrders(List.of(order));

        // Read back
        List<Order> readOrders = csvFileHandler.readOrders();
        assertEquals(1, readOrders.size());
        assertEquals("100000000001", readOrders.get(0).getOrderId());
    }

    @Test
    @DisplayName("Should validate header with correct columns")
    void validateHeader_correct() {
        String[] header = {"orderId", "clientName", "stockName", "orderQty", "price", "orderTimestamp", "orderType"};
        csvFileHandler.validateHeader(header); // should not throw
    }

    @Test
    @DisplayName("Should throw on wrong number of header columns")
    void validateHeader_wrongColumnCount() {
        String[] header = {"orderId", "clientName"};
        assertThrows(InvalidCsvException.class, () -> csvFileHandler.validateHeader(header));
    }

    @Test
    @DisplayName("Should parse valid rows successfully")
    void parseAndValidate_validRows() {
        List<String[]> rows = List.of(
                new String[]{"orderId", "clientName", "stockName", "orderQty", "price", "orderTimestamp", "orderType"},
                new String[]{"100000000001", "ABC", "TCS", "100.25", "3500.0", "2026-09-08T10:00:00", "BUY"},
                new String[]{"100000000002", "XYZ", "INFY", "200.50", "1800.0", "2026-09-08T10:05:00", "SELL"}
        );

        List<Order> orders = csvFileHandler.parseAndValidate(rows);

        assertEquals(2, orders.size());
        assertEquals("100000000001", orders.get(0).getOrderId());
        assertEquals("100000000002", orders.get(1).getOrderId());
    }

    @Test
    @DisplayName("Should throw on too many decimal places for orderQty")
    void parseAndValidate_tooManyDecimalPlaces() {
        List<String[]> rows = List.of(
                new String[]{"orderId", "clientName", "stockName", "orderQty", "price", "orderTimestamp", "orderType"},
                new String[]{"100000000001", "ABC", "TCS", "100.259", "3500.0", "2026-09-08T10:00:00", "BUY"}
        );

        assertThrows(InvalidCsvException.class, () -> csvFileHandler.parseAndValidate(rows));
    }

    @Test
    @DisplayName("Should throw on too many decimal places for price")
    void parseAndValidate_tooManyDecimalPlacesPrice() {
        List<String[]> rows = List.of(
                new String[]{"orderId", "clientName", "stockName", "orderQty", "price", "orderTimestamp", "orderType"},
                new String[]{"100000000001", "ABC", "TCS", "100.25", "3500.05", "2026-09-08T10:00:00", "BUY"}
        );

        assertThrows(InvalidCsvException.class, () -> csvFileHandler.parseAndValidate(rows));
    }

    private void writeTestCsv(String content) throws IOException {
        File file = new File(csvFilePath);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }
}
