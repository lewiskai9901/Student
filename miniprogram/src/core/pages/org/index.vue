<script setup lang="ts">
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { orgApi, type OrgUnitTreeNode } from '../../api/org'
import { BizError } from '../../api/request'

interface FlatNode {
  id: string
  unitName: string
  unitType: string
  status: string
  statusLabel?: string
  headcount?: number
  depth: number
  hasChildren: boolean
  parentChain: string[]
}

const tree = ref<OrgUnitTreeNode[]>([])
const expanded = ref<Set<string>>(new Set())
const loading = ref(true)
const errMsg = ref('')

function flatten(nodes: OrgUnitTreeNode[], depth: number, chain: string[], out: FlatNode[]) {
  for (const n of nodes) {
    out.push({
      id: n.id,
      unitName: n.unitName,
      unitType: n.unitType,
      status: n.status,
      statusLabel: n.statusLabel,
      headcount: n.headcount,
      depth,
      hasChildren: !!(n.children && n.children.length > 0),
      parentChain: chain
    })
    if (n.children && n.children.length > 0) {
      flatten(n.children, depth + 1, [...chain, n.id], out)
    }
  }
}

const flatList = computed<FlatNode[]>(() => {
  const out: FlatNode[] = []
  flatten(tree.value, 0, [], out)
  return out
})

const visibleList = computed<FlatNode[]>(() =>
  flatList.value.filter(n => n.parentChain.every(pid => expanded.value.has(pid)))
)

function toggle(node: FlatNode) {
  if (!node.hasChildren) return
  if (expanded.value.has(node.id)) expanded.value.delete(node.id)
  else expanded.value.add(node.id)
  expanded.value = new Set(expanded.value)
}

async function reload() {
  loading.value = true
  errMsg.value = ''
  try {
    tree.value = await orgApi.tree()
    for (const root of tree.value) {
      if (root.children?.length) expanded.value.add(root.id)
    }
    expanded.value = new Set(expanded.value)
  } catch (e) {
    errMsg.value = e instanceof BizError ? e.bizMessage : '加载失败'
  } finally {
    loading.value = false
  }
}

onShow(() => { reload() })
</script>

<template>
  <view class="page">
    <view class="header">
      <text class="title">组织架构</text>
    </view>

    <view v-if="loading" class="state">加载中…</view>
    <view v-else-if="errMsg" class="state err">{{ errMsg }}</view>
    <view v-else-if="visibleList.length === 0" class="state empty">暂无组织数据</view>

    <view v-else class="list">
      <view
        v-for="n in visibleList"
        :key="n.id"
        class="row"
        :class="{ clickable: n.hasChildren }"
        :style="{ paddingLeft: (16 + n.depth * 24) + 'rpx' }"
        @click="toggle(n)"
      >
        <view v-if="n.hasChildren" class="caret">{{ expanded.has(n.id) ? '▾' : '▸' }}</view>
        <view v-else class="caret-spacer" />
        <view class="content">
          <view class="name-row">
            <text class="name">{{ n.unitName }}</text>
            <text v-if="n.statusLabel" class="status">{{ n.statusLabel }}</text>
          </view>
          <view class="meta">
            <text class="type">{{ n.unitType }}</text>
            <text v-if="n.headcount != null" class="headcount">· 编制 {{ n.headcount }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.page { padding: 24rpx; min-height: 100vh; }
.header { padding: 16rpx 0 24rpx; }
.title { font-size: 32rpx; font-weight: 700; color: #1a2840; }
.state { padding: 80rpx 0; text-align: center; color: #a0aab4; font-size: 26rpx; }
.state.err { color: #e0592a; }
.list { background: #fff; border-radius: 14px; box-shadow: 0 2px 6px rgba(58,123,213,0.06); overflow: hidden; }
.row {
  display: flex; align-items: flex-start;
  padding: 20rpx 24rpx; border-bottom: 1rpx solid #f0f3f6;
  font-size: 26rpx; color: #1a2840;
}
.row:last-child { border-bottom: 0; }
.row.clickable { cursor: pointer; }
.caret, .caret-spacer { width: 32rpx; flex: none; line-height: 1.6; color: #5a6a7a; font-size: 22rpx; }
.content { flex: 1; }
.name-row { display: flex; align-items: center; gap: 12rpx; }
.name { font-weight: 600; }
.status {
  font-size: 20rpx; padding: 2rpx 12rpx; background: #eef5ff; color: #3a7bd5;
  border-radius: 999rpx;
}
.meta { margin-top: 4rpx; font-size: 22rpx; color: #a0aab4; }
.type { color: #5a6a7a; }
.headcount { margin-left: 8rpx; }
</style>
