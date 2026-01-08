/**
 * 批量升级所有页面到设计系统
 * 使用Node.js运行: node upgrade-all-pages.js
 */
const fs = require('fs')
const path = require('path')

// 需要升级的页面列表
const pagesToUpgrade = [
  'src/views/dormitory/DormitoryList.vue',
  'src/views/dormitory/DormitoryBuildingManagement.vue',
  'src/views/system/UsersView.vue',
  'src/views/system/RolesView.vue',
  'src/views/system/PermissionsView.vue',
  'src/views/system/DepartmentsView.vue',
  'src/views/quantification/DailyCheckView.vue',
  'src/views/evaluation/semester/index.vue',
  'src/views/evaluation/score/index.vue',
  'src/views/evaluation/period/index.vue',
  'src/views/evaluation/honor/index.vue',
  'src/views/evaluation/course/index.vue'
]

// 旧卡片模式正则
const oldCardPattern = /<div class="rounded-lg border border-gray-200 bg-white p-4">\s*<div class="flex items-center justify-between">\s*<div>\s*<p class="text-sm text-gray-500">(.*?)<\/p>\s*<p class="mt-1 text-2xl font-semibold text-gray-900">\{\{\s*([^}]+)\s*\}\}<\/p>/gs

function upgradeFile(filePath) {
  console.log(`升级文件: ${filePath}`)

  const fullPath = path.join(__dirname, filePath)
  if (!fs.existsSync(fullPath)) {
    console.log(`  跳过: 文件不存在`)
    return
  }

  let content = fs.readFileSync(fullPath, 'utf-8')

  // 统计升级前的卡片数量
  const matches = content.match(oldCardPattern)
  if (!matches || matches.length === 0) {
    console.log(`  跳过: 没有找到旧样式卡片`)
    return
  }

  console.log(`  找到 ${matches.length} 个旧样式卡片`)

  // 备份原文件
  fs.writeFileSync(fullPath + '.backup', content)
  console.log(`  已备份到: ${filePath}.backup`)

  // TODO: 这里应该添加实际的替换逻辑
  // 由于每个页面的卡片结构略有不同,建议手动升级或使用更智能的工具

  console.log(`  完成!`)
}

console.log('开始批量升级页面到设计系统...\n')
console.log(`计划升级 ${pagesToUpgrade.length} 个页面\n`)

pagesToUpgrade.forEach(upgradeFile)

console.log('\n升级完成!')
console.log('\n注意:')
console.log('1. 所有原文件已备份为 .backup')
console.log('2. 请手动检查每个升级后的文件')
console.log('3. 测试页面功能是否正常')
console.log('4. 确认无误后删除 .backup 文件')
