import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// EP 深色變數表，選擇器為 html.dark；由 composables/useEditorTheme.ts 在編輯器內掛上該 class，
// 未掛時完全不生效，故 Login / WorkflowList 維持 EP 淺色預設。
import 'element-plus/theme-chalk/dark/css-vars.css'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/minimap/dist/style.css'
// 須排在 EP 樣式之後，才能覆寫其預設值
import './styles/element-plus-wf.css'
import App from './App.vue'
import router from './router'

createApp(App).use(createPinia()).use(router).use(ElementPlus).mount('#app')
