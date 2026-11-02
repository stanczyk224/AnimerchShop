package com.shop.animerch.controller;

import com.shop.animerch.mapper.ConsumerMapper;
import com.shop.animerch.record.ConsumerCreateDTO;
import com.shop.animerch.record.ConsumerDTO;
import com.shop.animerch.service.ConsumerService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path =  "/api/consumers")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class ConsumerController {
    private final ConsumerService consumerService;
    private final ConsumerMapper consumerMapper;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ConsumerDTO> getAllConsumers() {
        return this.consumerService.getConsumer();
    }

    @GetMapping("{consumerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ConsumerDTO getConsumerById(
            @PathVariable("consumerId") Long id
    ) {
        return consumerService.getConsumerById(id);
    }

    @DeleteMapping(path = "delete/{consumerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteConsumer(@PathVariable("consumerId") Long id) {
        consumerService.deleteConsumer(id);
    }
}
