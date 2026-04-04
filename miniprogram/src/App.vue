<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { useAuthStore } from '@/stores/auth'
import { getToken } from '@/utils/request'

const LOGIN_PAGE = '/pages/login/index'
const WHITE_LIST = [LOGIN_PAGE]

onLaunch(() => {
  const authStore = useAuthStore()
  authStore.init()

  // Route guard
  const interceptor = {
    invoke(args: { url: string }) {
      const path = args.url.split('?')[0]
      if (WHITE_LIST.includes(path)) return
      if (!getToken()) {
        uni.reLaunch({ url: LOGIN_PAGE })
        return false
      }
    },
  }

  uni.addInterceptor('navigateTo', interceptor)
  uni.addInterceptor('redirectTo', interceptor)
  uni.addInterceptor('reLaunch', interceptor)
  uni.addInterceptor('switchTab', {
    invoke(args: { url: string }) {
      if (!getToken()) {
        uni.reLaunch({ url: LOGIN_PAGE })
        return false
      }
    },
  })

  // Check login on launch
  if (!getToken()) {
    uni.reLaunch({ url: LOGIN_PAGE })
  }
})
</script>

<style lang="scss">
:root {
  --wot-color-theme: #{$color-primary};
  --wot-color-danger: #{$color-danger};
  --wot-color-warning: #{$color-warning};
  --wot-color-success: #{$color-success};
}

page {
  background-color: $bg-page;
  color: $text-primary;
  font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', 'PingFang SC',
    'Microsoft YaHei', sans-serif;
  font-size: $font-md;
  line-height: 1.5;
}

.text-primary { color: $text-primary; }
.text-secondary { color: $text-secondary; }
.text-placeholder { color: $text-placeholder; }
.text-brand { color: $color-primary; }
.text-danger { color: $color-danger; }
.text-success { color: $color-success; }
.bg-page { background-color: $bg-page; }
.bg-card { background-color: $bg-card; }

.card {
  background: $bg-card;
  border-radius: $radius-card;
  box-shadow: $shadow-card;
}
</style>
