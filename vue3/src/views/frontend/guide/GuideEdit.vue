<template>
  <div class="guide-edit-container">
    <div class="page-header">
      <h1>{{ form.id ? '编辑攻略' : '发布新攻略' }}</h1>
      <p>分享您的旅行经历，帮助更多人探索世界的美好</p>
    </div>
    
    <div class="edit-form-container">
      <el-form 
        :model="form" 
        label-width="80px" 
        ref="formRef"
        class="edit-form"
      >
        <el-form-item label="标题" prop="title" required>
          <el-input 
            v-model="form.title" 
            placeholder="请输入攻略标题（建议30字以内）" 
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="封面" prop="coverImage" required>
          <div class="cover-uploader-container">
            <el-upload
              class="cover-uploader"
              action="#"
              :show-file-list="false"
              :http-request="customUploadCover"
              :before-upload="beforeCoverUpload"
            >
              <div v-if="form.coverImage" class="cover-preview-container">
                <img :src="getImageUrl(form.coverImage)" class="cover-preview" />
                <div class="cover-hover-mask">
                  <el-icon class="upload-icon"><EditPen /></el-icon>
                  <span>更换封面</span>
                </div>
              </div>
              <div v-else class="upload-placeholder">
                <el-icon class="upload-icon"><Plus /></el-icon>
                <span>上传封面图片</span>
                <div class="upload-tip">推荐尺寸: 900×600px, JPG/PNG格式</div>
              </div>
            </el-upload>
            <div class="form-tips" v-if="!form.coverImage">
              添加一张精美的封面图能吸引更多读者
            </div>
          </div>
        </el-form-item>
        
        <el-form-item label="内容" prop="content" required>
          <div class="editor-container">
            <WangEditor v-model="form.content" />
            <div class="form-tips">
              支持文字、图片、视频等多媒体内容，可插入旅游景点图片丰富您的攻略
            </div>
          </div>
        </el-form-item>
        
        <el-form-item>
          <div class="form-actions">
            <el-button @click="goBack">取消</el-button>
            <el-button type="primary" @click="submit" :loading="submitting">
              {{ form.id ? '保存修改' : '发布攻略' }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import WangEditor from '@/components/WangEditor.vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'


const baseAPI = process.env.VUE_APP_BASE_API || '/api'
const form = reactive({
  title: '',
  coverImage: '',
  content: '',
  id: ''
})
const formRef = ref(null)
const route = useRoute()
const router = useRouter()
const submitting = ref(false)
const userStore = useUserStore()

// 获取图片完整URL
const getImageUrl = (url) => {
  if (!url) return ''
  return url.startsWith('http') ? url : baseAPI + url
}

onMounted(async () => {
  if (route.query.id) {
    try {
      await request.get(`/guide/${route.query.id}`, {}, {
        showDefaultMsg: false,
        onSuccess: (res) => {
          form.title = res.title
          form.coverImage = res.coverImage
          form.content = res.content
          form.id = res.id
        }
      })
    } catch (error) {
      ElMessage.error('获取攻略信息失败')
      console.error('获取攻略信息失败', error)
    }
  }
})

const beforeCoverUpload = (file) => {
  const isJPG = file.type === 'image/jpeg'
  const isPNG = file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isJPG && !isPNG) {
    ElMessage.error('封面只能是 JPG 或 PNG 格式!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('封面大小不能超过 2MB!')
    return false
  }
  return true
}

const customUploadCover = async (options) => {
  try {
    const { file } = options
    const formData = new FormData()
    formData.append('file', file)
    await request.post('/file/upload/img', formData, {
      headers: { token: localStorage.getItem('token') || '' },
      transformRequest: [(data) => data],
      successMsg: '封面上传成功',
      errorMsg: '封面上传失败',
      onSuccess: (data) => {
        form.coverImage = data
        options.onSuccess({ data })
      },
      onError: (error) => {
        options.onError(new Error(error.message || '上传失败'))
      }
    })
  } catch (error) {
    options.onError(error)
  }
}

const submit = async () => {
  if (!form.title.trim()) {
    return ElMessage.warning('请输入攻略标题')
  }
  
  if (!form.coverImage) {
    return ElMessage.warning('请上传攻略封面图片')
  }
  
  if (!form.content || form.content === '<p><br></p>') {
    return ElMessage.warning('请填写攻略内容')
  }
  
  submitting.value = true
  form.userId = userStore.userInfo.id
  try {
    if (form.id) {
      await request.put('/guide/update', form, {
        successMsg: '修改成功',
        onSuccess: () => {
          router.push({ name: 'MyGuideList' })
        }
      })
    } else {
      await request.post('/guide/add', form, {
        successMsg: '发布成功',
        onSuccess: () => {
          form.title = ''
          form.coverImage = ''
          form.content = ''
          router.push({ name: 'MyGuideList' })
        }
      })
    }
  } catch (error) {
    console.error('提交失败', error)
  } finally {
    submitting.value = false
  }
}

const goBack = () => {
  router.back()
}
</script>

<style lang="scss" scoped>
.guide-edit-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 30px;
  
  h1 {
    font-size: 28px;
    font-weight: 600;
    color: #333;
    margin: 0 0 10px;
  }
  
  p {
    font-size: 16px;
    color: #666;
    max-width: 600px;
    margin: 0 auto;
  }
}

.edit-form-container {
  background: #fff;
  border-radius: 8px;
  padding: 30px;
}

.edit-form {
  .el-form-item {
    margin-bottom: 25px;
  }
}

.cover-uploader-container {
  width: 100%;
}

.cover-uploader {
  :deep(.el-upload) {
    cursor: pointer;
    position: relative;
    overflow: hidden;
    transition: all 0.3s;
    width: 100%;
    max-width: 360px;
  }
  
  .cover-preview-container {
    position: relative;
    width: 360px;
    height: 200px;
    border-radius: 8px;
    overflow: hidden;
    transition: all 0.3s;
    
    &:hover {
      .cover-hover-mask {
        opacity: 1;
      }
    }
    
    .cover-preview {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    
    .cover-hover-mask {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.5);
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      opacity: 0;
      transition: opacity 0.3s;
      color: #fff;
      
      .upload-icon {
        font-size: 24px;
        margin-bottom: 8px;
      }
    }
  }
  
  .upload-placeholder {
    width: 360px;
    height: 200px;
    border: 1px dashed #d9d9d9;
    border-radius: 8px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    color: #8c939d;
    transition: all 0.3s;
    
    &:hover {
      border-color: #409EFF;
      color: #409EFF;
    }
    
    .upload-icon {
      font-size: 28px;
      margin-bottom: 8px;
    }
    
    .upload-tip {
      font-size: 12px;
      margin-top: 8px;
      color: #999;
    }
  }
}

.form-tips {
  font-size: 13px;
  color: #909399;
  margin-top: 8px;
  line-height: 1.4;
}

.editor-container {
  :deep(.w-e-text-container) {
    min-height: 400px;
    max-height: 600px;
  }
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  
  .el-button {
    min-width: 100px;
    margin-left: 16px;
  }
}

@media (max-width: 768px) {
  .guide-edit-container {
    padding: 15px;
  }
  
  .edit-form-container {
    padding: 20px 15px;
  }
  
  .cover-uploader {
    .cover-preview-container,
    .upload-placeholder {
      width: 100%;
      max-width: 100%;
    }
  }
  
  .form-actions {
    flex-direction: column-reverse;
    
    .el-button {
      width: 100%;
      margin: 8px 0;
    }
  }
}
</style> 