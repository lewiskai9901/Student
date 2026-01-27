import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import StatusTag from '@/components/StatusTag.vue'
import ElementPlus from 'element-plus'

/**
 * StatusTag 组件测试
 */
describe('StatusTag', () => {
  const createWrapper = (props = {}, slots = {}) => {
    return mount(StatusTag, {
      props,
      slots,
      global: {
        plugins: [ElementPlus]
      }
    })
  }

  describe('Props', () => {
    it('should render with default props', () => {
      const wrapper = createWrapper()

      expect(wrapper.find('.status-tag').exists()).toBe(true)
    })

    it('should render label prop', () => {
      const wrapper = createWrapper({ label: '已完成' })

      expect(wrapper.text()).toContain('已完成')
    })

    it('should apply correct status type', () => {
      const statuses = ['success', 'warning', 'danger', 'info', 'primary', 'default'] as const

      statuses.forEach(status => {
        const wrapper = createWrapper({ status })
        const tag = wrapper.findComponent({ name: 'ElTag' })

        expect(tag.exists()).toBe(true)
      })
    })

    it('should render with size prop', () => {
      const wrapper = createWrapper({ size: 'small' })
      const tag = wrapper.findComponent({ name: 'ElTag' })

      expect(tag.props('size')).toBe('small')
    })

    it('should render with effect prop', () => {
      const wrapper = createWrapper({ effect: 'dark' })
      const tag = wrapper.findComponent({ name: 'ElTag' })

      expect(tag.props('effect')).toBe('dark')
    })

    it('should render with round prop', () => {
      const wrapper = createWrapper({ round: true })
      const tag = wrapper.findComponent({ name: 'ElTag' })

      expect(tag.props('round')).toBe(true)
    })

    it('should show dot when showDot is true', () => {
      const wrapper = createWrapper({ showDot: true, status: 'success' })

      expect(wrapper.find('.status-tag__dot').exists()).toBe(true)
      expect(wrapper.find('.status-tag__dot--success').exists()).toBe(true)
    })

    it('should not show dot by default', () => {
      const wrapper = createWrapper()

      expect(wrapper.find('.status-tag__dot').exists()).toBe(false)
    })
  })

  describe('Slots', () => {
    it('should render default slot content', () => {
      const wrapper = createWrapper({}, {
        default: () => '自定义内容'
      })

      expect(wrapper.text()).toContain('自定义内容')
    })

    it('should prefer slot over label prop', () => {
      const wrapper = createWrapper({ label: '标签内容' }, {
        default: () => '插槽内容'
      })

      expect(wrapper.text()).toContain('插槽内容')
    })
  })

  describe('Status to TagType mapping', () => {
    it('should map success status to success type', () => {
      const wrapper = createWrapper({ status: 'success' })
      const tag = wrapper.findComponent({ name: 'ElTag' })

      expect(tag.props('type')).toBe('success')
    })

    it('should map warning status to warning type', () => {
      const wrapper = createWrapper({ status: 'warning' })
      const tag = wrapper.findComponent({ name: 'ElTag' })

      expect(tag.props('type')).toBe('warning')
    })

    it('should map danger status to danger type', () => {
      const wrapper = createWrapper({ status: 'danger' })
      const tag = wrapper.findComponent({ name: 'ElTag' })

      expect(tag.props('type')).toBe('danger')
    })

    it('should map default status to empty type', () => {
      const wrapper = createWrapper({ status: 'default' })
      const tag = wrapper.findComponent({ name: 'ElTag' })

      expect(tag.props('type')).toBe('')
    })
  })

  describe('Dot styles', () => {
    const dotStatuses = ['success', 'warning', 'danger', 'info', 'primary', 'default'] as const

    dotStatuses.forEach(status => {
      it(`should apply correct dot class for ${status} status`, () => {
        const wrapper = createWrapper({ showDot: true, status })

        expect(wrapper.find(`.status-tag__dot--${status}`).exists()).toBe(true)
      })
    })
  })
})
