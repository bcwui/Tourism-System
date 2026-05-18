import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'
import BackendLayout from '@/layouts/BackendLayout.vue'

// 后台路由
export const backendRoutes = [
  {
    path: '/back',
    component: BackendLayout,
    redirect: '/back/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/backend/Dashboard.vue'),
        meta: { title: '首页', icon: 'HomeFilled' }
      },
      {
        path: 'user',
        name: 'UserManagement',
        component: () => import('@/views/backend/user/index.vue'),
        meta: { title: '用户管理', icon: 'User' }
      },
      {
        path: 'profile',
        name: 'BackendProfile',
        component: () => import('@/views/backend/user/PersonInfo.vue'),
        meta: { title: '个人信息', icon: 'UserFilled' }
      },
      {
        path: 'scenic',
        name: 'ScenicManagement',
        component: () => import('@/views/backend/scenic/index.vue'),
        meta: { title: '景点管理', icon: 'Location' }
      },
      {
        path: 'category',
        name: 'CategoryManagement',
        component: () => import('@/views/backend/category/CategoryList.vue'),
        meta: { title: '分类管理', icon: 'Menu' }
      },
      {
        path: 'comment',
        name: 'CommentManagement',
        component: () => import('@/views/backend/comment/index.vue'),
        meta: { title: '评论管理', icon: 'ChatDotRound' }
      },
      {
        path: 'guide',
        name: 'GuideManagement',
        component: () => import('@/views/backend/guide/GuideList.vue'),
        meta: { title: '攻略管理', icon: 'Document' }
      },
      {
        path: 'collection',
        name: 'CollectionManagement',
        component: () => import('@/views/backend/collection/CollectionList.vue'),
        meta: { title: '收藏管理', icon: 'Star' }
      },
      {
        path: 'ticket',
        name: 'TicketManagement',
        component: () => import('@/views/backend/ticket/index.vue'),
        meta: { title: '门票管理', icon: 'Ticket' }
      },
      {
        path: 'order',
        name: 'OrderManagement',
        component: () => import('@/views/backend/order/index.vue'),
        meta: { title: '订单管理', icon: 'List' }
      },
      {
        path: 'accommodation',
        name: 'AccommodationManagement',
        component: () => import('@/views/backend/accommodation/index.vue'),
        meta: { title: '住宿管理', icon: 'House' }
      },
      {
        path: 'carousel',
        name: 'CarouselManagement',
        component: () => import('@/views/backend/carousel/index.vue'),
        meta: { title: '轮播图管理', icon: 'Picture' }
      }
    ]
  }
]

// 前台路由配置
const frontendRoutes = [
  {
    path: '/',
    component: () => import('@/layouts/FrontendLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/frontend/Home.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/frontend/profile/index.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      },
      {
        path: 'accommodation',
        name: 'AccommodationList',
        component: () => import('@/views/frontend/accommodation/index.vue'),
        meta: { title: '周边住宿' }
      },
      {
        path: 'accommodation/:id',
        name: 'AccommodationDetail',
        component: () => import('@/views/frontend/accommodation/detail.vue'),
        meta: { title: '住宿详情' }
      },
      {
        path: 'tickets',
        name: 'Tickets',
        component: () => import('@/views/frontend/ticket/index.vue'),
        meta: { title: '门票预订' }
      },
      {
        path: 'ticket/booking/:id',
        name: 'TicketBooking',
        component: () => import('@/views/frontend/ticket/booking.vue'),
        meta: { title: '预订门票', requiresAuth: true }
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('@/views/frontend/orders/index.vue'),
        meta: { title: '我的订单', requiresAuth: true }
      },
      {
        path: 'scenic',
        name: 'ScenicList',
        component: () => import('@/views/frontend/scenic/ScenicList.vue'),
        meta: { title: '景点列表' }
      },
      {
        path: 'scenic/:id',
        name: 'ScenicDetail',
        component: () => import('@/views/frontend/scenic/ScenicDetail.vue'),
        meta: { title: '景点详情' }
      },
      {
        path: 'scenic/category/:categoryId',
        name: 'ScenicByCategory',
        component: () => import('@/views/frontend/scenic/ScenicList.vue'),
        props: true,
        meta: { title: '分类景点' }
      },
      {
        path: 'guide',
        name: 'GuideList',
        component: () => import('@/views/frontend/guide/GuideList.vue'),
        meta: { title: '攻略列表' }
      },
      {
        path: 'guide/detail/:id',
        name: 'GuideDetail',
        component: () => import('@/views/frontend/guide/GuideDetail.vue'),
        meta: { title: '攻略详情' }
      },
      {
        path: 'guide/edit',
        name: 'GuideEdit',
        component: () => import('@/views/frontend/guide/GuideEdit.vue'),
        meta: { title: '发布攻略', requiresAuth: true }
      },
      {
        path: 'my-guide',
        name: 'MyGuideList',
        component: () => import('@/views/frontend/guide/MyGuideList.vue'),
        meta: { title: '我的攻略', requiresAuth: true }
      },
      {
        path: 'collection',
        name: 'Collection',
        component: () => import('@/views/frontend/collection/MyCollection.vue'),
        meta: { title: '我的收藏', requiresAuth: true }
      },
      {
        path: 'payment/alipay/:id',
        name: 'AlipayPayment',
        component: () => import('@/views/frontend/payment/alipay-form.vue'),
        meta: { title: '支付宝支付', requiresAuth: true }
      },
      {
        path: 'payment-failed',
        name: 'PaymentFailed',
        component: () => import('@/views/frontend/payment/payment-failed.vue'),
        meta: { title: '支付失败' }
      },
      {
        path: 'payment/result',
        name: 'PaymentResult',
        component: () => import('@/views/frontend/payment/payment-result.vue'),
        meta: { title: '支付结果' }
      }
    ] 
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { title: '注册' }
  }
]

// 错误页面路由
const errorRoutes = [
  {
    path: '/404',
    name: '404',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404' }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404'
  }
]

// 路由配置
const router = createRouter({
  history: createWebHistory(),
  routes: [
    ...frontendRoutes,
    ...backendRoutes,
    ...errorRoutes
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - 旅游信息系统`
  }

  const userStore = useUserStore()
  console.log("Current route:", to.path)
  console.log("User status:", {
    isLoggedIn: userStore.isLoggedIn,
    isUser: userStore.isUser
  })

  // 检查是否需要登录权限
  if (to.matched.some(record => record.meta.requiresAuth) && !userStore.isLoggedIn) {
    next({
      path: '/login',
      query: { redirect: to.fullPath }
    })
    return
  }

  // 已登录用户的路由控制
  if (userStore.isLoggedIn) {
    // 处理登录页面访问
    if (to.path === '/login') {
      next(userStore.isUser ? '/' : '/back/dashboard')
      return
    }

    if (!userStore.isUser) {
      // 非普通用户只能访问后台路由
      if (to.path.startsWith('/back')) {
        next()
      } else {
        next('/back/dashboard')
      }
      return
    } else {
      // 普通用户只能访问前台路由
      if (to.path.startsWith('/back')) {
        next('/')
      } else {
        next()
      }
      return
    }
  } else {
    // 未登录用户
    if (to.path.startsWith('/back')) {
      next('/login')
      return
    }
  }

  next()
})

export default router
