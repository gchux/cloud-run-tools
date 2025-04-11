<script lang="ts">
import { z } from 'zod'
import { split, toNumber } from 'lodash'
import { MultiValueParamsSchema } from '../types/catalogs.ts'
import { useTestStore } from '../stores/test.ts'
import { useMessagesStore } from '../stores/messages.ts'
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
import type { MultiValueParam } from '../stores/test.ts'
import type { ModelValue } from './MultiValueInput.vue'

const DataSchema = z.object({
  values: z.record(
    z.number().nonnegative(),
    z.string(),
  ),
  counter: z.number().nonnegative(),
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
  },

  methods: {
    addValue() {
      const key = (this.counter += 1);
      this.values[key] = "";
    },

    updateValue(
      data: ModelValue
    ): MultiValueParam {
      const MESSAGES = useMessagesStore();
      
      let param: MultiValueParamType;
      
      try {
        param = toTestParam(this.testParam, data);
      } catch(error) {
        MESSAGES.Error(error as Error);
        throw error;
      }

      this.values[data.index] = data.value;

      const TEST = useTestStore();

      const i = toNumber(data.index);
      switch(this.id) {
        case MultiValueParamsSchema.Enum.qps:
          return TEST.setQPS(i, param as QPS);
        case MultiValueParamsSchema.Enum.concurrency:
          return TEST.setConcurrency(i, param as Concurrency);
        default:
          return TEST.setMultiValue(this.id, i, param);
      }
    },

    deleteIndex(index: number) {
      const TEST = useTestStore();

      delete this.values[index];
     
      const i = toNumber(index);
      switch(this.id) {
        case MultiValueParamsSchema.Enum.qps:
          return TEST.unsetQPS(i);
        case MultiValueParamsSchema.Enum.concurrency:
          return TEST.unsetConcurrency(i);
        default:
          return TEST.unsetMultiValue(this.id, i);
      }
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
      :key="testParam.id + '-' + index"
    >
      <MultiValueInput
        :key="testParam.id"
        :param-id="id"
        :index="index"
        :test-param="testParam"
        @update:model-value="updateValue"
        @delete:index="deleteIndex"
      ></MultiValueInput>
    </v-container>
  </v-card>
</template>