package com.shop.animerch.service;

import com.shop.animerch.exception.ResourceNotFoundException;
import com.shop.animerch.mapper.SellerMapper;
import com.shop.animerch.model.Seller;
import com.shop.animerch.record.SellerCreateDTO;
import com.shop.animerch.record.SellerDTO;
import com.shop.animerch.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class SellerService {
    private final SellerRepository sellerRepository;

    private final SellerMapper sellerMapper;

    public List<Seller> getSeller() {
        log.debug("Fetching all sellers");
        List<Seller> sellers = sellerRepository.findAll();

        log.info("Fetched {} sellers", sellers.size());
        return sellers;
    }

    public SellerDTO getSellerById(Long id) {
        log.debug("Fetching seller with id={}", id);
        Seller seller = sellerRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Seller with id={} not found", id);
                    return new ResourceNotFoundException(
                            "Seller with id " + id + " not found"
                    );
                });

        log.debug("Seller with id={} found", id);
        return sellerMapper.toDTO(seller);
    }

    public SellerDTO addSeller(SellerCreateDTO dto) {
        log.debug("Creating seller");
        Seller seller = new Seller();
        seller.setName(dto.name());
        seller.setDescription(dto.description());
        seller.setPhone(dto.phone());
        seller.setAddress(dto.address());
        seller.setEmail(dto.email());

        Seller savedSeller = sellerRepository.save(seller);
        log.info("Seller created successfully, id={}", savedSeller.getId());

        return sellerMapper.toDTO(seller);
    }

    public void deleteSeller(Long id) {
        log.debug("Deleting seller with id={}", id);
        boolean find = sellerRepository.existsById(id);
        if (!find) {
            log.warn("Cannot delete seller: id={} not found", id);
            throw new ResourceNotFoundException(
                    "Seller with id " + id + " not found"
            );
        }
        sellerRepository.deleteById(id);
        log.info("Seller deleted successfully, id={}", id);
    }


}
