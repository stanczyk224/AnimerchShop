package com.shop.animerch.service;

import com.shop.animerch.exception.ResourceNotFoundException;
import com.shop.animerch.mapper.CartItemMapper;
import com.shop.animerch.model.Cart;
import com.shop.animerch.model.CartItem;
import com.shop.animerch.model.Product;
import com.shop.animerch.repository.CartItemRepository;
import com.shop.animerch.repository.CartRepository;
import com.shop.animerch.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartItemServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CartItemMapper cartItemMapper;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartItemService cartItemService;


    @Test
    void addProductToCart_whenCartDoesNotExist_throwsException() {
        // Arrange — подготовка
        Long cartId = 1L;
        Long productId = 10L;

        // Act + Assert — вызов метода и проверка
        assertThrows(
                ResourceNotFoundException.class,
                () -> cartItemService.addProductToCart(cartId, productId)
        );
        verify(productRepository, never()).findById(productId);
        verify(cartItemRepository, never()).save(
                org.mockito.ArgumentMatchers.any()
        );
    }
    @Test
    void addProductToCart_whenStockAvailable_savesCartItem() {
        // Arrange — подготовка данных
        Long cartId = 1L;
        Long productId = 10L;

        Cart cart = new Cart();
        cart.setId(cartId);

        Product product = new Product();
        product.setId(productId);
        product.setQuantity(5);

        when(cartRepository.findById(cartId)).thenReturn(
                Optional.of(cart)
        );
        when(productRepository.findById(productId)).thenReturn(
                Optional.of(product)
        );
        when(cartItemRepository.findByCartAndProduct(cart,product)).thenReturn(
                Optional.empty()
        );
        when(cartItemRepository.save(any(CartItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        cartItemService.addProductToCart(cartId, productId);
        ArgumentCaptor<CartItem> captor =
                ArgumentCaptor.forClass(CartItem.class);

        verify(cartItemRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getQuantity());
        assertSame(cart, captor.getValue().getCart());
        assertSame(product, captor.getValue().getProduct());
    }

}
