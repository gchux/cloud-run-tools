<script lang="ts">
import { z } from 'zod'
import {
  lte,
  set,
  omit,
  toNumber,
  isUndefined,
  isEqual,
} from 'lodash'
import {
  MultiValueParamsSchema,
} from '../types/catalogs.ts'
import { useTestStore, DurationSchema } from '../stores/test.ts'
import { useMessagesStore } from '../stores/messages.ts'
import { toTestParam } from '../utils/test.ts'
import {
  default as MultiValueInput,
} from './MultiValueInput.vue'
import {
  ValuesSchema,
  default as TrafficShape,
} from './TrafficShape.vue'
import type { PropType } from 'vue';
import type {
  ArrayParamType,
  CatalogTestParam,
  MultiValueParamsType,
  MultiValueParamType,
  QPS, Concurrency,
} from '../types/catalogs.ts'
import type { ModelValue } from './MultiValueInput.vue'

const ComponentDataSchema = z.object({
  values: ValuesSchema,
  counter: z.number().nonnegative(),
  duration: z.number().nonnegative(),
});

type ComponentData = z.infer<typeof ComponentDataSchema>;

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
    } as ComponentData;
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
      this.values = set(this.values, key, undefined);
      return key;
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

      this.increaseTotalDuration(i, param);

      this.values = set(this.values, i, param);
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

      this.values = omit(this.values, [i]);
    },
  },

  components: {
    TrafficShape,
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
        total test duration:
        <b :class="'text-' + [hasValidDuration ? 'green' : 'red']">
          <code>{{ duration }}</code>
        </b>
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

    <TrafficShape
      v-if="isTrafficShape"
      :shape="id"
      :is-qps="isQPS"
      :is-concurrency="isConcurrency"
      :model-values="values"
    />
  </v-card>
</template>