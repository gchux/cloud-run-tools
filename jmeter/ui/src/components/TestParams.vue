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
}
</script>

<template>
  <v-row
    v-if="catalog && (params.length > 0)"
    v-for="(row, i) in params"
    :key="'params' + '-' + i"
  >
    <v-col
      v-if="row.length > 0"
      v-for="(param, j) in row"
      :key="'params' + '-' + i + '-' + j"
    >
      <TestParam
        :key="catalog[param]?.id"
        :param="catalog[param]"
      ></TestParam>
    </v-col>
  </v-row>
</template>