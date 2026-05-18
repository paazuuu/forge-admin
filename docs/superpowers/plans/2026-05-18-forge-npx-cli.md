# Forge NPX CLI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a standalone `npx` CLI that interactively generates Forge customer projects, selects modules, and optionally initializes MySQL using the generated server's `scripts/db/init-db.sh`.

**Architecture:** Keep the publishable CLI in a separate repository, for example `forge-create-cli`. The CLI consumes a versioned Forge template package or git archive, writes a customer project, then delegates database initialization to the generated backend script instead of reimplementing SQL execution logic.

**Tech Stack:** Node.js 20+, npm package with `bin`, `commander`, `@inquirer/prompts`, `execa`, `fs-extra`.

---

### Task 1: Standalone CLI Repository

**Files:**
- Create: `package.json`
- Create: `src/cli.mjs`
- Create: `src/prompts.mjs`
- Create: `src/generator.mjs`

- [ ] Create a package named `@mdframe/forge-create` with:

```json
{
  "name": "@mdframe/forge-create",
  "version": "0.1.0",
  "type": "module",
  "bin": {
    "forge-create": "src/cli.mjs",
    "create-forge": "src/cli.mjs"
  },
  "engines": {
    "node": ">=20"
  },
  "dependencies": {
    "@inquirer/prompts": "^7.0.0",
    "commander": "^12.0.0",
    "execa": "^9.0.0",
    "fs-extra": "^11.0.0"
  }
}
```

- [ ] `src/cli.mjs` parses non-interactive flags first, then falls back to prompts for missing values.
- [ ] `src/generator.mjs` calls the Forge template generator with the resolved options.

### Task 2: Interactive Module Selection

**Files:**
- Modify: `src/prompts.mjs`
- Modify: `src/generator.mjs`

- [ ] Prompt for project name, display name, package name, groupId, database name, preset, and optional modules.
- [ ] Support `minimal-admin`, `data-app`, `ai-report`, `full`.
- [ ] Support optional modules such as `business-core`, and expose `--strip-module-prefix` / `--module-artifact-prefix`.
- [ ] Persist the final selection to generated `forge.config.json`.

### Task 3: Database Initialization Flow

**Files:**
- Modify: `src/cli.mjs`
- Create: `src/database.mjs`

- [ ] After generation, prompt: `是否立即初始化数据库？`
- [ ] If yes, prompt for MySQL host, port, database, user, password, demo seed, optional seed, module SQL.
- [ ] Execute the generated script:

```bash
bash <project>/<artifactPrefix>-server/scripts/db/init-db.sh \
  --host <host> \
  --port <port> \
  --database <database> \
  --user <user> \
  --password <password>
```

- [ ] Add `--with-demo`, `--with-optional`, and `--with-module` only when selected.
- [ ] Stream script output directly to the user.

### Task 4: Release and Usage

**Files:**
- Create: `README.md`
- Create: `.github/workflows/release.yml`

- [ ] Document:

```bash
npx @mdframe/forge-create
```

- [ ] Document non-interactive usage:

```bash
npx @mdframe/forge-create ./nmg-lt-psi-system \
  --preset minimal-admin \
  --include business-core \
  --project-name nmg-lt-psi-system \
  --display-name 内蒙古商品进销存管理系统 \
  --base-package com.asiainfo.nm.psi \
  --group-id com.asiainfo.nm.psi \
  --database-name psi_admin \
  --strip-module-prefix nmg-lt
```

- [ ] Publish through npm with provenance once the Forge template artifact is versioned.
