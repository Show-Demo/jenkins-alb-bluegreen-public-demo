package com.example.order.controller;

import com.example.order.model.Order;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @GetMapping
    public List<Order> list() {
        return Arrays.asList(
            new Order(1001L, "PAID", new BigDecimal("199.00")),
            new Order(1002L, "CREATED", new BigDecimal("59.90"))
        );
    }

    @GetMapping("/{id}")
    public Order detail(@PathVariable Long id) {
        return new Order(id, "PAID", new BigDecimal("199.00"));
    }
}
