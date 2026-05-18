<template>
  <div class="category-menu">
    <h3 class="category-title">景点分类</h3>
    <ul class="category-list">
      <li 
        class="category-item" 
        :class="{ active: !currentCategoryId }"
        @click="handleCategoryClick(null)"
      >
        <div class="category-content">
          <div class="category-name">全部分类</div>
        </div>
      </li>
      <li 
        v-for="category in categories" 
        :key="category.id" 
        class="category-item"
        :class="{ active: currentCategoryId === category.id }"
        @click="handleCategoryClick(category.id)"
      >
        <div class="category-content">
          <div class="category-name">{{ category.name }}</div>
        </div>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const props = defineProps({
  currentCategoryId: {
    type: Number,
    default: null
  }
})

const emit = defineEmits(['category-change'])

const categories = ref([])
const baseAPI = process.env.VUE_APP_BASE_API || '/api'

// 获取分类列表
const fetchCategories = async () => {
  try {
    await request.get('/scenic-category/tree', {}, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        categories.value = res
      }
    })
  } catch (error) {
    console.error('获取分类列表失败:', error)
  }
}

// 点击分类
const handleCategoryClick = (categoryId) => {
  emit('category-change', categoryId)
}

onMounted(() => {
  fetchCategories()
})
</script>

<style lang="scss" scoped>
.category-menu {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: none;
  padding: 20px;
  margin-bottom: 20px;
  border: 1px solid #ecf0f1;

  .category-title {
    font-size: 18px;
    font-weight: 500;
    color: #34495e;
    padding-bottom: 15px;
    border-bottom: 1px solid #ecf0f1;
    margin-top: 0;
    margin-bottom: 15px;
  }

  .category-list {
    padding: 0;
    margin: 0;
    list-style: none;
  }

  .category-item {
    padding: 12px 8px;
    cursor: pointer;
    transition: all 0.3s;
    border-radius: 6px;
    margin-bottom: 5px;

    &:hover {
      background-color: #f8f9fa;
    }

    &.active {
      background-color: #3498db;
      color: #ffffff;
    }
  }

  .category-content {
    display: flex;
    align-items: center;
  }

  .category-name {
    font-size: 15px;
    font-weight: 400;
  }
}
</style> 