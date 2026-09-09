package com.example.orderreconciliation.service;

import com.example.orderreconciliation.csv.CsvFileHandler;
import com.example.orderreconciliation.exception.InvalidCsvException;
import com.example.orderreconciliation.model.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CsvServiceTest {

    @Mock
    private CsvFileHandler csvFileHandler;

    @InjectMocks
    private CsvService csvService;

    @Test
    @DisplayName("Should reject null file")
    void processUploadedCsv_nullFile() {
        assertThrows(InvalidCsvException.class, () -> csvService.processUploadedCsv(null));
    }

    @Test
    @DisplayName("Should reject empty file")
    void processUploadedCsv_emptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "orders.csv", "text/csv", new byte[0]);

        assertThrows(InvalidCsvException.class, () -> csvService.processUploadedCsv(emptyFile));
    }

    @Test
    @DisplayName("Should reject file without .csv extension")
    void processUploadedCsv_wrongExtension() {
        MockMultipartFile txtFile = new MockMultipartFile(
                "file", "orders.txt", "text/plain", "some content".getBytes());

        InvalidCsvException ex = assertThrows(InvalidCsvException.class,
                () -> csvService.processUploadedCsv(txtFile));
        assertEquals("File must have a .csv extension. Received: orders.txt", ex.getMessage());
    }

    @Test
    @DisplayName("Should reject CSV with invalid header")
    void processUploadedCsv_invalidHeader() {
        String csvContent = "wrongCol1,wrongCol2,wrongCol3,wrongCol4,wrongCol5,wrongCol6,wrongCol7\n" +
                "100000000001,ABC,TCS,100.25,3500.0,2026-09-08T10:00:00,BUY\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "orders.csv", "text/csv", csvContent.getBytes());

        // The CsvFileHandler.parseAndValidate is called inside processUploadedCsv
        // and will throw InvalidCsvException for invalid header.
        // Since csvFileHandler is mocked, we need to simulate this.
        org.mockito.Mockito.doThrow(new InvalidCsvException("CSV header column 1 must be 'orderId'"))
                .when(csvFileHandler).parseAndValidate(any());

        assertThrows(InvalidCsvException.class, () -> csvService.processUploadedCsv(file));
        verify(csvFileHandler, never()).writeOrders(any());
    }

    @Test
    @DisplayName("Should reject CSV with duplicate orderId")
    void processUploadedCsv_duplicateOrderId() {
        String csvContent = "orderId,clientName,stockName,orderQty,price,orderTimestamp,orderType\n" +
                "100000000001,ABC,TCS,100.25,3500.0,2026-09-08T10:00:00,BUY\n" +
                "100000000001,XYZ,INFY,200.50,1800.0,2026-09-08T10:05:00,SELL\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "orders.csv", "text/csv", csvContent.getBytes());

        org.mockito.Mockito.doThrow(new InvalidCsvException("duplicate orderId '100000000001'"))
                .when(csvFileHandler).parseAndValidate(any());

        assertThrows(InvalidCsvException.class, () -> csvService.processUploadedCsv(file));
        verify(csvFileHandler, never()).writeOrders(any());
    }

    @Test
    @DisplayName("Should process valid CSV successfully")
    void processUploadedCsv_validCsv() {
        String csvContent = "orderId,clientName,stockName,orderQty,price,orderTimestamp,orderType\n" +
                "100000000001,ABC,TCS,100.25,3500.0,2026-09-08T10:00:00,BUY\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "orders.csv", "text/csv", csvContent.getBytes());

        Order mockOrder = new Order();
        mockOrder.setOrderId("100000000001");
        List<Order> mockOrders = List.of(mockOrder);

        org.mockito.Mockito.when(csvFileHandler.parseAndValidate(any())).thenReturn(mockOrders);

        List<Order> result = csvService.processUploadedCsv(file);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(csvFileHandler).writeOrders(mockOrders);
    }
}
