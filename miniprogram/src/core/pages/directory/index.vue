<script setup lang="ts">
import type { LongId } from '@core/types'
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAuth } from '../../stores/auth'
import { userApi, type UserListItem } from '../../api/user'
import { BizError } from '../../api/request'

declare const uni: any

const auth = useAuth()
const users = ref<UserListItem[]>([])
const loading = ref(true)
const errMsg = ref('')
const keyword = ref('')
const expandedId = ref<LongId | null>(null)

onShow(async () => {
  if (!auth.loggedIn || !auth.orgUnitId) {
    errMsg.value = '请先登录或资料未加载完成'
    loading.value = false
    return
  }
  loading.value = true
  errMsg.value = ''
  try {
    users.value = await userApi.byOrgUnit(auth.orgUnitId)
  } catch (e) {
    errMsg.value = e instanceof BizError ? e.bizMessage : '加载失败'
  } finally {
    loading.value = false
  }
})

const filtered = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  if (!q) return users.value
  return users.value.filter(u =>
    (u.realName?.toLowerCase().includes(q)) ||
    (u.username?.toLowerCase().includes(q)) ||
    (u.phone?.includes(q)) ||
    (u.email?.toLowerCase().includes(q))
  )
})

function toggleExpand(u: UserListItem) {
  expandedId.value = expandedId.value === u.id ? null : u.id
}

function initialOf(u: UserListItem): string {
  return u.realName?.charAt(0) || u.username?.charAt(0) || 'U'
}
</script>

<template>
  <view class="page">
    <view class="header">
      <text class="title">通讯录</text>
      <view class="search">
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索姓名 / 手机 / 邮箱"
          confirm-type="search"
        />
      </view>
    </view>

    <view v-if="loading" class="state">加载中…</view>
    <view v-else-if="errMsg" class="state err">{{ errMsg }}</view>
    <view v-else-if="filtered.length === 0" class="state empty">
      {{ keyword ? '未找到匹配' : '暂无同事' }}
    </view>

    <view v-else class="list">
      <view
        v-for="u in filtered"
        :key="u.id"
        class="row"
        @click="toggleExpand(u)"
      >
        <view class="row-main">
          <image v-if="u.avatar" :src="u.avatar" class="avatar-img" mode="aspectFill" />
          <view v-else class="avatar">{{ initialOf(u) }}</view>
          <view class="info">
            <view class="name-row">
              <text class="name">{{ u.realName || u.username }}</text>
              <text v-if="u.userType" class="type-badge">{{ u.userType }}</text>
            </view>
            <view class="meta">
              <text>{{ u.username }}</text>
              <text v-if="u.phone"> · {{ u.phone }}</text>
            </view>
          </view>
        </view>

        <view v-if="expandedId === u.id" class="detail">
          <view class="kv"><text class="k">邮箱</text><text class="v">{{ u.email || '-' }}</text></view>
          <view class="kv"><text class="k">所属</text><text class="v">{{ u.orgUnitName || '-' }}</text></view>
          <view class="kv"><text class="k">状态</text><text class="v">{{ u.status || '-' }}</text></view>
          <view class="kv"><text class="k">角色</text><text class="v">{{ u.roleNames?.join(' / ') || '-' }}</text></view>
        </view>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.page { padding: 24rpx; min-height: 100vh; }
.header { padding: 16rpx 0 24rpx; }
.title { font-size: 32rpx; font-weight: 700; color: #1a2840; }
.search { margin-top: 16rpx; }
.search-input {
  width: 100%; box-sizing: border-box;
  padding: 16rpx 20rpx; background: #fff; border-radius: 8px;
  font-size: 26rpx; color: #1a2840;
  box-shadow: 0 2px 6px rgba(58,123,213,0.06);
}
.state { padding: 80rpx 0; text-align: center; color: #a0aab4; font-size: 26rpx; }
.state.err { color: #e0592a; }
.list { background: #fff; border-radius: 14px; box-shadow: 0 2px 6px rgba(58,123,213,0.06); overflow: hidden; }
.row { padding: 20rpx 24rpx; border-bottom: 1rpx solid #f0f3f6; }
.row:last-child { border-bottom: 0; }
.row-main { display: flex; align-items: center; }
.avatar, .avatar-img {
  width: 80rpx; height: 80rpx; border-radius: 50%;
  margin-right: 20rpx; flex: none; display: block;
}
.avatar {
  line-height: 80rpx; text-align: center; background: #3a7bd5;
  color: #fff; font-size: 32rpx; font-weight: 700;
}
.info { flex: 1; min-width: 0; }
.name-row { display: flex; align-items: center; gap: 12rpx; }
.name { font-size: 28rpx; font-weight: 600; color: #1a2840; }
.type-badge {
  font-size: 20rpx; padding: 2rpx 12rpx;
  background: #eef5ff; color: #3a7bd5; border-radius: 999rpx;
}
.meta { margin-top: 4rpx; font-size: 22rpx; color: #5a6a7a; }
.detail {
  margin-top: 16rpx; padding: 16rpx; background: #f7f9fc;
  border-radius: 8px; font-size: 24rpx;
}
.kv { display: flex; padding: 6rpx 0; }
.k { width: 120rpx; color: #5a6a7a; }
.v { flex: 1; color: #1a2840; word-break: break-all; }
</style>
