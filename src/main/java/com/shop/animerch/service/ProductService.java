package com.shop.animerch.service;

import com.shop.animerch.exception.BadRequestException;
import com.shop.animerch.exception.ResourceNotFoundException;
import com.shop.animerch.mapper.ProductMapper;
import com.shop.animerch.model.Category;
import com.shop.animerch.model.Product;
import com.shop.animerch.model.Seller;
import com.shop.animerch.record.ProductCreateDTO;
import com.shop.animerch.record.ProductDTO;
import com.shop.animerch.repository.CategoryRepository;
import com.shop.animerch.repository.ProductRepository;
import com.shop.animerch.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final SellerRepository sellerRepository;

    private final ProductMapper productMapper;
    private final CategoryRepository categoryRepository;

    public List<Product> getProduct() {
        log.debug("Fetching all products");
        List<Product> products = this.productRepository.findAllWithCategories();;
        log.info("Fetched {} products", products.size());
        return  products;
    }

    public ProductDTO getProductById(Long id) {
        log.debug("Fetching product with id={}", id);
        Product product = productRepository.findByIdWithCategories(id)
                .orElseThrow(() -> {

                    log.warn("Product with id={} not found", id);
                    return new ResourceNotFoundException(
                            "Product with id " + id + " not found"
                    );
                });
        log.debug("Product with id={} found", id);
        return productMapper.toDTO(product);
    }

    public ProductDTO addProduct(ProductCreateDTO dto) {
        log.debug("Creating product");
        log.debug("Fetching seller with id={}", dto.sellerId());
        Seller seller = sellerRepository.findById(dto.sellerId())
                .orElseThrow(() -> {

                    log.warn("Seller with id={} not found", dto.sellerId());
                    return new ResourceNotFoundException(
                            "Seller with id " + dto.sellerId() + " not found"
                    );
                });
        List<Category> categories = categoryRepository.findAllById(dto.categoryIds());
        if (categories.size() != dto.categoryIds().size()) {
            for (Long categoryId : dto.categoryIds()) {
                boolean exists = categories.stream()
                        .anyMatch(category -> category.getId().equals(categoryId));

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
        log.debug("Product successfully created with id={}", savedProduct.getId());

        return productMapper.toDTO(savedProduct);
    }

    public void deleteProduct(Long id) {
        log.debug("Deleting product with id={}", id);
        boolean find = productRepository.existsById(id);
        if (!find) {
            log.warn("Cannot delete product with id={}", id);
            throw new ResourceNotFoundException(
                    "Product with id " + id + " not found"
            );
        }
        productRepository.deleteById(id);
        log.info("Product deleted successfully, id={}", id);
    }

}
