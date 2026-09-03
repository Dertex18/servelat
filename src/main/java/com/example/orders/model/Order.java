package com.example.orders.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class Order {

    private Long id;
    private LocalDate date;
    private BigDecimal cost;
    private List<Product> products = new ArrayList<>();

    public Order() {

    }

    @JsonCreator
    public Order(@JsonProperty("id") Long id,
                 @JsonProperty("date") LocalDate date,
                 @JsonProperty("cost") BigDecimal cost,
                 @JsonProperty("products") List<Product> products) {
        this.id = id;
        this.date = date;
        this.cost = cost;
        this.products = products != null ? products : new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products != null ? products : new ArrayList<>();
    }


    public void recalculateCost() {
        this.cost = products.stream()
                .map(Product::getCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order)) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id)
                && Objects.equals(date, order.date)
                && Objects.equals(cost, order.cost)
                && Objects.equals(products, order.products);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, date, cost, products);
    }

    @Override
    public String toString() {
        return "Order{id=" + id + ", date=" + date + ", cost=" + cost + ", products=" + products + '}';
    }
}
