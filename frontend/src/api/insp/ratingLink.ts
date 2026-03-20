/**
 * V7 检查平台 - 评级关联 API
 */
import { http } from '@/utils/request'
import type { InspRatingLink, CreateRatingLinkRequest, UpdateRatingLinkRequest, CalculateRatingRequest } from '@/types/insp/ratingLink'

const BASE = '/v7/insp/rating-links'

export function getRatingLinksByProject(projectId: number): Promise<InspRatingLink[]> {
  return http.get<InspRatingLink[]>(BASE, { params: { projectId } })
}

export function getRatingLinkById(id: number): Promise<InspRatingLink> {
  return http.get<InspRatingLink>(`${BASE}/${id}`)
}

export function createRatingLink(data: CreateRatingLinkRequest): Promise<InspRatingLink> {
  return http.post<InspRatingLink>(BASE, data)
}

export function updateRatingLink(id: number, data: UpdateRatingLinkRequest): Promise<InspRatingLink> {
  return http.put<InspRatingLink>(`${BASE}/${id}`, data)
}

export function deleteRatingLink(id: number): Promise<void> {
  return http.delete<void>(`${BASE}/${id}`)
}

export function manualCalculateRating(data: CalculateRatingRequest): Promise<void> {
  return http.post<void>(`${BASE}/calculate`, data)
}

export const inspRatingLinkApi = {
  getRatingLinksByProject,
  getRatingLinkById,
  createRatingLink,
  updateRatingLink,
  deleteRatingLink,
  manualCalculateRating,
}
