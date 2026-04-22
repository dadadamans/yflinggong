<template>
  <router-view />
</template>

<script setup>
import { watch, onMounted } from "vue";
import { useUserStore } from "./stores/user";

const userStore = useUserStore();

function updateBodyClass() {
  document.body.classList.remove("font-medium", "font-large", "font-xlarge");
  
  if (userStore.isLoggedIn && userStore.fontSize && userStore.fontSize !== "small") {
    document.body.classList.add("font-" + userStore.fontSize);
  }
}

watch(() => userStore.fontSize, updateBodyClass);
watch(() => userStore.isLoggedIn, updateBodyClass);

onMounted(updateBodyClass);
</script>
