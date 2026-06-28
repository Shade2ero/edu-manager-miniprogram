<template>
  <div class="app-container">
    <router-view />
  </div>
</template>

<script setup>
import { onLaunch } from '@dcloudio/uni-app'

onLaunch(() => {
  console.log('智能课消教务管理小程序启动')
  // 初始化全局数据、检查登录态等
})
</script>

<style lang="scss">
/* 全局样式变量 */
page {
  --color-primary: #4A90D9;
  --color-success: #52C41A;
  --color-warning: #FAAD14;
  --color-danger: #FF4D4F;
  --color-text-primary: #333333;
  --color-text-secondary: #666666;
  --color-text-hint: #999999;
  --color-bg: #F5F5F5;
  --color-card: #FFFFFF;
  --radius-sm: 8rpx;
  --radius-md: 16rpx;
  --radius-lg: 24rpx;
  --shadow-card: 0 2rpx 16rpx rgba(0, 0, 0, 0.06);

  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Helvetica Neue', sans-serif;
  font-size: 28rpx;
  color: var(--color-text-primary);
  background-color: var(--color-bg);
  box-sizing: border-box;
}
</style>
