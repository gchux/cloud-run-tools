<script setup lang="ts">
import RunTest from '../components/RunTest.vue'
</script>

<template>
  <v-responsive class="border rounded" max-height="300">
    <v-app theme="dark">
      <v-app-bar title="JMaaS"></v-app-bar>

      <v-navigation-drawer>
        <v-list>
          <v-list-item title="Navigation drawer"></v-list-item>
        </v-list>
      </v-navigation-drawer>

      <v-main>
        <v-container>
          <h1>JMeter as a Service</h1>
        </v-container>
      </v-main>
    </v-app>
  </v-responsive>
</template>