import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import './styles.css';
import { initializeTheme, installThemeSync } from './theme';

// Author: huangbingrui.awa
initializeTheme();
installThemeSync();
createApp(App).use(router).mount('#app');
