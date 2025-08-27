package com.ytbooster.serviceImple;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ytbooster.model.Orders;
import com.ytbooster.model.dto.OrdersDTO;
import com.ytbooster.model.mapper.OrdersMapper;
import com.ytbooster.repository.OrdersRepository;
import com.ytbooster.service.OrdersService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrdersServiceImpl implements OrdersService {

    private final OrdersRepository ordersRepository;

    /**
     * Create a new order
     * - Sets status to PENDING by default
     */
    @Override
    @Transactional
    public String createOrder(OrdersDTO order) {
        order.setStatus("PENDING");
        Orders saved = ordersRepository.save(OrdersMapper.toEntity(order));
        return saved.getOrderId().toString();
    }

    @Override
    public OrdersDTO getById(Long orderId) {
        Orders order = ordersRepository.findByOrderId(orderId);
        return OrdersMapper.toDTO(order);
    }

    @Override
    public List<OrdersDTO> getByUserId(Long userId) {
        return ordersRepository.findByUserId(userId)
                .stream()
                .map(OrdersMapper::toDTO)
                .toList();
    }

    @Override
    public List<OrdersDTO> getOrdersByStatus(String status) {
        return ordersRepository.findByStatus(status)
                .stream()
                .map(OrdersMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public void update(OrdersDTO order) {
        ordersRepository.save(OrdersMapper.toEntity(order));
    }

    @Override
    @Transactional
    public void delete(Long orderId) {
        ordersRepository.deleteById(orderId);
    }

    @Override
    public List<OrdersDTO> getAllOrders() {
        return ordersRepository.findAll()
                .stream()
                .map(OrdersMapper::toDTO)
                .toList();
    }
}
