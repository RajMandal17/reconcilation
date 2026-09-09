# Order Reconciliation System

A Spring Boot application that reconciles order records between two data sources: a **Database** (Source A) and a **CSV file** (Source B).

## Overview

The system provides REST APIs to:
1. Insert/update orders in the database
2. Upload order data via CSV files
3. Reconcile records between the two sources using an efficient HashMap-based algorithm

## Architecture

```
                    CLIENT
                       |
        +--------------+--------------+
        |              |              |
        v              v              v
  OrderTable       CsvController   Reconciliation
  Controller                       Controller
        |              |              |
        v              v              v
  OrderTable        CsvService    Reconciliation
    Service                          Service
        |              |              |
        v              v              |
 OrderRepository  CsvFileHandler      |
        |              |              |
        v              v              |
   DATABASE          CSV FILE          |
        |              |              |
        +--------------+--------------+
                       |
                       v
                 Order objects
                       |
                       v
              HashMap<String,Order>
                       |
                       v
                  HashSet<String>
                       |
                       v
                 Compare fields
                       |
          +------------+------------+
          |            |            |
          v            v            v
       MATCHED     TABLE NOT      CSV NOT
                   MATCHED        MATCHED
```

## Package Structure

```
com.example.orderreconciliation
├── controller
│   ├── OrderTableController      # POST /api/v1/orders/table
│   ├── CsvController             # POST /api/v1/orders/csv
│   └── ReconciliationController  # GET /api/v1/reconciliation/*
│
├── service
│   ├── OrderTableService         # Database order operations
│   ├── CsvService                # CSV upload/validation
│   └── ReconciliationService     # Core reconciliation logic
│
├── repository
│   └── OrderRepository           # JPA repository
│
├── entity
│   └── OrderEntity               # JPA entity for orders table
│
├── model
│   └── Order                     # DTO + application model
│
├── dto
│   └── ReconciliationResult      # Reconciliation output
│
├── mapper
│   └── OrderMapper               # Order <-> OrderEntity converter
│
├── csv
│   └── CsvFileHandler            # CSV read/write/validate
│
├── enums
│   └── OrderType                 # BUY, SELL
│
└── exception
    ├── InvalidOrderException     # Order validation errors
    ├── InvalidCsvException       # CSV validation errors
    └── GlobalExceptionHandler    # Centralized error handling
```

## Database Schema

**Table: `orders`**

| Column          | Type          | Constraints              |
|-----------------|---------------|--------------------------|
| id              | BIGINT        | Primary Key, Auto-increment |
| order_id        | VARCHAR(12)   | NOT NULL, UNIQUE         |
| client_name     | VARCHAR       | NOT NULL                 |
| stock_name      | VARCHAR       | NOT NULL                 |
| order_qty       | DECIMAL(17,2) | NOT NULL                 |
| price           | DECIMAL(16,1) | NOT NULL                 |
| order_timestamp | TIMESTAMP     | NOT NULL                 |
| order_type      | VARCHAR       | NOT NULL (BUY/SELL)      |

## CSV Format

```csv
orderId,clientName,stockName,orderQty,price,orderTimestamp,orderType
100000000001,ABC,TCS,100.25,3500.0,2026-09-08T10:00:00,BUY
100000000002,XYZ,INFY,200.50,1800.0,2026-09-08T10:05:00,SELL
```

## API Documentation

### 1. Insert/Update Order in Database

```
POST /api/v1/orders/table
Content-Type: application/json
```

**Request Body:**
```json
{
  "orderId": "100000000001",
  "clientName": "ABC",
  "stockName": "TCS",
  "orderQty": 100.25,
  "price": 3500.0,
  "orderTimestamp": "2026-09-08T10:00:00",
  "orderType": "BUY"
}
```

**Response (200 OK):**
```json
{
  "message": "Order saved successfully",
  "order": {
    "orderId": "100000000001",
    "clientName": "ABC",
    "stockName": "TCS",
    "orderQty": 100.25,
    "price": 3500.0,
    "orderTimestamp": "2026-09-08T10:00:00",
    "orderType": "BUY"
  }
}
```

### 2. Upload CSV File

```
POST /api/v1/orders/csv
Content-Type: multipart/form-data
```

**Response (200 OK):**
```json
{
  "message": "CSV processed successfully",
  "recordCount": 2,
  "orders": [...]
}
```

### 3. Get Matched Records

```
GET /api/v1/reconciliation/matched
```

Returns records that exist in both DB and CSV with all fields matching.

### 4. Get Table Not-Matched Records

```
GET /api/v1/reconciliation/table-not-matched
```

Returns DB records that either don't exist in CSV or have different field values.

### 5. Get CSV Not-Matched Records

```
GET /api/v1/reconciliation/csv-not-matched
```

Returns CSV records that either don't exist in DB or have different field values.

## Reconciliation Algorithm

The reconciliation uses **HashMap** and **HashSet** for O(n) performance:

```
1. Read all DB orders → HashMap<orderId, Order> (tableMap)
2. Read all CSV orders → HashMap<orderId, Order> (csvMap)
3. Create HashSet<String> = union of all orderIds from both maps
4. Single iteration over the union set:
   - If orderId in BOTH maps:
     - Compare ALL fields → MATCHED if equal, else both go to NOT-MATCHED
   - If orderId only in tableMap → TABLE NOT MATCHED
   - If orderId only in csvMap → CSV NOT MATCHED
```

### Why HashMap + HashSet?

- **HashMap** provides O(1) average lookup by orderId
- **HashSet** creates the union of all orderIds without duplicates
- **Single pass** over the union set classifies all records
- **Total complexity: O(n)** vs O(n²) with nested loops

### BigDecimal Comparison

Uses `compareTo() == 0` instead of `equals()` to handle scale differences:
```java
// 3500.0 and 3500 should be considered equal
tableOrder.getPrice().compareTo(csvOrder.getPrice()) == 0
```

## Design Decisions

1. **Single Order class** — Used as both DTO and application model to avoid unnecessary duplication. Only `OrderEntity` is separate for JPA persistence.

2. **Temporary file strategy for CSV** — Writes to a temp file first, then atomically replaces the original to prevent data corruption on failure.

3. **No nested loops** — Reconciliation uses HashMap/HashSet for O(n) performance instead of O(n²) nested comparisons.

4. **Single reconciliation pass** — The `reconcile()` method runs once and returns all three result categories. Individual getter methods delegate to it.

5. **Constructor injection** — All services use constructor injection with final fields for immutability and testability.

## How to Run

### Prerequisites
- Java 17+
- Maven 3.x

### Start the Application

```bash
mvn spring-boot:run
```

The application starts on `http://localhost:8080`.

### Access H2 Console

Navigate to `http://localhost:8080/h2-console` with:
- JDBC URL: `jdbc:h2:file:./data/orderdb`
- Username: `sa`
- Password: (empty)

### Access Swagger UI

Navigate to `http://localhost:8080/swagger-ui.html`

## How to Run Tests

```bash
mvn test
```

## Sample curl Commands

### Insert an order into the database

```bash
curl -X POST http://localhost:8080/api/v1/orders/table \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "100000000001",
    "clientName": "ABC",
    "stockName": "TCS",
    "orderQty": 100.25,
    "price": 3500.0,
    "orderTimestamp": "2026-09-08T10:00:00",
    "orderType": "BUY"
  }'
```

### Upload a CSV file

```bash
curl -X POST http://localhost:8080/api/v1/orders/csv \
  -F "file=@data/orders.csv"
```

### Get matched records

```bash
curl http://localhost:8080/api/v1/reconciliation/matched
```

### Get table not-matched records

```bash
curl http://localhost:8080/api/v1/reconciliation/table-not-matched
```

### Get CSV not-matched records

```bash
curl http://localhost:8080/api/v1/reconciliation/csv-not-matched
```

## Error Response Format

All errors return a consistent JSON structure:

```json
{
  "timestamp": "2026-09-09T10:00:00",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "orderId must contain exactly 12 digits"
}
```

## Technology Stack

| Component         | Technology              |
|-------------------|-------------------------|
| Framework         | Spring Boot 3.2.5       |
| Language          | Java 17+                |
| Database          | H2 (file-based)         |
| ORM               | Spring Data JPA         |
| CSV Processing    | OpenCSV 5.9             |
| Validation        | Jakarta Bean Validation |
| API Documentation | SpringDoc OpenAPI 2.5   |
| Testing           | JUnit 5 + Mockito       |
| Build             | Maven                   |
# reconcilation
