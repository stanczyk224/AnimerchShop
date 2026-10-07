package com.shop.animerch.service;

import com.shop.animerch.mapper.SellerMapper;
import com.shop.animerch.model.Product;
import com.shop.animerch.model.Seller;
import com.shop.animerch.record.SellerCreateDTO;
import com.shop.animerch.record.SellerDTO;
import com.shop.animerch.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class SellerService {
    private final SellerRepository sellerRepository;

    private final SellerMapper sellerMapper;

    public List<Seller> getSeller() {
        return this.sellerRepository.findAll();
    }

    public SellerDTO getSellerById(Long id) {
        Seller seller = sellerRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Seller is not found!"));
        return sellerMapper.toDTO(seller);
    }

    public SellerDTO addSeller(SellerCreateDTO dto) {
        Seller seller = new Seller();
        seller.setName(dto.name());
        seller.setDescription(dto.description());
        seller.setPhone(dto.phone());
        seller.setAddress(dto.address());
        seller.setEmail(dto.email());

        sellerRepository.save(seller);
        return sellerMapper.toDTO(seller);
    }

    public void deleteSeller(Long id) {
        boolean find = sellerRepository.existsById(id);
        if (!find) {
            throw new IllegalStateException("seller not found!");
        }
        sellerRepository.deleteById(id);
    }


}
