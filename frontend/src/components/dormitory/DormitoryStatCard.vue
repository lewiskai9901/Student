<template>
  <div
    class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md"
  >
    <div class="flex items-center justify-between">
      <div>
        <p class="text-sm font-medium text-gray-500">{{ title }}</p>
        <p class="mt-1 text-2xl font-bold" :class="valueColorClass">{{ displayValue }}</p>
        <p v-if="suffix" class="mt-0.5 text-xs" :class="suffixColorClass">{{ suffix }}</p>
      </div>
      <div
        class="flex h-12 w-12 items-center justify-center rounded-xl"
        :class="iconBgClass"
      >
        <component :is="icon" class="h-6 w-6" />
      </div>
    </div>
    <div
      class="absolute bottom-0 left-0 h-1 w-full opacity-0 transition-opacity group-hover:opacity-100"
      :class="barGradientClass"
    ></div>
  </div>
</template>

<script setup lang="ts">
import { computed, type Component } from 'vue'

interface Props {
  title: string
  value: number | string
  suffix?: string
  icon: Component
  color?: 'teal' | 'emerald' | 'blue' | 'purple' | 'amber' | 'red' | 'gray'
  // For dynamic color based on thresholds (like occupancy rate)
  colorThresholds?: {
    high: number
    medium: number
    highColor: 'emerald' | 'teal' | 'blue'
    mediumColor: 'amber'
    lowColor: 'red'
  }
}

const props = withDefaults(defineProps<Props>(), {
  color: 'teal',
  suffix: ''
})

const displayValue = computed(() => {
  if (typeof props.value === 'number' && props.colorThresholds) {
    return `${props.value}%`
  }
  return props.value
})

const effectiveColor = computed(() => {
  if (props.colorThresholds && typeof props.value === 'number') {
    if (props.value >= props.colorThresholds.high) {
      return props.colorThresholds.highColor
    } else if (props.value >= props.colorThresholds.medium) {
      return props.colorThresholds.mediumColor
    } else {
      return props.colorThresholds.lowColor
    }
  }
  return props.color
})

const colorMap = {
  teal: {
    value: 'text-teal-600',
    suffix: 'text-gray-400',
    iconBg: 'bg-teal-100 text-teal-600',
    bar: 'bg-gradient-to-r from-teal-500 to-cyan-400'
  },
  emerald: {
    value: 'text-emerald-600',
    suffix: 'text-emerald-500',
    iconBg: 'bg-emerald-100 text-emerald-600',
    bar: 'bg-gradient-to-r from-emerald-500 to-green-400'
  },
  blue: {
    value: 'text-blue-600',
    suffix: 'text-gray-400',
    iconBg: 'bg-blue-100 text-blue-600',
    bar: 'bg-gradient-to-r from-blue-500 to-indigo-400'
  },
  purple: {
    value: 'text-purple-600',
    suffix: 'text-gray-400',
    iconBg: 'bg-purple-100 text-purple-600',
    bar: 'bg-gradient-to-r from-purple-500 to-violet-400'
  },
  amber: {
    value: 'text-amber-600',
    suffix: 'text-amber-500',
    iconBg: 'bg-amber-100 text-amber-600',
    bar: 'bg-gradient-to-r from-amber-500 to-yellow-400'
  },
  red: {
    value: 'text-red-500',
    suffix: 'text-red-400',
    iconBg: 'bg-red-100 text-red-500',
    bar: 'bg-gradient-to-r from-red-500 to-orange-400'
  },
  gray: {
    value: 'text-gray-600',
    suffix: 'text-gray-400',
    iconBg: 'bg-gray-100 text-gray-600',
    bar: 'bg-gradient-to-r from-gray-500 to-slate-400'
  }
}

const valueColorClass = computed(() => colorMap[effectiveColor.value].value)
const suffixColorClass = computed(() => colorMap[effectiveColor.value].suffix)
const iconBgClass = computed(() => colorMap[effectiveColor.value].iconBg)
const barGradientClass = computed(() => colorMap[effectiveColor.value].bar)
</script>
