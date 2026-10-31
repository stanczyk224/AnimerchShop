package com.shop.animerch.controller;

import com.shop.animerch.record.OrderCreateDTO;
import com.shop.animerch.record.OrderDTO;
import com.shop.animerch.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(path = "/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDTO createOrder(@Valid @RequestBody OrderCreateDTO dto) {
        return orderService.createOrder(dto);
    }
    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDTO checkout(@RequestParam Long consumerId) {
        return orderService.checkout(consumerId);
    }
}
