
package com.shop.animerch.service;

import com.shop.animerch.exception.BadRequestException;
import com.shop.animerch.exception.ConflictException;
import com.shop.animerch.record.CheckoutRequest;
import com.shop.animerch.exception.ResourceNotFoundException;
import com.shop.animerch.mapper.OrderMapper;
import com.shop.animerch.model.*;
import com.shop.animerch.record.OrderDTO;
import com.shop.animerch.record.OrderItemDTO;
import com.shop.animerch.record.OrderItemStatusUpdateDTO;
import com.shop.animerch.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AnonymousAuthenticationToken;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.persistence.EntityManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ConsumerRepository consumerRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final EntityManager entityManager;

    private final OrderItemRepository orderItemRepository;

    private Consumer getCurrentConsumer() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User is not authenticated");
        }

        String email = authentication.getName();

        return consumerRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Consumer with email=%s not found".formatted(email)
                ));
    }

    private Map<Long, Product> validateAndLockProducts(List<CartItem> cartItems) {
        Map<Long, Integer> productQuantities = new HashMap<>();

        for (CartItem cartItem : cartItems) {
            Long productId = cartItem.getProduct().getId();
            int quantity = cartItem.getQuantity();

            if (quantity < 1) {
                throw new BadRequestException(
                        "Invalid quantity for product id=%d".formatted(productId)
                );
            }

            productQuantities.merge(
                    productId,
                    quantity,
                    Integer::sum
            );
        }

        Map<Long, Product> products = new HashMap<>();

        for (Long productId : productQuantities.keySet().stream().sorted().toList()) {

            Product product = productRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product with id=%d not found".formatted(productId)
                    ));

            int requestedQuantity = productQuantities.get(productId);

            // Cart loading may already have put this product into the persistence context.
            // Read the stock again after obtaining the lock, rather than using that snapshot.
            entityManager.refresh(product);

            if (requestedQuantity > product.getQuantity()) {
                throw new BadRequestException(
                        "Insufficient stock for product id=%d. Available: %d, requested: %d"
                                .formatted(
                                        productId,
                                        product.getQuantity(),
                                        requestedQuantity
                                )
                );
            }

            products.put(productId, product);
        }

        return products;
    }

    private OrderItem createOrderItem(
            CartItem cartItem,
            Map<Long, Product> products
    ) {
        Long productId = cartItem.getProduct().getId();

        Product product = products.get(productId);
        int quantity = cartItem.getQuantity();

        product.setQuantity(product.getQuantity() - quantity);

        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(product);
        orderItem.setProductName(product.getName());
        orderItem.setQuantity(quantity);
        orderItem.setUnitPrice(product.getPrice());

        return orderItem;
    }

    @Transactional
    public OrderDTO checkout(String idempotencyKey, CheckoutRequest request) {
        if (idempotencyKey == null || !idempotencyKey.matches("[A-Za-z0-9._:-]{1,128}")) {
            throw new BadRequestException("Idempotency-Key must contain 1 to 128 letters, digits, dots, underscores, colons or hyphens");
        }
        Consumer consumer = getCurrentConsumer();

        var address = request.deliveryAddress();
        DeliveryAddress snapshot = new DeliveryAddress(address.recipientName(), address.phone(),
                address.addressLine1(), address.addressLine2(), address.city(), address.postalCode(), address.country());

        log.debug(
                "Starting checkout for consumer id={}",
                consumer.getId()
        );

        Cart cart = cartRepository.findByConsumerIdForUpdate(consumer.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart for consumer id=%d not found".formatted(consumer.getId())
                ));

        // Serialize checkout for this customer before checking the key. A waiting
        // retry must see the order committed by the first request before reading the cart.
        var existing = orderRepository.findByConsumerIdAndCheckoutKey(consumer.getId(), idempotencyKey);
        if (existing.isPresent()) {
            if (!snapshot.equals(existing.get().getDeliveryAddress())) {
                throw new ConflictException(
                        "Idempotency-Key was already used with different delivery details");
            }
            return orderMapper.toDTO(existing.get());
        }

        List<CartItem> cartItems = cartItemRepository.findAllByCart(cart);

        if (cartItems.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        Map<Long, Product> products = validateAndLockProducts(cartItems);

        Order order = new Order();
        order.setConsumer(consumer);
        order.setCheckoutKey(idempotencyKey);
        order.setDeliveryAddress(snapshot);

        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = createOrderItem(cartItem, products);
            order.addItem(orderItem);
        }

        order.recalculateTotalPrice();

        Order savedOrder = orderRepository.save(order);

        cartItemRepository.deleteAll(cartItems);
        cart.getCartItems().clear();

        log.info(
                "Checkout completed: orderId={}, consumerId={}, totalPrice={}, itemsCount={}",
                savedOrder.getId(),
                consumer.getId(),
                savedOrder.getTotalPrice(),
                savedOrder.getOrderItems().size()
        );

        return orderMapper.toDTO(savedOrder);
    }
    private Pageable orderPage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("Page must be non-negative and size must be between 1 and 100");
        }
        return PageRequest.of(page, size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")));
    }

    @Transactional(readOnly = true)
    public Page<OrderDTO> getMyOrders(int page, int size) {
        var pageable = orderPage(page, size);
        Consumer consumer = getCurrentConsumer();
        return orderRepository.findAllByConsumerId(consumer.getId(), pageable).map(orderMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<OrderDTO> getAllOrders(int page, int size) {
        return orderRepository.findAll(orderPage(page, size)).map(orderMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<OrderDTO> getOrdersByConsumerId(Long consumerId, int page, int size) {
        var pageable = orderPage(page, size);
        if (!consumerRepository.existsById(consumerId)) {
            throw new ResourceNotFoundException("Consumer with id=%d not found".formatted(consumerId));
        }
        return orderRepository.findAllByConsumerId(consumerId, pageable).map(orderMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long orderId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("User is not authenticated");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id=%d not found".formatted(orderId)));
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (!admin && !order.getConsumer().getId().equals(getCurrentConsumer().getId())) {
            throw new AccessDeniedException("You can only view your own orders");
        }
        return orderMapper.toDTO(order);
    }

    @Transactional
    public OrderItemDTO cancelMyOrderItem(Long orderItemId, Long orderId) {
        Consumer consumer = getCurrentConsumer();
        OrderItem item = orderItemRepository.findByIdAndOrderIdForUpdate(orderItemId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found"));
        if (!item.getOrder().getConsumer().getId().equals(consumer.getId())) {
            throw new AccessDeniedException("You can only cancel your own order items");
        }
        return changeStatus(item, OrderStatus.CANCELLED);
    }

    @Transactional
    public OrderItemDTO updateStatusOfOrderItem(Long orderItemId, Long orderId, OrderItemStatusUpdateDTO dto){
        OrderStatus nextStatus = dto.status();
        OrderItem orderItem = orderItemRepository.findByIdAndOrderIdForUpdate(orderItemId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Item with orderId=%d and orderItemId=%d".formatted(orderId,orderItemId)
                ));
        return changeStatus(orderItem, nextStatus);
    }

    private OrderItemDTO changeStatus(OrderItem orderItem, OrderStatus nextStatus) {
        boolean allowed = isAllowed(orderItem, nextStatus);
        if(!allowed) {
            throw new BadRequestException(
                    "invalid transition %s for status %s".formatted(nextStatus, orderItem.getStatus())
            );
        }
        if(nextStatus==OrderStatus.CANCELLED){
            Long productId = orderItem.getProduct().getId();
            Product product = productRepository.findByIdForUpdate(productId)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Product with id=%d not found".formatted(productId)
                            ));
            entityManager.refresh(product);
            product.setQuantity(product.getQuantity() + orderItem.getQuantity()); // Changes the quantity of the product
        }
        orderItem.setStatus(nextStatus);
        return orderMapper.toDTO(orderItem);


    }

    private static boolean isAllowed(OrderItem orderItem, OrderStatus nextStatus) {
        OrderStatus itemStatus = orderItem.getStatus();
        /*
        ________________________________________
        | Current status | Allowed next status |
        |----------------|---------------------|
        | PLACED         | SHIPPED, CANCELLED  |
        | SHIPPED        | DELIVERED           |
        | DELIVERED      | None                |
        | CANCELLED      | None                |
        ---------------------------------------
         */
        return switch (itemStatus) {
            case PLACED ->
                    nextStatus == OrderStatus.SHIPPED
                            || nextStatus == OrderStatus.CANCELLED;
            case SHIPPED ->
                    nextStatus == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}
