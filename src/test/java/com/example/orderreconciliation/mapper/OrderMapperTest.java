package com.example.orderreconciliation.mapper;

import com.example.orderreconciliation.entity.OrderEntity;
import com.example.orderreconciliation.enums.OrderType;
import com.example.orderreconciliation.model.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class OrderMapperTest {

    private final OrderMapper orderMapper = new OrderMapper();

    @Test
    @DisplayName("Should convert OrderEntity to Order model")
    void toModel() {
        OrderEntity entity = new OrderEntity();
        entity.setId(1L);
        entity.setOrderId("100000000001");
        entity.setClientName("ABC");
        entity.setStockName("TCS");
        entity.setOrderQty(new BigDecimal("100.25"));
        entity.setPrice(new BigDecimal("3500.0"));
        entity.setOrderTimestamp(LocalDateTime.of(2026, 9, 8, 10, 0, 0));
        entity.setOrderType(OrderType.BUY);

        Order order = orderMapper.toModel(entity);

        assertNotNull(order);
        assertEquals("100000000001", order.getOrderId());
        assertEquals("ABC", order.getClientName());
        assertEquals("TCS", order.getStockName());
        assertEquals(new BigDecimal("100.25"), order.getOrderQty());
        assertEquals(new BigDecimal("3500.0"), order.getPrice());
        assertEquals(LocalDateTime.of(2026, 9, 8, 10, 0, 0), order.getOrderTimestamp());
        assertEquals(OrderType.BUY, order.getOrderType());
    }

    @Test
    @DisplayName("Should convert Order model to OrderEntity")
    void toEntity() {
        Order order = new Order("100000000001", "ABC", "TCS",
                new BigDecimal("100.25"), new BigDecimal("3500.0"),
                LocalDateTime.of(2026, 9, 8, 10, 0, 0), OrderType.BUY);

        OrderEntity entity = orderMapper.toEntity(order);

        assertNotNull(entity);
        assertNull(entity.getId()); // No DB ID set for new entity
        assertEquals("100000000001", entity.getOrderId());
        assertEquals("ABC", entity.getClientName());
    }

    @Test
    @DisplayName("Should update existing OrderEntity from Order model")
    void updateEntity() {
        OrderEntity entity = new OrderEntity();
        entity.setId(1L);
        entity.setOrderId("100000000001");
        entity.setClientName("OLD");
        entity.setPrice(new BigDecimal("1000.0"));

        Order order = new Order("100000000001", "NEW", "TCS",
                new BigDecimal("100.25"), new BigDecimal("3500.0"),
                LocalDateTime.of(2026, 9, 8, 10, 0, 0), OrderType.BUY);

        orderMapper.updateEntity(entity, order);

        assertEquals(1L, entity.getId()); // ID preserved
        assertEquals("NEW", entity.getClientName());
        assertEquals(new BigDecimal("3500.0"), entity.getPrice());
    }

    @Test
    @DisplayName("Should handle null inputs gracefully")
    void nullInputs() {
        assertNull(orderMapper.toModel(null));
        assertNull(orderMapper.toEntity(null));
        // updateEntity with nulls should not throw
        orderMapper.updateEntity(null, new Order());
        orderMapper.updateEntity(new OrderEntity(), null);
    }
}
