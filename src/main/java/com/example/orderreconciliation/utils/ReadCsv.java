package com.example.orderreconciliation.utils;

import com.example.orderreconciliation.enums.OrderType;
import com.example.orderreconciliation.exception.InvalidCsvException;
import com.example.orderreconciliation.model.Order;
import com.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reads and validates CSV records.
 *
 * The header is file-level validation: an invalid header rejects the complete file.
 * Data rows are row-level validation: an invalid row is skipped and processing
 * continues with the next row. Only valid Order objects are returned.
 */
@Component
public class ReadCsv {

    private static final Logger log = LoggerFactory.getLogger(ReadCsv.class);

    private static final String[] EXPECTED_HEADERS = {
            "orderId",
            "clientName",
            "stockName",
            "orderQty",
            "price",
            "orderTimestamp",
            "orderType"
    };

    /**
     * Reads an uploaded MultipartFile and returns only valid orders.
     */
    public List<Order> readCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidCsvException("CSV file is required and must not be empty");
        }

        try (CSVReader reader = new CSVReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            return read(reader);
        } catch (IOException e) {
            throw new InvalidCsvException("Failed to read uploaded CSV file: " + e.getMessage());
        }
    }

    /**
     * Reads the backend CSV file and returns only valid orders.
     * A missing backend file is treated as an empty CSV source.
     */
    public List<Order> readCsv(String filePath) {
        File file = new File(filePath);

        if (!file.exists()) {
            log.info("CSV file does not exist at {}. Returning empty list.", filePath);
            return new ArrayList<>();
        }

        try (CSVReader reader = new CSVReader(new FileReader(file, StandardCharsets.UTF_8))) {
            return read(reader);
        } catch (IOException e) {
            throw new InvalidCsvException("Failed to read CSV file: " + e.getMessage());
        }
    }

    private List<Order> read(CSVReader reader) {
        try {
            String[] header = reader.readNext();

            if (header == null) {
                throw new InvalidCsvException("CSV file is empty");
            }

            validateHeader(header);

            List<Order> validOrders = new ArrayList<>();
            Set<String> orderIds = new HashSet<>();

            String[] row;
            int rowNumber = 1;

            while ((row = reader.readNext()) != null) {
                rowNumber++;

                if (isBlankRow(row)) {
                    continue;
                }

                try {
                    Order order = parseRow(row, rowNumber);

                    // Keep the first valid orderId and skip duplicate rows.
                    if (!orderIds.add(order.getOrderId())) {
                        log.warn("Skipping row {}: duplicate orderId '{}'", rowNumber, order.getOrderId());
                        continue;
                    }

                    validOrders.add(order);
                } catch (InvalidCsvException e) {
                    log.warn("Skipping invalid CSV row {}: {}", rowNumber, e.getMessage());
                }
            }

            log.info("CSV read completed. Valid records: {}", validOrders.size());
            return validOrders;
        } catch (IOException e) {
            throw new InvalidCsvException("Failed while reading CSV rows: " + e.getMessage());
        }
    }

    private void validateHeader(String[] header) {
        if (header == null || header.length != EXPECTED_HEADERS.length) {
            throw new InvalidCsvException(
                    "CSV header must contain exactly " + EXPECTED_HEADERS.length + " columns"
            );
        }

        for (int i = 0; i < EXPECTED_HEADERS.length; i++) {
            if (!EXPECTED_HEADERS[i].equalsIgnoreCase(header[i].trim())) {
                throw new InvalidCsvException(
                        "Invalid CSV header at column " + (i + 1)
                                + ". Expected '" + EXPECTED_HEADERS[i]
                                + "' but found '" + header[i].trim() + "'"
                );
            }
        }
    }

    private boolean isBlankRow(String[] row) {
        if (row == null || row.length == 0) {
            return true;
        }

        for (String value : row) {
            if (value != null && !value.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private Order parseRow(String[] row, int rowNumber) {
        if (row.length != EXPECTED_HEADERS.length) {
            throw new InvalidCsvException(
                    "Row " + rowNumber + " has " + row.length
                            + " columns; expected " + EXPECTED_HEADERS.length
            );
        }

        String orderId = value(row[0]);
        String clientName = value(row[1]);
        String stockName = value(row[2]);
        String orderQtyText = value(row[3]);
        String priceText = value(row[4]);
        String timestampText = value(row[5]);
        String orderTypeText = value(row[6]);

        if (orderId == null || !orderId.matches("\\d{12}")) {
            throw new InvalidCsvException("orderId must contain exactly 12 digits");
        }

        if (clientName == null) {
            throw new InvalidCsvException("clientName is missing");
        }

        if (stockName == null) {
            throw new InvalidCsvException("stockName is missing");
        }

        BigDecimal orderQty = parseDecimal(orderQtyText, "orderQty", rowNumber);
        if (orderQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidCsvException("orderQty must be positive");
        }
        if (orderQty.scale() > 2) {
            throw new InvalidCsvException("orderQty must have at most 2 decimal places");
        }

        BigDecimal price = parseDecimal(priceText, "price", rowNumber);
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidCsvException("price must be positive");
        }
        if (price.scale() > 1) {
            throw new InvalidCsvException("price must have at most 1 decimal place");
        }

        if (timestampText == null) {
            throw new InvalidCsvException("orderTimestamp is missing");
        }

        LocalDateTime orderTimestamp;
        try {
            orderTimestamp = LocalDateTime.parse(timestampText);
        } catch (DateTimeParseException e) {
            throw new InvalidCsvException("orderTimestamp is invalid: '" + timestampText + "'");
        }

        if (orderTypeText == null) {
            throw new InvalidCsvException("orderType is missing");
        }

        OrderType orderType;
        try {
            orderType = OrderType.valueOf(orderTypeText.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidCsvException("orderType must be BUY or SELL");
        }

        return new Order(
                orderId,
                clientName,
                stockName,
                orderQty,
                price,
                orderTimestamp,
                orderType
        );
    }

    private BigDecimal parseDecimal(String value, String fieldName, int rowNumber) {
        if (value == null) {
            throw new InvalidCsvException(fieldName + " is missing");
        }

        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new InvalidCsvException(
                    "Row " + rowNumber + ": " + fieldName + " is not a valid number"
            );
        }
    }

    private String value(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
