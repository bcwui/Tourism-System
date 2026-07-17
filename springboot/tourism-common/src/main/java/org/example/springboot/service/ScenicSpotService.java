package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.springboot.entity.ScenicCategory;
import org.example.springboot.entity.ScenicSpot;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.ScenicCategoryMapper;
import org.example.springboot.mapper.ScenicSpotMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScenicSpotService {
    @Resource
    private ScenicSpotMapper scenicSpotMapper;
    
    @Resource
    private ScenicCategoryService scenicCategoryService;
    @Autowired
    private ScenicCategoryMapper scenicCategoryMapper;

    public Page<ScenicSpot> getScenicSpotsByPage(String name, String location, Long categoryId, Integer currentPage, Integer size) {
        LambdaQueryWrapper<ScenicSpot> queryWrapper = new LambdaQueryWrapper<>();

        // 如果有名称搜索，进行综合搜索（名称、地区、描述）
        if (StringUtils.isNotBlank(name)) {
            queryWrapper.and(wrapper -> wrapper
                .like(ScenicSpot::getName, name)
                .or()
                .like(ScenicSpot::getLocation, name)
                .or()
                .like(ScenicSpot::getDescription, name)
            );
        }

        // 如果有单独的地区搜索
        if (StringUtils.isNotBlank(location)) {
            queryWrapper.like(ScenicSpot::getLocation, location);
        }

        // 分类筛选
        if (categoryId != null) {
            queryWrapper.eq(ScenicSpot::getCategoryId, categoryId);
        }

        // 按ID降序排序，让新添加的景点排在前面
        queryWrapper.orderByDesc(ScenicSpot::getId);

        Page<ScenicSpot> page = scenicSpotMapper.selectPage(new Page<>(currentPage, size), queryWrapper);

        // 填充分类信息
        fillCategoryInfo(page.getRecords());

        return page;
    }
    
    /**
     * 根据分类ID查询景点
     */
    public List<ScenicSpot> getScenicSpotsByCategoryId(Long categoryId) {
        LambdaQueryWrapper<ScenicSpot> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ScenicSpot::getCategoryId, categoryId);
        
        List<ScenicSpot> spots = scenicSpotMapper.selectList(queryWrapper);
        fillCategoryInfo(spots);
        
        return spots;
    }

    public ScenicSpot getById(Long id) {
        ScenicSpot spot = scenicSpotMapper.selectById(id);
        if (spot == null) throw new ServiceException("景点不存在");
        
        // 填充分类信息
        if (spot.getCategoryId() != null) {
            spot.setCategoryInfo(scenicCategoryService.getCategoryById(spot.getCategoryId()));
        }
        
        return spot;
    }

    public void createScenicSpot(ScenicSpot spot) {
        // 验证分类是否存在
        if (spot.getCategoryId() != null) {
            ScenicCategory category = scenicCategoryService.getCategoryById(spot.getCategoryId());
            if (category == null) {
                throw new ServiceException("所选分类不存在");
            }
        }
        
        if (scenicSpotMapper.insert(spot) <= 0) throw new ServiceException("新增景点失败");
    }

    public void updateScenicSpot(Long id, ScenicSpot spot) {
        if (scenicSpotMapper.selectById(id) == null) throw new ServiceException("景点不存在");
        spot.setId(id);
        
        // 验证分类是否存在
        if (spot.getCategoryId() != null) {
            ScenicCategory category = scenicCategoryService.getCategoryById(spot.getCategoryId());
            if (category == null) {
                throw new ServiceException("所选分类不存在");
            }
        }
        
        if (scenicSpotMapper.updateById(spot) <= 0) throw new ServiceException("更新景点失败");
    }

    public void deleteScenicSpot(Long id) {
        if (scenicSpotMapper.deleteById(id) <= 0) throw new ServiceException("删除景点失败");
    }

    public List<ScenicSpot> getAll() {
        List<ScenicSpot> spots = scenicSpotMapper.selectList(new LambdaQueryWrapper<>());
        fillCategoryInfo(spots);
        return spots;
    }
    
    /**
     * 填充景点的分类信息
     */
    private void fillCategoryInfo(List<ScenicSpot> spots) {
        if (spots == null || spots.isEmpty()) {
            return;
        }
        
        // 获取所有涉及到的分类ID
        List<Long> categoryIds = spots.stream()
                .map(ScenicSpot::getCategoryId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        
        if (categoryIds.isEmpty()) {
            return;
        }
        
        // 批量查询分类信息
        LambdaQueryWrapper<ScenicCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(ScenicCategory::getId, categoryIds);
        List<ScenicCategory> categories = scenicCategoryMapper.selectList(queryWrapper);
        
        // 转换为Map便于查找
        Map<Long, ScenicCategory> categoryMap = categories.stream()
                .collect(Collectors.toMap(ScenicCategory::getId, category -> category));
        
        // 填充分类信息
        spots.forEach(spot -> {
            if (spot.getCategoryId() != null && categoryMap.containsKey(spot.getCategoryId())) {
                spot.setCategoryInfo(categoryMap.get(spot.getCategoryId()));
            }
        });
    }

    /**
     * 获取热门景点
     * @param limit 限制数量
     * @return 热门景点列表
     */
    public List<ScenicSpot> getHotScenics(Integer limit) {
        // 这里可以根据实际需求定义热门景点的获取逻辑
        // 例如根据评分、访问量、价格等条件排序
        LambdaQueryWrapper<ScenicSpot> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(ScenicSpot::getId);
        queryWrapper.last("LIMIT " + limit);
        return scenicSpotMapper.selectList(queryWrapper);
    }

    /**
     * 根据ID列表批量查询景点
     * @param ids 景点ID列表
     * @return 景点列表
     */
    public List<ScenicSpot> getScenicSpotsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        // 直接从数据库查询，避免Redis缓存的类型转换问题
        LambdaQueryWrapper<ScenicSpot> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(ScenicSpot::getId, ids);
        List<ScenicSpot> spots = scenicSpotMapper.selectList(queryWrapper);

        // 填充分类信息
        fillCategoryInfo(spots);

        return spots;
    }

    /**
     * 获取搜索建议
     * @param keyword 搜索关键词
     * @param limit 限制数量
     * @return 搜索建议列表
     */
    public List<Map<String, Object>> getSearchSuggestions(String keyword, Integer limit) {
        List<Map<String, Object>> result = new ArrayList<>();

        if (StringUtils.isBlank(keyword)) {
            return result;
        }

        // 搜索景点建议
        LambdaQueryWrapper<ScenicSpot> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper
            .like(ScenicSpot::getName, keyword)
            .or()
            .like(ScenicSpot::getLocation, keyword)
        );
        queryWrapper.orderByDesc(ScenicSpot::getId);
        queryWrapper.last("LIMIT " + limit);

        List<ScenicSpot> scenics = scenicSpotMapper.selectList(queryWrapper);

        for (ScenicSpot scenic : scenics) {
            Map<String, Object> suggestion = new HashMap<>();
            suggestion.put("id", scenic.getId());
            suggestion.put("name", scenic.getName());
            suggestion.put("location", scenic.getLocation());
            suggestion.put("imageUrl", scenic.getImageUrl());
            suggestion.put("type", "scenic");
            suggestion.put("price", scenic.getPrice());
            result.add(suggestion);
        }

        return result;
    }
}