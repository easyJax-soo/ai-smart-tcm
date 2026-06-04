import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

/**
 * 路由配置
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '应用中心' }
  },
  {
    path: '/tcm-chat/:chatId?',
    name: 'TCMChat',
    component: () => import('@/views/TCMAIChat.vue'),
    meta: { title: 'AI 云中医问诊' }
  },
  {
    path: '/manus-chat/:chatId?',
    name: 'ManusChat',
    component: () => import('@/views/ManusChat.vue'),
    meta: { title: 'AI 超级智能体' }
  },
  {
    path: '/rag-management',
    name: 'RagManagement',
    component: () => import('@/views/RagManagement.vue'),
    meta: { title: 'RAG 知识库管理' }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

// 路由守卫：动态设置页面标题
router.beforeEach((to, _from, next) => {
  const title = to.meta?.title as string | undefined
  if (title) {
    document.title = `${title} · AI 智能云`
  }
  next()
})

export default router
