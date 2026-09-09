package com.example.orderreconciliation.service;

import com.example.orderreconciliation.entity.OrderEntity;
import com.example.orderreconciliation.enums.OrderType;
import com.example.orderreconciliation.mapper.OrderMapper;
import com.example.orderreconciliation.model.Order;
import com.example.orderreconciliation.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderTableServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderTableService orderTableService;

    private Order sampleOrder;
    private OrderEntity sampleEntity;

    @BeforeEach
    void setUp() {
        sampleOrder = new Order(
                "100000000001",
                "ABC",
                "TCS",
                new BigDecimal("100.25"),
                new BigDecimal("3500.0"),
                LocalDateTime.of(2026, 9, 8, 10, 0, 0),
                OrderType.BUY
        );

        sampleEntity = new OrderEntity();
        sampleEntity.setId(1L);
        sampleEntity.setOrderId("100000000001");
        sampleEntity.setClientName("ABC");
        sampleEntity.setStockName("TCS");
        sampleEntity.setOrderQty(new BigDecimal("100.25"));
        sampleEntity.setPrice(new BigDecimal("3500.0"));
        sampleEntity.setOrderTimestamp(LocalDateTime.of(2026, 9, 8, 10, 0, 0));
        sampleEntity.setOrderType(OrderType.BUY);
    }

    @Test
    @DisplayName("Should insert a new order when orderId does not exist")
    void saveOrder_insert() {
        when(orderRepository.findByOrderId("100000000001")).thenReturn(Optional.empty());
        when(orderMapper.toEntity(sampleOrder)).thenReturn(sampleEntity);
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(sampleEntity);
        when(orderMapper.toModel(sampleEntity)).thenReturn(sampleOrder);

        Order result = orderTableService.saveOrder(sampleOrder);

        assertNotNull(result);
        assertEquals("100000000001", result.getOrderId());
        verify(orderMapper).toEntity(sampleOrder);
        verify(orderRepository).save(any(OrderEntity.class));
    }

    @Test
    @DisplayName("Should update an existing order when orderId already exists")
    void saveOrder_update() {
        when(orderRepository.findByOrderId("100000000001")).thenReturn(Optional.of(sampleEntity));
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(sampleEntity);
        when(orderMapper.toModel(sampleEntity)).thenReturn(sampleOrder);

        Order result = orderTableService.saveOrder(sampleOrder);

        assertNotNull(result);
        assertEquals("100000000001", result.getOrderId());
        verify(orderMapper).updateEntity(sampleEntity, sampleOrder);
        verify(orderRepository).save(sampleEntity);
    }

    @Test
    @DisplayName("Should return saved order with correct fields")
    void saveOrder_returnsCorrectFields() {
        when(orderRepository.findByOrderId("100000000001")).thenReturn(Optional.empty());
        when(orderMapper.toEntity(sampleOrder)).thenReturn(sampleEntity);
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(sampleEntity);
        when(orderMapper.toModel(sampleEntity)).thenReturn(sampleOrder);

        Order result = orderTableService.saveOrder(sampleOrder);

        assertEquals("ABC", result.getClientName());
        assertEquals("TCS", result.getStockName());
        assertEquals(new BigDecimal("100.25"), result.getOrderQty());
        assertEquals(new BigDecimal("3500.0"), result.getPrice());
        assertEquals(OrderType.BUY, result.getOrderType());
    }
}
