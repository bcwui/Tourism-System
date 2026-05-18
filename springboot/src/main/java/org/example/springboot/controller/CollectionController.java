package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.Collection;
import org.example.springboot.entity.User;
import org.example.springboot.service.CollectionService;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "收藏接口")
@RestController
@RequestMapping("/collection")
public class CollectionController {
    @Resource
    private CollectionService collectionService;

    @Operation(summary = "添加收藏")
    @PostMapping("/add")
    public Result<?> addCollection(@RequestBody Collection collection) {
        // 设置当前登录用户ID
        User currentUser = JwtTokenUtils.getCurrentUser();
        collection.setUserId(currentUser.getId());
        
        collectionService.addCollection(collection);
        return Result.success();
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/cancel")
    public Result<?> cancelCollection(@RequestParam Long guideId) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        collectionService.cancelCollection(currentUser.getId(), guideId);
        return Result.success();
    }

    @Operation(summary = "查询用户收藏列表")
    @GetMapping("/page")
    public Result<?> getCollectionsByPage(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
                
        Page<Collection> page;
        if (userId != null) {
            page = collectionService.getCollectionsByPage(userId, currentPage, size);
        } else {
            page = collectionService.getCurrentUserCollections(currentPage, size);
        }
        return Result.success(page);
    }

    @Operation(summary = "检查是否已收藏")
    @GetMapping("/isCollected")
    public Result<?> isCollected(@RequestParam Long guideId) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        boolean collected = collectionService.isCollected(currentUser.getId(), guideId);
        return Result.success(collected);
    }
    
    @Operation(summary = "管理员查询所有用户收藏")
    @GetMapping("/admin/page")
    public Result<?> getAdminCollectionList(
            @RequestParam(defaultValue = "") String username,
            @RequestParam(defaultValue = "") String guideTitle,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<Collection> page = collectionService.getCollectionsByAdmin(username, guideTitle, currentPage, size);
        return Result.success(page);
    }
    
    @Operation(summary = "管理员删除收藏")
    @DeleteMapping("/admin/{id}")
    public Result<?> deleteCollection(@PathVariable Long id) {
        collectionService.deleteCollection(id);
        return Result.success();
    }
} 