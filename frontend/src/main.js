import { createApp } from 'vue';
import { markRaw } from 'vue';
import ElementPlus from 'element-plus';
import zhCn from 'element-plus/es/locale/lang/zh-cn';
import 'element-plus/dist/index.css';
import 'vue-echarts/style.css';
import router from './route';
import App from './App.vue';
import store from './utils/store.js';

import * as ElementPlusIconsVue from '@element-plus/icons-vue';

const app = createApp(App);

app.use(ElementPlus, {
  locale: zhCn,
  size: 'default',
  zIndex: 2000
});
app.use(router);

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, markRaw(component));
}

app.config.globalProperties.$store = store;

app.mount('#app');