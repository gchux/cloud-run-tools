<script lang="ts">
import { RouterLink, RouterView } from 'vue-router'
import { useMessagesStore } from './stores/messages.ts'
import type { Qeue, ID as QueueID } from './stores/messages.ts'
import type { Store } from 'pinia'

type Data = {
  queue: Store<QueueID, Qeue>,
}

export default {
  data: () => {
    return {
      queue: useMessagesStore(),
    } as Data;
  },
  mounted() {
    this.$router.push('/');
  },
}
</script>

<template>
  <v-responsive class="border rounded">
    <v-app theme="dark">
      <v-app-bar title="JMaaS [ JMeter as a Service ]"></v-app-bar>

      <v-navigation-drawer>
        <v-list>
          <RouterLink to="/">
            <v-list-item>Run a Load Test</v-list-item>
          </RouterLink>
          <RouterLink to="/stream">
            <v-list-item>Stream a Load Test</v-list-item>
          </RouterLink>
          <RouterLink to="/status">
            <v-list-item>Status of a Load Test</v-list-item>
          </RouterLink>
        </v-list>
      </v-navigation-drawer>

      <v-main>
        <v-container class="px-0 py-0">
          <RouterView />
        </v-container>
      </v-main>

      <v-snackbar-queue v-model="queue.messages"></v-snackbar-queue>
    </v-app>
  </v-responsive>
</template>