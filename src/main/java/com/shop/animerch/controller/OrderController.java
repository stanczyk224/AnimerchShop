package com.shop.animerch.controller;

import com.shop.animerch.record.OrderDTO;
import com.shop.animerch.record.CheckoutRequest;
import com.shop.animerch.record.OrderItemDTO;
import com.shop.animerch.record.OrderItemStatusUpdateDTO;
import com.shop.animerch.service.OrderService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;

@RestController
@RequestMapping("/api/order")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDTO checkout(@RequestHeader("Idempotency-Key") String idempotencyKey,
                             @Valid @RequestBody CheckoutRequest request) {
        return orderService.checkout(idempotencyKey, request);
    }
    @GetMapping("/my")
    public Page<OrderDTO> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return orderService.getMyOrders(page, size);
    }
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<OrderDTO> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return orderService.getAllOrders(page, size);
    }
    @GetMapping("/consumer/{consumerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<OrderDTO> getOrdersByConsumerId(
            @PathVariable Long consumerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return orderService.getOrdersByConsumerId(consumerId, page, size);
    }
    @GetMapping("/{orderId}")
    public OrderDTO getOrderById(@PathVariable Long orderId) {
        return orderService.getOrderById(orderId);
    }

    @PatchMapping("/{orderId}/items/{itemId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public OrderItemDTO patchOrderItem(
            @PathVariable Long itemId,
            @PathVariable Long orderId,
            @Valid @RequestBody OrderItemStatusUpdateDTO nextStatus
            ) {
        return orderService.updateStatusOfOrderItem(itemId,orderId,nextStatus);
    }

    @PatchMapping("/{orderId}/items/{itemId}/cancel")
    public OrderItemDTO cancelMyOrderItem(@PathVariable Long orderId, @PathVariable Long itemId) {
        return orderService.cancelMyOrderItem(itemId, orderId);
    }
}
