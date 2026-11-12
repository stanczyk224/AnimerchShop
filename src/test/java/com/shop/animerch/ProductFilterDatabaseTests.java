package com.shop.animerch;

import com.shop.animerch.exception.BadRequestException;
import com.shop.animerch.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class ProductFilterDatabaseTests {
    @Autowired ProductService products;
    @Autowired JdbcTemplate jdbc;

    @Test void sellerCategorySearchAndPricesCombineBeforePagination() {
        String prefix = "filter-" + UUID.randomUUID();
        long seller = jdbc.queryForObject("insert into seller(name) values (?) returning id", Long.class, prefix);
        long otherSeller = jdbc.queryForObject("insert into seller(name) values (?) returning id", Long.class, prefix + " other");
        long category = jdbc.queryForObject("insert into category(name) values (?) returning id", Long.class, prefix);
        long first = product(prefix + " A", seller, 100);
        long second = product(prefix + " B", seller, 150);
        long expensive = product(prefix + " C", seller, 300);
        long other = product(prefix + " D", otherSeller, 120);
        for (long product : new long[]{first, second, expensive, other})
            jdbc.update("insert into product_category(product_id, category_id) values (?, ?)", product, category);
        product(prefix + " no category", seller, 110);
        var page = products.searchProducts(prefix, category, new BigDecimal("90"), new BigDecimal("200"), seller,
                PageRequest.of(0, 1, Sort.by("price")));
        assertEquals(2, page.getTotalElements());
        assertEquals(first, page.getContent().getFirst().id());
        var next = products.searchProducts(prefix, category, new BigDecimal("90"), new BigDecimal("200"), seller,
                PageRequest.of(1, 1, Sort.by("price")));
        assertEquals(second, next.getContent().getFirst().id());
        assertTrue(products.searchProducts(prefix, category, null, null, Long.MAX_VALUE, PageRequest.of(0, 20)).isEmpty());
    }

    @Test void invalidPriceRangesAndIdentifiersAreRejected() {
        assertThrows(BadRequestException.class, () -> products.searchProducts(null, null, new BigDecimal("-1"), null, null, PageRequest.of(0, 20)));
        assertThrows(BadRequestException.class, () -> products.searchProducts(null, null, new BigDecimal("200"), new BigDecimal("100"), null, PageRequest.of(0, 20)));
        assertThrows(BadRequestException.class, () -> products.searchProducts(null, null, null, null, 0L, PageRequest.of(0, 20)));
    }

    private long product(String name, long seller, int price) {
        return jdbc.queryForObject("insert into product(name, description, quantity, price, seller_id) values (?, 'filter test', 5, ?, ?) returning id", Long.class, name, price, seller);
    }
}
