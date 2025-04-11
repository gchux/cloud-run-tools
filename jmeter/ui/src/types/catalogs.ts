import { z } from 'zod';

const MODE = [
  "qps",
  "concurrency"
] as const;

export const ModeEnumSchema = z.enum(MODE);

export type ModeEnumType = z.infer<typeof ModeEnumSchema>;

const PARAM = [
  "id",
  "async",
  "concurrency",
  "qps",
  "duration",
  "min-latency",
  "max-latency",
  "proto",
  "method",
  "host",
  "port",
  "path",
  "query",
  "headers",
  "payload",
] as const;

export const ParamEnumSchema = z.enum(PARAM)

export const ParamsEnumSchema = z.array(z.array(ParamEnumSchema));

export type ParamEnumType = z.infer<typeof ParamEnumSchema>;

export type ParamsEnumType = z.infer<typeof ParamsEnumSchema>;

const SIMPLE_TYPE = [
  "enum",
  "string",
  "str",
  "text",
  "txt",
  "number",
  "num",
  "integer",
  "int",
  "boolean",
  "bool"
] as const;

const COMPLEX_TYPE = [
  "union",
  "tuple",
  "array",
  "list",
  "set",
  "map",
  "kv",
] as const;

const TYPE = [
  ...SIMPLE_TYPE,
  ...COMPLEX_TYPE,
] as const;

export const SimpleTypeEnumSchema = z.enum(SIMPLE_TYPE);

export const ComplexTypeEnumSchema = z.enum(COMPLEX_TYPE);

export const TypeEnumSchema = z.enum(TYPE);

export type ParamType = z.infer<typeof TypeEnumSchema>;

const NonEmptyString = z.string().nonempty();

const ArrayOfStrings = z.array(NonEmptyString);

export const UnionTypeSchema = z.tuple([
  z.string()
    .startsWith(TypeEnumSchema.Enum.union)
    .endsWith(TypeEnumSchema.Enum.union)
    .length(5),
  z.array(SimpleTypeEnumSchema),
]);

export const TupleTypeSchema = z.tuple([
  z.string()
    .startsWith(TypeEnumSchema.Enum.tuple)
    .endsWith(TypeEnumSchema.Enum.tuple)
    .length(5),
  z.array(SimpleTypeEnumSchema)
]);

export const ArrayParamSchema = z.tuple([
  z.string()
    .startsWith(TypeEnumSchema.Enum.array)
    .endsWith(TypeEnumSchema.Enum.array)
    .length(5),
  z.union([      // type of values
    SimpleTypeEnumSchema,
    UnionTypeSchema,
    TupleTypeSchema,
  ]),
]);

export type ArrayParamType = z.infer<typeof ArrayParamSchema>;

export const MapParamSchema = z.tuple([
  z.string()
    .startsWith(TypeEnumSchema.Enum.map)
    .endsWith(TypeEnumSchema.Enum.map)
    .length(3),
  z.tuple([
    SimpleTypeEnumSchema,      // type of key
    z.union([                  // type of value
      SimpleTypeEnumSchema,
      UnionTypeSchema,
      TupleTypeSchema,
      ArrayParamSchema,
    ]),
  ]),
]);

export type MapParamType = z.infer<typeof MapParamSchema>;

const PROTO = [
  "HTTP",
  "HTTPS"
] as const;

export const ProtoEnumSchema = z.enum(PROTO);

export type ProtoEnumType = z.infer<typeof ProtoEnumSchema>;

const METHOD = [
  "GET",
  "PUT",
  "POST",
  "DELETE",
  "PATCH",
  "HEAD",
  "OPTIONS",
] as const;

export const MethodEnumSchema = z.enum(METHOD);

export type MethodEnumType = z.infer<typeof MethodEnumSchema>;

const KeyValueParamsEnum = [
  "query",
  "headers",
] as const;

export const KeyValueParamsSchema = z.enum(KeyValueParamsEnum);

export type KeyValueParamsType = z.infer<typeof KeyValueParamsSchema>;

const MultiValueParamsEnum = [
  "qps",
  "concurrency",
] as const;

export const MultiValueParamsSchema = z.enum(MultiValueParamsEnum);

export type MultiValueParamsType = z.infer<typeof MultiValueParamsSchema>;

export const QpsSchema = z.object({
  startQPS: z.number(),
  endQPS: z.number(),
  duration: z.number(),
});

export type QPS = z.infer<typeof QpsSchema>;

export const ConcurrencySchema = z.object({
  threadCount: z.number(),
  initialDelay: z.number(),
  rampupTime: z.number(),
  duration: z.number(),
  shutdownTime: z.number(),
});

export type Concurrency = z.infer<typeof ConcurrencySchema>;

export const TestModeSchema = z.union([
  QpsSchema,
  ConcurrencySchema,
]);

export type TestMode = z.infer<typeof TestModeSchema>;

export const MultiValueParamSchema = TestModeSchema;

export type MultiValueParamType = TestMode;

export const CatalogTestParamSchema = z.object({
  id: ParamEnumSchema,
  label: z.string(),
  type: z.union([
    z.tuple([
      SimpleTypeEnumSchema,
    ]),
    UnionTypeSchema,
    TupleTypeSchema,
    ArrayParamSchema,
    MapParamSchema,
  ]),
  values: z.optional(
    z.array(z.string().nonempty())
  ),
  default: z.optional(
    z.union([
      z.boolean(),
      z.string().nonempty(),
      z.number(),
    ])
  ),
  min: z.optional(z.number()),
  max: z.optional(z.number()),
});

export type CatalogTestParam = z.infer<typeof CatalogTestParamSchema>;

export const CatalogTestParamsSchema = z.array(CatalogTestParamSchema);

export type CatalogTestParams = z.infer<typeof CatalogTestParamsSchema>;

export const CatalogTestParamsObjectSchema = z.record(ParamEnumSchema, CatalogTestParamSchema);

export type CatalogTestParamsObject = z.infer<typeof CatalogTestParamsObjectSchema>;

export const CatalogTestSchema = z.object({
  id: z.string(),
  name: z.string(),
  desc: z.string(),
  mode: ModeEnumSchema,
  params: ParamsEnumSchema,
});

export const CatalogSchema = z.object({
  params: CatalogTestParamsSchema,
  tests: z.array(CatalogTestSchema),
});

export type Catalog = z.infer<typeof CatalogSchema>;

export type CatalogTest = z.infer<typeof CatalogTestSchema>;
