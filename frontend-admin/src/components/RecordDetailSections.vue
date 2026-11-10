<template>
  <div class="record-detail-sections">
    <section v-for="section in sections" :key="section.title" class="detail-section">
      <h3>{{ section.title }}<span v-if="section.description">{{ section.description }}</span></h3>
      <dl class="detail-fields">
        <div v-for="field in section.fields" :key="field.label" class="detail-field" :class="{ wide: field.wide }">
          <dt>{{ field.label }}</dt>
          <dd>
            <a v-if="field.link && safeLink(field.value)" :href="safeLink(field.value)" target="_blank" rel="noopener noreferrer">{{ field.value }}<span class="link-hint"> ↗</span></a>
            <span v-else :class="{ missing: isMissing(field.value) }">{{ isMissing(field.value) ? '未填写' : field.value }}</span>
          </dd>
        </div>
      </dl>
    </section>
  </div>
</template>

<script setup>
defineProps({ sections: { type: Array, default: () => [] } })
const isMissing = value => value === null || value === undefined || value === ''
const safeLink = value => {
  if (typeof value !== 'string' || !value.trim()) return ''
  try {
    const url = new URL(value, window.location.origin)
    return ['http:', 'https:'].includes(url.protocol) ? url.href : ''
  } catch {
    return ''
  }
}
</script>

<style scoped>
.record-detail-sections { display: grid; gap: 22px; }
.detail-section { border: 1px solid #e5eae2; border-radius: 14px; overflow: hidden; }
.detail-section h3 { display: flex; align-items: center; gap: 12px; margin: 0; padding: 15px 20px; color: #2c4537; background: #f6f8f3; font-size: 14px; font-weight: 650; border-bottom: 1px solid #e9ede6; }
.detail-section h3::before { content: ''; width: 3px; height: 14px; border-radius: 4px; background: #71927d; }
.detail-section h3 span { margin-left: auto; color: #849084; font-size: 11px; font-weight: 400; }
.detail-fields { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 19px 24px; margin: 0; padding: 20px; }
.detail-field { min-width: 0; }
.detail-field.wide { grid-column: 1 / -1; }
.detail-field dt { margin-bottom: 7px; font-size: 12px; color: #849084; }
.detail-field dd { margin: 0; color: #314b3c; font-size: 13px; line-height: 1.75; white-space: pre-wrap; overflow-wrap: anywhere; font-variant-numeric: tabular-nums; }
.detail-field a { color: #28745b; text-decoration-color: #c6d8cb; text-underline-offset: 3px; }
.detail-field a:hover { text-decoration-color: #28745b; }
.detail-field .missing { color: #a0aaa0; }
.link-hint { white-space: nowrap; }
@media (max-width: 560px) { .detail-fields { grid-template-columns: 1fr; padding: 16px; } .detail-section h3 { padding: 14px 16px; } .detail-section h3 span { display: none; } }
</style>
