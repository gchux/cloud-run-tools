<script lang="ts">
import { string, z } from 'zod';
import { pick, first } from 'lodash';
import jmaas from '../api/jmaas.ts';
import { useTestStore } from '../stores/test.ts'

const MODES = ["qps", "concurrency"] as const;

const PARAMS = [
  "qps",
  "proto",
  "method",
  "host",
  "port",
  "path",
  "query",
  "headers",
  "payload",
] as const;

const CatalogTestParams = z.array(z.enum(PARAMS)); 

const CatalogTestSchema = z.object({
  id: z.string(),
  name: z.string(),
  desc: z.string(),
  mode: z.enum(MODES),
  params: CatalogTestParams,
});

const CatalogSchema = z.object({
  tests: z.array(CatalogTestSchema),
});

type Catalog = z.infer<typeof CatalogSchema>;
type CatalogTest = z.infer<typeof CatalogTestSchema>;

type TestData = {
  test: CatalogTest,
  tests: CatalogTest[],
};

const defaultTest: CatalogTest = {
  id: "",
  name: "",
  desc: "",
  mode: MODES[0],
  params: [],
};

export default {
  data: () => {
    return {
      test: defaultTest,
      tests: [],
    } as Partial<TestData>;
  },

  methods: {
    async fetchCatalog() {
      const response = await jmaas.getCatalog();
      const catalog = CatalogSchema.parse(response.data);
      this.tests = catalog.tests;
      this.test = first(this.tests);
    },

    updateTest(test: CatalogTest) {
      const TEST = useTestStore();
      TEST.updateScript(test.id);
      this.test = test;
    }
  },

  mounted() {
    this.fetchCatalog();
  },
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
</template>
