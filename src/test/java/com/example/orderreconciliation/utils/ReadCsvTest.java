package com.example.orderreconciliation.utils;

import com.example.orderreconciliation.exception.InvalidCsvException;
import com.example.orderreconciliation.model.Order;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReadCsvTest {

    private final ReadCsv readCsv = new ReadCsv();

    @Test
    void validRowsAreReturned() {
        MockMultipartFile file = csv("orders.csv",
                "orderId,clientName,stockName,orderQty,price,orderTimestamp,orderType\n" +
                "100000000001,ABC,TCS,100.25,3500.0,2026-09-08T10:00:00,BUY\n" +
                "100000000002,XYZ,INFY,200.50,1800.0,2026-09-08T10:05:00,SELL\n");

        List<Order> result = readCsv.readCsv(file);

        assertEquals(2, result.size());
    }

    @Test
    void invalidHeaderRejectsWholeFile() {
        MockMultipartFile file = csv("orders.csv",
                "wrong,header,stockName,orderQty,price,orderTimestamp,orderType\n" +
                "100000000001,ABC,TCS,100.25,3500.0,2026-09-08T10:00:00,BUY\n");

        assertThrows(InvalidCsvException.class, () -> readCsv.readCsv(file));
    }

    @Test
    void invalidRowsAreSkipped() {
        MockMultipartFile file = csv("orders.csv",
                "orderId,clientName,stockName,orderQty,price,orderTimestamp,orderType\n" +
                "100000000001,ABC,TCS,100.25,3500.0,2026-09-08T10:00:00,BUY\n" +
                "bad-id,ABC,TCS,100.25,3500.0,2026-09-08T10:00:00,BUY\n" +
                "100000000002,XYZ,INFY,,1800.0,2026-09-08T10:05:00,SELL\n" +
                "100000000003,DEF,WIPRO,50.00,400.5,2026-09-08T10:10:00,BUY,EXTRA\n");

        List<Order> result = readCsv.readCsv(file);

        assertEquals(1, result.size());
        assertEquals("100000000001", result.get(0).getOrderId());
    }

    @Test
    void duplicateOrderIdIsSkipped() {
        MockMultipartFile file = csv("orders.csv",
                "orderId,clientName,stockName,orderQty,price,orderTimestamp,orderType\n" +
                "100000000001,ABC,TCS,100.25,3500.0,2026-09-08T10:00:00,BUY\n" +
                "100000000001,XYZ,INFY,200.50,1800.0,2026-09-08T10:05:00,SELL\n");

        List<Order> result = readCsv.readCsv(file);

        assertEquals(1, result.size());
    }

    private MockMultipartFile csv(String filename, String content) {
        return new MockMultipartFile(
                "file",
                filename,
                "text/csv",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }
}
