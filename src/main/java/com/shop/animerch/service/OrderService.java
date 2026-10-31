package com.shop.animerch.service;

import com.shop.animerch.exception.BadRequestException;
import com.shop.animerch.exception.ResourceNotFoundException;
import com.shop.animerch.mapper.OrderMapper;
import com.shop.animerch.model.Consumer;
import com.shop.animerch.model.Order;
import com.shop.animerch.model.OrderItem;
import com.shop.animerch.model.Product;
import com.shop.animerch.record.OrderCreateDTO;
import com.shop.animerch.record.OrderDTO;
import com.shop.animerch.record.OrderItemCreateDTO;
import com.shop.animerch.repository.ConsumerRepository;
import com.shop.animerch.repository.OrderRepository;
import com.shop.animerch.repository.ProductRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final ConsumerRepository consumerRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;

    private Map<Long, Product> validateDuplicateProducts(List<OrderItemCreateDTO> items) {

        Map<Long, Integer> productQuantities = new HashMap<>();

        for (OrderItemCreateDTO item : items) {
            productQuantities.merge(
                    item.productId(),
                    item.quantity(),
                    Integer::sum
            );
        }

        Map<Long, Product> products = new HashMap<>();

        for (Long productId : productQuantities.keySet().stream().sorted().toList()) {

            Product product = productRepository.findByIdForUpdate(productId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Product not found with id " + productId
                            )
                    );

            Integer requestedQuantity = productQuantities.get(productId);

            if (requestedQuantity > product.getQuantity()) {
                throw new BadRequestException(
                        "Insufficient stock for product with id " + productId
                                + ". Available: " + product.getQuantity()
                                + ", requested: " + requestedQuantity
                );
            }

            products.put(product.getId(), product);
        }

        return products;
    }


    private Consumer findConsumerById(Long id) {
        log.debug("Fetching consumer with id={}", id);

        Consumer consumer = consumerRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Consumer with id={} not found", id);
                    return new ResourceNotFoundException(
                            "Consumer with id " + id + " not found"
                    );
                });

        log.debug("Consumer with id={} found", id);
        return consumer;
    }
    private OrderItem createOrderItem(
            OrderItemCreateDTO dto,
            Map<Long, Product> products
    ) {
        Product product = products.get(dto.productId());

        product.setQuantity(product.getQuantity() - dto.quantity());

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setProductName(product.getName());
        item.setQuantity(dto.quantity());
        item.setUnitPrice(product.getPrice());

        return item;
    }

    @Transactional
    public OrderDTO createOrder(OrderCreateDTO dto) {
        log.debug("Starting order creation for consumerId={}", dto.consumerId());

        Consumer consumer = findConsumerById(dto.consumerId());

        Map<Long, Product> products = validateDuplicateProducts(dto.items());

        Order order = new Order();
        order.setConsumer(consumer);

        dto.items().forEach(itemDTO -> {
            OrderItem item = createOrderItem(itemDTO, products);
            order.addItem(item);
        });

        order.recalculateTotalPrice();

        log.debug("Calculated order total: {}", order.getTotalPrice());

        Order savedOrder = orderRepository.save(order);

        log.info(
                "Order created successfully: orderId={}, consumerId={}, totalPrice={}, itemsCount={}",
                savedOrder.getId(),
                consumer.getId(),
                savedOrder.getTotalPrice(),
                savedOrder.getOrderItems().size()
        );

        return orderMapper.toDTO(savedOrder);
    }


}
