package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.example.springboot.entity.Comment;
import org.example.springboot.entity.ScenicSpot;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.CommentMapper;
import org.example.springboot.mapper.ScenicSpotMapper;
import org.example.springboot.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class CommentService {
    @Resource
    private CommentMapper commentMapper;
    
    @Resource
    private ScenicSpotMapper scenicSpotMapper;
    
    @Resource
    private UserMapper userMapper;

    public Page<Comment> getCommentsByPage(Long scenicId, String scenicName, String userName, String content, Integer currentPage, Integer size) {
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        
        // 如果提供了景点ID，直接使用ID查询
        if (scenicId != null) {
            queryWrapper.eq(Comment::getScenicId, scenicId);
        } 
        // 如果提供了景点名称，先查询景点ID，再使用ID查询评论
        else if (StringUtils.isNotBlank(scenicName)) {
            List<Long> scenicIds = getScenicIdsByName(scenicName);
            if (scenicIds.isEmpty()) {
                // 如果没有找到匹配的景点，返回空结果
                return new Page<>(currentPage, size);
            }
            queryWrapper.in(Comment::getScenicId, scenicIds);
        }
        
        // 如果提供了用户名/昵称，先查询用户ID，再使用ID查询评论
        if (StringUtils.isNotBlank(userName)) {
            List<Long> userIds = getUserIdsByName(userName);
            if (userIds.isEmpty()) {
                // 如果没有找到匹配的用户，返回空结果
                return new Page<>(currentPage, size);
            }
            queryWrapper.in(Comment::getUserId, userIds);
        }
        
        // 如果提供了内容关键词，按内容过滤
        if (StringUtils.isNotBlank(content)) {
            queryWrapper.like(Comment::getContent, content);
        }
        
        queryWrapper.orderByDesc(Comment::getCreateTime);
        return commentMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
    }
    
    private List<Long> getScenicIdsByName(String name) {
        if (StringUtils.isBlank(name)) {
            return Collections.emptyList();
        }
        
        LambdaQueryWrapper<ScenicSpot> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(ScenicSpot::getName, name);
        return scenicSpotMapper.selectList(queryWrapper)
            .stream()
            .map(ScenicSpot::getId)
            .toList();
    }
    
    private List<Long> getUserIdsByName(String name) {
        if (StringUtils.isBlank(name)) {
            return Collections.emptyList();
        }
        
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(User::getUsername, name)
                   .or()
                   .like(User::getNickname, name);
        return userMapper.selectList(queryWrapper)
            .stream()
            .map(User::getId)
            .toList();
    }

    public void addComment(Comment comment) {
        if (commentMapper.insert(comment) <= 0) throw new ServiceException("评论失败");
    }

    public void deleteComment(Long id, Long userId, boolean isAdmin) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) throw new ServiceException("评论不存在");
        if (!isAdmin && !comment.getUserId().equals(userId)) throw new ServiceException("无权删除");
        if (commentMapper.deleteById(id) <= 0) throw new ServiceException("删除失败");
    }

    public void likeComment(Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) throw new ServiceException("评论不存在");
        comment.setLikes(comment.getLikes() == null ? 1 : comment.getLikes() + 1);
        if (commentMapper.updateById(comment) <= 0) throw new ServiceException("点赞失败");
    }

    public Comment getById(Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) throw new ServiceException("评论不存在");
        return comment;
    }

    public List<Comment> getAllByScenicId(Long scenicId) {
        return commentMapper.selectList(new LambdaQueryWrapper<Comment>().eq(Comment::getScenicId, scenicId));
    }
} 