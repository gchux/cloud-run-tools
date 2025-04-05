import { startsWith } from 'lodash';
import { z } from 'zod';

const MODE = [
  "qps",
  "concurrency"
] as const;

export const ModeEnumSchema = z.enum(MODE);

export type ModeEnumType = z.infer<typeof ModeEnumSchema>;

const PARAM = [
  "async",
  "concurrency",
  "qps",
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

export const ParamsEnumSchema = z.array(ParamEnumSchema);

export type ParamEnumType = z.infer<typeof ParamEnumSchema>;

export type ParamsEnumType = z.infer<typeof ParamsEnumSchema>;

const TYPE = [
  "string",
  "text",
  "number",
  "boolean",
  "array",
  "map",
  "union",
  "tuple",
] as const;

const TypeEnumSchema = z.enum(TYPE);

export const ArrayTypeSchema = z.tuple([
  z.string().startsWith("array").endsWith("array").length(5),
  z.union([      // type of values
    z.string(),
    z.array(z.string()),
  ]), 
]);

export const MapTypeSchema = z.tuple([
  z.string().startsWith("map").endsWith("map").length(3),
  z.union([      // type of key
    z.string(),
    z.array(z.string()),
  ]),
  z.union([      // type of value
    z.string(),
    z.array(z.string()),
  ]),  
]);

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

export const CatalogTestParamSchema = z.object({
  id: ParamEnumSchema,
  label: z.string(),
  type: z.union([
    z.array(
      z.string()
    ).length(1),
    ArrayTypeSchema,
    MapTypeSchema,
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
