export type Unsubscribe = () => void
export type EventHandler<T = unknown> = (payload: T) => void

export interface PluginEventBus {
  emit<T>(eventName: string, payload: T): void
  on<T>(eventName: string, handler: EventHandler<T>): Unsubscribe
  off(eventName: string, handler: EventHandler): void
}

export function createEventBus(): PluginEventBus {
  const handlers = new Map<string, Set<EventHandler>>()

  function validate(name: string) {
    if (!/^[a-z][a-z0-9-]*\.[a-z][a-z0-9-]*\.[a-z][a-z0-9-]*$/.test(name)) {
      throw new Error(`Invalid event name "${name}", must be <plugin>.<entity>.<verb>`)
    }
  }

  return {
    emit(name, payload) {
      validate(name)
      const set = handlers.get(name)
      if (!set) return
      for (const h of set) {
        try { h(payload) } catch (e) { console.error(`[bus] handler error on ${name}:`, e) }
      }
    },
    on(name, handler) {
      validate(name)
      let set = handlers.get(name)
      if (!set) handlers.set(name, set = new Set())
      set.add(handler as EventHandler)
      return () => set!.delete(handler as EventHandler)
    },
    off(name, handler) {
      handlers.get(name)?.delete(handler)
    }
  }
}
