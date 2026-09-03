package com.example.orders.repository;

import com.example.orders.model.Order;

import java.util.Optional;


public interface OrderRepository {


    Order create(Order order);


    Optional<Order> findById(Long id);


    Order update(Long id, Order order);

    boolean delete(Long id);
}
