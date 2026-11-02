package com.shop.animerch.service;

import com.shop.animerch.record.LoginRequest;
import com.shop.animerch.record.LoginResponse;
import com.shop.animerch.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.AuthenticationManager;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtService jwtService;
   private final AuthenticationManager authenticationManager;
    public LoginResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.email(),
                        loginRequest.password()
                )
        );
        String email = authentication.getName();
        String token = jwtService.generateToken(email);

        return new LoginResponse(
                token,
                "Bearer"
        );
    }
}
