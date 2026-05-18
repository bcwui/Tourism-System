package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.ScenicSpot;
import org.example.springboot.service.ScenicSpotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "景点管理接口")
@RestController
@RequestMapping("/scenic")
public class ScenicSpotController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScenicSpotController.class);
    @Resource
    private ScenicSpotService scenicSpotService;

    @Operation(summary = "分页查询景点")
    @GetMapping("/page")
    public Result<?> getScenicSpotsByPage(
            @RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "") String location,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<ScenicSpot> page = scenicSpotService.getScenicSpotsByPage(name, location,  categoryId, currentPage, size);
        return Result.success(page);
    }

    @Operation(summary = "获取景点详情")
    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        ScenicSpot spot = scenicSpotService.getById(id);
        return Result.success(spot);
    }

    @Operation(summary = "根据分类ID获取景点列表")
    @GetMapping("/category/{categoryId}")
    public Result<?> getScenicSpotsByCategoryId(@PathVariable Long categoryId) {
        List<ScenicSpot> spots = scenicSpotService.getScenicSpotsByCategoryId(categoryId);
        return Result.success(spots);
    }

    @Operation(summary = "新增景点")
    @PostMapping("/add")
    public Result<?> createScenicSpot(@RequestBody ScenicSpot spot) {
        scenicSpotService.createScenicSpot(spot);
        return Result.success("新增成功");
    }

    @Operation(summary = "更新景点")
    @PutMapping("/{id}")
    public Result<?> updateScenicSpot(@PathVariable Long id, @RequestBody ScenicSpot spot) {
        scenicSpotService.updateScenicSpot(id, spot);
        return Result.success("更新成功");
    }

    @Operation(summary = "删除景点")
    @DeleteMapping("/delete/{id}")
    public Result<?> deleteScenicSpot(@PathVariable Long id) {
        scenicSpotService.deleteScenicSpot(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "获取所有景点")
    @GetMapping("/all")
    public Result<?> getAll() {
        List<ScenicSpot> list = scenicSpotService.getAll();
        return Result.success(list);
    }

    // 获取热门景点
    @Operation(summary = "获取热门景点")
    @GetMapping("/hot")
    public Result<?> getHotScenics(
            @RequestParam(required = false, defaultValue = "4") Integer limit) {
        List<ScenicSpot> hotScenics = scenicSpotService.getHotScenics(limit);
        return Result.success(hotScenics);
    }

    @Operation(summary = "获取搜索建议")
    @GetMapping("/suggestions")
    public Result<?> getSearchSuggestions(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "5") Integer limit) {
        List<Map<String, Object>> suggestions = scenicSpotService.getSearchSuggestions(keyword, limit);
        return Result.success(suggestions);
    }

    @Operation(summary = "通过地址获取经纬度")
    @GetMapping("/geocode")
    public Result<?> geocodeAddress(@RequestParam String address) {
        try {
            // 这里只返回基本的数据结构，实际的地理编码由前端通过高德API完成
            // 这样做可以避免在后端配置高德API密钥，简化实现
            return Result.success(Map.of(
                "address", address,
                "status", "success",
                "message", "请使用前端高德API进行地理编码"
            ));
        } catch (Exception e) {
            return Result.error("地理编码请求失败: " + e.getMessage());
        }
    }
} 