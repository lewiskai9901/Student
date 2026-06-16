import { describe, it, expect } from 'vitest'
import type { ModulePermission } from '@/types/access'
import type { ScopeSpecVM } from '../../components/ScopeBuilder.vue'
import {
  inferDefaultAndExceptions,
  expandToCommands,
  scopeCodeFromAxis1,
  axis1Signature,
  anchorLevel,
  presetCodeToSpec,
  clampSpecToAllowed,
  type ResourceException,
} from '../scopePolicy'

// ── helpers to build module permissions tersely ──────────────────────────
function mod(moduleCode: string, spec: Partial<ScopeSpecVM>): ModulePermission {
  return { moduleCode, scopeCode: '', ...spec }
}

const SPEC = {
  all: (): ScopeSpecVM => ({ orgAnchor: 'ALL' }),
  self: (): ScopeSpecVM => ({ orgAnchor: 'SELF' }),
  dept: (): ScopeSpecVM => ({ orgAnchor: 'PRIMARY_ORG' }),
  deptAndBelow: (): ScopeSpecVM => ({ orgAnchor: 'PRIMARY_ORG', includeSubtree: true }),
  managed: (): ScopeSpecVM => ({ orgAnchor: 'RELATION', anchorParam: 'admin' }),
  managedAndBelow: (): ScopeSpecVM => ({
    orgAnchor: 'RELATION',
    anchorParam: 'admin',
    includeSubtree: true,
  }),
  custom: (): ScopeSpecVM => ({ orgAnchor: 'CUSTOM_ORG', customOrgIds: [10, 20] }),
}

describe('scopeCodeFromAxis1', () => {
  it('maps every anchor to its legacy preset', () => {
    expect(scopeCodeFromAxis1({ orgAnchor: 'ALL' })).toBe('ALL')
    expect(scopeCodeFromAxis1({ orgAnchor: 'SELF' })).toBe('SELF')
    expect(scopeCodeFromAxis1({ orgAnchor: 'PRIMARY_ORG' })).toBe('DEPARTMENT')
    expect(scopeCodeFromAxis1({ orgAnchor: 'PRIMARY_ORG', includeSubtree: true })).toBe(
      'DEPARTMENT_AND_BELOW'
    )
    expect(scopeCodeFromAxis1({ orgAnchor: 'RELATION', anchorParam: 'admin' })).toBe('MANAGED_ORGS')
    expect(
      scopeCodeFromAxis1({ orgAnchor: 'RELATION', anchorParam: 'admin', includeSubtree: true })
    ).toBe('MANAGED_ORGS_AND_BELOW')
    expect(scopeCodeFromAxis1({ orgAnchor: 'CUSTOM_ORG', customOrgIds: [1] })).toBe('CUSTOM')
    expect(scopeCodeFromAxis1({ orgAnchor: 'PLUGIN_DIM', anchorParam: 'campusDim' })).toBe(
      'campusDim'
    )
  })

  it('RELATION with a non-admin relation still maps to MANAGED_ORGS (preset ignores anchorParam)', () => {
    expect(scopeCodeFromAxis1({ orgAnchor: 'RELATION', anchorParam: 'mentor' })).toBe('MANAGED_ORGS')
    expect(
      scopeCodeFromAxis1({ orgAnchor: 'RELATION', anchorParam: 'mentor', includeSubtree: true })
    ).toBe('MANAGED_ORGS_AND_BELOW')
  })

  it('defaults missing anchor to SELF', () => {
    expect(scopeCodeFromAxis1({})).toBe('SELF')
  })
})

describe('axis1Signature', () => {
  it('ignores axes ②③ — same axis① yields same signature', () => {
    const a = axis1Signature({ orgAnchor: 'PRIMARY_ORG', includeSubtree: true })
    const b = axis1Signature({
      orgAnchor: 'PRIMARY_ORG',
      includeSubtree: true,
      typeFilter: ['STUDENT'],
      subjectRelExclude: ['admin'],
    })
    expect(a).toBe(b)
  })

  it('distinguishes subtree and anchorParam', () => {
    expect(axis1Signature(SPEC.dept())).not.toBe(axis1Signature(SPEC.deptAndBelow()))
    expect(
      axis1Signature({ orgAnchor: 'RELATION', anchorParam: 'admin' })
    ).not.toBe(axis1Signature({ orgAnchor: 'RELATION', anchorParam: 'mentor' }))
  })

  it('treats missing anchor as SELF', () => {
    expect(axis1Signature({})).toBe(axis1Signature({ orgAnchor: 'SELF' }))
  })
})

describe('anchorLevel', () => {
  it('orders by width', () => {
    expect(anchorLevel({ orgAnchor: 'ALL' })).toBe(100)
    expect(anchorLevel({ orgAnchor: 'PRIMARY_ORG', includeSubtree: true })).toBe(80)
    expect(anchorLevel({ orgAnchor: 'RELATION', includeSubtree: true })).toBe(75)
    expect(anchorLevel({ orgAnchor: 'RELATION' })).toBe(65)
    expect(anchorLevel({ orgAnchor: 'PRIMARY_ORG' })).toBe(60)
    expect(anchorLevel({ orgAnchor: 'CUSTOM_ORG' })).toBe(40)
    expect(anchorLevel({ orgAnchor: 'SELF' })).toBe(20)
    expect(anchorLevel({})).toBe(20)
  })
})

describe('inferDefaultAndExceptions', () => {
  it('empty input → SELF default, no exceptions', () => {
    const { defaultSpec, exceptions } = inferDefaultAndExceptions([])
    expect(defaultSpec.orgAnchor).toBe('SELF')
    expect(exceptions).toEqual([])
  })

  it('picks the most common axis① as default; matching modules are not exceptions', () => {
    const modules = [
      mod('a', SPEC.deptAndBelow()),
      mod('b', SPEC.deptAndBelow()),
      mod('c', SPEC.deptAndBelow()),
      mod('d', SPEC.dept()),
    ]
    const { defaultSpec, exceptions } = inferDefaultAndExceptions(modules)
    expect(axis1Signature(defaultSpec)).toBe(axis1Signature(SPEC.deptAndBelow()))
    // only 'd' differs in axis①
    expect(exceptions.map(e => e.moduleCode)).toEqual(['d'])
  })

  it('tie on frequency → widest by level wins', () => {
    // dept (level 60) x2 vs all (level 100) x2 → ALL wins
    const modules = [
      mod('a', SPEC.dept()),
      mod('b', SPEC.dept()),
      mod('c', SPEC.all()),
      mod('d', SPEC.all()),
    ]
    const { defaultSpec, exceptions } = inferDefaultAndExceptions(modules)
    expect(axis1Signature(defaultSpec)).toBe(axis1Signature(SPEC.all()))
    expect(exceptions.map(e => e.moduleCode).sort()).toEqual(['a', 'b'])
  })

  it('module with no orgAnchor is treated as SELF', () => {
    const modules = [mod('a', {}), mod('b', SPEC.self()), mod('c', SPEC.dept())]
    const { defaultSpec, exceptions } = inferDefaultAndExceptions(modules)
    // SELF appears twice (a treated as SELF + b), dept once → default SELF
    expect(axis1Signature(defaultSpec)).toBe(axis1Signature(SPEC.self()))
    expect(exceptions.map(e => e.moduleCode)).toEqual(['c'])
  })

  it('module with only ②③ (same axis① as default) IS an exception', () => {
    const modules = [
      mod('a', SPEC.deptAndBelow()),
      mod('b', SPEC.deptAndBelow()),
      // same axis① as default, but carries axis③ type filter → exception
      mod('c', { ...SPEC.deptAndBelow(), typeFilter: ['STUDENT'] }),
    ]
    const { exceptions } = inferDefaultAndExceptions(modules)
    expect(exceptions.map(e => e.moduleCode)).toEqual(['c'])
    expect(exceptions[0].spec.typeFilter).toEqual(['STUDENT'])
  })

  it('module with only axis② exclude (same axis① as default) IS an exception', () => {
    const modules = [
      mod('a', SPEC.deptAndBelow()),
      mod('b', SPEC.deptAndBelow()),
      mod('c', { ...SPEC.deptAndBelow(), subjectRelExclude: ['admin'] }),
    ]
    const { exceptions } = inferDefaultAndExceptions(modules)
    expect(exceptions.map(e => e.moduleCode)).toEqual(['c'])
  })

  it('exceptions carry the FULL spec (all three axes)', () => {
    const userSpec: ScopeSpecVM = {
      orgAnchor: 'RELATION',
      anchorParam: 'admin',
      subjectRelExclude: ['admin'],
      typeFilter: ['STUDENT'],
    }
    const modules = [
      mod('m1', SPEC.deptAndBelow()),
      mod('m2', SPEC.deptAndBelow()),
      mod('user', userSpec),
    ]
    const { exceptions } = inferDefaultAndExceptions(modules)
    const ex = exceptions.find(e => e.moduleCode === 'user')!
    expect(ex.spec.orgAnchor).toBe('RELATION')
    expect(ex.spec.anchorParam).toBe('admin')
    expect(ex.spec.subjectRelExclude).toEqual(['admin'])
    expect(ex.spec.typeFilter).toEqual(['STUDENT'])
  })
})

describe('expandToCommands', () => {
  const codes = ['user', 'inspection_record', 'org_unit', 'dormitory', 'classroom']

  it('emits one command per code; exceptions use full spec, rest use default', () => {
    const def = SPEC.deptAndBelow()
    const exc: ResourceException[] = [
      {
        moduleCode: 'user',
        spec: {
          orgAnchor: 'RELATION',
          anchorParam: 'admin',
          subjectRelExclude: ['admin'],
          typeFilter: ['STUDENT'],
        },
      },
    ]
    const cmds = expandToCommands(def, exc, codes)
    expect(cmds.map(c => c.moduleCode).sort()).toEqual([...codes].sort())

    const userCmd = cmds.find(c => c.moduleCode === 'user')!
    expect(userCmd.orgAnchor).toBe('RELATION')
    expect(userCmd.anchorParam).toBe('admin')
    expect(userCmd.subjectRelExclude).toEqual(['admin'])
    expect(userCmd.typeFilter).toEqual(['STUDENT'])
    expect(userCmd.scopeCode).toBe('MANAGED_ORGS')

    const otherCmd = cmds.find(c => c.moduleCode === 'org_unit')!
    expect(otherCmd.orgAnchor).toBe('PRIMARY_ORG')
    expect(otherCmd.includeSubtree).toBe(true)
    expect(otherCmd.scopeCode).toBe('DEPARTMENT_AND_BELOW')
    // default carries no ②③
    expect(otherCmd.subjectRelInclude ?? []).toEqual([])
    expect(otherCmd.subjectRelExclude ?? []).toEqual([])
    expect(otherCmd.typeFilter ?? []).toEqual([])
  })

  it('derives scopeCode on every command', () => {
    const cmds = expandToCommands(SPEC.all(), [], ['a', 'b'])
    expect(cmds.every(c => c.scopeCode === 'ALL')).toBe(true)
  })
})

describe('presetCodeToSpec', () => {
  it('round-trips with scopeCodeFromAxis1 for the 7 presets', () => {
    const presets = [
      'ALL',
      'SELF',
      'DEPARTMENT',
      'DEPARTMENT_AND_BELOW',
      'MANAGED_ORGS',
      'MANAGED_ORGS_AND_BELOW',
      'CUSTOM',
    ]
    for (const code of presets) {
      expect(scopeCodeFromAxis1(presetCodeToSpec(code))).toBe(code)
    }
  })

  it('maps each preset to the expected axis① anchor', () => {
    expect(presetCodeToSpec('ALL').orgAnchor).toBe('ALL')
    expect(presetCodeToSpec('SELF').orgAnchor).toBe('SELF')
    expect(presetCodeToSpec('DEPARTMENT')).toMatchObject({
      orgAnchor: 'PRIMARY_ORG',
    })
    expect(presetCodeToSpec('DEPARTMENT')).not.toMatchObject({ includeSubtree: true })
    expect(presetCodeToSpec('DEPARTMENT_AND_BELOW')).toMatchObject({
      orgAnchor: 'PRIMARY_ORG',
      includeSubtree: true,
    })
    expect(presetCodeToSpec('MANAGED_ORGS')).toMatchObject({
      orgAnchor: 'RELATION',
      anchorParam: 'admin',
    })
    expect(presetCodeToSpec('MANAGED_ORGS_AND_BELOW')).toMatchObject({
      orgAnchor: 'RELATION',
      anchorParam: 'admin',
      includeSubtree: true,
    })
    expect(presetCodeToSpec('CUSTOM').orgAnchor).toBe('CUSTOM_ORG')
  })
})

describe('clampSpecToAllowed', () => {
  it('null/undefined/empty allowed → no restriction (default unchanged)', () => {
    const def = SPEC.deptAndBelow()
    expect(clampSpecToAllowed(def, null)).toEqual(def)
    expect(clampSpecToAllowed(def, undefined)).toEqual(def)
    expect(clampSpecToAllowed(def, [])).toEqual(def)
  })

  it('default code already allowed → unchanged', () => {
    const def = SPEC.deptAndBelow()
    expect(clampSpecToAllowed(def, ['SELF', 'DEPARTMENT_AND_BELOW'])).toEqual(def)
  })

  it('default code excluded → falls back to allowed scope, never wider than default', () => {
    // default=DEPARTMENT_AND_BELOW (level 80), allowed only [SELF] → SELF
    const clamped = clampSpecToAllowed(SPEC.deptAndBelow(), ['SELF'])
    expect(scopeCodeFromAxis1(clamped)).toBe('SELF')
    expect(clamped.orgAnchor).toBe('SELF')
  })

  it('never widens beyond default: default=DEPARTMENT, allowed=[ALL,SELF] → SELF', () => {
    // DEPARTMENT level=60. ALL=100 (wider, forbidden), SELF=20 (≤60) → SELF
    const clamped = clampSpecToAllowed(SPEC.dept(), ['ALL', 'SELF'])
    expect(scopeCodeFromAxis1(clamped)).toBe('SELF')
  })

  it('picks the WIDEST allowed scope no wider than default', () => {
    // default=DEPARTMENT_AND_BELOW level=80. allowed=[SELF, DEPARTMENT] →
    // DEPARTMENT (60) is widest ≤80 → DEPARTMENT, not SELF
    const clamped = clampSpecToAllowed(SPEC.deptAndBelow(), ['SELF', 'DEPARTMENT'])
    expect(scopeCodeFromAxis1(clamped)).toBe('DEPARTMENT')
  })

  it('all allowed wider than default → pick the NARROWEST allowed', () => {
    // default=SELF level=20. allowed=[ALL, DEPARTMENT_AND_BELOW] both wider →
    // narrowest = DEPARTMENT_AND_BELOW (80) over ALL (100)
    const clamped = clampSpecToAllowed(SPEC.self(), ['ALL', 'DEPARTMENT_AND_BELOW'])
    expect(scopeCodeFromAxis1(clamped)).toBe('DEPARTMENT_AND_BELOW')
  })
})

describe('expandToCommands with allowed_scopes clamping', () => {
  const codes = ['user', 'org_unit', 'announcement']

  it('clamps a non-exception resource whose allowedScopes excludes the default code', () => {
    // default=DEPARTMENT_AND_BELOW; announcement allows only [SELF]
    const def = SPEC.deptAndBelow()
    const allowed: Record<string, string[] | null | undefined> = {
      user: null, // unrestricted
      org_unit: ['DEPARTMENT_AND_BELOW', 'DEPARTMENT'], // honors default
      announcement: ['SELF'], // cannot honor org scope
    }
    const cmds = expandToCommands(def, [], codes, allowed)

    const ann = cmds.find(c => c.moduleCode === 'announcement')!
    expect(ann.scopeCode).toBe('SELF')
    expect(ann.orgAnchor).toBe('SELF') // axis① matches the clamped scopeCode

    // unrestricted + honoring resources keep the default
    expect(cmds.find(c => c.moduleCode === 'user')!.scopeCode).toBe('DEPARTMENT_AND_BELOW')
    expect(cmds.find(c => c.moduleCode === 'org_unit')!.scopeCode).toBe('DEPARTMENT_AND_BELOW')
    expect(cmds.find(c => c.moduleCode === 'org_unit')!.orgAnchor).toBe('PRIMARY_ORG')
  })

  it('emitted scopeCode is ALWAYS ∈ allowedScopes when non-empty', () => {
    const def = SPEC.all()
    const allowed: Record<string, string[] | null | undefined> = {
      user: ['SELF', 'DEPARTMENT'],
      org_unit: ['MANAGED_ORGS'],
      announcement: null,
    }
    const cmds = expandToCommands(def, [], codes, allowed)
    for (const c of cmds) {
      const a = allowed[c.moduleCode]
      if (a && a.length) {
        expect(a).toContain(c.scopeCode)
      }
    }
  })

  it('allowedScopesByCode omitted → behaves exactly like before (no clamping)', () => {
    const def = SPEC.deptAndBelow()
    const cmds = expandToCommands(def, [], codes)
    expect(cmds.every(c => c.scopeCode === 'DEPARTMENT_AND_BELOW')).toBe(true)
  })

  it('exceptions are emitted UNCHANGED regardless of allowedScopes', () => {
    const def = SPEC.deptAndBelow()
    const exc: ResourceException[] = [
      {
        moduleCode: 'user',
        spec: {
          orgAnchor: 'RELATION',
          anchorParam: 'admin',
          subjectRelExclude: ['admin'],
          typeFilter: ['STUDENT'],
        },
      },
    ]
    // even though we pretend user only allows SELF, the exception is left as-is
    const allowed = { user: ['SELF'], org_unit: null, announcement: null }
    const cmds = expandToCommands(def, exc, codes, allowed)
    const userCmd = cmds.find(c => c.moduleCode === 'user')!
    expect(userCmd.orgAnchor).toBe('RELATION')
    expect(userCmd.scopeCode).toBe('MANAGED_ORGS')
    expect(userCmd.typeFilter).toEqual(['STUDENT'])
  })
})

describe('round-trip stability (THE invariant)', () => {
  function assertRoundTrip(def: ScopeSpecVM, exc: ResourceException[], codes: string[]) {
    const cmds = expandToCommands(def, exc, codes)
    const inferred = inferDefaultAndExceptions(cmds)
    // same default axis①
    expect(axis1Signature(inferred.defaultSpec)).toBe(axis1Signature(def))
    // same exception set (by moduleCode)
    expect(inferred.exceptions.map(e => e.moduleCode).sort()).toEqual(
      exc.map(e => e.moduleCode).sort()
    )
    // each exception preserves its axis① + ②③ semantics
    for (const original of exc) {
      const got = inferred.exceptions.find(e => e.moduleCode === original.moduleCode)!
      expect(axis1Signature(got.spec)).toBe(axis1Signature(original.spec))
      expect(got.spec.subjectRelInclude ?? []).toEqual(original.spec.subjectRelInclude ?? [])
      expect(got.spec.subjectRelExclude ?? []).toEqual(original.spec.subjectRelExclude ?? [])
      expect(got.spec.typeFilter ?? []).toEqual(original.spec.typeFilter ?? [])
    }
  }

  it('default=DEPARTMENT_AND_BELOW with user exception (RELATION admin + exclude admin + type STUDENT) over 5 codes', () => {
    const codes = ['user', 'inspection_record', 'org_unit', 'dormitory', 'classroom']
    const def = SPEC.deptAndBelow()
    const exc: ResourceException[] = [
      {
        moduleCode: 'user',
        spec: {
          orgAnchor: 'RELATION',
          anchorParam: 'admin',
          subjectRelExclude: ['admin'],
          typeFilter: ['STUDENT'],
        },
      },
    ]
    assertRoundTrip(def, exc, codes)
  })

  it('default=ALL, no exceptions', () => {
    assertRoundTrip(SPEC.all(), [], ['a', 'b', 'c', 'd'])
  })

  it('default=SELF with two exceptions', () => {
    const def = SPEC.self()
    const exc: ResourceException[] = [
      { moduleCode: 'user', spec: { ...SPEC.dept(), typeFilter: ['STUDENT'] } },
      { moduleCode: 'org_unit', spec: SPEC.all() },
    ]
    assertRoundTrip(def, exc, ['user', 'org_unit', 'x', 'y', 'z'])
  })

  it('default=CUSTOM_ORG with a managed-and-below exception', () => {
    const def = SPEC.custom()
    const exc: ResourceException[] = [{ moduleCode: 'user', spec: SPEC.managedAndBelow() }]
    assertRoundTrip(def, exc, ['user', 'a', 'b'])
  })
})
