
package com.shop.animerch.service;

import com.shop.animerch.exception.ResourceNotFoundException;
import com.shop.animerch.mapper.ConsumerMapper;
import com.shop.animerch.model.Cart;
import com.shop.animerch.model.Consumer;
import com.shop.animerch.record.ConsumerCreateDTO;
import com.shop.animerch.record.ConsumerDTO;
import com.shop.animerch.repository.CartRepository;
import com.shop.animerch.repository.ConsumerRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsumerService {

    private final ConsumerRepository consumerRepository;
    private final ConsumerMapper consumerMapper;
    private final CartRepository cartRepository;

    public List<ConsumerDTO> getConsumer() {
        log.debug("Fetching all consumers");

        List<Consumer> consumers = consumerRepository.findAll();

        log.info("Fetched {} consumers", consumers.size());

        return consumerMapper.toDTOList(consumers);
    }

    public ConsumerDTO getConsumerById(Long id) {
        log.debug("Fetching consumer with id={}", id);

        Consumer consumer = consumerRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Consumer with id={} not found", id);
                    return new ResourceNotFoundException(
                            "Consumer with id " + id + " not found"
                    );
                });

        log.debug("Consumer with id={} found", id);

        return consumerMapper.toDTO(consumer);
    }
    @Transactional
    public ConsumerDTO addConsumer(ConsumerCreateDTO dto) {
        log.debug("Creating consumer");

        Consumer consumer = new Consumer();
        consumer.setName(dto.name());
        consumer.setEmail(dto.email());
        consumer.setPhone(dto.phone());

        Consumer savedConsumer = consumerRepository.save(consumer);

        log.info("Consumer created successfully, id={}",
                savedConsumer.getId());
        log.debug("Creating cart for consumer id={}", savedConsumer.getId());
        Cart cart = new Cart();
        cart.setConsumer(savedConsumer);

        cartRepository.save(cart);
        log.debug("Cart with id={} created successfully", cart.getId());
        return consumerMapper.toDTO(savedConsumer);
    }

    public void deleteConsumer(Long id) {
        log.debug("Deleting consumer with id={}", id);

        if (!consumerRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "Consumer with id " + id + " not found"
            );
        }

        consumerRepository.deleteById(id);

        log.info("Consumer deleted successfully, id={}", id);
    }
}
