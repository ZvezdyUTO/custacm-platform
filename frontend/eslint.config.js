import js from '@eslint/js';
import vue from 'eslint-plugin-vue';
import tseslint from 'typescript-eslint';

const browserGlobals = {
  Blob: 'readonly',
  console: 'readonly',
  document: 'readonly',
  Event: 'readonly',
  File: 'readonly',
  HTMLImageElement: 'readonly',
  HTMLInputElement: 'readonly',
  HTMLSelectElement: 'readonly',
  navigator: 'readonly',
  setTimeout: 'readonly',
  URL: 'readonly',
  window: 'readonly',
};

export default [
  {
    ignores: ['dist', 'vite.config.ts.timestamp-*'],
  },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  ...vue.configs['flat/essential'],
  {
    files: ['**/*.vue'],
    languageOptions: {
      parserOptions: {
        parser: tseslint.parser,
      },
    },
  },
  {
    files: ['**/*.{ts,vue}'],
    languageOptions: {
      ecmaVersion: 2022,
      globals: browserGlobals,
      parserOptions: {
      },
      sourceType: 'module',
    },
    rules: {
      'vue/multi-word-component-names': 'off',
      '@typescript-eslint/no-unused-vars': [
        'error',
        {
          argsIgnorePattern: '^_',
          varsIgnorePattern: '^_',
        },
      ],
    },
  },
];
