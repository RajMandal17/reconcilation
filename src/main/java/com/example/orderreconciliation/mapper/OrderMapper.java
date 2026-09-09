package com.example.orderreconciliation.mapper;

import com.example.orderreconciliation.entity.OrderEntity;
import com.example.orderreconciliation.model.Order;
import org.springframework.stereotype.Component;

/**
 * Converts between Order (DTO/model) and OrderEntity (JPA entity).
 */
@Component
public class OrderMapper {

    /**
     * Converts an OrderEntity to an Order model.
     */
    public Order toModel(OrderEntity entity) {
        if (entity == null) {
            return null;
        }

        Order order = new Order();
        order.setOrderId(entity.getOrderId());
        order.setClientName(entity.getClientName());
        order.setStockName(entity.getStockName());
        order.setOrderQty(entity.getOrderQty());
        order.setPrice(entity.getPrice());
        order.setOrderTimestamp(entity.getOrderTimestamp());
        order.setOrderType(entity.getOrderType());
        return order;
    }

    /**
     * Converts an Order model to a new OrderEntity.
     */
    public OrderEntity toEntity(Order order) {
        if (order == null) {
            return null;
        }

        OrderEntity entity = new OrderEntity();
        entity.setOrderId(order.getOrderId());
        entity.setClientName(order.getClientName());
        entity.setStockName(order.getStockName());
        entity.setOrderQty(order.getOrderQty());
        entity.setPrice(order.getPrice());
        entity.setOrderTimestamp(order.getOrderTimestamp());
        entity.setOrderType(order.getOrderType());
        return entity;
    }

    /**
     * Updates an existing OrderEntity with values from an Order model.
     * Preserves the entity's database ID.
     */
    public void updateEntity(OrderEntity entity, Order order) {
        if (entity == null || order == null) {
            return;
        }

        entity.setOrderId(order.getOrderId());
        entity.setClientName(order.getClientName());
        entity.setStockName(order.getStockName());
        entity.setOrderQty(order.getOrderQty());
        entity.setPrice(order.getPrice());
        entity.setOrderTimestamp(order.getOrderTimestamp());
        entity.setOrderType(order.getOrderType());
    }
}
