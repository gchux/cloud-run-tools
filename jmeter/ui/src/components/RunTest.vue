<script setup lang="ts">
import jmaas from '../api/jmaas.ts';
</script>

<template>
  <v-select
    clearable
    multiple
    label="Parameters"
    :items="['ID', 'Host']"
    >
    </v-select>
</template>
