<script lang="ts">
import { 
  NIL as defaultUUID,
  v4 as uuidv4,
} from 'uuid';
import { bind } from 'lodash';
import { default as jmaas } from '../api/jmaas.ts';
import { useTestStore } from '../stores/test.ts';
import { useMessagesStore } from '../stores/messages.ts'
import {
  getTestID,
  onTestData,
} from '../utils/test.ts';
import type { Test } from '../stores/test.ts';
import type { TestStreamEvent } from '../api/jmaas.ts';
import type {
  TestData
} from '../types/test.ts';
import {
  default as TestStream,
} from './TestStream.vue'
import CurlView from './CurlView.vue'

export default {
  data: () => {
    return {
      id: defaultUUID,
      traceID: "",
      instanceID: "",
      output: "",
      isComplete: false,
    } as TestData;
  },

  computed: {
    isAsync(): boolean {
      const TEST = useTestStore();
      return TEST.isAsync();
    }
  },

  methods: {
    onData(
      test: Test,
      data: TestStreamEvent,
    ) {
      this.id = getTestID(data);
      onTestData(this, data);
    },

    runTest(test: Test) {
      const that = this;
      
      const handler = bind(this.onData, this, test);

      jmaas
        .runTest(test, handler)
        .finally(() => {
          that.isComplete = true;
        });
    },

    startTest(id: string) {
      const MESSAGES = useMessagesStore();
      const TEST = useTestStore();

      try {
        TEST.setID(id);
        this.runTest(TEST.get());
      } catch (error) {
        MESSAGES.Error(error as Error);
        this.$router.push('/');
      }
    },

    streamTest(id: string) {
      this.$router.push(`/stream/${id}`);
    },
  },

  created() {
    const id = (
      this.id = uuidv4()
    );
    this.startTest(id);
  },

  components: {
    CurlView,
    TestStream,
  },
}
</script>

<template>
  <!-- [WIP]: add details of the test being executed -->
  <v-progress-linear
    v-if="!isComplete"
    indeterminate
  ></v-progress-linear>

  <v-card flat
     v-if="isAsync"
     variant="text"
  >
    <v-card-title>
      Running Async Load Test
    </v-card-title>

    <v-card-subtitle v-if="id">
      <v-list-item>
        <template v-slot:prepend>
          <v-icon
            icon="mdi-identifier" 
            size="x-large"
            color="warning"
          ></v-icon>
        </template>
        <v-list-item-title>
          <code>{{ id }}</code>
        </v-list-item-title>
      </v-list-item>
    </v-card-subtitle>

    <v-card-text>
      <v-list-item v-if="traceID">
        <template v-slot:prepend>
          <v-icon
            icon="mdi-label"
            size="x-large"
            color="primary"
          ></v-icon>
        </template>
        <v-list-item-title
          class="text-primary"
        >
        <code>
          <b>Trace ID</b>: {{ traceID }}
        </code>
        </v-list-item-title>
      </v-list-item>

      <v-list-item v-if="instanceID">
        <template v-slot:prepend>
          <v-icon
            icon="mdi-server"
            size="x-large"
            color="teal"
          ></v-icon>
        </template>
        <v-list-item-title
          class="text-teal"
        >
          <code>
            <b>Instance ID</b>: {{ instanceID }}
          </code>
        </v-list-item-title>
      </v-list-item>

      <v-divider class="my-2"></v-divider>
  
      <v-list-item>
        <CurlView />
      </v-list-item>
    </v-card-text>

    <v-card-actions>
      <v-btn
         v-if="id"
        color="orange"
        text="Stream"
        @click="streamTest(id)"
      ></v-btn>
      <v-btn color="orange" text="Status"></v-btn>
    </v-card-actions>
  </v-card>

  <TestStream
    v-else
    :id="id"
    :instance-id="instanceID"
    :trace-id="traceID"
    :data="output"
    :is-streaming="!isComplete"
  ></TestStream>
</template>