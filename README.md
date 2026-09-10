# Order Reconciliation System

A Spring Boot application that reconciles order records between two independent data sources: a **Database** (Source A) and a **backend CSV file** (Source B).

## New CSV Flow

The old `CsvService` / `CsvFileHandler` flow has been removed. CSV processing now follows one simple flow:

```text
CSV upload
   |
   v
CsvController
   |
   v
OrderCsvService
   |
   v
ReadCsv
   |
   +--> Validate header
   |       |
   |       +--> wrong header -> reject complete file
   |
   +--> Read data rows
           |
           +--> valid row   -> collect Order
           |
           +--> invalid row -> skip row and continue
   |
   v
List<Order> containing only valid rows
   |
   v
Write all valid rows to backend CSV once
   |
   v
./data/orders.csv
```

Header validation is file-level validation. A wrong header rejects the complete upload. Data-row validation is row-level validation: missing fields, wrong column count, invalid numbers, invalid timestamp, invalid order type, or other row errors cause only that row to be skipped.

## Reconciliation Flow

```text
Database Source A                 CSV Source B
      |                                  |
      v                                  v
OrderTableService                OrderCsvService
      |                                  |
      v                                  v
HashMap<String, Order>           HashMap<String, Order>
      |                                  |
      +---------------+------------------+
                      |
                      v
              HashSet<String>
              union of orderIds
                      |
                      v
              Compare all fields
                      |
          +-----------+-----------+
          |           |           |
          v           v           v
       MATCHED    TABLE NOT    CSV NOT
                  MATCHED      MATCHED
```

The reconciliation uses `HashMap` for O(1) average lookup by `orderId` and one `HashSet` containing the union of IDs. There are no nested loops.

## Package Structure

```text
com.example.orderreconciliation
├── controller
│   ├── OrderTableController
│   ├── CsvController
│   └── ReconciliationController
│
├── service
│   ├── OrderTableService
│   ├── OrderCsvService
│   └── ReconciliationService
│
├── utils
│   └── ReadCsv
│
├── repository
│   └── OrderRepository
│
├── entity
│   └── OrderEntity
│
├── model
│   └── Order                  # DTO + application model
│
├── dto
│   └── ReconciliationResult
│
├── mapper
│   └── OrderMapper
│
├── enums
│   └── OrderType              # BUY, SELL
│
└── exception
    ├── InvalidOrderException
    ├── InvalidCsvException
    └── GlobalExceptionHandler
```

The legacy `csv/CsvFileHandler.java` and `service/CsvService.java` are intentionally removed.

## Data Model

### Database: `orders`

| Column          | Type           | Constraints |
|-----------------|----------------|-------------|
| id              | BIGINT         | Primary Key, auto-increment |
| order_id        | VARCHAR(12)    | NOT NULL, UNIQUE |
| client_name     | VARCHAR        | NOT NULL |
| stock_name      | VARCHAR        | NOT NULL |
| order_qty       | DECIMAL(17,2)  | NOT NULL |
| price           | DECIMAL(16,1)  | NOT NULL |
| order_timestamp | TIMESTAMP      | NOT NULL |
| order_type      | VARCHAR        | NOT NULL (BUY/SELL) |

### CSV Format

```csv
orderId,clientName,stockName,orderQty,price,orderTimestamp,orderType
100000000001,ABC,TCS,100.25,3500.0,2026-09-08T10:00:00,BUY
100000000002,XYZ,INFY,200.50,1800.0,2026-09-08T10:05:00,SELL
```

Validation rules:
- `orderId`: exactly 12 digits
- `clientName`: mandatory
- `stockName`: mandatory
- `orderQty`: positive number, maximum 2 decimal places
- `price`: positive number, maximum 1 decimal place
- `orderTimestamp`: valid `LocalDateTime`
- `orderType`: `BUY` or `SELL`
- row must contain exactly 7 columns
- duplicate `orderId` rows are skipped after the first valid occurrence

## APIs

### 1. Insert/Update Database Order

```text
POST /api/v1/orders/table
Content-Type: application/json
```

The existing `OrderTableService` inserts a new record or updates an existing record by `orderId`.

### 2. Upload CSV

```text
POST /api/v1/orders/csv
Content-Type: multipart/form-data
```

Processing order:
1. Validate file presence and `.csv` extension.
2. `ReadCsv` reads the header.
3. Invalid header -> HTTP 400 and no backend CSV replacement.
4. Read every data row.
5. Invalid row -> skip and continue.
6. Collect all valid `Order` objects.
7. Write the collected valid records to the backend CSV in one write operation.
8. Replace the previous backend CSV only after the new file has been written successfully.

A successful response contains only the valid records that were accepted.

### 3. Get Matched Records

```text
GET /api/v1/reconciliation/matched
```

Returns records that exist in both sources and have all fields equal.

### 4. Get Table Not-Matched Records

```text
GET /api/v1/reconciliation/table-not-matched
```

Returns DB records that either do not exist in CSV or have different field values.

### 5. Get CSV Not-Matched Records

```text
GET /api/v1/reconciliation/csv-not-matched
```

Returns CSV records that either do not exist in DB or have different field values.

## Reconciliation Algorithm

```text
1. Read DB orders.
2. Read backend CSV orders.
3. Build tableMap = HashMap<orderId, Order>.
4. Build csvMap   = HashMap<orderId, Order>.
5. Build allOrderIds = HashSet containing keys from both maps.
6. Iterate once over allOrderIds.
7. For each orderId:
   - Both exist + all fields equal -> MATCHED
   - Both exist + any field differs -> DB version to TABLE NOT MATCHED,
                                      CSV version to CSV NOT MATCHED
   - Only DB exists -> TABLE NOT MATCHED
   - Only CSV exists -> CSV NOT MATCHED
```

`BigDecimal.compareTo()` is used for `orderQty` and `price`, so values such as `3500.0` and `3500` are treated as numerically equal during reconciliation.

## Design Decisions

1. **Order remains the common DTO/application model.** `OrderEntity` remains separate for JPA.
2. **ReadCsv owns CSV parsing and row validation.** It returns only valid `Order` objects.
3. **OrderCsvService owns upload orchestration and backend CSV persistence.**
4. **Invalid header is a file-level failure; invalid data rows are row-level failures.**
5. **The CSV source remains independent from the database source.** The CSV upload does not insert CSV records into the `orders` database table; otherwise reconciliation would compare the same source with itself.
6. **HashMap + HashSet** are used for O(n) reconciliation.
7. **Temporary-file replacement** prevents a partially written backend CSV from becoming the active source.

## How to Run

Prerequisites:
- Java 17+
- Maven 3.x

Start:

```bash
mvn spring-boot:run
```

Application URL: `http://localhost:8080`

Swagger UI: `http://localhost:8080/swagger-ui.html`

H2 Console: `http://localhost:8080/h2-console`

## Tests

Run:

```bash
mvn test
```

Tests cover:
- valid CSV rows
- invalid header rejection
- invalid row skipping
- duplicate row skipping
- DB-only reconciliation
- CSV-only reconciliation
- matched records
- field mismatch reconciliation
- end-to-end CSV upload and reconciliation

## Sample curl

### Database order

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

### CSV upload

```bash
curl -X POST http://localhost:8080/api/v1/orders/csv \
  -F "file=@data/orders.csv"
```

### Reconciliation

```bash
curl http://localhost:8080/api/v1/reconciliation/matched
curl http://localhost:8080/api/v1/reconciliation/table-not-matched
curl http://localhost:8080/api/v1/reconciliation/csv-not-matched
```

## Technology Stack

| Component | Technology |
|---|---|
| Framework | Spring Boot 3.2.5 |
| Language | Java 17+ |
| Database | H2 file-based |
| ORM | Spring Data JPA |
| CSV | OpenCSV 5.9 |
| Validation | Jakarta Bean Validation + explicit CSV validation |
| API Docs | SpringDoc OpenAPI 2.5 |
| Testing | JUnit 5 + Mockito |
| Build | Maven |
