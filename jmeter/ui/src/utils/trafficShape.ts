import { z } from 'zod'
import {
  toNumber,
  isEqual,
  chain,
  slice,
  defaultTo,
  range,
  constant,
  add,
  subtract,
  divide,
  multiply,
  partial,
  overArgs,
  first,
  nth,
  size,
  fill,
  concat,
} from 'lodash'
import {
  MultiValueParamSchema,
  MultiValueParamsSchema,
} from '../types/catalogs.ts'
import type {
  MultiValueParamType,
  MultiValueParamsType,
  QPS, Concurrency,
} from '../types/catalogs.ts'

const ValuesSchema = z.record(
  z.number().nonnegative(),
  z.optional(MultiValueParamSchema),
);

type ValuesType = z.infer<typeof ValuesSchema>;

const MapperSchema = z.function()
  .args(
    z.number(),
    z.number(),
  )
  .returns(
    z.number()
  );

type Mapper = z.infer<typeof MapperSchema>;

const newQpsMapper = (
  qps: QPS,
): Mapper => {
  const duration = toNumber(qps.duration);
  const startQPS = toNumber(qps.startQPS);
  const endQPS = toNumber(qps.endQPS);
  const delta = divide(subtract(endQPS, startQPS), duration);

  // functional way of applying:
  //   - `startQPS + (step * delta)`
  //   - see: https://lodash.com/docs/4.17.15#overArgs
  return overArgs(
    add, [
    constant(startQPS),
    partial(
      multiply, delta,
    )
  ],
  ) as Mapper;
};

const newConcurrencyMapper = (
  concurrency: Concurrency,
): Mapper => {
  const threadCount = toNumber(concurrency.threadCount);
  const shutdownTime = toNumber(concurrency.shutdownTime);
  const shutDownDelta = divide(threadCount, shutdownTime);

  // functional way of applying:
  //   - `threadCount - (step * shutDownDelta)`
  //   - see: https://lodash.com/docs/4.17.15#overArgs
  return overArgs(
    subtract, [
      constant(threadCount),
      partial(
        multiply,
        shutDownDelta,
      ),
    ]
  ) as Mapper;
};

const newMapper = (
  param: MultiValueParamsType,
  value: MultiValueParamType,
) => {
  const id = MultiValueParamsSchema.parse(param);
  if (isEqual(id, MultiValueParamsSchema.Enum.qps)) {
    return newQpsMapper(value as QPS);
  }
  return newConcurrencyMapper(value as Concurrency);
};

export const toShapeOfQPS = (
  qps: QPS,
): number[] => {
  const duration = toNumber(qps.duration);

  return chain(
    range(duration)
  )
    .map(
      newQpsMapper(qps)
    )
    .flatten()
    .value();
};

export const trafficShapeOfQPS = (
  trafficShape: number[][],
): number[] => {
  const shape = chain(
    trafficShape
  ).flatten()
    .compact()
    .concat(0)
    .value();

  return [
    0,
    ...shape,
    0,
  ];
};

export const toShapeOfConcurrency = (
  concurrency: Concurrency,
): number[] => {
  const initialDelay = toNumber(concurrency.initialDelay);
  const threadCount = toNumber(concurrency.threadCount);
  const rampupTime = toNumber(concurrency.rampupTime);
  const duration = toNumber(concurrency.duration);
  const shutdownTime = toNumber(concurrency.shutdownTime);

  const rampUpDelta = divide(threadCount, rampupTime);
  const rampUpSteps = chain(
    range(rampupTime)
  )
    .map(
      partial(multiply, rampUpDelta)
    )
    .value();

  const shutDownSteps = chain(
    range(shutdownTime)
  )
    .map(
      newConcurrencyMapper(concurrency)
    )
    .value();

  const steps = chain(
    range(duration)
  )
    .map(
      constant(threadCount)
    )
    .value();

  return [
    // pack initial delay
    initialDelay,
    ...rampUpSteps,
    ...steps,
    ...shutDownSteps,
    0,
  ];
};

export const trafficShapeOfConcurrency = (
  trafficShape: number[][],
): number[] => {
  const firstStep = defaultTo(
    first(trafficShape), [],
  );

  const traffic = chain(trafficShape)
    .slice(1) // skip 1st step
    .reduce((state, step) => {
      const { offset } = state;
      let { shape } = state;

      const sizeOfShape = size(shape);
      const sizeOfStep = size(step);

      const currentOffset = defaultTo(
        // unpack initial delay
        first(step), 0,
      );

      const end = add(currentOffset, sizeOfStep);

      let sizeOfGap: number;
      if ( currentOffset > sizeOfShape ) {
        sizeOfGap = add(
          subtract(
            currentOffset,
            sizeOfShape
          ),
          sizeOfStep,
        );
      } if ( end > sizeOfShape ) {
        sizeOfGap = subtract(end, sizeOfShape);
      } else {
        sizeOfGap = 0;
      }
      
      const gap = new Array(sizeOfGap);
      shape = concat(shape, fill(gap, 0));

      // fixed section of `shape` so far
      const prefix = slice(shape, 0, currentOffset);

      // tail of `shape` so far
      const suffix = slice(shape, currentOffset);

      // handle overlap between suffix and current step
      const values = chain(
        slice(step, 1)
      )
        .map((value, index) => {
          const delta = defaultTo(
            nth(suffix, index), 0,
          );
          return add(value, delta);
        })
        .value();

      return {
        offset: add(offset, currentOffset),
        shape: [
          ...prefix, // prepend prefix as-is
          ...values, // append everything new
        ],
      };
    }, {
      offset: defaultTo(
        // unpack initial delay
        first(firstStep), 0,
      ),
      shape: slice(firstStep, 1), // skip 1st step
    })
    .value();

  return [
    0,
    ...(traffic.shape),
    0,
  ];
};