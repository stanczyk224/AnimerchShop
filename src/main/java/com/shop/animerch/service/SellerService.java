package com.shop.animerch.service;

import com.shop.animerch.exception.BadRequestException;
import com.shop.animerch.exception.ResourceNotFoundException;
import com.shop.animerch.mapper.SellerMapper;
import com.shop.animerch.model.Product;
import com.shop.animerch.model.ProductPhoto;
import com.shop.animerch.model.Seller;
import com.shop.animerch.model.SellerPhoto;
import com.shop.animerch.record.*;
import com.shop.animerch.repository.ProductPhotoRepository;
import com.shop.animerch.repository.SellerPhotoRepository;
import com.shop.animerch.repository.SellerRepository;
import com.shop.animerch.service.storage.StorageService;
import com.shop.animerch.service.storage.StorageType;
import com.shop.animerch.specification.SellerSpecification;
import com.shop.animerch.utils.SellerCsvReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.apache.commons.csv.CSVRecord;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
public class SellerService {
    private final SellerRepository sellerRepository;
    private final SellerCsvReader sellerCsvReader;
    private final SellerMapper sellerMapper;
    private final ProductPhotoRepository productPhotoRepository;
    private final SellerPhotoRepository sellerPhotoRepository;
    private final StorageService storageService;
    private static final int MAX_PAGE_SIZE = 20;
    public ImportResultDTO importSellers(MultipartFile file) throws IOException {

        List<CSVRecord> records = sellerCsvReader.readSellers(file);

        List<Seller> validSellers = new ArrayList<>();
        List<ImportErrorDTO> errors = new ArrayList<>();

        Set<String> seenNames = new HashSet<>();

        for (CSVRecord record : records) {

            int rowNumber = (int) record.getRecordNumber() + 1;

            String name = record.get("name");
            String description = record.get("description");
            String status = record.get("status");
            String phone = record.get("phone");
            String email = record.get("email");
            String address = record.get("address");

            if (name == null || name.isBlank()) {
                errors.add(new ImportErrorDTO(rowNumber, "Name is empty"));
                continue;
            }

            String normalizedName = name.trim().toLowerCase(Locale.ROOT);

            if (!seenNames.add(normalizedName)) {
                errors.add(new ImportErrorDTO(rowNumber, "Duplicate in CSV"));
                continue;
            }

            if (sellerRepository.existsByNameIgnoreCase(name.trim())) {
                errors.add(new ImportErrorDTO(rowNumber, "Seller already exists"));
                continue;
            }

            Seller seller = new Seller();

            seller.setName(name.trim());
            seller.setDescription(description);
            seller.setStatus(status);
            seller.setPhone(phone);
            seller.setEmail(email);
            seller.setAddress(address);

            validSellers.add(seller);
        }

        List<Seller> savedSellers = sellerRepository.saveAll(validSellers);

        return new ImportResultDTO(
                records.size(),
                savedSellers.size(),
                errors.size(),
                errors
        );
    }

    public List<Seller> getSeller() {
        log.debug("Fetching all sellers");
        List<Seller> sellers = sellerRepository.findAllWithProducts();

        log.info("Fetched {} sellers", sellers.size());
        return sellers;
    }

    public Page<SellerSummaryDTO> searchSellers(
            String search,
            Pageable pageable
    ) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new BadRequestException(
                    "Page size cannot exceed " + MAX_PAGE_SIZE
            );
        }

        Specification<Seller> spec =
                Specification.where(SellerSpecification.hasName(search));

        Page<Seller> sellers = sellerRepository.findAll(spec, pageable);

        return sellers.map(sellerMapper::toSummaryDTO);
    }

    public SellerDTO getSellerById(Long id) {
        log.debug("Fetching seller with id={}", id);
        Seller seller = sellerRepository.findByIdWithProducts(id)
                .orElseThrow(() -> {
                    log.warn("Seller with id={} not found", id);
                    return new ResourceNotFoundException(
                            "Seller with id " + id + " not found"
                    );
                });

        log.debug("Seller with id={} found", id);
        return sellerMapper.toDTO(seller);
    }

    public SellerDTO addSeller(SellerCreateDTO dto) {
        log.debug("Creating seller");
        Seller seller = new Seller();
        seller.setName(dto.name());
        seller.setDescription(dto.description());
        seller.setPhone(dto.phone());
        seller.setAddress(dto.address());
        seller.setEmail(dto.email());

        Seller savedSeller = sellerRepository.save(seller);
        log.info("Seller created successfully, id={}", savedSeller.getId());

        return sellerMapper.toDTO(savedSeller);
    }
    @Transactional
    public void deleteSeller(Long id) {
        log.debug("Deleting seller with id={}", id);

        Seller seller = sellerRepository.findByIdWithProducts(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Seller with id " + id + " not found"
                ));

        List<Long> productIds = seller.getProducts().stream()
                .map(Product::getId)
                .toList();

        List<ProductPhoto> productPhotos = productIds.isEmpty()
                ? List.of()
                : productPhotoRepository.findAllByProductIdIn(productIds);
        SellerPhoto sellerPhoto = sellerPhotoRepository.findBySellerId(id)
                .orElse(null);

        List<String> productFilenames = productPhotos.stream()
                .map(ProductPhoto::getUrl)
                .toList();

        String sellerFilename = sellerPhoto != null
                ? sellerPhoto.getUrl()
                : null;

        productPhotoRepository.deleteAll(productPhotos);

        if (sellerPhoto != null) {
            sellerPhotoRepository.delete(sellerPhoto);
        }

        sellerRepository.delete(seller);

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        for (String filename : productFilenames) {
                            try {
                                storageService.delete(filename, StorageType.PRODUCTS);
                            } catch (IOException exception) {
                                log.error(
                                        "Failed to delete product photo: {}",
                                        filename,
                                        exception
                                );
                            }
                        }
                        if (sellerFilename != null) {
                            try {
                                storageService.delete(
                                        sellerFilename,
                                        StorageType.SELLERS
                                );
                            } catch (IOException exception) {
                                log.error(
                                        "Failed to delete seller photo: {}",
                                        sellerFilename,
                                        exception
                                );
                            }
                        }
                    }
                }
        );

        log.info("Seller deleted successfully, id={}", id);
    }


}
