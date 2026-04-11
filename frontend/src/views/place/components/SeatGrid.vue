<template>
  <div class="seat-grid">
    <div
      class="grid gap-1.5"
      :style="{ gridTemplateColumns: `repeat(${cols}, 1fr)` }"
    >
      <div
        v-for="pos in positions"
        :key="pos.no"
        class="seat-cell"
        :class="pos.occupied ? 'seat-occupied' : 'seat-empty'"
        @click="handleClick(pos)"
      >
        <span class="seat-no">{{ pos.no }}</span>
        <span v-if="pos.occupied" class="seat-name" :title="pos.occupantName">{{ pos.occupantName }}</span>
        <span v-else class="seat-empty-label">空</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { PlaceOccupant } from '@/types/universalPlace'

interface Props {
  capacity: number
  occupants: PlaceOccupant[]
  capacityUnit?: string
  editable?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  capacityUnit: '人',
  editable: false
})

const emit = defineEmits<{
  'check-in': [positionNo: string]
  select: [occupant: PlaceOccupant]
}>()

const cols = computed(() => {
  const c = props.capacity
  if (c <= 4) return c
  if (c <= 6) return 3
  if (c <= 8) return 4
  if (c <= 12) return 4
  if (c <= 16) return 4
  if (c <= 20) return 5
  return Math.min(6, Math.ceil(Math.sqrt(c)))
})

interface SeatPosition {
  no: string
  occupied: boolean
  occupantName?: string
  occupant?: PlaceOccupant
}

const positions = computed<SeatPosition[]>(() => {
  const result: SeatPosition[] = []
  const occupantMap = new Map<string, PlaceOccupant>()
  for (const occ of props.occupants) {
    if (occ.positionNo) {
      occupantMap.set(String(occ.positionNo), occ)
    }
  }

  for (let i = 1; i <= props.capacity; i++) {
    const no = String(i).padStart(2, '0')
    const occ = occupantMap.get(no)
    result.push({
      no,
      occupied: !!occ,
      occupantName: occ?.occupantName,
      occupant: occ
    })
  }
  return result
})

function handleClick(pos: SeatPosition) {
  if (pos.occupied && pos.occupant) {
    emit('select', pos.occupant)
  } else if (!pos.occupied) {
    emit('check-in', pos.no)
  }
}
</script>

<style scoped>
.seat-grid {
  padding: 8px;
}
.seat-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  min-height: 52px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
  padding: 4px 2px;
}
.seat-occupied {
  background: #eff6ff;
  border: 1.5px solid #bfdbfe;
  color: #1e40af;
}
.seat-occupied:hover {
  border-color: #60a5fa;
  background: #dbeafe;
}
.seat-empty {
  background: #fff;
  border: 1.5px dashed #d1d5db;
  color: #9ca3af;
}
.seat-empty:hover {
  border-color: #93c5fd;
  background: #f0f9ff;
  color: #3b82f6;
}
.seat-no {
  font-size: 10px;
  font-weight: 600;
  opacity: 0.6;
}
.seat-name {
  font-size: 11px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
  text-align: center;
}
.seat-empty-label {
  font-size: 11px;
}
</style>
