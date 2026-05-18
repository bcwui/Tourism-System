package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.ScenicCategory;
import org.example.springboot.service.ScenicCategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@Tag(name = "景点分类接口")
@RestController
@RequestMapping("/scenic-category")
public class ScenicCategoryController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ScenicCategoryController.class);
    
    @Resource
    private ScenicCategoryService scenicCategoryService;
    
    @Operation(summary = "分页查询分类")
    @GetMapping("/page")
    public Result<?> getCategoriesByPage(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(scenicCategoryService.getCategoriesByPage(name, currentPage, size));
    }
    
    @Operation(summary = "获取分类树")
    @GetMapping("/tree")
    public Result<?> getCategoryTree() {
        return Result.success(scenicCategoryService.getCategoryTree());
    }
    
    @Operation(summary = "获取分类详情")
    @GetMapping("/{id}")
    public Result<?> getCategoryById(@PathVariable Long id) {
        return Result.success(scenicCategoryService.getCategoryById(id));
    }
    
    @Operation(summary = "添加分类")
    @PostMapping
    public Result<?> addCategory(@RequestBody ScenicCategory category) {
        if (scenicCategoryService.addCategory(category)) {
            return Result.success();
        } else {
            return Result.error("添加分类失败");
        }
    }
    
    @Operation(summary = "更新分类")
    @PutMapping("/{id}")
    public Result<?> updateCategory(@PathVariable Long id, @RequestBody ScenicCategory category) {
        category.setId(id);
        if (scenicCategoryService.updateCategory(category)) {
            return Result.success();
        } else {
            return Result.error("更新分类失败");
        }
    }
    
    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public Result<?> deleteCategory(@PathVariable Long id) {
        if (scenicCategoryService.deleteCategory(id)) {
            return Result.success();
        } else {
            return Result.error("删除失败，请确保该分类下没有子分类");
        }
    }
} 