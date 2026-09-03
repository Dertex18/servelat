package com.example.orders.repository;

import com.example.orders.exception.OrderNotFoundException;
import com.example.orders.model.Order;
import com.example.orders.model.Product;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;


public class InMemoryOrderRepository implements OrderRepository {

    private final Map<Long, Order> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);
    private final AtomicLong productIdGenerator = new AtomicLong(0);

    @Override
    public Order create(Order order) {
        long id = idGenerator.incrementAndGet();
        order.setId(id);
        assignProductIdsIfMissing(order);
        order.recalculateCost();
        storage.put(id, order);
        return order;
    }

    @Override
    public Optional<Order> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Order update(Long id, Order order) {
        if (!storage.containsKey(id)) {
            throw new OrderNotFoundException(id);
        }
        order.setId(id);
        assignProductIdsIfMissing(order);
        order.recalculateCost();
        storage.put(id, order);
        return order;
    }

    @Override
    public boolean delete(Long id) {
        return storage.remove(id) != null;
    }

    private void assignProductIdsIfMissing(Order order) {
        if (order.getProducts() == null) {
            return;
        }
        for (Product product : order.getProducts()) {
            if (product.getId() == null) {
                product.setId(productIdGenerator.incrementAndGet());
            }
        }
    }
}
