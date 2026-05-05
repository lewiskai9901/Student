import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import RiskMatrixEditor from '@/views/inspection/config/components/RiskMatrixEditor.vue'

describe('RiskMatrixEditor', () => {
  it('挂载默认 4×4 矩阵 + L/M/H/VH 等级', () => {
    const w = mount(RiskMatrixEditor, {
      global: { stubs: { 'el-select': true, 'el-option': true, 'el-input-number': true, 'el-radio-group': true, 'el-radio': true } },
    })
    expect(w.findAll('.rmx-cell').length).toBe(16)  // 4×4
    const levels = w.findAll('.rmx-cell').map(c => c.text())
    expect(new Set(levels).size).toBeGreaterThanOrEqual(2)  // 至少 2 种 level
  })

  it('cycleCell 点击单元格循环 L→M→H→VH→L', async () => {
    const w = mount(RiskMatrixEditor, {
      global: { stubs: { 'el-select': true, 'el-option': true, 'el-input-number': true, 'el-radio-group': true, 'el-radio': true } },
    })
    const firstCell = w.findAll('.rmx-cell')[0]
    const original = firstCell.text()
    await firstCell.trigger('click')
    expect(firstCell.text()).not.toBe(original)
  })

  it('emit update:modelValue 输出合法 JSON', async () => {
    const w = mount(RiskMatrixEditor, {
      global: { stubs: { 'el-select': true, 'el-option': true, 'el-input-number': true, 'el-radio-group': true, 'el-radio': true } },
    })
    // immediate watch 触发 emit
    await w.vm.$nextTick()
    const events = w.emitted('update:modelValue')
    expect(events).toBeTruthy()
    const last = events![events!.length - 1][0] as string
    const parsed = JSON.parse(last)
    expect(parsed.matrix).toHaveLength(4)
    expect(parsed.matrix[0]).toHaveLength(4)
    expect(parsed.levelToSeverity).toEqual({ L: 0, M: 0.4, H: 0.75, VH: 1.0 })
  })

  it('反向加载已有 modelValue 应用矩阵', async () => {
    const json = JSON.stringify({
      matrix: [
        [{ level: 'L' }, { level: 'L' }, { level: 'L' }],
        [{ level: 'M' }, { level: 'M' }, { level: 'M' }],
        [{ level: 'H' }, { level: 'H' }, { level: 'H' }],
      ],
      levelToSeverity: { L: 0, M: 0.5, H: 1.0 },
      correctiveTrigger: { fromLevel: 'M' },
    })
    const w = mount(RiskMatrixEditor, {
      props: { modelValue: json },
      global: { stubs: { 'el-select': true, 'el-option': true, 'el-input-number': true, 'el-radio-group': true, 'el-radio': true } },
    })
    await w.vm.$nextTick()
    expect(w.findAll('.rmx-cell').length).toBe(9)  // 3×3
  })
})
