import { http } from '@/utils/request'
import type { EvaluationRule, EvaluationLevel, EvaluationResult } from '@/types/insp/evaluation'

const BASE = '/v7/insp/evaluation-rules'

export function listRules(projectId: number): Promise<EvaluationRule[]> { return http.get(`${BASE}?projectId=${projectId}`) }
export function getRule(id: number): Promise<EvaluationRule> { return http.get(`${BASE}/${id}`) }
export function createRule(data: Partial<EvaluationRule>): Promise<EvaluationRule> { return http.post(BASE, data) }
export function updateRule(id: number, data: Partial<EvaluationRule>): Promise<EvaluationRule> { return http.put(`${BASE}/${id}`, data) }
export function deleteRule(id: number): Promise<void> { return http.delete(`${BASE}/${id}`) }

// 等级
export function getLevels(ruleId: number): Promise<EvaluationLevel[]> { return http.get(`${BASE}/${ruleId}/levels`) }
export function saveLevels(ruleId: number, levels: EvaluationLevel[]): Promise<EvaluationLevel[]> { return http.put(`${BASE}/${ruleId}/levels`, levels) }

// 评选
export function evaluate(ruleId: number, cycleStart: string, cycleEnd: string): Promise<EvaluationResult[]> { return http.post(`${BASE}/${ruleId}/evaluate`, { cycleStart, cycleEnd }) }
export function getResults(ruleId: number, cycleDate: string): Promise<EvaluationResult[]> { return http.get(`${BASE}/${ruleId}/results?cycleDate=${cycleDate}`) }
