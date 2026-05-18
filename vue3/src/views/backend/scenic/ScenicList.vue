<template>
  <div class="scenic-list-container">
    <div class="page-header">
      <h1 class="page-title">景点管理</h1>
      <p class="page-subtitle">Scenic Spot Management</p>
    </div>

    <!-- 操作按钮 -->
    <div class="action-bar">
      <div class="action-right">
        <el-button type="primary" @click="handleAdd" class="add-btn">
          <i class="el-icon-plus"></i> 新增景点
        </el-button>
      </div>
    </div>

    <!-- 搜索区域 -->
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="名称">
          <el-input v-model="searchForm.name" placeholder="请输入景点名称" clearable>
            <template #prefix>
              <i class="el-icon-search"></i>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="地区">
          <el-cascader
            v-model="searchForm.regionValue"
            :options="regionOptions"
            @change="handleRegionChange"
            placeholder="请选择地区"
            clearable
            filterable
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="searchForm.categoryId" placeholder="请选择分类" clearable>
            <el-option
              v-for="item in categoryOptions" 
              :key="item.id" 
              :label="item.name" 
              :value="item.id"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" class="search-btn">
            <i class="el-icon-search"></i> 搜索
          </el-button>
          <el-button @click="resetSearch" class="reset-btn">
            <i class="el-icon-refresh"></i> 重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格区域 -->
    <el-card class="table-card" shadow="never">
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
        class="scenic-table"
      >
        <el-table-column prop="id" label="ID" width="80" align="center"></el-table-column>
        <el-table-column prop="name" label="名称" width="150">
          <template #default="scope">
            <div class="scenic-name">{{ scope.row.name }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" width="200" show-overflow-tooltip></el-table-column>
        <el-table-column prop="location" label="地区" width="230" align="center">
          <template #default="scope">
            <el-tag size="small" effect="plain" class="location-tag">
              <i class="el-icon-location"></i> {{ scope.row.location }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="120" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.categoryInfo" size="small" type="success" effect="plain">
              {{ scope.row.categoryInfo.name }}
            </el-tag>
            <el-tag v-else size="small" type="info" effect="plain">未分类</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="票价" width="100" align="center">
          <template #default="scope">
            <span class="price-tag">¥ {{ scope.row.price }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="openingHours" label="开放时间" width="150" align="center">
          <template #default="scope">
            <span class="time-tag">
              <i class="el-icon-time"></i> {{ scope.row.openingHours }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="坐标" width="180" align="center">
          <template #default="scope">
            <span v-if="scope.row.longitude && scope.row.latitude" class="coordinate-tag">
              <el-tooltip :content="`经度: ${scope.row.longitude}, 纬度: ${scope.row.latitude}`" placement="top">
                <span>{{ scope.row.longitude.toFixed(4) }}, {{ scope.row.latitude.toFixed(4) }}</span>
              </el-tooltip>
            </span>
            <span v-else class="no-coordinate">未设置</span>
          </template>
        </el-table-column>
        <el-table-column prop="imageUrl" label="图片" width="120" align="center">
          <template #default="scope">
            <el-image 
              :src="baseAPI + scope.row.imageUrl" 
              style="width: 80px; height: 60px; border-radius: 4px" 
              fit="cover" 
              :preview-teleported="true"
              v-if="scope.row.imageUrl"
              :preview-src-list="[baseAPI + scope.row.imageUrl]"
            >
              <template #error>
                <div class="image-error">
                  <i class="el-icon-picture"></i>
                </div>
              </template>
            </el-image>
            <div v-else class="no-image">
              <i class="el-icon-picture-outline"></i>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间"  align="center">
          <template #default="scope">
            <span class="date-text">{{ formatDate(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="180" align="center">
          <template #default="scope">
            <el-button size="small" type="primary" plain @click="handleEdit(scope.row)" class="edit-btn">
              <i class="el-icon-edit"></i> 编辑
            </el-button>
            <el-button size="small" type="danger" plain @click="handleDelete(scope.row)" class="delete-btn">
              <i class="el-icon-delete"></i> 删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页区域 -->
      <div class="pagination-container">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :current-page="currentPage"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="pageSize"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        ></el-pagination>
      </div>
    </el-card>

    <!-- 景点表单对话框 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="600px"
      @close="resetForm"
      class="scenic-dialog"
    >
      <el-form
        ref="scenicFormRef"
        :model="scenicForm"
        :rules="scenicFormRules"
        label-width="100px"
        class="scenic-form"
      >
        <el-form-item label="名称" prop="name">
          <el-input v-model="scenicForm.name" placeholder="请输入景点名称"></el-input>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="scenicForm.description" type="textarea" rows="4" placeholder="请输入描述"></el-input>
        </el-form-item>
        <el-form-item label="地区" prop="location">
          <el-cascader
            v-model="scenicForm.regionValue"
            :options="regionOptions"
            @change="handleFormRegionChange"
            placeholder="请选择地区"
            clearable
            filterable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="scenicForm.categoryId" placeholder="请选择分类" clearable style="width: 100%">
            <el-option
              v-for="item in categoryOptions" 
              :key="item.id" 
              :label="item.name" 
              :value="item.id"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="票价" prop="price">
          <el-input v-model="scenicForm.price" type="number" placeholder="请输入票价">
            <template #prefix>
              <span class="price-prefix">¥</span>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="开放时间" prop="openingHours">
          <el-input v-model="scenicForm.openingHours" placeholder="请输入开放时间">
            <template #prefix>
              <i class="el-icon-time"></i>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="经度" prop="longitude">
          <el-input v-model="scenicForm.longitude" placeholder="请输入经度(如: 116.397428)">
            <template #prefix>
              <i class="el-icon-location-outline"></i>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="纬度" prop="latitude">
          <el-input v-model="scenicForm.latitude" placeholder="请输入纬度(如: 39.916527)">
            <template #prefix>
              <i class="el-icon-location-outline"></i>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="small" @click="searchCoordinates">
            <i class="el-icon-search"></i> 通过地址搜索坐标
          </el-button>
          <el-tooltip content="使用高德地图API根据地址自动获取经纬度" placement="top">
            <i class="el-icon-question"></i>
          </el-tooltip>
        </el-form-item>
        <el-form-item label="图片" prop="imageUrl">
          <div class="upload-container">
            <el-upload
              class="scenic-uploader"
              action="#"
              :show-file-list="false"
              :http-request="customUploadImage"
              :before-upload="beforeImageUpload"
            >
              <div class="upload-trigger" v-if="!scenicForm.imageUrl">
                <i class="el-icon-plus"></i>
                <span>上传图片</span>
              </div>
              <el-image 
                v-else 
                :src="baseAPI + scenicForm.imageUrl" 
                class="preview-image" 
                fit="cover" 
              />
            </el-upload>
            <div v-if="scenicForm.imageUrl" class="image-actions">
              <el-button size="small" type="primary" @click="reuploadImage">更换图片</el-button>
              <el-button size="small" type="danger" @click="removeImage">删除图片</el-button>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="submitLoading">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 地图确认对话框 -->
    <el-dialog
      title="确认坐标位置"
      v-model="mapDialogVisible"
      width="800px"
      @close="cancelCoordinates"
      class="map-dialog"
      destroy-on-close
    >
      <div class="map-tools">
        <div class="search-container">
          <el-input
            v-model="mapSearchKeyword"
            placeholder="搜索地点"
            class="map-search-input"
            @input="handleSearchInput"
            @keyup.enter="searchMapLocation"
          >
            <template #append>
              <el-button @click="searchMapLocation">搜索</el-button>
            </template>
          </el-input>
          <!-- 自定义搜索结果下拉菜单 -->
          <div class="search-results" v-if="showSearchResults && searchResults.length > 0">
            <div 
              v-for="(item, index) in searchResults" 
              :key="index" 
              class="search-result-item"
              @click="selectSearchResult(item)"
            >
              <div class="result-name">{{ item.name }}</div>
              <div class="result-address">{{ item.address || item.district }}</div>
            </div>
          </div>
        </div>
        
        <div class="coordinates-input">
          <el-input v-model="tempCoordinates.lng" placeholder="经度" class="coord-input">
            <template #prepend>经度</template>
          </el-input>
          <el-input v-model="tempCoordinates.lat" placeholder="纬度" class="coord-input">
            <template #prepend>纬度</template>
          </el-input>
          <el-button type="primary" @click="updateMapFromInput" size="small">更新地图</el-button>
        </div>
      </div>
      
      <div class="map-container">
        <div id="confirm-map-container" style="width: 100%; height: 400px;"></div>
        <div class="map-tip">
          <i class="el-icon-info"></i> 提示：您可以拖动标记调整位置，或使用搜索框查找位置
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="cancelCoordinates">取消</el-button>
          <el-button type="primary" @click="confirmCoordinates">确认坐标</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, onUnmounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { formatDate } from '@/utils/dateUtils'
import ChinaRegionData from '@/assets/中国地区数据.json'

const baseAPI = process.env.VUE_APP_BASE_API || '/api'

// 高德地图API相关配置
const AMAP_KEY = '16e2711c3a087b844eb977103e4b2d13'
const AMAP_SECURITY_CODE = 'cc6ce30d593e182d159e8378417b2553'

// 加载高德地图API
const loadAmapScript = () => {
  return new Promise((resolve, reject) => {
    if (window.AMap) {
      // 即使AMap已加载，也需要确保Geocoder插件加载
      if (!window.AMap.Geocoder) {
        window.AMap.plugin(['AMap.Geocoder', 'AMap.PlaceSearch'], () => {
          resolve(window.AMap)
        })
      } else {
        resolve(window.AMap)
      }
      return
    }
    
    // 设置高德安全密钥
    window._AMapSecurityConfig = {
      securityJsCode: AMAP_SECURITY_CODE
    }
    
    const script = document.createElement('script')
    script.type = 'text/javascript'
    script.async = true
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${AMAP_KEY}&plugin=AMap.Geocoder,AMap.PlaceSearch`
    script.onerror = reject
    script.onload = () => {
      // 确保插件加载完成
      window.AMap.plugin(['AMap.Geocoder', 'AMap.PlaceSearch'], () => {
        resolve(window.AMap)
      })
    }
    document.head.appendChild(script)
  })
}

const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const categoryOptions = ref([])

// 格式化地区数据为级联选择器格式
const formatRegionData = () => {
  return ChinaRegionData.map(province => ({
    value: province.province,
    label: province.province,
    children: province.citys.map(city => ({
      value: city.city,
      label: city.city,
      children: city.areas.map(area => ({
        value: area.area,
        label: area.area
      }))
    }))
  }))
}

const regionOptions = ref(formatRegionData())

const searchForm = reactive({
  name: '',
  location: '',
  regionValue: [],
  categoryId: null
})

const scenicFormRef = ref(null)
const dialogVisible = ref(false)
const dialogType = ref('add')
const dialogTitle = ref('新增景点')
const submitLoading = ref(false)

const scenicForm = reactive({
  id: null,
  name: '',
  description: '',
  location: '',
  regionValue: [],
  categoryId: null,
  price: '',
  openingHours: '',
  imageUrl: '',
  longitude: '',
  latitude: ''
})

const scenicFormRules = {
  name: [
    { required: true, message: '请输入景点名称', trigger: 'blur' }
  ],
  price: [
    { required: true, message: '请输入票价', trigger: 'blur' }
  ],
  categoryId: [
    { required: true, message: '请选择分类', trigger: 'change' }
  ]
}

// 在script部分修改响应式变量
const mapDialogVisible = ref(false)
const mapInstance = ref(null)
const mapMarker = ref(null)
const confirmingLocation = ref(false)
const mapSearchKeyword = ref('')
const inputTips = ref(null)
const searchResults = ref([])
const showSearchResults = ref(false)
const tempCoordinates = reactive({
  lng: null,
  lat: null
})

onMounted(() => {
  fetchCategories()
  fetchScenicSpots()
  loadAmapScript().catch(err => {
    console.error('加载高德地图API失败:', err)
  })
})

const fetchScenicSpots = async () => {
  loading.value = true
  try {
    const params = {
      name: searchForm.name,
      location: searchForm.location,
      categoryId: searchForm.categoryId,
      currentPage: currentPage.value,
      size: pageSize.value
    }
    await request.get('/scenic/page', params, {
      onSuccess: (res) => {
        tableData.value = res.records || []
        total.value = res.total || 0
      }
    })
  } catch (error) {
    console.error('获取景点列表失败:', error)
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchScenicSpots()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  fetchScenicSpots()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchScenicSpots()
}

const handleAdd = () => {
  dialogType.value = 'add'
  dialogTitle.value = '新增景点'
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogType.value = 'edit'
  dialogTitle.value = '编辑景点'
  Object.keys(scenicForm).forEach(key => {
    if (key in row) {
      scenicForm[key] = row[key]
    }
  })
  scenicForm.regionValue = parseLocationToRegionValue(row.location)
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该景点吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.delete(`/scenic/delete/${row.id}`, {
        successMsg: '删除成功'
      })
      fetchScenicSpots()
    } catch (error) {
      console.error('删除景点失败:', error)
    }
  }).catch(() => {})
}

const submitForm = () => {
  scenicFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        // 创建一个新对象，不包含regionValue字段
        const formData = { ...scenicForm }
        delete formData.regionValue
        
        if (dialogType.value === 'add') {
          await request.post('/scenic/add', formData, {
            successMsg: '添加景点成功'
          })
        } else {
          await request.put(`/scenic/${formData.id}`, formData, {
            successMsg: '更新景点成功'
          })
        }
        dialogVisible.value = false
        fetchScenicSpots()
      } catch (error) {
        console.error('提交表单失败:', error)
      } finally {
        submitLoading.value = false
      }
    }
  })
}

const resetForm = () => {
  if (scenicFormRef.value) {
    scenicFormRef.value.resetFields()
  }
  Object.keys(scenicForm).forEach(key => {
    if (key === 'id' || key === 'categoryId') {
      scenicForm[key] = null
    } else if (key === 'regionValue') {
      scenicForm[key] = []
    } else {
      scenicForm[key] = ''
    }
  })
}

// 图片上传相关
const beforeImageUpload = (file) => {
  const isJPG = file.type === 'image/jpeg'
  const isPNG = file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isJPG && !isPNG) {
    ElMessage.error('图片只能是 JPG 或 PNG 格式!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB!')
    return false
  }
  return true
}

const customUploadImage = async (options) => {
  try {
    const { file } = options
    const formData = new FormData()
    formData.append('file', file)
    await request.post('/file/upload/img', formData, {
      headers: {
        token: localStorage.getItem('token') || ''
      },
      transformRequest: [(data) => data],
      successMsg: '图片上传成功',
      errorMsg: '图片上传失败',
      onSuccess: (data) => {
        scenicForm.imageUrl = data
        options.onSuccess({ data })
      },
      onError: (error) => {
        options.onError(new Error(error.message || '上传失败'))
      }
    })
  } catch (error) {
    options.onError(error)
    console.error('图片上传过程发生错误:', error)
  }
}

// 获取分类列表
const fetchCategories = async () => {
  try {
    await request.get('/scenic-category/tree', {}, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        categoryOptions.value = res || []
      }
    })
  } catch (error) {
    console.error('获取分类列表失败:', error)
    categoryOptions.value = []
  }
}

// 新增的方法
const reuploadImage = () => {
  // 触发上传组件的点击事件
  const uploadEl = document.querySelector('.scenic-uploader .el-upload')
  if (uploadEl) {
    uploadEl.click()
  }
}

const removeImage = () => {
  scenicForm.imageUrl = ''
}

// 处理搜索表单中的地区选择变化
const handleRegionChange = (value) => {
  if (value && value.length > 0) {
    // 将级联选择的结果拼接为地区字符串
    searchForm.location = value.join(' - ')
  } else {
    searchForm.location = ''
  }
}

// 处理编辑表单中的地区选择变化
const handleFormRegionChange = (value) => {
  if (value && value.length > 0) {
    // 将级联选择的结果拼接为地区字符串
    scenicForm.location = value.join(' - ')
  } else {
    scenicForm.location = ''
  }
}

// 当编辑已有景点时，尝试从location字符串解析级联值
const parseLocationToRegionValue = (location) => {
  if (!location) return []
  
  const parts = location.split(' - ')
  if (parts.length === 0) return []
  
  // 简单匹配，仅作演示用
  // 实际使用时可能需要更复杂的匹配逻辑
  for (const province of regionOptions.value) {
    if (location.includes(province.value)) {
      for (const city of province.children || []) {
        if (location.includes(city.value)) {
          for (const district of city.children || []) {
            if (location.includes(district.value)) {
              return [province.value, city.value, district.value]
            }
          }
          return [province.value, city.value]
        }
      }
      return [province.value]
    }
  }
  
  return []
}

// 在编辑时自动解析地区
watch(() => scenicForm.location, (newLocation) => {
  if (dialogType.value === 'edit' && newLocation && !scenicForm.regionValue.length) {
    scenicForm.regionValue = parseLocationToRegionValue(newLocation)
  }
}, { immediate: true })

const resetSearch = () => {
  Object.keys(searchForm).forEach(key => {
    if (key === 'categoryId') {
      searchForm[key] = null
    } else if (key === 'regionValue') {
      searchForm[key] = []
    } else {
      searchForm[key] = ''
    }
  })
  currentPage.value = 1
  fetchScenicSpots()
}

const searchCoordinates = () => {
  if (!window.AMap) {
    ElMessage.warning('高德地图API尚未加载完成，请稍后再试')
    return
  }
  
  // 如果表单中已有坐标值，直接使用这些值打开地图
  if (scenicForm.longitude && scenicForm.latitude) {
    // 重置临时数据
    mapSearchKeyword.value = scenicForm.name || scenicForm.location || ''
    searchResults.value = []
    showSearchResults.value = false
    
    // 使用表单中的坐标值
    tempCoordinates.lng = scenicForm.longitude
    tempCoordinates.lat = scenicForm.latitude
    
    // 打开地图对话框
    mapDialogVisible.value = true
    
    // 延迟初始化地图，确保DOM已渲染
    setTimeout(() => {
      initConfirmMap(scenicForm.longitude, scenicForm.latitude, scenicForm.name)
    }, 100)
    
    return
  }
  
  if (!scenicForm.name && !scenicForm.location) {
    ElMessage.warning('请先填写景点名称或地区信息')
    return
  }
  
  const searchText = scenicForm.location || scenicForm.name
  
  // 显示加载中
  const loadingMessage = ElMessage.info({
    message: '正在搜索坐标...',
    duration: 0,
    showClose: true
  })
  
  // 确保Geocoder插件已加载
  if (!window.AMap.Geocoder) {
    window.AMap.plugin('AMap.Geocoder', () => {
      performGeocoding(searchText, loadingMessage)
    })
  } else {
    performGeocoding(searchText, loadingMessage)
  }
}

// 修改performGeocoding函数，使其打开地图对话框
const performGeocoding = (searchText, loadingMessage) => {
  try {
    // 创建地理编码器
    const geocoder = new window.AMap.Geocoder()
    
    // 地理编码
    geocoder.getLocation(searchText, (status, result) => {
      // 关闭加载提示
      loadingMessage.close()
      
      if (status === 'complete' && result.geocodes.length) {
        const location = result.geocodes[0].location
        
        // 重置临时数据
        mapSearchKeyword.value = searchText
        searchResults.value = []
        showSearchResults.value = false
        
        // 保存临时坐标
        tempCoordinates.lng = location.lng
        tempCoordinates.lat = location.lat
        
        // 打开地图对话框让用户确认
        mapDialogVisible.value = true
        // 延迟初始化地图，确保DOM已渲染
        setTimeout(() => {
          initConfirmMap(location.lng, location.lat, searchText)
        }, 100)
      } else {
        ElMessage.error('未能获取到坐标，请尝试更精确的地址描述')
      }
    })
  } catch (error) {
    loadingMessage.close()
    console.error('地理编码错误:', error)
    ElMessage.error(`地理编码失败: ${error.message}`)
  }
}

// 初始化确认地图
const initConfirmMap = (lng, lat, placeName) => {
  if (mapInstance.value) {
    mapInstance.value.destroy()
  }
  
  // 创建地图实例
  mapInstance.value = new window.AMap.Map('confirm-map-container', {
    zoom: 15,
    center: [lng, lat]
  })
  
  // 添加标记
  mapMarker.value = new window.AMap.Marker({
    position: [lng, lat],
    draggable: true, // 可拖动
    title: placeName || '所选位置'
  })
  
  // 监听标记拖动结束事件，更新临时坐标
  mapMarker.value.on('dragend', () => {
    const position = mapMarker.value.getPosition()
    // 使用解构和重新赋值确保响应式更新
    const newCoords = {
      ...tempCoordinates,
      lng: position.lng,
      lat: position.lat
    }
    tempCoordinates.lng = newCoords.lng
    tempCoordinates.lat = newCoords.lat
    
    console.log('标记拖动结束，新坐标:', tempCoordinates.lng, tempCoordinates.lat)
  })
  
  mapInstance.value.add(mapMarker.value)
  
  // 添加控件
  mapInstance.value.plugin(['AMap.ToolBar', 'AMap.Scale'], () => {
    mapInstance.value.addControl(new window.AMap.ToolBar())
    mapInstance.value.addControl(new window.AMap.Scale())
  })
  
  // 初始化输入提示
  initInputTips()
}

// 初始化输入提示
const initInputTips = () => {
  if (!window.AMap) return
  
  // 确保InputTips插件加载
  window.AMap.plugin(['AMap.PlaceSearch', 'AMap.Geocoder'], () => {
    // 创建输入提示实例
    inputTips.value = new window.AMap.PlaceSearch({
      city: '全国', // 搜索范围，可以是城市名、citycode、adcode
      citylimit: false, // 是否限制城市
      pageSize: 10, // 每页结果数,默认10
      pageIndex: 1 // 查询页码,默认1
    })
  })
}

// 监听搜索框输入
const handleSearchInput = () => {
  if (!mapSearchKeyword.value || mapSearchKeyword.value.length < 2) {
    searchResults.value = []
    showSearchResults.value = false
    return
  }
  
  if (!window.AMap || !inputTips.value) {
    return
  }
  
  // 使用输入提示API获取结果
  inputTips.value.search(mapSearchKeyword.value, (status, result) => {
    if (status === 'complete' && result.poiList && result.poiList.pois) {
      searchResults.value = result.poiList.pois
      showSearchResults.value = true
    } else {
      searchResults.value = []
      showSearchResults.value = false
    }
  })
}

// 选择搜索结果
const selectSearchResult = (poi) => {
  if (poi && poi.location) {
    // 更新临时坐标
    tempCoordinates.lng = poi.location.lng
    tempCoordinates.lat = poi.location.lat
    
    // 更新地图位置
    mapInstance.value.setCenter([poi.location.lng, poi.location.lat])
    mapMarker.value.setPosition([poi.location.lng, poi.location.lat])
    
    // 更新搜索框
    mapSearchKeyword.value = poi.name
    
    // 隐藏搜索结果
    showSearchResults.value = false
    
    ElMessage.success('位置已更新')
  }
}

// 确认坐标
const confirmCoordinates = () => {
  console.log('确认坐标，临时坐标值:', tempCoordinates.lng, tempCoordinates.lat)
  scenicForm.longitude = tempCoordinates.lng
  scenicForm.latitude = tempCoordinates.lat
  console.log('表单坐标值已更新:', scenicForm.longitude, scenicForm.latitude)
  
  // 清除临时数据
  tempCoordinates.lng = null
  tempCoordinates.lat = null
  mapSearchKeyword.value = ''
  searchResults.value = []
  showSearchResults.value = false
  
  mapDialogVisible.value = false
  ElMessage.success('坐标已确认')
}

// 取消确认
const cancelCoordinates = () => {
  // 清除临时坐标数据
  tempCoordinates.lng = null
  tempCoordinates.lat = null
  mapSearchKeyword.value = ''
  searchResults.value = []
  showSearchResults.value = false
  mapDialogVisible.value = false
  console.log('对话框已关闭，临时坐标已清除')
}

// 在组件卸载时销毁地图实例
onUnmounted(() => {
  if (mapInstance.value) {
    mapInstance.value.destroy()
  }
})

// 地图搜索位置
const searchMapLocation = () => {
  if (!mapSearchKeyword.value) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  
  if (!window.AMap || !mapInstance.value) {
    ElMessage.warning('地图尚未加载完成')
    return
  }
  
  // 显示加载中
  const loadingMessage = ElMessage.info({
    message: '正在搜索位置...',
    duration: 0,
    showClose: true
  })
  
  // 使用PlaceSearch搜索
  if (!inputTips.value) {
    window.AMap.plugin(['AMap.PlaceSearch'], () => {
      inputTips.value = new window.AMap.PlaceSearch({
        city: '全国',
        citylimit: false
      })
      doSearch(loadingMessage)
    })
  } else {
    doSearch(loadingMessage)
  }
}

// 执行搜索
const doSearch = (loadingMessage) => {
  inputTips.value.search(mapSearchKeyword.value, (status, result) => {
    loadingMessage.close()
    
    if (status === 'complete' && result.poiList && result.poiList.pois && result.poiList.pois.length > 0) {
      const poi = result.poiList.pois[0]
      
      // 更新临时坐标
      tempCoordinates.lng = poi.location.lng
      tempCoordinates.lat = poi.location.lat
      
      // 更新地图位置
      mapInstance.value.setCenter([poi.location.lng, poi.location.lat])
      mapMarker.value.setPosition([poi.location.lng, poi.location.lat])
      
      ElMessage.success('位置已更新')
    } else {
      ElMessage.error('未能获取到坐标，请尝试更精确的地址描述')
    }
  })
}

// 从输入框更新地图位置
const updateMapFromInput = () => {
  if (!mapInstance.value) return
  
  const lng = parseFloat(tempCoordinates.lng)
  const lat = parseFloat(tempCoordinates.lat)
  
  if (isNaN(lng) || isNaN(lat)) {
    ElMessage.warning('请输入有效的经纬度')
    return
  }
  
  // 验证经纬度范围
  if (lng < -180 || lng > 180 || lat < -90 || lat > 90) {
    ElMessage.warning('经纬度超出有效范围')
    return
  }
  
  // 更新地图位置
  mapInstance.value.setCenter([lng, lat])
  mapMarker.value.setPosition([lng, lat])
}
</script>

<style lang="scss" scoped>
.scenic-list-container {
  padding: 20px;
  background-color: #f9fafc;
  min-height: calc(100vh - 120px);

  .page-header {
    margin-bottom: 24px;
    text-align: left;
    
    .page-title {
      font-size: 24px;
      color: #34495e;
      margin: 0 0 8px 0;
      font-weight: 500;
    }
    
    .page-subtitle {
      font-size: 14px;
      color: #7f8c8d;
      margin: 0;
      font-style: italic;
    }
  }

  .action-bar {
    margin-bottom: 20px;
    display: flex;
    justify-content: flex-end;
    align-items: center;
    
    .action-right {
      display: flex;
      justify-content: flex-end;
      
      .add-btn {
        background-color: #2ecc71;
        border-color: #2ecc71;
        
        &:hover, &:focus {
          background-color: #27ae60;
          border-color: #27ae60;
        }
      }
    }
  }

  .search-card {
    margin-bottom: 20px;
    border-radius: 8px;
    background-color: #fff;
    box-shadow: none;
  }
  
  .search-form {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    
    .el-form-item {
      margin-bottom: 10px;
      margin-right: 16px;
    }
    
    .search-btn {
      background-color: #3498db;
      border-color: #3498db;
      
      &:hover, &:focus {
        background-color: #2980b9;
        border-color: #2980b9;
      }
    }
    
    .reset-btn {
      color: #7f8c8d;
      border-color: #bdc3c7;
      
      &:hover, &:focus {
        color: #34495e;
        border-color: #95a5a6;
        background-color: #f5f5f5;
      }
    }
  }

  .table-card {
    border-radius: 8px;
    overflow: hidden;
    box-shadow: none;
    
    .scenic-table {
      border-radius: 4px;
      overflow: hidden;
      
      :deep(thead) {
        background-color: #ecf0f1;
        
        th {
          background-color: #ecf0f1;
          color: #34495e;
          font-weight: 500;
          padding: 12px 0;
        }
      }
      
      :deep(tbody tr) {
        transition: all 0.3s;
        
        &:hover {
          background-color: #f8f9fa;
        }
      }
      
      .scenic-name {
        font-weight: 500;
        color: #2980b9;
      }
      
      .location-tag {
        background-color: #e8f4fd;
        color: #3498db;
        border-color: #3498db;
      }
      
      .price-tag {
        color: #e74c3c;
        font-weight: 500;
      }
      
      .time-tag {
        color: #7f8c8d;
      }
      
      .coordinate-tag {
        font-family: monospace;
        color: #3498db;
        background-color: #eef7fe;
        padding: 2px 6px;
        border-radius: 4px;
        font-size: 12px;
        cursor: pointer;
        
        &:hover {
          background-color: #d6eafc;
        }
      }
      
      .no-coordinate {
        color: #95a5a6;
        font-style: italic;
        font-size: 12px;
      }
      
      .date-text {
        color: #7f8c8d;
        font-size: 12px;
      }
      
      .edit-btn {
        padding: 5px 12px;
        margin-right: 8px;
      }
      
      .delete-btn {
        padding: 5px 12px;
      }
      
      .image-error, .no-image {
        width: 80px;
        height: 60px;
        background-color: #ecf0f1;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #95a5a6;
        border-radius: 4px;
        
        i {
          font-size: 24px;
        }
      }
    }
  }
  
  .pagination-container {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
    padding: 0 20px;
  }

  .scenic-dialog {
    :deep(.el-dialog__header) {
      border-bottom: 1px solid #ecf0f1;
      padding: 20px;
      
      .el-dialog__title {
        font-size: 18px;
        color: #34495e;
        font-weight: 500;
      }
    }
    
    :deep(.el-dialog__body) {
      padding: 30px 20px;
    }
    
    :deep(.el-dialog__footer) {
      border-top: 1px solid #ecf0f1;
      padding: 15px 20px;
    }
    
    .scenic-form {
      .upload-container {
        display: flex;
        flex-direction: column;
        
        .scenic-uploader {
          margin-bottom: 10px;
          
          :deep(.el-upload) {
            border: 1px dashed #d9d9d9;
            border-radius: 6px;
            cursor: pointer;
            position: relative;
            overflow: hidden;
            
            &:hover {
              border-color: #3498db;
            }
          }
          
          .upload-trigger {
            width: 150px;
            height: 150px;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            color: #95a5a6;
            
            i {
              font-size: 28px;
              margin-bottom: 8px;
            }
          }
          
          .preview-image {
            width: 150px;
            height: 150px;
            object-fit: cover;
            border-radius: 4px;
          }
        }
        
        .image-actions {
          display: flex;
          gap: 10px;
        }
      }
      
      .price-prefix {
        color: #e74c3c;
        font-weight: bold;
      }
    }
  }

  .scenic-card {
    &:hover {
      transform: translateY(-5px);
      box-shadow: 0 5px 15px rgba(0, 0, 0, 0.05);
    }
  }

  .map-dialog {
    :deep(.el-dialog__header) {
      border-bottom: 1px solid #ecf0f1;
      padding: 20px;
      
      .el-dialog__title {
        font-size: 18px;
        color: #34495e;
        font-weight: 500;
      }
    }
    
    :deep(.el-dialog__body) {
      padding: 20px;
    }
    
    :deep(.el-dialog__footer) {
      border-top: 1px solid #ecf0f1;
      padding: 15px 20px;
    }
    
    .map-tools {
      margin-bottom: 15px;
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
      
      .search-container {
        flex: 1;
        min-width: 250px;
        position: relative;
        
        .search-results {
          position: absolute;
          top: 100%;
          left: 0;
          right: 0;
          max-height: 300px;
          overflow-y: auto;
          background-color: #fff;
          border: 1px solid #dcdfe6;
          border-radius: 4px;
          box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
          z-index: 2001;
          
          .search-result-item {
            padding: 10px 15px;
            cursor: pointer;
            border-bottom: 1px solid #f0f0f0;
            
            &:last-child {
              border-bottom: none;
            }
            
            &:hover {
              background-color: #f5f7fa;
            }
            
            .result-name {
              font-size: 14px;
              font-weight: 500;
              color: #303133;
            }
            
            .result-address {
              font-size: 12px;
              color: #909399;
              margin-top: 4px;
            }
          }
        }
      }
      
      .coordinates-input {
        display: flex;
        gap: 10px;
        flex-wrap: wrap;
        align-items: center;
        
        .coord-input {
          width: 180px;
        }
      }
    }
    
    .map-container {
      height: 400px;
      width: 100%;
      position: relative;
      border-radius: 8px;
      overflow: hidden;
      box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
      
      .map-tip {
        position: absolute;
        bottom: 10px;
        left: 10px;
        background-color: rgba(255, 255, 255, 0.8);
        padding: 5px 10px;
        border-radius: 4px;
        font-size: 12px;
        color: #666;
        max-width: 80%;
        
        i {
          color: #3498db;
          margin-right: 5px;
        }
      }
    }

    // 添加高德地图自动完成样式
    :deep(.amap-sug-result) {
      z-index: 2001 !important; // 确保下拉列表在对话框上方显示
    }
  }

  // 全局样式，确保自动完成下拉框正常显示
  :deep(.amap-sug-result) {
    border: 1px solid #dcdfe6;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
    border-radius: 4px;
    
    .auto-item {
      padding: 8px 10px;
      cursor: pointer;
      font-size: 14px;
      
      &:hover {
        background-color: #f5f7fa;
      }
    }
  }
}
</style> 