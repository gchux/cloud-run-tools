<script lang="ts">
import { z } from 'zod'
import { toString, toNumber } from 'lodash'
import { useTestStore } from '../stores/test.ts'
import { useMessagesStore } from '../stores/messages.ts'
import {
  default as KeyValueInput,
  ModelValueSchema,
  SourceSchema,
} from './KeyValueInput.vue'
import type { PropType } from 'vue';
import type { ParamEnumType, CatalogTestParam } from '../types/catalogs.ts'
import type { ModelValue } from './KeyValueInput.vue'

const DataSchema = z.object({
  values: z.record(
    z.number().nonnegative(),
    ModelValueSchema,
  ),
  counter: z.number().nonnegative(),
});

type Data = z.infer<typeof DataSchema>;

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
    id(): ParamEnumType {
      return this.testParam.id;
    },
  },

  methods: {
    addKeyValue() {
      const key = (this.counter += 1);
      this.values[key] = {
        index: key,
        name: "",
        value: "",
        source: SourceSchema.Values.empty,
      };
    },

    updateValue(data: ModelValue) {
      const MESSAGES = useMessagesStore();
      const TEST = useTestStore();

      const index = toNumber(data.index);

      try {
        TEST.setKeyValue(
          this.id,
          index,
          data.name,
          toString(data.value),
        );
        this.values[index] = data;
      } catch(error) {
        MESSAGES.parameterError(
          this.id,
          data.value,
          error as z.ZodError
        );
      }
    },

    deleteIndex(index: number) {
      const i = toNumber(index);
      const TEST = useTestStore();
      TEST.unsetKeyValue(this.id, i);
      delete this.values[i];
    },
  },

  components: {
    KeyValueInput,
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
          @click="addKeyValue"
        />
      </template>
    </v-list-item>
    
    <v-container
      v-for="(value, index) in values"
      :key="id + '-' + index"
    >
      <KeyValueInput
        :index="index"
        @update:model-value="updateValue"
        @delete:index="deleteIndex"
      />
    </v-container>
  </v-card>
</template>