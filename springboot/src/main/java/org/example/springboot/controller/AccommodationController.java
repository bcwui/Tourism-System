package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.Accommodation;
import org.example.springboot.service.AccommodationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "住宿管理接口")
@RestController
@RequestMapping("/accommodation")
public class AccommodationController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AccommodationController.class);
    
    @Resource
    private AccommodationService accommodationService;
    
    @Operation(summary = "分页查询住宿列表")
    @GetMapping("/page")
    public Result<?> getAccommodationsByPage(
            @RequestParam(required = false) Integer scenicId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String minPrice,
            @RequestParam(required = false) String maxPrice,
            @RequestParam(required = false) String minRating,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        
        try {
            LOGGER.info("分页查询住宿列表，参数：scenicId={}, type={}, price={}~{}, rating>={}, page={}, size={}", 
                        scenicId, type, minPrice, maxPrice, minRating, currentPage, size);
            
            Page<Accommodation> page = accommodationService.getAccommodationsByPage(
                    scenicId, type, minPrice, maxPrice, minRating, currentPage, size);
            
            return Result.success(page);
        } catch (Exception e) {
            LOGGER.error("查询住宿列表失败", e);
            return Result.error("查询住宿列表失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "获取住宿详情")
    @GetMapping("/{id}")
    public Result<?> getAccommodationById(@PathVariable Integer id) {
        try {
            LOGGER.info("获取住宿详情，id={}", id);
            
            Accommodation accommodation = accommodationService.getAccommodationById(id);
            
            if (accommodation == null) {
                return Result.error("住宿信息不存在");
            }
            
            return Result.success(accommodation);
        } catch (Exception e) {
            LOGGER.error("获取住宿详情失败", e);
            return Result.error("获取住宿详情失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "添加住宿信息")
    @PostMapping
    public Result<?> addAccommodation(@RequestBody Accommodation accommodation) {
        try {
            LOGGER.info("添加住宿信息：{}", accommodation);
            
            boolean result = accommodationService.addAccommodation(accommodation);
            
            if (result) {
                return Result.success(accommodation);
            } else {
                return Result.error("添加住宿信息失败");
            }
        } catch (Exception e) {
            LOGGER.error("添加住宿信息失败", e);
            return Result.error("添加住宿信息失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "更新住宿信息")
    @PutMapping("/{id}")
    public Result<?> updateAccommodation(@PathVariable Long id, @RequestBody Accommodation accommodation) {
        try {
            LOGGER.info("更新住宿信息，id={}，数据：{}", id, accommodation);
            
            accommodation.setId(id);
            boolean result = accommodationService.updateAccommodation(accommodation);
            
            if (result) {
                return Result.success(accommodation);
            } else {
                return Result.error("更新住宿信息失败");
            }
        } catch (Exception e) {
            LOGGER.error("更新住宿信息失败", e);
            return Result.error("更新住宿信息失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "删除住宿信息")
    @DeleteMapping("/{id}")
    public Result<?> deleteAccommodation(@PathVariable Integer id) {
        try {
            LOGGER.info("删除住宿信息，id={}", id);
            
            boolean result = accommodationService.deleteAccommodation(id);
            
            if (result) {
                return Result.success();
            } else {
                return Result.error("删除住宿信息失败");
            }
        } catch (Exception e) {
            LOGGER.error("删除住宿信息失败", e);
            return Result.error("删除住宿信息失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "获取住宿类型列表")
    @GetMapping("/types")
    public Result<?> getAccommodationTypes() {
        try {
            List<String> types = accommodationService.getAccommodationTypes();
            return Result.success(types);
        } catch (Exception e) {
            LOGGER.error("获取住宿类型列表失败", e);
            return Result.error("获取住宿类型列表失败：" + e.getMessage());
        }
    }
} 