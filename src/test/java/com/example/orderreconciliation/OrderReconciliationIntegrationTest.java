package com.example.orderreconciliation;

import com.example.orderreconciliation.enums.OrderType;
import com.example.orderreconciliation.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderReconciliationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Value("${app.csv.file-path}")
    private String csvFilePath;

    @BeforeEach
    void setUp() throws Exception {
        orderRepository.deleteAll();
        Files.deleteIfExists(Path.of(csvFilePath));
    }

    @Test
    void uploadCsvSkipsInvalidRowsButAcceptsValidRows() throws Exception {
        String csv = "orderId,clientName,stockName,orderQty,price,orderTimestamp,orderType\n"
                + "100000000001,ABC,TCS,100.25,3500.0,2026-09-08T10:00:00,BUY\n"
                + "bad-id,ABC,TCS,100.25,3500.0,2026-09-08T10:00:00,BUY\n"
                + "100000000002,XYZ,INFY,,1800.0,2026-09-08T10:05:00,SELL\n";

        MockMultipartFile file = new MockMultipartFile("file", "orders.csv", "text/csv", csv.getBytes());

        mockMvc.perform(multipart("/api/v1/orders/csv").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordCount", is(1)))
                .andExpect(jsonPath("$.orders", hasSize(1)))
                .andExpect(jsonPath("$.orders[0].orderId", is("100000000001")));
    }

    @Test
    void invalidHeaderRejectsWholeFile() throws Exception {
        String csv = "badHeader,clientName,stockName,orderQty,price,orderTimestamp,orderType\n"
                + "100000000001,ABC,TCS,100.25,3500.0,2026-09-08T10:00:00,BUY\n";

        MockMultipartFile file = new MockMultipartFile("file", "orders.csv", "text/csv", csv.getBytes());

        mockMvc.perform(multipart("/api/v1/orders/csv").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_CSV")));
    }

    @Test
    void reconciliationUsesDbAndBackendCsvAsSeparateSources() throws Exception {
        mockMvc.perform(post("/api/v1/orders/table")
                        .contentType("application/json")
                        .content("{\"orderId\":\"100000000001\",\"clientName\":\"ABC\",\"stockName\":\"TCS\",\"orderQty\":100.25,\"price\":3500.0,\"orderTimestamp\":\"2026-09-08T10:00:00\",\"orderType\":\"BUY\"}"))
                .andExpect(status().isOk());

        String csv = "orderId,clientName,stockName,orderQty,price,orderTimestamp,orderType\n"
                + "100000000001,ABC,TCS,100.25,3500.0,2026-09-08T10:00:00,BUY\n";

        mockMvc.perform(multipart("/api/v1/orders/csv")
                        .file(new MockMultipartFile("file", "orders.csv", "text/csv", csv.getBytes())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/reconciliation/matched"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].orderId", is("100000000001")));
    }
}
