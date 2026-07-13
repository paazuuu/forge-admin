# Forge Admin Design System

## 1. Visual Theme & Atmosphere

Forge Admin is a compact enterprise operations interface. The default style is restrained, clear, and built for repeated daily use. It should feel like a professional permission, organization, and data management console, not a marketing site.

- **Density:** 7/10. Information should be compact and scannable, with enough spacing to avoid crowding.
- **Variance:** 4/10. Use subtle layout variation only when it clarifies hierarchy. Avoid decorative asymmetry.
- **Motion:** 2/10. Keep motion restrained. Use hover, active, and loading feedback only where it helps operation clarity.
- **Primary goal:** Users should immediately understand the main object, the current selection, and where each action belongs.

## 2. Color Palette & Roles

- **App Canvas** (#F6F8FB) — page background and large empty areas.
- **Panel Surface** (#FFFFFF) — main content panels, list containers, and form surfaces.
- **Ink Text** (#0F172A) — primary text and active object labels.
- **Steel Text** (#64748B) — secondary metadata, helper text, role keys, usernames, and descriptions.
- **Quiet Border** (#E5E7EB) — panel borders, toolbar separators, input grouping lines.
- **Soft Hover** (#F8FAFC) — hover backgrounds and low-emphasis grouped surfaces.
- **Selected Wash** (#F3F7FF) — selected object background.
- **Action Blue** (#2563EB) — the single primary accent for primary buttons, selected borders, focus states, and active tabs.
- **Danger Red** (#DC2626) — destructive actions only.

Rules:

- Use only one normal accent color: **Action Blue**.
- Do not use purple-blue neon, gradient glows, oversaturated colors, or heavy shadows.
- Do not use pure black.
- Prefer borders, background contrast, and typography weight over decorative effects.

## 3. Typography Rules

- Use the project’s existing sans-serif stack. Dashboard and admin pages must stay sans-serif.
- Page and panel titles: 14px, weight 650, compact line-height.
- Table/list body: 13px.
- Metadata and helper text: 11-12px, Steel Text.
- Do not use oversized headings inside management pages.
- Do not add explanatory marketing copy inside operational pages.

## 4. Layout Principles

### Main Pattern: Object Rail + Workspace

For pages where one entity controls the detail view, use a clear master-detail layout:

- Left side: primary object rail, such as roles, organizations, categories, tenants, or posts.
- Right side: selected object workspace, such as members, permissions, details, or related records.
- The left rail is the “first position” when that entity is the page’s main subject.
- The selected object must be visually obvious.

Reference pattern:

- `/system/role`: left role management rail, right current role user workspace.

Recommended dimensions:

- If the object is the page’s main subject, let the object rail/work area take the remaining flexible width.
- Secondary detail areas may use a fixed width, normally 460-540px when they contain filters and cards.
- For role management, keep roles flexible on the left and users fixed around 520px on the right.
- Gap between panels: 8px.
- Panel radius: 6px.
- Panel border: 1px Quiet Border.
- Avoid attached panels with no spacing unless the UI is intentionally a single compound control.

### Search And Toolbar

- Search and filters stay close to the data they affect.
- Search rows should be compact: 6-8px vertical padding, 8px gap.
- Search rows inside fixed-width side workspaces must use responsive grid tracks. Group query/reset buttons together so they do not drift apart.
- Query and reset buttons should use fixed width when placed in a grid, normally 72px.
- Do not create large top banners for CRUD management pages.
- Remove fake metrics, descriptive hero copy, or unused summary cards from management screens.

## 5. Component Rules

### Object Cards

Use compact object cards when an item is selected and controls another panel.

- Card height: about 58-66px.
- Primary object cards should normally use four columns on desktop and two columns on small screens. Avoid cramming five or six object cards into one row.
- Card radius: 6px.
- Card border: Quiet Border.
- Main label on first line.
- Metadata and status on second line.
- Selected state uses Action Blue border plus subtle Selected Wash.
- Add an explicit selected marker when useful, such as “当前”.
- Avoid generic thick left blue bars as the only selected state.

### Actions

Actions must follow ownership.

- Role actions belong on the role card, not in the member list header.
- Organization actions belong on the organization tree node or organization panel.
- Member/user actions belong on the member/user item.
- Page-level actions only belong in the page toolbar when they affect the whole dataset.
- Use vertical three-dot menus for low-frequency object actions.
- Do not hide a single common action behind “更多”; only group when there are multiple secondary actions.

### Buttons

- Primary button: Action Blue, used for one main action in the local area.
- Secondary/ghost buttons: restrained, no glow.
- Icon-only buttons must have a clear title/aria-label.
- Active state should feel tactile but not animated heavily.

### Trees

- Tree rows should be compact and stable.
- Node actions appear on hover, but icons must be recognizable.
- Tree selection should not use heavy backgrounds or large shadows.
- Filter trees should use a consistent, restrained icon treatment. Do not mix multiple visually heavy organization icons in the same tree.
- Organization trees use `account-tree` for branch nodes and `domain` for leaf nodes. Avoid folder, settings, or managed-folder icons in organization trees.
- Selected tree nodes should use a subtle wash plus 1px border. Avoid thick left bars for normal filter trees.
- Left tree pages should keep consistent spacing and panel width.
- Organization trees inside modals, such as user selection dialogs, follow the same icons, row height, hover, and selected states as left-side organization trees.

### Member Lists

When fields are few, member/user cards are acceptable:

- Avatar/initial on left.
- Name and status on first line.
- Account and phone/metadata on second line.
- Destructive action on the right.
- Keep pagination visible and compact.
- In fixed-width side workspaces, member cards must use self-adaptive grid columns. Around 520px, compact member cards should fit more than two columns when content allows.

Use tables when:

- More than 5-6 fields are shown.
- Sorting, column scanning, or dense comparison matters.
- Batch operations are core to the workflow.

## 6. Loading, Empty, And Error States

- Prefer local loading inside the panel or list that is loading.
- Do not stack global loading over table/list loading for normal CRUD requests.
- Global loading is reserved for transfer-like actions: import, export, upload, download, or long blocking operations.
- Empty states should be compact and local to the area.
- Error messages should identify the failed operation clearly.

## 7. System Page Patterns

### `/system/role`

- Role is the primary object.
- Left side uses a role management rail.
- Role cards show role name, key, status, selected marker, system marker, and object menu.
- Role object menu contains edit, organization scope, permission authorization, and delete.
- Right side is the selected role workspace.
- Adding users requires a concrete authorized organization. If only one organization is available, auto-select it. If multiple exist, disable Add User until one is selected.

### `/system/org`

- Organization tree is the primary navigation object.
- User and post views should stay in one right workspace using tabs or clear sections.
- Add user belongs in the user list toolbar, not in an extra context header.

### `/system/user`, `/system/post`, `/system/tenant`

- Use the same compact CRUD surface.
- Keep top toolbars minimal.
- Batch actions are visible when they are the only secondary action; use More only for multiple secondary actions.
- More button uses vertical three dots.

### `/system/menu`

- Keep menu creation convenient.
- Do not let explanatory headers or metrics consume the first viewport.
- Client switching may remain at the top, but it must be compact.

## 8. Motion And Interaction

- Use transitions only for color, background, border, opacity, and transform.
- Duration should stay around 120-180ms.
- Do not animate width, height, top, or left.
- No perpetual animation in admin CRUD screens.
- Hover states must not shift layout.
- Keyboard focus should remain visible for clickable cards and icon buttons.

## 9. Anti-Patterns

Never use:

- Purple-blue neon gradients or button glows.
- Decorative dashboard cards filled with fake metrics.
- Large hero sections in admin pages.
- Long feature explanations inside operational pages.
- Unclear action ownership, such as role actions placed in a user toolbar.
- Horizontal hidden scrolling for primary objects unless there is an obvious carousel control.
- Three equal decorative cards as a default layout.
- Overlapping UI elements.
- Emoji icons.
- Fake data or invented statistics.
- Heavy shadows, oversized radius, or marketing-style panels in system management pages.

## 10. Implementation Checklist

Before finishing a system page:

- The primary object is visually first.
- The selected object is obvious.
- Actions are placed next to the object they affect.
- Search/filter controls are close to their data.
- Toolbar buttons are not excessive.
- More menus only exist when there are multiple secondary actions.
- Loading is local unless the operation blocks the whole app.
- Empty states are local and compact.
- Text fits at desktop and mobile widths.
- No horizontal overflow on mobile.
