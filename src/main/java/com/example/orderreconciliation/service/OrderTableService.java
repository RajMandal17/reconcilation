package com.example.orderreconciliation.service;

import com.example.orderreconciliation.entity.OrderEntity;
import com.example.orderreconciliation.mapper.OrderMapper;
import com.example.orderreconciliation.model.Order;
import com.example.orderreconciliation.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service handling database order operations (insert/update via POST /api/v1/orders/table).
 */
@Service
public class OrderTableService {

    private static final Logger log = LoggerFactory.getLogger(OrderTableService.class);

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    public OrderTableService(OrderRepository orderRepository, OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
    }

    /**
     * Inserts or updates an order in the database.
     * If the orderId already exists, updates the existing record.
     * Otherwise, inserts a new record.
     */
    @Transactional
    public Order saveOrder(Order order) {
        Optional<OrderEntity> existingEntity = orderRepository.findByOrderId(order.getOrderId());

        OrderEntity entity;
        if (existingEntity.isPresent()) {
            entity = existingEntity.get();
            orderMapper.updateEntity(entity, order);
            log.info("Updating existing order with orderId: {}", order.getOrderId());
        } else {
            entity = orderMapper.toEntity(order);
            log.info("Inserting new order with orderId: {}", order.getOrderId());
        }

        OrderEntity savedEntity = orderRepository.save(entity);
        return orderMapper.toModel(savedEntity);
    }

    /**
     * Retrieves all orders from the database as Order models.
     */
    public List<Order> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(orderMapper::toModel)
                .collect(Collectors.toList());
    }
}
