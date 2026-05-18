package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.AccommodationReview;
import org.example.springboot.service.AccommodationReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@Tag(name = "住宿评价接口")
@RestController
@RequestMapping("/accommodation/review")
public class AccommodationReviewController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AccommodationReviewController.class);
    
    @Resource
    private AccommodationReviewService reviewService;
    
    @Operation(summary = "分页查询住宿评价")
    @GetMapping("/page")
    public Result<?> getReviewsByPage(
            @RequestParam Integer accommodationId,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        
        try {
            LOGGER.info("分页查询住宿评价，参数：accommodationId={}, page={}, size={}",
                    accommodationId, currentPage, size);
            
            Page<AccommodationReview> page = reviewService.getReviewsByPage(
                    accommodationId, currentPage, size);
            
            return Result.success(page);
        } catch (Exception e) {
            LOGGER.error("查询住宿评价失败", e);
            return Result.error("查询住宿评价失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "添加住宿评价")
    @PostMapping
    public Result<?> addReview(@RequestBody AccommodationReview review) {
        try {
            LOGGER.info("添加住宿评价：{}", review);
            
            boolean result = reviewService.addReview(review);
            
            if (result) {
                return Result.success(review);
            } else {
                return Result.error("添加住宿评价失败");
            }
        } catch (Exception e) {
            LOGGER.error("添加住宿评价失败", e);
            return Result.error("添加住宿评价失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "删除住宿评价")
    @DeleteMapping("/{id}")
    public Result<?> deleteReview(@PathVariable Integer id) {
        try {
            LOGGER.info("删除住宿评价，id={}", id);
            
            boolean result = reviewService.deleteReview(id);
            
            if (result) {
                return Result.success();
            } else {
                return Result.error("删除住宿评价失败，可能无权限或评价不存在");
            }
        } catch (Exception e) {
            LOGGER.error("删除住宿评价失败", e);
            return Result.error("删除住宿评价失败：" + e.getMessage());
        }
    }
} 