<script lang="ts">
import { headerName } from '../api/jmaas.ts';
import { useTestStore } from '../stores/test.ts'
import { getQPS, getConcurrency } from '../api/jmaas.ts';
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
  },

  methods: {
    param(id: ParamEnumType) {
      const TEST = useTestStore();
      return TEST.getValue(id);
    },

    header(id: ParamEnumType): string {
      return `${headerName(id)}: ${this.param(id)}`;
    },

    trafficShape(): string {
      const TEST = useTestStore();
      let trafficShape: string;
      let testDuration: number;
      const mode = TEST.getMode();
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
}
</script>

<template>
  <v-card variant="plain" flat>
    <v-card-item>
      <v-card-title>
        Run this test using <code class="text-green">cURL</code>
      </v-card-title>
    </v-card-item>
    <v-card-text>
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
  --header '{{ trafficShape() }}' \
  '{{ origin }}/jmeter/test/run' \
  -d '{{ param("payload") }}'
      </pre>
    </v-card-text>
  </v-card>
</template>
