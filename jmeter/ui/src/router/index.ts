import { createRouter, createMemoryHistory } from 'vue-router'

import NewTestView from '../views/NewTestView.vue'

const router = createRouter({
  history: createMemoryHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      alias: '/new',
      component: NewTestView,
    },
    {
      path: '/run',
      name: 'run',
      component: () => import('../views/RunTestView.vue'),
    },
    {
      path: '/stream/:id',
      name: 'stream',
      alias: '/stream',
      component: () => import('../views/StreamTestView.vue'),
    },
  ],
})

export default router
