import { createRouter, createMemoryHistory } from 'vue-router'

import RunTestView from '../views/RunTestView.vue'

const router = createRouter({
  history: createMemoryHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: RunTestView,
    },
    {
      path: '/run',
      name: 'run',
      component: RunTestView,
    },
    {
      path: '/stream',
      name: 'stream',
      component: () => import('../views/StreamTestView.vue'),
    },
    {
      path: '/stream/:id',
      name: 'stream_by_id',
      component: () => import('../views/StreamTestView.vue'),
    },
  ],
})

export default router
