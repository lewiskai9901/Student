<template>
  <div class="pagination-test" style="padding: 40px;">
    <h1>分页组件测试</h1>

    <el-card style="margin-top: 20px;">
      <template #header>
        <h3>测试1: 最简单的分页组件 (total=100)</h3>
      </template>
      <el-pagination
        v-model:current-page="currentPage1"
        :total="100"
      />
    </el-card>

    <el-card style="margin-top: 20px;">
      <template #header>
        <h3>测试2: 带背景色的分页组件 (total=100)</h3>
      </template>
      <el-pagination
        v-model:current-page="currentPage2"
        :total="100"
        :background="true"
      />
    </el-card>

    <el-card style="margin-top: 20px;">
      <template #header>
        <h3>测试3: 完整配置的分页组件 (total=100)</h3>
      </template>
      <el-pagination
        v-model:current-page="currentPage3"
        v-model:page-size="pageSize3"
        :total="100"
        :page-sizes="[10, 20, 50, 100]"
        :background="true"
        layout="total, sizes, prev, pager, next, jumper"
      />
    </el-card>

    <el-card style="margin-top: 20px;">
      <template #header>
        <h3>测试4: total=0 不强制显示</h3>
      </template>
      <el-pagination
        v-model:current-page="currentPage4"
        :total="0"
        :background="true"
      />
      <p style="color: red; margin-top: 10px;">如果看不到分页组件,说明 total=0 时默认隐藏</p>
    </el-card>

    <el-card style="margin-top: 20px;">
      <template #header>
        <h3>测试5: total=0 强制显示 (hide-on-single-page=false)</h3>
      </template>
      <el-pagination
        v-model:current-page="currentPage5"
        :total="0"
        :background="true"
        :hide-on-single-page="false"
      />
      <p style="color: green; margin-top: 10px;">即使 total=0 也应该显示分页组件</p>
    </el-card>

    <el-card style="margin-top: 20px;">
      <template #header>
        <h3>测试6: 用户管理页面的实际配置 (total={{ testTotal }})</h3>
      </template>
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage6"
          v-model:page-size="pageSize6"
          :total="testTotal"
          :page-sizes="[10, 20, 30, 50, 100]"
          :background="true"
          :hide-on-single-page="false"
          layout="total, sizes, prev, pager, next, jumper"
        />
      </div>
      <el-button @click="testTotal = 0" style="margin-top: 10px;">设置 total = 0</el-button>
      <el-button @click="testTotal = 100" style="margin-top: 10px;">设置 total = 100</el-button>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const currentPage1 = ref(1)
const currentPage2 = ref(1)
const currentPage3 = ref(1)
const pageSize3 = ref(10)
const currentPage4 = ref(1)
const currentPage5 = ref(1)
const currentPage6 = ref(1)
const pageSize6 = ref(10)
const testTotal = ref(0)
</script>

<style scoped lang="scss">
.pagination-test {
  background: #f5f7fa;
  min-height: 100vh;

  h1 {
    margin-bottom: 20px;
    color: #303133;
  }

  .el-card {
    margin-bottom: 20px;
  }

  .pagination-container {
    display: flex !important;
    justify-content: flex-end;
    align-items: center;
    padding: 20px;
    margin-top: 20px;
    background: #fff;
    min-height: 60px;

    :deep(.el-pagination) {
      display: flex !important;
    }
  }
}
</style>
