<script lang="ts">
import { toString, isEmpty, isArray, first, replace, split } from 'lodash';
import { default as jmaas, JMAAS_HEADERS } from '../api/jmaas.ts';
import { useTestStore } from '../stores/test.ts';
import { useMessagesStore } from '../stores/messages.ts'
import type { TestStreamEvent } from '../api/jmaas.ts';
import type { Test } from '../stores/test.ts';

type Data = {
  id: string,
  traceID: string,
  instanceID: string,
  ready: boolean,
  loading: boolean,
  data: string,
};

const headerValue = (
  data: TestStreamEvent,
  name: string,
): string => {
return data.headers[name];
};

const getTestID = (
  data: TestStreamEvent,
): string => {
  return headerValue(data, JMAAS_HEADERS.TEST_ID);
};

const getTraceID = (
  data: TestStreamEvent,
): string => {
  return headerValue(data, JMAAS_HEADERS.TRACE_ID);
};

const getInstanceID = (
  data: TestStreamEvent,
): string => {
  return headerValue(data, JMAAS_HEADERS.INSTANCE_ID);
};

const getData = (
  data: TestStreamEvent,
) => {
  // reduce noise from non-relevant output entries
  return replace(
    toString(data.response),
    /^.*?\sINFO\s.*?\.(?:JMeterThread|VariableThroughputTimer|ClassFinder):\s.*[\r\n]+/gm,
    ""
  );
};

export default {
  data: () => {
    return {
      id: "",
      traceID: "",
      instanceID: "",
      data: "",
      ready: false,
      loading: false,
    } as Partial<Data>;
  },

  computed: {
    testID() {
      const strOrArr = this.$route.params.id || "";
      const id = isArray(strOrArr) ? first(strOrArr) : strOrArr;
      return toString(id);
    },
  },

  methods: {
    onData(
      test: Test,
      data: TestStreamEvent,
    ) {
      this.ready = true;
      this.id = getTestID(data);
      this.traceID = getTraceID(data);
      this.instanceID = getInstanceID(data);
      this.data = getData(data);
    },

    runTest(test: Test) {
      const that = this;
      this.loading = true;
      jmaas.runTest(test, this.onData)
        .then(() => {
          that.loading = false;
        });
    },

    streamTest() {
      const MESSAGES = useMessagesStore();
      const TEST = useTestStore();
      
      if (isEmpty(this.testID) && TEST.isComplete()) {
        // handle redirect from `run-test`
        try {
          this.runTest(TEST.get());
        } catch(error) {
          MESSAGES.Error(error as Error);
          this.$router.push('/run');
        }
      } else {
        this.id = this.testID;
      }
    },
  },

  watch: {
    id(newTestID) {
      const MESSAGES = useMessagesStore();
      MESSAGES.info(`streaming test ID: ${newTestID}`);
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
    variant="text"
    flat
  >

    <v-card-item>
      <v-card-title>
        Streaming test output
      </v-card-title>

      <v-card-subtitle>
        <v-list-item>
          <b>{{ id }}</b>
          <v-progress-circular
            v-if="loading"
            class="ms-2"
            size="20"
            width="2"
            indeterminate
          ></v-progress-circular>
        </v-list-item>

        <v-list-item>
          <v-chip
            label
            variant="outlined"
          >
            <v-icon icon="mdi-label" start></v-icon>
            {{ traceID }}
          </v-chip>
        </v-list-item>

        <v-list-item>
          <v-chip
            label
            variant="outlined"
          >
            <v-icon icon="mdi-server" start></v-icon>
            {{ instanceID }}
          </v-chip>
        </v-list-item>
      </v-card-subtitle>
    </v-card-item>

    <v-card-text class="px-0 py-0 mx-0 my-0">
      <pre
        v-if="ready"
        class="bg-black px-2 py-2"
      >{{ data }}</pre>
      <v-progress-linear
        v-if="loading"
        indeterminate
      ></v-progress-linear>
    </v-card-text>
  
  </v-card>

  <pre v-else>UNAVAILABLE</pre>
</template>