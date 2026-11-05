package com.shop.animerch.controller;

import com.shop.animerch.record.SellerCreateDTO;
import com.shop.animerch.record.SellerDTO;
import com.shop.animerch.record.SellerSummaryDTO;
import com.shop.animerch.service.SellerService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.shop.animerch.record.ImportResultDTO;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/sellers")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
public class SellerController {
    private final SellerService sellerService;
    @PostMapping(
            value = "/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ImportResultDTO importSellers(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        return sellerService.importSellers(file);
    }
    @GetMapping
    public Page<SellerSummaryDTO> getAllSellers(
            @RequestParam(required = false) String search,
            @ParameterObject Pageable pageable
    ) {
        return sellerService.searchSellers(
                search,
                pageable
        );
    }
    @GetMapping("{SellerId}")
    public SellerDTO getSellerById(
            @PathVariable("SellerId") Long id
    ) {
        return sellerService.getSellerById(id);
    }
    @PostMapping(path = "addSeller")
    @PreAuthorize("hasRole('ADMIN')")
    public SellerDTO addSeller(@Valid @RequestBody SellerCreateDTO dto){
        return sellerService.addSeller(dto);
    }
    @DeleteMapping(path = "delete/{sellerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteSeller(@PathVariable("sellerId") Long id){
        sellerService.deleteSeller(id);
    }

}

