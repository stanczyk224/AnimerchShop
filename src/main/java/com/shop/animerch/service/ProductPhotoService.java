package com.shop.animerch.service;

import com.shop.animerch.model.Product;
import com.shop.animerch.model.ProductPhoto;
import com.shop.animerch.exception.ConflictException;
import com.shop.animerch.exception.ResourceNotFoundException;
import com.shop.animerch.record.ProductPhotoDTO;
import com.shop.animerch.repository.ProductPhotoRepository;
import com.shop.animerch.repository.ProductRepository;
import com.shop.animerch.service.storage.StorageService;
import com.shop.animerch.service.storage.StorageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductPhotoService {

    private static final int MAX_PHOTOS = 5;

    private final ProductPhotoRepository productPhotoRepository;
    private final ProductRepository productRepository;
    private final StorageService storageService;

    @Transactional(rollbackFor = Exception.class)
    public ProductPhotoDTO uploadPhoto(Long productId, MultipartFile file)
            throws IOException {

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found"));

        if (productPhotoRepository.countByProductId(productId) >= MAX_PHOTOS) {
            throw new ConflictException("Product cannot have more than 5 photos");
        }

        String filename = storageService.store(file, StorageType.PRODUCTS);

        try {
            ProductPhoto photo = ProductPhoto.builder()
                    .url(filename)
                    .product(product)
                    .build();

            ProductPhoto savedPhoto = productPhotoRepository.saveAndFlush(photo);

            return toDTO(savedPhoto);

        } catch (RuntimeException exception) {
            try {
                storageService.delete(filename, StorageType.PRODUCTS);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
                log.error("Failed to clean up uploaded file: {}", filename,
                        cleanupException);
            }

            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<ProductPhotoDTO> getPhotos(Long productId) {

        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found");
        }

        return productPhotoRepository.findAllByProductId(productId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public void deletePhoto(Long productId, Long photoId) {

        ProductPhoto photo = productPhotoRepository
                .findByIdAndProductId(photoId, productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product photo not found"));

        productPhotoRepository.delete(photo);

        // Удаляем физический файл только после успешного коммита транзакции.
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            storageService.delete(
                                    photo.getUrl(),
                                    StorageType.PRODUCTS
                            );
                        } catch (IOException exception) {
                            log.error("Failed to delete photo file: {}",
                                    photo.getUrl(), exception);
                        }
                    }
                }
        );
    }

    @Transactional(readOnly = true)
    public Resource getPhotoContent(Long productId, Long photoId) throws IOException {

        ProductPhoto photo = productPhotoRepository
                .findByIdAndProductId(photoId, productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product photo not found"));

        return storageService.loadAsResource(
                photo.getUrl(),
                StorageType.PRODUCTS
        );
    }

    private ProductPhotoDTO toDTO(ProductPhoto photo) {
        return new ProductPhotoDTO(
                photo.getId(),
                "/api/products/" + photo.getProduct().getId()
                        + "/photos/" + photo.getId() + "/content"
        );
    }
}