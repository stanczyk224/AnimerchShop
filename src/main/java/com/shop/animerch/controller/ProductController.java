package com.shop.animerch.controller;

import com.shop.animerch.record.ProductCreateDTO;
import com.shop.animerch.record.ProductDTO;
import com.shop.animerch.record.ProductUpdateDTO;
import com.shop.animerch.service.ProductService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;
import com.shop.animerch.record.ImportResultDTO;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import org.springdoc.core.annotations.ParameterObject;
@RestController
@RequestMapping(path = "/api/products")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public Page<ProductDTO> getAllProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @ParameterObject Pageable pageable
    ) {
        return productService.searchProducts(
                search,
                categoryId,
                minPrice,
                maxPrice,
                pageable
        );
    }
    @PostMapping(
            value = "/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ImportResultDTO importProducts(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        return productService.importProducts(file);
    }

    @GetMapping("{productId}")
    public ProductDTO getProductById(
            @PathVariable("productId") Long id
    ) {
        return productService.getProductById(id);
    }

    @PostMapping(path = "addProduct")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductDTO addProduct(
            @Valid @RequestBody ProductCreateDTO dto) {
        return productService.addProduct(dto);
    }

    @DeleteMapping(path = "delete/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteProduct(@PathVariable("productId") Long id) {
        productService.deleteProduct(id);
    }

    @PatchMapping(path = "{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductDTO editProductById(
            @Valid @RequestBody ProductUpdateDTO dto,
            @PathVariable("productId") Long id
    ) {
        return productService.editProduct(dto, id);
    }
}
