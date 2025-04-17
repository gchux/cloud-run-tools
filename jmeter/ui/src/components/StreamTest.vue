<script lang="ts">
import { 
  NIL as defaultUUID,
} from 'uuid';
import { toString, isArray, first, bind, isEmpty, isEqual } from 'lodash';
import { useMessagesStore } from '../stores/messages.ts'
import { default as jmaas } from '../api/jmaas.ts';
import {
  onTestData,
} from '../utils/test.ts';
import type {
  TestData
} from '../types/test.ts';
import {
  default as TestStream,
} from './TestStream.vue'

export default {
  data: () => {
    return {
      id: "",
      traceID: "",
      instanceID: "",
      output: "",
      isComplete: false,
    } as TestData;
  },

  computed: {
    hasTestID(): boolean {
      return !isEmpty(this.id) && !isEqual(this.id, defaultUUID);
    },
  },

  methods: {
    testID(): string {
      const strOrArr = this.$route.params.id || defaultUUID;
      const id = isArray(strOrArr) ? first(strOrArr) : strOrArr;
      return toString(id);
    },

    streamTest(id: string) {
      const that = this;
      const handler = bind(onTestData, this, this);

      const MESSAGES = useMessagesStore();

      try {
        jmaas
        .streamTest(id, handler)
        .finally(() => {
          that.isComplete = true;
        });
      } catch (error) {
        MESSAGES.Error(error as Error);
        this.$router.push('/');
      }
    },
  },

  components: {
    TestStream,
  },

  created() {
    const id = (
      this.id = this.testID()
    );
    if ( !isEmpty(id) ) {
      this.streamTest(id);
    }
  },
}
</script>

<template>
  <TestStream
    v-if="hasTestID"
    :id="id"
    :instance-id="instanceID"
    :trace-id="traceID"
    :data="output"
    :is-streaming="!isComplete"
  ></TestStream>
  <!-- [ToDo]: enable stream by Test ID from user input -->
</template>