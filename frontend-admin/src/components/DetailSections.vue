<template>
  <section v-for="section in sections" :key="section.title" class="detail-section">
    <h3>{{ section.title }}</h3>
    <dl class="detail-fields">
      <div v-for="field in section.fields" :key="field.label" :class="{ wide: field.wide }">
        <dt>{{ field.label }}</dt>
        <dd><a v-if="field.link && safeSourceUrl(field.value)" :href="safeSourceUrl(field.value)" target="_blank" rel="noopener noreferrer">{{ field.value }} ↗</a><template v-else>{{ present(field.value) }}</template></dd>
      </div>
    </dl>
  </section>
</template>

<script setup>
import { safeSourceUrl } from '../utils/navigation'
defineProps({ sections: { type: Array, default: () => [] } })
const present = value => value === null || value === undefined || value === '' ? '未填写' : value
</script>

<style scoped>
.detail-section + .detail-section { margin-top: 24px; }
h3 { display: flex; align-items: center; gap: 9px; color: #42624e; font-size: 13px; font-weight: 600; margin-bottom: 12px; }
h3::before { content: ''; width: 3px; height: 13px; background: #84a78b; border-radius: 2px; }
.detail-fields { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); border: 1px solid #e7ece4; border-radius: 12px; overflow: hidden; background: #fafbf8; }
.detail-fields > div { padding: 15px 18px; min-width: 0; border-bottom: 1px solid #e9ede7; }
.detail-fields > div.wide { grid-column: 1 / -1; }
dt { color: #8a978a; font-size: 11px; line-height: 1.6; margin-bottom: 6px; }
dd { color: #415b49; font-size: 13px; line-height: 1.9; overflow-wrap: anywhere; white-space: pre-wrap; }
dd a { color: #28745b; text-decoration-thickness: 1px; text-underline-offset: 4px; }
@media (max-width: 500px) { .detail-fields { grid-template-columns: 1fr; } .detail-fields > div { padding: 12px 15px; } }
</style>
