<template>
  <div class="profile-container">
    <!-- 现代化页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">
          <span class="title-icon">👤</span>
          个人中心
        </h1>
        <p class="page-subtitle">
          管理您的个人信息和账户设置
        </p>
      </div>
  
    </div>

    <!-- 现代化标签页区域 -->
    <div class="profile-section">
      <div class="section-container">
          <div class="profile-tabs">
            <el-tabs
            v-model="activeTab"
            class="modern-tabs"
          >
            <!-- 基本信息标签页 -->
            <el-tab-pane label="基本信息" name="basic">
              <template #label>
                <div class="tab-label">
                  <el-icon><User /></el-icon>
                  <span>基本信息</span>
                </div>
              </template>

              <div class="profile-content">
                <div class="profile-layout">
                  <!-- 头像区域 -->
                  <div class="avatar-section">
                    <div class="avatar-card">
                      <div class="avatar-wrapper">
                        <el-avatar :size="120" :src="avatarUrl" class="user-avatar">
                          <span class="avatar-fallback">{{ userForm.nickname?.charAt(0) || userForm.username?.charAt(0) || '用' }}</span>
                        </el-avatar>
                        <div class="avatar-overlay">
                          <el-upload
                            class="avatar-uploader"
                            action="#"
                            :auto-upload="true"
                            :show-file-list="false"
                            :http-request="customUploadAvatar"
                            :before-upload="beforeAvatarUpload"
                          >
                            <el-button type="primary" :icon="Camera" circle class="upload-btn">
                            </el-button>
                          </el-upload>
                        </div>
                      </div>
                      <div class="avatar-info">
                        <h3 class="user-name">{{ userForm.nickname || userForm.username }}</h3>
                        <p class="user-desc">点击头像更换</p>
                      </div>
                    </div>
                  </div>

                  <!-- 表单区域 -->
                  <div class="form-section">
                    <div class="form-card">
                      <div class="form-header">
                        <h3 class="form-title">
                          <el-icon><Edit /></el-icon>
                          编辑个人信息
                        </h3>
                      </div>

                      <el-form
                        ref="userFormRef"
                        :model="userForm"
                        :rules="rules"
                        label-width="100px"
                        class="modern-form"
                      >
                        <div class="form-row">
                          <el-form-item label="用户名" prop="username" class="form-item">
                            <el-input
                              v-model="userForm.username"
                              disabled
                              class="form-input"
                              :prefix-icon="User"
                            />
                          </el-form-item>

                          <el-form-item label="昵称" prop="nickname" class="form-item">
                            <el-input
                              v-model="userForm.nickname"
                              class="form-input"
                              :prefix-icon="EditPen"
                              placeholder="请输入昵称"
                            />
                          </el-form-item>
                        </div>

                        <div class="form-row">
                          <el-form-item label="性别" prop="sex" class="form-item">
                            <el-radio-group v-model="userForm.sex" class="gender-group">
                              <el-radio label="男" class="gender-radio">
                                <el-icon><Male /></el-icon>
                                <span>男</span>
                              </el-radio>
                              <el-radio label="女" class="gender-radio">
                                <el-icon><Female /></el-icon>
                                <span>女</span>
                              </el-radio>
                            </el-radio-group>
                          </el-form-item>

                          <el-form-item label="电子邮箱" prop="email" class="form-item">
                            <el-input
                              v-model="userForm.email"
                              class="form-input"
                              :prefix-icon="Message"
                              placeholder="请输入邮箱地址"
                            />
                          </el-form-item>
                        </div>

                        <div class="form-row">
                          <el-form-item label="手机号码" prop="phone" class="form-item full-width">
                            <el-input
                              v-model="userForm.phone"
                              class="form-input"
                              :prefix-icon="Phone"
                              placeholder="请输入手机号码"
                            />
                          </el-form-item>
                        </div>

                        <div class="form-actions">
                          <el-button type="primary" @click="submitUserInfo" class="save-btn" size="large">
                            <el-icon><Check /></el-icon>
                            保存修改
                          </el-button>
                        </div>
                      </el-form>
                    </div>
                  </div>
                </div>
              </div>
            </el-tab-pane>

            <!-- 修改密码标签页 -->
            <el-tab-pane label="修改密码" name="password">
              <template #label>
                <div class="tab-label">
                  <el-icon><Lock /></el-icon>
                  <span>修改密码</span>
                </div>
              </template>

              <div class="password-content">
                <div class="password-card">
                  <div class="password-header">
                    <div class="security-icon">🔐</div>
                    <h3 class="password-title">账户安全</h3>
                    <p class="password-desc">定期更换密码，保护账户安全</p>
                  </div>

                  <el-form
                    ref="passwordFormRef"
                    :model="passwordForm"
                    :rules="passwordRules"
                    label-width="120px"
                    class="password-form"
                  >
                    <el-form-item label="旧密码" prop="oldPassword" class="form-item">
                      <el-input
                        v-model="passwordForm.oldPassword"
                        show-password
                        placeholder="请输入旧密码"
                        class="form-input"
                        :prefix-icon="Lock"
                      />
                    </el-form-item>

                    <el-form-item label="新密码" prop="newPassword" class="form-item">
                      <el-input
                        v-model="passwordForm.newPassword"
                        show-password
                        placeholder="请输入新密码"
                        class="form-input"
                        :prefix-icon="Key"
                      />
                    </el-form-item>

                    <el-form-item label="确认新密码" prop="confirmPassword" class="form-item">
                      <el-input
                        v-model="passwordForm.confirmPassword"
                        show-password
                        placeholder="请再次输入新密码"
                        class="form-input"
                        :prefix-icon="Key"
                      />
                    </el-form-item>

                    <div class="form-actions">
                      <el-button type="primary" @click="submitPassword" class="save-btn" size="large">
                        <el-icon><Check /></el-icon>
                        修改密码
                      </el-button>
                    </div>
                  </el-form>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useUserStore } from "@/store/user";
import request from "@/utils/request";
import {User,Edit,Lock,Key,Check,Camera,Male,Female,Phone,Message,EditPen} from '@element-plus/icons-vue'

const baseAPI = process.env.VUE_APP_BASE_API || "/api";
const userStore = useUserStore();
const activeTab = ref("basic");

// 表单引用
const userFormRef = ref(null);
const passwordFormRef = ref(null);

// 用户表单数据
const userForm = reactive({
  id: "",
  username: "",
  nickname: "",
  email: "",
  phone: "",
  sex: "",
  avatar: "",
});

// 头像地址
const avatarUrl = computed(() => {
  return baseAPI + userForm.avatar;
});

// 密码表单数据
const passwordForm = reactive({
  oldPassword: "",
  newPassword: "",
  confirmPassword: "",
});

// 表单校验规则
const rules = {
  name: [{ required: true, message: "请输入姓名", trigger: "blur" }],
  email: [
    { required: true, message: "请输入邮箱地址", trigger: "blur" },
    {
      type: "email",
      message: "请输入正确的邮箱地址",
      trigger: ["blur", "change"],
    },
  ],
  phone: [
    { required: false, trigger: "blur" },
    {
      pattern: /^1[3-9]\d{9}$/,
      message: "请输入正确的手机号码",
      trigger: ["blur", "change"],
    },
  ],
};

// 密码表单校验规则
const passwordRules = {
  oldPassword: [
    { required: true, message: "请输入旧密码", trigger: "blur" },
    { min: 6, message: "密码长度不能小于6个字符", trigger: "blur" },
  ],
  newPassword: [
    { required: true, message: "请输入新密码", trigger: "blur" },
    { min: 6, message: "密码长度不能小于6个字符", trigger: "blur" },
  ],
  confirmPassword: [
    { required: true, message: "请再次输入新密码", trigger: "blur" },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error("两次输入的密码不一致"));
        } else {
          callback();
        }
      },
      trigger: ["blur", "change"],
    },
  ],
};

// 获取用户信息
const getUserInfo = async () => {
  try {
    // 如果用户已登录，从 store 中获取用户信息
    if (userStore.isLoggedIn) {
      // 从后端重新获取最新的用户信息
      const response = await request.get("/user/current", null, {
        showDefaultMsg: false,
      });

      // 确保返回数据存在
      if (response) {
        // 更新store中的用户信息
        userStore.updateUserInfo(response);

        // 直接更新表单数据
        userForm.id = response.id || "";
        userForm.username = response.username || "";
        userForm.nickname = response.nickname || "";
        userForm.email = response.email || "";
        userForm.phone = response.phone || "";
        userForm.sex = response.sex || "男";
        userForm.avatar = response.avatar || "";

        console.log("用户信息加载成功:", userForm);
      }
    }
  } catch (error) {
    console.error("获取用户信息失败", error);
    ElMessage.error("获取用户信息失败");
  }
};

// 上传头像前的校验
const beforeAvatarUpload = (file) => {
  const isJPG = file.type === "image/jpeg";
  const isPNG = file.type === "image/png";
  const isLt2M = file.size / 1024 / 1024 < 2;

  if (!isJPG && !isPNG) {
    ElMessage.error("头像只能是 JPG 或 PNG 格式!");
    return false;
  }
  if (!isLt2M) {
    ElMessage.error("头像大小不能超过 2MB!");
    return false;
  }
  return true;
};

// 自定义头像上传方法
const customUploadAvatar = async (options) => {
  try {
    const { file } = options;

    // 创建 FormData 对象
    const formData = new FormData();
    formData.append("file", file);

    // 设置自定义上传选项
    const uploadOptions = {
      headers: {
        token: localStorage.getItem("token") || "",
      },
      // 不进行JSON处理
      transformRequest: [(data) => data],
      // 自定义成功消息
      successMsg: "头像上传成功",
      // 自定义错误消息
      errorMsg: "头像上传失败",
      // 成功回调
      onSuccess: async (data) => {
        // 更新用户头像
        userForm.avatar = data;

        // 保存到后端
        await updateUserAvatar(data);

        // 通知上传成功
        options.onSuccess({ data });
      },
      // 错误回调
      onError: (error) => {
        console.error("头像上传错误:", error);
        options.onError(new Error(error.message || "上传失败"));
      },
    };

    // 发送上传请求
    await request.post("/file/upload/img", formData, uploadOptions);
  } catch (error) {
    options.onError(error);
    console.error("头像上传过程发生错误:", error);
  }
};

// 更新用户头像信息
const updateUserAvatar = async (avatarPath) => {
  try {
    await request.put(
      `/user/${userForm.id}`,
      { avatar: avatarPath },
      {
        showDefaultMsg: false,
        onSuccess: (data) => {
          // 更新本地用户信息
          const updatedUserInfo = { ...userStore.userInfo, avatar: avatarPath };
          userStore.updateUserInfo(updatedUserInfo);
        },
        onError: (error) => {
          console.error("头像信息保存失败", error);
          ElMessage.error("头像信息保存失败");
        },
      }
    );
  } catch (error) {
    console.error("头像信息保存失败", error);
    ElMessage.error("头像信息保存失败");
    throw error;
  }
};

// 提交用户信息更新
const submitUserInfo = async () => {
  if (!userFormRef.value) return;

  try {
    // 表单验证
    await userFormRef.value.validate();

    await request.put(
      `/user/${userForm.id}`,
      {
        name: userForm.name,
        email: userForm.email,
        phone: userForm.phone,
        sex: userForm.sex,
      },
      {
        showDefaultMsg: false,
        successMsg: "个人信息更新成功!",
        onSuccess: (data) => {
          // 更新本地用户信息
          const updatedUserInfo = {
            ...userStore.userInfo,
            name: userForm.name,
            email: userForm.email,
            phone: userForm.phone,
            sex: userForm.sex,
          };
          userStore.updateUserInfo(updatedUserInfo);
        },
        onError: (error) => {
          console.error("用户信息更新失败", error);
          ElMessage.error("用户信息更新失败");
        },
      }
    );

  } catch (error) {
    console.error("保存个人信息失败", error);
    ElMessage.error("保存个人信息失败");
  }
};

// 提交密码修改
const submitPassword = async () => {
  if (!passwordFormRef.value) return;

  try {
    // 表单验证
    await passwordFormRef.value.validate();

    await request.put(
      `/user/password/${userForm.id}`,
      {
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword,
      },
      {
        showDefaultMsg: false,

        onSuccess: (data) => {
          // 清空表单
          passwordForm.oldPassword = "";
          passwordForm.newPassword = "";
          passwordForm.confirmPassword = "";

          // 提示用户重新登录
          ElMessageBox.confirm("密码已修改，需要重新登录", "提示", {
            confirmButtonText: "重新登录",
            cancelButtonText: "取消",
            type: "warning",
          }).then(() => {
            // 清除用户信息并跳转到登录页
            userStore.clearUserInfo();
            window.location.href = "/login";
          });
        },
        onError: (error) => {
          console.error("密码信息保存失败", error);
          ElMessage.error("密码信息保存失败");
        },
      }
    );
  } catch (error) {
    console.error("密码修改失败", error);
    ElMessage.error(error.message || "密码修改失败");
  }
};

// 监听用户表单数据变化
watch(
  userForm,
  (newVal) => {
    console.log("用户表单数据变化:", newVal);
  },
  { deep: true }
);

// 组件挂载时获取用户信息
onMounted(() => {
  getUserInfo();
});
</script>

<style lang="scss" scoped>
.profile-container {
  min-height: 100vh;
  background: #f8fafc;
  font-family: "思源黑体", "Source Han Sans", "Noto Sans CJK SC", sans-serif;
  color: #333;

  // 通用容器样式
  .section-container {
    max-width: 1300px;
    margin: 0 auto;
    padding: 40px 20px;
  }

  // 页面头部
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 40px;
    padding: 40px 0 20px;
    border-bottom: 1px solid #e2e8f0;
  }

  .header-content {
    flex: 1;
  }

  .page-title {
    font-size: 36px;
    font-weight: 700;
    margin: 0 0 8px;
    color: #2d3748;
    display: flex;
    align-items: center;
    gap: 12px;

    .title-icon {
      font-size: 32px;
    }
  }

  .page-subtitle {
    font-size: 16px;
    color: #64748b;
    margin: 0;
  }




  // 个人资料区域
  .profile-section {
    background: white;
    margin: 0;
    padding-top: 20px;
  }

  .profile-tabs {
    background: white;
    border-radius: 16px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
    overflow: hidden;
    border: 1px solid #e2e8f0;
  }

  // 现代化标签页样式
  .modern-tabs {
    :deep(.el-tabs__header) {
      margin: 0;
      background: #f8fafc;
      border-bottom: 1px solid #e2e8f0;
    }

    :deep(.el-tabs__nav-wrap) {
      padding: 0 24px;
    }

    :deep(.el-tabs__item) {
      padding: 20px 0;
      font-size: 16px;
      font-weight: 600;
      color: #64748b;
      border: none;
      margin-right: 40px;

      &.is-active {
        color: #667eea;
      }

      &:hover {
        color: #667eea;
      }
    }

    :deep(.el-tabs__active-bar) {
      background: linear-gradient(45deg, #667eea, #764ba2);
      height: 3px;
    }

    :deep(.el-tabs__content) {
      padding: 40px 24px;
    }

    .tab-label {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }

  // 个人资料内容
  .profile-content {
    .profile-layout {
      display: grid;
      grid-template-columns: 1fr 2fr;
      gap: 40px;
      align-items: start;
    }
  }

  // 头像区域
  .avatar-section {
    .avatar-card {
      background: white;
      border-radius: 16px;
      padding: 30px;
      text-align: center;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
      border: 1px solid #e2e8f0;
    }

    .avatar-wrapper {
      position: relative;
      display: inline-block;
      margin-bottom: 20px;

      .user-avatar {
        border: 4px solid #fff;
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        font-size: 48px;
        font-weight: 700;
      }

      .avatar-overlay {
        position: absolute;
        bottom: -5px;
        right: -5px;

        .upload-btn {
          background: linear-gradient(45deg, #667eea, #764ba2);
          border: 3px solid white;
          box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);

          &:hover {
            transform: scale(1.1);
          }
        }
      }
    }

    .avatar-info {
      .user-name {
        font-size: 20px;
        font-weight: 700;
        color: #2d3748;
        margin: 0 0 8px;
      }

      .user-desc {
        font-size: 14px;
        color: #64748b;
        margin: 0;
      }
    }
  }

  // 表单区域
  .form-section {
    .form-card {
      background: white;
      border-radius: 16px;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
      border: 1px solid #e2e8f0;
      overflow: hidden;
    }

    .form-header {
      padding: 24px 24px 0;
      border-bottom: 1px solid #f1f5f9;
      margin-bottom: 24px;

      .form-title {
        font-size: 20px;
        font-weight: 700;
        color: #2d3748;
        margin: 0;
        display: flex;
        align-items: center;
        gap: 8px;

        .el-icon {
          color: #667eea;
        }
      }
    }

    .modern-form {
      padding: 0 24px 24px;

      .form-row {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 20px;
        margin-bottom: 20px;
      }

      .form-item {
        &.full-width {
          grid-column: 1 / -1;
        }

        :deep(.el-form-item__label) {
          font-weight: 600;
          color: #2d3748;
          margin-bottom: 8px;
          font-size: 14px;
        }

        .form-input {
          :deep(.el-input__wrapper) {
            border-radius: 8px;
            border: 2px solid #e2e8f0;
            transition: all 0.3s ease;
            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);

            &:hover {
              border-color: #667eea;
            }

            &.is-focus {
              border-color: #667eea;
              box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
            }
          }
        }

        .gender-group {
          display: flex;
          gap: 20px;

          .gender-radio {
            :deep(.el-radio__label) {
              display: flex;
              align-items: center;
              gap: 4px;
              font-weight: 500;
            }
          }
        }
      }

      .form-actions {
        display: flex;
        justify-content: center;
        margin-top: 32px;

        .save-btn {
          background: linear-gradient(45deg, #667eea, #764ba2);
          border: none;
          border-radius: 12px;
          font-weight: 600;
          padding: 12px 32px;
          box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
          transition: all 0.3s ease;

          &:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6);
          }
        }
      }
    }
  }

  // 密码修改内容
  .password-content {
    max-width: 600px;
    margin: 0 auto;

    .password-card {
      background: white;
      border-radius: 16px;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
      border: 1px solid #e2e8f0;
      overflow: hidden;
    }

    .password-header {
      text-align: center;
      padding: 40px 24px 24px;
      background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);

      .security-icon {
        font-size: 48px;
        margin-bottom: 16px;
      }

      .password-title {
        font-size: 24px;
        font-weight: 700;
        color: #2d3748;
        margin: 0 0 8px;
      }

      .password-desc {
        font-size: 14px;
        color: #64748b;
        margin: 0;
      }
    }

    .password-form {
      padding: 24px;

      .form-item {
        margin-bottom: 24px;

        :deep(.el-form-item__label) {
          font-weight: 600;
          color: #2d3748;
          margin-bottom: 8px;
          font-size: 14px;
        }

        .form-input {
          :deep(.el-input__wrapper) {
            border-radius: 8px;
            border: 2px solid #e2e8f0;
            transition: all 0.3s ease;
            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);

            &:hover {
              border-color: #667eea;
            }

            &.is-focus {
              border-color: #667eea;
              box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
            }
          }
        }
      }

      .form-actions {
        display: flex;
        justify-content: center;
        margin-top: 32px;

        .save-btn {
          background: linear-gradient(45deg, #667eea, #764ba2);
          border: none;
          border-radius: 12px;
          font-weight: 600;
          padding: 12px 32px;
          box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
          transition: all 0.3s ease;

          &:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6);
          }
        }
      }
    }
  }



}
</style>