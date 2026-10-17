package com.shop.animerch.service;

import com.shop.animerch.mapper.ConsumerMapper;
import com.shop.animerch.model.Consumer;
import com.shop.animerch.record.ConsumerCreateDTO;
import com.shop.animerch.record.ConsumerDTO;
import com.shop.animerch.repository.ConsumerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsumerService {
    private final ConsumerRepository consumerRepository;
    private final ConsumerMapper consumerMapper;

    public List<Consumer> getConsumer() {
      return this.consumerRepository.findAll();
    }

    public ConsumerDTO getConsumerById(Long id) {
        Consumer consumer = consumerRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Consumer is not found!"));

        return consumerMapper.toDTO(consumer);
    }

    public ConsumerDTO addConsumer(ConsumerCreateDTO dto){
        Consumer consumer = new Consumer();
        consumer.setName(dto.name());
        consumer.setEmail(dto.email());
        consumer.setPhone(dto.phone());

        consumerRepository.save(consumer);
        return consumerMapper.toDTO(consumer);

    }

    public void deleteConsumer(Long id) {
        boolean find = consumerRepository.existsById(id);
        if(!find) {
            throw new IllegalStateException("consumer is not found!");

        }
        consumerRepository.deleteById(id);
    }

}
