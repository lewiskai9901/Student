export interface AppModule {
  /** Permission code required to see this module */
  key: string
  /** Display label */
  label: string
  /** Icon name (wot-design-uni icon) */
  icon: string
  /** Icon color for the container background */
  iconColor: string
  /** Navigation path */
  path: string
}
