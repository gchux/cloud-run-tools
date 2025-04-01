import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

export const jMeterTestStore = defineStore('test', () => {
  const test = ref("cloud_run_qps_full")
  return { test }
})
