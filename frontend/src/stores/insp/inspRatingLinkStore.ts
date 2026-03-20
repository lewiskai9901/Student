/**
 * V7 检查平台 - 评级关联 Store
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { InspRatingLink, CreateRatingLinkRequest, UpdateRatingLinkRequest, CalculateRatingRequest } from '@/types/insp/ratingLink'
import { inspRatingLinkApi } from '@/api/insp/ratingLink'

export const useInspRatingLinkStore = defineStore('inspRatingLink', () => {
  const links = ref<InspRatingLink[]>([])
  const loading = ref(false)

  async function fetchByProject(projectId: number) {
    loading.value = true
    try {
      links.value = await inspRatingLinkApi.getRatingLinksByProject(projectId)
    } finally {
      loading.value = false
    }
  }

  async function create(data: CreateRatingLinkRequest) {
    const link = await inspRatingLinkApi.createRatingLink(data)
    links.value.push(link)
    return link
  }

  async function update(id: number, data: UpdateRatingLinkRequest) {
    const updated = await inspRatingLinkApi.updateRatingLink(id, data)
    const idx = links.value.findIndex(l => l.id === id)
    if (idx >= 0) links.value[idx] = updated
    return updated
  }

  async function remove(id: number) {
    await inspRatingLinkApi.deleteRatingLink(id)
    links.value = links.value.filter(l => l.id !== id)
  }

  async function manualCalculate(data: CalculateRatingRequest) {
    await inspRatingLinkApi.manualCalculateRating(data)
  }

  return {
    links, loading,
    fetchByProject, create, update, remove, manualCalculate,
  }
})
