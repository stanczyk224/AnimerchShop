package com.shop.animerch.service;

import com.shop.animerch.mapper.ProductMapper;
import com.shop.animerch.model.Product;
import com.shop.animerch.record.ProductRatingSummary;
import com.shop.animerch.repository.*;
import com.shop.animerch.service.storage.StorageService;
import com.shop.animerch.utils.ProductCsvReader;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.mockito.Mockito.*;

class ProductRatingBatchTests {
    @Test
    void pageLoadsSummariesOnceAndMapsThemInPageOrder() {
        ProductRepository repository = mock(ProductRepository.class);
        ReviewRepository reviews = mock(ReviewRepository.class);
        ProductMapper mapper = mock(ProductMapper.class);
        ProductService service = new ProductService(repository, mock(SellerRepository.class),
                mock(ProductPhotoRepository.class), mock(StorageService.class), mock(ProductCsvReader.class),
                mapper, mock(CategoryRepository.class), reviews);
        Product first = new Product();
        first.setId(10L);
        Product second = new Product();
        second.setId(20L);
        var pageable = PageRequest.of(0, 20);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(first, second), pageable, 2));
        when(repository.findAllWithPhotosByIdIn(List.of(10L, 20L))).thenReturn(List.of(second, first));
        var summary = new ProductRatingSummary(10L, 4.5, 2);
        when(reviews.summarizeByProductIds(List.of(10L, 20L))).thenReturn(List.of(summary));

        service.getProduct(pageable);

        verify(reviews).summarizeByProductIds(List.of(10L, 20L));
        verifyNoMoreInteractions(reviews);
        var order = inOrder(mapper);
        order.verify(mapper).toDTO(first, summary);
        order.verify(mapper).toDTO(second, new ProductRatingSummary(20L, null, 0));
    }
}
