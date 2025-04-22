<script lang="ts">
import { isEmpty, isUndefined } from 'lodash';
import { useMessagesStore } from '../stores/messages.ts'
import {
  cleanTestOutput,
} from '../utils/test.ts';
import type { PropType } from 'vue';
import {
  default as TestDetails,
} from './TestDetails.vue'

export default {
  data: () => {
    return {};
  },

  props: {
    testId: {
      type: Object as PropType<string>,
      required: true,
    },

    traceId: {
      type: Object as PropType<string>,
      required: false,
    },

    instanceId: {
      type: Object as PropType<string>,
      required: false,
    },

    data: {
      type: Object as PropType<string>,
      required: false,
    },

    isStreaming: {
      type: Object as PropType<boolean>,
      required: true,
    },
  },

  computed: {
    cleanData(): string {
      if( isUndefined(this.data) || isEmpty(this.data) ) {
        return "";
      }
      return cleanTestOutput(this.data);
    },

    hasData() {
      return !isEmpty(this.data);
    },
  },

  watch: {
    id(newTestID) {
      const MESSAGES = useMessagesStore();
      MESSAGES.info(`streaming test ID: ${newTestID}`);
    },
  },

  components: {
    TestDetails,
  },
}
</script>

<template>
  <v-card flat
    v-if="testId"
    variant="text"
  >

    <v-card-item>
      <v-card-title>
        Streaming Load Test Output
      </v-card-title>

      <v-card-subtitle>
        <v-list-item>
          <v-icon start
            icon="mdi-identifier" 
            size="x-large"
            color="warning"
          />
          <b><code>{{ testId }}</code></b>
          <v-progress-circular
            v-if="isStreaming"
            class="ms-4"
            size="20"
            width="2"
            indeterminate
          />
        </v-list-item>

        <v-list-item v-if="traceId">
          <v-chip label
            color="primary"
            variant="outlined"
          >
            <v-icon start icon="mdi-label" color="info" />
            <code>{{ traceId }}</code>
          </v-chip>
        </v-list-item>

        <v-list-item v-if="instanceId">
          <v-chip label
            color="teal"
            variant="outlined"
          >
            <v-icon start icon="mdi-server" color="teal" />
            <code>{{ instanceId }}</code>
          </v-chip>
        </v-list-item>

        <v-divider class="my-2" />
        
        <v-list-item class="pt-1">
          <TestDetails
            v-if="hasData"
            :test-id="testId"
          />
        </v-list-item>
      </v-card-subtitle>
    </v-card-item>

    <v-card-text
      v-if="cleanData"
      class="px-0 py-0 mx-0 my-0 h-100"
    >
      <pre
        class="bg-black px-2 py-2 h-100"
      >{{ cleanData }}</pre>

      <v-progress-linear
        v-if="isStreaming"
        indeterminate
      />
    </v-card-text>
  
  </v-card>
</template>