package com.shop.animerch.service;

import com.shop.animerch.exception.ConflictException;
import com.shop.animerch.model.Cart;
import com.shop.animerch.model.Consumer;
import com.shop.animerch.model.User;
import com.shop.animerch.record.RegisterRequest;
import com.shop.animerch.record.RegisterResponse;
import com.shop.animerch.repository.CartRepository;
import com.shop.animerch.repository.ConsumerRepository;
import com.shop.animerch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RegisterService {

    private final UserRepository userRepository;
    private final ConsumerRepository consumerRepository;
    private final PasswordEncoder passwordEncoder;
    private final CartRepository cartRepository;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Transactional
    public RegisterResponse register(RegisterRequest registerRequest) {
        String email = registerRequest.email()
                .trim()
                .toLowerCase(Locale.ROOT);
        boolean isEmailAlreadyExist =
                userRepository.existsByEmail(email);

        if (isEmailAlreadyExist || Objects.equals(email, adminEmail)) {
            throw new ConflictException(
                    "Email: %s already exists".formatted(email)
            );
        }


        String hashedPassword = passwordEncoder.encode(registerRequest.password());

        User user = User.createCustomer(email,hashedPassword);
        userRepository.save(user);

        Consumer consumer = Consumer.create(
                registerRequest.name(),
                registerRequest.phone(),
                user
        );
        consumerRepository.save(consumer);
        Cart cart = new Cart();
        cart.setConsumer(consumer);

        cartRepository.save(cart);
        return new RegisterResponse(
                user.getId(),
                user.getEmail(),
                user.getUserRole()
        );
    }
}
