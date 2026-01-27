<template>
  <div class="min-h-screen bg-gray-50/50 p-6">
    <!-- 页面头部 -->
    <div class="mb-6 rounded-xl bg-gradient-to-r from-indigo-600 to-purple-500 p-6 shadow-lg">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="flex items-center gap-3 text-2xl font-bold text-white">
            <Palette class="h-8 w-8" />
            登录页自定义
          </h1>
          <p class="mt-1 text-indigo-100">自定义登录页背景和品牌展示</p>
        </div>
        <div class="flex gap-3">
          <button
            @click="resetToDefault"
            class="flex items-center gap-2 rounded-lg bg-white/20 px-4 py-2 text-sm font-medium text-white backdrop-blur-sm transition-all hover:bg-white/30"
          >
            <RotateCcw class="h-4 w-4" />
            恢复默认
          </button>
          <button
            @click="saveConfig"
            :disabled="saving"
            class="flex items-center gap-2 rounded-lg bg-white/95 px-4 py-2 font-medium text-indigo-600 shadow-md transition-all hover:-translate-y-0.5 hover:bg-white hover:shadow-lg disabled:opacity-50"
          >
            <Loader2 v-if="saving" class="h-5 w-5 animate-spin" />
            <Save v-else class="h-5 w-5" />
            保存配置
          </button>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-5 gap-6">
      <!-- 左侧配置表单 (3列宽) -->
      <div class="lg:col-span-3">
        <!-- Tab 导航 -->
        <div class="bg-white rounded-xl shadow-sm mb-6">
          <div class="flex border-b border-gray-200">
            <button
              v-for="tab in tabs"
              :key="tab.id"
              @click="activeTab = tab.id"
              :class="[
                'flex items-center gap-2 px-6 py-4 text-sm font-medium border-b-2 transition-all -mb-px',
                activeTab === tab.id
                  ? 'border-indigo-600 text-indigo-600 bg-indigo-50/50'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              ]"
            >
              <component :is="tab.icon" class="h-5 w-5" />
              {{ tab.label }}
            </button>
          </div>

          <!-- Tab 内容区 -->
          <div class="p-6">
            <!-- Tab 1: 布局与背景 -->
            <div v-show="activeTab === 'layout'" class="space-y-6">
              <!-- 显示模式 -->
              <div>
                <h4 class="text-sm font-semibold text-gray-900 mb-3 flex items-center gap-2">
                  <Layout class="h-4 w-4 text-indigo-600" />
                  显示模式
                </h4>
                <div class="grid grid-cols-2 gap-4">
                  <button
                    v-for="mode in displayModes"
                    :key="mode.value"
                    type="button"
                    @click="formData.displayMode = mode.value"
                    :class="[
                      'flex flex-col items-center gap-2 p-4 rounded-xl border-2 transition-all',
                      formData.displayMode === mode.value
                        ? 'border-indigo-500 bg-indigo-50'
                        : 'border-gray-200 hover:border-gray-300'
                    ]"
                  >
                    <component
                      :is="mode.icon"
                      :class="[
                        'h-7 w-7',
                        formData.displayMode === mode.value ? 'text-indigo-600' : 'text-gray-400'
                      ]"
                    />
                    <span
                      :class="[
                        'text-sm font-medium',
                        formData.displayMode === mode.value ? 'text-indigo-600' : 'text-gray-600'
                      ]"
                    >
                      {{ mode.label }}
                    </span>
                    <span
                      :class="[
                        'text-xs',
                        formData.displayMode === mode.value ? 'text-indigo-500' : 'text-gray-400'
                      ]"
                    >
                      {{ mode.desc }}
                    </span>
                  </button>
                </div>
                <p class="mt-3 text-xs text-gray-500">
                  <span class="font-medium">半屏模式：</span>背景仅左侧显示，推荐竖向（3:4）素材 |
                  <span class="font-medium">全屏模式：</span>背景铺满页面，推荐横向（16:9）素材
                </p>
              </div>

              <div class="border-t border-gray-100 pt-6">
                <!-- 背景模式选择 -->
                <h4 class="text-sm font-semibold text-gray-900 mb-3 flex items-center gap-2">
                  <Image class="h-4 w-4 text-indigo-600" />
                  背景设置
                </h4>
                <div class="grid grid-cols-3 gap-3 mb-4">
                  <button
                    v-for="mode in backgroundModes"
                    :key="mode.value"
                    type="button"
                    @click="formData.backgroundMode = mode.value"
                    :class="[
                      'flex flex-col items-center gap-2 p-3 rounded-xl border-2 transition-all',
                      formData.backgroundMode === mode.value
                        ? 'border-indigo-500 bg-indigo-50'
                        : 'border-gray-200 hover:border-gray-300'
                    ]"
                  >
                    <component
                      :is="mode.icon"
                      :class="[
                        'h-5 w-5',
                        formData.backgroundMode === mode.value ? 'text-indigo-600' : 'text-gray-400'
                      ]"
                    />
                    <span
                      :class="[
                        'text-sm font-medium',
                        formData.backgroundMode === mode.value ? 'text-indigo-600' : 'text-gray-600'
                      ]"
                    >
                      {{ mode.label }}
                    </span>
                  </button>
                </div>

                <!-- 渐变色设置 -->
                <div v-if="formData.backgroundMode === 'gradient'" class="space-y-4">
                  <div class="grid grid-cols-3 gap-4">
                    <div>
                      <label class="block text-sm font-medium text-gray-700 mb-2">起始颜色</label>
                      <div class="flex items-center gap-2">
                        <input
                          type="color"
                          v-model="formData.gradientFrom"
                          class="w-10 h-10 rounded-lg border border-gray-300 cursor-pointer"
                        />
                        <input
                          type="text"
                          v-model="formData.gradientFrom"
                          class="flex-1 h-10 px-3 text-sm border border-gray-300 rounded-lg"
                        />
                      </div>
                    </div>
                    <div>
                      <label class="block text-sm font-medium text-gray-700 mb-2">中间颜色</label>
                      <div class="flex items-center gap-2">
                        <input
                          type="color"
                          v-model="formData.gradientVia"
                          class="w-10 h-10 rounded-lg border border-gray-300 cursor-pointer"
                        />
                        <input
                          type="text"
                          v-model="formData.gradientVia"
                          class="flex-1 h-10 px-3 text-sm border border-gray-300 rounded-lg"
                        />
                      </div>
                    </div>
                    <div>
                      <label class="block text-sm font-medium text-gray-700 mb-2">结束颜色</label>
                      <div class="flex items-center gap-2">
                        <input
                          type="color"
                          v-model="formData.gradientTo"
                          class="w-10 h-10 rounded-lg border border-gray-300 cursor-pointer"
                        />
                        <input
                          type="text"
                          v-model="formData.gradientTo"
                          class="flex-1 h-10 px-3 text-sm border border-gray-300 rounded-lg"
                        />
                      </div>
                    </div>
                  </div>
                  <div
                    class="h-10 rounded-lg"
                    :style="{
                      background: `linear-gradient(to right, ${formData.gradientFrom}, ${formData.gradientVia}, ${formData.gradientTo})`
                    }"
                  ></div>
                </div>

                <!-- 图片上传 -->
                <div v-else-if="formData.backgroundMode === 'image'" class="space-y-3">
                  <p class="text-xs text-gray-500">
                    支持 JPG/PNG/GIF 格式。
                    {{ formData.displayMode === 'fullScreen' ? '推荐 1920x1080（16:9）' : '推荐 1080x1440（3:4）' }}
                  </p>
                  <ImageCropper
                    v-model="formData.backgroundImage"
                    :aspect-ratio="cropAspectRatio"
                    :max-size-m-b="20"
                    accept-types="image/jpeg,image/png,image/gif,image/webp"
                    :preview-width="formData.displayMode === 'fullScreen' ? 200 : 150"
                    :preview-height="formData.displayMode === 'fullScreen' ? 112 : 200"
                    @uploaded="onMediaUploaded"
                  />
                </div>

                <!-- 视频上传 -->
                <div v-else-if="formData.backgroundMode === 'video'" class="space-y-3">
                  <p class="text-xs text-gray-500">
                    支持 MP4 格式，建议不超过 50MB。
                    {{ formData.displayMode === 'fullScreen' ? '推荐 1920x1080（16:9）' : '推荐 1080x1440（3:4）' }}
                  </p>
                  <ImageCropper
                    v-model="formData.backgroundVideo"
                    :aspect-ratio="cropAspectRatio"
                    :max-size-m-b="50"
                    accept-types="video/mp4,video/webm"
                    :preview-width="formData.displayMode === 'fullScreen' ? 200 : 150"
                    :preview-height="formData.displayMode === 'fullScreen' ? 112 : 200"
                    @uploaded="onMediaUploaded"
                  />
                </div>
              </div>

              <!-- 透明度设置 -->
              <div class="border-t border-gray-100 pt-6">
                <h4 class="text-sm font-semibold text-gray-900 mb-4 flex items-center gap-2">
                  <Sliders class="h-4 w-4 text-indigo-600" />
                  透明度调整
                </h4>
                <div class="grid grid-cols-2 gap-6">
                  <div>
                    <div class="flex items-center justify-between mb-2">
                      <label class="text-sm text-gray-700">背景遮罩</label>
                      <span class="text-sm text-gray-500">{{ formData.overlayOpacity }}%</span>
                    </div>
                    <input
                      type="range"
                      v-model.number="formData.overlayOpacity"
                      min="0"
                      max="100"
                      step="5"
                      class="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-indigo-600"
                    />
                    <p class="mt-1 text-xs text-gray-400">图片/视频模式的黑色遮罩</p>
                  </div>
                  <div>
                    <div class="flex items-center justify-between mb-2">
                      <label class="text-sm text-gray-700">表单背景</label>
                      <span class="text-sm text-gray-500">{{ formData.formBgOpacity }}%</span>
                    </div>
                    <input
                      type="range"
                      v-model.number="formData.formBgOpacity"
                      min="0"
                      max="100"
                      step="5"
                      class="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-indigo-600"
                    />
                    <p class="mt-1 text-xs text-gray-400">登录表单区域的背景</p>
                  </div>
                </div>
              </div>

              <!-- 背景轮播 -->
              <div class="border-t border-gray-100 pt-6">
                <BackgroundCarouselManager
                  v-model="formData.backgroundCarousel"
                  :display-mode="formData.displayMode"
                />
              </div>
            </div>

            <!-- Tab 2: 品牌内容 -->
            <div v-show="activeTab === 'content'" class="space-y-6">
              <!-- 元素显示开关 -->
              <div class="grid grid-cols-4 gap-3 p-4 bg-gray-50 rounded-xl">
                <div class="flex items-center gap-2">
                  <button
                    type="button"
                    @click="formData.showLogo = !formData.showLogo"
                    :class="[
                      'relative inline-flex h-5 w-9 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none',
                      formData.showLogo ? 'bg-indigo-600' : 'bg-gray-300'
                    ]"
                  >
                    <span
                      :class="[
                        'pointer-events-none inline-block h-4 w-4 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out',
                        formData.showLogo ? 'translate-x-4' : 'translate-x-0'
                      ]"
                    />
                  </button>
                  <span class="text-sm text-gray-700">Logo</span>
                </div>
                <div class="flex items-center gap-2">
                  <button
                    type="button"
                    @click="formData.showTitle = !formData.showTitle"
                    :class="[
                      'relative inline-flex h-5 w-9 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none',
                      formData.showTitle ? 'bg-indigo-600' : 'bg-gray-300'
                    ]"
                  >
                    <span
                      :class="[
                        'pointer-events-none inline-block h-4 w-4 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out',
                        formData.showTitle ? 'translate-x-4' : 'translate-x-0'
                      ]"
                    />
                  </button>
                  <span class="text-sm text-gray-700">标题</span>
                </div>
                <div class="flex items-center gap-2">
                  <button
                    type="button"
                    @click="formData.showSubtitle = !formData.showSubtitle"
                    :class="[
                      'relative inline-flex h-5 w-9 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none',
                      formData.showSubtitle ? 'bg-indigo-600' : 'bg-gray-300'
                    ]"
                  >
                    <span
                      :class="[
                        'pointer-events-none inline-block h-4 w-4 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out',
                        formData.showSubtitle ? 'translate-x-4' : 'translate-x-0'
                      ]"
                    />
                  </button>
                  <span class="text-sm text-gray-700">标语</span>
                </div>
                <div class="flex items-center gap-2">
                  <button
                    type="button"
                    @click="formData.showCopyright = !formData.showCopyright"
                    :class="[
                      'relative inline-flex h-5 w-9 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none',
                      formData.showCopyright ? 'bg-indigo-600' : 'bg-gray-300'
                    ]"
                  >
                    <span
                      :class="[
                        'pointer-events-none inline-block h-4 w-4 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out',
                        formData.showCopyright ? 'translate-x-4' : 'translate-x-0'
                      ]"
                    />
                  </button>
                  <span class="text-sm text-gray-700">版权</span>
                </div>
              </div>

              <!-- 标题设置 -->
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-2">主标题</label>
                  <input
                    v-model="formData.title"
                    type="text"
                    placeholder="留空则使用系统名称"
                    class="w-full h-10 px-3 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                  />
                  <p class="mt-1 text-xs text-gray-400">
                    用 <code class="bg-gray-100 px-1 rounded">\n</code> 换行，如：学生管理\n系统
                  </p>
                </div>
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-2">版权文字</label>
                  <input
                    v-model="formData.copyrightText"
                    type="text"
                    placeholder="留空则使用系统版权"
                    class="w-full h-10 px-3 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                  />
                  <p class="mt-1 text-xs text-gray-400">当前：{{ configStore.systemCopyright }}</p>
                </div>
              </div>

              <!-- 标语 -->
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">标语</label>
                <input
                  v-model="formData.subtitle"
                  type="text"
                  placeholder="请输入标语"
                  class="w-full h-10 px-3 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                />
                <p class="mt-1 text-xs text-gray-400">
                  提示：用 <code class="bg-gray-100 px-1 rounded">**关键词**</code> 语法可高亮显示
                </p>
              </div>

              <!-- 文字位置 -->
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-3">内容位置</label>
                <div class="grid grid-cols-3 gap-3">
                  <button
                    v-for="pos in textPositions"
                    :key="pos.value"
                    type="button"
                    @click="formData.textPosition = pos.value"
                    :class="[
                      'flex flex-col items-center gap-1 p-3 rounded-lg border-2 transition-all',
                      formData.textPosition === pos.value
                        ? 'border-indigo-500 bg-indigo-50'
                        : 'border-gray-200 hover:border-gray-300'
                    ]"
                  >
                    <component
                      :is="pos.icon"
                      :class="[
                        'h-5 w-5',
                        formData.textPosition === pos.value ? 'text-indigo-600' : 'text-gray-400'
                      ]"
                    />
                    <span
                      :class="[
                        'text-xs font-medium',
                        formData.textPosition === pos.value ? 'text-indigo-600' : 'text-gray-600'
                      ]"
                    >
                      {{ pos.label }}
                    </span>
                  </button>
                </div>
              </div>
            </div>

            <!-- Tab 3: 视觉效果 -->
            <div v-show="activeTab === 'style'" class="space-y-6">
              <!-- 标题字体 -->
              <div>
                <h4 class="text-sm font-semibold text-gray-900 mb-3 flex items-center gap-2">
                  <Sparkles class="h-4 w-4 text-indigo-600" />
                  标题字体
                </h4>
                <div class="grid grid-cols-7 gap-2 mb-4">
                  <button
                    v-for="font in fontOptions"
                    :key="font.value"
                    type="button"
                    @click="formData.titleFontFamily = font.value"
                    :class="[
                      'p-2 rounded-lg border-2 transition-all text-center',
                      formData.titleFontFamily === font.value
                        ? 'border-indigo-500 bg-indigo-50'
                        : 'border-gray-200 hover:border-gray-300'
                    ]"
                  >
                    <span
                      :class="[
                        'block text-base mb-0.5',
                        formData.titleFontFamily === font.value ? 'text-indigo-600' : 'text-gray-600'
                      ]"
                      :style="{ fontFamily: font.fontFamily }"
                    >
                      Aa
                    </span>
                    <span
                      :class="[
                        'text-xs',
                        formData.titleFontFamily === font.value ? 'text-indigo-600' : 'text-gray-500'
                      ]"
                    >
                      {{ font.label }}
                    </span>
                  </button>
                </div>

                <!-- 字体大小和粗细 -->
                <div class="grid grid-cols-2 gap-4">
                  <div>
                    <div class="flex items-center justify-between mb-2">
                      <label class="text-sm text-gray-700">字体大小</label>
                      <span class="text-sm text-gray-500">{{ formData.titleFontSize }}px</span>
                    </div>
                    <input
                      type="range"
                      v-model.number="formData.titleFontSize"
                      min="24"
                      max="72"
                      step="2"
                      class="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-indigo-600"
                    />
                  </div>
                  <div>
                    <label class="block text-sm text-gray-700 mb-2">字体粗细</label>
                    <div class="grid grid-cols-5 gap-1">
                      <button
                        v-for="weight in fontWeightOptions"
                        :key="weight.value"
                        type="button"
                        @click="formData.titleFontWeight = weight.value"
                        :class="[
                          'py-1.5 rounded text-xs transition-all',
                          formData.titleFontWeight === weight.value
                            ? 'bg-indigo-600 text-white'
                            : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                        ]"
                      >
                        {{ weight.label }}
                      </button>
                    </div>
                  </div>
                </div>

                <!-- 字体效果开关 -->
                <div class="flex items-center gap-6 mt-4 pt-4 border-t border-gray-100">
                  <label class="flex items-center gap-2 cursor-pointer">
                    <input
                      type="checkbox"
                      v-model="formData.titleTextShadow"
                      class="w-4 h-4 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500"
                    />
                    <span class="text-sm text-gray-700">文字阴影</span>
                  </label>
                  <label class="flex items-center gap-2 cursor-pointer">
                    <input
                      type="checkbox"
                      v-model="formData.titleGradient"
                      class="w-4 h-4 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500"
                    />
                    <span class="text-sm text-gray-700">渐变色效果</span>
                  </label>
                </div>

                <!-- 渐变颜色选择 -->
                <div v-if="formData.titleGradient" class="grid grid-cols-2 gap-4 mt-4 p-4 bg-gray-50 rounded-lg">
                  <div>
                    <label class="block text-xs font-medium text-gray-600 mb-1.5">起始颜色</label>
                    <div class="flex items-center gap-2">
                      <input
                        type="color"
                        v-model="formData.titleGradientFrom"
                        class="w-8 h-8 rounded border border-gray-300 cursor-pointer"
                      />
                      <input
                        type="text"
                        v-model="formData.titleGradientFrom"
                        class="flex-1 h-8 px-2 text-sm border border-gray-300 rounded"
                      />
                    </div>
                  </div>
                  <div>
                    <label class="block text-xs font-medium text-gray-600 mb-1.5">结束颜色</label>
                    <div class="flex items-center gap-2">
                      <input
                        type="color"
                        v-model="formData.titleGradientTo"
                        class="w-8 h-8 rounded border border-gray-300 cursor-pointer"
                      />
                      <input
                        type="text"
                        v-model="formData.titleGradientTo"
                        class="flex-1 h-8 px-2 text-sm border border-gray-300 rounded"
                      />
                    </div>
                  </div>
                  <div class="col-span-2">
                    <div
                      class="h-8 rounded flex items-center justify-center text-base font-bold"
                      :style="{
                        background: `linear-gradient(135deg, ${formData.titleGradientFrom}, ${formData.titleGradientTo})`,
                        WebkitBackgroundClip: 'text',
                        WebkitTextFillColor: 'transparent',
                        backgroundClip: 'text'
                      }"
                    >
                      标题渐变预览
                    </div>
                  </div>
                </div>
              </div>

              <!-- 表单样式 -->
              <div class="border-t border-gray-100 pt-6">
                <h4 class="text-sm font-semibold text-gray-900 mb-3 flex items-center gap-2">
                  <Hexagon class="h-4 w-4 text-indigo-600" />
                  表单样式
                </h4>
                <div class="grid grid-cols-3 gap-4">
                  <button
                    v-for="style in formStyleOptions"
                    :key="style.value"
                    type="button"
                    @click="formData.formStyle = style.value"
                    :class="[
                      'flex flex-col items-center gap-2 p-4 rounded-xl border-2 transition-all',
                      formData.formStyle === style.value
                        ? 'border-indigo-500 bg-indigo-50'
                        : 'border-gray-200 hover:border-gray-300'
                    ]"
                  >
                    <div
                      :class="[
                        'w-10 h-14 rounded-lg flex items-center justify-center',
                        style.value === 'classic' ? 'bg-white border border-gray-300 shadow-sm' : '',
                        style.value === 'modern' ? 'bg-white/80 backdrop-blur border border-white/20 shadow-lg' : '',
                        style.value === 'minimal' ? 'bg-transparent border border-dashed border-gray-300' : ''
                      ]"
                    >
                      <Square
                        :class="[
                          'h-5 w-5',
                          formData.formStyle === style.value ? 'text-indigo-500' : 'text-gray-400'
                        ]"
                      />
                    </div>
                    <div class="text-center">
                      <span
                        :class="[
                          'block text-sm font-medium',
                          formData.formStyle === style.value ? 'text-indigo-600' : 'text-gray-600'
                        ]"
                      >
                        {{ style.label }}
                      </span>
                      <span
                        :class="[
                          'text-xs',
                          formData.formStyle === style.value ? 'text-indigo-500' : 'text-gray-400'
                        ]"
                      >
                        {{ style.desc }}
                      </span>
                    </div>
                  </button>
                </div>
              </div>

              <!-- 装饰图片 -->
              <div class="border-t border-gray-100 pt-6">
                <DecorationImageManager v-model="formData.decorationImages" />
              </div>

              <!-- 自定义字体 -->
              <div class="border-t border-gray-100 pt-6">
                <FontUploader
                  v-model="formData.customFonts"
                  v-model:selectedFontId="formData.titleCustomFontId"
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧预览 (2列宽) -->
      <div class="lg:col-span-2 lg:sticky lg:top-6 h-fit">
        <LoginPreview :config="previewConfig" @update:textPosition="onTextPositionChange" />

        <!-- 操作提示 -->
        <div class="mt-4 bg-amber-50 rounded-xl p-4">
          <div class="flex gap-3">
            <AlertCircle class="h-5 w-5 text-amber-600 flex-shrink-0 mt-0.5" />
            <div class="text-sm text-amber-700">
              <p class="font-medium mb-1">使用提示</p>
              <ul class="list-disc list-inside space-y-0.5 text-amber-600 text-xs">
                <li>图片模式支持静态图和 GIF 动图</li>
                <li>视频模式将自动循环播放且静音</li>
                <li>标语中使用 **关键词** 可高亮</li>
                <li>保存后刷新登录页即可看到效果</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, markRaw } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Palette,
  Save,
  RotateCcw,
  Image,
  Type,
  AlertCircle,
  Loader2,
  Film,
  Layers,
  Sliders,
  AlignStartVertical,
  AlignCenterVertical,
  AlignEndVertical,
  Maximize,
  SplitSquareHorizontal,
  Layout,
  Eye,
  EyeOff,
  Copyright,
  Sparkles,
  Square,
  Hexagon,
  Circle,
  LayoutGrid,
  Brush
} from 'lucide-vue-next'
import { http } from '@/utils/request'
import { useConfigStore } from '@/stores/config'
import ImageCropper from '@/components/common/ImageCropper.vue'
import LoginPreview from '@/components/settings/LoginPreview.vue'
import DecorationImageManager from '@/components/settings/DecorationImageManager.vue'
import BackgroundCarouselManager from '@/components/settings/BackgroundCarouselManager.vue'
import FontUploader from '@/components/settings/FontUploader.vue'
import type { BackgroundMode, TextPosition, DisplayMode, LoginFeature, LoginCustomizationConfig, TitleFontFamily, TitleFontWeight, FormStyle, DecorationImage, BackgroundCarouselConfig, CustomFont } from '@/types/loginCustomization'
import { defaultLoginConfig, fontOptions, fontWeightOptions, formStyleOptions, getFontFamily, getFontWeight } from '@/types/loginCustomization'

const configStore = useConfigStore()

// Tab 配置
const tabs = [
  { id: 'layout', label: '布局与背景', icon: markRaw(LayoutGrid) },
  { id: 'content', label: '品牌内容', icon: markRaw(Type) },
  { id: 'style', label: '视觉效果', icon: markRaw(Brush) }
]
const activeTab = ref('layout')

// 显示模式选项
const displayModes = [
  { value: 'halfScreen' as DisplayMode, label: '半屏模式', icon: markRaw(SplitSquareHorizontal), desc: '背景仅左侧显示' },
  { value: 'fullScreen' as DisplayMode, label: '全屏模式', icon: markRaw(Maximize), desc: '背景铺满整个页面' }
]

// 背景模式选项
const backgroundModes = [
  { value: 'gradient' as BackgroundMode, label: '渐变色', icon: markRaw(Layers) },
  { value: 'image' as BackgroundMode, label: '图片/GIF', icon: markRaw(Image) },
  { value: 'video' as BackgroundMode, label: '视频', icon: markRaw(Film) }
]

// 文字位置选项
const textPositions = [
  { value: 'top' as TextPosition, label: '顶部', icon: markRaw(AlignStartVertical) },
  { value: 'center' as TextPosition, label: '居中', icon: markRaw(AlignCenterVertical) },
  { value: 'bottom' as TextPosition, label: '底部', icon: markRaw(AlignEndVertical) }
]

// 表单数据
const formData = reactive({
  displayMode: 'halfScreen' as DisplayMode,
  backgroundMode: 'gradient' as BackgroundMode,
  backgroundImage: '',
  backgroundVideo: '',
  title: '',
  subtitle: '专业**智能**教育管理平台',
  features: [
    { icon: 'users', text: '学生信息一站式管理' },
    { icon: 'chart', text: '量化考核智能分析' },
    { icon: 'building', text: '宿舍资源可视化管理' }
  ] as LoginFeature[],
  showLogo: true,
  gradientFrom: '#2563eb',
  gradientVia: '#1d4ed8',
  gradientTo: '#4338ca',
  textPosition: 'center' as TextPosition,
  textPositionX: 50,
  textPositionY: 50,
  formBgOpacity: 100,
  overlayOpacity: 40,
  // 新增：版权与显示控制
  copyrightText: '',
  showTitle: true,
  showSubtitle: true,
  showCopyright: true,
  // 新增：标题字体设置
  titleFontFamily: 'default' as TitleFontFamily,
  titleFontSize: 48,
  titleFontWeight: 'bold' as TitleFontWeight,
  titleTextShadow: true,
  titleGradient: false,
  titleGradientFrom: '#ffffff',
  titleGradientTo: '#60a5fa',
  // 新增：表单样式
  formStyle: 'modern' as FormStyle,
  // 新增：装饰图片
  decorationImages: [] as DecorationImage[],
  // 新增：背景轮播
  backgroundCarousel: {
    enabled: false,
    items: [],
    interval: 5,
    transition: 'fade'
  } as BackgroundCarouselConfig,
  // 新增：自定义字体
  customFonts: [] as CustomFont[],
  titleCustomFontId: ''
})

// 保存状态
const saving = ref(false)

// 计算当前显示模式的推荐裁切比例
const cropAspectRatio = computed(() => {
  // 全屏模式使用 16:9，半屏模式使用 3:4
  return formData.displayMode === 'fullScreen' ? 16 / 9 : 3 / 4
})

// 预览配置
const previewConfig = computed<LoginCustomizationConfig>(() => ({
  displayMode: formData.displayMode,
  backgroundMode: formData.backgroundMode,
  backgroundImage: formData.backgroundImage,
  backgroundVideo: formData.backgroundVideo,
  title: formData.title || configStore.systemName,
  subtitle: formData.subtitle,
  features: formData.features,
  showLogo: formData.showLogo,
  gradientFrom: formData.gradientFrom,
  gradientVia: formData.gradientVia,
  gradientTo: formData.gradientTo,
  textPosition: formData.textPosition,
  textPositionX: formData.textPositionX,
  textPositionY: formData.textPositionY,
  formBgOpacity: formData.formBgOpacity,
  overlayOpacity: formData.overlayOpacity,
  // 新增字段
  copyrightText: formData.copyrightText,
  showTitle: formData.showTitle,
  showSubtitle: formData.showSubtitle,
  showCopyright: formData.showCopyright,
  titleFontFamily: formData.titleFontFamily,
  titleFontSize: formData.titleFontSize,
  titleFontWeight: formData.titleFontWeight,
  titleTextShadow: formData.titleTextShadow,
  titleGradient: formData.titleGradient,
  titleGradientFrom: formData.titleGradientFrom,
  titleGradientTo: formData.titleGradientTo,
  formStyle: formData.formStyle,
  // 新增字段
  decorationImages: formData.decorationImages,
  backgroundCarousel: formData.backgroundCarousel,
  customFonts: formData.customFonts,
  titleCustomFontId: formData.titleCustomFontId
}))

// 处理文字位置拖拽
const onTextPositionChange = (x: number, y: number) => {
  formData.textPositionX = x
  formData.textPositionY = y
}

// 加载配置
const loadConfig = async () => {
  await configStore.refreshConfig()
  // 从store同步配置
  formData.displayMode = configStore.loginDisplayMode
  formData.backgroundMode = configStore.loginBackgroundMode
  formData.backgroundImage = configStore.loginBackgroundImage
  formData.backgroundVideo = configStore.loginBackgroundVideo
  formData.title = configStore.loginTitle
  formData.subtitle = configStore.loginSubtitle
  formData.features = [...configStore.loginFeatures]
  formData.showLogo = configStore.loginShowLogo
  formData.gradientFrom = configStore.loginGradientFrom
  formData.gradientVia = configStore.loginGradientVia
  formData.gradientTo = configStore.loginGradientTo
  formData.textPosition = configStore.loginTextPosition
  formData.textPositionX = configStore.loginTextPositionX
  formData.textPositionY = configStore.loginTextPositionY
  formData.formBgOpacity = configStore.loginFormBgOpacity
  formData.overlayOpacity = configStore.loginOverlayOpacity
  // 新增字段
  formData.copyrightText = configStore.loginCopyrightText
  formData.showTitle = configStore.loginShowTitle
  formData.showSubtitle = configStore.loginShowSubtitle
  formData.showCopyright = configStore.loginShowCopyright
  formData.titleFontFamily = configStore.loginTitleFontFamily
  formData.titleFontSize = configStore.loginTitleFontSize
  formData.titleFontWeight = configStore.loginTitleFontWeight
  formData.titleTextShadow = configStore.loginTitleTextShadow
  formData.titleGradient = configStore.loginTitleGradient
  formData.titleGradientFrom = configStore.loginTitleGradientFrom
  formData.titleGradientTo = configStore.loginTitleGradientTo
  formData.formStyle = configStore.loginFormStyle
  // 新增字段
  formData.decorationImages = [...configStore.loginDecorationImages]
  formData.backgroundCarousel = {
    enabled: configStore.loginCarouselEnabled,
    items: [...configStore.loginCarouselItems],
    interval: configStore.loginCarouselInterval,
    transition: configStore.loginCarouselTransition
  }
  formData.customFonts = [...configStore.loginCustomFonts]
  formData.titleCustomFontId = configStore.loginTitleCustomFontId
}

// 保存配置
const saveConfig = async () => {
  saving.value = true
  try {
    const configs: Record<string, string> = {
      'ui.login.display_mode': formData.displayMode,
      'ui.login.background_mode': formData.backgroundMode,
      'ui.login.background_image': formData.backgroundImage,
      'ui.login.background_video': formData.backgroundVideo,
      'ui.login.title': formData.title,
      'ui.login.subtitle': formData.subtitle,
      'ui.login.features': JSON.stringify(formData.features),
      'ui.login.show_logo': String(formData.showLogo),
      'ui.login.gradient_from': formData.gradientFrom,
      'ui.login.gradient_via': formData.gradientVia,
      'ui.login.gradient_to': formData.gradientTo,
      'ui.login.text_position': formData.textPosition,
      'ui.login.text_position_x': String(formData.textPositionX),
      'ui.login.text_position_y': String(formData.textPositionY),
      'ui.login.form_bg_opacity': String(formData.formBgOpacity),
      'ui.login.overlay_opacity': String(formData.overlayOpacity),
      // 新增字段
      'ui.login.copyright_text': formData.copyrightText,
      'ui.login.show_title': String(formData.showTitle),
      'ui.login.show_subtitle': String(formData.showSubtitle),
      'ui.login.show_copyright': String(formData.showCopyright),
      'ui.login.title_font_family': formData.titleFontFamily,
      'ui.login.title_font_size': String(formData.titleFontSize),
      'ui.login.title_font_weight': formData.titleFontWeight,
      'ui.login.title_text_shadow': String(formData.titleTextShadow),
      'ui.login.title_gradient': String(formData.titleGradient),
      'ui.login.title_gradient_from': formData.titleGradientFrom,
      'ui.login.title_gradient_to': formData.titleGradientTo,
      'ui.login.form_style': formData.formStyle,
      // 新增配置项
      'ui.login.decoration_images': JSON.stringify(formData.decorationImages),
      'ui.login.carousel_enabled': String(formData.backgroundCarousel.enabled),
      'ui.login.carousel_items': JSON.stringify(formData.backgroundCarousel.items),
      'ui.login.carousel_interval': String(formData.backgroundCarousel.interval),
      'ui.login.carousel_transition': formData.backgroundCarousel.transition,
      'ui.login.custom_fonts': JSON.stringify(formData.customFonts),
      'ui.login.title_custom_font_id': formData.titleCustomFontId
    }

    await http.put('/system/configs/batch', configs)
    await configStore.refreshConfig()
    ElMessage.success('保存成功')
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 恢复默认
const resetToDefault = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要恢复默认配置吗？当前配置将被覆盖。',
      '恢复默认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    Object.assign(formData, {
      displayMode: defaultLoginConfig.displayMode,
      backgroundMode: defaultLoginConfig.backgroundMode,
      backgroundImage: defaultLoginConfig.backgroundImage,
      backgroundVideo: defaultLoginConfig.backgroundVideo,
      title: defaultLoginConfig.title,
      subtitle: defaultLoginConfig.subtitle,
      features: [...defaultLoginConfig.features],
      showLogo: defaultLoginConfig.showLogo,
      gradientFrom: defaultLoginConfig.gradientFrom,
      gradientVia: defaultLoginConfig.gradientVia,
      gradientTo: defaultLoginConfig.gradientTo,
      textPosition: defaultLoginConfig.textPosition,
      textPositionX: defaultLoginConfig.textPositionX,
      textPositionY: defaultLoginConfig.textPositionY,
      formBgOpacity: defaultLoginConfig.formBgOpacity,
      overlayOpacity: defaultLoginConfig.overlayOpacity,
      // 新增字段
      copyrightText: defaultLoginConfig.copyrightText,
      showTitle: defaultLoginConfig.showTitle,
      showSubtitle: defaultLoginConfig.showSubtitle,
      showCopyright: defaultLoginConfig.showCopyright,
      titleFontFamily: defaultLoginConfig.titleFontFamily,
      titleFontSize: defaultLoginConfig.titleFontSize,
      titleFontWeight: defaultLoginConfig.titleFontWeight,
      titleTextShadow: defaultLoginConfig.titleTextShadow,
      titleGradient: defaultLoginConfig.titleGradient,
      titleGradientFrom: defaultLoginConfig.titleGradientFrom,
      titleGradientTo: defaultLoginConfig.titleGradientTo,
      formStyle: defaultLoginConfig.formStyle,
      // 新增字段
      decorationImages: [...defaultLoginConfig.decorationImages],
      backgroundCarousel: { ...defaultLoginConfig.backgroundCarousel },
      customFonts: [...defaultLoginConfig.customFonts],
      titleCustomFontId: defaultLoginConfig.titleCustomFontId
    })

    ElMessage.success('已恢复默认配置，请点击保存以应用')
  } catch {
    // 用户取消
  }
}

// 媒体上传完成
const onMediaUploaded = (data: { url: string; type: 'image' | 'gif' | 'video' }) => {
  if (data.type === 'video') {
    formData.backgroundVideo = data.url
  } else {
    formData.backgroundImage = data.url
  }
}

onMounted(() => {
  loadConfig()
})
</script>
