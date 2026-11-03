package com.shop.animerch.service;

import com.shop.animerch.exception.ConflictException;
import com.shop.animerch.exception.ResourceNotFoundException;
import com.shop.animerch.mapper.CategoryMapper;
import com.shop.animerch.model.Category;
import com.shop.animerch.record.CategoryCreateDTO;
import com.shop.animerch.record.CategoryDTO;
import com.shop.animerch.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.shop.animerch.utils.CategoryCsvReader;
import java.util.List;
import com.shop.animerch.record.ImportResultDTO;
import com.shop.animerch.record.ImportErrorDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final CategoryCsvReader categoryCsvReader;
    public List<CategoryDTO> getCategory() {
        log.debug("Fetching all categories");

        List<Category> categories = categoryRepository.findAll();

        log.info("Fetched {} categories", categories.size());

        return categoryMapper.toDTOList(categories);
    }

    public ImportResultDTO importCategories(MultipartFile file) throws IOException {

        List<String> names = categoryCsvReader.readCategories(file);

        List<Category> validCategories = new ArrayList<>();
        List<ImportErrorDTO> errors = new ArrayList<>();

        Set<String> seenNames = new HashSet<>();

        for (int i = 0; i < names.size(); i++) {

            String name = names.get(i);
            int rowNumber = i + 2;

            if (name == null || name.isBlank()) {
                errors.add(new ImportErrorDTO(rowNumber, "Name is empty"));
                continue;
            }

            String normalizedName = name.trim().toLowerCase(Locale.ROOT);

            if (!seenNames.add(normalizedName)) {
                errors.add(new ImportErrorDTO(rowNumber, "Duplicate in CSV"));
                continue;
            }

            if (categoryRepository.existsByNameIgnoreCase(name.trim())) {
                errors.add(new ImportErrorDTO(rowNumber, "Category already exists"));
                continue;
            }

            Category category = new Category();
            category.setName(name.trim());

            validCategories.add(category);
        }

        List<Category> savedCategories = categoryRepository.saveAll(validCategories);

        return new ImportResultDTO(
                names.size(),
                savedCategories.size(),
                errors.size(),
                errors
        );
    }

    public CategoryDTO getCategoryById(Long id) {
        log.debug("Fetching category with id={}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category with id " + id + " not found"
                ));
        log.debug("Category with id={} found", id);

        return categoryMapper.toDTO(category);
    }

    public CategoryDTO addCategory(CategoryCreateDTO dto) {
        log.debug("Creating category");
        String dtoNameTrimLow = dto.name().trim().toLowerCase();
        boolean isCategoryExists = categoryRepository.existsByName(dtoNameTrimLow);
        if (isCategoryExists) {
            throw new ConflictException(
                    "Category with name " +  dtoNameTrimLow + " already exists"
            );
        }
        Category category = new Category();
        category.setName(dtoNameTrimLow);

        Category savedCategory = categoryRepository.save(category);

        log.info("Category created successfully, id={}",
                savedCategory.getId());

        return categoryMapper.toDTO(savedCategory);
    }
    public void deleteCategory(Long id) {
        log.debug("Deleting category with id={}", id);

        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "Category with id " + id + " not found"
            );
        }

        categoryRepository.deleteById(id);

        log.info("Category deleted successfully, id={}", id);
    }


}
