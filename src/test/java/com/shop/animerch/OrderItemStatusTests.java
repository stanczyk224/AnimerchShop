package com.shop.animerch;

import com.shop.animerch.controller.OrderController;
import com.shop.animerch.mapper.OrderMapper;
import com.shop.animerch.model.OrderItem;
import com.shop.animerch.model.OrderStatus;
import com.shop.animerch.model.Product;
import com.shop.animerch.repository.*;
import com.shop.animerch.service.OrderService;
import com.shop.animerch.utils.GlobalExceptionHandler;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(OrderItemStatusTests.Config.class)
class OrderItemStatusTests {
    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean OrderItemRepository items() { return mock(OrderItemRepository.class); }
        @Bean ProductRepository products() { return mock(ProductRepository.class); }
        @Bean EntityManager entityManager() { return mock(EntityManager.class); }
        @Bean OrderService service(OrderItemRepository items, ProductRepository products, EntityManager entityManager) {
            return new OrderService(mock(OrderRepository.class), mock(ConsumerRepository.class),
                    products, Mappers.getMapper(OrderMapper.class),
                    mock(CartRepository.class), mock(CartItemRepository.class), entityManager, items);
        }
        @Bean OrderController controller(OrderService service) { return new OrderController(service); }
    }

    @Autowired OrderController controller;
    @Autowired OrderItemRepository items;
    @Autowired ProductRepository products;
    @Autowired EntityManager entityManager;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        reset(items, products, entityManager);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        authenticate("ADMIN");
    }

    @AfterEach
    void cleanUp() { SecurityContextHolder.clearContext(); }

    private void authenticate(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "test@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private OrderItem item(OrderStatus status) {
        OrderItem item = new OrderItem();
        item.setId(2L);
        item.setStatus(status);
        when(items.findByIdAndOrderIdForUpdate(2L, 1L)).thenReturn(Optional.of(item));
        return item;
    }

    @Test
    void placedToShippedSucceeds() throws Exception {
        OrderItem item = item(OrderStatus.PLACED);
        mvc.perform(patch("/api/order/1/items/2/status").contentType("application/json")
                        .content("{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SHIPPED"));
        assertEquals(OrderStatus.SHIPPED, item.getStatus());
    }

    @Test
    void shippedToDeliveredSucceeds() throws Exception {
        OrderItem item = item(OrderStatus.SHIPPED);
        mvc.perform(patch("/api/order/1/items/2/status").contentType("application/json")
                        .content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DELIVERED"));
        assertEquals(OrderStatus.DELIVERED, item.getStatus());
    }

    @Test
    void deliveredToShippedReturns400() throws Exception {
        OrderItem item = item(OrderStatus.DELIVERED);
        mvc.perform(patch("/api/order/1/items/2/status").contentType("application/json")
                        .content("{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isBadRequest());
        assertEquals(OrderStatus.DELIVERED, item.getStatus());
    }

    @Test
    void mismatchedOrderReturns404() throws Exception {
        when(items.findByIdAndOrderIdForUpdate(2L, 99L)).thenReturn(Optional.empty());
        mvc.perform(patch("/api/order/99/items/2/status").contentType("application/json")
                        .content("{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isNotFound());
        verify(items).findByIdAndOrderIdForUpdate(2L, 99L);
    }

    @Test
    void customerReturns403BeforeLoadingItem() throws Exception {
        authenticate("CUSTOMER");
        mvc.perform(patch("/api/order/1/items/2/status").contentType("application/json")
                        .content("{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(items);
    }

    private Product attachProduct(OrderItem item, int stock) {
        Product product = new Product();
        product.setId(10L);
        product.setQuantity(stock);
        item.setProduct(product);
        item.setQuantity(3);
        return product;
    }

    @Test
    void cancellationRestoresThreeUnitsUsingRefreshedStock() throws Exception {
        OrderItem item = item(OrderStatus.PLACED);
        Product product = attachProduct(item, 10);
        when(products.findByIdForUpdate(10L)).thenReturn(Optional.of(product));
        // Another checkout reduced stock before the product lock was acquired.
        doAnswer(invocation -> {
            product.setQuantity(8);
            return null;
        }).when(entityManager).refresh(product);

        mvc.perform(patch("/api/order/1/items/2/status").contentType("application/json")
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));

        assertEquals(11, product.getQuantity());
        assertEquals(OrderStatus.CANCELLED, item.getStatus());
        var sequence = inOrder(products, entityManager);
        sequence.verify(products).findByIdForUpdate(10L);
        sequence.verify(entityManager).refresh(product);
    }

    @Test
    void repeatedCancellationDoesNotRestoreStockTwice() throws Exception {
        OrderItem item = item(OrderStatus.PLACED);
        Product product = attachProduct(item, 10);
        when(products.findByIdForUpdate(10L)).thenReturn(Optional.of(product));

        mvc.perform(patch("/api/order/1/items/2/status").contentType("application/json")
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk());
        assertEquals(13, product.getQuantity());

        mvc.perform(patch("/api/order/1/items/2/status").contentType("application/json")
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isBadRequest());
        assertEquals(13, product.getQuantity());
        verify(products, times(1)).findByIdForUpdate(10L);
        verify(entityManager, times(1)).refresh(product);
    }

    @Test
    void shippedItemCannotBeCancelledAndStockIsUnchanged() throws Exception {
        OrderItem item = item(OrderStatus.SHIPPED);
        Product product = attachProduct(item, 10);

        mvc.perform(patch("/api/order/1/items/2/status").contentType("application/json")
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isBadRequest());

        assertEquals(10, product.getQuantity());
        assertEquals(OrderStatus.SHIPPED, item.getStatus());
        verifyNoInteractions(products, entityManager);
    }
}
