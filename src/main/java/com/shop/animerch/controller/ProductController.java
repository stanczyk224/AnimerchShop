package com.shop.animerch.controller;

import com.shop.animerch.mapper.ProductMapper;
import com.shop.animerch.record.ProductCreateDTO;
import com.shop.animerch.record.ProductDTO;
import com.shop.animerch.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final ProductMapper productMapper;

    @GetMapping
    public List<ProductDTO> getAllProducts() {
        return this.productMapper.toDTOList(productService.getProduct());
    }

    @GetMapping("{productId}")
    public ProductDTO getProductById(
            @PathVariable("productId") Long id
    ) {
        return productService.getProductById(id);
    }

    @PostMapping(path = "addProduct")
    public ProductDTO addProduct(
            @Valid @RequestBody ProductCreateDTO dto) {
        return productService.addProduct(dto);
    }

    @DeleteMapping(path = "delete/{productId}")
    public void deleteProduct(@PathVariable("productId") Long id) {
        productService.deleteProduct(id);
    }
}
