
package com.shop.animerch.service;

import com.shop.animerch.exception.BadRequestException;
import com.shop.animerch.exception.ConflictException;
import com.shop.animerch.exception.ResourceNotFoundException;
import com.shop.animerch.mapper.CartItemMapper;
import com.shop.animerch.model.Cart;
import com.shop.animerch.model.CartItem;
import com.shop.animerch.model.Consumer;
import com.shop.animerch.model.Product;
import com.shop.animerch.record.CartItemDTO;
import com.shop.animerch.repository.CartItemRepository;
import com.shop.animerch.repository.CartRepository;
import com.shop.animerch.repository.ConsumerRepository;
import com.shop.animerch.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartItemService {

    private final CartItemRepository cartItemRepository;
    private final CartItemMapper cartItemMapper;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final ConsumerRepository consumerRepository;

    @Transactional
    public List<CartItemDTO> getCartItems() {
        Cart cart = getCurrentCart();

        List<CartItem> cartItems = cartItemRepository.findAllByCart(cart);

        log.debug("Fetched {} cart items for current user's cart", cartItems.size());

        return cartItemMapper.toDTOList(cartItems);
    }

    @Transactional
    public CartItemDTO getCartItem(Long productId) {
        Cart cart = getCurrentCart();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product with id=%d not found".formatted(productId)
                ));

        CartItem cartItem = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product id=%d not found in current user's cart".formatted(productId)
                ));

        log.debug("Fetched cart item for product id={}", productId);

        return cartItemMapper.toDTO(cartItem);
    }

    @Transactional
    public CartItemDTO addProductToCart(Long productId, int quantity) {
        log.debug("Adding product id={} to current user's cart", productId);
        if (quantity < 1) {
            throw new BadRequestException("Quantity must be at least 1");
        }

        Cart cart = getCurrentCart();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product with id=%d not found".formatted(productId)
                ));

        CartItem cartItem = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseGet(() -> {
                    CartItem newItem = new CartItem();
                    newItem.setCart(cart);
                    newItem.setProduct(product);
                    newItem.setQuantity(0);
                    return newItem;
                });

        cartItem.setQuantity(cartItem.getQuantity() + quantity);

        if (cartItem.getQuantity() > product.getQuantity()) {
            throw new ConflictException(
                    "Not enough stock for product id=%d".formatted(productId)
            );
        }

        CartItem savedCartItem = cartItemRepository.save(cartItem);

        log.info(
                "Cart item id={} saved with quantity={}",
                savedCartItem.getId(),
                savedCartItem.getQuantity()
        );

        return cartItemMapper.toDTO(savedCartItem);
    }

    @Transactional
    public CartItemDTO editQuantityCartItem(Long productId, int quantity) {
        log.debug(
                "Updating quantity of product id={} in current user's cart to {}",
                productId, quantity
        );

        if (quantity < 1) {
            throw new BadRequestException("Quantity must be at least 1");
        }

        Cart cart = getCurrentCart();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product with id=%d not found".formatted(productId)
                ));

        CartItem cartItem = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product id=%d not found in current user's cart".formatted(productId)
                ));

        if (quantity > product.getQuantity()) {
            throw new ConflictException(
                    "Not enough stock for product id=%d".formatted(productId)
            );
        }

        cartItem.setQuantity(quantity);

        CartItem savedCartItem = cartItemRepository.save(cartItem);

        log.info(
                "Cart item id={} updated with quantity={}",
                savedCartItem.getId(),
                savedCartItem.getQuantity()
        );

        return cartItemMapper.toDTO(savedCartItem);
    }

    @Transactional
    public void deleteCartItem(Long productId) {
        log.debug("Removing product id={} from current user's cart", productId);

        Cart cart = getCurrentCart();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product with id=%d not found".formatted(productId)
                ));

        CartItem cartItem = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product id=%d not found in current user's cart".formatted(productId)
                ));

        cartItemRepository.delete(cartItem);

        log.debug("Cart item with id={} successfully deleted", cartItem.getId());
    }

    private Cart getCurrentCart() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User is not authenticated");
        }

        String email = authentication.getName();

        Consumer consumer = consumerRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Consumer with email=%s not found".formatted(email)
                ));

        return cartRepository.findByConsumerId(consumer.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart for consumer with id=%d not found".formatted(consumer.getId())
                ));
    }
    @Transactional
    public void clearCart() {
        Cart cart = getCurrentCart();

        cartItemRepository.deleteAllByCart(cart);

        log.info("Cart id={} successfully cleared", cart.getId());
    }
}
