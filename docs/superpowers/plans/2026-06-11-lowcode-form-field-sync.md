# Lowcode Form Field Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the low-code object designer so form designer changes and field-asset changes stay in sync, and saving the designer clears the unsaved-change prompt correctly.

**Architecture:** Keep the existing object-designer flow and patch the current synchronization seams instead of introducing a new store. Form-save should reconcile field assets against the current form schema, while persisted field-asset operations should refresh the full designer context so backend-side reference rewrites are reflected immediately in the UI.

**Tech Stack:** Vue 3 `script setup`, Naive UI, existing Forge low-code designer helpers, backend draft persistence APIs

---

### Task 1: Reconcile field assets from form schema on save

**Files:**
- Modify: `forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue`

- [ ] Add a reconciliation step in `buildCurrentDesignerDraft()` that derives the current bound field set from `formDesignerSchema`.
- [ ] When a field is still present on the form, sync its `fieldName`, `required`, `readonly`, `defaultValue`, `dictType`, `referenceObjectCode`, `referenceDisplayField`, `placeholder`, `sortOrder`, and `formVisible`.
- [ ] When a field asset is no longer present on the form canvas, keep the asset but force `formVisible = false` so “delete from form” is persisted as an unbind instead of leaving stale visibility.
- [ ] Keep auto-created designer fields working by preserving the existing `buildAutoFieldAssets()` flow for newly introduced bindings.

### Task 2: Refresh designer context after persisted field/layout saves

**Files:**
- Modify: `forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue`
- Modify: `forge-admin-ui/src/views/app-center/components/designer/BusinessFieldManager.vue`

- [ ] Extend field-manager `updated` emissions so persisted create/update/delete/sort actions mark that the full designer context must be reloaded.
- [ ] In the page-level `handleFieldsUpdated()` handler, reload the full designer context when the event indicates backend-side schema/reference rewrites happened.
- [ ] After successful form/detail/list layout saves, reload the designer context so the page schema, form schema, fields, and dirty baseline all come back from the saved backend draft.

### Task 3: Verification

**Files:**
- Verify: `forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue`
- Verify: `forge-admin-ui/src/views/app-center/components/designer/BusinessFieldManager.vue`
- Verify: `forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue`

- [ ] Run `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && pnpm --dir forge-admin-ui build`.
- [ ] Manually verify these flows in the object designer:
- [ ] Delete a bound form field, save, switch to 字段资产, and confirm the field is no longer treated as form-visible/used.
- [ ] Edit or delete a field in 字段资产, save, switch back to 表单设计, and confirm the latest bindings/layout are reflected without stale references.
- [ ] Save the designer, switch panels or leave the page, and confirm the unsaved-change prompt no longer appears unless a new change was made.
