package com.shop.animerch.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTests {
    private final String email = "customer@example.com";
    private final String secret = Base64.getEncoder().encodeToString(new byte[32]);
    private final JwtService jwt = new JwtService(secret, 60000);
    private final CustomUserDetailsService users = mock(CustomUserDetailsService.class);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validTokenAuthenticatesWithCurrentDatabaseRole() throws Exception {
        when(users.loadUserByUsername(email)).thenReturn(User.withUsername(email)
                .password("hash").roles("CUSTOMER").build());
        filter(jwt.generateToken(email));
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(email, authentication.getName());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_CUSTOMER")));
    }

    @Test
    void disabledAccountCannotUsePreviouslyIssuedToken() throws Exception {
        String token = jwt.generateToken(email);
        when(users.loadUserByUsername(email)).thenReturn(User.withUsername(email)
                .password("hash").roles("CUSTOMER").disabled(true).build());
        filter(token);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void expiredTokenIsRejectedBeforeLoadingUser() throws Exception {
        filter(new JwtService(secret, -60000).generateToken(email));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(users);
    }

    @Test
    void tokenSignedWithDifferentKeyIsRejected() throws Exception {
        String otherSecret = Base64.getEncoder().encodeToString(
                "a-different-signing-key-with-at-least-32-bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        filter(new JwtService(otherSecret, 60000).generateToken(email));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(users);
    }

    private void filter(String token) throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);
        new JwtAuthenticationFilter(jwt, users).doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }
}
