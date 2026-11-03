
package com.shop.animerch.utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import com.shop.animerch.exception.BadRequestException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class CategoryCsvReader {

    public List<String> readCategories(MultipartFile file) throws IOException {

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get();

        List<String> categories = new ArrayList<>();

        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                file.getInputStream(),
                                StandardCharsets.UTF_8
                        )
                );
                CSVParser parser = format.parse(reader)
        ) {

            if (!parser.getHeaderMap().containsKey("name")) {
                throw new BadRequestException(
                        "CSV must contain 'name' header"
                );
            }

            for (CSVRecord record : parser) {
                String name = record.get("name");
                categories.add(name);
            }
        }

        return categories;
    }
}
