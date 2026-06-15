---
name: forge-codegen-crud
description: Generate or review Forge project code-generation output for CRUD modules. Use when creating single-table CRUD, master-detail CRUD, left-tree-right-table pages, or generated Forge backend/frontend/SQL artifacts covering list/detail/add/update/delete, batch delete, import/export, dictionary translation, API encryption/decryption, Flyway migrations, sys_dict_type/sys_dict_data inserts, sys_excel_export_config/sys_excel_column_config inserts, and sys_resource menu or permission inserts.
---

# Forge Codegen CRUD

## Purpose

Generate Forge-compliant CRUD artifacts from business requirements without re-discovering project rules. Treat `AGENTS.md` as the highest priority source; this skill only codifies the repeatable codegen workflow.

## Workflow

1. Read `AGENTS.md`, `.opencode/memory/pitfalls.md`, `.opencode/memory/decisions.md`, and `.opencode/memory/preferences.md` before generating code.
2. Identify the page pattern: `single-table`, `master-detail`, or `left-tree-right-table`.
3. For current single-table CRUD generation, read `references/single-table-crud.md`, `references/sql-seeds.md`, and `references/validation-checklist.md`.
4. If the request involves master-detail or left-tree-right-table, still enforce this skill's global SQL, dictionary, Excel, resource, tenant, encryption, and validation rules, then inspect existing page-template components before implementation.
5. Generate Flyway SQL first, then backend files, frontend files, and menu/resource seed SQL.
6. Verify generated code against the checklist before reporting completion.

## Non-Negotiable Rules

- Use Flyway scripts under `forge/db/migration/` for all schema and built-in data changes.
- Use `tenant_id = 1` for all built-in business data, dictionaries, and resources. Do not seed tenant `0`.
- Put query SQL in Mapper XML. Do not generate Service-layer `LambdaQueryWrapper` query chains except MyBatis-Plus built-in `selectById`, `insert`, `updateById`, and `deleteById` style operations.
- Use `pageNum` and `pageSize`; never generate backend `page` as the page parameter.
- Use AiCrudPage URL placeholders with `:id` or `:${rowKey}`. Do not generate `{id}` placeholders.
- Generated CRUD controllers and frontend API configs must use Forge's POST-safe codegen contract for detail, create, update, and delete:
  - detail: `POST /getById`
  - create: `POST /add`
  - update: `POST /edit`
  - delete: `POST /remove/{id}` and `post@.../remove/:id`
  Do not generate `PUT` or `DELETE` endpoints for generated CRUD modules.
- Use `DictSelect`, `DictTag`, and `useDict()` for dictionary fields. Do not hardcode frontend options or status label maps.
- Generate `sys_dict_type` and `sys_dict_data` seed SQL for new dictionaries, or explicitly reuse an existing dictionary type discovered in migrations.
- Generate `sys_excel_export_config` and `sys_excel_column_config` seed SQL when import/export is enabled.
- Generate `sys_resource` seed SQL for menus and permissions after the page/API contract is known.
- Add `@ApiDecrypt` and `@ApiEncrypt` for sensitive or encrypted endpoints, and use encrypted frontend request modes consistently.

## Required References

- `references/single-table-crud.md`: generated file layout, backend/frontend CRUD contract, batch delete, import/export endpoint shape.
- `references/sql-seeds.md`: table DDL, dictionary inserts, Excel config inserts, resource/menu inserts.
- `references/validation-checklist.md`: final review checklist for generated artifacts.

## Output Expectations

When generating a CRUD module, provide:

- Backend Java artifacts: entity, DTO/query DTO, VO, Mapper, Mapper XML, Service, Service impl, Controller.
- Frontend artifacts: API module if the page needs it, Vue page using AiCrudPage, dictionary bindings, import/export options.
- Flyway SQL: table DDL, indexes, dictionaries, Excel config, resources, and optional API resources.
- Verification notes: compile/build commands attempted and any commands not run.
