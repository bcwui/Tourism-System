package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.ScenicCollection;
import org.example.springboot.service.ScenicCollectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "景点收藏接口")
@RestController
@RequestMapping("/scenic-collection")
public class ScenicCollectionController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScenicCollectionController.class);
    
    @Resource
    private ScenicCollectionService scenicCollectionService;

    @Operation(summary = "添加景点收藏")
    @PostMapping("/{scenicId}")
    public Result<?> addCollection(@PathVariable Long scenicId) {
        scenicCollectionService.addCollection(scenicId);
        return Result.success("收藏成功");
    }

    @Operation(summary = "取消景点收藏")
    @DeleteMapping("/{scenicId}")
    public Result<?> cancelCollection(@PathVariable Long scenicId) {
        scenicCollectionService.cancelCollection(scenicId);
        return Result.success("取消收藏成功");
    }

    @Operation(summary = "查询用户是否已收藏某景点")
    @GetMapping("/is-collected/{scenicId}")
    public Result<?> isCollected(@PathVariable Long scenicId) {
        boolean collected = scenicCollectionService.isCollected(scenicId);
        return Result.success(collected);
    }

    @Operation(summary = "批量查询用户是否已收藏景点")
    @PostMapping("/batch-is-collected")
    public Result<?> batchIsCollected(@RequestBody List<Long> scenicIds) {
        Map<Long, Boolean> result = scenicCollectionService.batchIsCollected(scenicIds);
        return Result.success(result);
    }

    @Operation(summary = "查询用户收藏的景点列表")
    @GetMapping("/user")
    public Result<?> getUserCollections(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<ScenicCollection> page = scenicCollectionService.getUserCollections(userId, currentPage, size);
        return Result.success(page);
    }

    @Operation(summary = "查询用户收藏的景点ID列表")
    @GetMapping("/user/ids")
    public Result<?> getUserCollectionIds(@RequestParam(required = false) Long userId) {
        List<Long> ids = scenicCollectionService.getUserCollectionIds(userId);
        return Result.success(ids);
    }
} 