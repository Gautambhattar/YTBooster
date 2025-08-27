package com.ytbooster.service;

import java.util.List;

import com.ytbooster.model.Orders;
import com.ytbooster.model.dto.OrdersDTO;

public interface OrdersService {
    String createOrder(OrdersDTO order);
    OrdersDTO getById(Long orderId);
    List<OrdersDTO> getByUserId(Long userId);
    void update(OrdersDTO order);
    void delete(Long orderId);
	List<OrdersDTO> getOrdersByStatus(String status);
	List<OrdersDTO> getAllOrders();
}
