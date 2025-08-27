package com.ytbooster.model.mapper;

import com.ytbooster.model.Orders;
import com.ytbooster.model.dto.OrdersDTO;
import java.time.LocalDateTime;

/**
 * Mapper for Orders <-> OrdersDTO
 * - BigDecimal-safe
 * - Null-safe conversions
 */
public class OrdersMapper {

    /** Entity → DTO */
    public static OrdersDTO toDTO(Orders orders) {
        if (orders == null) return null;

        return OrdersDTO.builder()
                .orderId(orders.getOrderId())
                .userId(orders.getUserId())
                .link(orders.getLink())
                .orderDescription(orders.getOrderDescription())
                .status(orders.getStatus())
                .amount(orders.getAmount()) // BigDecimal-safe
                .createdAt(orders.getCreatedAt()) // ✅ map createdAt
                .build();
    }

    /** DTO → Entity */
    public static Orders toEntity(OrdersDTO dto) {
        if (dto == null) return null;

        Orders orders = new Orders();
        orders.setOrderId(dto.getOrderId());
        orders.setUserId(dto.getUserId());
        orders.setLink(dto.getLink());
        orders.setOrderDescription(dto.getOrderDescription());
        orders.setStatus(dto.getStatus());
        orders.setAmount(dto.getAmount()); // BigDecimal-safe
        orders.setCreatedAt(dto.getCreatedAt()); // ✅ set createdAt if needed
        return orders;
    }
}
