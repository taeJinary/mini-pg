package com.example.minipg.domain.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.minipg.domain.Order;

public interface OrderRepository extends JpaRepository<Order, String> {
}
