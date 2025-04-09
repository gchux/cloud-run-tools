<script lang="ts">
import { first, keyBy } from 'lodash';
import jmaas from '../api/jmaas.ts';
import { useTestStore } from '../stores/test.ts'
import type { Test } from '../stores/test.ts'
import type {
  Catalog,
  CatalogTest,
  CatalogTestParamsObject,
  ParamEnumType
} from '../types/catalogs.ts'
import { ModeEnumSchema, CatalogSchema } from '../types/catalogs.ts'
import TestParams from './TestParams.vue'

type Data = {
  catalog: Catalog,
  tests: CatalogTest[],
  params: CatalogTestParamsObject,
  test: CatalogTest,
};

const defaultTest: CatalogTest = {
  id: "",
  name: "loading...",
  desc: "loading...",
  mode: ModeEnumSchema.Values.qps,
  params: [],
};

export default {
  data: () => {
    return {
      test: defaultTest,
      tests: [],
      params: [],
    } as Partial<Data>;
  },

  methods: {
    async fetchCatalog() {
      const response = await jmaas.getCatalog();
      this.catalog = CatalogSchema.parse(response.data);
      console.log(this.catalog);
      this.params = keyBy(this.catalog.params, 'id');
      this.tests = this.catalog.tests;
      this.test = first(this.tests);
    },

    updateTest(test: CatalogTest) {
      const TEST = useTestStore();
      TEST.setScript(test.id);
      TEST.setMode(test.mode);
      this.test = test;
    },

    runTest() {
      const TEST = useTestStore();
      jmaas.runTest(TEST.get());
    },
  },

  mounted() {
    this.fetchCatalog();
  },

  components: {
    TestParams,
  }
}
</script>

<template>
  <v-select
    :model-value="test"
    :items="tests"
    item-title="name"
    item-value="id"
    return-object
    single-line
    @update:model-value="updateTest"
  >
    <template v-slot:item="{ props: itemProps, item }">
      <v-list-item v-bind="itemProps" :subtitle="item.raw.desc"></v-list-item>
    </template>
  </v-select>
  <TestParams
    v-if="catalog"
    :catalog="params"
    :params="test?.params"
  ></TestParams>
  <v-btn block
    class="text-none"
    color="success"
    size="x-large"
    variant="flat"
    @click=""
  >
    Run the Load Test!
  </v-btn>
</template>
