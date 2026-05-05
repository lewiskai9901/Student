import { describe, it, expect, vi } from 'vitest'
import { createEventBus } from './event-bus'

describe('PluginEventBus', () => {
  it('emits event to subscribed handler', () => {
    const bus = createEventBus()
    const handler = vi.fn()
    bus.on('a.b.c', handler)
    bus.emit('a.b.c', { x: 1 })
    expect(handler).toHaveBeenCalledWith({ x: 1 })
  })

  it('multiple handlers all receive event', () => {
    const bus = createEventBus()
    const h1 = vi.fn(), h2 = vi.fn()
    bus.on('e.f.g', h1); bus.on('e.f.g', h2)
    bus.emit('e.f.g', 1)
    expect(h1).toHaveBeenCalled(); expect(h2).toHaveBeenCalled()
  })

  it('unsubscribe via returned handle', () => {
    const bus = createEventBus()
    const handler = vi.fn()
    const off = bus.on('e.f.g', handler)
    off()
    bus.emit('e.f.g', 1)
    expect(handler).not.toHaveBeenCalled()
  })

  it('rejects event names without three dot-segments (must be plugin.entity.verb)', () => {
    const bus = createEventBus()
    expect(() => bus.emit('bad', {})).toThrow(/event name/i)
    expect(() => bus.emit('only.two', {})).toThrow(/event name/i)
  })

  it('handler error does not break other handlers', () => {
    const bus = createEventBus()
    const broken = vi.fn(() => { throw new Error('boom') })
    const ok = vi.fn()
    bus.on('e.f.g', broken)
    bus.on('e.f.g', ok)
    bus.emit('e.f.g', 1)
    expect(ok).toHaveBeenCalled()
  })
})
