import type { PluginPackage } from '../core/plugin/package'
import demo from './demo/manifest'
import inspection from './inspection/manifest'
import education from './education/manifest'

export const allPlugins: PluginPackage[] = [demo, inspection, education]
