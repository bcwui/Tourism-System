package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.TravelGuide;
import org.example.springboot.service.TravelGuideService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "旅游攻略接口")
@RestController
@RequestMapping("/guide")
public class TravelGuideController {
    @Resource
    private TravelGuideService travelGuideService;

    @Operation(summary = "分页查询攻略")
    @GetMapping("/page")
    public Result<?> getGuidesByPage(
        @RequestParam(defaultValue = "") String title,
        @RequestParam(required = false) Long userId,
        @RequestParam(defaultValue = "1") Integer currentPage,
        @RequestParam(defaultValue = "10") Integer size) {
        Page<TravelGuide> page = travelGuideService.getGuidesByPage(title, userId, currentPage, size);
        return Result.success(page);
    }

    @Operation(summary = "查看攻略详情")
    @GetMapping({"/{id}", "/detail/{id}"})
    public Result<?> getGuideById(@PathVariable Long id) {
        travelGuideService.addView(id); // 增加浏览量
        return Result.success(travelGuideService.getById(id));
    }

    @Operation(summary = "获取相关攻略")
    @GetMapping("/related")
    public Result<?> getRelatedGuides(
            @RequestParam Long guideId,
            @RequestParam(defaultValue = "3") Integer limit) {
        List<Map<String, Object>> related = travelGuideService.getRelatedGuides(guideId, limit);
        return Result.success(related);
    }

    @Operation(summary = "新增攻略")
    @PostMapping("/add")
    public Result<?> addGuide(@RequestBody TravelGuide guide) {
        travelGuideService.addGuide(guide);
        return Result.success();
    }

    @Operation(summary = "修改攻略")
    @PutMapping("/update")
    public Result<?> updateGuide(@RequestBody TravelGuide guide) {
        travelGuideService.updateGuide(guide);
        return Result.success();
    }

    @Operation(summary = "删除攻略")
    @DeleteMapping("/delete/{id}")
    public Result<?> deleteGuide(@PathVariable Long id) {
        travelGuideService.deleteGuide(id);
        return Result.success();
    }

    // 获取热门攻略
    @Operation(summary = "获取热门攻略")
    @GetMapping("/hot")
    public Result<?> getHotGuides(
            @RequestParam(required = false, defaultValue = "3") Integer limit) {
        List<Map<String, Object>> hotGuides = travelGuideService.getHotGuides(limit);
        return Result.success(hotGuides);
    }

    @Operation(summary = "获取攻略搜索建议")
    @GetMapping("/suggestions")
    public Result<?> getGuideSuggestions(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "5") Integer limit) {
        List<Map<String, Object>> suggestions = travelGuideService.getGuideSuggestions(keyword, limit);
        return Result.success(suggestions);
    }
}