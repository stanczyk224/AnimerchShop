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

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public List<CategoryDTO> getCategory() {
        log.debug("Fetching all categories");

        List<Category> categories = categoryRepository.findAll();

        log.info("Fetched {} categories", categories.size());

        return categoryMapper.toDTOList(categories);
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
        category.setName(dto.name());

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
