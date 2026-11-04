package com.shop.animerch.controller;

import com.shop.animerch.record.SellerPhotoDTO;
import com.shop.animerch.service.SellerPhotoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/sellers/{sellerId}/photos")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
@Tag(name = "Seller Photos", description = "Seller photo management")
public class SellerPhotoController {

    private final SellerPhotoService sellerPhotoService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a photo for a seller")
    public ResponseEntity<SellerPhotoDTO> uploadPhoto(
            @PathVariable Long sellerId,
            @RequestPart("file") MultipartFile file
    ) throws IOException {

        SellerPhotoDTO photo = sellerPhotoService.uploadPhoto(sellerId, file);

        return ResponseEntity.status(HttpStatus.CREATED).body(photo);
    }

    @GetMapping
    @Operation(summary = "Get all photos of a seller")
    public ResponseEntity<List<SellerPhotoDTO>> getPhotos(
            @PathVariable Long sellerId
    ) {
        return ResponseEntity.ok(sellerPhotoService.getPhotos(sellerId));
    }

    @GetMapping("/{photoId}/content")
    @Operation(summary = "Get seller photo content")
    public ResponseEntity<Resource> getPhotoContent(
            @PathVariable Long sellerId,
            @PathVariable Long photoId
    ) throws IOException {

        Resource resource = sellerPhotoService.getPhotoContent(sellerId, photoId);

        MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(resource.getFilename())
                                .build()
                                .toString()
                )
                .body(resource);
    }

    @DeleteMapping("/{photoId}")
    @Operation(summary = "Delete a seller photo")
    public ResponseEntity<Void> deletePhoto(
            @PathVariable Long sellerId,
            @PathVariable Long photoId
    ) {
        sellerPhotoService.deletePhoto(sellerId, photoId);

        return ResponseEntity.noContent().build();
    }
}
