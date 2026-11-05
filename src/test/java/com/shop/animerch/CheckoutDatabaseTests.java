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
                    orders.checkout();
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
