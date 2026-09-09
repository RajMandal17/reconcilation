package com.example.orderreconciliation.csv;

import com.example.orderreconciliation.enums.OrderType;
import com.example.orderreconciliation.exception.InvalidCsvException;
import com.example.orderreconciliation.model.Order;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles all CSV file operations: reading, parsing, validating, and writing.
 * Keeps CSV-specific logic isolated from service and reconciliation layers.
 */
@Component
public class CsvFileHandler {

    private static final Logger log = LoggerFactory.getLogger(CsvFileHandler.class);

    private static final String[] EXPECTED_HEADERS = {
            "orderId", "clientName", "stockName", "orderQty", "price", "orderTimestamp", "orderType"
    };

    private final String csvFilePath;

    public CsvFileHandler(@Value("${app.csv.file-path}") String csvFilePath) {
        this.csvFilePath = csvFilePath;
    }

    /**
     * Returns the configured CSV file path.
     */
    public String getCsvFilePath() {
        return csvFilePath;
    }

    /**
     * Reads all orders from the configured CSV file.
     * Returns an empty list if the file does not exist.
     */
    public List<Order> readOrders() {
        File file = new File(csvFilePath);
        if (!file.exists()) {
            log.info("CSV file does not exist at {}. Returning empty list.", csvFilePath);
            return new ArrayList<>();
        }

        try (CSVReader reader = new CSVReader(new FileReader(file))) {
            List<String[]> allRows = reader.readAll();

            if (allRows.isEmpty()) {
                log.info("CSV file is empty at {}.", csvFilePath);
                return new ArrayList<>();
            }

            // Validate header
            String[] header = allRows.get(0);
            validateHeader(header);

            List<Order> orders = new ArrayList<>();
            for (int i = 1; i < allRows.size(); i++) {
                String[] row = allRows.get(i);
                if (row.length == 1 && row[0].isBlank()) {
                    continue; // skip blank lines
                }
                orders.add(parseRow(row, i + 1));
            }

            log.info("Read {} orders from CSV file.", orders.size());
            return orders;

        } catch (InvalidCsvException e) {
            throw e;
        } catch (IOException e) {
            throw new InvalidCsvException("Failed to read CSV file: " + e.getMessage());
        } catch (Exception e) {
            throw new InvalidCsvException("Error processing CSV file: " + e.getMessage());
        }
    }

    /**
     * Parses uploaded CSV content (lines) into a list of Orders.
     * Validates header, each row, and detects duplicates.
     */
    public List<Order> parseAndValidate(List<String[]> allRows) {
        if (allRows == null || allRows.isEmpty()) {
            throw new InvalidCsvException("CSV file is empty");
        }

        // Validate header
        String[] header = allRows.get(0);
        validateHeader(header);

        List<Order> orders = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<String> seenOrderIds = new ArrayList<>();

        for (int i = 1; i < allRows.size(); i++) {
            String[] row = allRows.get(i);
            if (row.length == 1 && row[0].isBlank()) {
                continue; // skip blank lines
            }
            try {
                Order order = parseRow(row, i + 1);

                // Check for duplicate orderId within the CSV
                if (seenOrderIds.contains(order.getOrderId())) {
                    errors.add("Row " + (i + 1) + ": duplicate orderId '" + order.getOrderId() + "'");
                } else {
                    seenOrderIds.add(order.getOrderId());
                }

                orders.add(order);
            } catch (InvalidCsvException e) {
                errors.add(e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            throw new InvalidCsvException("CSV validation failed: " + String.join("; ", errors));
        }

        log.info("Parsed and validated {} orders from uploaded CSV.", orders.size());
        return orders;
    }

    /**
     * Writes orders to the configured CSV file using a temporary file strategy.
     * If writing succeeds, the temp file replaces the actual file.
     * If writing fails, the temp file is deleted and the original remains intact.
     */
    public void writeOrders(List<Order> orders) {
        File targetFile = new File(csvFilePath);
        ensureParentDirectory(targetFile);

        // Use temporary file strategy
        File tempFile;
        try {
            tempFile = File.createTempFile("orders_", ".csv", targetFile.getParentFile());
        } catch (IOException e) {
            throw new InvalidCsvException("Failed to create temporary CSV file: " + e.getMessage());
        }

        try (CSVWriter writer = new CSVWriter(new FileWriter(tempFile))) {
            // Write header
            writer.writeNext(EXPECTED_HEADERS);

            // Write each order as a row
            for (Order order : orders) {
                writer.writeNext(toRow(order));
            }

            log.info("Successfully wrote {} orders to temporary CSV file.", orders.size());

        } catch (IOException e) {
            tempFile.delete();
            throw new InvalidCsvException("Failed to write CSV file: " + e.getMessage());
        }

        // Replace original file with temp file
        try {
            Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("CSV file updated successfully at {}.", csvFilePath);
        } catch (IOException e) {
            tempFile.delete();
            throw new InvalidCsvException("Failed to replace CSV file: " + e.getMessage());
        }
    }

    /**
     * Validates that the CSV header matches the expected columns.
     */
    public void validateHeader(String[] header) {
        if (header == null || header.length != EXPECTED_HEADERS.length) {
            throw new InvalidCsvException("CSV header must have exactly " + EXPECTED_HEADERS.length +
                    " columns: " + Arrays.toString(EXPECTED_HEADERS) +
                    ". Found: " + (header == null ? "null" : Arrays.toString(header)));
        }

        for (int i = 0; i < EXPECTED_HEADERS.length; i++) {
            if (!EXPECTED_HEADERS[i].equalsIgnoreCase(header[i].trim())) {
                throw new InvalidCsvException("CSV header column " + (i + 1) +
                        " must be '" + EXPECTED_HEADERS[i] + "' but found '" + header[i].trim() + "'");
            }
        }
    }

    /**
     * Parses a single CSV row into an Order object with validation.
     */
    private Order parseRow(String[] row, int rowNumber) {
        if (row.length != EXPECTED_HEADERS.length) {
            throw new InvalidCsvException("Row " + rowNumber + ": expected " +
                    EXPECTED_HEADERS.length + " columns but found " + row.length);
        }

        List<String> rowErrors = new ArrayList<>();

        String orderId = row[0].trim();
        String clientName = row[1].trim();
        String stockName = row[2].trim();
        String orderQtyStr = row[3].trim();
        String priceStr = row[4].trim();
        String timestampStr = row[5].trim();
        String orderTypeStr = row[6].trim();

        // Validate orderId
        if (orderId.isBlank()) {
            rowErrors.add("orderId is mandatory");
        } else if (!orderId.matches("\\d{12}")) {
            rowErrors.add("orderId must contain exactly 12 digits");
        }

        // Validate clientName
        if (clientName.isBlank()) {
            rowErrors.add("clientName is mandatory");
        }

        // Validate stockName
        if (stockName.isBlank()) {
            rowErrors.add("stockName is mandatory");
        }

        // Validate orderQty
        BigDecimal orderQty = null;
        if (orderQtyStr.isBlank()) {
            rowErrors.add("orderQty is mandatory");
        } else {
            try {
                orderQty = new BigDecimal(orderQtyStr);
                if (orderQty.compareTo(BigDecimal.ZERO) <= 0) {
                    rowErrors.add("orderQty must be positive");
                }
                if (orderQty.scale() > 2) {
                    rowErrors.add("orderQty must have at most 2 decimal places");
                }
            } catch (NumberFormatException e) {
                rowErrors.add("orderQty is not a valid number: '" + orderQtyStr + "'");
            }
        }

        // Validate price
        BigDecimal price = null;
        if (priceStr.isBlank()) {
            rowErrors.add("price is mandatory");
        } else {
            try {
                price = new BigDecimal(priceStr);
                if (price.compareTo(BigDecimal.ZERO) <= 0) {
                    rowErrors.add("price must be positive");
                }
                if (price.scale() > 1) {
                    rowErrors.add("price must have at most 1 decimal place");
                }
            } catch (NumberFormatException e) {
                rowErrors.add("price is not a valid number: '" + priceStr + "'");
            }
        }

        // Validate orderTimestamp
        LocalDateTime orderTimestamp = null;
        if (timestampStr.isBlank()) {
            rowErrors.add("orderTimestamp is mandatory");
        } else {
            try {
                orderTimestamp = LocalDateTime.parse(timestampStr);
            } catch (DateTimeParseException e) {
                rowErrors.add("orderTimestamp is not a valid timestamp: '" + timestampStr + "'");
            }
        }

        // Validate orderType
        OrderType orderType = null;
        if (orderTypeStr.isBlank()) {
            rowErrors.add("orderType is mandatory");
        } else {
            try {
                orderType = OrderType.valueOf(orderTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                rowErrors.add("orderType must be BUY or SELL, found: '" + orderTypeStr + "'");
            }
        }

        if (!rowErrors.isEmpty()) {
            throw new InvalidCsvException("Row " + rowNumber + ": " + String.join("; ", rowErrors));
        }

        return new Order(orderId, clientName, stockName, orderQty, price, orderTimestamp, orderType);
    }

    /**
     * Converts an Order to a CSV row array.
     */
    private String[] toRow(Order order) {
        return new String[]{
                order.getOrderId(),
                order.getClientName(),
                order.getStockName(),
                order.getOrderQty().toPlainString(),
                order.getPrice().toPlainString(),
                order.getOrderTimestamp().toString(),
                order.getOrderType().name()
        };
    }

    /**
     * Ensures the parent directory of the target file exists.
     */
    private void ensureParentDirectory(File file) {
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            if (created) {
                log.info("Created directory: {}", parentDir.getAbsolutePath());
            }
        }
    }
}
