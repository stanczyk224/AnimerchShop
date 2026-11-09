package com.shop.animerch;

import com.shop.animerch.mapper.OrderMapper;
import com.shop.animerch.model.Order;
import com.shop.animerch.model.OrderItem;
import com.shop.animerch.model.OrderStatus;
import com.shop.animerch.model.Product;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderTotalsTests {
    private final OrderMapper mapper = Mappers.getMapper(OrderMapper.class);

    private OrderItem addItem(Order order, String price, int quantity, OrderStatus status) {
        OrderItem item = new OrderItem();
        item.setUnitPrice(new BigDecimal(price));
        item.setQuantity(quantity);
        item.setStatus(status);
        Product product = new Product();
        product.setPrice(new BigDecimal("999.99"));
        item.setProduct(product);
        order.addItem(item);
        return item;
    }

    private void assertTotals(Order order, String original, String cancelled, String active) {
        var dto = mapper.toDTO(order);
        assertEquals(0, new BigDecimal(original).compareTo(dto.originalTotal()));
        assertEquals(0, new BigDecimal(cancelled).compareTo(dto.cancelledTotal()));
        assertEquals(0, new BigDecimal(active).compareTo(dto.activeTotal()));
        assertEquals(order.getTotalPrice(), dto.totalPrice());
    }

    @Test
    void totalsUseSavedPricesAndTrackPartialThenFullCancellation() {
        Order order = new Order();
        OrderItem first = addItem(order, "15.00", 2, OrderStatus.PLACED);
        OrderItem second = addItem(order, "70.00", 1, OrderStatus.PLACED);
        order.recalculateTotalPrice();
        assertTotals(order, "100.00", "0.00", "100.00");
        first.setStatus(OrderStatus.CANCELLED);
        assertTotals(order, "100.00", "30.00", "70.00");
        second.setStatus(OrderStatus.CANCELLED);
        assertTotals(order, "100.00", "100.00", "0.00");
        assertEquals(new BigDecimal("100.00"), order.getTotalPrice());
    }

    @Test
    void shippedAndDeliveredItemsRemainActiveWithExactDecimalAmounts() {
        Order order = new Order();
        addItem(order, "0.10", 3, OrderStatus.SHIPPED);
        addItem(order, "0.20", 2, OrderStatus.DELIVERED);
        addItem(order, "0.01", 3, OrderStatus.CANCELLED);
        order.recalculateTotalPrice();
        assertTotals(order, "0.73", "0.03", "0.70");
    }

    @Test
    void emptyOrderHasZeroTotals() {
        Order order = new Order();
        order.recalculateTotalPrice();
        assertTotals(order, "0", "0", "0");
    }
}
