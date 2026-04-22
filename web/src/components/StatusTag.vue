<template>
  <span class="status" :class="props.status">{{ textMap[props.status] || props.status }}</span>
</template>

<script setup>
const props = defineProps({
  status: {
    type: String,
    default: "waiting"
  }
});

const textMap = {
  waiting: "待接单",
  applying: "待审核",
  working: "进行中",
  pending_payment: "待支付",
  done: "已完成",
  cancelled: "已取消"
};
</script>
