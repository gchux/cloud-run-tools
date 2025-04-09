<script lang="ts">
import { first, keyBy } from 'lodash';
import jmaas from '../api/jmaas.ts';
import type { TestStreamEvent } from '../api/jmaas.ts';
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
      this.$router.push('/stream');
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
  <v-card>
    <v-card-item>
      <v-card-title>Configure test execution</v-card-title>
    </v-card-item>
    <v-card-text>
      <v-divider color="info"></v-divider>
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
      <v-divider color="info"></v-divider>
      <TestParams
        v-if="catalog"
        :catalog="params"
        :params="test?.params"
      ></TestParams>
      <v-divider color="info"></v-divider>
      <v-btn block
        class="text-none mt-2"
        color="success"
        size="x-large"
        variant="flat"
        @click="runTest"
      >
        Run the Load Test!
      </v-btn>
    </v-card-text>  
  </v-card> 
</template>
