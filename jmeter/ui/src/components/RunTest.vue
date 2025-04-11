<script lang="ts">
import { keyBy, startsWith } from 'lodash';
import { default as jmaas } from '../api/jmaas.ts';
import { useTestStore } from '../stores/test.ts'
import { ParamEnumSchema } from '../types/catalogs.ts'
import type {
  Catalog,
  CatalogTest,
  CatalogTestParamsObject,
} from '../types/catalogs.ts'
import { ModeEnumSchema, CatalogSchema } from '../types/catalogs.ts'
import TestParams from './TestParams.vue'
import CurlView from './CurlView.vue'

type Data = {
  catalog: Catalog,
  params: CatalogTestParamsObject,
  tests: CatalogTest[],
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
    const TEST = useTestStore();

    return {
      test: defaultTest,
      tests: [],
      params: [],
    } as Partial<Data>;
  },

  methods: {
    isCloudRun(test: CatalogTest): boolean {
      const testID = test.id;
      return startsWith(testID, "cloud_run_");
    },

    async fetchCatalog() {
      const response = await jmaas.getCatalog();
      this.catalog = CatalogSchema.parse(response.data);
      this.params = keyBy(
        this.catalog.params,
        ParamEnumSchema.Enum.id
      );
      this.tests = this.catalog.tests;
      this.updateTest(
        this.test = this.tests[0]
      );
    },

    updatePort(test: CatalogTest) {
      const TEST = useTestStore();
      if ( this.isCloudRun(test) ) {
        TEST.setPort(443);
      }
    },

    updateTest(test: CatalogTest) {
      const TEST = useTestStore();
      TEST.setScript(test.id);
      TEST.setMode(test.mode);
      this.updatePort(test);
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
    CurlView,
  }
}
</script>

<template>
  <v-card variant="text" flat>
    <v-card-item>
      <v-card-title>
        Configure test execution
      </v-card-title>
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
  <CurlView />
</template>
