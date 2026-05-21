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

    @Transactional
    public boolean toggleLike(Long commentId) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("用户未登录");
        }

        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new ServiceException("评论不存在");
        }

        LambdaQueryWrapper<CommentLike> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CommentLike::getUserId, currentUser.getId())
                   .eq(CommentLike::getCommentId, commentId);
        CommentLike like = commentLikeMapper.selectOne(queryWrapper);

        if (like == null) {
            // 使用 INSERT IGNORE 防止并发重复点赞
            int inserted = commentLikeMapper.insertIgnore(currentUser.getId(), commentId);
            if (inserted > 0) {
                // 原子递增点赞数，与 insert 在同一个事务中
                commentMapper.incrementLikes(commentId);
                return true;
            }
            // INSERT IGNORE 返回 0 说明并发时已有其他请求先插入了
            return false;
        } else {
            commentLikeMapper.deleteById(like.getId());
            // 原子递减点赞数
            commentMapper.decrementLikes(commentId);
            return false;
        }
    }

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
