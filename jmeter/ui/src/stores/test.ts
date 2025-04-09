import { defineStore } from 'pinia'
import { z } from 'zod'
import type {
  ModeEnumType,
  ProtoEnumType,
  ParamEnumType,
  MethodEnumType,
} from '../types/catalogs.ts'
import {
  ModeEnumSchema,
  ProtoEnumSchema,
  MethodEnumSchema
} from '../types/catalogs.ts'
import { isEqual, toNumber } from 'lodash'

const KeyValueSchema = z.map(
  z.number(),
  z.object({
    key: z.string(),
    value: z.string(),
  }),
);

type KeyValue = z.infer<typeof KeyValueSchema>;

const MultiValueSchema = z.map(z.number(), z.string());

type MultiValue = z.infer<typeof MultiValueSchema>;

const PortSchema = z.number().min(1).max(65535).finite();

export type Port = z.infer<typeof PortSchema>;

const MinMaxLatencySchema = z.number().positive().gte(1).lte(3600000).finite();

export type MinMaxLatencySchema = z.infer<typeof MinMaxLatencySchema>;

const DurationSchema = z.number().positive().gte(10).lte(3600).finite();

export type Duration = z.infer<typeof DurationSchema>;

const TestSchema = z.object({
  script: z.string(),
  mode: ModeEnumSchema,
  host: z.string().nonempty(),
  port: PortSchema,
  async: z.boolean(),
  method: MethodEnumSchema,
  proto: ProtoEnumSchema,
  path: z.string().nonempty(),
  payload: z.optional(z.string()),
  query: KeyValueSchema,
  headers: KeyValueSchema,
  qps: MultiValueSchema,
  concurrency: MultiValueSchema,
  duration: DurationSchema,
  minLatency: MinMaxLatencySchema,
  maxLatency: MinMaxLatencySchema,
});

export type Test = z.infer<typeof TestSchema>;

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
      query: new Map(),
      headers: new Map(),
      qps: new Map(),
      concurrency: new Map(),
      duration: DurationSchema.minValue,
      minLatency: MinMaxLatencySchema.minValue,
      maxLatency: 1000, // 1 second
    } as Test;
  },

  getters: {},

  actions: {
    get(): Test {
      return this.$state;
    },
    getMethod(): MethodEnumType {
      return this.method;
    },
    getKeyValue(id: string): KeyValue {
      if ( isEqual(id, "query") ) {
        return this.query;
      }
      return this.headers;
    },
    getMultiValue(id: string): MultiValue {
      if ( isEqual(id, "qps") ) {
        return this.qps;
      }
      return this.concurrency;
    },
    setScript(script: string): string {
      return this.script = script;
    },
    setMode(mode: ModeEnumType): ModeEnumType {
      return this.mode = mode;
    },
    setHost(host: string): string {
      return this.host = TestSchema.shape.host.parse(host);
    },
    setPort(port: number): Port {
      return this.port = PortSchema.parse(port);
    },
    setAsync(async: boolean): boolean {
      return this.async = TestSchema.shape.async.parse(async);
    },
    setProto(proto: string): ProtoEnumType {
      return this.proto = ProtoEnumSchema.parse(proto);
    },
    setMethod(method: string): string {
      return this.method = MethodEnumSchema.parse(method);
    },
    setPath(path: string): string {
      return this.path = TestSchema.shape.path.parse(path);
    },
    setPayload(payload: string): string | undefined {
      return this.payload = TestSchema.shape.payload.parse(payload);
    },
    setDuration(duration: number): Duration {
      return this.duration = DurationSchema.parse(duration);
    },
    setMinLatency(minLatency: number): MinMaxLatencySchema {
      return this.minLatency = MinMaxLatencySchema.parse(minLatency);
    },
    setMaxLatency(maxLatency: number): MinMaxLatencySchema {
      return this.maxLatency = MinMaxLatencySchema.parse(maxLatency);
    },
    setValue(
      id: ParamEnumType | undefined,
      value: string,
    ) {
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
        case "query":
          return this.setPayload(value);
        case "headers":
          return this.setPayload(value);
        case "duration":
          return this.setDuration(toNumber(value));
        case "min-latency":
          return this.setMinLatency(toNumber(value));
        case "max-latency":
          return this.setMaxLatency(toNumber(value));
        default:
          throw new Error("invalid test parameter");
      }
    },
    setKeyValue(
      id: ParamEnumType,
      index: number,
      key: string,
      value: string,
    ) {
      const kv = this.getKeyValue(id);
      kv.set(index, { key, value });
    },
    unsetKeyValue(
      id: ParamEnumType,
      index: number,
    ) {
      const kv = this.getKeyValue(id);
      kv.delete(index);
    },
    setMultiValue(
      id: ParamEnumType,
      index: number,
      value: string,
    ) {
      const values = this.getMultiValue(id);
      values.set(index, value);
    },
    unsetMultiValue(
      id: ParamEnumType,
      index: number,
    ) {
      const values = this.getMultiValue(id);
      values.delete(index);
    },
  },
});