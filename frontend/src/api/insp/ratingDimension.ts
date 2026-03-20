import { http } from '@/utils/request'
import type { RatingDimension, RatingResult, CreateRatingDimensionRequest, UpdateRatingDimensionRequest } from '@/types/insp/project'

const BASE = '/v7/insp/rating-dimensions'

export function getRatingDimensionsByProject(projectId: number): Promise<RatingDimension[]> {
  return http.get<RatingDimension[]>(BASE, { params: { projectId } })
}

export function createRatingDimension(data: CreateRatingDimensionRequest): Promise<RatingDimension> {
  return http.post<RatingDimension>(BASE, data)
}

export function updateRatingDimension(id: number, data: UpdateRatingDimensionRequest): Promise<RatingDimension> {
  return http.put<RatingDimension>(`${BASE}/${id}`, data)
}

export function deleteRatingDimension(id: number): Promise<void> {
  return http.delete(`${BASE}/${id}`)
}

export function computeRatings(dimensionId: number, cycleDate: string): Promise<RatingResult[]> {
  return http.post<RatingResult[]>(`${BASE}/${dimensionId}/compute`, { cycleDate })
}

export function getRatingResults(dimensionId: number, cycleDate?: string): Promise<RatingResult[]> {
  return http.get<RatingResult[]>(`${BASE}/${dimensionId}/rankings`, { params: { cycleDate } })
}
