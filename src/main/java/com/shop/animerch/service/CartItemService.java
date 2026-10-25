package com.shop.animerch.service;

import com.shop.animerch.exception.BadRequestException;
import com.shop.animerch.exception.ConflictException;
import com.shop.animerch.exception.ResourceNotFoundException;
import com.shop.animerch.mapper.CartItemMapper;
import com.shop.animerch.model.Cart;
import com.shop.animerch.model.CartItem;
import com.shop.animerch.model.Product;
import com.shop.animerch.record.CartItemDTO;
import com.shop.animerch.repository.CartItemRepository;
import com.shop.animerch.repository.CartRepository;
import com.shop.animerch.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public List<CartItemDTO> getCartItemsByCartId(Long cartId) {
        log.debug("Fetching cart items");
        List<CartItem> cartItems = cartItemRepository.findAllByCart_Id(cartId);
        log.info("fetched {} cart items", cartItems.size());
        return cartItemMapper.toDTOList(cartItems);
    }
    public CartItemDTO getCartItemByCartAndProduct(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart with id=%d not found".formatted(cartId)
                ));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product with id=%d not found".formatted(productId)
                ));

        CartItem cartItem = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item for cart id=%d and product id=%d not found"
                                .formatted(cartId, productId)
                ));

        return cartItemMapper.toDTO(cartItem);
    }
    @Transactional
    public CartItemDTO addProductToCart(Long cartId, Long productId) {
        log.debug("Adding product id={} to cart id={}", productId, cartId);

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart with id=%d not found".formatted(cartId)
                ));

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

        cartItem.setQuantity(cartItem.getQuantity() + 1);
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
    public CartItemDTO editQuantityCartItem(Long cartId, Long productId, int quantity){
        log.debug("Editing quantity of the product with id={} to cart with id={}", productId, cartId);
        if (quantity < 1) {
            throw new BadRequestException(
                    "Quantity must be at least 1"
            );
        }
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart with id=%d not found".formatted(cartId)
                ));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product with id=%d not found".formatted(productId)
                ));

        CartItem cartItem = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item for cart id=%d and product id=%d not found"
                                .formatted(cartId, productId)
                ));
        cartItem.setQuantity(quantity);
        if (cartItem.getQuantity() > product.getQuantity()) {
            throw new ConflictException(
                    "Not enough stock for product id=%d".formatted(productId)
            );
        }
        CartItem savedCartItem = cartItemRepository.save(cartItem);

        log.info(
                "Cart item id={} edited with quantity={}",
                savedCartItem.getId(),
                savedCartItem.getQuantity()
        );

        return cartItemMapper.toDTO(savedCartItem);
    }
    @Transactional
    public void deleteCartItem(Long cartId, Long productId) {
        log.debug("Deleting product id={} from cart id={}", productId, cartId);
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart with id=%d not found".formatted(cartId)
                ));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product with id=%d not found".formatted(productId)
                ));

        CartItem cartItem = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item for cart id=%d and product id=%d not found"
                                .formatted(cartId, productId)
                ));

        cartItemRepository.delete(cartItem);
        log.debug("Cart item with id={} successfully deleted", cartItem.getId());
    }

}
