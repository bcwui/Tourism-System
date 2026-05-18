package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.springboot.entity.TravelGuide;
import org.example.springboot.entity.User;
import org.example.springboot.mapper.TravelGuideMapper;
import org.example.springboot.mapper.UserMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.HashMap;

@Service
public class TravelGuideService {
    @Resource
    private TravelGuideMapper travelGuideMapper;
    @Resource
    private UserMapper userMapper;

    public Page<TravelGuide> getGuidesByPage(String title, Long userId, Integer currentPage, Integer size) {
        LambdaQueryWrapper<TravelGuide> queryWrapper = new LambdaQueryWrapper<>();

        // 如果有标题搜索，进行综合搜索（标题、内容）
        if (StringUtils.isNotBlank(title)) {
            queryWrapper.and(wrapper -> wrapper
                .like(TravelGuide::getTitle, title)
                .or()
                .like(TravelGuide::getContent, title)
            );
        }

        if (userId != null) {
            queryWrapper.eq(TravelGuide::getUserId, userId);
        }

        // 按创建时间降序排序
        queryWrapper.orderByDesc(TravelGuide::getCreateTime);
        Page<TravelGuide> page = travelGuideMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        // 批量查用户昵称和头像
        List<Long> userIds = page.getRecords().stream().map(TravelGuide::getUserId).distinct().collect(Collectors.toList());
        if (!userIds.isEmpty()) {
            Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));
            for (TravelGuide guide : page.getRecords()) {
                User user = userMap.get(guide.getUserId());
                if (user != null) {
                    guide.setUserNickname(user.getNickname());
                    guide.setUserAvatar(user.getAvatar());
                }
            }
        }
        return page;
    }

    public TravelGuide getById(Long id) {
        TravelGuide guide = travelGuideMapper.selectById(id);
        if (guide != null) {
            User user = userMapper.selectById(guide.getUserId());
            if (user != null) {
                guide.setUserNickname(user.getNickname());
                guide.setUserAvatar(user.getAvatar());
            }
        }

        return guide;
    }

    public void addGuide(TravelGuide guide) {
        travelGuideMapper.insert(guide);
    }

    public void updateGuide(TravelGuide guide) {
        travelGuideMapper.updateById(guide);
    }

    public void deleteGuide(Long id) {
        travelGuideMapper.deleteById(id);
    }

    public void addView(Long id) {
        travelGuideMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<TravelGuide>().eq("id", id).setSql("views = views + 1"));
    }

    /**
     * 获取热门攻略列表
     * @param limit 限制数量
     * @return 热门攻略列表
     */
    public List<Map<String, Object>> getHotGuides(Integer limit) {
        // 根据浏览量倒序排序获取热门攻略
        LambdaQueryWrapper<TravelGuide> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(TravelGuide::getViews);
        queryWrapper.last("LIMIT " + limit);
        List<TravelGuide> guides = travelGuideMapper.selectList(queryWrapper);
        
        // 组装返回数据，包含用户信息
        List<Map<String, Object>> result = new ArrayList<>();
        for (TravelGuide guide : guides) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", guide.getId());
            map.put("title", guide.getTitle());
            map.put("coverImage", guide.getCoverImage());
            map.put("views", guide.getViews());
            map.put("createTime", guide.getCreateTime());
            
            // 获取用户信息
            if (guide.getUserId() != null) {
                User user = userMapper.selectById(guide.getUserId());
                if (user != null) {
                    map.put("userNickname", user.getNickname());
                    map.put("userAvatar", user.getAvatar());
                }
            }
            
            result.add(map);
        }
        
        return result;
    }

    /**
     * 获取攻略搜索建议
     * @param keyword 搜索关键词
     * @param limit 限制数量
     * @return 搜索建议列表
     */
    public List<Map<String, Object>> getGuideSuggestions(String keyword, Integer limit) {
        List<Map<String, Object>> result = new ArrayList<>();

        if (StringUtils.isBlank(keyword)) {
            return result;
        }

        // 搜索攻略建议
        LambdaQueryWrapper<TravelGuide> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper
            .like(TravelGuide::getTitle, keyword)
            .or()
            .like(TravelGuide::getContent, keyword)
        );
        queryWrapper.orderByDesc(TravelGuide::getCreateTime);
        queryWrapper.last("LIMIT " + limit);

        List<TravelGuide> guides = travelGuideMapper.selectList(queryWrapper);

        // 获取用户信息
        List<Long> userIds = guides.stream().map(TravelGuide::getUserId).distinct().collect(Collectors.toList());
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMap = userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));
        }

        for (TravelGuide guide : guides) {
            Map<String, Object> suggestion = new HashMap<>();
            suggestion.put("id", guide.getId());
            suggestion.put("title", guide.getTitle());
            suggestion.put("coverImage", guide.getCoverImage());
            suggestion.put("views", guide.getViews());
            suggestion.put("createTime", guide.getCreateTime());
            suggestion.put("type", "guide");

            // 添加用户信息
            User user = userMap.get(guide.getUserId());
            if (user != null) {
                suggestion.put("userNickname", user.getNickname());
                suggestion.put("userAvatar", user.getAvatar());
            }

            result.add(suggestion);
        }

        return result;
    }

    public List<Map<String, Object>> getRelatedGuides(Long guideId, Integer limit) {
        // 先获取当前攻略，用于匹配相关内容
        TravelGuide current = travelGuideMapper.selectById(guideId);
        LambdaQueryWrapper<TravelGuide> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(TravelGuide::getId, guideId);
        if (current != null && StringUtils.isNotBlank(current.getTitle())) {
            // 取标题前4个字做模糊匹配
            String keyword = current.getTitle().length() > 4 ? current.getTitle().substring(0, 4) : current.getTitle();
            queryWrapper.like(TravelGuide::getTitle, keyword);
        }
        queryWrapper.orderByDesc(TravelGuide::getViews);
        queryWrapper.last("LIMIT " + limit);
        List<TravelGuide> guides = travelGuideMapper.selectList(queryWrapper);

        // 若相关攻略不足，补充热门攻略
        if (guides.size() < limit) {
            LambdaQueryWrapper<TravelGuide> hotWrapper = new LambdaQueryWrapper<>();
            hotWrapper.ne(TravelGuide::getId, guideId);
            List<Long> existIds = guides.stream().map(TravelGuide::getId).collect(Collectors.toList());
            if (!existIds.isEmpty()) hotWrapper.notIn(TravelGuide::getId, existIds);
            hotWrapper.orderByDesc(TravelGuide::getViews);
            hotWrapper.last("LIMIT " + (limit - guides.size()));
            guides.addAll(travelGuideMapper.selectList(hotWrapper));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (TravelGuide guide : guides) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", guide.getId());
            map.put("title", guide.getTitle());
            map.put("coverImage", guide.getCoverImage());
            map.put("views", guide.getViews());
            map.put("createTime", guide.getCreateTime());
            if (guide.getUserId() != null) {
                User user = userMapper.selectById(guide.getUserId());
                if (user != null) {
                    map.put("userNickname", user.getNickname());
                    map.put("userAvatar", user.getAvatar());
                }
            }
            result.add(map);
        }
        return result;
    }
}