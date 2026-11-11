package com.shop.animerch;

import com.shop.animerch.record.ProductDTO;
import com.shop.animerch.record.RegisterRequest;
import com.shop.animerch.service.ProductService;
import com.shop.animerch.service.RegisterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class ProductRatingDatabaseTests {
    @Autowired ProductService products;
    @Autowired RegisterService registration;
    @Autowired JdbcTemplate jdbc;

    private long customer() {
        String email = UUID.randomUUID() + "@example.test";
        registration.register(new RegisterRequest("Rating test", "12345", email, "Test-password-123"));
        return jdbc.queryForObject("select c.id from consumer c join app_user u on u.id = c.user_id where u.email = ?",
                Long.class, email);
    }

    private long product(String name) {
        return jdbc.queryForObject("insert into product(name, description, quantity, price) values (?, 'test', 1, 12.34) returning id",
                Long.class, name);
    }

    private long review(long consumer, long product, int rating) {
        return jdbc.queryForObject("""
                insert into review(consumer_id, product_id, rating, comment, created_at)
                values (?, ?, ?, 'Test review', current_timestamp) returning id
                """, Long.class, consumer, product, rating);
    }

    private void assertSummary(ProductDTO product, Double average, long count) {
        assertEquals(average, product.averageRating());
        assertEquals(count, product.reviewCount());
    }

    @Test
    void summariesFollowReviewCreationRatingEditsAndDeletion() {
        long product = product("rating-" + UUID.randomUUID());
        assertSummary(products.getProductById(product), null, 0);
        long first = review(customer(), product, 5);
        long second = review(customer(), product, 4);
        assertSummary(products.getProductById(product), 4.5, 2);
        jdbc.update("update review set rating = 2 where id = ?", second);
        assertSummary(products.getProductById(product), 3.5, 2);
        jdbc.update("delete from review where id = ?", second);
        assertSummary(products.getProductById(product), 5.0, 1);
        jdbc.update("delete from review where id = ?", first);
        assertSummary(products.getProductById(product), null, 0);
    }

    @Test
    void filteredPagesAndUnpaginatedListsKeepEachProductsOwnSummary() {
        String prefix = "rating-" + UUID.randomUUID();
        long first = product(prefix + "-first");
        long empty = product(prefix + "-empty");
        long second = product(prefix + "-second");
        long consumer = customer();
        review(consumer, first, 5);
        review(consumer, second, 2);
        var page = products.searchProducts(prefix, null, null, null, PageRequest.of(0, 20));
        assertEquals(3, page.getTotalElements());
        for (ProductDTO product : page) {
            if (product.id() == first) assertSummary(product, 5.0, 1);
            else if (product.id() == second) assertSummary(product, 2.0, 1);
            else {
                assertEquals(empty, product.id());
                assertSummary(product, null, 0);
            }
        }
        assertSummary(products.getProduct().stream().filter(p -> p.id() == first).findFirst().orElseThrow(), 5.0, 1);
        assertTrue(products.searchProducts(prefix, null, null, null, PageRequest.of(1, 20)).isEmpty());
    }
}
