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
      name: 'run_test',
      component: RunTestView,
    },
  ],
})

export default router
