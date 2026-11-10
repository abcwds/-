<template>
  <el-select :model-value="modelValue" class="record-pet-select" filterable remote clearable :remote-method="searchPets" :loading="loading" :placeholder="placeholder" @visible-change="onVisible" @update:model-value="selectPet">
    <el-option v-for="pet in displayOptions" :key="pet.id" :label="`${pet.name || '未命名宠物'} · #${pet.id}`" :value="pet.id">
      <div class="pet-option"><strong>{{ pet.name || '未命名宠物' }}</strong><span>#{{ pet.id }} · {{ pet.breed || pet.type || '品种未填' }}</span></div>
    </el-option>
    <template #empty>
      <div class="selector-empty"><span>{{ loading ? '正在查找宠物…' : loadError ? '暂时无法加载宠物' : '没有匹配的宠物，可更换关键词' }}</span><el-button v-if="loadError" link type="primary" @click="searchPets(lastKeyword)">重新加载</el-button></div>
    </template>
  </el-select>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import request from '../utils/request'
const props = defineProps({ modelValue: { type: [Number, String], default: null }, petName: { type: String, default: '' }, placeholder: { type: String, default: '按宠物名称或品种搜索' } })
const emit = defineEmits(['update:modelValue', 'select'])
const options = ref([])
const selectedPet = ref(null)
const loading = ref(false)
const loadError = ref(false)
const lastKeyword = ref('')
let searchVersion = 0
let resolveVersion = 0
const displayOptions = computed(() => {
  const selected = selectedPet.value || (props.modelValue ? { id: props.modelValue, name: props.petName || '宠物' } : null)
  return selected && !options.value.some(pet => String(pet.id) === String(selected.id)) ? [selected, ...options.value] : options.value
})
async function searchPets(keyword = '') {
  const version = ++searchVersion
  lastKeyword.value = keyword
  loading.value = true
  loadError.value = false
  try {
    const res = await request.get('/admin/pets', { params: { page: 1, size: 30, keyword } })
    if (res.code !== 200) throw new Error(res.message || '加载失败')
    if (version === searchVersion) options.value = res.data?.records || []
  } catch {
    if (version === searchVersion) { options.value = []; loadError.value = true }
  } finally {
    if (version === searchVersion) loading.value = false
  }
}
const onVisible = visible => { if (visible && !options.value.length) searchPets('') }
const selectPet = value => {
  const pet = displayOptions.value.find(item => item.id === value) || null
  selectedPet.value = pet
  emit('update:modelValue', value || null)
  emit('select', pet)
}
watch(() => props.modelValue, async value => {
  const version = ++resolveVersion
  if (!value) {
    const hadSelection = Boolean(selectedPet.value)
    selectedPet.value = null
    if (hadSelection) emit('select', null)
    return
  }
  if (String(selectedPet.value?.id) === String(value)) return
  selectedPet.value = null
  emit('select', null)
  try {
    const res = await request.get(`/admin/pets/${value}`)
    if (version === resolveVersion && res.code === 200 && res.data) {
      selectedPet.value = res.data
      emit('select', res.data)
    }
  } catch {
    // ID 输入仍可使用；列表搜索失败会在选择器内提供重试。
  }
}, { immediate: true })
onBeforeUnmount(() => { searchVersion++; resolveVersion++ })
</script>

<style scoped>
.record-pet-select { width: 100%; }
.pet-option { display: flex; align-items: center; justify-content: space-between; gap: 18px; }
.pet-option strong { overflow: hidden; text-overflow: ellipsis; font-size: 13px; font-weight: 500; }
.pet-option span { flex-shrink: 0; color: #879287; font-size: 11px; }
.selector-empty { display: grid; justify-items: center; gap: 8px; padding: 18px; color: #7c8980; font-size: 12px; }
</style>
