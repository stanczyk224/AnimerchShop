package com.shop.animerch.utils;

import com.shop.animerch.exception.BadRequestException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class SellerCsvReader {

    public List<CSVRecord> readSellers(MultipartFile file) throws IOException {

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get();

        List<CSVRecord> sellers = new ArrayList<>();

        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                file.getInputStream(),
                                StandardCharsets.UTF_8
                        )
                );
                CSVParser parser = format.parse(reader)
        ) {

            List<String> requiredHeaders = List.of(
                    "name",
                    "description",
                    "status",
                    "phone",
                    "email",
                    "address"
            );

            for (String header : requiredHeaders) {
                if (!parser.getHeaderMap().containsKey(header)) {
                    throw new BadRequestException(
                            "CSV must contain '" + header + "' header"
                    );
                }
            }

            for (CSVRecord record : parser) {
                sellers.add(record);
            }
        }

        return sellers;
    }
}