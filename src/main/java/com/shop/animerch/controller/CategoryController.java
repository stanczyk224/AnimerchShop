package com.shop.animerch.controller;

import com.shop.animerch.mapper.CategoryMapper;
import com.shop.animerch.model.Category;
import com.shop.animerch.record.CategoryCreateDTO;
import com.shop.animerch.record.CategoryDTO;
import com.shop.animerch.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/api/categories")
@RequiredArgsConstructor
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
    public CategoryDTO addCategory(
            @Valid @RequestBody CategoryCreateDTO dto
            ){
        return categoryService.addCategory(dto);
    }
    @DeleteMapping(path = "delete/{categoryId}")
    public void deleteCategory(@PathVariable("categoryId") Long id) {
        categoryService.deleteCategory(id);
    }
}
