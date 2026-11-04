package com.shop.animerch.controller;

import com.shop.animerch.record.ProductPhotoDTO;
import com.shop.animerch.service.ProductPhotoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaTypeFactory;
@RestController
@RequestMapping("/api/products/{productId}/photos")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
@Tag(name = "Product Photos", description = "Product photo management")
public class ProductPhotoController {

    private final ProductPhotoService productPhotoService;

    @GetMapping("/{photoId}/content")
    @Operation(summary = "Get product photo content")
    public ResponseEntity<Resource> getPhotoContent(
            @PathVariable Long productId,
            @PathVariable Long photoId
    ) throws IOException {

        Resource resource = productPhotoService.getPhotoContent(productId, photoId);

        return ResponseEntity.ok()
                .contentType(
                        MediaTypeFactory.getMediaType(resource)
                                .orElse(MediaType.APPLICATION_OCTET_STREAM)
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + resource.getFilename() + "\""
                )
                .body(resource);
    }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a photo for a product")
    public ResponseEntity<ProductPhotoDTO> uploadPhoto(
            @PathVariable Long productId,
            @RequestPart("file") @NotNull MultipartFile file
    ) throws IOException {
        ProductPhotoDTO photo = productPhotoService.uploadPhoto(productId, file);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(photo);
    }

    @GetMapping
    @Operation(summary = "Get all photos of a product")
    public ResponseEntity<List<ProductPhotoDTO>> getPhotos(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(
                productPhotoService.getPhotos(productId)
        );
    }

    @DeleteMapping("/{photoId}")
    @Operation(summary = "Delete a product photo")
    public ResponseEntity<Void> deletePhoto(
            @PathVariable Long productId,
            @PathVariable Long photoId
    ) {
        productPhotoService.deletePhoto(productId, photoId);

        return ResponseEntity.noContent().build();
    }
}
