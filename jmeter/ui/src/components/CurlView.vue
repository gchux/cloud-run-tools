<script lang="ts">
import { headerName } from '../api/jmaas.ts';
import { useTestStore } from '../stores/test.ts'
import { getHeaders, getQuery, getQPS, getConcurrency } from '../api/jmaas.ts';
import { ModeEnumSchema } from '../types/catalogs.ts'
import type { Test } from '../stores/test.ts'
import type { ParamEnumType } from '../types/catalogs.ts'

type Data = {
  state: Test,
};

export default {
  data: () => {
    const TEST = useTestStore();
    return {
      state: TEST.get(),
    } as Partial<Data>;
  },

  computed: {
    origin(): string {
      return window.location.origin;
    },

    query(): string {
      const TEST = useTestStore();
      return `${headerName("query")}: ${getQuery(TEST.get())}`;
    },

    headers(): string {
      const TEST = useTestStore();
      return `${headerName("headers")}: ${getHeaders(TEST.get())}`;
    },

    trafficShape(): string {
      const TEST = useTestStore();

      const mode = TEST.getMode();

      let trafficShape: string;
      let testDuration: number;
      
      switch (mode) {
        case ModeEnumSchema.Enum.qps:
          [
            trafficShape,
            testDuration
          ] = getQPS(TEST.get());
          break;

        case ModeEnumSchema.Enum.concurrency:
          [
            trafficShape,
            testDuration
          ] = getConcurrency(TEST.get());
          break;
      }
      return `${headerName(mode)}: ${trafficShape}`;
    },
  },

  methods: {
    param(id: ParamEnumType) {
      const TEST = useTestStore();
      return TEST.getValue(id);
    },

    header(id: ParamEnumType): string {
      return `${headerName(id)}: ${this.param(id)}`;
    },
  },
}
</script>

<template>
  <v-container class="bg-transparent">
    <v-row
      align="center"
      justify="center"
      align-content="center"
      dense no-gutters
    >
      <v-col>
        <v-sheet class="bg-transparent">
          <h3 class="mb-3">
            Run this test using <b><code class="text-green">cURL</code></b>
          </h3>
          <pre class="text-green">
curl -iv --request {{ param("method") }} \
  --header '{{ header("async") }}' \
  --header '{{ header("script") }}' \
  --header '{{ header("mode") }}' \
  --header '{{ header("proto") }}' \
  --header '{{ header("method") }}' \
  --header '{{ header("host") }}' \
  --header '{{ header("port") }}' \
  --header '{{ header("path") }}' \
  --header '{{ header("duration") }}' \
  --header '{{ header("min-latency") }}' \
  --header '{{ header("max-latency") }}' \
  --header '{{ query }}' \
  --header '{{ headers }}' \
  --header '{{ trafficShape }}' \
  '{{ origin }}/jmeter/test/run' \
  -d '{{ param("payload") }}'
          </pre>
        </v-sheet>
      </v-col>
    </v-row>
  </v-container>
</template>
