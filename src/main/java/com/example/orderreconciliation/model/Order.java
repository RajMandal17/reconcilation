package com.example.orderreconciliation.model;

import com.example.orderreconciliation.enums.OrderType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Common Order class used as DTO + application model across
 * REST API, CSV processing, and reconciliation.
 */
public class Order {

    @NotBlank(message = "orderId is mandatory")
    @Pattern(regexp = "\\d{12}", message = "orderId must contain exactly 12 digits")
    private String orderId;

    @NotBlank(message = "clientName is mandatory")
    private String clientName;

    @NotBlank(message = "stockName is mandatory")
    private String stockName;

    @NotNull(message = "orderQty is mandatory")
    @DecimalMin(value = "0.0", inclusive = false, message = "orderQty must be positive")
    @Digits(integer = 15, fraction = 2, message = "orderQty must have at most 2 decimal places")
    private BigDecimal orderQty;

    @NotNull(message = "price is mandatory")
    @DecimalMin(value = "0.0", inclusive = false, message = "price must be positive")
    @Digits(integer = 15, fraction = 1, message = "price must have at most 1 decimal place")
    private BigDecimal price;

    @NotNull(message = "orderTimestamp is mandatory")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime orderTimestamp;

    @NotNull(message = "orderType is mandatory")
    private OrderType orderType;

    public Order() {
    }

    public Order(String orderId, String clientName, String stockName,
                 BigDecimal orderQty, BigDecimal price,
                 LocalDateTime orderTimestamp, OrderType orderType) {
        this.orderId = orderId;
        this.clientName = clientName;
        this.stockName = stockName;
        this.orderQty = orderQty;
        this.price = price;
        this.orderTimestamp = orderTimestamp;
        this.orderType = orderType;
    }

    // Getters and Setters

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public BigDecimal getOrderQty() {
        return orderQty;
    }

    public void setOrderQty(BigDecimal orderQty) {
        this.orderQty = orderQty;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDateTime getOrderTimestamp() {
        return orderTimestamp;
    }

    public void setOrderTimestamp(LocalDateTime orderTimestamp) {
        this.orderTimestamp = orderTimestamp;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderId='" + orderId + '\'' +
                ", clientName='" + clientName + '\'' +
                ", stockName='" + stockName + '\'' +
                ", orderQty=" + orderQty +
                ", price=" + price +
                ", orderTimestamp=" + orderTimestamp +
                ", orderType=" + orderType +
                '}';
    }
}
