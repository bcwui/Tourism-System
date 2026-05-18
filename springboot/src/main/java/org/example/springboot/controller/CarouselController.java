package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.Carousel;
import org.example.springboot.service.CarouselService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "轮播图管理接口")
@RestController
@RequestMapping("/carousel")
public class CarouselController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarouselController.class);
    
    @Resource
    private CarouselService carouselService;
    
    @Operation(summary = "获取前台启用的轮播图列表")
    @GetMapping("/active")
    public Result<?> getActiveCarousels() {
        List<Carousel> carousels = carouselService.getActiveCarousels();
        return Result.success(carousels);
    }
    
    @Operation(summary = "分页查询轮播图")
    @GetMapping("/page")
    public Result<?> getCarouselsByPage(
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(carouselService.getCarouselsByPage(currentPage, size));
    }
    
    @Operation(summary = "根据ID获取轮播图")
    @GetMapping("/{id}")
    public Result<?> getCarouselById(@PathVariable Integer id) {
        Carousel carousel = carouselService.getCarouselById(id);
        if (carousel == null) {
            return Result.error("轮播图不存在");
        }
        return Result.success(carousel);
    }
    
    @Operation(summary = "添加轮播图")
    @PostMapping
    public Result<?> addCarousel(@RequestBody Carousel carousel) {
        // 设置默认状态为启用
        if (carousel.getStatus() == null) {
            carousel.setStatus(1);
        }
        boolean success = carouselService.addCarousel(carousel);
        return success ? Result.success() : Result.error("添加轮播图失败");
    }
    
    @Operation(summary = "更新轮播图")
    @PutMapping
    public Result<?> updateCarousel(@RequestBody Carousel carousel) {
        if (carousel.getId() == null) {
            return Result.error("轮播图ID不能为空");
        }
        boolean success = carouselService.updateCarousel(carousel);
        return success ? Result.success() : Result.error("更新轮播图失败");
    }
    
    @Operation(summary = "删除轮播图")
    @DeleteMapping("/{id}")
    public Result<?> deleteCarousel(@PathVariable Integer id) {
        boolean success = carouselService.deleteCarousel(id);
        return success ? Result.success() : Result.error("删除轮播图失败");
    }
    
    @Operation(summary = "切换轮播图状态")
    @PutMapping("/status/{id}")
    public Result<?> updateStatus(
            @PathVariable Integer id,
            @RequestParam Integer status) {
        if (status != 0 && status != 1) {
            return Result.error("状态值只能为0或1");
        }
        boolean success = carouselService.updateStatus(id, status);
        return success ? Result.success() : Result.error("更新状态失败");
    }
} 