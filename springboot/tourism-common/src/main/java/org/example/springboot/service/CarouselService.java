package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.springboot.entity.Carousel;
import org.example.springboot.mapper.CarouselMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CarouselService {
    @Resource
    private CarouselMapper carouselMapper;
    
    /**
     * 获取所有已启用的轮播图
     * @return 轮播图列表
     */
    public List<Carousel> getActiveCarousels() {
        LambdaQueryWrapper<Carousel> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Carousel::getStatus, 1)
                .orderByAsc(Carousel::getId);
        return carouselMapper.selectList(queryWrapper);
    }
    
    /**
     * 分页查询轮播图
     * @param currentPage 当前页
     * @param size 每页大小
     * @return 分页结果
     */
    public Page<Carousel> getCarouselsByPage(Integer currentPage, Integer size) {
        Page<Carousel> page = new Page<>(currentPage, size);
        LambdaQueryWrapper<Carousel> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Carousel::getCreateTime);
        return carouselMapper.selectPage(page, queryWrapper);
    }
    
    /**
     * 获取轮播图详情
     * @param id 轮播图ID
     * @return 轮播图信息
     */
    public Carousel getCarouselById(Integer id) {
        return carouselMapper.selectById(id);
    }
    
    /**
     * 添加轮播图
     * @param carousel 轮播图信息
     * @return 添加结果
     */
    public boolean addCarousel(Carousel carousel) {
        return carouselMapper.insert(carousel) > 0;
    }
    
    /**
     * 更新轮播图
     * @param carousel 轮播图信息
     * @return 更新结果
     */
    public boolean updateCarousel(Carousel carousel) {
        return carouselMapper.updateById(carousel) > 0;
    }
    
    /**
     * 删除轮播图
     * @param id 轮播图ID
     * @return 删除结果
     */
    public boolean deleteCarousel(Integer id) {
        return carouselMapper.deleteById(id) > 0;
    }
    
    /**
     * 切换轮播图状态
     * @param id 轮播图ID
     * @param status 状态值
     * @return 操作结果
     */
    public boolean updateStatus(Integer id, Integer status) {
        Carousel carousel = new Carousel();
        carousel.setId(id);
        carousel.setStatus(status);
        return carouselMapper.updateById(carousel) > 0;
    }
} 