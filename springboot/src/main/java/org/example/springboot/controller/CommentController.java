package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.Comment;
import org.example.springboot.entity.ScenicSpot;
import org.example.springboot.entity.User;
import org.example.springboot.service.CommentLikeService;
import org.example.springboot.service.CommentService;
import org.example.springboot.service.ScenicSpotService;
import org.example.springboot.service.UserService;
import org.example.springboot.util.JwtTokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "评论管理接口")
@RestController
@RequestMapping("/comment")
public class CommentController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommentController.class);
    @Resource
    private CommentService commentService;
    @Resource
    private UserService userService;
    @Resource
    private CommentLikeService commentLikeService;
    @Resource
    private ScenicSpotService scenicSpotService;

    @Operation(summary = "分页查询评论")
    @GetMapping("/page")
    public Result<?> getCommentsByPage(
            @RequestParam(required = false) Long scenicId,
            @RequestParam(required = false) String scenicName,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String content,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<Comment> page = commentService.getCommentsByPage(scenicId, scenicName, userName, content, currentPage, size);
        // 批量查用户
        List<Long> userIds = page.getRecords().stream().map(Comment::getUserId).distinct().toList();
        List<User> users = userService.getUsersByIds(userIds);
        
        // 处理用户信息
        for (Comment c : page.getRecords()) {
            users.stream()
                .filter(u -> u.getId().equals(c.getUserId()))
                .findFirst()
                .ifPresent(u -> {
                    c.setUserNickname(u.getNickname());
                    c.setUserAvatar(u.getAvatar());
                });
        }
        
        // 批量查询景点信息
        List<Long> scenicIds = page.getRecords().stream().map(Comment::getScenicId).distinct().toList();
        List<ScenicSpot> scenicSpots = scenicSpotService.getScenicSpotsByIds(scenicIds);
        for (Comment c : page.getRecords()) {
            scenicSpots.stream()
                .filter(s -> s.getId().equals(c.getScenicId()))
                .findFirst()
                .ifPresent(s -> c.setScenicName(s.getName()));
        }
        
        // 批量查询点赞状态
        if (!page.getRecords().isEmpty() && JwtTokenUtils.getCurrentUser() != null) {
            List<Long> commentIds = page.getRecords().stream().map(Comment::getId).toList();
            List<Long> likedIds = commentLikeService.batchCheckLiked(commentIds);
            
            // 将liked标记添加到每个评论中
            Map<Long, Boolean> likedMap = likedIds.stream().collect(Collectors.toMap(id -> id, id -> true));
            for (Comment c : page.getRecords()) {
                c.setLiked(likedMap.getOrDefault(c.getId(), false));
            }
        }
        
        return Result.success(page);
    }

    @Operation(summary = "添加评论")
    @PostMapping("/add")
    public Result<?> addComment(@RequestBody Comment comment) {
        // 获取当前用户ID
        comment.setUserId(JwtTokenUtils.getCurrentUser().getId());
        commentService.addComment(comment);
        return Result.success("评论成功");
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/delete/{id}")
    public Result<?> deleteComment(@PathVariable Long id) {
        var user = JwtTokenUtils.getCurrentUser();
        boolean isAdmin = user != null && "ADMIN".equals(user.getRoleCode());
        commentService.deleteComment(id, user.getId(), isAdmin);
        return Result.success("删除成功");
    }

    @Operation(summary = "点赞/取消点赞评论")
    @PutMapping("/like/{id}")
    public Result<?> toggleLike(@PathVariable Long id) {
        boolean isLiked = commentLikeService.toggleLike(id);
        return Result.success(isLiked ? "点赞成功" : "取消点赞成功", isLiked);
    }

    @Operation(summary = "获取评论详情")
    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        Comment comment = commentService.getById(id);
        return Result.success(comment);
    }

    @Operation(summary = "获取某景点所有评论")
    @GetMapping("/scenic/{scenicId}")
    public Result<?> getAllByScenicId(@PathVariable Long scenicId) {
        List<Comment> list = commentService.getAllByScenicId(scenicId);
        List<Long> userIds = list.stream().map(Comment::getUserId).distinct().toList();
        List<User> users = userService.getUsersByIds(userIds);
        for (Comment c : list) {
            users.stream()
                 .filter(u -> u.getId().equals(c.getUserId()))
                 .findFirst()
                 .ifPresent(u -> {
                     c.setUserNickname(u.getNickname());
                     c.setUserAvatar(u.getAvatar());
                 });
        }
        
        // 添加景点名称
        ScenicSpot scenicSpot = scenicSpotService.getById(scenicId);
        if (scenicSpot != null) {
            for (Comment c : list) {
                c.setScenicName(scenicSpot.getName());
            }
        }
        
        // 批量查询点赞状态
        if (!list.isEmpty() && JwtTokenUtils.getCurrentUser() != null) {
            List<Long> commentIds = list.stream().map(Comment::getId).toList();
            List<Long> likedIds = commentLikeService.batchCheckLiked(commentIds);
            
            // 将liked标记添加到每个评论中
            Map<Long, Boolean> likedMap = likedIds.stream().collect(Collectors.toMap(id -> id, id -> true));
            for (Comment c : list) {
                c.setLiked(likedMap.getOrDefault(c.getId(), false));
            }
        }
        
        return Result.success(list);
    }
    
    @Operation(summary = "检查评论是否已点赞")
    @GetMapping("/isLiked/{id}")
    public Result<?> isLiked(@PathVariable Long id) {
        boolean liked = commentLikeService.isLiked(id);
        return Result.success(liked);
    }
} 