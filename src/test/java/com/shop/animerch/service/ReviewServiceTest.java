package com.shop.animerch.service;

import com.shop.animerch.exception.ConflictException;
import com.shop.animerch.exception.ResourceNotFoundException;
import com.shop.animerch.model.Consumer;
import com.shop.animerch.model.Product;
import com.shop.animerch.model.Review;
import com.shop.animerch.record.ReviewCreateDTO;
import com.shop.animerch.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {
    @Mock ReviewRepository reviews;
    @Mock ConsumerRepository consumers;
    @Mock ProductRepository products;
    @Mock OrderRepository orders;
    @InjectMocks ReviewService service;

    private Consumer consumer;
    private Product product;
    private final ReviewCreateDTO request = new ReviewCreateDTO(5, " Great figure! ");

    @BeforeEach
    void setUp() {
        consumer = new Consumer();
        consumer.setId(7L);
        consumer.setName("Customer");
        product = new Product();
        product.setId(12L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("customer@example.com", null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void customerAndProductExist() {
        when(consumers.findByUserEmailForUpdate("customer@example.com")).thenReturn(Optional.of(consumer));
        when(products.findById(12L)).thenReturn(Optional.of(product));
    }

    @Test
    void purchaserCanCreateReviewUsingAuthenticatedIdentity() {
        customerAndProductExist();
        when(orders.existsByConsumerIdAndOrderItemsProductId(7L, 12L)).thenReturn(true);
        when(reviews.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            assertSame(consumer, review.getConsumer());
            review.setId(1L);
            review.setCreatedAt(LocalDateTime.now());
            return review;
        });
        var result = service.create(12L, request);
        assertEquals(1L, result.id());
        assertEquals(12L, result.productId());
        assertEquals(5, result.rating());
        assertEquals("Great figure!", result.comment());
        assertNotNull(result.createdAt());
        assertNull(result.editedAt());
        assertFalse(result.edited());
    }

    @Test
    void customerWhoDidNotOrderProductCannotReview() {
        customerAndProductExist();
        assertThrows(AccessDeniedException.class, () -> service.create(12L, request));
        verify(reviews, never()).save(any());
    }

    @Test
    void repeatPurchaseDoesNotAllowDuplicateReview() {
        customerAndProductExist();
        when(orders.existsByConsumerIdAndOrderItemsProductId(7L, 12L)).thenReturn(true);
        when(reviews.existsByConsumerIdAndProductId(7L, 12L)).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.create(12L, request));
        verify(reviews, never()).save(any());
    }

    @Test
    void unauthenticatedCallerCannotCreateReview() {
        SecurityContextHolder.clearContext();
        assertThrows(AccessDeniedException.class, () -> service.create(12L, request));
        verifyNoInteractions(consumers, products, orders, reviews);
    }

    @Test
    void accountWithoutCustomerProfileCannotReview() {
        assertThrows(AccessDeniedException.class, () -> service.create(12L, request));
        verifyNoInteractions(orders, reviews);
    }

    @Test
    void missingProductCannotBeReviewed() {
        when(consumers.findByUserEmailForUpdate("customer@example.com")).thenReturn(Optional.of(consumer));
        assertThrows(ResourceNotFoundException.class, () -> service.create(12L, request));
        verifyNoInteractions(orders, reviews);
    }

    @Test
    void listingReturnsPaginatedReviewsWithoutPrivateCustomerDetails() {
        var pageable = PageRequest.of(0, 20);
        Review review = new Review();
        review.setConsumer(consumer);
        review.setProduct(product);
        review.setRating(4);
        review.setComment("Nice");
        when(products.existsById(12L)).thenReturn(true);
        when(reviews.findAllByProductId(12L, pageable)).thenReturn(new PageImpl<>(List.of(review)));
        var result = service.getByProduct(12L, pageable);
        assertEquals(1, result.getTotalElements());
        assertEquals("Customer", result.getContent().getFirst().consumerName());
    }

    @Test
    void listingMissingProductReturnsNotFound() {
        assertThrows(ResourceNotFoundException.class,
                () -> service.getByProduct(12L, PageRequest.of(0, 20)));
        verifyNoInteractions(reviews);
    }

    private Review existingReview() {
        Review review = new Review();
        review.setId(1L);
        review.setConsumer(consumer);
        review.setProduct(product);
        review.setRating(4);
        review.setComment("Nice");
        review.setCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        when(consumers.findByUserEmailForUpdate("customer@example.com")).thenReturn(Optional.of(consumer));
        when(reviews.findById(1L)).thenReturn(Optional.of(review));
        return review;
    }

    @Test
    void authorCanEditAndCreationDateIsPreserved() {
        Review review = existingReview();
        var result = service.update(12L, 1L, request);
        assertEquals(5, result.rating());
        assertEquals("Great figure!", result.comment());
        assertEquals(review.getCreatedAt(), result.createdAt());
        assertNotNull(result.editedAt());
        assertTrue(result.edited());
        assertEquals(result.editedAt(), review.getEditedAt());
    }

    @Test
    void unchangedContentDoesNotMarkReviewEdited() {
        existingReview();
        var result = service.update(12L, 1L, new ReviewCreateDTO(4, " Nice "));
        assertFalse(result.edited());
        assertNull(result.editedAt());
    }

    @Test
    void subsequentEditUpdatesLastEditDate() {
        Review review = existingReview();
        LocalDateTime previous = LocalDateTime.of(2026, 2, 1, 12, 0);
        review.setEditedAt(previous);
        var result = service.update(12L, 1L, request);
        assertTrue(result.editedAt().isAfter(previous));
        assertTrue(result.edited());
    }

    @Test
    void authorCanDeleteReview() {
        Review review = existingReview();
        service.delete(12L, 1L);
        verify(reviews).delete(review);
    }

    @Test
    void anotherCustomerCannotEditOrDeleteReview() {
        Review review = existingReview();
        Consumer other = new Consumer();
        other.setId(99L);
        review.setConsumer(other);
        assertThrows(AccessDeniedException.class, () -> service.update(12L, 1L, request));
        assertThrows(AccessDeniedException.class, () -> service.delete(12L, 1L));
        assertEquals(4, review.getRating());
        assertNull(review.getEditedAt());
        verify(reviews, never()).delete(any(Review.class));
    }

    @Test
    void wrongProductCannotBeUsedToEditOrDeleteReview() {
        existingReview();
        assertThrows(ResourceNotFoundException.class, () -> service.update(99L, 1L, request));
        assertThrows(ResourceNotFoundException.class, () -> service.delete(99L, 1L));
        verify(reviews, never()).delete(any(Review.class));
    }

    @Test
    void missingReviewCannotBeEditedOrDeleted() {
        when(consumers.findByUserEmailForUpdate("customer@example.com")).thenReturn(Optional.of(consumer));
        assertThrows(ResourceNotFoundException.class, () -> service.update(12L, 1L, request));
        assertThrows(ResourceNotFoundException.class, () -> service.delete(12L, 1L));
    }

    @Test
    void unauthenticatedCallerCannotEditOrDelete() {
        SecurityContextHolder.clearContext();
        assertThrows(AccessDeniedException.class, () -> service.update(12L, 1L, request));
        assertThrows(AccessDeniedException.class, () -> service.delete(12L, 1L));
        verifyNoInteractions(reviews);
    }
}
