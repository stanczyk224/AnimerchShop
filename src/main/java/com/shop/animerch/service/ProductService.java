
package com.shop.animerch.service;

import com.shop.animerch.exception.BadRequestException;
import com.shop.animerch.exception.ResourceNotFoundException;
import com.shop.animerch.mapper.ProductMapper;
import com.shop.animerch.model.Category;
import com.shop.animerch.model.Product;
import com.shop.animerch.model.ProductPhoto;
import com.shop.animerch.model.Seller;
import com.shop.animerch.record.*;
import com.shop.animerch.repository.CategoryRepository;
import com.shop.animerch.repository.ProductPhotoRepository;
import com.shop.animerch.repository.ProductRepository;
import com.shop.animerch.repository.SellerRepository;
import com.shop.animerch.repository.ReviewRepository;
import com.shop.animerch.service.storage.StorageService;
import com.shop.animerch.service.storage.StorageType;
import com.shop.animerch.utils.ProductCsvReader;
import com.shop.animerch.specification.ProductSpecification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final SellerRepository sellerRepository;
    private final ProductPhotoRepository productPhotoRepository;
    private final StorageService storageService;
    private final ProductCsvReader productCsvReader;
    private final ProductMapper productMapper;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;

    private static final int MAX_PAGE_SIZE = 20;

    // =========================
    // CSV IMPORT
    // =========================

    public ImportResultDTO importProducts(MultipartFile file) throws IOException {

        List<CSVRecord> records = productCsvReader.readProducts(file);

        List<Product> validProducts = new ArrayList<>();
        List<ImportErrorDTO> errors = new ArrayList<>();

        Set<String> seenNames = new HashSet<>();

        for (CSVRecord record : records) {

            int rowNumber = (int) record.getRecordNumber() + 1;

            String name = record.get("name");
            String description = record.get("description");
            String model = record.get("model");
            String weight = record.get("weight");
            String quantityValue = record.get("quantity");
            String priceValue = record.get("price");
            String sellerName = record.get("seller");
            String categoriesValue = record.get("categories");

            // name
            if (name == null || name.isBlank()) {
                errors.add(new ImportErrorDTO(rowNumber, "Name is empty"));
                continue;
            }

            // duplicate in database
            if (productRepository.existsByNameIgnoreCase(name.trim())) {
                errors.add(new ImportErrorDTO(
                        rowNumber,
                        "Product already exists"
                ));
                continue;
            }

            String normalizedName = name.trim().toLowerCase(Locale.ROOT);

            // duplicate in CSV
            if (!seenNames.add(normalizedName)) {
                errors.add(new ImportErrorDTO(rowNumber, "Duplicate in CSV"));
                continue;
            }

            // description
            if (description == null || description.isBlank()) {
                errors.add(new ImportErrorDTO(rowNumber, "Description is empty"));
                continue;
            }

            // quantity
            int quantity;

            try {
                quantity = Integer.parseInt(quantityValue);
            } catch (NumberFormatException e) {
                errors.add(new ImportErrorDTO(rowNumber, "Invalid quantity"));
                continue;
            }

            if (quantity < 0) {
                errors.add(new ImportErrorDTO(
                        rowNumber,
                        "Quantity cannot be negative"
                ));
                continue;
            }

            // price
            BigDecimal price;

            try {
                price = new BigDecimal(priceValue);
            } catch (NumberFormatException e) {
                errors.add(new ImportErrorDTO(rowNumber, "Invalid price"));
                continue;
            }

            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                errors.add(new ImportErrorDTO(
                        rowNumber,
                        "Price must be greater than zero"
                ));
                continue;
            }

            // seller
            if (sellerName == null || sellerName.isBlank()) {
                errors.add(new ImportErrorDTO(rowNumber, "Seller is empty"));
                continue;
            }

            Optional<Seller> sellerOptional =
                    sellerRepository.findByNameIgnoreCase(sellerName.trim());

            if (sellerOptional.isEmpty()) {
                errors.add(new ImportErrorDTO(
                        rowNumber,
                        "Seller not found: " + sellerName
                ));
                continue;
            }

            // categories
            if (categoriesValue == null || categoriesValue.isBlank()) {
                errors.add(new ImportErrorDTO(
                        rowNumber,
                        "Categories are empty"
                ));
                continue;
            }

            List<Category> categories = new ArrayList<>();

            String[] categoryNames = categoriesValue.split(",");

            boolean categoriesValid = true;

            for (String categoryName : categoryNames) {

                String trimmedCategoryName = categoryName.trim();

                if (trimmedCategoryName.isEmpty()) {
                    continue;
                }

                Optional<Category> categoryOptional =
                        categoryRepository.findByNameIgnoreCase(
                                trimmedCategoryName
                        );

                if (categoryOptional.isEmpty()) {
                    errors.add(new ImportErrorDTO(
                            rowNumber,
                            "Category not found: " + trimmedCategoryName
                    ));

                    categoriesValid = false;
                    break;
                }

                categories.add(categoryOptional.get());
            }

            if (!categoriesValid) {
                continue;
            }

            // create product
            Product product = new Product();

            product.setName(name.trim());
            product.setDescription(description.trim());
            product.setModel(model);
            product.setWeight(weight);
            product.setQuantity(quantity);
            product.setPrice(price);
            product.setSeller(sellerOptional.get());
            product.setCategories(categories);

            validProducts.add(product);
        }

        List<Product> savedProducts =
                productRepository.saveAll(validProducts);

        return new ImportResultDTO(
                records.size(),
                savedProducts.size(),
                errors.size(),
                errors
        );
    }

    // =========================
    // FIND HELPERS
    // =========================

    private List<Category> getCategoriesById(List<Long> ids) {
        log.debug("Fetching categories with ids={}", ids);

        return categoryRepository.findAllById(ids);
    }

    private Seller findSellerById(Long id) {
        return sellerRepository.findById(id)
                .orElseThrow(() -> {

                    log.warn("Seller with id={} not found", id);

                    return new ResourceNotFoundException(
                            "Seller with id " + id + " not found"
                    );
                });
    }

    private Product findProductById(Long id) {
        log.debug("Fetching product with id={}", id);

        Product product = productRepository.findByIdWithCategories(id)
                .orElseThrow(() -> {

                    log.warn("Product with id={} not found", id);

                    return new ResourceNotFoundException(
                            "Product with id " + id + " not found"
                    );
                });

        log.debug("Product with id={} found", id);

        return product;
    }

    // =========================
    // GET PRODUCTS
    // =========================
    @Transactional(readOnly = true)
    public List<ProductDTO> getProduct() {
        List<Product> products = productRepository.findAllWithCategories();

        loadCategories(products);

        return toProductDTOs(products);
    }

    // Paginated products
    @Transactional(readOnly = true)
    public Page<ProductDTO> getProduct(Pageable pageable) {

        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new BadRequestException(
                    "Page size cannot exceed " + MAX_PAGE_SIZE
            );
        }

        Page<Product> products = productRepository.findAll(pageable);

        return loadDetailedPage(products);
    }

    // Get product by ID
    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {

        Product product = findProductById(id);

        return toProductDTOs(List.of(product)).getFirst();
    }

    // =========================
    // SEARCH AND FILTER
    // =========================
    @Transactional(readOnly = true)
    public Page<ProductDTO> searchProducts(
            String search,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable
    ) {

        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new BadRequestException(
                    "Page size cannot exceed " + MAX_PAGE_SIZE
            );
        }

        Specification<Product> spec = Specification
                .where(ProductSpecification.hasName(search))
                .and(ProductSpecification.hasCategoryId(categoryId))
                .and(ProductSpecification.hasMinPrice(minPrice))
                .and(ProductSpecification.hasMaxPrice(maxPrice));

        Page<Product> products = productRepository.findAll(spec, pageable);

        return loadDetailedPage(products);
    }

    // =========================
    // TWO-PHASE PAGINATION
    // =========================
    private void loadCategories(List<Product> products) {
        if (products.isEmpty()) {
            return;
        }

        List<Long> ids = products.stream()
                .map(Product::getId)
                .toList();

        productRepository.findAllWithCategoriesByIdIn(ids);
    }
    private Page<ProductDTO> loadDetailedPage(Page<Product> page) {

        List<Long> ids = page.getContent()
                .stream()
                .map(Product::getId)
                .toList();

        if (ids.isEmpty()) {
            return new PageImpl<>(
                    List.of(),
                    page.getPageable(),
                    page.getTotalElements()
            );
        }

        // First query: photos and seller
        List<Product> detailedProducts =
                productRepository.findAllWithPhotosByIdIn(ids);

        // Second query: categories
        productRepository.findAllWithCategoriesByIdIn(ids);

        Map<Long, Product> productsById = detailedProducts.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        product -> product
                ));

        List<Product> orderedProducts = ids.stream()
                .map(productsById::get)
                .filter(Objects::nonNull)
                .toList();

        List<ProductDTO> content = toProductDTOs(orderedProducts);

        return new PageImpl<>(
                content,
                page.getPageable(),
                page.getTotalElements()
        );
    }
    // =========================
    // CREATE PRODUCT
    // =========================

    @Transactional
    public ProductDTO addProduct(ProductCreateDTO dto) {

        log.debug("Creating product");
        log.debug("Fetching seller with id={}", dto.sellerId());

        Seller seller = findSellerById(dto.sellerId());

        List<Category> categories =
                categoryRepository.findAllById(dto.categoryIds());

        if (categories.size() != dto.categoryIds().size()) {

            for (Long categoryId : dto.categoryIds()) {

                boolean exists = categories.stream()
                        .anyMatch(category ->
                                category.getId().equals(categoryId)
                        );

                if (!exists) {
                    throw new ResourceNotFoundException(
                            "Category with id " + categoryId + " not found"
                    );
                }
            }
        }

        Product product = new Product();

        product.setName(dto.name());
        product.setDescription(dto.description());
        product.setModel(dto.model());
        product.setWeight(dto.weight());
        product.setQuantity(dto.quantity());
        product.setPrice(dto.price());
        product.setSeller(seller);
        product.setCategories(categories);

        Product savedProduct = productRepository.save(product);

        log.debug(
                "Product successfully created with id={}",
                savedProduct.getId()
        );

        return productMapper.toDTO(savedProduct,
                new ProductRatingSummary(savedProduct.getId(), null, 0));
    }

    // =========================
    // DELETE PRODUCT
    // =========================

    @Transactional
    public void deleteProduct(Long id) {

        log.debug("Deleting product with id={}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product with id " + id + " not found"
                ));

        List<ProductPhoto> photos =
                productPhotoRepository.findAllByProductId(id);

        List<String> filenames = photos.stream()
                .map(ProductPhoto::getUrl)
                .toList();

        productPhotoRepository.deleteAll(photos);

        productRepository.delete(product);

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {

                        for (String filename : filenames) {
                            try {
                                storageService.delete(
                                        filename,
                                        StorageType.PRODUCTS
                                );
                            } catch (IOException exception) {
                                log.error(
                                        "Failed to delete product photo: {}",
                                        filename,
                                        exception
                                );
                            }
                        }
                    }
                }
        );

        log.info("Product deleted successfully, id={}", id);
    }

    // =========================
    // UPDATE PRODUCT
    // =========================

    @Transactional
    public ProductDTO editProduct(ProductUpdateDTO dto, Long id) {

        Product product = findProductById(id);

        productMapper.updateProductFromDto(dto, product);

        if (dto.categoryIds() != null) {

            List<Long> categoryIds = dto.categoryIds();

            if (new HashSet<>(categoryIds).size() != categoryIds.size()) {
                throw new BadRequestException(
                        "Duplicate category IDs are not allowed"
                );
            }

            List<Category> categories =
                    categoryRepository.findAllById(categoryIds);

            if (categories.size() != categoryIds.size()) {

                Set<Long> foundIds = categories.stream()
                        .map(Category::getId)
                        .collect(Collectors.toSet());

                List<Long> missingIds = categoryIds.stream()
                        .filter(categoryId ->
                                !foundIds.contains(categoryId)
                        )
                        .toList();

                throw new ResourceNotFoundException(
                        "Categories not found: " + missingIds
                );
            }

            product.setCategories(categories);
        }

        return toProductDTOs(List.of(product)).getFirst();
    }

    private List<ProductDTO> toProductDTOs(List<Product> products) {
        if (products.isEmpty()) {
            return List.of();
        }
        // One aggregate query for the entire list, rather than one per product.
        List<Long> ids = products.stream().map(Product::getId).toList();
        Map<Long, ProductRatingSummary> summaries = reviewRepository.summarizeByProductIds(ids)
                .stream().collect(Collectors.toMap(ProductRatingSummary::productId, summary -> summary));
        return products.stream()
                .map(product -> productMapper.toDTO(product, summaries.getOrDefault(product.getId(),
                        new ProductRatingSummary(product.getId(), null, 0))))
                .toList();
    }
}
