import { defineConfig, globalIgnores } from 'eslint/config';
import angularEslintEslintPlugin from '@angular-eslint/eslint-plugin';
import typescriptEslint from '@typescript-eslint/eslint-plugin';
import unusedImports from 'eslint-plugin-unused-imports';
import tsParser from '@typescript-eslint/parser';
import parser from '@angular-eslint/template-parser';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import js from '@eslint/js';
import { FlatCompat } from '@eslint/eslintrc';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const compat = new FlatCompat({
  baseDirectory: __dirname,
  recommendedConfig: js.configs.recommended,
  allConfig: js.configs.all,
});

export default defineConfig([
  globalIgnores(['projects/**/*', 'node_modules/', 'dist/', 'node/', 'postcss.config.js', 'webpack/']),
  {
    plugins: {
      '@angular-eslint': angularEslintEslintPlugin,
      '@typescript-eslint': typescriptEslint,
      'unused-imports': unusedImports,
    },

    languageOptions: {
      parser: tsParser,
    },
  },
  {
    files: ['**/*.ts'],

    extends: compat.extends('plugin:@angular-eslint/recommended', 'plugin:@angular-eslint/template/process-inline-templates'),

    languageOptions: {
      ecmaVersion: 5,
      sourceType: 'script',

      parserOptions: {
        project: ['tsconfig.json'],
        createDefaultProgram: true,
      },
    },

    rules: {
      '@angular-eslint/directive-selector': [
        'error',
        {
          type: 'attribute',
          prefix: 'vm',
          style: 'camelCase',
        },
      ],

      '@angular-eslint/component-selector': [
        'error',
        {
          type: 'element',
          prefix: 'vm',
          style: 'kebab-case',
        },
      ],
      '@angular-eslint/prefer-inject': 'off',
      '@angular-eslint/prefer-standalone': ['off'],
      '@angular-eslint/relative-url-prefix': 'error',

      '@typescript-eslint/explicit-function-return-type': [
        'error',
        {
          allowExpressions: true,
        },
      ],

      '@typescript-eslint/no-shadow': 'error',
      '@typescript-eslint/prefer-optional-chain': 'error',
      curly: 'error',

      eqeqeq: [
        'error',
        'always',
        {
          null: 'ignore',
        },
      ],

      'guard-for-in': 'error',
      'no-bitwise': 'error',
      'no-caller': 'error',
      'no-eval': 'error',
      'no-labels': 'error',
      'no-new-wrappers': 'error',
      radix: 'error',
      'spaced-comment': ['warn', 'always'],
      'no-duplicate-imports': 'error',
      'unused-imports/no-unused-imports': 'error',
      'unused-imports/no-unused-vars': 'error',
      'no-unused-expressions': 'error',
    },
  },
  {
    files: ['**/*.html'],
    extends: compat.extends('plugin:@angular-eslint/template/recommended'),

    languageOptions: {
      parser: parser,
    },

    rules: {},
  },
]);
