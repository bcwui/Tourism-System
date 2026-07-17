package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.example.springboot.entity.Accommodation;
import org.example.springboot.entity.ScenicSpot;
import org.example.springboot.mapper.AccommodationMapper;
import org.example.springboot.mapper.AccommodationReviewMapper;
import org.example.springboot.mapper.ScenicSpotMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AccommodationService {
    
    @Resource
    private AccommodationMapper accommodationMapper;
    
    @Resource
    private ScenicSpotMapper scenicSpotMapper;
    
    @Resource
    private AccommodationReviewMapper reviewMapper;
    
    /**
     * 分页查询住宿列表
     * @param scenicId 景点ID
     * @param type 住宿类型
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @param minRating 最低评分
     * @param currentPage 当前页码
     * @param size 每页记录数
     * @return 分页数据
     */
    public Page<Accommodation> getAccommodationsByPage(Integer scenicId, String type, 
                                                      String minPrice, String maxPrice, 
                                                      String minRating, 
                                                      Integer currentPage, Integer size) {
        LambdaQueryWrapper<Accommodation> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加查询条件
        if (scenicId != null) {
            queryWrapper.eq(Accommodation::getScenicId, scenicId);
        }
        
        if (StringUtils.isNotBlank(type)) {
            queryWrapper.eq(Accommodation::getType, type);
        }
        
        // 处理价格区间筛选（这里使用简单字符串比较，实际可能需要更复杂的逻辑）
        if (StringUtils.isNotBlank(minPrice) && StringUtils.isNotBlank(maxPrice)) {
            queryWrapper.like(Accommodation::getPriceRange, minPrice + "-" + maxPrice);
        }
        
        if (StringUtils.isNotBlank(minRating)) {
            queryWrapper.ge(Accommodation::getStarLevel, Double.parseDouble(minRating));
        }
        
        // 按评分降序排序
        queryWrapper.orderByDesc(Accommodation::getStarLevel);
        
        Page<Accommodation> page = accommodationMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        
        // 获取关联的景点信息
        List<Long> scenicIds = page.getRecords().stream()
                .map(Accommodation::getScenicId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        
        if (!scenicIds.isEmpty()) {
            List<ScenicSpot> scenicSpots = scenicSpotMapper.selectBatchIds(scenicIds);
            Map<Long, String> scenicNameMap = scenicSpots.stream()
                    .collect(Collectors.toMap(ScenicSpot::getId, ScenicSpot::getName));
            
            // 设置关联的景点名称
            page.getRecords().forEach(accommodation -> {
                if (accommodation.getScenicId() != null) {
                    accommodation.setScenicName(scenicNameMap.get(accommodation.getScenicId()));
                }
            });
        }
        
        return page;
    }
    
    /**
     * 获取住宿详情
     * @param id 住宿ID
     * @return 住宿详情
     */
    public Accommodation getAccommodationById(Integer id) {
        Accommodation accommodation = accommodationMapper.selectById(id);
        if (accommodation != null && accommodation.getScenicId() != null) {
            // 查询关联景点信息
            ScenicSpot scenicSpot = scenicSpotMapper.selectById(accommodation.getScenicId());
            if (scenicSpot != null) {
                accommodation.setScenicName(scenicSpot.getName());
            }
            
            // 查询评价数量
            LambdaQueryWrapper<org.example.springboot.entity.AccommodationReview> reviewWrapper = new LambdaQueryWrapper<>();
            reviewWrapper.eq(org.example.springboot.entity.AccommodationReview::getAccommodationId, id);
            Long reviewCount = reviewMapper.selectCount(reviewWrapper);
            accommodation.setReviewCount(reviewCount);
        }
        return accommodation;
    }
    
    /**
     * 添加住宿信息
     * @param accommodation 住宿信息
     * @return 是否成功
     */
    public boolean addAccommodation(Accommodation accommodation) {
        return accommodationMapper.insert(accommodation) > 0;
    }
    
    /**
     * 更新住宿信息
     * @param accommodation 住宿信息
     * @return 是否成功
     */
    public boolean updateAccommodation(Accommodation accommodation) {
        return accommodationMapper.updateById(accommodation) > 0;
    }
    
    /**
     * 删除住宿信息
     * @param id 住宿ID
     * @return 是否成功
     */
    public boolean deleteAccommodation(Integer id) {
        return accommodationMapper.deleteById(id) > 0;
    }
    
    /**
     * 获取住宿类型列表
     * @return 住宿类型列表
     */
    public List<String> getAccommodationTypes() {
        LambdaQueryWrapper<Accommodation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(Accommodation::getType);
        queryWrapper.groupBy(Accommodation::getType);
        
        List<Accommodation> accommodations = accommodationMapper.selectList(queryWrapper);
        return accommodations.stream().map(Accommodation::getType).collect(Collectors.toList());
    }
} 