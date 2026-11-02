package com.shop.animerch.controller;

import com.shop.animerch.record.OrderDTO;
import com.shop.animerch.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDTO checkout() {
        return orderService.checkout();
    }
    @GetMapping("/my")
    public List<OrderDTO> getMyOrders() {
        return orderService.getMyOrders();
    }
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<OrderDTO> getAllOrders() {
        return orderService.getAllOrders();
    }
    @GetMapping("/consumer/{consumerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<OrderDTO> getOrdersByConsumerId(
            @PathVariable Long consumerId
    ) {
        return orderService.getOrdersByConsumerId(consumerId);
    }
}