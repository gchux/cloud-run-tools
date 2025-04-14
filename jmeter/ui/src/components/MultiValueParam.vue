<script lang="ts">
import { z } from 'zod'
import {
  split,
  toNumber,
  isUndefined,
  isEmpty,
  isEqual,
  lte,
  chain,
  get,
} from 'lodash'
import {
  MultiValueParamsSchema,
  MultiValueParamSchema,
} from '../types/catalogs.ts'
import { useTestStore, DurationSchema } from '../stores/test.ts'
import { useMessagesStore } from '../stores/messages.ts'
import {
  toShapeOfQPS,
  trafficShapeOfQPS,
  toShapeOfConcurrency,
  trafficShapeOfConcurrency
} from '../utils/trafficShape.ts'
import {
  default as MultiValueInput,
} from './MultiValueInput.vue'
import type { PropType } from 'vue';
import type {
  ArrayParamType,
  CatalogTestParam,
  MultiValueParamsType,
  MultiValueParamType,
  QPS, Concurrency,
} from '../types/catalogs.ts'
import type { ModelValue } from './MultiValueInput.vue'

const ValuesSchema = z.record(
  z.number().nonnegative(),
  z.optional(MultiValueParamSchema),
);

const DataSchema = z.object({
  values: ValuesSchema,
  counter: z.number().nonnegative(),
  duration: z.number().nonnegative(),
  trafficShape: z.array(z.number().nonnegative()),
});

type Data = z.infer<typeof DataSchema>;

const toTestParam = (
  param: CatalogTestParam,
  data: ModelValue,
): MultiValueParamType => {
  const parts = split(data.value, ',');

  switch(param.id) {
    case MultiValueParamsSchema.Enum.qps:
      if ( parts.length == 3 ) {
        return {
          startQPS: toNumber(parts[0]),
          endQPS: toNumber(parts[1]),
          duration: toNumber(parts[2]),
        }; 
      }
      break;

    case MultiValueParamsSchema.Enum.concurrency:
      if ( parts.length == 5 ) {
        return {
          threadCount: toNumber(parts[0]),
          initialDelay: toNumber(parts[1]),
          rampupTime: toNumber(parts[2]),
          duration: toNumber(parts[3]),
          shutdownTime: toNumber(parts[4]),
        }; 
      }
      break;

    default:
      throw new Error(`invalid multi value parameter: '${param.id}'`);
  }
  throw new Error(`invalid shape for '${param.id}': '[${data.value}]'`);
};

export default {
  props: {
    testParam: {
      type: Object as PropType<CatalogTestParam>,
      required: true,
    },
    label: {
      type: String,
      required: true,
    },
  },

  data: () => {
    return {
      counter: 0,
      values: {},
      duration: 0,
      trafficShape: []
    } as Data;
  },

  computed: {
    id(): MultiValueParamsType {
      try {
        return MultiValueParamsSchema.parse(this.testParam.id);
      } catch(error) {
        console.error(error);
      }
      return MultiValueParamsSchema.Enum.concurrency;
    },

    type(): ArrayParamType {
      return this.testParam.type as ArrayParamType;
    },

    isQPS(): boolean {
      return isEqual(this.id, MultiValueParamsSchema.Enum.qps);
    },

    isConcurrency(): boolean {
      return isEqual(this.id, MultiValueParamsSchema.Enum.concurrency);
    },
    
    isTrafficShape(): boolean {
      return this.isQPS || this.isConcurrency;
    },

    hasTrafficShape(): boolean {
      return this.isTrafficShape && !isEmpty(this.trafficShape);
    },

    hasValidDuration(): boolean {
      const TEST = useTestStore();
      const duration = DurationSchema.safeParse(this.duration);
      return !this.isTrafficShape || (
              duration.success
              &&
              ( TEST.isAsync() || lte(duration.data, 3600) ) 
              && 
              isEqual(duration.data, TEST.getDuration())
            );
    },
  },

  methods: {
    addValue(): number {
      const key = (this.counter += 1);
      this.values[key] = undefined;
      return key;
    },

    getTrafficShape(): number[][] {
      return chain(this.values)
        .keys()
        .map(toNumber)
        .sort()
        .map((
          key: number,
        ): number[] => {
          return this.isQPS ?
            toShapeOfQPS(
              get(this.values, key) as QPS
            ) :
            toShapeOfConcurrency(
              get(this.values, key) as Concurrency
            );
        })
        .value() || [];
    },

    updateTrafficShape(): number[] {
      const trafficShape = this.getTrafficShape();
      if (isEmpty(trafficShape) || isEmpty(trafficShape)) {
        return (this.trafficShape = []);
      } else if (this.isQPS) {
        return (this.trafficShape = trafficShapeOfQPS(trafficShape));
      } else if(this.isConcurrency) {
        return (this.trafficShape = trafficShapeOfConcurrency(trafficShape));
      }
      return (this.trafficShape = []);
    },

    increaseTotalDuration(
      index: number,
      param: MultiValueParamType,
    ): number {
      if ( !this.isTrafficShape ) {
        return this.duration;
      }

      this.decreaseTotalDuration(index);

      switch(this.id) {
        case MultiValueParamsSchema.Enum.concurrency:
          const p = param as Concurrency;
          this.duration += p.rampupTime + p.shutdownTime;
          /* falls through */
        
          case MultiValueParamsSchema.Enum.qps:
          this.duration += param.duration;
      }
      return this.duration;
    },

    decreaseTotalDuration(
      index: number,
    ): number {
      if ( !this.isTrafficShape ) {
        return this.duration;
      }

      const param = this.values[index];

      if ( isUndefined(param) ) {
        return this.duration;
      }

      switch(this.id) {
        case MultiValueParamsSchema.Enum.concurrency:
          const p = param as Concurrency;
          this.duration -= p.rampupTime + p.shutdownTime;
          /* falls through */
        
        case MultiValueParamsSchema.Enum.qps:
          this.duration -= param.duration;
      }
      return this.duration;
    },

    setValue(
      data: ModelValue,
      param: MultiValueParamType,
    ) {
      const i = toNumber(data.index);

      const MESSAGES = useMessagesStore();
      const TEST = useTestStore();

      // handle ALL possible multi-value params
      try {
        switch(this.id) {
          case MultiValueParamsSchema.Enum.qps:
            TEST.setQPS(i, param as QPS);
            break;

          case MultiValueParamsSchema.Enum.concurrency:
            TEST.setConcurrency(i, param as Concurrency);
            break;

          default:
            TEST.setMultiValue(this.id, i, param);
            break;
        }
      } catch(error) {
        MESSAGES.parameterError(
          this.id,
          data.value,
          error as z.ZodError
        );
      }

      if ( this.isTrafficShape ) {
        this.increaseTotalDuration(i, param);
      }
      this.values[i] = param;
      this.updateTrafficShape();
    },

    updateValue(
      data: ModelValue
    ) {
      const MESSAGES = useMessagesStore();
      
      let param: MultiValueParamType;
      
      try {
        param = toTestParam(this.testParam, data);
      } catch(error) {
        MESSAGES.Error(error as Error);
        return undefined;
      }
      
      return this.setValue(data, param);
    },

    deleteIndex(index: number) {
      const TEST = useTestStore();

      const i = toNumber(index);

      switch(this.id) {
        case MultiValueParamsSchema.Enum.qps:
          TEST.unsetQPS(i);
          break;

        case MultiValueParamsSchema.Enum.concurrency:
          TEST.unsetConcurrency(i);
          break;

        default:
          TEST.unsetMultiValue(this.id, i);
          break;
      }

      this.decreaseTotalDuration(i);
      delete this.values[i];
      this.updateTrafficShape();
    },
  },

  components: {
    MultiValueInput,
  },
}
</script>

<template>
  <v-card class="mb-3">
    <v-list-item>
      <template v-slot:title>{{ label }}</template>
      <template
        v-if="isTrafficShape"
        v-slot:subtitle
      >
        total test duration: <b :class="'text-' + [hasValidDuration ? 'green' : 'red']"><code>{{ duration }}</code></b>
      </template>
      <template v-slot:append>
        <v-btn
          class="my-3"
          density="compact"
          icon="mdi-plus"
          color="success"
          @click="addValue"
        ></v-btn>
      </template>
    </v-list-item>
    
    <v-container
      v-for="(value, index) in values"
      :key="id + '-' + index"
    >
      <MultiValueInput
        :key="id"
        :param-id="id"
        :index="index"
        :test-param="testParam"
        @update:model-value="updateValue"
        @delete:index="deleteIndex"
      ></MultiValueInput>
    </v-container>

    <v-alert
      v-if="!hasValidDuration"
      title="Invalid Traffic Shape"
      type="error"
      variant="tonal"
    >
      Total test duration must be greater than <b><code>10 seconds</code></b>.<br>
      Maximum duration for <b>Cloud Run <code>non-async</code></b> tests is <b><code>3600 seconds</code></b>.<br>
      Total <b>traffic shape duration</b> must be equal to <b>test duration in seconds</b>.<br>
      If yo need to run a load test for more than <b><code>1 hour</code></b>,
      consider running an <b><code>async</code></b> test instead,
      and make sure to use <b><code>instance-based</code> billing</b>.
    </v-alert>

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
      ></v-sparkline>
    </v-sheet>
  </v-card>
</template>