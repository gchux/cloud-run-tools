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
      constant(
        threadCount
      ),
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
  const rampUpMapper = partial(multiply, rampUpDelta);
  const rampUpSteps = chain(
    range(rampupTime)
  )
    .map(rampUpMapper)
    .value();

  const shutDownMapper = newConcurrencyMapper(concurrency);
  const shutDownSteps = chain(
    range(shutdownTime)
  )
    .map(shutDownMapper)
    .value();

  const stepsMapper = constant(threadCount);
  const steps = chain(
    range(duration)
  )
    .map(stepsMapper)
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
  const traffic = chain(trafficShape)
    .reduce((state, step) => {
      const { offset } = state;
      let { shape } = state;

      const sizeOfShape = size(shape);
      const sizeOfStep = size(step);

      const initialDelay = defaultTo(
        // unpack initial delay
        first(step), 0,
      );

      const end = add(initialDelay, sizeOfStep);

      let sizeOfGap: number = 0;
      if ( initialDelay > sizeOfShape ) {
        // offset is beyond current shape:
        // ( current shape )< ... [ current step ]>
        sizeOfGap = add(
          subtract(
            initialDelay,
            sizeOfShape
          ),
          sizeOfStep,
        );
      } else if ( end > sizeOfShape ) {
        // offset is within current shape:
        // ( current shape [ ... )< current step ]>
        sizeOfGap = subtract(end, sizeOfShape);
      }

      if ( sizeOfGap > 0 ) {
        // if there is a gap between the current end of the shape
        // and the end of the step ( which starts after the offset ),
        // then append the gap to the current shape and fill it with 0's.
        const gap = new Array(sizeOfGap);
        shape = concat(shape, fill(gap, 0));
      }

      // fixed section of `shape` so far
      const prefix = slice(shape, 0, initialDelay);

      // tail of `shape` so far
      const suffix = slice(shape, initialDelay);

      // handle overlap between suffix and current step
      const values = chain(
        // skip initial delay
        slice(step, 1)
      )
        .map((value, index) => {
          const delta = defaultTo(
            nth(suffix, index), 0,
          );
          // overlay current onto suffix
          return add(value, delta);
        })
        .value();

      return {
        offset: add(offset, initialDelay),
        shape: [
          ...prefix, // prepend prefix as-is
          ...values, // append everything new
        ],
      };
    }, {
      offset: 0,
      shape: new Array<number>(),
    })
    .value();

  return [
    0,
    ...(traffic.shape),
    0,
  ];
};