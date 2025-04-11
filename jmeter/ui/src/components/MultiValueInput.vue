<script lang="ts">
import { z } from 'zod'
import { defineComponent } from 'vue';
import { debounce, isEmpty, gt, lte, toString, has, compact, join } from 'lodash'
import { ComplexTypeEnumSchema, MultiValueParamsSchema } from '../types/catalogs.ts'
import type { PropType, ComponentPublicInstance } from 'vue';
import type { CatalogTestParam, MultiValueParamsType } from '../types/catalogs.ts'

const DataSchema = z.object({
  value: z.string(),
  values: z.array(z.string()),
  index: z.number().positive(),
});

export const ModelValueSchema = DataSchema;

type Data = Omit<z.infer<typeof DataSchema>, "index">;

export type ModelValue = Omit<z.infer<typeof DataSchema>, "values">;

const MultiValueParamsLabelsScheme = z.record(
  MultiValueParamsSchema,
  z.array(z.string())
);

type MultiValueParamsLabels = z.infer<typeof MultiValueParamsLabelsScheme>;

const LABELS: MultiValueParamsLabels = {};

LABELS[MultiValueParamsSchema.Values.qps] = [
  "Start QPS",
  "End QPS",
  "Step Duration",
];

LABELS[MultiValueParamsSchema.Values.concurrency] = [
  "Thread Count",
  "Initial Delay",
  "Rampup Time",
  "Step Duration",
  "Shutdown Time",
];

export default defineComponent({
  props: {
    testParam: {
      type: Object as PropType<CatalogTestParam>,
      required: true,
    },

    index: {
      type: Number,
      required: true,
    },

    paramId: {
      type: Object as PropType<MultiValueParamsType>,
      required: true,
    }
  },

  data: () => {
    return {
      value: "",
      values: [],
    } as Data;
  },

  emits: {
    'update:modelValue': function (
      payload: ModelValue
    ) {
      return !isEmpty(payload.value) && gt(payload.index, 0);
    },
    'delete:index': function (
      index: number
    ) {
      return gt(index, 0);
    },
  },

  computed: {
    id(): MultiValueParamsType {
      try {
        return MultiValueParamsSchema.parse(this.paramId);
      } catch(error) {
        console.error(error);
      }
      return MultiValueParamsSchema.Enum.concurrency;
    },

    type(): string {
      if ( this.testParam.type.length == 2 ) {
        return this.testParam.type[1][0];
        
      }
      return this.testParam.type[0];
    },

    isTuple(): boolean {
      return this.type == ComplexTypeEnumSchema.Enum.tuple;
    },

    items(): string[] {
      if ( !this.isTuple ) {
        return [];
      } else if ( this.testParam.type.length == 2 ) {
        return this.testParam.type[1][1] as string[];
      }
      return [];
    },

    size() {
      return this.items.length || 1;
    },
  },

  methods: {
    emitUpdate: debounce(function (
      this: ComponentPublicInstance<ModelValue>,
      newValue: string,
    ) {
      if (isEmpty(newValue) || lte(this.index, 0)) {
        return;
      }

      const value: ModelValue = {
        value: newValue,
        index: this.index,
      };

      this.$emit('update:model-value', value);
    }, 300, { maxWait: 300 }),

    updateValue(value: any) {
      this.emitUpdate(
        toString(
          this.value = value
        )
      );
    },

    updateTupleValue(
      value: string,
      index: number,
      item: string,
    ) {
      if ( index == 0 ) {
        this.value = value;
      } else {
        this.values[index-1] = value;
      }

      const values = compact([
        this.value,
        ...this.values
      ]);
      if ( values.length == this.size ) {
        this.emitUpdate(join(values, ','));
      }
    },

    deleteValue() {
      this.$emit('delete:index', this.index);
    },

    label(index: number): string {
      const labels = LABELS[this.id];
      if ( has(LABELS, this.id) && labels ) {
        return labels[index];
      }
      return "";
    },
  },

  mounted() {
    this.values = new Array(this.size-1).fill('');
  },
});
</script>

<template>
  <v-row>
    <v-btn
        class="ms-2 mt-3"
        density="compact"
        icon="mdi-minus"
        color="error"
        @click="deleteValue"
    ></v-btn>
    <template v-if="isTuple">
      <v-col
        v-for="(item, i) in items"
        :key="index + '-' + id + '-' + item + '-' + i"
        class="py-0"
      >
        <v-text-field
          :label="label(i)"
          @update:model-value="updateTupleValue($event, i, item)"
        ></v-text-field>
      </v-col>
    </template>
    <v-col v-else class="py-0">
      <v-text-field
        label="Value"
        :model-value="value"
        @update:model-value="updateValue"
      ></v-text-field>
    </v-col>
  </v-row>
</template>