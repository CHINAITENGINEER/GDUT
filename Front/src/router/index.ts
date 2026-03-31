import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { requiresAuth: true, title: '首页' }
  },
  {
    path: '/rally',
    name: 'Rally',
    component: () => import('@/views/Rally.vue'),
    meta: { requiresAuth: true, title: '实时组队' }
  },
  {
    path: '/task/publish',
    name: 'TaskPublish',
    component: () => import('@/views/TaskPublish.vue'),
    meta: { requiresAuth: true, title: '发布任务' }
  },
  {
    path: '/task/:id',
    name: 'TaskDetail',
    component: () => import('@/views/TaskDetail.vue'),
    meta: { requiresAuth: true, title: '任务详情' }
  },
  {
    path: '/my/tasks',
    name: 'MyTasks',
    component: () => import('@/views/MyTasks.vue'),
    meta: { requiresAuth: true, title: '我的任务' }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { requiresAuth: true, title: '个人主页' }
  },
  {
    path: '/level',
    name: 'Level',
    component: () => import('@/views/LevelCenter.vue'),
    meta: { requiresAuth: true, title: '等级中心' }
  },
  {
    path: '/messages',
    name: 'Messages',
    component: () => import('@/views/MessageCenter.vue'),
    meta: { requiresAuth: true, title: '消息中心' }
  },
  {
    path: '/recommendation',
    name: 'Recommendation',
    component: () => import('@/views/Recommendation.vue'),
    meta: { requiresAuth: true, title: '推荐中心' }
  },
  {
    path: '/payment/pay',
    name: 'Payment',
    component: () => import('@/views/Payment.vue'),
    meta: { requiresAuth: true, title: '支付' }
  },
  {
    path: '/payment/records',
    name: 'PaymentRecords',
    component: () => import('@/views/PaymentRecords.vue'),
    meta: { requiresAuth: true, title: '支付记录' }
  },
  {
    path: '/settlement/records',
    name: 'SettlementRecords',
    component: () => import('@/views/SettlementRecords.vue'),
    meta: { requiresAuth: true, title: '结算记录' }
  },
  {
    path: '/user/:id',
    name: 'UserHome',
    component: () => import('@/views/UserHome.vue'),
    meta: { requiresAuth: true, title: '用户主页' }
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true, adminOnly: true, title: '管理后台' },
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { title: '后台总览' }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/UserManage.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'tasks/pending',
        name: 'AdminTaskAudit',
        component: () => import('@/views/admin/TaskAudit.vue'),
        meta: { title: '任务审核' }
      },
      {
        path: 'fee-config',
        name: 'AdminFeeConfig',
        component: () => import('@/views/admin/FeeConfig.vue'),
        meta: { title: '手续费配置' }
      },
      {
        path: 'trades',
        name: 'AdminTrades',
        component: () => import('@/views/admin/TradeStats.vue'),
        meta: { title: '交易统计' }
      },
      {
        path: 'disputes',
        name: 'AdminDisputes',
        component: () => import('@/views/admin/DisputeHandle.vue'),
        meta: { title: '争议处理' }
      },
      {
        path: '',
        name: 'AdminIndex',
        redirect: '/admin/dashboard'
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '404' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  if (to.meta.requiresAuth && !userStore.isLogin) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  
  if (to.meta.adminOnly && !userStore.isAdmin) {
    next({ path: '/' })
    return
  }
  
  if (to.meta.title) {
    document.title = `${to.meta.title} - 校园任务接单平台`
  }
  
  next()
})

export default router
