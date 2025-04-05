<script lang="ts">
import type { PropType } from 'vue';
import type {
  CatalogTestParamsObject,
  ParamsEnumType
} from '../types/catalogs.ts'
import TestParam from './TestParam.vue'

export default {
  props: {
    catalog: {
      type: Object as PropType<CatalogTestParamsObject>,
    },
    params: {
      type: Array as PropType<ParamsEnumType>,
    },
  },

  components: {
    TestParam,
  },

  mounted() {
    console.log(this.params);
    console.log(this.catalog);
  }
}
</script>

<template>
<TestParam
  v-if="catalog"
  v-for="param in params"
  :key="catalog[param]?.id"
  :param="catalog[param]"
>
</TestParam>
</template>