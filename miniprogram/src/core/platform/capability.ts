// Full implementation in Task 5 (PlatformCapability SPI).
// This shell exists so PluginContext can reference the type.
export interface PlatformCapability {
  readonly platform: string
}
