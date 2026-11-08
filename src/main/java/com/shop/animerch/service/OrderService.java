
package com.shop.animerch.service;

import com.shop.animerch.exception.BadRequestException;
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
    public OrderDTO checkout() {
        Consumer consumer = getCurrentConsumer();

        log.debug(
                "Starting checkout for consumer id={}",
                consumer.getId()
        );

        Cart cart = cartRepository.findByConsumerIdForUpdate(consumer.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart for consumer id=%d not found".formatted(consumer.getId())
                ));

        List<CartItem> cartItems = cartItemRepository.findAllByCart(cart);

        if (cartItems.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        Map<Long, Product> products = validateAndLockProducts(cartItems);

        Order order = new Order();
        order.setConsumer(consumer);

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
    @Transactional(readOnly = true)
    public List<OrderDTO> getMyOrders() {
        Consumer consumer = getCurrentConsumer();

        List<Order> orders = orderRepository
                .findAllByConsumerIdOrderByCreatedAtDesc(consumer.getId());

        log.debug(
                "Fetched {} orders for consumer id={}",
                orders.size(),
                consumer.getId()
        );

        return orderMapper.toDTOList(orders);
    }
    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrders() {
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();

        log.debug("Fetched {} orders", orders.size());

        return orderMapper.toDTOList(orders);
    }
    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByConsumerId(Long consumerId) {
        if (!consumerRepository.existsById(consumerId)) {
            throw new ResourceNotFoundException(
                    "Consumer with id=%d not found".formatted(consumerId)
            );
        }

        List<Order> orders = orderRepository
                .findAllByConsumerIdOrderByCreatedAtDesc(consumerId);

        log.debug(
                "Fetched {} orders for consumer id={}",
                orders.size(),
                consumerId
        );

        return orderMapper.toDTOList(orders);
    }

    @Transactional
    public OrderItemDTO updateStatusOfOrderItem(Long orderItemId, Long orderId, OrderItemStatusUpdateDTO dto){
        OrderStatus nextStatus = dto.status();
        OrderItem orderItem = orderItemRepository.findByIdAndOrderIdForUpdate(orderItemId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Item with orderId=%d and orderItemId=%d".formatted(orderId,orderItemId)
                ));
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
