<template>
  <footer class="site-footer" :class="{ compact }">
    <div v-if="!compact" class="footer-inner">
      <div class="footer-brand"><span class="footer-mark"><PawMark /></span><strong>陪伴，从一次相遇开始。</strong><p>为它们找到家，也让每一天的照护更安心。</p></div>
      <div class="footer-links"><strong>发现伙伴</strong><router-link to="/pets">寻找宠物</router-link><router-link to="/announcements">公告资讯</router-link></div>
      <div class="footer-links"><strong>日常照护</strong><router-link to="/health-radar">健康雷达</router-link><router-link to="/care-plan">照护计划</router-link></div>
      <div class="footer-links"><strong>我的空间</strong><router-link to="/profile">个人中心</router-link><router-link to="/adoptions">领养进度</router-link></div>
    </div>
    <div class="footer-bottom"><span>© {{ year }} 宠物领养及健康管理平台</span><span class="footer-message">用心连接，让爱延续。</span></div>
  </footer>
</template>
<script setup>
import PawMark from './PawMark.vue'
defineProps({ compact: Boolean })
const year = new Intl.DateTimeFormat('zh-CN', { year: 'numeric', timeZone: 'Asia/Shanghai' }).format(new Date()).replace('年', '')
</script>
<style scoped>
.site-footer { margin-top: 56px; background: #f0f3eb; border-top: 1px solid #e3e9dc; color: #7e8d78; }
.footer-inner { display: grid; grid-template-columns: 2fr repeat(3, 1fr); max-width: 1200px; gap: 40px; padding: 42px 28px; margin: 0 auto; }
.footer-mark { width: 28px; height: 28px; display: block; color: #719469; margin-bottom: 12px; }
.footer-brand > strong { font-size: 16px; font-weight: 500; color: #4c6547; }
.footer-brand p { font-size: 12px; margin-top: 10px; line-height: 1.8; }
.footer-links { display: flex; flex-direction: column; gap: 12px; padding-top: 6px; }
.footer-links strong { font-size: 12px; color: #58704f; font-weight: 600; margin-bottom: 3px; }
.footer-links a { font-size: 12px; text-decoration: none; color: #7e8d78; }
.footer-links a:hover { color: #28745b; }
.footer-bottom { max-width: 1144px; display: flex; align-items: center; justify-content: space-between; gap: 10px; margin: 0 auto; border-top: 1px solid #dfe7d8; padding: 20px 0 22px; font-size: 11px; }
.compact { margin-top: 0; background: #f7f8f4; border-top: none; }
.compact .footer-bottom { justify-content: center; border-top: none; padding: 20px 24px 28px; font-size: 10px; }
.compact .footer-message { display: none; }
@media (max-width: 900px) { .footer-bottom { margin: 0 28px; } .footer-inner { gap: 26px; } }
@media (max-width: 600px) { .site-footer { margin-top: 32px; } .footer-inner { grid-template-columns: repeat(3, 1fr); gap: 28px 20px; padding: 28px 22px; } .footer-brand { grid-column: 1 / -1; } .footer-bottom { margin: 0 22px; font-size: 10px; flex-wrap: wrap; line-height: 1.7; } .compact { margin-top: 0; } }
</style>
