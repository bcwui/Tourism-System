<template>
  <div class="auth-container">
    <div class="auth-box">
      <div class="auth-header" v-if="showHeader">
        <div class="logo"><el-icon><Place /></el-icon></div>
        <h1 class="title">旅游信息系统</h1>
        <div class="subtitle">TRAVEL INFORMATION SYSTEM</div>
      </div>
      
      <el-form :model="formData" :rules="rules" ref="formRef" class="auth-form">
        <slot name="form-items"></slot>
        
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit" class="auth-button">
            {{ submitText }}
          </el-button>
        </el-form-item>
        
        <div class="auth-links">
          <slot name="auth-links"></slot>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Place } from '@element-plus/icons-vue'

const props = defineProps({
  formData: {
    type: Object,
    required: true
  },
  rules: {
    type: Object,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  },
  submitText: {
    type: String,
    default: '提交'
  },
  showHeader: {
    type: Boolean,
    default: true
  }
})

const formRef = ref(null)

const emit = defineEmits(['submit'])

const handleSubmit = () => {
  formRef.value.validate(valid => {
    if (valid) {
      emit('submit', formRef)
    }
  })
}

defineExpose({
  formRef
})
</script>

<style lang="scss" scoped>
.auth-container {
  height: 100vh;
  width: 100vw;
  display: flex;
  justify-content: center;
  align-items: center;
  background-image: url('@/assets/bg.jpg');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  position: relative;
  overflow: hidden;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(0, 0, 0, 0.156);
    // backdrop-filter: blur(8px);
    z-index: 1;
  }
}

.auth-box {
  width: 380px;
  padding: 35px;
  background-color: rgba(255, 255, 255, 0.85);
  border-radius: 12px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.3);
  z-index: 2;
  position: relative;
  backdrop-filter: blur(5px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  animation: fadeIn 0.6s ease-out;
  
  &::before {
    content: '';
    position: absolute;
    top: -10px;
    left: -10px;
    right: -10px;
    bottom: -10px;
    background: linear-gradient(135deg, rgba(255,255,255,0.2) 0%, rgba(255,255,255,0) 100%);
    border-radius: 16px;
    z-index: -1;
    pointer-events: none;
  }
}

.auth-header {
  text-align: center;
  margin-bottom: 20px;
  
  .logo {
    font-size: 42px;
    color: #3490dc;
    margin-bottom: 10px;
    
    .el-icon {
      font-size: 42px;
    }
  }
  
  .title {
    font-size: 22px;
    font-weight: 600;
    color: #333;
    margin: 0 0 5px;
  }
  
  .subtitle {
    font-size: 13px;
    color: #888;
    letter-spacing: 1px;
  }
}

.auth-form {
  .el-form-item {
    margin-bottom: 16px;
  }
  
  :deep(.el-input__inner) {
    padding-left: 40px;
    height: 44px;
    border-radius: 22px;
    background-color: rgba(255, 255, 255, 0.8);
    border: 1px solid #dcdfe6;
    
    &:focus {
      border-color: #3490dc;
      box-shadow: 0 0 0 2px rgba(52, 144, 220, 0.2);
    }
  }
  
  :deep(.el-input__prefix) {
    left: 12px;
    font-size: 16px;
    color: #3490dc;
  }
}

.auth-button {
  width: 100%;
  height: 44px;
  border-radius: 22px;
  font-size: 16px;
  background-color: #3490dc;
  border-color: #3490dc;
  
  &:hover, &:focus {
    background-color: #2779bd;
    border-color: #2779bd;
  }
}

.auth-links {
  margin-top: 16px;
  text-align: center;
  
  a {
    color: #3490dc;
    text-decoration: none;
    transition: color 0.3s;
    
    &:hover {
      color: #2779bd;
      text-decoration: underline;
    }
  }
}

@media (max-width: 576px) {
  .auth-box {
    width: 85%;
    max-width: 320px;
    padding: 25px 18px;
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>