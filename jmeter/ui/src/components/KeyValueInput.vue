<script lang="ts">
import { z } from 'zod'
import { defineComponent } from 'vue';
import { debounce, isEmpty, gt, lte, toString } from 'lodash'
import type { PropType, ComponentPublicInstance } from 'vue';
import type { CatalogTestParam } from '../types/catalogs.ts'

export const SOURCE = [
  "name",
  "value",
  "empty",
  "delete"
] as const;

export const SourceSchema = z.enum(SOURCE);

type Source = z.infer<typeof SourceSchema>;

const DataSchema = z.object({
  name: z.string(),
  value: z.string(),
  source: z.enum(SOURCE),
  index: z.number().positive(),
});

export const ModelValueSchema = DataSchema;

type Data = Omit<z.infer<typeof DataSchema>, "index" | "source">;

export type ModelValue = z.infer<typeof DataSchema>;

const component = defineComponent({
  props: {
    testParam: {
      type: Object as PropType<CatalogTestParam>,
    },
    index: {
      type: Number,
      required: true,
    },
  },

  data: () => {
    return {
      name: "",
      value: "",
    } as Data;
  },

  emits: {
    'update:modelValue': function (
      payload: ModelValue
    ) {
      return !isEmpty(payload.name) && !isEmpty(payload.value) && gt(payload.index, 0);
    },
    'delete:index': function (
      index: number
    ) {
      return index > 0;
    }
  },

  methods: {
    emitUpdate: debounce(function (
      this: ComponentPublicInstance<ModelValue>,
      source: Source
    ) {
      if (isEmpty(this.name) || isEmpty(this.value) || lte(this.index, 0)) {
        return;
      }

      const value: ModelValue = {
        name: this.name,
        value: this.value,
        index: this.index,
        source,
      };

      this.$emit('update:model-value', value);
    }, 300, { maxWait: 300 }),

    updateName(name: any) {
      this.name = toString(name);
      this.emitUpdate(SourceSchema.Values.name);
    },

    updateValue(value: string) {
      this.value = toString(value);
      this.emitUpdate(SourceSchema.Values.value);
    },

    deleteIndex() {
      this.$emit('delete:index', this.index);
    },
  },
});

export default component;
</script>

<template>
  <v-row>
    <v-btn
      class="ms-2 mt-3"
      density="compact"
      icon="mdi-minus"
      color="error"
      @click="deleteIndex"
    />

    <v-col class="py-0">
      <v-text-field
        label="Name"
        :model-value="name"
        @update:model-value="updateName"
      />
    </v-col>

    <v-col class="py-0">
      <v-text-field
        label="Value"
        :model-value="value"
        @update:model-value="updateValue"
      />
    </v-col>
  </v-row>
</template>