import { defineStore } from 'pinia'
import { z } from 'zod'
import type {
  ModeEnumType,
  ProtoEnumType,
  ParamEnumType,
  MethodEnumType,
  MultiValueParamsType,
  MultiValueParamType,
  QPS,
  Concurrency,
} from '../types/catalogs.ts'
import {
  ModeEnumSchema,
  ProtoEnumSchema,
  MethodEnumSchema,
  MultiValueParamsSchema,
  QpsSchema,
  ConcurrencySchema,
} from '../types/catalogs.ts'
import { isEqual, toNumber, values } from 'lodash'

const KeyValueSchema = z.map(
  z.number(),
  z.object({
    key: z.string(),
    value: z.string(),
  }),
);

type KeyValue = z.infer<typeof KeyValueSchema>;

export const QpsValueSchema = z.map(z.number(), QpsSchema);

export type QpsValue = z.infer<typeof QpsValueSchema>;

export const ConcurrencyValueSchema = z.map(z.number(), ConcurrencySchema);

export type ConcurrencyValue = z.infer<typeof ConcurrencyValueSchema>;

const PortSchema = z.number().min(1).max(65535).finite();

export type Port = z.infer<typeof PortSchema>;

const MinMaxLatencySchema = z.number().positive().gte(1).lte(3600000).finite();

export type MinMaxLatencySchema = z.infer<typeof MinMaxLatencySchema>;

const DurationSchema = z.number().positive().gte(10).lte(3600).finite();

export type Duration = z.infer<typeof DurationSchema>;

const NonEmptyString = z.string().nonempty();

export const TestSchema = z.object({
  id: z.optional(NonEmptyString),
  script: NonEmptyString,
  mode: ModeEnumSchema,
  host: NonEmptyString,
  port: PortSchema,
  async: z.boolean(),
  method: MethodEnumSchema,
  proto: ProtoEnumSchema,
  path: NonEmptyString,
  payload: z.optional(NonEmptyString),
  query: KeyValueSchema,
  headers: KeyValueSchema,
  qps: QpsValueSchema,
  concurrency: ConcurrencyValueSchema,
  duration: DurationSchema,
  minLatency: MinMaxLatencySchema,
  maxLatency: MinMaxLatencySchema,
});

export type Test = z.infer<typeof TestSchema>;

export const MultiValueSchema = z.union([QpsValueSchema, ConcurrencyValueSchema]);

export type MultiValue = z.infer<typeof MultiValueSchema>;

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
      qps: new Map<number, QPS>(),
      concurrency: new Map<number, Concurrency>(),
      duration: DurationSchema.minValue,
      minLatency: MinMaxLatencySchema.minValue,
      maxLatency: 1000, // 1 second
    } as Test;
  },

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
    getQPS(): QPS[] {
      return Array.from(this.qps.values());
    },
    getConcurrency(): Concurrency[] {
      return Array.from(this.concurrency.values());
    },
    getMultiValue(id: string): MultiValue {
      if ( isEqual(id, "qps") ) {
        return this.qps;
      }
      return this.concurrency;
    },
    isComplete() {
      return (this.qps.size > 0) || (this.concurrency.size > 0);
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
    setQPS(
      index: number,
      value: QPS,
    ): QpsValue {
      return this.qps.set(
        QpsValueSchema.keySchema.parse(index),
        QpsSchema.parse(value),
      );
    },
    setConcurrency(
      index: number,
      value: Concurrency,
    ): ConcurrencyValue {
      return this.concurrency.set(
        ConcurrencyValueSchema.keySchema.parse(index),
        ConcurrencySchema.parse(value),
      );
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
      id: MultiValueParamsType,
      index: number,
      value: MultiValueParamType,
    ) {
      switch(id) {
        case MultiValueParamsSchema.Values.qps:
          return this.setQPS(index, value as QPS);
        case MultiValueParamsSchema.Values.qps:
          return this.setConcurrency(index, value as Concurrency);
      }
    },
    unsetMultiValue(
      id: MultiValueParamsType,
      index: number,
    ): boolean {
      return this.getMultiValue(id).delete(index);
    },
  },
});