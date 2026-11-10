<template>
  <div class="ai-advisor">
    <div class="ai-header">
      <div>
        <h2>AI领养顾问</h2>
        <p>基于当前宠物档案和已爬取的品种知识库回答</p>
      </div>
      <span class="ai-badge">DeepSeek</span>
    </div>

    <div class="quick-actions">
      <button v-for="item in quickQuestions" :key="item" type="button" @click="askQuick(item)">
        {{ item }}
      </button>
    </div>

    <div class="chat-window" ref="chatWindowRef">
      <div v-for="(message, index) in messages" :key="index" class="chat-row" :class="message.role">
        <div class="bubble">
          <p>{{ message.content }}</p>
          <div v-if="message.sources?.length" class="source-list">
            <span>数据来源</span>
            <a v-for="source in message.sources" :key="source" :href="sourceUrl(source)" target="_blank">
              {{ sourceName(source) }}
            </a>
          </div>
        </div>
      </div>
    </div>

    <div class="chat-input">
      <el-input
        v-model="input"
        type="textarea"
        :rows="2"
        resize="none"
        maxlength="180"
        show-word-limit
        placeholder="例如：它适合第一次养宠的人吗？"
        @keydown.enter.prevent="send"
      />
      <el-button type="primary" :loading="loading" @click="send">发送</el-button>
    </div>
  </div>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const props = defineProps({
  pet: {
    type: Object,
    required: true
  }
})

const quickQuestions = ['适合公寓饲养吗', '领养前要准备什么', '健康护理建议', '和儿童相处要注意什么']
const messages = ref([
  {
    role: 'assistant',
    content: '你好，我可以结合当前宠物档案、真实品种资料和系统健康记录，帮你判断领养准备、护理重点和风险提醒。'
  }
])
const input = ref('')
const loading = ref(false)
const chatWindowRef = ref(null)

watch(() => props.pet?.id, () => {
  messages.value = [
    {
      role: 'assistant',
      content: `已切换到 ${props.pet?.name || '当前宠物'} 的档案，可以继续咨询领养匹配和健康护理问题。`
    }
  ]
})

const askQuick = (question) => {
  input.value = question
  send()
}

const send = async () => {
  const text = input.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text })
  input.value = ''
  loading.value = true
  scrollToBottom()

  try {
    const res = await request.post('/ai/chat', {
      petId: props.pet.id,
      message: text
    })
    if (res.code === 200) {
      messages.value.push({
        role: 'assistant',
        content: res.data.answer,
        sources: res.data.sources || []
      })
    } else {
      ElMessage.error(res.message || 'AI服务暂时不可用')
    }
  } catch (error) {
    ElMessage.error('AI服务暂时不可用')
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (chatWindowRef.value) {
      chatWindowRef.value.scrollTop = chatWindowRef.value.scrollHeight
    }
  })
}

const sourceName = (source) => source.split('：')[0]
const sourceUrl = (source) => source.split('：').slice(1).join('：')
</script>

<style scoped>
.ai-advisor {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ai-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.ai-header h2 {
  margin: 0;
  font-size: 20px;
  color: #1a1a2e;
}

.ai-header p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #667085;
}

.ai-badge {
  padding: 6px 12px;
  border-radius: 999px;
  background: #eef2ff;
  color: #4f46e5;
  font-size: 12px;
  font-weight: 700;
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-actions button {
  border: 1px solid #d9e0ff;
  background: #fff;
  color: #4f46e5;
  border-radius: 999px;
  padding: 7px 12px;
  cursor: pointer;
  font-size: 13px;
}

.quick-actions button:hover {
  background: #f5f7ff;
}

.chat-window {
  max-height: 360px;
  overflow-y: auto;
  padding: 14px;
  background: rgba(248, 250, 252, 0.9);
  border-radius: 14px;
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.chat-row {
  display: flex;
  margin-bottom: 12px;
}

.chat-row.user {
  justify-content: flex-end;
}

.bubble {
  max-width: 78%;
  padding: 12px 14px;
  border-radius: 14px;
  background: #fff;
  color: #1f2937;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.06);
}

.chat-row.user .bubble {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.bubble p {
  margin: 0;
  line-height: 1.7;
  white-space: pre-wrap;
  font-size: 14px;
}

.source-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #edf0f7;
}

.source-list span,
.source-list a {
  font-size: 12px;
}

.source-list span {
  color: #64748b;
}

.source-list a {
  color: #4f46e5;
  text-decoration: none;
}

.chat-input {
  display: grid;
  grid-template-columns: 1fr 88px;
  gap: 10px;
  align-items: stretch;
}

.chat-input :deep(.el-button) {
  height: auto;
}

@media (max-width: 768px) {
  .chat-input {
    grid-template-columns: 1fr;
  }

  .bubble {
    max-width: 92%;
  }
}
</style>
