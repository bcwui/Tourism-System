<template>
  <Auth 
    :formData="activeTab === 'account' ? loginForm : emailLoginForm" 
    :rules="activeTab === 'account' ? accountRules : emailRules" 
    :loading="loading"
    submitText="登录"
    @submit="handleSubmit"
    ref="authFormRef"
  >
    <template #form-items>
      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <el-tab-pane label="账号密码登录" name="account">
          <el-form-item prop="username">
            <el-input 
              v-model="loginForm.username"
              :prefix-icon="User"
              placeholder="请输入用户名">
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input 
              v-model="loginForm.password"
              :prefix-icon="Lock"
              type="password"
              placeholder="请输入密码">
            </el-input>
          </el-form-item>
        </el-tab-pane>

        <el-tab-pane label="邮箱登录" name="email">
          <el-form-item prop="email">
            <el-input 
              v-model="emailLoginForm.email"
              :prefix-icon="Message"
              placeholder="请输入邮箱">
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input 
              v-model="emailLoginForm.password"
              :prefix-icon="Lock"
              type="password"
              placeholder="请输入密码">
            </el-input>
          </el-form-item>
        </el-tab-pane>
      </el-tabs>
    </template>

    <template #auth-links>
      <router-link to="/register">立即注册</router-link>
    </template>
  </Auth>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import { User, Lock, Message } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import Auth from './Auth.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const authFormRef = ref(null)
const loading = ref(false)
const activeTab = ref('account')

const loginForm = reactive({
  username: '',
  password: ''
})

const emailLoginForm = reactive({
  email: '',
  password: ''
})

const validateEmail = (rule, value, callback) => {
  if (!value) {
    callback(new Error('邮箱不能为空'))
    return
  }
  
  const emailRegex = /^[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)*@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/
  if (!emailRegex.test(value)) {
    callback(new Error('邮箱格式不正确'))
  } else {
    callback()
  }
}

const accountRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

const emailRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { validator: validateEmail, trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

const handleTabClick = () => {
  // 切换标签时重置表单
  if (authFormRef.value && authFormRef.value.formRef) {
    authFormRef.value.formRef.resetFields()
  }
  
  if (activeTab.value === 'account') {
    // 重置账号密码登录表单
    Object.assign(loginForm, {
      username: '',
      password: ''
    })
  } else if (activeTab.value === 'email') {
    // 重置邮箱登录表单
    Object.assign(emailLoginForm, {
      email: '',
      password: ''
    })
  }
}

const handleSubmit = () => {
  if (!authFormRef.value || !authFormRef.value.formRef) {
    ElMessage.error('表单引用错误')
    return
  }

  const formRef = authFormRef.value.formRef
  
  formRef.validate(async (valid) => {
    if (!valid) return
    
    loading.value = true
    try {
      if (activeTab.value === 'account') {
        // 账号密码登录
        await request.post("/user/login", loginForm, {
          successMsg: "登录成功",
          showDefaultMsg: true,
          onSuccess: handleLoginSuccess
        })
      } else if (activeTab.value === 'email') {
        // 邮箱登录
        await request.post("/user/login/email", emailLoginForm, {
          successMsg: "登录成功",
          showDefaultMsg: true,
          onSuccess: handleLoginSuccess
        })
      }
    } catch (error) {
      console.error('登录失败:', error)
    } finally {
      loading.value = false
    }
  })
}

const handleLoginSuccess = async (data) => {
  userStore.setUserInfo(data)
  
  // 根据返回的角色决定跳转路径
  if (data.roleCode !== 'USER') {
    // 管理员登录，跳转到后台
    await router.isReady()
    router.push(route.query.redirect || '/back/dashboard')
  } else {
    // 普通用户登录，跳转到前台
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  }
}
</script>

<style lang="scss" scoped>
</style> 