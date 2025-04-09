<script lang="ts">
import { z } from 'zod'
import type { PropType } from 'vue';
import { debounce, toString } from 'lodash'
import type { CatalogTestParam } from '../types/catalogs.ts'
import { useTestStore } from '../stores/test.ts'
import {
  default as MultiValueInput,
  ModelValueSchema,
} from './MultiValueInput.vue'
import type { ModelValue } from './MultiValueInput.vue'

const DataSchema = z.object({
  values: z.record(
    z.number().nonnegative(),
    z.string(),
  ),
  counter: z.number().nonnegative(),
});

type Data = z.infer<typeof DataSchema>;

export default {
  props: {
    testParam: {
      type: Object as PropType<CatalogTestParam>,
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
    id() {
      return this.testParam?.id || "concurrency";
    },
  },

  methods: {
    addValue() {
      const key = (this.counter += 1);
      this.values[key] = "";
    },

    updateValue(data: ModelValue) {
      const TEST = useTestStore();
      TEST.setMultiValue(
        this.id,
        data.index,
        toString(data.value),
      );
      this.values[data.index] = data.value;
    },

    deleteIndex(index: number) {
      const TEST = useTestStore();
      TEST.unsetMultiValue(
        this.id, index
      );
      delete this.values[index];
    },
  },

  components: {
    MultiValueInput,
  }
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
      :key="testParam?.id + '-' + index"
    >
      <MultiValueInput
        :key="testParam?.id"
        :index="index"
        :test-param="testParam"
        @update:model-value="updateValue"
        @delete:index="deleteIndex"
      ></MultiValueInput>
    </v-container>
  </v-card>
</template>