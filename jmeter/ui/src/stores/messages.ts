import { defineStore } from 'pinia'
import { z } from 'zod'
import { omit } from 'lodash'

const COLOR = [
  "error",
  "success"
] as const;

export const ColorEnumSchema = z.enum(COLOR);

export type ColorEnum = z.infer<typeof ColorEnumSchema>;

export const MessageSchema = z.object({
  text: z.string().nonempty(),
  timeout: z.number().gte(1000),
  color: ColorEnumSchema,
});

export const VMessageSchema =
  MessageSchema.extend({
    closeOnContentClick: z.boolean(),
    multiLine: z.boolean(),
  });

export type Message = z.infer<typeof MessageSchema>;

export type VMessage = z.infer<typeof VMessageSchema>;

export const QueueSchema = z.object({
  messages: z.array(VMessageSchema),
});

export type Qeue = z.infer<typeof QueueSchema>;

export type ID = 'messages';

const ID = 'messages' as const;

export const useMessagesStore = defineStore(ID, {
  state: () => {
    return {
      messages: [],
    } as Qeue;
  },

  getters: {
    queue(): Message[] {
      return this.messages;
    }
  },

  actions: {
    get(): Message[] {
      return this.messages;
    },

    push(message: Message): void {
      this.messages.push({
        ...message,
        multiLine: true,
        closeOnContentClick: true,
      });
    },

    error(message: string): void {
      this.push({
        text: message,
        timeout: 6000,
        color: ColorEnumSchema.Values.error,
      });
    },

    Error(error: Error): void {
      this.error(error.message);
    },

    parameterError(
      param: string,
      value: any,
      error: z.ZodError
    ): void {
      const issue = error.issues[0];
      this.error(`invalid value '${value}' for '${param || "parameter"}': ${issue.message}`);
      console.error(error);
    },
  },
});