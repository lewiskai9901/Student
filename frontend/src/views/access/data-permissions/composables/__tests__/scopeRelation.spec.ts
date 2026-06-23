import { describe, it, expect } from 'vitest'
import {
  grantToKey,
  keyToGrant,
  keyIsOrg,
  ensureGrants,
  permissionToGrants,
} from '../scopeRelation'
import type { RelationGrant } from '@/types/access'

describe('scopeRelation — grantToKey', () => {
  it('创建者 / 全部', () => {
    expect(grantToKey({ relation: 'creator', subject: 'SELF' })).toBe('creator')
    expect(grantToKey({ relation: 'owner_org', subject: 'ALL' })).toBe('all')
  })
  it('org 关系: RELATION+param / legacy MY_ORG→成员 / owner_org+SELF→创建者', () => {
    expect(grantToKey({ relation: 'owner_org', subject: 'RELATION', subjectParam: 'admin' })).toBe('org:admin')
    expect(grantToKey({ relation: 'owner_org', subject: 'MY_ORG' })).toBe('org:member')
    expect(grantToKey({ relation: 'owner_org', subject: 'SELF' })).toBe('creator')
  })
  it('PLUGIN_DIM→维度 / CUSTOM→custom / 资源关系→res:', () => {
    expect(grantToKey({ relation: 'owner_org', subject: 'PLUGIN_DIM', subjectParam: 'BY_CLASS' })).toBe('dim:BY_CLASS')
    expect(grantToKey({ relation: 'owner_org', subject: 'CUSTOM', orgIds: [1] })).toBe('custom')
    expect(grantToKey({ relation: 'reviewer', subject: 'SELF' })).toBe('res:reviewer')
    expect(grantToKey({ relation: 'inspected', subject: 'SELF' })).toBe('res:inspected')
  })
})

describe('scopeRelation — keyToGrant', () => {
  it('各键 → grant', () => {
    expect(keyToGrant('creator', false)).toEqual({ relation: 'creator', subject: 'SELF' })
    expect(keyToGrant('all', false)).toEqual({ relation: 'owner_org', subject: 'ALL' })
    expect(keyToGrant('org:admin', true)).toEqual({ relation: 'owner_org', subject: 'RELATION', subjectParam: 'admin', subtree: true })
    expect(keyToGrant('dim:BY_CLASS', false)).toEqual({ relation: 'owner_org', subject: 'PLUGIN_DIM', subjectParam: 'BY_CLASS' })
    expect(keyToGrant('res:reviewer', false)).toEqual({ relation: 'reviewer', subject: 'SELF' })
  })
  it('keyIsOrg: 仅 org: 前缀', () => {
    expect(keyIsOrg('org:member')).toBe(true)
    expect(keyIsOrg('creator')).toBe(false)
    expect(keyIsOrg('dim:BY_CLASS')).toBe(false)
  })
})

describe('scopeRelation — 往返 (round-trip)', () => {
  const cases: RelationGrant[] = [
    { relation: 'creator', subject: 'SELF' },
    { relation: 'owner_org', subject: 'ALL' },
    { relation: 'owner_org', subject: 'RELATION', subjectParam: 'admin', subtree: true },
    { relation: 'owner_org', subject: 'RELATION', subjectParam: 'member', subtree: false },
    { relation: 'owner_org', subject: 'PLUGIN_DIM', subjectParam: 'BY_CLASS' },
    { relation: 'reviewer', subject: 'SELF' },
  ]
  it('grant → key → grant 稳定 (PLUGIN_DIM 含金标准 BY_CLASS)', () => {
    for (const g of cases) {
      const key = grantToKey(g)
      const back = keyToGrant(key, !!g.subtree)
      expect(back.relation).toBe(g.relation)
      expect(back.subject).toBe(g.subject)
      if (g.subjectParam) expect(back.subjectParam).toBe(g.subjectParam)
    }
  })
})

describe('scopeRelation — permissionToGrants (载入)', () => {
  it('已带 relationGrants → 直接用 (PLUGIN_DIM BY_CLASS 原样, 护金标准)', () => {
    const grants = permissionToGrants({
      moduleCode: 'student',
      scopeCode: 'BY_CLASS',
      relationGrants: [{ relation: 'owner_org', subject: 'PLUGIN_DIM', subjectParam: 'BY_CLASS' }],
    })
    expect(grants).toEqual([{ relation: 'owner_org', subject: 'PLUGIN_DIM', subjectParam: 'BY_CLASS' }])
  })
  it('legacy 轴① 派生: PRIMARY_ORG→成员 / RELATION→该关系 / SELF→创建者 / PLUGIN_DIM 保全', () => {
    expect(permissionToGrants({ moduleCode: 'x', scopeCode: 'DEPARTMENT_AND_BELOW', orgAnchor: 'PRIMARY_ORG', includeSubtree: true }))
      .toEqual([{ relation: 'owner_org', subject: 'RELATION', subjectParam: 'member', subtree: true }])
    expect(permissionToGrants({ moduleCode: 'x', scopeCode: 'X', orgAnchor: 'RELATION', anchorParam: 'admin' }))
      .toEqual([{ relation: 'owner_org', subject: 'RELATION', subjectParam: 'admin', subtree: false }])
    expect(permissionToGrants({ moduleCode: 'x', scopeCode: 'SELF', orgAnchor: 'SELF' }))
      .toEqual([{ relation: 'creator', subject: 'SELF' }])
    expect(permissionToGrants({ moduleCode: 'x', scopeCode: 'BY_CLASS', orgAnchor: 'PLUGIN_DIM', anchorParam: 'BY_CLASS' }))
      .toEqual([{ relation: 'owner_org', subject: 'PLUGIN_DIM', subjectParam: 'BY_CLASS' }])
  })
  it('未配 (undefined) → 创建者兜底', () => {
    expect(permissionToGrants(undefined)).toEqual([{ relation: 'creator', subject: 'SELF' }])
  })
})

describe('scopeRelation — ensureGrants', () => {
  it('空 → 创建者; 非空 → 原样', () => {
    expect(ensureGrants(undefined)).toEqual([{ relation: 'creator', subject: 'SELF' }])
    expect(ensureGrants([])).toEqual([{ relation: 'creator', subject: 'SELF' }])
    const g = [{ relation: 'owner_org', subject: 'ALL' } as RelationGrant]
    expect(ensureGrants(g)).toBe(g)
  })
})
