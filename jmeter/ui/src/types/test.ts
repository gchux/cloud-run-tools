import { z } from 'zod'

export const TestDataSchema = z.object({
    id: z.string().uuid(),
    traceID: z.string().nonempty(),
    instanceID: z.string().nonempty(),
    isComplete : z.boolean(),
    output: z.string(),
});

export type TestData = z.infer<typeof TestDataSchema>;