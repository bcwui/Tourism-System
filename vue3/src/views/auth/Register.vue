<template>
  <Auth 
    :formData="activeTab === 'account' ? registerForm : emailRegisterForm" 
    :rules="activeTab === 'account' ? accountRules : emailRules" 
    :loading="loading"
    submitText="注册"
    @submit="handleSubmit"
    ref="authFormRef"
  >
    <template #form-items>
      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <el-tab-pane label="账号密码注册" name="account">
          <el-form-item prop="username">
            <el-input 
              v-model="registerForm.username"
              :prefix-icon="User"
              placeholder="请输入用户名">
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input 
              v-model="registerForm.password"
              :prefix-icon="Lock"
              type="password"
              placeholder="请输入密码">
            </el-input>
          </el-form-item>
          <el-form-item prop="confirmPassword">
            <el-input 
              v-model="registerForm.confirmPassword"
              :prefix-icon="Lock"
              type="password"
              placeholder="请确认密码">
            </el-input>
          </el-form-item>
          <el-form-item prop="email">
            <el-input 
              v-model="registerForm.email"
              :prefix-icon="Message"
              placeholder="请输入邮箱">
            </el-input>
          </el-form-item>
          <el-form-item prop="phone">
            <el-input 
              v-model="registerForm.phone"
              :prefix-icon="Phone"
              placeholder="请输入手机号（选填）">
            </el-input>
          </el-form-item>
          <el-form-item prop="nickname">
            <el-input 
              v-model="registerForm.nickname"
              :prefix-icon="UserFilled"
              placeholder="请输入昵称">
            </el-input>
          </el-form-item>
        </el-tab-pane>

        <el-tab-pane label="邮箱验证码注册" name="email">
          <el-form-item prop="email">
            <el-input 
              v-model="emailRegisterForm.email"
              :prefix-icon="Message"
              placeholder="请输入邮箱">
              <template #append>
                <el-button 
                  :disabled="isSendingEmailCode || emailCountdown > 0" 
                  @click="sendEmailVerifyCode"
                >
                  {{ emailCountdown > 0 ? `${emailCountdown}秒后重新获取` : '获取验证码' }}
                </el-button>
              </template>
            </el-input>
          </el-form-item>
          
          <el-form-item prop="verifyCode">
            <el-input 
              v-model="emailRegisterForm.verifyCode"
              :prefix-icon="Key"
              placeholder="请输入验证码">
            </el-input>
          </el-form-item>
          
          <el-form-item prop="password">
            <el-input 
              v-model="emailRegisterForm.password"
              :prefix-icon="Lock"
              type="password"
              placeholder="请输入密码">
            </el-input>
          </el-form-item>
          
          <el-form-item prop="confirmPassword">
            <el-input 
              v-model="emailRegisterForm.confirmPassword"
              :prefix-icon="Lock"
              type="password"
              placeholder="请确认密码">
            </el-input>
          </el-form-item>
          
          <el-form-item prop="username">
            <el-input 
              v-model="emailRegisterForm.username"
              :prefix-icon="User"
              placeholder="请输入用户名（选填）">
            </el-input>
          </el-form-item>

          <el-form-item prop="nickname">
            <el-input 
              v-model="emailRegisterForm.nickname"
              :prefix-icon="UserFilled"
              placeholder="请输入昵称（选填）">
            </el-input>
          </el-form-item>
        </el-tab-pane>
      </el-tabs>
    </template>

    <template #auth-links>
      已有账号？<router-link to="/login">立即登录</router-link>
    </template>
  </Auth>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, Message, Phone, UserFilled, Key } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import Auth from './Auth.vue'

const router = useRouter()
const authFormRef = ref(null)
const loading = ref(false)
const activeTab = ref('account')
const isSendingEmailCode = ref(false)
const emailCountdown = ref(0)

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: '',
  nickname: '',
  roleCode: 'USER', // 默认注册为普通用户
})

const emailRegisterForm = reactive({
  email: '',
  verifyCode: '',
  password: '',
  confirmPassword: '',
  username: '',
  nickname: '',
  roleCode: 'USER' // 默认注册为普通用户
})

const validateAccountPass2 = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const validateEmailPass2 = (rule, value, callback) => {
  if (value !== emailRegisterForm.password) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

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

const validatePhone = (rule, value, callback) => {
  if (value && !/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('手机号格式不正确'))
  } else {
    callback()
  }
}

// 账号密码注册的表单验证规则
const accountRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度必须在3到50个字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度必须在6到100个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateAccountPass2, trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { validator: validateEmail, trigger: 'blur' }
  ],
  phone: [
    { validator: validatePhone, trigger: 'blur' }
  ],
  nickname: [
    { required: false }
  ]
}

// 邮箱验证码注册的表单验证规则
const emailRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { validator: validateEmail, trigger: 'blur' }
  ],
  verifyCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { min: 6, max: 6, message: '验证码长度必须为6位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度必须在6到100个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateEmailPass2, trigger: 'blur' }
  ],
  username: [
    { required: false }
  ],
  nickname: [
    { required: false }
  ]
}

const handleTabClick = () => {
  // 切换标签时重置表单
  if (authFormRef.value && authFormRef.value.formRef) {
    authFormRef.value.formRef.resetFields()
  }
  
  if (activeTab.value === 'account') {
    // 重置账号密码注册表单
    Object.assign(registerForm, {
      username: '',
      password: '',
      confirmPassword: '',
      email: '',
      phone: '',
      nickname: '',
      roleCode: 'USER'
    })
  } else if (activeTab.value === 'email') {
    // 重置邮箱注册表单
    Object.assign(emailRegisterForm, {
      email: '',
      verifyCode: '',
      password: '',
      confirmPassword: '',
      username: '',
      nickname: '',
      roleCode: 'USER'
    })
  }
}

const startEmailCountdown = () => {
  emailCountdown.value = 60
  const timer = setInterval(() => {
    emailCountdown.value--
    if (emailCountdown.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)
}

const sendEmailVerifyCode = async () => {
  if (!emailRegisterForm.email) {
    ElMessage.error('请输入邮箱')
    return
  }
  
  const emailRegex = /^[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)*@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/
  if (!emailRegex.test(emailRegisterForm.email)) {
    ElMessage.error('邮箱格式不正确')
    return
  }
  
  isSendingEmailCode.value = true
  try {
    await request.get(`/email/code/${emailRegisterForm.email}`, null, {
      successMsg: '验证码已发送',
      onSuccess: () => {
        // 开始倒计时
        startEmailCountdown()
        // 不再自动填充验证码
      }
    })
  } catch (error) {
    console.error('发送邮箱验证码失败:', error)
  } finally {
    isSendingEmailCode.value = false
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
        const { confirmPassword, ...registerData } = registerForm
        await request.post("/user/add", registerData, {
          successMsg: "注册成功",
          showDefaultMsg: true,
          onSuccess: () => {
            router.push('/login')
          }
        })
      } else if (activeTab.value === 'email') {
        // 从表单数据中提取验证码和用户数据
        const { confirmPassword, verifyCode, ...userData } = emailRegisterForm
        
        // 使用查询参数传递验证码，而不是包含在请求体中
        await request.post("/user/register/email?verifyCode=" + verifyCode, userData, {
          successMsg: "注册成功",
          showDefaultMsg: true,
          onSuccess: () => {
            router.push('/login')
          }
        })
      }
    } catch (error) {
      console.error('注册失败:', error)
    } finally {
      loading.value = false
    }
  })
}
</script>

<style lang="scss" scoped>
// 如有必要，可添加特定于注册页面的样式
</style> 