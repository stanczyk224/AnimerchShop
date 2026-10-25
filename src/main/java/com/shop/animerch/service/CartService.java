package com.shop.animerch.service;

import com.shop.animerch.exception.ResourceNotFoundException;
import com.shop.animerch.mapper.CartMapper;
import com.shop.animerch.model.Cart;
import com.shop.animerch.record.CartDTO;
import com.shop.animerch.repository.CartRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final CartMapper cartMapper;

    @Transactional(readOnly = true)
    public CartDTO getCart(Long consumerId) {
        log.debug("Fetching cart for consumer id={}", consumerId);
        Cart cart = cartRepository.findByConsumerId(consumerId)
                .orElseThrow(()-> new ResourceNotFoundException(
                        "Cart for consumer with id %d not found".formatted(consumerId)
                ));
        return cartMapper.toDTO(cart);

    }
}
