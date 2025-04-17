import { z } from 'zod'
import { NIL as defaultUUID } from 'uuid';
import { defineStore } from 'pinia'
import { isEqual, toNumber } from 'lodash'
import {
  ModeEnumSchema,
  ProtoEnumSchema,
  MethodEnumSchema,
  ParamEnumSchema,
  MultiValueParamsSchema,
  QpsSchema,
  ConcurrencySchema,
  KeyValueParamsSchema,
} from '../types/catalogs.ts'
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

const NonEmptyString = z.string().nonempty().trim();

const KVSchema = z.object({
  key: NonEmptyString,
  value: NonEmptyString,
});

type KV = z.infer<typeof KVSchema>;

const KEY = z.number().nonnegative().finite();

const KeyValueSchema = z.map(KEY, KVSchema);

type KeyValueType = z.infer<typeof KeyValueSchema>;

export const QpsValueSchema = z.map(KEY, QpsSchema);

export type QpsValue = z.infer<typeof QpsValueSchema>;

export const ConcurrencyValueSchema = z.map(KEY, ConcurrencySchema);

export type ConcurrencyValue = z.infer<typeof ConcurrencyValueSchema>;

const PortSchema = z.number().min(1).max(65535).finite();

export type Port = z.infer<typeof PortSchema>;

const MinMaxLatencySchema = z.number().positive().gte(1).lte(3600000).finite();

export type MinMaxLatencySchema = z.infer<typeof MinMaxLatencySchema>;

export const DurationSchema = z.number().gte(10).finite();

export type Duration = z.infer<typeof DurationSchema>;

export const TestSchema = z.object({
  id: z.string().uuid(),
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

export const MultiValueParamSchema = z.union([QpsValueSchema, ConcurrencyValueSchema]);

export type MultiValueParam = z.infer<typeof MultiValueParamSchema>;

export const useTestStore = defineStore('test', {
  state: () => {
    return {
      id: defaultUUID,
      async: false,
      script: "cloud_run_qps_full",
      mode: "qps",
      host: "localhost",
      port: 80,
      method: "GET",
      proto: "HTTPS",
      path: "/",
      payload: "test",
      query: new Map<number, KV>(),
      headers: new Map<number, KV>(),
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
    isAsync(): boolean {
      return this.async;
    },
    getMode(): ModeEnumType {
      return this.mode;
    },
    getMethod(): MethodEnumType {
      return this.method;
    },
    getDuration(): number {
      return this.duration;
    },
    getKeyValue(id: ParamEnumType): KeyValueType {
      const ID = KeyValueParamsSchema.parse(id);
      if ( isEqual(ID, KeyValueParamsSchema.Enum.query) ) {
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
    getMultiValue(id: ParamEnumType): MultiValueParam {
      const ID = MultiValueParamsSchema.parse(id);
      if ( isEqual(ID, MultiValueParamsSchema.Enum.qps) ) {
        return this.qps;
      }
      return this.concurrency;
    },
    isComplete() {
      return (this.qps.size > 0) || (this.concurrency.size > 0);
    },
    setID(id: string) {
      return this.id = TestSchema.shape.id.parse(id);
    },
    setScript(script: string): string {
      return this.script = TestSchema.shape.script.parse(script);
    },
    setMode(mode: ModeEnumType): ModeEnumType {
      return this.mode = TestSchema.shape.mode.parse(mode);
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
      id: ParamEnumType,
      value: string,
    ) {
      switch ( id ) {
        case ParamEnumSchema.Enum.host:
          return this.setHost(value);
        case ParamEnumSchema.Enum.port:
          return this.setPort(toNumber(value));
        case ParamEnumSchema.Enum.async:
          return this.setAsync(
            (/true/i).test(value)
          );
        case ParamEnumSchema.Enum.proto:
          return this.setProto(value);
        case ParamEnumSchema.Enum.method:
          return this.setMethod(value);
        case ParamEnumSchema.Enum.path:
          return this.setPath(value);
        case ParamEnumSchema.Enum.payload:
          return this.setPayload(value);
        case ParamEnumSchema.Enum.duration:
          return this.setDuration(toNumber(value));
        case ParamEnumSchema.Enum['min-latency']:
          return this.setMinLatency(toNumber(value));
        case ParamEnumSchema.Enum['max-latency']:
          return this.setMaxLatency(toNumber(value));
        case ParamEnumSchema.Enum.script:
          this.setScript(value);
        default:
          throw new Error(`invalid test parameter: ${id}`);
      }
    },
    getValue(
      id: ParamEnumType,
    ) {
      switch ( id ) {
        case ParamEnumSchema.Enum.host:
          return this.host;
        case ParamEnumSchema.Enum.port:
          return this.port;
        case ParamEnumSchema.Enum.async:
          return this.async;
        case ParamEnumSchema.Enum.proto:
          return this.proto;
        case ParamEnumSchema.Enum.method:
          return this.method;
        case ParamEnumSchema.Enum.path:
          return this.path;
        case ParamEnumSchema.Enum.payload:
          return this.payload;
        case ParamEnumSchema.Enum.duration:
          return this.duration;
        case ParamEnumSchema.Enum['min-latency']:
          return this.minLatency;
        case ParamEnumSchema.Enum['max-latency']:
          return this.maxLatency;
        case ParamEnumSchema.Enum.mode:
            return this.mode;
        case ParamEnumSchema.Enum.script:
          return this.script;
        case ParamEnumSchema.Enum.query:
          return Array.from(this.query.values());
        case ParamEnumSchema.Enum.headers:
          return Array.from(this.headers.values());
        case ParamEnumSchema.Enum.qps:
            return this.getQPS();
        case ParamEnumSchema.Enum.concurrency:
          return this.getConcurrency();
        default:
          throw new Error(`invalid test parameter: ${id}`);
      }
    },
    setKeyValue(
      id: ParamEnumType,
      index: number,
      key: string,
      value: string,
    ) {
      const kv = this.getKeyValue(id);
      kv.set(
        KeyValueSchema.keySchema.parse(index),
        { 
          key: KVSchema.shape.key.parse(key), 
          value:  KVSchema.shape.value.parse(value),
        }
      );
    },
    unsetKeyValue(
      id: ParamEnumType,
      index: number,
    ) {
      const kv = this.getKeyValue(id);
      kv.delete(
        KeyValueSchema.keySchema.parse(index)
      );
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
    setMultiValue(
      id: MultiValueParamsType,
      index: number,
      value: MultiValueParamType,
    ): MultiValueParam {
      switch(id) {
        case MultiValueParamsSchema.Enum.qps:
          return this.setQPS(
            QpsValueSchema.keySchema.parse(index),
            QpsSchema.parse(value)
          );
        case MultiValueParamsSchema.Enum.concurrency:
          return this.setConcurrency(
            ConcurrencyValueSchema.keySchema.parse(index),
            ConcurrencySchema.parse(value)
          );
      }
    },
    unsetQPS(
      index: number,
    ): boolean {
      return this.qps.delete(
        QpsValueSchema.keySchema.parse(index)
      );
    },
    unsetConcurrency(
      index: number,
    ): boolean {
      return this.concurrency.delete(
        ConcurrencyValueSchema.keySchema.parse(index)
      );
    },
    unsetMultiValue(
      id: MultiValueParamsType,
      index: number,
    ): boolean {
      return this.getMultiValue(id).delete(toNumber(index));
    },
  },
});