package com.shop.animerch.controller;

import com.shop.animerch.mapper.ConsumerMapper;
import com.shop.animerch.record.ConsumerCreateDTO;
import com.shop.animerch.record.ConsumerDTO;
import com.shop.animerch.service.ConsumerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

    @RestController
    @RequestMapping(path =  "api/consumers")
    @RequiredArgsConstructor
public class ConsumerController {
    private final ConsumerService consumerService;
    private final ConsumerMapper consumerMapper;

    @GetMapping
    public List<ConsumerDTO> getAllConsumers() {
        return this.consumerService.getConsumer();
    }

    @GetMapping("{consumerId}")
    public ConsumerDTO getConsumerById(
            @PathVariable("consumerId") Long id
    ) {
        return consumerService.getConsumerById(id);
    }

    @PostMapping(path = "addConsumer")
    public ConsumerDTO addConsumer(
            @Valid @RequestBody ConsumerCreateDTO dto
            ){
        return consumerService.addConsumer(dto);
    }
    @DeleteMapping(path = "delete/{consumerId}")
    public void deleteConsumer(@PathVariable("consumerId") Long id) {
        consumerService.deleteConsumer(id);
    }
}
