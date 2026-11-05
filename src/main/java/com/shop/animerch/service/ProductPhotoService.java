package com.shop.animerch.service;

import com.shop.animerch.model.Product;
import com.shop.animerch.model.ProductPhoto;
import com.shop.animerch.exception.ConflictException;
import com.shop.animerch.exception.ResourceNotFoundException;
import com.shop.animerch.record.ProductPhotoDTO;
import com.shop.animerch.repository.ProductPhotoRepository;
import com.shop.animerch.repository.ProductRepository;
import com.shop.animerch.service.storage.ImageValidationService;
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
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductPhotoService {

    private static final int MAX_PHOTOS = 5;

    private final ProductPhotoRepository productPhotoRepository;
    private final ProductRepository productRepository;
    private final StorageService storageService;
    private final ImageValidationService imageValidationService;

    private Product findProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found"));
    }

    private long countProductPhotos(Long productId) {
        return productPhotoRepository.countByProductId(productId);
    }

    private boolean checkTheLimitOfImporting(MultipartFile[] files) {
        return files.length <= MAX_PHOTOS;
    }

    private boolean checkTheLimitOfUploadingProductPhotos(Long productId, MultipartFile[] files) {
        if(!checkTheLimitOfImporting(files)){
            return false;
        }
        long numberOfUploadingPhotos = files.length;
        long numberOfProductPhotos = countProductPhotos(productId);
        return numberOfUploadingPhotos + numberOfProductPhotos <= MAX_PHOTOS;
    }
    private boolean checkTheLimitOfUploadingProductPhoto(Long productId) {
        long numberOfProductPhotos = countProductPhotos(productId);
        return 1 + numberOfProductPhotos <= MAX_PHOTOS;
    }

    private void registerFileCleanup(List<String> uploadedFilenames) {

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCompletion(int status) {

                        if (status == STATUS_COMMITTED) {
                            return;
                        }

                        for (String filename : uploadedFilenames) {
                            try {
                                storageService.delete(
                                        filename,
                                        StorageType.PRODUCTS
                                );

                            } catch (IOException exception) {
                                log.error(
                                        "Failed to clean up uploaded file: {}",
                                        filename,
                                        exception
                                );
                            }
                        }
                    }
                }
        );
    }


    private ProductPhotoDTO saveProductPhoto(Product product, String filename) {
        ProductPhoto photo = ProductPhoto.builder()
                .url(filename)
                .product(product)
                .build();

        ProductPhoto savedPhoto = productPhotoRepository.saveAndFlush(photo);

        return toDTO(savedPhoto);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProductPhotoDTO uploadPhoto(Long productId, MultipartFile file)
            throws IOException {

        Product product = findProductById(productId);

        if (!checkTheLimitOfUploadingProductPhoto(productId)) {
            throw new ConflictException(
                    "Product cannot have more than 5 photos"
            );
        }

        imageValidationService.validate(file);

        List<String> uploadedFilenames = new ArrayList<>();

        registerFileCleanup(uploadedFilenames);

        String filename = storageService.store(
                file, StorageType.PRODUCTS
        );
        uploadedFilenames.add(filename);

        return saveProductPhoto(product, filename);
    }
    @Transactional(rollbackFor = Exception.class)
    public List<ProductPhotoDTO> importPhotos(
            Long productId,
            MultipartFile[] files
    ) throws IOException {

        Product product = findProductById(productId);

        if (!checkTheLimitOfUploadingProductPhotos(productId, files)) {
            throw new ConflictException(
                    "Product cannot have more than 5 photos"
            );
        }

        // Сначала валидируем абсолютно все файлы
        for (MultipartFile file : files) {
            imageValidationService.validate(file);
        }

        List<String> uploadedFilenames = new ArrayList<>();
        List<ProductPhotoDTO> productPhotoDTOS = new ArrayList<>();

        registerFileCleanup(uploadedFilenames);

        for (MultipartFile file : files) {

            String filename = storageService.store(
                    file,
                    StorageType.PRODUCTS
            );

            uploadedFilenames.add(filename);

            ProductPhotoDTO savedDTO = saveProductPhoto(
                    product,
                    filename
            );

            productPhotoDTOS.add(savedDTO);
        }

        return productPhotoDTOS;
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