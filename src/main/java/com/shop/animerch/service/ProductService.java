package com.shop.animerch.service;

import com.shop.animerch.mapper.ProductMapper;
import com.shop.animerch.model.Product;
import com.shop.animerch.model.Seller;
import com.shop.animerch.record.ProductCreateDTO;
import com.shop.animerch.record.ProductDTO;
import com.shop.animerch.repository.ProductRepository;
import com.shop.animerch.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    private final SellerRepository sellerRepository;

    private final ProductMapper productMapper;

    public List<Product> getProduct() {
        return this.productRepository.findAll();
    }

    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Product is not found!"));

        return productMapper.toDTO(product);
    }

    public ProductDTO addProduct(ProductCreateDTO dto) {
        Seller seller = sellerRepository.findById(dto.sellerId())
                .orElseThrow(() -> new IllegalStateException("Seller not found"));

        Product product = new Product();
        product.setName(dto.name());
        product.setPrice(dto.price());
        product.setDescription(dto.description());
        product.setSeller(seller);

        productRepository.save(product);

        return productMapper.toDTO(product);
    }

    public void deleteProduct(Long id) {
        boolean find = productRepository.existsById(id);
        if (!find) {
            throw new IllegalStateException("product not found!");
        }
        productRepository.deleteById(id);
    }

}
