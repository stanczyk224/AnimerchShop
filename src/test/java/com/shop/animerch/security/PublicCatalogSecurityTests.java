package com.shop.animerch.security;

import com.shop.animerch.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig(PublicCatalogSecurityTests.Config.class)
@WebAppConfiguration
class PublicCatalogSecurityTests {
    @Configuration
    @EnableWebSecurity
    @EnableWebMvc
    @Import(SecurityConfig.class)
    static class Config {
        @Bean CustomUserDetailsService users() { return mock(CustomUserDetailsService.class); }
        @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
        @Bean JwtAuthenticationFilter jwtFilter(CustomUserDetailsService users) {
            return new JwtAuthenticationFilter(mock(JwtService.class), users);
        }
        @Bean CatalogProbe catalogProbe() { return new CatalogProbe(); }
    }

    @RestController
    static class CatalogProbe {
        @GetMapping({"/api/products", "/api/products/{productId}",
                "/api/products/{productId}/photos",
                "/api/products/{productId}/photos/{photoId}/content",
                "/api/categories", "/api/categories/{categoryId}", "/api/sellers", "/api/sellers/{sellerId}"})
        String catalog() { return "public catalog"; }
    }

    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy security;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/products", "/api/products/1", "/api/products/1/photos",
            "/api/products/1/photos/2/content", "/api/categories", "/api/categories/1", "/api/sellers", "/api/sellers/1"})
    void guestsCanReadCatalog(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/cart", "/api/cart/items", "/api/wishlist", "/api/order/my",
            "/api/consumers", "/api/products/1/reviews"})
    void privateReadsStillRequireAuthentication(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST /api/order/checkout", "POST /api/cart/items/1",
            "POST /api/products/addProduct", "POST /api/products/import",
            "PATCH /api/products/1", "DELETE /api/products/delete/1",
            "POST /api/products/1/photos", "DELETE /api/products/1/photos/2",
            "POST /api/sellers/addSeller", "DELETE /api/sellers/delete/1"})
    void guestsCannotCheckoutOrModifyCatalog(String action) throws Exception {
        String[] parts = action.split(" ", 2);
        mvc.perform(request(HttpMethod.valueOf(parts[0]), parts[1]))
                .andExpect(status().isUnauthorized());
    }
}
