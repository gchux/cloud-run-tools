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
      default: [],
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
    v-if="catalog && (params.length > 0)"
    v-for="(param, index) in params"
    :key="catalog[param]?.id || index"
    :param="catalog[param]"
  ></TestParam>
</template>