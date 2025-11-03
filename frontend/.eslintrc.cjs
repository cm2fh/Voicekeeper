// ESLint configuration for Vue 3 + TypeScript project

module.exports = {
  root: true,
  env: {
    node: true,
    es2022: true
  },
  extends: [
    'eslint:recommended',
    'plugin:vue/vue3-essential',
    'plugin:@typescript-eslint/recommended'
  ],
  parser: 'vue-eslint-parser',
  parserOptions: {
    parser: '@typescript-eslint/parser',
    ecmaVersion: 'latest',
    sourceType: 'module',
    ecmaFeatures: {
      jsx: true
    },
    extraFileExtensions: ['.vue']
  },
  plugins: [
    'vue',
    '@typescript-eslint'
  ],
  rules: {
    // Add your custom rules here
  },
  ignorePatterns: [
    'dist/**',
    'node_modules/**'
  ]
};
