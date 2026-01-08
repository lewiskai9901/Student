<script setup lang="ts">
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'

onLaunch(() => {
  console.log('App Launch')

  // 初始化用户状态 (token会从storage自动恢复)
  const userStore = useUserStore()

  // 检查登录状态
  if (!userStore.isLoggedIn) {
    // 未登录，跳转登录页
    uni.reLaunch({ url: '/pages/login/index' })
  }
})

onShow(() => {
  console.log('App Show')
})

onHide(() => {
  console.log('App Hide')
})
</script>

<style lang="scss">
@import 'uview-plus/index.scss';

/* 全局样式 - 现代设计系统 */
page {
  background-color: #F8FAFC;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'Helvetica Neue', Arial, sans-serif;
  font-size: 28rpx;
  color: #1F2937;
  box-sizing: border-box;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* 重置按钮样式 */
button {
  margin: 0;
  padding: 0;
  background: none;
  line-height: inherit;

  &::after {
    border: none;
  }
}

/* 滚动条美化 */
::-webkit-scrollbar {
  width: 0;
  height: 0;
  background: transparent;
}

/* 通用容器 */
.container {
  padding: 24rpx;
}

/* Flex 布局工具类 */
.flex {
  display: flex;
}

.flex-center {
  display: flex;
  align-items: center;
  justify-content: center;
}

.flex-between {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.flex-start {
  display: flex;
  align-items: flex-start;
}

.flex-end {
  display: flex;
  align-items: flex-end;
}

.flex-column {
  display: flex;
  flex-direction: column;
}

.flex-wrap {
  flex-wrap: wrap;
}

.flex-1 {
  flex: 1;
}

/* 文字颜色 */
.text-primary {
  color: #6366F1 !important;
}

.text-success {
  color: #10B981 !important;
}

.text-warning {
  color: #F59E0B !important;
}

.text-danger {
  color: #EF4444 !important;
}

.text-gray {
  color: #9CA3AF !important;
}

.text-dark {
  color: #1F2937 !important;
}

.text-white {
  color: #fff !important;
}

/* 背景颜色 */
.bg-white {
  background-color: #fff !important;
}

.bg-gray {
  background-color: #F8FAFC !important;
}

.bg-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
}

/* 边距 */
.mt-8 { margin-top: 8rpx; }
.mt-12 { margin-top: 12rpx; }
.mt-16 { margin-top: 16rpx; }
.mt-20 { margin-top: 20rpx; }
.mt-24 { margin-top: 24rpx; }
.mt-32 { margin-top: 32rpx; }

.mb-8 { margin-bottom: 8rpx; }
.mb-12 { margin-bottom: 12rpx; }
.mb-16 { margin-bottom: 16rpx; }
.mb-20 { margin-bottom: 20rpx; }
.mb-24 { margin-bottom: 24rpx; }
.mb-32 { margin-bottom: 32rpx; }

.ml-8 { margin-left: 8rpx; }
.ml-12 { margin-left: 12rpx; }
.ml-16 { margin-left: 16rpx; }
.ml-20 { margin-left: 20rpx; }

.mr-8 { margin-right: 8rpx; }
.mr-12 { margin-right: 12rpx; }
.mr-16 { margin-right: 16rpx; }
.mr-20 { margin-right: 20rpx; }

.p-16 { padding: 16rpx; }
.p-20 { padding: 20rpx; }
.p-24 { padding: 24rpx; }
.p-32 { padding: 32rpx; }

/* 圆角 */
.rounded-sm { border-radius: 8rpx; }
.rounded { border-radius: 16rpx; }
.rounded-md { border-radius: 20rpx; }
.rounded-lg { border-radius: 24rpx; }
.rounded-xl { border-radius: 32rpx; }
.rounded-full { border-radius: 9999rpx; }

/* 阴影 */
.shadow-sm {
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
}

.shadow {
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}

.shadow-md {
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
}

.shadow-lg {
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

/* 现代卡片样式 */
.card {
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.card-elevated {
  background: #fff;
  border-radius: 24rpx;
  padding: 32rpx;
  box-shadow: 0 10rpx 40rpx rgba(0, 0, 0, 0.08);
}

/* 分割线 */
.divider {
  height: 2rpx;
  background-color: #F3F4F6;
  margin: 24rpx 0;
}

/* 安全区域 */
.safe-area-bottom {
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
}

.safe-area-top {
  padding-top: constant(safe-area-inset-top);
  padding-top: env(safe-area-inset-top);
}

/* 渐变色背景 */
.gradient-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.gradient-success {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
}

.gradient-warning {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.gradient-info {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

/* 字体大小 */
.text-xs { font-size: 20rpx; }
.text-sm { font-size: 24rpx; }
.text-base { font-size: 28rpx; }
.text-md { font-size: 32rpx; }
.text-lg { font-size: 36rpx; }
.text-xl { font-size: 44rpx; }
.text-2xl { font-size: 56rpx; }

/* 字体粗细 */
.font-normal { font-weight: 400; }
.font-medium { font-weight: 500; }
.font-semibold { font-weight: 600; }
.font-bold { font-weight: 700; }

/* 文字溢出省略 */
.truncate {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.line-clamp-3 {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  overflow: hidden;
}

/* 隐藏元素 */
.hidden {
  display: none !important;
}

/* 点击态 */
.clickable {
  transition: all 0.2s ease;

  &:active {
    opacity: 0.7;
    transform: scale(0.98);
  }
}

/* 加载中骨架屏效果 */
.skeleton {
  background: linear-gradient(90deg, #F3F4F6 25%, #E5E7EB 50%, #F3F4F6 75%);
  background-size: 200% 100%;
  animation: skeleton-loading 1.5s infinite;
}

@keyframes skeleton-loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

/* 淡入动画 */
.fade-in {
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80rpx 40rpx;

  .empty-icon {
    margin-bottom: 24rpx;
  }

  .empty-text {
    font-size: 28rpx;
    color: #9CA3AF;
    text-align: center;
  }

  .empty-action {
    margin-top: 32rpx;
  }
}
</style>
