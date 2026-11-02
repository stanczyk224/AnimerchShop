package com.shop.animerch.controller;

import com.shop.animerch.mapper.CategoryMapper;
import com.shop.animerch.model.Category;
import com.shop.animerch.record.CategoryCreateDTO;
import com.shop.animerch.record.CategoryDTO;
import com.shop.animerch.service.CategoryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/api/categories")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class CategoryController {
    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;

    @GetMapping
    public List<CategoryDTO> getAllCategories() {
        return this.categoryService.getCategory();
    }

    @GetMapping("{categoryId}")
    public CategoryDTO getCategoryById(
            @PathVariable("categoryId") Long id
    ) {
        return categoryService.getCategoryById(id);
    }
    @PostMapping(path = "addCategory")
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryDTO addCategory(
            @Valid @RequestBody CategoryCreateDTO dto
            ){
        return categoryService.addCategory(dto);
    }
    @DeleteMapping(path = "delete/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteCategory(@PathVariable("categoryId") Long id) {
        categoryService.deleteCategory(id);
    }
}
