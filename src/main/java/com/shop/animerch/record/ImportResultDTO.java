package com.shop.animerch.record;

import java.util.List;

public record ImportResultDTO(
        int totalRows,
        int imported,
        int rejected,
        List<ImportErrorDTO> errors
) {
}