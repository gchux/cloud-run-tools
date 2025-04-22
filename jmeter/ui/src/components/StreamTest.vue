<script lang="ts">
import { z } from 'zod'
import { 
  NIL as defaultUUID,
} from 'uuid';
import { toString, isArray, first, bind, isEmpty, isEqual } from 'lodash';
import { useMessagesStore } from '../stores/messages.ts'
import { default as jmaas } from '../api/jmaas.ts';
import {
  onTestData,
} from '../utils/test.ts';
import {
  TestDataSchema
} from '../types/test.ts';
import {
  default as TestStream,
} from './TestStream.vue'

const ComponentSchema = TestDataSchema.extend({
  textboxID: z.string().uuid(),
  defaultID: z.string().uuid(),
});

type ComponentData = z.infer<typeof ComponentSchema>;

export default {
  data: () => {
    return {
      id: "",
      traceID: "",
      instanceID: "",
      output: "",
      textboxID: "",
      defaultID: defaultUUID,
      isComplete: false,
    } as ComponentData;
  },

  computed: {
    
  },

  methods: {
    hasTestID(id?: string): boolean {
      id = id || this.id;
      return !isEmpty(id) && !isEqual(id, defaultUUID);
    },

    testID(): string {
      const strOrArr = this.$route.params.id || defaultUUID;
      const id = isArray(strOrArr) ? first(strOrArr) : strOrArr;
      return toString(id);
    },

    streamTest(id: string) {
      const that = this;
      const MESSAGES = useMessagesStore();

      try {
        id = TestDataSchema.shape.id.parse(id);

        jmaas
          .streamTest(
            this.id = id,
            bind(onTestData, that, that),
          )
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
    const id = this.testID();
    if ( this.hasTestID(id) ) {
      this.streamTest(id);
    }
  },
}
</script>

<template>
  <TestStream
    v-if="hasTestID()"
    :test-id="id"
    :instance-id="instanceID"
    :trace-id="traceID"
    :data="output"
    :is-streaming="!isComplete"
  />

  <v-card flat
    v-else
    variant="text"
  >
    <v-card-item>
      <v-card-title>
        Stream a Load Test Output
      </v-card-title>

      <v-card-subtitle>
        enter a valid <b>Test ID</b> to start streaming its output
      </v-card-subtitle>
    </v-card-item>

    <v-card-text>
      <v-text-field
        clearable
        class="mt-4"
        v-model="textboxID"
        variant="outlined"
        label="Test ID"
        prepend-inner-icon="mdi-identifier"
        :placeholder="defaultID"
      />
    </v-card-text>

    <v-card-actions>
      <v-btn
        color="orange"
        text="Stream"
        @click="streamTest(textboxID)"
      />
    </v-card-actions>
  
  </v-card>
</template>