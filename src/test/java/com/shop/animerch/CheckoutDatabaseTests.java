package com.shop.animerch;

import com.shop.animerch.exception.BadRequestException;
import com.shop.animerch.exception.ConflictException;
import com.shop.animerch.record.RegisterRequest;
import com.shop.animerch.service.CartItemService;
import com.shop.animerch.service.OrderService;
import com.shop.animerch.service.RegisterService;
import com.shop.animerch.service.SellerService;
import com.shop.animerch.service.SellerPhotoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import com.shop.animerch.exception.ResourceNotFoundException;
import com.shop.animerch.model.OrderStatus;
import com.shop.animerch.record.OrderItemStatusUpdateDTO;
import org.junit.jupiter.api.AfterEach;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Run against a disposable PostgreSQL database by setting DB_URL and credentials. */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class CheckoutDatabaseTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired RegisterService registration;
    @Autowired CartItemService carts;
    @Autowired OrderService orders;
    @Autowired SellerService sellers;
    @Autowired SellerPhotoService sellerPhotos;

    @AfterEach
    void clearAuthentication() { SecurityContextHolder.clearContext(); }

    private com.shop.animerch.record.CheckoutRequest deliveryRequest() {
        return new com.shop.animerch.record.CheckoutRequest(new com.shop.animerch.record.DeliveryAddressDTO(
                "Recipient", "+998901234567", "12 Main Street", null, "Tashkent", "100000", "Uzbekistan"));
    }

    private record Purchase(long product, String email, long order, long item) {}

    @Test
    void deliverySnapshotSurvivesProfileChangesAndChangedRetryReturnsConflict() {
        long product = product();
        String email = customerWithItem(product);
        authenticate(email);
        String key = UUID.randomUUID().toString();
        var request = deliveryRequest();
        var order = orders.checkout(key, request);
        assertEquals(request.deliveryAddress(), order.deliveryAddress());
        jdbc.update("update consumer set name = 'Changed name', phone = '999' where id = ?", order.consumerId());
        assertEquals(request.deliveryAddress(), orders.getOrderById(order.id()).deliveryAddress());
        assertEquals(request.deliveryAddress(), orders.getMyOrders(0, 20).getContent().getFirst().deliveryAddress());
        assertEquals(order.id(), orders.checkout(key, request).id());
        var changed = new com.shop.animerch.record.CheckoutRequest(new com.shop.animerch.record.DeliveryAddressDTO(
                "Recipient", "+998901234567", "99 Other Street", null, "Tashkent", "100000", "Uzbekistan"));
        assertThrows(ConflictException.class, () -> orders.checkout(key, changed));
        assertEquals(1, orders.getMyOrders(0, 20).getTotalElements());
        assertEquals(0, jdbc.queryForObject("select quantity from product where id = ?", Integer.class, product));
        assertEquals(request.deliveryAddress(), orders.getOrderById(order.id()).deliveryAddress());
    }

    @Test
    void competingRequestsWithDifferentAddressesHaveOneSuccessAndOneConflict() throws Exception {
        long product = product();
        String email = customerWithItem(product);
        String key = UUID.randomUUID().toString();
        var alternative = new com.shop.animerch.record.CheckoutRequest(new com.shop.animerch.record.DeliveryAddressDTO(
                "Another recipient", "12345", "Other Street", "Apartment 2", "Samarkand", "140100", "Uzbekistan"));
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = List.of(deliveryRequest(), alternative).stream().map(request -> executor.submit(() -> {
                assertTrue(start.await(10, TimeUnit.SECONDS));
                authenticate(email);
                try {
                    assertEquals(request.deliveryAddress(), orders.checkout(key, request).deliveryAddress());
                    return true;
                } catch (ConflictException expected) {
                    return false;
                } finally {
                    SecurityContextHolder.clearContext();
                }
            })).toList();
            start.countDown();
            int successes = 0;
            for (var result : results) if (result.get(30, TimeUnit.SECONDS)) successes++;
            assertEquals(1, successes);
        }
        authenticate(email);
        assertEquals(1, orders.getMyOrders(0, 20).getTotalElements());
        assertEquals(0, jdbc.queryForObject("select quantity from product where id = ?", Integer.class, product));
    }

    @Test
    void retryReturnsOriginalOrderAndLeavesNewCartItemsUntouched() {
        long product = product();
        String email = customerWithItem(product);
        authenticate(email);
        String key = UUID.randomUUID().toString();
        var original = orders.checkout(key, deliveryRequest());
        assertEquals(original, orders.checkout(key, deliveryRequest()));
        long nextProduct = product();
        carts.addProductToCart(nextProduct, 1);
        assertEquals(original, orders.checkout(key, deliveryRequest()));
        assertEquals(0, jdbc.queryForObject("select quantity from product where id = ?", Integer.class, product));
        assertEquals(1, jdbc.queryForObject("select quantity from product where id = ?", Integer.class, nextProduct));
        var next = orders.checkout(UUID.randomUUID().toString(), deliveryRequest());
        assertNotEquals(original.id(), next.id());
        assertEquals(2, orders.getMyOrders(0, 20).getTotalElements());
    }

    @Test
    void sameKeyIsIndependentForDifferentCustomers() {
        String key = UUID.randomUUID().toString();
        String first = customerWithItem(product());
        authenticate(first);
        long firstOrder = orders.checkout(key, deliveryRequest()).id();
        String second = customerWithItem(product());
        authenticate(second);
        long secondOrder = orders.checkout(key, deliveryRequest()).id();
        assertNotEquals(firstOrder, secondOrder);
        assertEquals(secondOrder, orders.checkout(key, deliveryRequest()).id());
        authenticate(first);
        assertEquals(firstOrder, orders.checkout(key, deliveryRequest()).id());
    }

    @Test
    void failedCheckoutDoesNotConsumeKeyOrPartiallyDeductStock() {
        long available = product();
        long unavailable = product();
        String email = customerWithItem(available);
        authenticate(email);
        carts.addProductToCart(unavailable, 1);
        jdbc.update("update product set quantity = 0 where id = ?", unavailable);
        String key = UUID.randomUUID().toString();
        assertThrows(BadRequestException.class, () -> orders.checkout(key, deliveryRequest()));
        assertEquals(0, orders.getMyOrders(0, 20).getTotalElements());
        assertEquals(1, jdbc.queryForObject("select quantity from product where id = ?", Integer.class, available));
        jdbc.update("update product set quantity = 1 where id = ?", unavailable);
        var order = orders.checkout(key, deliveryRequest());
        assertEquals(2, order.items().size());
        assertEquals(order, orders.checkout(key, deliveryRequest()));
    }

    @Test
    void simultaneousSameKeyRequestsReturnOneOrderAndDeductStockOnce() throws Exception {
        long product = product();
        String email = customerWithItem(product);
        String key = UUID.randomUUID().toString();
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = List.of(1, 2).stream().map(ignored -> executor.submit(() -> {
                assertTrue(start.await(10, TimeUnit.SECONDS));
                authenticate(email);
                try {
                    return orders.checkout(key, deliveryRequest());
                } finally {
                    SecurityContextHolder.clearContext();
                }
            })).toList();
            start.countDown();
            assertEquals(results.getFirst().get(30, TimeUnit.SECONDS), results.getLast().get(30, TimeUnit.SECONDS));
        }
        authenticate(email);
        assertEquals(1, orders.getMyOrders(0, 20).getTotalElements());
        assertEquals(0, jdbc.queryForObject("select quantity from product where id = ?", Integer.class, product));
        assertEquals(1, jdbc.queryForObject("select count(*) from order_item where product_id = ?", Integer.class, product));
    }

    @Test
    void orderDetailsEnforceOwnershipAndAllowAdminWithoutConsumerProfile() {
        Purchase purchase = purchase();
        assertEquals(purchase.order(), orders.getOrderById(purchase.order()).id());
        assertEquals(purchase.item(), orders.getOrderById(purchase.order()).items().getFirst().id());
        String other = customerWithItem(product());
        authenticate(other);
        assertThrows(AccessDeniedException.class, () -> orders.getOrderById(purchase.order()));
        assertThrows(ResourceNotFoundException.class, () -> orders.getOrderById(-1L));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin-without-consumer@example.test", null,
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))));
        assertEquals(purchase.order(), orders.getOrderById(purchase.order()).id());
        assertThrows(ResourceNotFoundException.class, () -> orders.getOrderById(-1L));
    }

    @Test
    void orderPagesAreBoundedScopedAndNewestFirstWithStableTies() {
        Purchase first = purchase();
        carts.addProductToCart(product(), 1);
        long second = orders.checkout(UUID.randomUUID().toString(), deliveryRequest()).id();
        carts.addProductToCart(product(), 1);
        long third = orders.checkout(UUID.randomUUID().toString(), deliveryRequest()).id();
        jdbc.update("update customer_order set created_at = '2020-01-01' where id in (?, ?)", second, third);
        jdbc.update("update customer_order set created_at = '2019-01-01' where id = ?", first.order());
        var page = orders.getMyOrders(0, 2);
        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
        assertEquals(List.of(third, second), page.getContent().stream().map(order -> order.id()).toList());
        assertEquals(first.order(), orders.getMyOrders(1, 2).getContent().getFirst().id());
        assertTrue(orders.getMyOrders(2, 2).isEmpty());
        long consumer = page.getContent().getFirst().consumerId();
        Purchase other = purchase();
        assertEquals(1, orders.getMyOrders(0, 20).getTotalElements());
        assertEquals(other.order(), orders.getMyOrders(0, 20).getContent().getFirst().id());
        assertEquals(List.of(third, second), orders.getOrdersByConsumerId(consumer, 0, 2)
                .getContent().stream().map(order -> order.id()).toList());
        var all = orders.getAllOrders(0, 1);
        assertEquals(1, all.getNumberOfElements());
        assertEquals(other.order(), all.getContent().getFirst().id());
        assertThrows(ResourceNotFoundException.class, () -> orders.getOrdersByConsumerId(-1L, 0, 20));
        assertThrows(BadRequestException.class, () -> orders.getMyOrders(-1, 20));
        assertThrows(BadRequestException.class, () -> orders.getMyOrders(0, 0));
        assertThrows(BadRequestException.class, () -> orders.getAllOrders(0, 101));
        assertEquals(100, orders.getMyOrders(0, 100).getSize());
    }

    private Purchase purchase() {
        long product = product();
        String email = customerWithItem(product);
        authenticate(email);
        long order = orders.checkout(UUID.randomUUID().toString(), deliveryRequest()).id();
        long item = jdbc.queryForObject("select id from order_item where consumer_order_id = ?",
                Long.class, order);
        return new Purchase(product, email, order, item);
    }

    private void assertPurchase(Purchase purchase, OrderStatus status, int stock) {
        assertEquals(status.name(), jdbc.queryForObject("select status from order_item where id = ?",
                String.class, purchase.item()));
        assertEquals(stock, jdbc.queryForObject("select quantity from product where id = ?",
                Integer.class, purchase.product()));
    }

    @Test
    void ownerCancellationRestoresStockOnce() {
        Purchase purchase = purchase();
        assertEquals(OrderStatus.CANCELLED,
                orders.cancelMyOrderItem(purchase.item(), purchase.order()).status());
        assertPurchase(purchase, OrderStatus.CANCELLED, 1);
        assertThrows(BadRequestException.class,
                () -> orders.cancelMyOrderItem(purchase.item(), purchase.order()));
        assertPurchase(purchase, OrderStatus.CANCELLED, 1);
    }

    @Test
    void orderHistoryTotalsReflectPartialAndFullCancellationUsingCheckoutPrices() {
        long first = product();
        long second = product();
        jdbc.update("update product set quantity = 2, price = 15.00 where id = ?", first);
        jdbc.update("update product set price = 70.00 where id = ?", second);
        String email = customerWithItem(first);
        authenticate(email);
        carts.editQuantityCartItem(first, 2);
        carts.addProductToCart(second, 1);
        var checkout = orders.checkout(UUID.randomUUID().toString(), deliveryRequest());
        assertOrderTotals(checkout, "100.00", "0.00", "100.00");
        jdbc.update("update product set price = 999.99 where id in (?, ?)", first, second);
        long firstItem = checkout.items().stream().filter(item -> item.productId().equals(first))
                .findFirst().orElseThrow().id();
        long secondItem = checkout.items().stream().filter(item -> item.productId().equals(second))
                .findFirst().orElseThrow().id();
        orders.cancelMyOrderItem(firstItem, checkout.id());
        assertOrderTotals(orders.getMyOrders(0, 20).getContent().getFirst(), "100.00", "30.00", "70.00");
        orders.cancelMyOrderItem(secondItem, checkout.id());
        assertOrderTotals(orders.getMyOrders(0, 20).getContent().getFirst(), "100.00", "100.00", "0.00");
    }

    private void assertOrderTotals(com.shop.animerch.record.OrderDTO order,
                                   String original, String cancelled, String active) {
        assertEquals(new BigDecimal(original), order.originalTotal());
        assertEquals(0, new BigDecimal(cancelled).compareTo(order.cancelledTotal()));
        assertEquals(new BigDecimal(active), order.activeTotal());
        assertEquals(new BigDecimal(original), order.totalPrice());
    }

    @Test
    void anotherCustomerCannotCancelAndMismatchedOrderIsNotFound() {
        Purchase purchase = purchase();
        String other = customerWithItem(product());
        authenticate(other);
        assertThrows(AccessDeniedException.class,
                () -> orders.cancelMyOrderItem(purchase.item(), purchase.order()));
        assertPurchase(purchase, OrderStatus.PLACED, 0);
        authenticate(purchase.email());
        assertThrows(ResourceNotFoundException.class,
                () -> orders.cancelMyOrderItem(purchase.item(), -1L));
        assertPurchase(purchase, OrderStatus.PLACED, 0);
    }

    @Test
    void shippedAndDeliveredItemsCannotBeCancelled() {
        Purchase purchase = purchase();
        orders.updateStatusOfOrderItem(purchase.item(), purchase.order(),
                new OrderItemStatusUpdateDTO(OrderStatus.SHIPPED));
        assertThrows(BadRequestException.class,
                () -> orders.cancelMyOrderItem(purchase.item(), purchase.order()));
        assertPurchase(purchase, OrderStatus.SHIPPED, 0);
        orders.updateStatusOfOrderItem(purchase.item(), purchase.order(),
                new OrderItemStatusUpdateDTO(OrderStatus.DELIVERED));
        assertThrows(BadRequestException.class,
                () -> orders.cancelMyOrderItem(purchase.item(), purchase.order()));
        assertPurchase(purchase, OrderStatus.DELIVERED, 0);
    }

    @Test
    void simultaneousCancellationsRestoreStockOnce() throws Exception {
        raceCancellation(false);
    }

    @Test
    void cancellationCompetingWithShippingHasOneWinner() throws Exception {
        raceCancellation(true);
    }

    private void raceCancellation(boolean ship) throws Exception {
        Purchase purchase = purchase();
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = List.of(false, ship).stream().map(shipping -> executor.submit(() -> {
                assertTrue(start.await(10, TimeUnit.SECONDS));
                authenticate(purchase.email());
                try {
                    if (shipping) {
                        orders.updateStatusOfOrderItem(purchase.item(), purchase.order(),
                                new OrderItemStatusUpdateDTO(OrderStatus.SHIPPED));
                    } else {
                        orders.cancelMyOrderItem(purchase.item(), purchase.order());
                    }
                    return true;
                } catch (BadRequestException expected) {
                    return false;
                } finally {
                    SecurityContextHolder.clearContext();
                }
            })).toList();
            start.countDown();
            int successes = 0;
            for (var result : results) if (result.get(30, TimeUnit.SECONDS)) successes++;
            assertEquals(1, successes);
        }
        String status = jdbc.queryForObject("select status from order_item where id = ?",
                String.class, purchase.item());
        if (OrderStatus.CANCELLED.name().equals(status)) {
            assertPurchase(purchase, OrderStatus.CANCELLED, 1);
        } else {
            assertTrue(ship);
            assertPurchase(purchase, OrderStatus.SHIPPED, 0);
        }
    }

    @Test
    void sellerResponsesLoadPhotosAndSecondUploadIsRejected() {
        String name = "seller-" + UUID.randomUUID();
        Long seller = jdbc.queryForObject("insert into seller(name) values (?) returning id", Long.class, name);
        Long photo = jdbc.queryForObject("insert into seller_photo(seller_id, url) values (?, ?) returning id",
                Long.class, seller, UUID.randomUUID() + ".png");
        assertEquals(photo, sellers.getSellerById(seller).photo().id());
        assertEquals(photo, sellers.searchSellers(name, PageRequest.of(0, 20)).getContent().getFirst().photo().id());
        assertThrows(ConflictException.class, () -> sellerPhotos.uploadPhoto(seller,
                new MockMultipartFile("file", "test.png", "image/png", new byte[0])));
    }

    @Test
    void competingCustomersCannotBuyTheSameLastItem() throws Exception {
        long product = product();
        String first = customerWithItem(product);
        String second = customerWithItem(product);
        raceCheckout(first, second);
        assertEquals(0, jdbc.queryForObject("select quantity from product where id = ?", Integer.class, product));
        assertEquals(1, jdbc.queryForObject("select count(*) from order_item where product_id = ?", Integer.class, product));
        assertEquals(new BigDecimal("12.34"), jdbc.queryForObject(
                "select unit_price from order_item where product_id = ?", BigDecimal.class, product));
    }

    @Test
    void simultaneousCheckoutOfOneCartCreatesOnlyOneOrder() throws Exception {
        long product = product();
        String email = customerWithItem(product);
        raceCheckout(email, email);
        assertEquals(1, jdbc.queryForObject("select count(*) from order_item where product_id = ?", Integer.class, product));
    }

    @Test
    void databaseRejectsDuplicateCartsItemsAndInvalidCatalogValues() {
        long product = product();
        String email = customerWithItem(product);
        Long consumer = jdbc.queryForObject("select c.id from consumer c join app_user u on u.id = c.user_id where u.email = ?", Long.class, email);
        Long cart = jdbc.queryForObject("select id from cart where consumer_id = ?", Long.class, consumer);
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("insert into cart(consumer_id) values (?)", consumer));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("insert into cart_item(cart_id, product_id, quantity) values (?, ?, 1)", cart, product));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("update product set price = 0 where id = ?", product));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("update product set quantity = -1 where id = ?", product));
    }

    private long product() {
        return jdbc.queryForObject("insert into product(name, description, quantity, price) values (?, 'test', 1, 12.34) returning id",
                Long.class, "regression-" + UUID.randomUUID());
    }

    private String customerWithItem(long product) {
        String email = UUID.randomUUID() + "@example.com";
        registration.register(new RegisterRequest("Regression test", "12345", email, "Test-password-123"));
        authenticate(email);
        try {
            carts.addProductToCart(product, 1);
        } finally {
            SecurityContextHolder.clearContext();
        }
        return email;
    }

    private void raceCheckout(String first, String second) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = List.of(first, second).stream().map(email -> executor.submit(() -> {
                assertTrue(start.await(10, TimeUnit.SECONDS));
                authenticate(email);
                try {
                    orders.checkout(UUID.randomUUID().toString(), deliveryRequest());
                    return true;
                } catch (BadRequestException expected) {
                    return false;
                } finally {
                    SecurityContextHolder.clearContext();
                }
            })).toList();
            start.countDown();
            int successes = 0;
            for (var result : results) {
                if (result.get(30, TimeUnit.SECONDS)) successes++;
            }
            assertEquals(1, successes);
        }
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, List.of()));
    }
}
