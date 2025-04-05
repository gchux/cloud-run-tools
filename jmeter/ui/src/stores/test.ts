import { defineStore } from 'pinia'
import { z } from 'zod'

const TestSchema = z.object({
  script: z.string(),
});

type Test = z.infer<typeof TestSchema>;

export const useTestStore = defineStore('test', {
  state: () => {
    return {
      script: "cloud_run_qps_full",
    } as Partial<Test>;
  },

  getters: {},

  actions: {
    updateScript(script: string) {
      this.script = script;
    },
  },
});