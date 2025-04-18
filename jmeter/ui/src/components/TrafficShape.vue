<script lang="ts">
import { z } from 'zod'
import {
  get,
  chain,
  toNumber,
  isEmpty,
  isUndefined,
} from 'lodash'
import {
  toShapeOfQPS,
  trafficShapeOfQPS,
  toShapeOfConcurrency,
  trafficShapeOfConcurrency
} from '../utils/trafficShape.ts'
import type { PropType } from 'vue';
import {
  type QPS,
  type Concurrency,
  MultiValueParamSchema,
  type MultiValueParamType,
} from '../types/catalogs.ts'

export const ValuesSchema = z.record(
  z.number().nonnegative(),
  z.optional(MultiValueParamSchema),
);

export type Values = z.infer<typeof ValuesSchema>;

const ComponentDataSchema = z.object({
  trafficShape: z.array(z.number().nonnegative()),
});

type ComponentData = z.infer<typeof ComponentDataSchema>;

export default {
  props: {
    shape: {
      type: Object as PropType<string>,
      required: true,
    },

    modelValues: {
      type: Object as PropType<Values>,
      required: true,
    },

    isQps: {
      type: Object as PropType<boolean>,
      required: true,
    },

    isConcurrency: {
      type: Object as PropType<boolean>,
      required: true,
    },
  },

  data: () => {
    return {
      trafficShape: [],
    } as ComponentData;
  },

  computed: {
    hasTrafficShape(): boolean {
      return !isEmpty(this.trafficShape);
    },
  },

  methods: {
    getTrafficShape(values: Values): number[][] {
      if (isEmpty(values)) {
        return [];
      }

      return chain(values)
        .keys()
        .map(toNumber)
        .sort()
        .map((
          key: number,
        ): number[] | undefined => {
          const value = get(values, key);
          if (isUndefined(value)) {
            return undefined;
          }
          return this.isQps ?
            toShapeOfQPS(value as QPS)
            : toShapeOfConcurrency(value as Concurrency);
        })
        .compact()
        .value() || [];
    },
  },

  watch: {
    modelValues: {
      handler(newValues: Values): number[] {
        const trafficShape = this.getTrafficShape(newValues);

        if (isEmpty(trafficShape) || isEmpty(trafficShape)) {
          return (this.trafficShape = []);
        } else if (this.isQps) {
          return (
            this.trafficShape = trafficShapeOfQPS(trafficShape)
          );
        } else if (this.isConcurrency) {
          return (
            this.trafficShape = trafficShapeOfConcurrency(trafficShape)
          );
        }

        return (this.trafficShape = []);
      },
      immediate: true,
      deep: true,
    },
  },

  created() {
    console.log(this.modelValues);
  },
};
</script>

<template>
  <v-sheet
    v-if="hasTrafficShape"
    class="px-3 pt-3 bg-black text-green"
  >
    <v-sparkline
      :model-value="trafficShape"
      :fill="false"
      :line-width="0.5"
      :padding="0.0"
      :smooth="true"
      :auto-draw="true"
    />
  </v-sheet>
</template>