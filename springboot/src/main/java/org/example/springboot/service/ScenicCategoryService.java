package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.example.springboot.entity.ScenicCategory;
import org.example.springboot.mapper.ScenicCategoryMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ScenicCategoryService {
    
    @Resource
    private ScenicCategoryMapper scenicCategoryMapper;
    
    /**
     * 分页查询分类列表
     */
    public Page<ScenicCategory> getCategoriesByPage(String name, Integer currentPage, Integer size) {
        LambdaQueryWrapper<ScenicCategory> queryWrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.isNotBlank(name)) {
            queryWrapper.like(ScenicCategory::getName, name);
        }
        
        // 按排序字段升序排列
        queryWrapper.orderByAsc(ScenicCategory::getSortOrder);
        
        return scenicCategoryMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
    }
    
    /**
     * 获取所有分类（树形结构）
     */
    public List<ScenicCategory> getCategoryTree() {
        // 1. 查询所有分类
        List<ScenicCategory> allCategories = scenicCategoryMapper.selectList(
            new LambdaQueryWrapper<ScenicCategory>()
                .orderByAsc(ScenicCategory::getSortOrder)
        );
        
        // 2. 按父ID分组
        Map<Long, List<ScenicCategory>> parentIdMap = allCategories.stream()
            .collect(Collectors.groupingBy(ScenicCategory::getParentId));
        
        // 3. 构建树形结构
        List<ScenicCategory> rootCategories = parentIdMap.getOrDefault(0L, new ArrayList<>());
        rootCategories.forEach(category -> buildChildrenCategories(category, parentIdMap));
        
        return rootCategories;
    }
    
    /**
     * 递归构建子分类
     */
    private void buildChildrenCategories(ScenicCategory parent, Map<Long, List<ScenicCategory>> parentIdMap) {
        buildChildrenCategories(parent, parentIdMap, new HashSet<>());
    }

    private void buildChildrenCategories(ScenicCategory parent, Map<Long, List<ScenicCategory>> parentIdMap, Set<Long> visited) {
        if (!visited.add(parent.getId())) {
            return; // 已访问过，跳过，防止循环引用
        }
        List<ScenicCategory> children = parentIdMap.getOrDefault(parent.getId(), new ArrayList<>());
        if (!children.isEmpty()) {
            parent.setChildren(children);
            children.forEach(child -> buildChildrenCategories(child, parentIdMap, visited));
        }
    }
    
    /**
     * 新增分类
     */
    public boolean addCategory(ScenicCategory category) {
        return scenicCategoryMapper.insert(category) > 0;
    }
    
    /**
     * 更新分类
     */
    public boolean updateCategory(ScenicCategory category) {
        return scenicCategoryMapper.updateById(category) > 0;
    }
    
    /**
     * 删除分类
     */
    public boolean deleteCategory(Long id) {
        // 检查是否有子分类
        Long count = scenicCategoryMapper.selectCount(
            new LambdaQueryWrapper<ScenicCategory>()
                .eq(ScenicCategory::getParentId, id)
        );
        
        if (count > 0) {
            return false; // 有子分类，不能删除
        }
        
        return scenicCategoryMapper.deleteById(id) > 0;
    }
    
    /**
     * 获取单个分类详情
     */
    public ScenicCategory getCategoryById(Long id) {
        return scenicCategoryMapper.selectById(id);
    }
} 