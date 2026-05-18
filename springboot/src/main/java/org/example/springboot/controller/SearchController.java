package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.service.ScenicSpotService;
import org.example.springboot.service.TravelGuideService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Tag(name = "搜索接口")
@RestController
@RequestMapping("/search")
public class SearchController {

    @Resource
    private ScenicSpotService scenicSpotService;
    
    @Resource
    private TravelGuideService travelGuideService;

    @Operation(summary = "获取综合搜索建议")
    @GetMapping("/suggestions")
    public Result<?> getSearchSuggestions(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "3") Integer scenicLimit,
            @RequestParam(defaultValue = "3") Integer guideLimit) {
        
        Map<String, Object> result = new HashMap<>();
        
        // 获取景点建议
        List<Map<String, Object>> scenicSuggestions = scenicSpotService.getSearchSuggestions(keyword, scenicLimit);
        result.put("scenics", scenicSuggestions);
        
        // 获取攻略建议
        List<Map<String, Object>> guideSuggestions = travelGuideService.getGuideSuggestions(keyword, guideLimit);
        result.put("guides", guideSuggestions);
        
        return Result.success(result);
    }
}
