<template>
  <div class="app-container" :class="{ 'auth-layout': isAuthPage }">
    <Header :compact="isAuthPage" />
    <main id="main-content" class="app-main" :class="{ 'auth-main': isAuthPage }"><router-view /></main>
    <Footer :compact="isAuthPage" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import Header from './components/Header.vue'
import Footer from './components/Footer.vue'
const route = useRoute()
const isAuthPage = computed(() => ['/login', '/register'].includes(route.path))
</script>

<style>
.app-container { min-height: 100vh; display: flex; flex-direction: column; }
.app-main { flex: 1; min-width: 0; padding-top: 20px; }
.app-main.auth-main { display: flex; flex-direction: column; padding-top: 0; }
.auth-layout { min-height: 100dvh; }
</style>
