package com.shop.animerch.service;

import com.shop.animerch.model.Seller;
import com.shop.animerch.model.SellerPhoto;
import com.shop.animerch.exception.ConflictException;
import com.shop.animerch.exception.ResourceNotFoundException;
import com.shop.animerch.record.SellerPhotoDTO;
import com.shop.animerch.repository.SellerPhotoRepository;
import com.shop.animerch.repository.SellerRepository;
import com.shop.animerch.service.storage.StorageService;
import com.shop.animerch.service.storage.StorageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerPhotoService {

    private static final int MAX_PHOTOS = 1;

    private final SellerPhotoRepository sellerPhotoRepository;
    private final SellerRepository sellerRepository;
    private final StorageService storageService;

    @Transactional
    public SellerPhotoDTO uploadPhoto(Long sellerId, MultipartFile file)
            throws IOException {

        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Seller not found"));

        if (sellerPhotoRepository.countBySellerId(sellerId) >= MAX_PHOTOS) {
            throw new ConflictException(
                    "Seller already has a photo; delete it before uploading another"
            );
        }

        String filename = storageService.store(file, StorageType.SELLERS);

        SellerPhoto photo = SellerPhoto.builder()
                .url(filename)
                .seller(seller)
                .build();

        SellerPhoto savedPhoto;

        try {
            savedPhoto = sellerPhotoRepository.saveAndFlush(photo);
        } catch (RuntimeException exception) {
            try {
                storageService.delete(filename, StorageType.SELLERS);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
                log.error("Failed to clean up uploaded file: {}",
                        filename, cleanupException);
            }
            throw exception;
        }

        return toDTO(savedPhoto);
    }

    @Transactional(readOnly = true)
    public List<SellerPhotoDTO> getPhotos(Long sellerId) {

        if (!sellerRepository.existsById(sellerId)) {
            throw new ResourceNotFoundException("Seller not found");
        }

        return sellerPhotoRepository.findAllBySellerId(sellerId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public Resource getPhotoContent(Long sellerId, Long photoId)
            throws IOException {

        SellerPhoto photo = sellerPhotoRepository
                .findByIdAndSellerId(photoId, sellerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Seller photo not found"));

        return storageService.loadAsResource(
                photo.getUrl(),
                StorageType.SELLERS
        );
    }

    @Transactional
    public void deletePhoto(Long sellerId, Long photoId) {

        SellerPhoto photo = sellerPhotoRepository
                .findByIdAndSellerId(photoId, sellerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Seller photo not found"));

        String filename = photo.getUrl();

        sellerPhotoRepository.delete(photo);

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            storageService.delete(filename, StorageType.SELLERS);
                        } catch (IOException exception) {
                            log.error("Failed to delete seller photo file: {}",
                                    filename, exception);
                        }
                    }
                }
        );
    }

    private SellerPhotoDTO toDTO(SellerPhoto photo) {
        return new SellerPhotoDTO(
                photo.getId(),
                "/api/sellers/" + photo.getSeller().getId()
                        + "/photos/" + photo.getId() + "/content"
        );
    }
}
