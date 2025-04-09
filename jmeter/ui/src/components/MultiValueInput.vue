<script lang="ts">
import { z } from 'zod'
import type { PropType, ComponentPublicInstance } from 'vue';
import { defineComponent } from 'vue';
import { bind, debounce, isEmpty, gt, lte, toString } from 'lodash'
import { useTestStore } from '../stores/test.ts'

const DataSchema = z.object({
  value: z.string(),
  index: z.number().positive(),
});

export const ModelValueSchema = DataSchema;

type Data = Omit<z.infer<typeof DataSchema>, "index">;

export type ModelValue = z.infer<typeof DataSchema>;

export default defineComponent({
  props: {
    index: {
      type: Number,
      required: true,
    },
  },

  data: () => {
    return {
      value: "",
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
    }
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

    deleteValue() {
      this.$emit('delete:index', this.index);
    },
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
    <v-col class="py-0">
      <v-text-field
        label="Value"
        :model-value="value"
        @update:model-value="updateValue"
      ></v-text-field>
    </v-col>
  </v-row>
</template>