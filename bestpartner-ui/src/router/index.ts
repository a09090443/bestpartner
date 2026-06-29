import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { defineComponent, h } from 'vue'

const PlaceholderView = defineComponent({
  name: 'PlaceholderView',
  render() {
    return h('div', { style: 'padding: 24px; text-align: center;' }, 'BestPartner UI — 正在建置中')
  },
})

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'home',
    component: PlaceholderView,
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

export default router
