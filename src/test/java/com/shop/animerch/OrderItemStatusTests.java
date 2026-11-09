package com.shop.animerch;

import com.shop.animerch.controller.OrderController;
import com.shop.animerch.mapper.OrderMapper;
import com.shop.animerch.model.OrderItem;
import com.shop.animerch.model.OrderStatus;
import com.shop.animerch.model.Product;
import com.shop.animerch.model.Consumer;
import com.shop.animerch.model.Order;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(OrderItemStatusTests.Config.class)
class OrderItemStatusTests {
    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean OrderItemRepository items() { return mock(OrderItemRepository.class); }
        @Bean ProductRepository products() { return mock(ProductRepository.class); }
        @Bean EntityManager entityManager() { return mock(EntityManager.class); }
        @Bean ConsumerRepository consumers() { return mock(ConsumerRepository.class); }
        @Bean OrderRepository orders() { return mock(OrderRepository.class); }
        @Bean OrderService service(OrderItemRepository items, ProductRepository products, EntityManager entityManager,
                                   ConsumerRepository consumers, OrderRepository orders) {
            return new OrderService(orders, consumers,
                    products, Mappers.getMapper(OrderMapper.class),
                    mock(CartRepository.class), mock(CartItemRepository.class), entityManager, items);
        }
        @Bean OrderController controller(OrderService service) { return new OrderController(service); }
    }

    @Autowired OrderController controller;
    @Autowired OrderItemRepository items;
    @Autowired ProductRepository products;
    @Autowired EntityManager entityManager;
    @Autowired ConsumerRepository consumers;
    @Autowired OrderRepository orders;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        reset(items, products, entityManager, consumers, orders);
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

    private OrderItem customerItem(long ownerId) {
        authenticate("CUSTOMER");
        Consumer consumer = new Consumer();
        consumer.setId(7L);
        when(consumers.findByUserEmail("test@example.com")).thenReturn(Optional.of(consumer));
        Consumer owner = new Consumer();
        owner.setId(ownerId);
        Order order = new Order();
        order.setConsumer(owner);
        OrderItem item = item(OrderStatus.PLACED);
        item.setOrder(order);
        return item;
    }

    @Test
    void checkoutRequiresValidIdempotencyKeyBeforeAccessingDatabase() throws Exception {
        mvc.perform(post("/api/order/checkout")).andExpect(status().isBadRequest());
        for (String key : List.of("", " ", "contains spaces", "a".repeat(129))) {
            mvc.perform(post("/api/order/checkout").header("Idempotency-Key", key)
                            .contentType("application/json").content(validCheckoutBody()))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(orders, consumers, items, products);
    }

    private String validCheckoutBody() {
        return """
                {"deliveryAddress":{"recipientName":"Recipient","phone":"+998901234567",
                "addressLine1":"12 Main Street","city":"Tashkent","postalCode":"100000","country":"Uzbekistan"}}
                """;
    }

    @Test
    void checkoutValidatesNestedDeliveryAddressBeforeAccessingDatabase() throws Exception {
        for (String body : List.of("{}", "{\"deliveryAddress\":null}", "{\"deliveryAddress\":{}}",
                validCheckoutBody().replace("Recipient", " "),
                validCheckoutBody().replace("Tashkent", "x".repeat(101)))) {
            mvc.perform(post("/api/order/checkout").header("Idempotency-Key", "checkout-test")
                            .contentType("application/json").content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(orders, consumers, items, products);
    }

    @Test
    void orderDetailsReturnOwnerDataAndRejectOtherCustomers() throws Exception {
        OrderItem item = customerItem(7L);
        item.getOrder().setId(1L);
        when(orders.findById(1L)).thenReturn(Optional.of(item.getOrder()));
        mvc.perform(get("/api/order/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        item.getOrder().getConsumer().setId(8L);
        mvc.perform(get("/api/order/1")).andExpect(status().isForbidden());
        authenticate("ADMIN");
        mvc.perform(get("/api/order/1")).andExpect(status().isOk());
        mvc.perform(get("/api/order/999")).andExpect(status().isNotFound());
    }

    @Test
    void orderListRoutesValidatePagingAndKeepAdminRestrictions() throws Exception {
        authenticate("CUSTOMER");
        mvc.perform(get("/api/order")).andExpect(status().isForbidden());
        mvc.perform(get("/api/order/consumer/7")).andExpect(status().isForbidden());
        mvc.perform(get("/api/order/my?page=-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/order/my?size=0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/order/my?size=101")).andExpect(status().isBadRequest());
        authenticate("ADMIN");
        when(orders.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenAnswer(invocation -> new org.springframework.data.domain.PageImpl<>(
                        List.of(), invocation.getArgument(0), 0));
        mvc.perform(get("/api/order?page=1&size=5")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
        verify(orders).findAll(org.springframework.data.domain.PageRequest.of(1, 5,
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Order.desc("createdAt"),
                        org.springframework.data.domain.Sort.Order.desc("id"))));
    }

    @Test
    void customerCanCancelOwnItem() throws Exception {
        OrderItem item = customerItem(7L);
        Product product = attachProduct(item, 10);
        when(products.findByIdForUpdate(10L)).thenReturn(Optional.of(product));
        mvc.perform(patch("/api/order/1/items/2/cancel"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        assertEquals(13, product.getQuantity());
    }

    @Test
    void customerCannotCancelAnotherOwnersItem() throws Exception {
        OrderItem item = customerItem(8L);
        mvc.perform(patch("/api/order/1/items/2/cancel"))
                .andExpect(status().isForbidden());
        assertEquals(OrderStatus.PLACED, item.getStatus());
        verifyNoInteractions(products, entityManager);
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
