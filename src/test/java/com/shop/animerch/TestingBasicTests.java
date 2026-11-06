package com.shop.animerch;

import com.shop.animerch.exception.BadRequestException;
import com.shop.animerch.exception.ResourceNotFoundException;
import com.shop.animerch.mapper.CartItemMapper;
import com.shop.animerch.model.Cart;
import com.shop.animerch.model.Consumer;
import com.shop.animerch.repository.CartItemRepository;
import com.shop.animerch.repository.CartRepository;
import com.shop.animerch.repository.ConsumerRepository;
import com.shop.animerch.repository.ProductRepository;
import com.shop.animerch.service.CartItemService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class TestingBasicTests {
    private CartItemRepository cartItemRepository;
    private CartItemMapper cartItemMapper;
    private CartRepository cartRepository;
    private ProductRepository productRepository;
    private ConsumerRepository consumerRepository;
    private CartItemService service;

    @BeforeEach
    void setUp() {
        cartItemRepository = mock(CartItemRepository.class);
        cartItemMapper = mock(CartItemMapper.class);
        cartRepository = mock(CartRepository.class);
        productRepository = mock(ProductRepository.class);
        consumerRepository = mock(ConsumerRepository.class);

        service = new CartItemService(
                cartItemRepository,
                cartItemMapper,
                cartRepository,
                productRepository,
                consumerRepository);
        SecurityContextHolder.clearContext();
    }
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addingZeroItemsToCartThrowsBadRequest() {
        assertThrows(
                BadRequestException.class,
                () -> service.addProductToCart(1L, 0)
        );
        verifyNoInteractions(
                cartItemRepository,
                cartRepository,
                productRepository,
                consumerRepository
        );

    }
    @Test
    void addingNegativeItemsToCartThrowsBadRequest() {
        assertThrows(
                BadRequestException.class,
                () -> service.addProductToCart(1L, -1)
        );
    }
    @Test
    void addingItemToCartWithoutAuthenticationThrowsAccessDenied() {
        assertThrows(
                AccessDeniedException.class,
                () -> service.addProductToCart(1L, 2)
        );

    }
    @Test
    void addingItemWhenConsumerIsMissingThrowsResourceNotFound() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "customer@example.com", null, List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(consumerRepository.findByUserEmail("customer@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.addProductToCart(1L, 1)
        );
        verify(consumerRepository).findByUserEmail("customer@example.com");
    }
    @Test
    void addingItemWhenCartIsMissingThrowsResourceNotFound() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "customer@example.com", null, List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        var consumer = new Consumer();
        consumer.setId(10L);
        when(consumerRepository.findByUserEmail("customer@example.com"))
                .thenReturn(Optional.of(consumer));

        when(cartRepository.findByConsumerIdForUpdate(10L))
                .thenReturn(Optional.empty());
        assertThrows(
                ResourceNotFoundException.class,
                () -> service.addProductToCart(1L,1)
        );
        verify(cartRepository).findByConsumerIdForUpdate(10L);
    }
    @Test
    void addingMissingProductToCartThrowsResourceNotFound() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "customer@example.com", null, List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        var consumer = new Consumer();
        consumer.setId(10L);
        when(consumerRepository.findByUserEmail("customer@example.com"))
                .thenReturn(Optional.of(consumer));
        var cart = new Cart();
        cart.setId(10L);
        cart.setConsumer(consumer);
        when(cartRepository.findByConsumerIdForUpdate(10L))
                .thenReturn(Optional.of(cart));

        when(productRepository.findById(1L))
                .thenReturn(Optional.empty());
        assertThrows(
                ResourceNotFoundException.class,
                () -> service.addProductToCart(1L, 1)
        );
        verify(productRepository).findById(1L);
        verifyNoInteractions(cartItemRepository);

    }

}


