package com.shop.animerch.service;

import com.shop.animerch.exception.ResourceNotFoundException;
import com.shop.animerch.mapper.CartMapper;
import com.shop.animerch.model.Cart;
import com.shop.animerch.model.Consumer;
import com.shop.animerch.record.CartDTO;
import com.shop.animerch.repository.CartRepository;
import com.shop.animerch.repository.ConsumerRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final CartMapper cartMapper;
    private final ConsumerRepository consumerRepository;

    @Transactional(readOnly = true)
    public CartDTO getCart() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null ||
                !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User is not authenticated");
        }
        String email = authentication.getName();
        Consumer consumer = consumerRepository.findByUserEmail(email)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Consumer with email=%s not found".formatted(email)
                        ));
        Long consumerId = consumer.getId();
        log.debug("Fetching cart for consumer id={}", consumerId);
        Cart cart = cartRepository.findByConsumerId(consumerId)
                .orElseThrow(()-> new ResourceNotFoundException(
                        "Cart for consumer with id %d not found".formatted(consumerId)
                ));
        return cartMapper.toDTO(cart);

    }
}
