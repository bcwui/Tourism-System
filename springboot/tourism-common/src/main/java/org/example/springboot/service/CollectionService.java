package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.springboot.entity.Collection;
import org.example.springboot.entity.TravelGuide;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.CollectionMapper;
import org.example.springboot.mapper.TravelGuideMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CollectionService {
    @Resource
    private CollectionMapper collectionMapper;
    
    @Resource
    private TravelGuideMapper travelGuideMapper;
    
    @Resource
    private UserMapper userMapper;
    
    /**
     * 添加收藏
     */
    public void addCollection(Collection collection) {
        // 验证用户是否存在
        if (userMapper.selectById(collection.getUserId()) == null) {
            throw new ServiceException("用户不存在");
        }
        
        // 验证攻略是否存在
        if (travelGuideMapper.selectById(collection.getGuideId()) == null) {
            throw new ServiceException("攻略不存在");
        }
        
        // 检查是否已收藏
        LambdaQueryWrapper<Collection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Collection::getUserId, collection.getUserId())
                   .eq(Collection::getGuideId, collection.getGuideId());
        if (collectionMapper.selectOne(queryWrapper) != null) {
            throw new ServiceException("已经收藏过该攻略");
        }
        
        // 添加收藏
        if (collectionMapper.insert(collection) <= 0) {
            throw new ServiceException("收藏失败");
        }
    }
    
    /**
     * 取消收藏
     */
    public void cancelCollection(Long userId, Long guideId) {
        LambdaQueryWrapper<Collection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Collection::getUserId, userId)
                   .eq(Collection::getGuideId, guideId);
        
        if (collectionMapper.delete(queryWrapper) <= 0) {
            throw new ServiceException("取消收藏失败");
        }
    }
    
    /**
     * 分页查询用户的收藏列表
     */
    public Page<Collection> getCollectionsByPage(Long userId, Integer currentPage, Integer size) {
        // 检查用户是否存在
        if (userMapper.selectById(userId) == null) {
            throw new ServiceException("用户不存在");
        }
        
        // 查询用户收藏
        LambdaQueryWrapper<Collection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Collection::getUserId, userId)
                   .orderByDesc(Collection::getCreateTime);
        
        Page<Collection> page = collectionMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        
        // 填充攻略信息
        fillGuideInfo(page.getRecords());
        
        return page;
    }
    
    /**
     * 检查用户是否已收藏某攻略
     */
    public boolean isCollected(Long userId, Long guideId) {
        LambdaQueryWrapper<Collection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Collection::getUserId, userId)
                   .eq(Collection::getGuideId, guideId);
        return collectionMapper.selectCount(queryWrapper) > 0;
    }
    
    /**
     * 获取当前登录用户的收藏列表
     */
    public Page<Collection> getCurrentUserCollections(Integer currentPage, Integer size) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("未登录");
        }
        return getCollectionsByPage(currentUser.getId(), currentPage, size);
    }
    
    /**
     * 管理员查询所有收藏
     */
    public Page<Collection> getCollectionsByAdmin(String username, String guideTitle, Integer currentPage, Integer size) {
        // 首先获取符合条件的用户
        List<Long> userIds = new ArrayList<>();
        if (StringUtils.isNotBlank(username)) {
            LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<>();
            userQuery.like(User::getUsername, username);
            List<User> users = userMapper.selectList(userQuery);
            if (!users.isEmpty()) {
                userIds = users.stream().map(User::getId).collect(Collectors.toList());
            } else {
                // 如果没找到用户，直接返回空结果
                return new Page<>(currentPage, size);
            }
        }
        
        // 查询符合条件的攻略
        List<Long> guideIds = new ArrayList<>();
        if (StringUtils.isNotBlank(guideTitle)) {
            LambdaQueryWrapper<TravelGuide> guideQuery = new LambdaQueryWrapper<>();
            guideQuery.like(TravelGuide::getTitle, guideTitle);
            List<TravelGuide> guides = travelGuideMapper.selectList(guideQuery);
            if (!guides.isEmpty()) {
                guideIds = guides.stream().map(TravelGuide::getId).collect(Collectors.toList());
            } else {
                // 如果没找到攻略，直接返回空结果
                return new Page<>(currentPage, size);
            }
        }
        
        // 构建查询条件
        LambdaQueryWrapper<Collection> queryWrapper = new LambdaQueryWrapper<>();
        if (!userIds.isEmpty()) {
            queryWrapper.in(Collection::getUserId, userIds);
        }
        if (!guideIds.isEmpty()) {
            queryWrapper.in(Collection::getGuideId, guideIds);
        }
        
        queryWrapper.orderByDesc(Collection::getCreateTime);
        Page<Collection> page = collectionMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        
        // 填充攻略和用户信息
        fillGuideAndUserInfo(page.getRecords());
        
        return page;
    }
    
    /**
     * 管理员删除收藏
     */
    public void deleteCollection(Long id) {
        if (collectionMapper.deleteById(id) <= 0) {
            throw new ServiceException("删除收藏失败");
        }
    }
    
    /**
     * 填充攻略信息
     */
    private void fillGuideInfo(List<Collection> collections) {
        if (collections.isEmpty()) return;
        
        List<Long> guideIds = collections.stream()
                               .map(Collection::getGuideId)
                               .distinct()
                               .collect(Collectors.toList());
        
        if (!guideIds.isEmpty()) {
            Map<Long, TravelGuide> guideMap = travelGuideMapper.selectBatchIds(guideIds)
                                           .stream()
                                           .collect(Collectors.toMap(TravelGuide::getId, guide -> guide));
            
            for (Collection collection : collections) {
                TravelGuide guide = guideMap.get(collection.getGuideId());
                if (guide != null) {
                    collection.setGuideTitle(guide.getTitle());
                    collection.setGuideCoverImage(guide.getCoverImage());
                    collection.setGuideViews(guide.getViews());
                }
            }
        }
    }
    
    /**
     * 填充攻略和用户信息
     */
    private void fillGuideAndUserInfo(List<Collection> collections) {
        // 填充攻略信息
        fillGuideInfo(collections);
        
        // 填充用户信息
        if (!collections.isEmpty()) {
            List<Long> userIds = collections.stream()
                                 .map(Collection::getUserId)
                                 .distinct()
                                 .collect(Collectors.toList());
            
            if (!userIds.isEmpty()) {
                Map<Long, User> userMap = userMapper.selectBatchIds(userIds)
                                         .stream()
                                         .collect(Collectors.toMap(User::getId, user -> user));
                
                for (Collection collection : collections) {
                    User user = userMap.get(collection.getUserId());
                    if (user != null) {
                        collection.setUsername(user.getUsername());
                        collection.setUserNickname(user.getNickname());
                        collection.setUserAvatar(user.getAvatar());
                    }
                }
            }
        }
    }
} 