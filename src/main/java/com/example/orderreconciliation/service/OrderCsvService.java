package com.example.orderreconciliation.service;

import com.example.orderreconciliation.exception.InvalidCsvException;
import com.example.orderreconciliation.model.Order;
import com.example.orderreconciliation.utils.ReadCsv;
import com.opencsv.CSVWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * New CSV flow:
 * upload -> ReadCsv -> valid orders -> write backend CSV.
 */
@Service
public class OrderCsvService {

    private final ReadCsv readCsv;
    private final String csvFilePath;

    public OrderCsvService(ReadCsv readCsv,
                           @Value("${app.csv.file-path}") String csvFilePath) {
        this.readCsv = readCsv;
        this.csvFilePath = csvFilePath;
    }

    @Transactional
    public List<Order> uploadCsv(MultipartFile file) {
        validateFile(file);

        List<Order> validOrders = readCsv.readCsv(file);
        writeOrders(validOrders);

        return validOrders;
    }

    public List<Order> getAllOrders() {
        return readCsv.readCsv(csvFilePath);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidCsvException("CSV file is required and must not be empty");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new InvalidCsvException("File must have a .csv extension. Received: " + filename);
        }
    }

    private void writeOrders(List<Order> orders) {
        File target = new File(csvFilePath);
        File parent = target.getParentFile();

        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new InvalidCsvException("Unable to create CSV directory: " + parent);
        }

        File temp;
        try {
            temp = File.createTempFile("orders_", ".csv", parent);
        } catch (IOException e) {
            throw new InvalidCsvException("Unable to create temporary CSV file");
        }

        try (CSVWriter writer = new CSVWriter(new FileWriter(temp))) {
            writer.writeNext(new String[]{
                    "orderId", "clientName", "stockName", "orderQty",
                    "price", "orderTimestamp", "orderType"
            });

            for (Order order : orders) {
                writer.writeNext(new String[]{
                        order.getOrderId(),
                        order.getClientName(),
                        order.getStockName(),
                        order.getOrderQty().toPlainString(),
                        order.getPrice().toPlainString(),
                        order.getOrderTimestamp().toString(),
                        order.getOrderType().name()
                });
            }
        } catch (IOException e) {
            temp.delete();
            throw new InvalidCsvException("Unable to write CSV file: " + e.getMessage());
        }

        if (!temp.renameTo(target)) {
            temp.delete();
            throw new InvalidCsvException("Unable to replace backend CSV file");
        }
    }
}
