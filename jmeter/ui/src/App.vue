<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <v-responsive class="border rounded">
    <v-app theme="dark">
      <v-app-bar title="JMeter as a Service"></v-app-bar>

      <v-navigation-drawer>
        <v-list>
          <v-list-item title="Run Test"></v-list-item>
        </v-list>
      </v-navigation-drawer>

      <v-main>
        <v-container>
          <RouterView />
        </v-container>
      </v-main>
    </v-app>
  </v-responsive>
</template>