package com.shop.animerch.specification;

import com.shop.animerch.model.Seller;
import org.springframework.data.jpa.domain.Specification;

public class SellerSpecification {
    public static Specification<Seller> hasName(String search) {
        return (root, query, criteriaBuilder) -> {
            if (search == null || search.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")),
                    "%" + search.trim().toLowerCase() + "%"
            );
        };
    }
}
