import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import { initializeTheme, installThemeSync } from './theme';
import './styles.css';

// Author: huangbingrui.awa
initializeTheme();
installThemeSync();
createApp(App).use(router).mount('#app');
