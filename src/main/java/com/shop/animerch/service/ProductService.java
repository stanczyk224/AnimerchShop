package com.shop.animerch.service;

import com.shop.animerch.exception.ResourceNotFoundException;
import com.shop.animerch.mapper.ProductMapper;
import com.shop.animerch.model.Product;
import com.shop.animerch.model.Seller;
import com.shop.animerch.record.ProductCreateDTO;
import com.shop.animerch.record.ProductDTO;
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

    public List<Product> getProduct() {
        log.debug("Fetching all products");
        List<Product> products = this.productRepository.findAll();
        log.info("Fetched {} products", products.size());
        return  products;
    }

    public ProductDTO getProductById(Long id) {
        log.debug("Fetching product with id={}", id);
        Product product = productRepository.findById(id)
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

        Product product = new Product();
        product.setName(dto.name());
        product.setPrice(dto.price());
        product.setDescription(dto.description());
        product.setSeller(seller);

        Product savedProduct = productRepository.save(product);
        log.debug("Product successfully created with id={}", savedProduct.getId());

        return productMapper.toDTO(product);
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
