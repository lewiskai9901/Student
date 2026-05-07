import type { PluginPackage } from '../core/plugin/package'
import demo from './demo/manifest'
import healthcare from './healthcare/manifest'
import inspection from './inspection/manifest'
import education from './education/manifest'

export const allPlugins: PluginPackage[] = [demo, healthcare, inspection, education]
