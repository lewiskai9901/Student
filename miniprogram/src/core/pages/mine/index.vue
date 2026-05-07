<script setup lang="ts">
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAuth } from '../../stores/auth'
import { authApi } from '../../api/auth'
import { BizError } from '../../api/request'

declare const uni: any

const auth = useAuth()
const refreshing = ref(false)

const profileExtras = ref<{
  phone?: string
  email?: string
  orgUnitId?: number
  userTypeCode?: string
}>({})

onShow(async () => {
  if (!auth.loggedIn) return
  refreshing.value = true
  try {
    const fresh = await authApi.me()
    // Sync to store: rebuild narrow UserInfo + update permissions/tenantId
    auth.user = {
      id: fresh.userId,
      username: fresh.username,
      name: fresh.realName,
      avatar: fresh.avatar,
      roles: fresh.roles ?? []
    }
    auth.permissions = fresh.permissions ?? []
    auth.tenantId = fresh.tenantId ?? null
    auth.orgUnitId = fresh.orgUnitId ?? null
    // Stash extra fields on the store for this page (lightweight; not full schema)
    profileExtras.value = {
      phone: fresh.phone,
      email: fresh.email,
      orgUnitId: fresh.orgUnitId,
      userTypeCode: fresh.userTypeCode
    }
  } catch (e) {
    uni.showToast({
      title: e instanceof BizError ? e.bizMessage : '加载失败',
      icon: 'none'
    })
  } finally {
    refreshing.value = false
  }
})

const initial = computed(() => auth.user?.name?.charAt(0) ?? auth.user?.username?.charAt(0) ?? 'U')

function logout() {
  auth.logout()
  uni.reLaunch({ url: '/core/pages/login/index' })
}
</script>

<template>
  <view class="mine">
    <view v-if="!auth.loggedIn" class="state">
      <view class="state-text">未登录</view>
      <wd-button block @click="$nextTick(() => { uni.reLaunch({ url: '/core/pages/login/index' }) })">去登录</wd-button>
    </view>

    <template v-else>
      <view class="profile">
        <image v-if="auth.user?.avatar" :src="auth.user.avatar" class="avatar-img" mode="aspectFill" />
        <view v-else class="avatar">{{ initial }}</view>
        <view class="name">{{ auth.user?.name || auth.user?.username || '-' }}</view>
        <view class="role">{{ auth.user?.username }}</view>
        <view v-if="profileExtras.userTypeCode" class="type-badge">{{ profileExtras.userTypeCode }}</view>
      </view>

      <view class="card">
        <view class="kv"><text class="k">手机</text><text class="v">{{ profileExtras.phone || '-' }}</text></view>
        <view class="kv"><text class="k">邮箱</text><text class="v">{{ profileExtras.email || '-' }}</text></view>
        <view class="kv"><text class="k">组织</text><text class="v">{{ profileExtras.orgUnitId ?? '-' }}</text></view>
        <view class="kv"><text class="k">租户</text><text class="v">{{ auth.tenantId ?? '-' }}</text></view>
      </view>

      <view v-if="(auth.user?.roles?.length ?? 0) > 0" class="card">
        <view class="card-title">角色</view>
        <view class="chips">
          <text v-for="role in auth.user?.roles" :key="role" class="chip">{{ role }}</text>
        </view>
      </view>

      <view class="actions">
        <wd-button block plain @click="logout">退出登录</wd-button>
      </view>
    </template>
  </view>
</template>

<style lang="scss" scoped>
.mine { padding: 32rpx; }
.state { padding: 80rpx 0; text-align: center; }
.state-text { color: #a0aab4; margin-bottom: 24rpx; font-size: 28rpx; }
.profile {
  background: #fff; border-radius: 14px; padding: 48rpx; text-align: center;
  margin-bottom: 24rpx; box-shadow: 0 2px 6px rgba(58,123,213,0.06);
}
.avatar, .avatar-img {
  width: 120rpx; height: 120rpx; border-radius: 50%;
  margin: 0 auto 16rpx; display: block;
}
.avatar {
  line-height: 120rpx; background: #3a7bd5; color: #fff;
  font-size: 48rpx; font-weight: 700;
}
.name { font-size: 32rpx; font-weight: 700; color: #1a2840; }
.role { font-size: 24rpx; color: #5a6a7a; margin-top: 8rpx; }
.type-badge {
  display: inline-block; margin-top: 12rpx; padding: 4rpx 16rpx;
  background: #f0f5ff; color: #3a7bd5; border-radius: 999rpx; font-size: 22rpx;
}
.card {
  background: #fff; border-radius: 14px; padding: 24rpx;
  margin-bottom: 16rpx; box-shadow: 0 2px 6px rgba(58,123,213,0.06);
}
.card-title { font-size: 26rpx; font-weight: 700; color: #1a2840; margin-bottom: 12rpx; }
.kv {
  display: flex; padding: 16rpx 0; border-bottom: 1rpx solid #f0f3f6;
  font-size: 26rpx;
}
.kv:last-child { border-bottom: 0; }
.k { width: 160rpx; color: #5a6a7a; }
.v { flex: 1; color: #1a2840; word-break: break-all; }
.chips { display: flex; flex-wrap: wrap; gap: 12rpx; }
.chip {
  padding: 4rpx 16rpx; background: #eef2f7; color: #1a2840;
  border-radius: 999rpx; font-size: 22rpx;
}
.actions { margin-top: 32rpx; }
</style>
