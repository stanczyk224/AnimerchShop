package com.shop.animerch;

import com.shop.animerch.exception.BadRequestException;
import com.shop.animerch.exception.ResourceNotFoundException;
import com.shop.animerch.record.RegisterRequest;
import com.shop.animerch.security.JwtService;
import com.shop.animerch.service.RegisterService;
import com.shop.animerch.service.WishlistService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class WishlistDatabaseTests {
    @Autowired
    WishlistService wishlist;
    @Autowired
    RegisterService registration;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    JwtService jwt;
    @Autowired
    WebApplicationContext context;
    @Autowired
    FilterChainProxy security;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private String customer() {
        String email = UUID.randomUUID() + "@example.com";
        registration.register(new RegisterRequest("Wishlist test", "12345", email, "Test-password-123"));
        authenticate(email);
        return email;
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, List.of()));
    }

    private long product() {
        return jdbc.queryForObject("insert into product(name, description, quantity, price) values (?, 'test', 0, 12.34) returning id",
                Long.class, "wishlist-" + UUID.randomUUID());
    }

    @Test
    void addIsIdempotentAndAllowsOutOfStockProductsWithoutReservingStock() {
        customer();
        long product = product();
        var item = wishlist.add(product);
        assertNotNull(item.id());
        assertNotNull(item.createdAt());
        assertEquals(product, item.product().id());
        assertEquals(item, wishlist.add(product));
        assertEquals(1, wishlist.getMyWishlist(0, 20).getTotalElements());
        assertEquals(0, jdbc.queryForObject("select quantity from product where id = ?", Integer.class, product));
        assertThrows(ResourceNotFoundException.class, () -> wishlist.add(-1L));
    }

    @Test
    void listsAndRemovalAreScopedToAuthenticatedCustomer() {
        String first = customer();
        long product = product();
        wishlist.add(product);
        String second = customer();
        assertTrue(wishlist.getMyWishlist(0, 20).isEmpty());
        wishlist.remove(product);
        wishlist.add(product);
        wishlist.remove(product);
        wishlist.remove(product);
        assertTrue(wishlist.getMyWishlist(0, 20).isEmpty());
        authenticate(first);
        assertEquals(1, wishlist.getMyWishlist(0, 20).getTotalElements());
        authenticate(second);
        assertTrue(wishlist.getMyWishlist(0, 20).isEmpty());
    }

    @Test
    void paginationIsBoundedAndSortsNewestFirstWithStableTies() {
        customer();
        var first = wishlist.add(product());
        var second = wishlist.add(product());
        var third = wishlist.add(product());
        jdbc.update("update wishlist_item set created_at = '2020-01-01' where id in (?, ?)", first.id(), second.id());
        var page = wishlist.getMyWishlist(0, 2);
        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
        assertEquals(List.of(third.id(), second.id()), page.getContent().stream().map(item -> item.id()).toList());
        assertEquals(first.id(), wishlist.getMyWishlist(1, 2).getContent().getFirst().id());
        assertTrue(wishlist.getMyWishlist(2, 2).isEmpty());
        assertThrows(BadRequestException.class, () -> wishlist.getMyWishlist(-1, 20));
        assertThrows(BadRequestException.class, () -> wishlist.getMyWishlist(0, 0));
        assertThrows(BadRequestException.class, () -> wishlist.getMyWishlist(0, 101));
    }

    @Test
    void concurrentDuplicateSavesCreateOnlyOneBookmark() throws Exception {
        String email = customer();
        long product = product();
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = List.of(1, 2).stream().map(ignored -> executor.submit(() -> {
                assertTrue(start.await(10, TimeUnit.SECONDS));
                authenticate(email);
                try {
                    return wishlist.add(product);
                } finally {
                    SecurityContextHolder.clearContext();
                }
            })).toList();
            start.countDown();
            assertEquals(results.getFirst().get(30, TimeUnit.SECONDS), results.getLast().get(30, TimeUnit.SECONDS));
        }
        assertEquals(1, wishlist.getMyWishlist(0, 20).getTotalElements());
    }

    @Test
    void databaseEnforcesUniquePairAndCascadesDeletedProductsAndConsumers() {
        customer();
        long product = product();
        var item = wishlist.add(product);
        Long consumer = jdbc.queryForObject("select consumer_id from wishlist_item where id = ?", Long.class, item.id());
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "insert into wishlist_item(consumer_id, product_id, created_at) values (?, ?, current_timestamp)", consumer, product));
        jdbc.update("delete from product where id = ?", product);
        assertTrue(wishlist.getMyWishlist(0, 20).isEmpty());
        wishlist.add(product());
        jdbc.update("delete from cart where consumer_id = ?", consumer);
        jdbc.update("delete from consumer where id = ?", consumer);
        assertEquals(0, jdbc.queryForObject("select count(*) from wishlist_item where consumer_id = ?", Integer.class, consumer));
    }

    @Test
    void httpEndpointsAuthenticateValidateAndReturnDocumentedResponses() throws Exception {
        String email = customer();
        String authorization = "Bearer " + jwt.generateToken(email);
        long product = product();
        SecurityContextHolder.clearContext();
        mvc.perform(get("/api/wishlist")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/wishlist/items/" + product)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/wishlist/items/" + product)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/wishlist/items/" + product).header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.product.id").value(product));
        mvc.perform(get("/api/wishlist").header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/wishlist?size=101").header("Authorization", authorization))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/wishlist/items/-1").header("Authorization", authorization))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/wishlist/items/" + product).header("Authorization", authorization))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/wishlist/items/" + product).header("Authorization", authorization))
                .andExpect(status().isNoContent());
    }

    @Test
    void callersWithoutCustomerProfilesCannotUseWishlist() {
        SecurityContextHolder.clearContext();
        assertThrows(AccessDeniedException.class, () -> wishlist.add(1L));
        authenticate("no-profile@example.test");
        assertThrows(AccessDeniedException.class, () -> wishlist.getMyWishlist(0, 20));
        assertThrows(AccessDeniedException.class, () -> wishlist.remove(1L));
    }
}
