import { defineStore } from 'pinia'
import { z } from 'zod'

const TestSchema = z.object({
  script: z.string(),
  host: z.string().url().ip(),
  port: z.number().min(1).max(65535),
});

export type Test = z.infer<typeof TestSchema>;

export const useTestStore = defineStore('test', {
  state: () => ({} as Partial<Test>),
  getters: {
    script: (state) => state.script,
  },
  actions: {
    setScript(script: string) {
      this.script = TestSchema.shape.script.parse(script)
    },
    setHost(host: string) {
      this.host = TestSchema.shape.host.parse(host)
    },
  },
})