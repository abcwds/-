<template>
  <div class="auth-experience" :class="{ 'auth-experience--register': mode === 'register' }">
    <div class="auth-shell">
      <section class="auth-story" aria-label="关于宠物领养平台">
        <div class="auth-story-brand"><span class="auth-brand-symbol"><PawMark /></span><span>有爱相伴<small>A HOME, A NEW BEGINNING</small></span></div>
        <div class="auth-story-copy">
          <span class="auth-eyebrow"><i></i> 从相遇，到相伴</span>
          <h2>一个小小的决定，<br />一个温暖的新家。</h2>
          <p>给它一份安心的陪伴，<br />也让生活多一份柔软的期待。</p>
        </div>
        <svg class="auth-illustration" viewBox="0 0 400 278" fill="none" aria-hidden="true">
          <path d="M84 227V106a106 106 0 1 1 212 0v121" fill="#f7f8ee" />
          <path d="M98 226V106a92 92 0 1 1 184 0v120" fill="#d3e0cb" />
          <circle cx="238" cy="78" r="23" fill="#f6e6c5" />
          <path d="M99 159c36-27 67-29 99-14 34 16 52 8 85-4v86H99v-68Z" fill="#b5cbb1" />
          <path d="M99 193c43-30 86-23 126-8 22 8 40 5 58-2v44H99v-34Z" fill="#9bb89d" />
          <path d="M190 14v213M98 122h185" stroke="#f7f8ee" stroke-width="9" />
          <path d="M84 227V106a106 106 0 1 1 212 0v121" stroke="#90a789" stroke-width="3" />
          <path d="M66 229h249" stroke="#829b7b" stroke-width="6" stroke-linecap="round" />
          <path d="M77 235h227l-8 12H85l-8-12Z" fill="#d9c3a0" />
          <path d="M159 227c-19-7-31-23-22-38 5-9 13-6 14 1" stroke="#bf936b" stroke-width="10" stroke-linecap="round" />
          <path d="M155 228c-8-35-1-62 19-65 25-4 41 23 38 65" fill="#d5ad7e" />
          <path d="m151 147-1-34 24 14 20-15 11 34c4 20-8 33-25 33-18 0-32-12-29-32Z" fill="#e1bf92" />
          <path d="m157 122 2 16 10-7m22-8-10 9 16 7" fill="#b88c6c" />
          <path d="M162 148c3 3 6 3 9 0m15 0c3 3 6 3 9 0" stroke="#5a6250" stroke-width="2.5" stroke-linecap="round" />
          <path d="m175 156 4 3 4-3" fill="#aa7c64" />
          <path d="M179 159v3m0 0c-3 3-5 3-8 1m8-1c3 3 5 3 8 1" stroke="#aa7c64" stroke-width="1.5" stroke-linecap="round" />
          <path d="M159 157h-15m16 5-14 4m49-9h15m-16 5 14 4" stroke="#b99475" stroke-width="1.5" stroke-linecap="round" />
          <path d="M167 182v43m24-42v42" stroke="#bf936b" stroke-width="2.5" stroke-linecap="round" />
          <path d="M166 222h8m12 0h8" stroke="#f1d9b7" stroke-width="5" stroke-linecap="round" />
          <path d="M262 226v-37" stroke="#68866d" stroke-width="3" />
          <path d="M262 209c-19-2-27-15-24-28 18 1 28 13 24 28Zm0-12c21-1 29-15 26-29-18 2-29 15-26 29Z" fill="#7b9c7a" />
          <path d="M247 226h29l5-25h-39l5 25Z" fill="#cfa780" />
          <path d="M57 149v9m-4.5-4.5h9M327 97v11m-5.5-5.5h11" stroke="#9bb38e" stroke-width="2" stroke-linecap="round" />
          <path d="M313 184c-8-11-21 1 0 15 21-14 8-26 0-15Z" fill="#d6b78c" />
          <circle cx="62" cy="207" r="3" fill="#c3cfb5" />
          <path d="M105 264h173" stroke="#c9d2bf" stroke-width="2" stroke-linecap="round" />
        </svg>
        <div class="auth-story-foot"><span>用领养代替购买</span><i></i><span>让陪伴成为日常</span></div>
      </section>

      <section class="auth-content" :aria-labelledby="titleId">
        <div class="auth-content-kicker">{{ mode === 'register' ? 'A NEW FRIENDSHIP' : 'WELCOME BACK' }}</div>
        <header class="auth-heading">
          <h1 :id="titleId">{{ title }}</h1>
          <p>{{ description }}</p>
        </header>
        <slot />
      </section>
    </div>
  </div>
</template>

<script setup>
import PawMark from './PawMark.vue'
import '../styles/auth.css'

defineProps({
  mode: { type: String, default: 'login' },
  title: { type: String, required: true },
  description: { type: String, required: true },
  titleId: { type: String, default: 'auth-page-title' }
})
</script>
