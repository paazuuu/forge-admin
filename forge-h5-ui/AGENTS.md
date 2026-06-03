# Repository Guidelines

## Project Structure & Module Organization
This repository is a `uni-app` + Vue 3 application managed with `pnpm`. Main code lives in `src/`: `pages/` for route pages, `components/` for shared UI, `store/` for Pinia state, `api/` and `utils/http/` for requests, `composables/` for reusable logic, `directives/` for custom directives, and `styles/` plus `uni.scss` for global styling. Static assets live under `src/static/`. Build-time plugins and icon helpers are in `build/`, while generated output goes to `dist/`.

## Build, Test, and Development Commands
Install dependencies with `pnpm install`. Use `pnpm dev:h5` for the default H5 dev server, or a platform-specific target such as `pnpm dev:mp-weixin`. Create production bundles with `pnpm build:h5` or `pnpm build:mp-weixin`. Environment values are split across `.env`, `.env.development`, `.env.production`, and `.env.test`; verify proxy settings before local API work.

## Coding Style & Naming Conventions
Follow the existing Vue SFC structure: `<template>`, `<script>`, then `<style>`. Prefer the current JavaScript style used across `src/`: single quotes, semicolons only when helpful, and two-space indentation in Vue/JS files unless the surrounding file clearly differs. Use PascalCase for reusable components such as `AiCard.vue`, camelCase for composables and utilities such as `usePaging.js`, and keep store modules under `src/store/modules/`. Use the `@` alias for `src` imports.

## Testing Guidelines
There is no committed unit or E2E test runner configured in `package.json` yet. Until one is added, treat a successful target build as the minimum validation step: run the relevant `pnpm build:*` command for the platform you changed. If you add tests later, place them beside the feature or under a dedicated `tests/` directory and use `*.spec.js` or `*.test.js`.

## Commit & Pull Request Guidelines
Local Git history is not available in this checkout, so commit conventions cannot be derived here. Use short, imperative commit messages such as `feat: add paging composable guard` or `fix: correct H5 proxy path`. PRs should describe the affected platform (`h5`, `mp-weixin`, etc.), summarize config or env changes, link the related issue, and include screenshots for UI changes.

## Configuration Notes
Do not commit real secrets in `.env*` files. Review `vite.config.js` proxy targets and UnoCSS settings in `uno.config.js` when changing networking, icons, or shared styling behavior.
