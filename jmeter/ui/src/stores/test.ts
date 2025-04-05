import { defineStore } from 'pinia'
import { z } from 'zod'
import type {
  ModeEnumType,
  ParamEnumType,
} from '../types/catalogs.ts'
import {
  ModeEnumSchema,
  ProtoEnumSchema,
  MethodEnumSchema
} from '../types/catalogs.ts'
import { toNumber } from 'lodash'

const TestSchema = z.object({
  script: z.string(),
  mode: ModeEnumSchema,
  host: z.string().nonempty().url(),
  port: z.number().min(1).max(65535),
  async: z.boolean(),
  method: MethodEnumSchema,
  proto: ProtoEnumSchema,
  path: z.string().nonempty(),
  payload: z.optional(z.string()),
});

type Test = z.infer<typeof TestSchema>;

export const useTestStore = defineStore('test', {
  state: () => {
    return {
      script: "cloud_run_qps_full",
      mode: "qps",
      host: "localhost",
      port: 443,
      method: "GET",
      proto: "HTTPS",
      path: "/",
    } as Test;
  },

  getters: {},

  actions: {
    setScript(script: string) {
      this.script = script;
    },
    setMode(mode: ModeEnumType) {
      this.mode = mode;
    },
    setHost(host: string) {
      this.host = TestSchema.shape.host.parse(host);
    },
    setPort(port: number) {
      this.port = TestSchema.shape.port.parse(port);
    },
    setAsync(async: boolean) {
      this.async = TestSchema.shape.async.parse(async);
    },
    setProto(proto: string) {
      this.proto = ProtoEnumSchema.parse(proto);
    },
    setMethod(method: string) {
      this.method = MethodEnumSchema.parse(method);
    },
    setPath(path: string) {
      this.path = TestSchema.shape.path.parse(path);
    },
    setPayload(payload: string) {
      this.payload = TestSchema.shape.payload.parse(payload);
    },
    setValue(id: ParamEnumType | undefined, value: string) {
      console.log("test value: ", id, value);
      switch ( id ) {
        case "host":
          return this.setHost(value);
        case "port":
          return this.setPort(toNumber(value));
        case "async":
          return this.setAsync((/true/i).test(value));
        case "proto":
          return this.setProto(value);
        case "method":
          return this.setMethod(value);
        case "path":
          return this.setPath(value);
        case "payload":
          return this.setPayload(value);
      }
      
    }
  },
});