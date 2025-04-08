<script lang="ts">
import { z } from 'zod'
import type { PropType } from 'vue';
import type { CatalogTestParam } from '../types/catalogs.ts'
import { useTestStore } from '../stores/test.ts'
import { toString } from 'lodash'
import KeyValueParam from './KeyValueParam.vue'

const DataSchema = z.object({
  value: z.any(),
  component: z.string(),
});

type Data = z.infer<typeof DataSchema>;

export default {
  props: {
    param: {
      type: Object as PropType<CatalogTestParam>,
    }
  },

  data: () => {
    return {
      component: "v-text-field",
      value: "",
    } as Data;
  },

  computed: {
    items(): string[] {
      if ( this.type == "enum" ) {
        return this.param?.values || [];
      }
      return [];
    },
    type(): String | undefined {
      return this.param?.type[0];
    },
    component() {
      switch(this.type) {
        case "string":
        default:
          return "v-text-field";
        case "text":
          return "v-textarea";
        case "enum":
          return "v-select";
        case "bool":
        case "boolean":
          return "v-switch";
        case "map":
          return "KeyValueParam"
      }
    },
  },

  methods: {
    updateValue(value: any) {
      const TEST = useTestStore();
      TEST.setValue(
        this.param?.id,
        toString(value)
      );
      this.value = value;
    },
  },

  mounted() {
    console.log("param", this.param?.id, this.param);
    const defualtValue = this.param?.default;
    if ( this.type == "boolean" ) {
      this.updateValue(toString(defualtValue || "false"));
    }
  },

  components: {
    KeyValueParam,
  }
}
</script>

<template>
  <v-responsive
    class="mx-auto"
  >
    <component
      v-if="param"
      :is="component"
      :model-value="value"
      :label="param?.label"
      :items="items"
      :test-param="param"
      @update:model-value="updateValue"
    ></component>
  </v-responsive>
</template>