#!/usr/bin/env node

import { constants as fsConstants } from 'node:fs'
import fs from 'node:fs/promises'
import path from 'node:path'
import process from 'node:process'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const repoRoot = path.resolve(__dirname, '../..')
const catalogPath = path.join(__dirname, 'module-catalog.json')

const backendParentArtifacts = {
  root: 'forge',
  framework: 'forge-framework',
  dependencies: 'forge-dependencies',
  starterParent: 'forge-starter-parent',
  pluginParent: 'forge-plugin-parent',
  flowParent: 'forge-flow',
  businessParent: 'forge-business',
}

const rootModuleArtifacts = [
  'forge-admin-server',
  'forge-framework',
  'forge-report-server',
  'forge-app-server',
  'forge-flow',
  'forge-business',
]

const flowModuleArtifacts = [
  'forge-flow-client',
  'forge-flow-server',
]

const businessModuleArtifacts = [
  'forge-business-core',
]

const ignoredNames = new Set([
  '.git',
  '.idea',
  '.vscode',
  '.DS_Store',
  '.vite',
  'node_modules',
  'dist',
  'deploy',
  'target',
  'logs',
  '.pnpm-store',
  '__pycache__',
])

const ignoredFileNames = new Set([
  'application-dev.yml',
  '.env.local',
  '.flattened-pom.xml',
])

const projectContextNames = [
  'AGENTS.md',
  '.agents',
  'code-copilot',
]

const binaryExtensions = new Set([
  '.png',
  '.jpg',
  '.jpeg',
  '.gif',
  '.webp',
  '.ico',
  '.woff',
  '.woff2',
  '.ttf',
  '.eot',
  '.xdb',
  '.jar',
  '.zip',
  '.gz',
  '.pdf',
])

main().catch((error) => {
  console.error(`\n[forge:create] ${error.message}`)
  process.exit(1)
})

async function main() {
  const catalog = await readJson(catalogPath)
  const args = parseArgs(process.argv.slice(2))
  if (args.help) {
    printHelp(catalog)
    return
  }

  const options = normalizeOptions(args, catalog)
  const selection = resolveSelection(catalog, options.preset)
  const artifactMap = buildArtifactMap(catalog, options)
  const applicationClassMap = buildApplicationClassMap(options)
  const outputRoot = path.resolve(process.cwd(), options.target)

  await assertWritableTarget(outputRoot, options.force)
  await fs.mkdir(outputRoot, { recursive: true })

  const serverDirName = `${options.artifactPrefix}-server`
  const serverRoot = path.join(outputRoot, serverDirName)
  const selectedArtifacts = collectSelectedArtifacts(catalog, selection)

  await copyBackend(serverRoot)
  await pruneBackend(serverRoot, catalog, selection)
  await pruneBackendSourceGlue(serverRoot, selectedArtifacts)
  await patchBackendPoms(serverRoot, catalog, selection, selectedArtifacts)
  await rewritePomGroupIds(serverRoot, options.groupId)
  await rewriteRootPomArtifact(serverRoot, `${options.artifactPrefix}-server`)

  if (selection.frontendIds.has('admin-ui')) {
    await copyProjectDir(path.join(repoRoot, 'forge-admin-ui'), path.join(outputRoot, `${options.projectName}-admin-ui`))
  }
  if (selection.frontendIds.has('report-ui')) {
    await copyProjectDir(path.join(repoRoot, 'forge-report-ui'), path.join(outputRoot, `${options.projectName}-report-ui`))
  }

  await copyOptionalRootFiles(outputRoot)
  await copyProjectContextFiles(outputRoot)
  await writeGeneratedConfig(outputRoot, options, selection, catalog)

  const replacements = buildTextReplacements(artifactMap, applicationClassMap, options)
  await rewriteTextFiles(outputRoot, replacements)
  await moveJavaPackageDirectories(outputRoot, 'com.mdframe.forge', options.basePackage)
  await renameFilesByBasename(outputRoot, applicationClassMap, '.java')
  await renameArtifactDirectories(serverRoot, artifactMap)

  printSummary(outputRoot, options, selection, catalog)
}

function parseArgs(argv) {
  const args = {
    _: [],
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--') {
      continue
    }
    if (arg === '--help' || arg === '-h') {
      args.help = true
      continue
    }
    if (!arg.startsWith('--')) {
      args._.push(arg)
      continue
    }
    const eqIndex = arg.indexOf('=')
    if (eqIndex > -1) {
      args[arg.slice(2, eqIndex)] = arg.slice(eqIndex + 1)
      continue
    }
    const key = arg.slice(2)
    const next = argv[index + 1]
    if (!next || next.startsWith('--')) {
      args[key] = true
      continue
    }
    args[key] = next
    index += 1
  }
  return args
}

function normalizeOptions(args, catalog) {
  const target = args._[0]
  if (!target) {
    throw new Error('缺少目标目录。示例：pnpm forge:create -- ../smart-factory --base-package com.company.smartfactory')
  }

  const projectName = normalizeProjectName(args['project-name'] || path.basename(path.resolve(target)))
  const artifactPrefix = normalizeProjectName(args['artifact-prefix'] || projectName)
  const displayName = String(args['display-name'] || projectName)
  const preset = String(args.preset || 'ai-report')
  const basePackage = String(args['base-package'] || '').trim()
  const groupId = String(args['group-id'] || basePackage).trim()
  const databaseName = String(args['database-name'] || toSnakeCase(projectName)).trim()

  if (!catalog.presets[preset]) {
    throw new Error(`未知 preset：${preset}。可选值：${Object.keys(catalog.presets).join(', ')}`)
  }
  if (!isValidJavaPackage(basePackage)) {
    throw new Error('请通过 --base-package 指定合法 Java 包名，例如 com.company.smartfactory')
  }
  if (!isValidJavaPackage(groupId)) {
    throw new Error('请通过 --group-id 指定合法 Maven groupId，默认等于 --base-package')
  }
  if (!/^[a-z][a-z0-9_]*$/.test(databaseName)) {
    throw new Error('数据库名只能使用小写字母、数字和下划线，并且必须以字母开头')
  }

  return {
    target,
    projectName,
    artifactPrefix,
    displayName,
    preset,
    basePackage,
    groupId,
    databaseName,
    force: args.force === true || args.force === 'true',
  }
}

function normalizeProjectName(value) {
  const normalized = String(value || '')
    .trim()
    .replace(/_/g, '-')
    .toLowerCase()
  if (!/^[a-z][a-z0-9-]*$/.test(normalized)) {
    throw new Error(`项目英文名不合法：${value}。只允许小写字母、数字、中横线，并且必须以字母开头`)
  }
  return normalized
}

function isValidJavaPackage(value) {
  return /^[a-z_][a-z0-9_]*(\.[a-z_][a-z0-9_]*)+$/.test(value)
}

function resolveSelection(catalog, presetName) {
  const preset = catalog.presets[presetName]
  const selectedModuleIds = new Set()
  const frontendIds = new Set()

  const visit = (moduleId) => {
    const moduleInfo = catalog.modules[moduleId]
    if (!moduleInfo) {
      throw new Error(`模块清单缺少定义：${moduleId}`)
    }
    if (moduleInfo.type === 'frontend') {
      frontendIds.add(moduleId)
      return
    }
    if (selectedModuleIds.has(moduleId)) {
      return
    }
    selectedModuleIds.add(moduleId)
    for (const dependency of moduleInfo.dependencies || []) {
      visit(dependency)
    }
  }

  for (const root of preset.roots) {
    visit(root)
  }

  return {
    presetName,
    selectedModuleIds,
    frontendIds,
  }
}

function collectSelectedArtifacts(catalog, selection) {
  const artifacts = new Set(Object.values(backendParentArtifacts))
  for (const moduleId of selection.selectedModuleIds) {
    const artifactId = catalog.modules[moduleId]?.artifactId
    if (artifactId) {
      artifacts.add(artifactId)
    }
  }
  if (![...selection.selectedModuleIds].some(id => catalog.modules[id]?.type === 'flow')) {
    artifacts.delete(backendParentArtifacts.flowParent)
  }
  if (![...selection.selectedModuleIds].some(id => catalog.modules[id]?.type === 'business')) {
    artifacts.delete(backendParentArtifacts.businessParent)
  }
  return artifacts
}

function buildArtifactMap(catalog, options) {
  const map = {
    [backendParentArtifacts.framework]: `${options.artifactPrefix}-framework`,
    [backendParentArtifacts.dependencies]: `${options.artifactPrefix}-dependencies`,
    [backendParentArtifacts.starterParent]: `${options.artifactPrefix}-starter-parent`,
    [backendParentArtifacts.pluginParent]: `${options.artifactPrefix}-plugin-parent`,
    [backendParentArtifacts.flowParent]: `${options.artifactPrefix}-flow`,
    [backendParentArtifacts.businessParent]: `${options.artifactPrefix}-business`,
    'forge-admin': `${options.artifactPrefix}-admin`,
    'forge-report': `${options.artifactPrefix}-report`,
    'forge-starter-property': `${options.artifactPrefix}-starter-property`,
  }

  for (const moduleInfo of Object.values(catalog.modules)) {
    if (!moduleInfo.artifactId) {
      continue
    }
    const next = moduleInfo.artifactId
      .replace(/^forge-/, `${options.artifactPrefix}-`)
      .replace(/-parent$/, '-parent')
    map[moduleInfo.artifactId] = next
  }
  return map
}

function buildApplicationClassMap(options) {
  const prefix = toPascalCase(options.projectName)
  return {
    ForgeAdminApplication: `${prefix}AdminApplication`,
    ForgeReportApplication: `${prefix}ReportApplication`,
    ForgeAppServerApplication: `${prefix}AppServerApplication`,
    ForgeFlowApplication: `${prefix}FlowApplication`,
  }
}

/**
 * 将字符串转换为 PascalCase（大驼峰）
 * 支持：kebab-case、snake_case、space 分隔、混合大小写
 * 示例：my-app → MyApp, my_app → MyApp, my app → MyApp, MyApp → MyApp
 */
function toPascalCase(str) {
  if (!str) return '';

  return str
  // 1. 把所有分隔符（- _ 空格）替换成空格，统一处理
  .replace(/[-_\s]+/g, ' ')
  // 2. 每个单词首字母大写，其余小写
  .replace(/\b\w+/g, (word) => {
    return word.charAt(0).toUpperCase() + word.slice(1).toLowerCase();
  })
  // 3. 去掉所有空格
  .replace(/ /g, '');
}

async function copyBackend(serverRoot) {
  await copyProjectDir(path.join(repoRoot, 'forge'), serverRoot)
}

async function copyProjectDir(source, target) {
  await fs.cp(source, target, {
    recursive: true,
    filter: sourcePath => shouldCopyPath(sourcePath),
  })
}

function shouldCopyPath(sourcePath) {
  const baseName = path.basename(sourcePath)
  if (ignoredNames.has(baseName) || ignoredFileNames.has(baseName)) {
    return false
  }
  return true
}

async function pruneBackend(serverRoot, catalog, selection) {
  const selectedArtifacts = collectSelectedArtifacts(catalog, selection)

  await pruneModuleDirectories(serverRoot, rootModuleArtifacts, selectedArtifacts)
  await pruneModuleDirectories(
    path.join(serverRoot, 'forge-framework/forge-plugin-parent'),
    artifactsByType(catalog, 'plugin'),
    selectedArtifacts,
  )
  await pruneModuleDirectories(
    path.join(serverRoot, 'forge-framework/forge-starter-parent'),
    artifactsByType(catalog, 'starter'),
    selectedArtifacts,
  )
  await pruneModuleDirectories(path.join(serverRoot, 'forge-flow'), flowModuleArtifacts, selectedArtifacts)
  await pruneModuleDirectories(path.join(serverRoot, 'forge-business'), businessModuleArtifacts, selectedArtifacts)
}

async function pruneBackendSourceGlue(serverRoot, selectedArtifacts) {
  const adminServerRoot = path.join(serverRoot, 'forge-admin-server')
  if (!selectedArtifacts.has('forge-plugin-generator')) {
    await fs.rm(path.join(adminServerRoot, 'src/main/java/com/mdframe/forge/admin/bridge'), {
      recursive: true,
      force: true,
    })
  }
  if (!selectedArtifacts.has('forge-plugin-ai')) {
    await fs.rm(path.join(adminServerRoot, 'src/main/java/com/mdframe/forge/admin/ai'), {
      recursive: true,
      force: true,
    })
  }
}

function artifactsByType(catalog, type) {
  return Object.values(catalog.modules)
    .filter(moduleInfo => moduleInfo.type === type)
    .map(moduleInfo => moduleInfo.artifactId)
}

async function pruneModuleDirectories(parentDir, artifactIds, selectedArtifacts) {
  if (!(await exists(parentDir))) {
    return
  }
  for (const artifactId of artifactIds) {
    if (!selectedArtifacts.has(artifactId)) {
      await fs.rm(path.join(parentDir, artifactId), { recursive: true, force: true })
    }
  }
}

async function patchBackendPoms(serverRoot, catalog, selection, selectedArtifacts) {
  const selectedRootModules = rootModuleArtifacts.filter((artifactId) => {
    if (artifactId === 'forge-framework') {
      return true
    }
    return selectedArtifacts.has(artifactId)
  })
  await replacePomModules(path.join(serverRoot, 'pom.xml'), selectedRootModules)
  await replacePomModules(path.join(serverRoot, 'forge-framework/pom.xml'), [
    'forge-dependencies',
    'forge-starter-parent',
    'forge-plugin-parent',
  ])
  await replacePomModules(
    path.join(serverRoot, 'forge-framework/forge-plugin-parent/pom.xml'),
    artifactsByType(catalog, 'plugin').filter(artifactId => selectedArtifacts.has(artifactId)),
  )
  await replacePomModules(
    path.join(serverRoot, 'forge-framework/forge-starter-parent/pom.xml'),
    artifactsByType(catalog, 'starter').filter(artifactId => selectedArtifacts.has(artifactId)),
  )
  if (selectedArtifacts.has('forge-flow')) {
    await replacePomModules(
      path.join(serverRoot, 'forge-flow/pom.xml'),
      flowModuleArtifacts.filter(artifactId => selectedArtifacts.has(artifactId)),
    )
  }
  if (selectedArtifacts.has('forge-business')) {
    await replacePomModules(
      path.join(serverRoot, 'forge-business/pom.xml'),
      businessModuleArtifacts.filter(artifactId => selectedArtifacts.has(artifactId)),
    )
  }

  const pomFiles = await collectFiles(serverRoot, filePath => path.basename(filePath) === 'pom.xml')
  for (const pomFile of pomFiles) {
    await prunePomDependencies(pomFile, selectedArtifacts)
  }
}

async function replacePomModules(pomFile, modules) {
  if (!(await exists(pomFile))) {
    return
  }
  const content = await fs.readFile(pomFile, 'utf8')
  const moduleContent = [
    '    <modules>',
    ...modules.map(moduleName => `        <module>${moduleName}</module>`),
    '    </modules>',
  ].join('\n')
  const nextContent = content.includes('<modules>')
    ? content.replace(/[\t ]*<modules>[\s\S]*?<\/modules>/, moduleContent)
    : content
  await fs.writeFile(pomFile, nextContent)
}

async function prunePomDependencies(pomFile, selectedArtifacts) {
  const content = await fs.readFile(pomFile, 'utf8')
  const nextContent = content.replace(/[\t ]*<dependency>[\s\S]*?<groupId>com\.mdframe\.forge<\/groupId>[\s\S]*?<artifactId>(forge[^<]+)<\/artifactId>[\s\S]*?<\/dependency>\s*/g, (block, artifactId) => {
    return selectedArtifacts.has(artifactId) ? block : ''
  })
  if (nextContent !== content) {
    await fs.writeFile(pomFile, nextContent)
  }
}

async function rewritePomGroupIds(serverRoot, groupId) {
  const pomFiles = await collectFiles(serverRoot, filePath => path.basename(filePath) === 'pom.xml')
  for (const pomFile of pomFiles) {
    const content = await fs.readFile(pomFile, 'utf8')
    const nextContent = content.split('com.mdframe.forge').join(groupId)
    if (nextContent !== content) {
      await fs.writeFile(pomFile, nextContent)
    }
  }
}

async function rewriteRootPomArtifact(serverRoot, rootArtifactId) {
  const pomFiles = await collectFiles(serverRoot, filePath => path.basename(filePath) === 'pom.xml')
  for (const pomFile of pomFiles) {
    const content = await fs.readFile(pomFile, 'utf8')
    const nextContent = content
      .split('<artifactId>forge</artifactId>').join(`<artifactId>${rootArtifactId}</artifactId>`)
      .split('<name>forge</name>').join(`<name>${rootArtifactId}</name>`)
      .split('<description>forge</description>').join(`<description>${rootArtifactId}</description>`)
    if (nextContent !== content) {
      await fs.writeFile(pomFile, nextContent)
    }
  }
}

function buildTextReplacements(artifactMap, applicationClassMap, options) {
  const snakeName = toSnakeCase(options.projectName)
  const serverDirName = `${options.artifactPrefix}-server`
  const reportPath = `/${options.projectName}-report`
  const replacements = [
    ['com.mdframe.forge', options.basePackage],
    ['com/mdframe/forge', options.basePackage.replaceAll('.', '/')],
    ['Forge Admin', options.displayName],
    ['企业级中后台基础框架', options.displayName],
    ['forge-project', options.projectName],
    ['cd forge &&', `cd ${serverDirName} &&`],
    ['cd forge ', `cd ${serverDirName} `],
    ['forge/                          # 后端根目录', `${serverDirName}/              # 后端根目录`],
    ['forge/                           # 后端根工程', `${serverDirName}/               # 后端根工程`],
    ['forge/\n', `${serverDirName}/\n`],
    ['CREATE DATABASE forge ', `CREATE DATABASE ${options.databaseName} `],
    ['mysql -u root -p forge ', `mysql -u root -p ${options.databaseName} `],
    ['forge-admin-ui', `${options.projectName}-admin-ui`],
    ['forge-report-ui', `${options.projectName}-report-ui`],
    ['forge/forge-admin/', `${serverDirName}/${options.artifactPrefix}-admin-server/`],
    ['forge/forge-report/', `${serverDirName}/${options.artifactPrefix}-report-server/`],
    ['forge-admin/', `${options.artifactPrefix}-admin-server/`],
    ['forge-report/', `${options.artifactPrefix}-report-server/`],
    ['forge/db', `${serverDirName}/db`],
    ['forge/scripts', `${serverDirName}/scripts`],
    ['forge/var', `${serverDirName}/var`],
    ['forge_admin_new', options.databaseName],
    ['forge_admin', options.databaseName],
    ['forge_flow', `${options.databaseName}_flow`],
    ['forge_schema_history', `${snakeName}_schema_history`],
    ['forge_report', `${snakeName}_report`],
    ['forge_pc_001', `${snakeName}_pc_001`],
    ['/forge-report', reportPath],
    ['vue-naive-admin', `${options.projectName}-admin-ui`],
    ['com.forge', options.basePackage],
  ]

  for (const [from, to] of Object.entries(applicationClassMap)) {
    replacements.push([from, to])
  }

  for (const [from, to] of Object.entries(artifactMap).sort((a, b) => b[0].length - a[0].length)) {
    replacements.push([`forge/${from}`, `${serverDirName}/${to}`])
  }

  for (const [from, to] of Object.entries(artifactMap).sort((a, b) => b[0].length - a[0].length)) {
    replacements.push([from, to])
  }

  for (const targetName of Object.values(artifactMap).sort((a, b) => b.length - a.length)) {
    replacements.push([`forge/${targetName}`, `${serverDirName}/${targetName}`])
  }

  return replacements
}

async function rewriteTextFiles(rootDir, replacements) {
  const files = await collectFiles(rootDir, filePath => !isBinaryFile(filePath))
  for (const file of files) {
    let content
    try {
      content = await fs.readFile(file, 'utf8')
    }
    catch {
      continue
    }
    let nextContent = content
    for (const [from, to] of replacements) {
      nextContent = nextContent.split(from).join(to)
    }
    if (nextContent !== content) {
      await fs.writeFile(file, nextContent)
    }
  }
}

function isBinaryFile(filePath) {
  return binaryExtensions.has(path.extname(filePath).toLowerCase())
}

async function moveJavaPackageDirectories(rootDir, oldPackage, newPackage) {
  const oldPackagePath = oldPackage.replaceAll('.', path.sep)
  const newPackagePath = newPackage.replaceAll('.', path.sep)
  const javaRoots = await collectDirectories(rootDir, dirPath => /src[/\\](main|test)[/\\]java$/.test(dirPath))

  for (const javaRoot of javaRoots) {
    const oldDir = path.join(javaRoot, oldPackagePath)
    if (!(await exists(oldDir))) {
      continue
    }
    const newDir = path.join(javaRoot, newPackagePath)
    await fs.mkdir(path.dirname(newDir), { recursive: true })
    if (await exists(newDir)) {
      await mergeDirectory(oldDir, newDir)
      await fs.rm(oldDir, { recursive: true, force: true })
    }
    else {
      await fs.rename(oldDir, newDir)
    }
    await removeEmptyParents(path.dirname(oldDir), javaRoot)
  }
}

async function mergeDirectory(source, target) {
  await fs.mkdir(target, { recursive: true })
  const entries = await fs.readdir(source, { withFileTypes: true })
  for (const entry of entries) {
    const sourcePath = path.join(source, entry.name)
    const targetPath = path.join(target, entry.name)
    if (entry.isDirectory()) {
      await mergeDirectory(sourcePath, targetPath)
    }
    else {
      await fs.rename(sourcePath, targetPath)
    }
  }
}

async function removeEmptyParents(startDir, stopDir) {
  let current = startDir
  while (current.startsWith(stopDir) && current !== stopDir) {
    try {
      await fs.rmdir(current)
    }
    catch {
      break
    }
    current = path.dirname(current)
  }
}

async function renameFilesByBasename(rootDir, basenameMap, extension = '') {
  const files = await collectFiles(rootDir, filePath => Object.hasOwn(basenameMap, path.basename(filePath, extension)))
  for (const file of files) {
    const basename = path.basename(file, extension)
    const nextBasename = basenameMap[basename]
    if (!nextBasename || nextBasename === basename) {
      continue
    }
    const target = path.join(path.dirname(file), `${nextBasename}${extension}`)
    if (await exists(target)) {
      continue
    }
    await fs.rename(file, target)
  }
}

async function renameArtifactDirectories(serverRoot, artifactMap) {
  const dirs = await collectDirectories(serverRoot, () => true)
  dirs.sort((a, b) => b.length - a.length)
  for (const dir of dirs) {
    const baseName = path.basename(dir)
    const nextBaseName = artifactMap[baseName]
    if (!nextBaseName || nextBaseName === baseName) {
      continue
    }
    const target = path.join(path.dirname(dir), nextBaseName)
    if (await exists(target)) {
      continue
    }
    await fs.rename(dir, target)
  }
}

async function copyOptionalRootFiles(outputRoot) {
  for (const fileName of ['LICENSE', '.gitignore']) {
    const source = path.join(repoRoot, fileName)
    if (await exists(source)) {
      await fs.copyFile(source, path.join(outputRoot, fileName))
    }
  }
}

async function copyProjectContextFiles(outputRoot) {
  for (const name of projectContextNames) {
    const source = path.join(repoRoot, name)
    if (await exists(source)) {
      await copyProjectDir(source, path.join(outputRoot, name))
    }
  }
}

async function writeGeneratedConfig(outputRoot, options, selection, catalog) {
  const config = {
    projectName: options.projectName,
    displayName: options.displayName,
    basePackage: options.basePackage,
    groupId: options.groupId,
    artifactPrefix: options.artifactPrefix,
    databaseName: options.databaseName,
    preset: options.preset,
    modules: [...selection.selectedModuleIds].sort(),
    frontends: [...selection.frontendIds].sort(),
  }
  await fs.writeFile(
    path.join(outputRoot, 'forge.config.json'),
    `${JSON.stringify(config, null, 2)}\n`,
  )

  const presetDescription = catalog.presets[options.preset]?.description || options.preset
  const readme = `# ${options.displayName}

该工程由 Forge 项目装配器生成。

## 生成参数

- preset: ${options.preset} - ${presetDescription}
- Java 包名: ${options.basePackage}
- Maven groupId: ${options.groupId}
- artifact 前缀: ${options.artifactPrefix}
- 数据库名: ${options.databaseName}

## 常用命令

\`\`\`bash
cd ${options.artifactPrefix}-server
mvn -pl ${options.artifactPrefix}-admin-server -am compile -DskipTests
\`\`\`

\`\`\`bash
cd ${options.projectName}-admin-ui
pnpm install
pnpm dev
\`\`\`

${selection.frontendIds.has('report-ui')
  ? `\`\`\`bash
cd ${options.projectName}-report-ui
pnpm install
pnpm dev
\`\`\`
`
  : ''}
## 说明

第一版生成器按 preset 裁剪 Maven 子模块、starter、plugin 和前端工程目录。管理端前端内部页面暂未做菜单级裁剪，后续应结合 seed 菜单数据和路由清单继续细化。
`
  await fs.writeFile(path.join(outputRoot, 'README.md'), readme)
}

async function assertWritableTarget(outputRoot, force) {
  if (!(await exists(outputRoot))) {
    return
  }
  const entries = await fs.readdir(outputRoot)
  if (entries.length === 0) {
    return
  }
  if (!force) {
    throw new Error(`目标目录非空：${outputRoot}。如需覆盖请加 --force`)
  }
  await fs.rm(outputRoot, { recursive: true, force: true })
}

async function collectFiles(rootDir, predicate) {
  const result = []
  if (!(await exists(rootDir))) {
    return result
  }
  const entries = await fs.readdir(rootDir, { withFileTypes: true })
  for (const entry of entries) {
    const entryPath = path.join(rootDir, entry.name)
    if (entry.isDirectory()) {
      result.push(...await collectFiles(entryPath, predicate))
      continue
    }
    if (predicate(entryPath)) {
      result.push(entryPath)
    }
  }
  return result
}

async function collectDirectories(rootDir, predicate) {
  const result = []
  if (!(await exists(rootDir))) {
    return result
  }
  const entries = await fs.readdir(rootDir, { withFileTypes: true })
  for (const entry of entries) {
    if (!entry.isDirectory()) {
      continue
    }
    const entryPath = path.join(rootDir, entry.name)
    if (predicate(entryPath)) {
      result.push(entryPath)
    }
    result.push(...await collectDirectories(entryPath, predicate))
  }
  return result
}

async function exists(filePath) {
  try {
    await fs.access(filePath, fsConstants.F_OK)
    return true
  }
  catch {
    return false
  }
}

async function readJson(filePath) {
  return JSON.parse(await fs.readFile(filePath, 'utf8'))
}

function toSnakeCase(value) {
  return String(value)
    .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
    .replace(/-/g, '_')
    .replace(/[^a-zA-Z0-9_]/g, '_')
    .replace(/_+/g, '_')
    .replace(/^_+|_+$/g, '')
    .toLowerCase()
}

function printHelp(catalog) {
  console.log(`Forge 项目装配器

用法：
  pnpm forge:create -- <target-dir> --base-package com.company.project [options]

参数：
  --preset            ${Object.keys(catalog.presets).join(' | ')}，默认 ai-report
  --project-name      项目英文名，默认取目标目录名
  --display-name      系统中文名，默认等于项目英文名
  --base-package      新 Java 包名，必填，例如 com.company.smartfactory
  --group-id          Maven groupId，默认等于 base-package
  --artifact-prefix   Maven artifactId 前缀，默认等于 project-name
  --database-name     数据库名，默认由 project-name 转 snake_case
  --force             目标目录非空时覆盖

示例：
  pnpm forge:create -- ../smart-factory \\
    --preset ai-report \\
    --display-name 智慧工厂管理平台 \\
    --base-package com.company.smartfactory
`)
}

function printSummary(outputRoot, options, selection, catalog) {
  const modules = [...selection.selectedModuleIds].sort()
  const frontends = [...selection.frontendIds].sort()
  console.log('\n[forge:create] 生成完成')
  console.log(`目标目录：${outputRoot}`)
  console.log(`preset：${options.preset} - ${catalog.presets[options.preset].description}`)
  console.log(`后端模块：${modules.join(', ')}`)
  console.log(`前端工程：${frontends.length ? frontends.join(', ') : '无'}`)
  console.log('\n建议验证：')
  console.log(`  cd ${path.join(outputRoot, `${options.artifactPrefix}-server`)}`)
  console.log(`  mvn -pl ${options.artifactPrefix}-admin-server -am compile -DskipTests`)
  if (selection.frontendIds.has('admin-ui')) {
    console.log(`  cd ${path.join(outputRoot, `${options.projectName}-admin-ui`)}`)
    console.log('  pnpm install && pnpm build')
  }
}
