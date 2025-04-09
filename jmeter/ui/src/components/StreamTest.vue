<script lang="ts">
import { toString, isEmpty, isArray, first, replace, split } from 'lodash';
import jmaas from '../api/jmaas.ts';
import type { TestStreamEvent } from '../api/jmaas.ts';
import { useTestStore } from '../stores/test.ts';
import type { Test } from '../stores/test.ts';

type Data = {
  id: string,
  ready: boolean,
  loading: boolean,
  data: string,
};

export default {
  data: () => {
    return {
      id: "",
      data: "",
      ready: false,
      loading: false,
    } as Partial<Data>;
  },

  methods: {
    onData(
      test: Test,
      data: TestStreamEvent,
    ) {
      this.ready = true;
      this.id = data.headers["x-jmaas-test-id"];
      this.data = replace(
        toString(data.response),
        /^.*?\sINFO\s.*?\.(?:JMeterThread|VariableThroughputTimer|ClassFinder):\s.*[\r\n]+/gm,
        ""
      );
    },

    streamTest() {
      const TEST = useTestStore();
      const testID = this.$route.params.id || "";
      if ( isEmpty(testID) &&  TEST.isComplete() ) {
        const that = this;
        this.loading = true;
        jmaas.runTest(TEST.get(), this.onData)
        .then(() => {
          that.loading = false;
        });
      } else if ( isArray(testID) ) {
        this.id = toString(first(testID));
      } else {
        this.id = toString(testID);
      }
    },
  },

  created() {
    this.streamTest();
  },
}
</script>

<template>
  <v-card
    v-if="id"
  >
    <v-card-item>
      <v-card-title>Streaming test output</v-card-title>
      <v-card-subtitle>
        {{ id }}
        <v-progress-circular
          v-if="loading"
          class="ms-2"
          size="20"
          width="2"
          indeterminate
        ></v-progress-circular>
      </v-card-subtitle>
    </v-card-item>
    <v-card-text class="px-0 py-0">
      <pre v-if="ready" class="bg-black px-2 py-2">{{ data }}</pre>
      <v-progress-linear
        v-if="loading"
        indeterminate
      ></v-progress-linear>
    </v-card-text>
  </v-card>
  <pre v-else>UNAVAILABLE</pre>
</template>