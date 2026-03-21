import { http } from '@/utils/request'
import type { ScoringPolicy, PolicyGradeBand, PolicyCalcRule } from '@/types/insp/evaluation'

const BASE = '/v7/insp/scoring-policies'

// ScoringPolicy CRUD
export function listPolicies(): Promise<ScoringPolicy[]> { return http.get(BASE) }
export function getPolicy(id: number): Promise<ScoringPolicy> { return http.get(`${BASE}/${id}`) }
export function createPolicy(data: Partial<ScoringPolicy>): Promise<ScoringPolicy> { return http.post(BASE, data) }
export function updatePolicy(id: number, data: Partial<ScoringPolicy>): Promise<ScoringPolicy> { return http.put(`${BASE}/${id}`, data) }
export function deletePolicy(id: number): Promise<void> { return http.delete(`${BASE}/${id}`) }

// GradeBand CRUD
export function getGradeBands(policyId: number): Promise<PolicyGradeBand[]> { return http.get(`${BASE}/${policyId}/grade-bands`) }
export function createGradeBand(policyId: number, data: Partial<PolicyGradeBand>): Promise<PolicyGradeBand> { return http.post(`${BASE}/${policyId}/grade-bands`, data) }
export function updateGradeBand(policyId: number, bandId: number, data: Partial<PolicyGradeBand>): Promise<PolicyGradeBand> { return http.put(`${BASE}/${policyId}/grade-bands/${bandId}`, data) }
export function deleteGradeBand(policyId: number, bandId: number): Promise<void> { return http.delete(`${BASE}/${policyId}/grade-bands/${bandId}`) }

// CalcRule CRUD
export function getCalcRules(policyId: number): Promise<PolicyCalcRule[]> { return http.get(`${BASE}/${policyId}/calc-rules`) }
export function createCalcRule(policyId: number, data: Partial<PolicyCalcRule>): Promise<PolicyCalcRule> { return http.post(`${BASE}/${policyId}/calc-rules`, data) }
export function updateCalcRule(policyId: number, ruleId: number, data: Partial<PolicyCalcRule>): Promise<PolicyCalcRule> { return http.put(`${BASE}/${policyId}/calc-rules/${ruleId}`, data) }
export function deleteCalcRule(policyId: number, ruleId: number): Promise<void> { return http.delete(`${BASE}/${policyId}/calc-rules/${ruleId}`) }
