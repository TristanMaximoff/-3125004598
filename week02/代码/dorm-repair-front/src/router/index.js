import { createRouter, createWebHistory } from 'vue-router'
import UserStore from '../store/user'

import AdminLayout from '../views/admin/Layout.vue'
import StudentLayout from '../views/student/Layout.vue'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import StudentHome from '../views/student/Home.vue'
import StudentRepairs from '../views/student/MyRepairs.vue'
import AdminRepairs from '../views/admin/AllRepairs.vue'
// ========== 修改：确保导入的是 RepairDetail.vue，不是 RepairDetails.vue ==========
import AdminRepairDetail from '../views/admin/RepairDetail.vue'

const routes = [
  { path: '/login', name: 'Login', component: Login },
  { path: '/register', name: 'Register', component: Register },
  {
    path: '/student',
    component: StudentLayout,
    meta: { role: 'student' },
    children: [
      { path: '', name: 'StudentHome', component: StudentHome },
      { path: 'repairs', name: 'StudentRepairs', component: StudentRepairs }
    ]
  },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { role: 'admin' },
    children: [
      { path: '', name: 'AdminRepairs', component: AdminRepairs },
      { path: 'detail/:id', name: 'AdminRepairDetail', component: AdminRepairDetail }
    ]
  },
  { path: '/', redirect: '/login' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = UserStore()
  const token = userStore.token
  const role = userStore.userInfo?.role

  if (to.path === '/login' || to.path === '/register') {
    token ? next(role === 'admin' ? '/admin' : '/student') : next()
  } else {
    if (!token) return next('/login')
    if (to.meta.role && to.meta.role !== role) {
      next('/login')
    } else {
      next()
    }
  }
})

export default router