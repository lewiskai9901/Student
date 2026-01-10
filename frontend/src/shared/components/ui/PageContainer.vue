<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <header v-if="$slots.header || title" class="page-header">
      <slot name="header">
        <div class="page-header__content">
          <div class="page-header__title-section">
            <h1 class="page-title">{{ title }}</h1>
            <p v-if="subtitle" class="page-subtitle">{{ subtitle }}</p>
          </div>
          <div v-if="$slots.actions" class="page-header__actions">
            <slot name="actions" />
          </div>
        </div>
      </slot>
    </header>

    <!-- 工具栏 -->
    <div v-if="$slots.toolbar" class="page-toolbar">
      <slot name="toolbar" />
    </div>

    <!-- 主要内容 -->
    <main class="page-content" :class="{ 'page-content--no-padding': noPadding }">
      <slot />
    </main>

    <!-- 页脚 -->
    <footer v-if="$slots.footer" class="page-footer">
      <slot name="footer" />
    </footer>
  </div>
</template>

<script setup lang="ts">
interface Props {
  title?: string
  subtitle?: string
  noPadding?: boolean
}

withDefaults(defineProps<Props>(), {
  noPadding: false
})
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  min-height: 100%;
  background: var(--color-gray-50);
}

.page-header {
  padding: 1.5rem 2rem;
  background: white;
  border-bottom: 1px solid var(--color-gray-200);
}

.page-header__content {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.page-header__title-section {
  flex: 1;
}

.page-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-gray-900);
}

.page-subtitle {
  margin: 0.25rem 0 0;
  font-size: 0.875rem;
  color: var(--color-gray-600);
}

.page-header__actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-shrink: 0;
}

.page-toolbar {
  padding: 1rem 2rem;
  background: white;
  border-bottom: 1px solid var(--color-gray-200);
}

.page-content {
  flex: 1;
  padding: 1.5rem 2rem;
}

.page-content--no-padding {
  padding: 0;
}

.page-footer {
  padding: 1rem 2rem;
  background: white;
  border-top: 1px solid var(--color-gray-200);
}

@media (max-width: 768px) {
  .page-header,
  .page-toolbar,
  .page-content,
  .page-footer {
    padding-left: 1rem;
    padding-right: 1rem;
  }

  .page-header__content {
    flex-direction: column;
    align-items: stretch;
  }

  .page-header__actions {
    margin-top: 1rem;
    justify-content: flex-start;
  }
}
</style>
