package com.shop.animerch.controller;

import com.shop.animerch.mapper.SellerMapper;
import com.shop.animerch.record.SellerCreateDTO;
import com.shop.animerch.record.SellerDTO;
import com.shop.animerch.service.SellerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/sellers")
@RequiredArgsConstructor
public class SellerController {
    private final SellerService sellerService;
    private final SellerMapper sellerMapper;

    @GetMapping
    public List<SellerDTO> getAllSellers() {
        return this.sellerMapper.toDTOList(sellerService.getSeller());
    }
    @GetMapping("{SellerId}")
    public SellerDTO getSellerById(
            @PathVariable("SellerId") Long id
    ) {
        return sellerService.getSellerById(id);
    }
    @PostMapping(path = "addSeller")
    public SellerDTO addSeller(@Valid @RequestBody SellerCreateDTO dto){
        return sellerService.addSeller(dto);
    }
    @DeleteMapping(path = "delete/{sellerId}")
    public void deleteSeller(@PathVariable("sellerId") Long id){
        sellerService.deleteSeller(id);
    }

}

