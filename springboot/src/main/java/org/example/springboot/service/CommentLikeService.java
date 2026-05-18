package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.example.springboot.entity.Comment;
import org.example.springboot.entity.CommentLike;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.CommentLikeMapper;
import org.example.springboot.mapper.CommentMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentLikeService {
    @Resource
    private CommentLikeMapper commentLikeMapper;
    
    @Resource
    private CommentMapper commentMapper;
    
    /**
     * 点赞或取消点赞
     * @param commentId 评论ID
     * @return 当前点赞状态 true-已点赞 false-未点赞
     */
    @Transactional
    public boolean toggleLike(Long commentId) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("用户未登录");
        }

        // 检查评论是否存在
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new ServiceException("评论不存在");
        }
        
        // 检查是否已点赞
        LambdaQueryWrapper<CommentLike> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CommentLike::getUserId, currentUser.getId())
                   .eq(CommentLike::getCommentId, commentId);
        CommentLike like = commentLikeMapper.selectOne(queryWrapper);
        
        if (like == null) {
            // 未点赞，添加点赞
            like = new CommentLike();
            like.setUserId(currentUser.getId());
            like.setCommentId(commentId);
            commentLikeMapper.insert(like);
            
            // 增加评论点赞数
            Integer currentLikes = comment.getLikes();
            comment.setLikes(currentLikes == null ? 1 : currentLikes + 1);
            commentMapper.updateById(comment);
            return true;
        } else {
            // 已点赞，取消点赞
            commentLikeMapper.deleteById(like.getId());
            
            // 减少评论点赞数
            Integer currentLikes = comment.getLikes();
            comment.setLikes(currentLikes == null || currentLikes <= 0 ? 0 : currentLikes - 1);
            commentMapper.updateById(comment);
            return false;
        }
    }
    
    /**
     * 检查用户是否已点赞评论
     * @param commentId 评论ID
     * @return 是否已点赞
     */
    public boolean isLiked(Long commentId) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return false;
        }
        
        LambdaQueryWrapper<CommentLike> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CommentLike::getUserId, currentUser.getId())
                   .eq(CommentLike::getCommentId, commentId);
        return commentLikeMapper.selectCount(queryWrapper) > 0;
    }
    
    /**
     * 批量检查评论是否已点赞
     * @param commentIds 评论ID列表
     * @return 已点赞的评论ID列表
     */
    public List<Long> batchCheckLiked(List<Long> commentIds) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null || commentIds == null || commentIds.isEmpty()) {
            return List.of();
        }
        
        LambdaQueryWrapper<CommentLike> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CommentLike::getUserId, currentUser.getId())
                   .in(CommentLike::getCommentId, commentIds);
        return commentLikeMapper.selectList(queryWrapper)
                               .stream()
                               .map(CommentLike::getCommentId)
                               .collect(Collectors.toList());
    }
} 