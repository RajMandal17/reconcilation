package com.example.orderreconciliation;

import com.example.orderreconciliation.csv.CsvFileHandler;
import com.example.orderreconciliation.enums.OrderType;
import com.example.orderreconciliation.model.Order;
import com.example.orderreconciliation.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CsvFileHandler csvFileHandler;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        // Write an empty CSV to reset state
        csvFileHandler.writeOrders(List.of());
    }

    // ============================
    // POST /api/v1/orders/table
    // ============================

    @Test
    @DisplayName("POST /api/v1/orders/table - should insert new order")
    void postTable_insertNewOrder() throws Exception {
        Order order = new Order("100000000001", "ABC", "TCS",
                new BigDecimal("100.25"), new BigDecimal("3500.0"),
                LocalDateTime.of(2026, 9, 8, 10, 0, 0), OrderType.BUY);

        mockMvc.perform(post("/api/v1/orders/table")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Order saved successfully")))
                .andExpect(jsonPath("$.order.orderId", is("100000000001")))
                .andExpect(jsonPath("$.order.clientName", is("ABC")));
    }

    @Test
    @DisplayName("POST /api/v1/orders/table - should update existing order")
    void postTable_updateExistingOrder() throws Exception {
        // Insert first
        Order order = new Order("100000000001", "ABC", "TCS",
                new BigDecimal("100.25"), new BigDecimal("3500.0"),
                LocalDateTime.of(2026, 9, 8, 10, 0, 0), OrderType.BUY);

        mockMvc.perform(post("/api/v1/orders/table")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isOk());

        // Update with different price
        order.setPrice(new BigDecimal("4000.0"));
        mockMvc.perform(post("/api/v1/orders/table")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.price", is(4000.0)));
    }

    @Test
    @DisplayName("POST /api/v1/orders/table - should fail validation for invalid orderId")
    void postTable_invalidOrderId() throws Exception {
        Order order = new Order("12345", "ABC", "TCS",
                new BigDecimal("100.25"), new BigDecimal("3500.0"),
                LocalDateTime.of(2026, 9, 8, 10, 0, 0), OrderType.BUY);

        mockMvc.perform(post("/api/v1/orders/table")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("POST /api/v1/orders/table - should fail validation for missing clientName")
    void postTable_missingClientName() throws Exception {
        String json = """
                {
                  "orderId": "100000000001",
                  "clientName": "",
                  "stockName": "TCS",
                  "orderQty": 100.25,
                  "price": 3500.0,
                  "orderTimestamp": "2026-09-08T10:00:00",
                  "orderType": "BUY"
                }
                """;

        mockMvc.perform(post("/api/v1/orders/table")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    // ============================
    // POST /api/v1/orders/csv
    // ============================

    @Test
    @DisplayName("POST /api/v1/orders/csv - should upload valid CSV")
    void postCsv_validFile() throws Exception {
        String csvContent = "orderId,clientName,stockName,orderQty,price,orderTimestamp,orderType\n" +
                "100000000001,ABC,TCS,100.25,3500.0,2026-09-08T10:00:00,BUY\n" +
                "100000000002,XYZ,INFY,200.50,1800.0,2026-09-08T10:05:00,SELL\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "orders.csv", "text/csv", csvContent.getBytes());

        mockMvc.perform(multipart("/api/v1/orders/csv").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("CSV processed successfully")))
                .andExpect(jsonPath("$.recordCount", is(2)));
    }

    @Test
    @DisplayName("POST /api/v1/orders/csv - should reject non-CSV file")
    void postCsv_nonCsvFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "orders.txt", "text/plain", "some content".getBytes());

        mockMvc.perform(multipart("/api/v1/orders/csv").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_CSV")));
    }

    @Test
    @DisplayName("POST /api/v1/orders/csv - should reject CSV with invalid header")
    void postCsv_invalidHeader() throws Exception {
        String csvContent = "wrong1,wrong2,wrong3,wrong4,wrong5,wrong6,wrong7\n" +
                "100000000001,ABC,TCS,100.25,3500.0,2026-09-08T10:00:00,BUY\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "orders.csv", "text/csv", csvContent.getBytes());

        mockMvc.perform(multipart("/api/v1/orders/csv").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_CSV")));
    }

    @Test
    @DisplayName("POST /api/v1/orders/csv - should reject CSV with duplicate orderId")
    void postCsv_duplicateOrderId() throws Exception {
        String csvContent = "orderId,clientName,stockName,orderQty,price,orderTimestamp,orderType\n" +
                "100000000001,ABC,TCS,100.25,3500.0,2026-09-08T10:00:00,BUY\n" +
                "100000000001,XYZ,INFY,200.50,1800.0,2026-09-08T10:05:00,SELL\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "orders.csv", "text/csv", csvContent.getBytes());

        mockMvc.perform(multipart("/api/v1/orders/csv").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_CSV")));
    }

    // ============================
    // Reconciliation APIs
    // ============================

    @Test
    @DisplayName("GET /api/v1/reconciliation/matched - matching records")
    void getMatched() throws Exception {
        // Insert same record in DB and CSV
        Order order = new Order("100000000001", "ABC", "TCS",
                new BigDecimal("100.25"), new BigDecimal("3500.0"),
                LocalDateTime.of(2026, 9, 8, 10, 0, 0), OrderType.BUY);

        mockMvc.perform(post("/api/v1/orders/table")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isOk());

        String csvContent = "orderId,clientName,stockName,orderQty,price,orderTimestamp,orderType\n" +
                "100000000001,ABC,TCS,100.25,3500.0,2026-09-08T10:00:00,BUY\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "orders.csv", "text/csv", csvContent.getBytes());

        mockMvc.perform(multipart("/api/v1/orders/csv").file(file))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/reconciliation/matched"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].orderId", is("100000000001")));
    }

    @Test
    @DisplayName("GET /api/v1/reconciliation/table-not-matched - DB only record")
    void getTableNotMatched() throws Exception {
        // Insert record only in DB
        Order order = new Order("100000000001", "ABC", "TCS",
                new BigDecimal("100.25"), new BigDecimal("3500.0"),
                LocalDateTime.of(2026, 9, 8, 10, 0, 0), OrderType.BUY);

        mockMvc.perform(post("/api/v1/orders/table")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isOk());

        // CSV is empty (from setUp)

        mockMvc.perform(get("/api/v1/reconciliation/table-not-matched"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].orderId", is("100000000001")));
    }

    @Test
    @DisplayName("GET /api/v1/reconciliation/csv-not-matched - CSV only record")
    void getCsvNotMatched() throws Exception {
        // Upload CSV with record
        String csvContent = "orderId,clientName,stockName,orderQty,price,orderTimestamp,orderType\n" +
                "100000000001,ABC,TCS,100.25,3500.0,2026-09-08T10:00:00,BUY\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "orders.csv", "text/csv", csvContent.getBytes());

        mockMvc.perform(multipart("/api/v1/orders/csv").file(file))
                .andExpect(status().isOk());

        // DB is empty (from setUp)

        mockMvc.perform(get("/api/v1/reconciliation/csv-not-matched"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].orderId", is("100000000001")));
    }

    @Test
    @DisplayName("GET reconciliation - field mismatch should show in both not-matched lists")
    void getReconciliation_fieldMismatch() throws Exception {
        // Insert in DB with price 3500.0
        Order order = new Order("100000000001", "ABC", "TCS",
                new BigDecimal("100.25"), new BigDecimal("3500.0"),
                LocalDateTime.of(2026, 9, 8, 10, 0, 0), OrderType.BUY);

        mockMvc.perform(post("/api/v1/orders/table")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isOk());

        // Upload CSV with same orderId but different price
        String csvContent = "orderId,clientName,stockName,orderQty,price,orderTimestamp,orderType\n" +
                "100000000001,ABC,TCS,100.25,4000.0,2026-09-08T10:00:00,BUY\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "orders.csv", "text/csv", csvContent.getBytes());

        mockMvc.perform(multipart("/api/v1/orders/csv").file(file))
                .andExpect(status().isOk());

        // Matched should be empty
        mockMvc.perform(get("/api/v1/reconciliation/matched"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Table not-matched should have 1 (DB version)
        mockMvc.perform(get("/api/v1/reconciliation/table-not-matched"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].price", is(3500.0)));

        // CSV not-matched should have 1 (CSV version)
        mockMvc.perform(get("/api/v1/reconciliation/csv-not-matched"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].price", is(4000.0)));
    }

    @Test
    @DisplayName("GET reconciliation - both sources empty")
    void getReconciliation_bothEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/reconciliation/matched"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/v1/reconciliation/table-not-matched"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/v1/reconciliation/csv-not-matched"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
