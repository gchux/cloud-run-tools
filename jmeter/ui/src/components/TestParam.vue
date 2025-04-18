<script lang="ts">
import { z } from 'zod'
import { useTestStore, DurationSchema } from '../stores/test.ts'
import { useMessagesStore } from '../stores/messages.ts'
import { toString } from 'lodash'
import { TypeEnumSchema, ParamEnumSchema } from '../types/catalogs.ts'
import MultiValueParam from './MultiValueParam.vue'
import KeyValueParam from './KeyValueParam.vue'
import type { PropType } from 'vue';
import type { ParamEnumType, CatalogTestParam } from '../types/catalogs.ts'

const DataSchema = z.object({
  value: z.any(),
  component: z.string(),
});

type Data = z.infer<typeof DataSchema>;

export default {
  props: {
    testParam: {
      type: Object as PropType<CatalogTestParam>,
      required: true,
    }
  },

  data: () => {
    return {
      component: "v-text-field",
      value: "",
    } as Data;
  },

  computed: {
    id(): ParamEnumType {
      return this.testParam.id;
    },

    defaultValue() {
      return this.testParam.default;
    },

    values(): string[] {
      return this.testParam.values || [];
    },
    
    items(): string[] {
      if ( this.type == TypeEnumSchema.Enum.enum ) {
        return this.values;
      }
      return [];
    },
    
    type(): String | undefined {
      return this.testParam.type[0];
    },

    isDuration(): boolean {
      return this.id == ParamEnumSchema.Enum.duration;
    },
    
    component() {
      switch(this.type) {
        case TypeEnumSchema.Enum.string:
        case TypeEnumSchema.Enum.str:
        case TypeEnumSchema.Enum.number:
        case TypeEnumSchema.Enum.num:
        case TypeEnumSchema.Enum.integer:
        case TypeEnumSchema.Enum.int:
        default:
          return "v-text-field";
        case TypeEnumSchema.Enum.text:
        case TypeEnumSchema.Enum.txt:
          return "v-textarea";
        case TypeEnumSchema.Enum.enum:
          return "v-select";
        case TypeEnumSchema.Enum.boolean:
        case TypeEnumSchema.Enum.bool:
          return "v-switch";
        case TypeEnumSchema.Enum.array:
        case TypeEnumSchema.Enum.list:
        case TypeEnumSchema.Enum.set:
          return "MultiValueParam";
        case TypeEnumSchema.Enum.map:
        case TypeEnumSchema.Enum.kv:
          return "KeyValueParam";
      };
    },
  },

  methods: {
    updateValue(value: any) {
      const MESSAGES = useMessagesStore();
      const TEST = useTestStore();
      try {
        TEST.setValue(
          this.id,
          toString(value)
        );
        this.value = value;
      } catch(error) {
        MESSAGES.parameterError(
          this.id,
          value,
          error as z.ZodError
        );
      }
    },
  },

  mounted() {
    const TEST = useTestStore();
    if ( this.type == TypeEnumSchema.Enum.boolean ) {
      this.updateValue(
        toString(this.defaultValue || "false")
      );
    }
    this.value = TEST.getValue(this.id);
  },

  components: {
    KeyValueParam,
    MultiValueParam,
  }
}
</script>

<template>
  <v-responsive
    class="mx-auto"
  >
    <component
      :is="component"
      :model-value="value"
      :label="testParam.label"
      :items="items"
      :test-param="testParam"
      @update:model-value="updateValue"
    />
  </v-responsive>
</template>