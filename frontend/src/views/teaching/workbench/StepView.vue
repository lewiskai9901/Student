<template>
  <div>
    <!-- Sub-tabs: 基准/实况/总览 -->
    <div class="tm-tabs">
      <button :class="['tm-tab', { active: tab === 'master' }]" @click="tab = 'master'">基准课表</button>
      <button :class="['tm-tab', { active: tab === 'live' }]" @click="tab = 'live'">实况课表</button>
      <button :class="['tm-tab', { active: tab === 'overview' }]" @click="tab = 'overview'">总览矩阵</button>
    </div>

    <div style="flex: 1; overflow-y: auto;">
      <TimetableViewer v-if="tab === 'master'" :semester-id="semesterId" />
      <LiveTimetable v-else-if="tab === 'live'" :semester-id="semesterId" />
      <ScheduleOverview v-else-if="tab === 'overview'" :semester-id="semesterId" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import TimetableViewer from '../schedule/TimetableViewer.vue'
import LiveTimetable from '../schedule/LiveTimetable.vue'
import ScheduleOverview from '../schedule/ScheduleOverview.vue'

defineProps<{
  semesterId: number | string | undefined
}>()

const tab = ref<'master' | 'live' | 'overview'>('master')
</script>
