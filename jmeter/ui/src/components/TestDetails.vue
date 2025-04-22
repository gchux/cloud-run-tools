<script lang="ts">
import { 
  NIL as defaultUUID,
} from 'uuid';
import { bind, first, isArray, isEmpty, isEqual, isUndefined, toLower, toString, chain } from 'lodash';
import { default as jmaas, type TestDetails } from '../api/jmaas.ts';
import type { PropType } from 'vue';

type ComponentData = {
  id: string
  details?: TestDetails,
};

export default {
  data: () => {
    return {
      id: defaultUUID,
      details: undefined,
    } as ComponentData;
  },

  props: {
    testId: {
      type: Object as PropType<string>,
    },
  },

  computed: {
    hasTestID(): boolean {
      const id = this.id;
      return !isUndefined(id) && !isEmpty(id) && !isEqual(id, defaultUUID);
    },

    hasTestDetails(): boolean {
      return !isUndefined(this.details) && !isEmpty(this.details);
    },

    protocol(): string {
      if (!isUndefined(this.details)) {
        return toLower(this.details.proto) || "";
      }
      return "";
    },

    queryString(): string {
      if (isUndefined(this.details)) {
        return "";
      }

      const params: string = chain(
        this.details.params
      )
        .map((
          value: any,
          key: string,
        ): string => {
          return `${key}=${value}`;
        })
        .compact()
        .join('&')
        .value();

      if ( isEmpty(params) ) {
        return "";
      }
      return `?${params}`;
    },

    headers(): string {
      if (isUndefined(this.details)) {
        return "";
      }

      return chain(
        this.details.headers
      )
        .map((
          value: any,
          key: string,
        ): string => {
          return `${key}: ${value}`;
        })
        .compact()
        .join('\n')
        .value();
    },
  },

  methods: {
    setTestID(): string {
      const strOrArr = this.$route.params.id || this.testId || defaultUUID;
      const id = (isArray(strOrArr) ? first(strOrArr) : strOrArr) || defaultUUID;
      return (this.id = toString(id));
    },

    onTestDetails(details: TestDetails) {
      console.log(details)
      this.details = details;
    },

    getTestDetails() {
      const callback = bind(this.onTestDetails, this);
      jmaas.getTestByID(this.id, callback);
    },
  },

  created() {
    this.setTestID();
    this.getTestDetails();
  },
}
</script>

<template>
  <v-sheet
    v-if="details && hasTestDetails"
    class="bg-transparent"
  >
    <v-chip label
      color="error"
      variant="outlined"
    >
      <v-icon start icon="mdi-web" color="red" />
      <code>
        {{ protocol }}://{{ details.host }}:{{ details.port }}
      </code>
    </v-chip>
    
<pre class="pa-4 bg-black text-green">
{{ details.method }} {{ details.path }}{{ queryString }}
Host: {{ details.host }}
x-cloud-trace-context: {{ details.trace_id }}
{{ headers }}

{{ details.payload || "" }}
</pre>
  </v-sheet>
</template>