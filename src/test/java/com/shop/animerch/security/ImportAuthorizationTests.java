package com.shop.animerch.security;

import com.shop.animerch.controller.*;
import com.shop.animerch.service.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(ImportAuthorizationTests.Config.class)
class ImportAuthorizationTests {
    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean ProductService products() { return mock(ProductService.class); }
        @Bean SellerService sellers() { return mock(SellerService.class); }
        @Bean CategoryService categories() { return mock(CategoryService.class); }
        @Bean ProductController productController(ProductService service) { return new ProductController(service); }
        @Bean SellerController sellerController(SellerService service) { return new SellerController(service); }
        @Bean CategoryController categoryController(CategoryService service) { return new CategoryController(service); }
    }

    @Autowired ProductController products;
    @Autowired SellerController sellers;
    @Autowired CategoryController categories;

    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    @Test void customerCannotImportCatalogData() {
        authenticate("CUSTOMER");
        assertThrows(AccessDeniedException.class, () -> products.importProducts(null));
        assertThrows(AccessDeniedException.class, () -> sellers.importSellers(null));
        assertThrows(AccessDeniedException.class, () -> categories.importCategories(null));
    }

    @Test void adminCanImportCatalogData() {
        authenticate("ADMIN");
        assertDoesNotThrow(() -> products.importProducts(null));
        assertDoesNotThrow(() -> sellers.importSellers(null));
        assertDoesNotThrow(() -> categories.importCategories(null));
    }

    private void authenticate(String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
