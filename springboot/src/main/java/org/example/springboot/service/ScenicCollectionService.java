package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.springboot.entity.ScenicCollection;
import org.example.springboot.entity.ScenicSpot;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.ScenicCollectionMapper;
import org.example.springboot.mapper.ScenicSpotMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class ScenicCollectionService {
    @Resource
    private ScenicCollectionMapper scenicCollectionMapper;
    
    @Resource
    private ScenicSpotMapper scenicSpotMapper;
    
    @Resource
    private ScenicSpotService scenicSpotService;

    /**
     * 添加景点收藏
     */
    public void addCollection(Long scenicId) {
        // 获取当前用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("用户未登录");
        }
        
        // 检查景点是否存在
        ScenicSpot scenicSpot = scenicSpotMapper.selectById(scenicId);
        if (scenicSpot == null) {
            throw new ServiceException("景点不存在");
        }
        
        // 检查是否已收藏
        LambdaQueryWrapper<ScenicCollection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ScenicCollection::getUserId, currentUser.getId())
                   .eq(ScenicCollection::getScenicId, scenicId);
        if (scenicCollectionMapper.selectOne(queryWrapper) != null) {
            throw new ServiceException("已收藏该景点");
        }
        
        // 添加收藏
        ScenicCollection collection = new ScenicCollection();
        collection.setUserId(currentUser.getId());
        collection.setScenicId(scenicId);
        collection.setCreateTime(LocalDateTime.now());
        
        if (scenicCollectionMapper.insert(collection) <= 0) {
            throw new ServiceException("收藏失败");
        }
    }
    
    /**
     * 取消景点收藏
     */
    public void cancelCollection(Long scenicId) {
        // 获取当前用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("用户未登录");
        }
        
        // 删除收藏记录
        LambdaQueryWrapper<ScenicCollection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ScenicCollection::getUserId, currentUser.getId())
                   .eq(ScenicCollection::getScenicId, scenicId);
                   
        if (scenicCollectionMapper.delete(queryWrapper) <= 0) {
            throw new ServiceException("取消收藏失败，可能未收藏该景点");
        }
    }
    
    /**
     * 查询用户是否已收藏某景点
     */
    public boolean isCollected(Long scenicId) {
        // 获取当前用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return false;
        }
        
        // 查询是否存在收藏记录
        System.out.printf("当前登录："+currentUser.getId()+"<UNK>");
        LambdaQueryWrapper<ScenicCollection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ScenicCollection::getUserId, currentUser.getId())
                   .eq(ScenicCollection::getScenicId, scenicId);
                   
        return scenicCollectionMapper.selectCount(queryWrapper) > 0;
    }
    
    /**
     * 查询用户收藏的景点列表（分页）
     */
    public Page<ScenicCollection> getUserCollections(Long userId, Integer currentPage, Integer size) {
        if (userId == null) {
            // 获取当前用户
            User currentUser = JwtTokenUtils.getCurrentUser();
            if (currentUser == null) {
                throw new ServiceException("用户未登录");
            }
            userId = currentUser.getId();
        }
        
        // 查询收藏记录
        LambdaQueryWrapper<ScenicCollection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ScenicCollection::getUserId, userId)
                   .orderByDesc(ScenicCollection::getCreateTime);
                   
        Page<ScenicCollection> page = scenicCollectionMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        
        // 填充景点信息
        fillScenicInfo(page.getRecords());
        
        return page;
    }
    
    /**
     * 查询用户收藏的所有景点ID
     */
    public List<Long> getUserCollectionIds(Long userId) {
        if (userId == null) {
            // 获取当前用户
            User currentUser = JwtTokenUtils.getCurrentUser();
            if (currentUser == null) {
                return new ArrayList<>();
            }
            userId = currentUser.getId();
        }
        
        // 查询收藏记录
        LambdaQueryWrapper<ScenicCollection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ScenicCollection::getUserId, userId)
                   .select(ScenicCollection::getScenicId);
                   
        List<ScenicCollection> collections = scenicCollectionMapper.selectList(queryWrapper);
        
        return collections.stream()
                .map(ScenicCollection::getScenicId)
                .collect(Collectors.toList());
    }
    
    /**
     * 批量查询用户是否已收藏景点
     */
    public Map<Long, Boolean> batchIsCollected(List<Long> scenicIds) {
        // 获取当前用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null || scenicIds == null || scenicIds.isEmpty()) {
            return scenicIds.stream().collect(Collectors.toMap(id -> id, id -> false));
        }
        
        // 查询用户收藏的景点ID
        List<Long> collectedIds = getUserCollectionIds(currentUser.getId());
        Set<Long> collectedIdSet = Set.copyOf(collectedIds);
        
        // 构建结果
        return scenicIds.stream()
                .collect(Collectors.toMap(
                    id -> id,
                    collectedIdSet::contains
                ));
    }
    
    /**
     * 填充景点信息
     */
    private void fillScenicInfo(List<ScenicCollection> collections) {
        if (collections == null || collections.isEmpty()) {
            return;
        }
        
        // 提取景点ID
        List<Long> scenicIds = collections.stream()
                .map(ScenicCollection::getScenicId)
                .collect(Collectors.toList());
                
        // 批量查询景点信息
        List<ScenicSpot> scenicSpots = scenicSpotService.getScenicSpotsByIds(scenicIds);
        
        // 转换为Map便于查找
        Map<Long, ScenicSpot> scenicSpotMap = scenicSpots.stream()
                .collect(Collectors.toMap(ScenicSpot::getId, spot -> spot));
                
        // 填充景点信息
        collections.forEach(collection -> {
            if (scenicSpotMap.containsKey(collection.getScenicId())) {
                collection.setScenicInfo(scenicSpotMap.get(collection.getScenicId()));
            }
        });
    }
} 