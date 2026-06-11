<template>
  <view class="todo-page">
    <view class="page-glow page-glow-blue" />
    <view class="page-glow page-glow-cyan" />
    <view class="grid-layer" />

    <view class="todo-content">
      <view class="page-header animate-in">
        <button class="header-button" @click="goBack">
          <AiIcon icon="/static/icons/ai-icon/arrow-left.svg" color="#334155" size="sm" />
        </button>
        <view class="header-copy">
          <text class="page-title">我的待办</text>
          <text class="page-subtitle">{{ loading ? '正在同步流程任务' : `共 ${total} 条待处理任务` }}</text>
        </view>
        <button class="header-button" @click="refreshList">
          <AiIcon icon="/static/icons/ai-icon/loader.svg" color="#2563eb" size="sm" />
        </button>
      </view>

      <view class="summary-card animate-in delay-1">
        <view class="summary-main">
          <text class="summary-label">待审批</text>
          <text class="summary-value">{{ total }}</text>
        </view>
        <view class="summary-side">
          <view class="summary-pill">
            <AiIcon icon="/static/icons/ai-icon/clock.svg" color="#d97706" size="sm" />
            <text>{{ urgentCount }} 个高优先级</text>
          </view>
          <view class="summary-pill">
            <AiIcon icon="/static/icons/ai-icon/check-circle.svg" color="#16a34a" size="sm" />
            <text>可移动端处理</text>
          </view>
        </view>
      </view>

      <view class="search-bar animate-in delay-2">
        <AiIcon icon="/static/icons/ai-icon/search.svg" color="#64748b" size="sm" />
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索任务标题"
          placeholder-class="search-placeholder"
          confirm-type="search"
          @confirm="handleSearch"
        />
        <button v-if="keyword" class="search-clear" @click="clearSearch">
          <AiIcon icon="/static/icons/ai-icon/x.svg" color="#94a3b8" size="sm" />
        </button>
      </view>

      <view class="task-list animate-in delay-3">
        <view
          v-for="task in tasks"
          :key="taskKey(task)"
          class="task-card"
          @click="openTask(task)"
        >
          <view class="task-top">
            <view class="task-icon" :class="priorityClass(task)">
              <AiIcon icon="/static/icons/ai-icon/file.svg" color="#ffffff" size="md" />
            </view>
            <view class="task-main">
              <text class="task-title">{{ taskTitle(task) }}</text>
              <text class="task-desc">{{ task.taskName || task.name || '审批节点' }}</text>
            </view>
            <view class="task-status">
              <text>{{ statusText(task) }}</text>
            </view>
          </view>
          <view class="task-meta">
            <view class="meta-item">
              <AiIcon icon="/static/icons/ai-icon/user.svg" color="#64748b" size="sm" />
              <text>{{ task.startUserName || task.createByName || '-' }}</text>
            </view>
            <view class="meta-item">
              <AiIcon icon="/static/icons/ai-icon/calendar.svg" color="#64748b" size="sm" />
              <text>{{ task.createTime || task.startTime || '-' }}</text>
            </view>
          </view>
        </view>

        <view v-if="!loading && !tasks.length" class="empty-state">
          <view class="empty-icon">
            <AiIcon icon="/static/icons/ai-icon/inbox.svg" color="#94a3b8" size="lg" />
          </view>
          <text class="empty-title">{{ flowServiceUnavailable ? '流程服务未连接' : (keyword ? '暂无匹配待办' : '暂无待办') }}</text>
          <text class="empty-desc">{{ emptyDescription }}</text>
        </view>

        <view v-if="loading" class="list-hint">
          <text>加载中...</text>
        </view>
        <view v-else-if="tasks.length && !hasMore" class="list-hint">
          <text>没有更多了</text>
        </view>
      </view>
    </view>

    <AiPopupSheet
      v-model="detailVisible"
      :title="taskTitle(currentTask)"
      :description="currentTask?.taskName || '审批详情'"
      max-height="92vh"
      body-max-height="calc(92vh - 236rpx - env(safe-area-inset-bottom))"
      :close-on-mask="!actionLoading"
    >
      <view v-if="currentTask" class="detail-body">
        <view class="detail-section">
          <view class="section-title-row">
            <AiIcon icon="/static/icons/ai-icon/info.svg" color="#2563eb" size="sm" />
            <text>任务信息</text>
          </view>
          <view class="detail-grid">
            <view v-for="item in detailItems" :key="item.label" class="detail-item">
              <text class="detail-label">{{ item.label }}</text>
              <text class="detail-value">{{ item.value }}</text>
            </view>
          </view>
        </view>

        <view v-if="formNotice" class="form-notice">
          <AiIcon icon="/static/icons/ai-icon/alert-triangle.svg" color="#d97706" size="sm" />
          <text>{{ formNotice }}</text>
        </view>

        <view v-if="variablePairs.length" class="detail-section">
          <view class="section-title-row">
            <AiIcon icon="/static/icons/ai-icon/briefcase.svg" color="#0f766e" size="sm" />
            <text>流程变量</text>
          </view>
          <view class="variable-list">
            <view v-for="item in variablePairs" :key="item.key" class="variable-row">
              <text class="variable-key">{{ item.key }}</text>
              <text class="variable-value">{{ item.value }}</text>
            </view>
          </view>
        </view>

        <view class="detail-section">
          <view class="section-title-row">
            <AiIcon icon="/static/icons/ai-icon/clock.svg" color="#7c3aed" size="sm" />
            <text>审批进度</text>
          </view>
          <view v-if="historyItems.length" class="timeline">
            <view v-for="item in historyItems" :key="historyKey(item)" class="timeline-row">
              <view class="timeline-dot" />
              <view class="timeline-content">
                <text class="timeline-title">{{ item.activityName || item.taskName || item.name || '流程节点' }}</text>
                <text class="timeline-desc">{{ item.assigneeName || item.userName || item.operatorName || '-' }} · {{ item.endTime || item.createTime || item.startTime || '-' }}</text>
                <text v-if="item.comment" class="timeline-comment">{{ item.comment }}</text>
              </view>
            </view>
          </view>
          <view v-else class="timeline-empty">
            <text>{{ detailLoading ? '审批进度加载中...' : '暂无审批记录' }}</text>
          </view>
        </view>

        <view class="approve-box">
          <view class="section-title-row">
            <AiIcon icon="/static/icons/ai-icon/send.svg" color="#16a34a" size="sm" />
            <text>审批意见</text>
          </view>
          <textarea
            v-model="approveComment"
            class="comment-input"
            maxlength="500"
            placeholder="请输入审批意见"
            placeholder-class="comment-placeholder"
          />
        </view>
      </view>

      <template #footer>
        <view class="detail-actions">
          <AiButton
            v-if="canReject"
            variant="danger"
            :loading="actionLoading && actionType === 'reject'"
            :disabled="actionLoading"
            @click="submitTask('reject')"
          >
            驳回
          </AiButton>
          <AiButton
            v-if="canApprove"
            :loading="actionLoading && actionType === 'approve'"
            :disabled="actionLoading"
            @click="submitTask('approve')"
          >
            通过
          </AiButton>
        </view>
      </template>
    </AiPopupSheet>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import AiButton from '@/components/AiButton.vue'
import AiIcon from '@/components/AiIcon.vue'
import AiPopupSheet from '@/components/AiPopupSheet.vue'
import api from '@/api'
import { useAuthStore } from '@/store'
import { ensureLogin } from '@/utils/auth-guard'
import { showConfirmDialog } from '@/utils/dialog'
import { toast } from '@/utils/notify'

const authStore = useAuthStore()
const tasks = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')
const loading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const currentTask = ref(null)
const taskFormInfo = ref(null)
const historyItems = ref([])
const approveComment = ref('')
const actionLoading = ref(false)
const actionType = ref('')
const routeTaskId = ref('')
const routeTaskOpened = ref(false)
const flowServiceUnavailable = ref(false)

const userId = computed(() => {
  const user = authStore.userInfo || {}
  return user.id || user.userId || user.user_id || ''
})
const hasMore = computed(() => tasks.value.length < total.value)
const urgentCount = computed(() => tasks.value.filter(task => Number(task.priority || 0) >= 2).length)
const emptyDescription = computed(() => {
  if (flowServiceUnavailable.value) {
    return '请确认 forge-flow-server 已启动，并检查 H5 的 VITE_FLOW_PROXY_TARGET'
  }
  return keyword.value ? '换个关键词再试试' : '当前账号没有待处理流程任务'
})
const canApprove = computed(() => taskFormInfo.value?.allowApprove !== false)
const canReject = computed(() => taskFormInfo.value?.allowReject !== false)
const requireComment = computed(() => taskFormInfo.value?.requireComment !== false)
const formNotice = computed(() => {
  if (!taskFormInfo.value) {
    return ''
  }
  if (taskFormInfo.value.formType === 'external' && taskFormInfo.value.formUrl) {
    return '该节点配置了外部业务表单，H5 当前展示基础信息，请确认后再处理。'
  }
  if (taskFormInfo.value.formType === 'dynamic' && taskFormInfo.value.formJson) {
    return '该节点配置了动态表单，H5 当前先提交已有流程变量。'
  }
  return ''
})
const variablePairs = computed(() => {
  const variables = taskFormInfo.value?.variables || currentTask.value?.variables || {}
  return Object.entries(variables)
    .filter(([, value]) => ['string', 'number', 'boolean'].includes(typeof value))
    .slice(0, 8)
    .map(([key, value]) => ({ key, value: String(value) }))
})
const detailItems = computed(() => {
  const task = currentTask.value || {}
  return [
    { label: '当前节点', value: task.taskName || task.name || '-' },
    { label: '流程分类', value: task.businessType || task.categoryName || '-' },
    { label: '发起人', value: task.startUserName || task.createByName || '-' },
    { label: '发起部门', value: task.startDeptName || '-' },
    { label: '发起时间', value: task.createTime || task.startTime || '-' },
    { label: '流程实例', value: task.processInstanceId || '-' },
  ]
})

onLoad((options = {}) => {
  routeTaskId.value = options.taskId || options.id || ''
})

onShow(async () => {
  const ok = await ensureLogin({ redirect: routeTaskId.value ? `/pages/todo?taskId=${routeTaskId.value}` : '/pages/todo' })
  if (!ok) {
    return
  }
  if (!tasks.value.length) {
    await loadTasks({ reset: true })
  }
})

onPullDownRefresh(async () => {
  try {
    await loadTasks({ reset: true })
  }
  finally {
    uni.stopPullDownRefresh()
  }
})

onReachBottom(() => {
  if (!loading.value && hasMore.value) {
    loadTasks()
  }
})

async function refreshList() {
  await loadTasks({ reset: true })
  toast('已刷新', { type: 'success' })
}

function handleSearch() {
  routeTaskOpened.value = true
  loadTasks({ reset: true })
}

function clearSearch() {
  keyword.value = ''
  handleSearch()
}

async function loadTasks(options = {}) {
  if (loading.value) {
    return
  }
  if (options.reset) {
    pageNum.value = 1
    total.value = 0
    tasks.value = []
  }

  loading.value = true
  try {
    flowServiceUnavailable.value = false
    const res = await api.getTodoTasks({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      userId: userId.value || undefined,
      title: keyword.value.trim() || undefined,
    })
    const pageData = normalizePageData(res?.data)
    total.value = pageData.total
    tasks.value = options.reset ? pageData.records : tasks.value.concat(pageData.records)
    pageNum.value += 1
    await openRouteTaskIfNeeded()
  }
  catch (error) {
    flowServiceUnavailable.value = isFlowServiceUnavailableError(error)
    if (!flowServiceUnavailable.value) {
      console.error('加载待办失败:', error)
    }
  }
  finally {
    loading.value = false
  }
}

async function openRouteTaskIfNeeded() {
  if (!routeTaskId.value || routeTaskOpened.value) {
    return
  }
  const target = tasks.value.find((task) => {
    const id = String(routeTaskId.value)
    return [task.taskId, task.id, task.bizKey, task.businessKey].map(value => String(value || '')).includes(id)
  })

  routeTaskOpened.value = true
  if (target) {
    await openTask(target)
    return
  }

  try {
    const res = await api.getFlowTaskDetail(routeTaskId.value)
    if (res?.data) {
      await openTask(res.data)
    }
  }
  catch (error) {
    console.error('按 taskId 打开待办失败:', error)
  }
}

async function openTask(task) {
  currentTask.value = task
  taskFormInfo.value = null
  historyItems.value = []
  approveComment.value = ''
  detailVisible.value = true
  detailLoading.value = true

  const taskId = task.taskId || task.id
  const promises = []
  if (taskId) {
    promises.push(
      api.getFlowTaskForm(taskId)
        .then((res) => {
          taskFormInfo.value = res?.data || null
        })
        .catch(error => console.error('加载任务表单失败:', error))
    )
  }
  if (task.processInstanceId) {
    promises.push(
      api.getFlowTaskHistory(task.processInstanceId)
        .then((res) => {
          historyItems.value = Array.isArray(res?.data) ? res.data : []
        })
        .catch(error => console.error('加载审批进度失败:', error))
    )
  }

  await Promise.allSettled(promises)
  detailLoading.value = false
}

async function submitTask(type) {
  if (!currentTask.value || actionLoading.value) {
    return
  }
  if (requireComment.value && !approveComment.value.trim()) {
    toast('请输入审批意见', { type: 'warning' })
    return
  }

  const title = type === 'approve' ? '确认通过' : '确认驳回'
  const confirmed = await showConfirmDialog({
    title,
    description: `确认${type === 'approve' ? '通过' : '驳回'}该流程任务？`,
    icon: type === 'approve' ? 'success' : 'warning',
    confirmText: type === 'approve' ? '通过' : '驳回',
    cancelText: '取消',
    isDestructive: type === 'reject',
  })
  if (!confirmed) {
    return
  }

  actionLoading.value = true
  actionType.value = type
  try {
    const payload = {
      taskId: currentTask.value.taskId || currentTask.value.id,
      userId: userId.value,
      comment: approveComment.value.trim(),
      signature: '',
      variables: taskFormInfo.value?.variables || currentTask.value.variables || {},
    }
    const request = type === 'approve' ? api.approveFlowTask : api.rejectFlowTask
    await request(payload)
    toast(type === 'approve' ? '审批已通过' : '任务已驳回', { type: 'success' })
    detailVisible.value = false
    await loadTasks({ reset: true })
  }
  catch (error) {
    console.error('处理待办失败:', error)
  }
  finally {
    actionLoading.value = false
    actionType.value = ''
  }
}

function normalizePageData(data) {
  const records = data?.records || data?.list || data?.rows || data?.data || []
  const safeRecords = Array.isArray(records) ? records : []
  const totalValue = Number(data?.total ?? data?.totalCount ?? data?.count ?? safeRecords.length)
  return {
    records: safeRecords,
    total: Number.isFinite(totalValue) ? totalValue : safeRecords.length,
  }
}

function isFlowServiceUnavailableError(error) {
  const status = Number(error?.code || error?.error?.status || 0)
  const responseData = error?.error?.data
  return error?.code === 'NETWORK_ERROR'
    || status === 404
    || (status === 500 && (responseData === '' || responseData == null))
}

function taskKey(task) {
  return task.taskId || task.id || task.processInstanceId || task.title
}

function taskTitle(task = {}) {
  task = task || {}
  return task.title || task.businessTitle || task.processName || task.processDefinitionName || task.taskName || '审批任务'
}

function priorityClass(task) {
  const priority = Number(task.priority || 0)
  if (priority >= 3) {
    return 'urgent'
  }
  if (priority >= 2) {
    return 'high'
  }
  return ''
}

function statusText(task) {
  if (task.status === 0 || task.status === '0') {
    return '待签收'
  }
  if (task.status === 1 || task.status === '1') {
    return '处理中'
  }
  return '待处理'
}

function historyKey(item) {
  return item.id || item.taskId || `${item.activityName || item.taskName}-${item.startTime || item.createTime}`
}

function goBack() {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack()
    return
  }
  uni.switchTab({ url: '/pages/index/index' })
}
</script>

<style lang="scss" scoped>
.todo-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  box-sizing: border-box;
  background: #f8fafc;
}

.todo-page::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at 14% 8%, rgba(191, 219, 254, 0.46), transparent 32%),
    radial-gradient(circle at 88% 24%, rgba(153, 246, 228, 0.3), transparent 30%),
    radial-gradient(circle at 28% 92%, rgba(221, 214, 254, 0.28), transparent 34%);
}

.grid-layer {
  position: absolute;
  inset: 0;
  opacity: 0.14;
  pointer-events: none;
  background-image:
    linear-gradient(#e2e8f0 1rpx, transparent 1rpx),
    linear-gradient(90deg, #e2e8f0 1rpx, transparent 1rpx);
  background-size: 82rpx 82rpx;
}

.page-glow {
  position: absolute;
  width: 500rpx;
  height: 500rpx;
  border-radius: 999rpx;
  filter: blur(90rpx);
  pointer-events: none;
}

.page-glow-blue {
  top: -190rpx;
  left: -180rpx;
  background: rgba(147, 197, 253, 0.38);
}

.page-glow-cyan {
  right: -180rpx;
  bottom: 120rpx;
  background: rgba(153, 246, 228, 0.34);
}

.todo-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  padding: 42rpx 28rpx 64rpx;
  box-sizing: border-box;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.header-button {
  display: flex;
  width: 72rpx;
  height: 72rpx;
  align-items: center;
  justify-content: center;
  margin: 0;
  padding: 0;
  border: 1rpx solid rgba(255, 255, 255, 0.9);
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.74);
  box-shadow: 0 8rpx 22rpx rgba(15, 23, 42, 0.05);
}

.header-button::after,
.search-clear::after {
  border: 0;
}

.header-copy {
  min-width: 0;
  flex: 1;
}

.page-title,
.page-subtitle,
.summary-label,
.summary-value,
.summary-pill text,
.task-title,
.task-desc,
.task-status text,
.meta-item text,
.empty-title,
.empty-desc,
.list-hint text,
.section-title-row text,
.detail-label,
.detail-value,
.form-notice text,
.variable-key,
.variable-value,
.timeline-title,
.timeline-desc,
.timeline-comment,
.timeline-empty text {
  display: block;
}

.page-title {
  color: #0f172a;
  font-size: 40rpx;
  font-weight: 950;
  line-height: 1.18;
}

.page-subtitle {
  margin-top: 6rpx;
  color: #64748b;
  font-size: 24rpx;
  font-weight: 700;
}

.summary-card {
  display: flex;
  align-items: stretch;
  gap: 22rpx;
  padding: 30rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.84);
  border-radius: 34rpx;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.94), rgba(15, 118, 110, 0.88));
  box-shadow: 0 20rpx 46rpx rgba(37, 99, 235, 0.18);
}

.summary-main {
  min-width: 180rpx;
}

.summary-label {
  color: rgba(255, 255, 255, 0.76);
  font-size: 24rpx;
  font-weight: 800;
}

.summary-value {
  margin-top: 6rpx;
  color: #ffffff;
  font-size: 70rpx;
  font-weight: 950;
  line-height: 1;
}

.summary-side {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  justify-content: center;
  gap: 14rpx;
}

.summary-pill {
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding: 12rpx 16rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.32);
  border-radius: 18rpx;
  background: rgba(255, 255, 255, 0.18);
}

.summary-pill text {
  min-width: 0;
  overflow: hidden;
  color: #ffffff;
  font-size: 23rpx;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 16rpx;
  height: 88rpx;
  padding: 0 24rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.88);
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 8rpx 28rpx rgba(15, 23, 42, 0.04);
  box-sizing: border-box;
}

.search-input {
  min-width: 0;
  flex: 1;
  height: 82rpx;
  color: #334155;
  font-size: 28rpx;
  font-weight: 700;
}

:deep(.search-placeholder),
:deep(.comment-placeholder) {
  color: #94a3b8;
  font-weight: 500;
}

.search-clear {
  display: flex;
  width: 48rpx;
  height: 48rpx;
  align-items: center;
  justify-content: center;
  margin: 0;
  padding: 0;
  border: 0;
  border-radius: 999rpx;
  background: transparent;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.task-card {
  padding: 26rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.86);
  border-radius: 32rpx;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 10rpx 30rpx rgba(15, 23, 42, 0.045);
  backdrop-filter: blur(18rpx);
}

.task-top {
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.task-icon {
  display: flex;
  width: 76rpx;
  height: 76rpx;
  flex: 0 0 76rpx;
  align-items: center;
  justify-content: center;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #2563eb, #0891b2);
  box-shadow: 0 12rpx 24rpx rgba(37, 99, 235, 0.18);
}

.task-icon.high {
  background: linear-gradient(135deg, #d97706, #f59e0b);
  box-shadow: 0 12rpx 24rpx rgba(217, 119, 6, 0.16);
}

.task-icon.urgent {
  background: linear-gradient(135deg, #e11d48, #ef4444);
  box-shadow: 0 12rpx 24rpx rgba(225, 29, 72, 0.18);
}

.task-main {
  min-width: 0;
  flex: 1;
}

.task-title {
  overflow: hidden;
  color: #1e293b;
  font-size: 30rpx;
  font-weight: 950;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-desc {
  overflow: hidden;
  margin-top: 7rpx;
  color: #64748b;
  font-size: 23rpx;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-status {
  flex-shrink: 0;
  padding: 8rpx 14rpx;
  border-radius: 16rpx;
  background: #eff6ff;
}

.task-status text {
  color: #2563eb;
  font-size: 21rpx;
  font-weight: 900;
}

.task-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx 18rpx;
  margin-top: 22rpx;
}

.meta-item {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8rpx;
  padding: 8rpx 12rpx;
  border-radius: 14rpx;
  background: #f8fafc;
}

.meta-item text {
  max-width: 430rpx;
  overflow: hidden;
  color: #64748b;
  font-size: 22rpx;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 90rpx 40rpx;
  border: 1rpx solid rgba(226, 232, 240, 0.72);
  border-radius: 34rpx;
  background: rgba(255, 255, 255, 0.62);
}

.empty-icon {
  display: flex;
  width: 104rpx;
  height: 104rpx;
  align-items: center;
  justify-content: center;
  border-radius: 32rpx;
  background: #f1f5f9;
}

.empty-title {
  margin-top: 22rpx;
  color: #334155;
  font-size: 30rpx;
  font-weight: 900;
}

.empty-desc {
  margin-top: 10rpx;
  color: #94a3b8;
  font-size: 24rpx;
  font-weight: 700;
}

.list-hint {
  padding: 20rpx 0;
  text-align: center;
}

.list-hint text {
  color: #94a3b8;
  font-size: 24rpx;
  font-weight: 700;
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 22rpx;
}

.detail-section,
.approve-box,
.form-notice {
  padding: 24rpx;
  border: 1rpx solid rgba(226, 232, 240, 0.82);
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.74);
}

.section-title-row {
  display: flex;
  align-items: center;
  gap: 10rpx;
  margin-bottom: 18rpx;
}

.section-title-row text {
  color: #1e293b;
  font-size: 27rpx;
  font-weight: 950;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14rpx;
}

.detail-item {
  min-width: 0;
  padding: 16rpx;
  border-radius: 18rpx;
  background: #f8fafc;
}

.detail-label {
  color: #94a3b8;
  font-size: 21rpx;
  font-weight: 800;
}

.detail-value {
  overflow: hidden;
  margin-top: 8rpx;
  color: #334155;
  font-size: 24rpx;
  font-weight: 850;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.form-notice {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
  background: #fffbeb;
}

.form-notice text {
  flex: 1;
  color: #92400e;
  font-size: 24rpx;
  font-weight: 750;
  line-height: 1.5;
}

.variable-list {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.variable-row {
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding: 14rpx 16rpx;
  border-radius: 16rpx;
  background: #f8fafc;
}

.variable-key {
  width: 210rpx;
  overflow: hidden;
  color: #64748b;
  font-size: 22rpx;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.variable-value {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  color: #1e293b;
  font-size: 23rpx;
  font-weight: 850;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.timeline {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.timeline-row {
  display: flex;
  gap: 16rpx;
}

.timeline-dot {
  width: 18rpx;
  height: 18rpx;
  flex: 0 0 18rpx;
  margin-top: 9rpx;
  border: 4rpx solid #dbeafe;
  border-radius: 999rpx;
  background: #2563eb;
}

.timeline-content {
  min-width: 0;
  flex: 1;
}

.timeline-title {
  color: #334155;
  font-size: 25rpx;
  font-weight: 900;
}

.timeline-desc {
  margin-top: 6rpx;
  color: #94a3b8;
  font-size: 22rpx;
  font-weight: 700;
}

.timeline-comment {
  margin-top: 10rpx;
  padding: 12rpx 14rpx;
  border-radius: 16rpx;
  color: #475569;
  font-size: 23rpx;
  font-weight: 700;
  line-height: 1.5;
  background: #f8fafc;
}

.timeline-empty {
  padding: 24rpx;
  border-radius: 18rpx;
  background: #f8fafc;
  text-align: center;
}

.timeline-empty text {
  color: #94a3b8;
  font-size: 24rpx;
  font-weight: 700;
}

.comment-input {
  width: 100%;
  height: 168rpx;
  padding: 20rpx;
  border: 1rpx solid rgba(203, 213, 225, 0.8);
  border-radius: 22rpx;
  color: #334155;
  font-size: 26rpx;
  font-weight: 700;
  line-height: 1.5;
  background: #ffffff;
  box-sizing: border-box;
}

.detail-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18rpx;
}

.detail-actions :deep(.ai-button:only-child) {
  grid-column: 1 / -1;
}

.animate-in {
  animation: enterUp 0.48s ease both;
}

.delay-1 {
  animation-delay: 0.06s;
}

.delay-2 {
  animation-delay: 0.12s;
}

.delay-3 {
  animation-delay: 0.18s;
}

@keyframes enterUp {
  from {
    opacity: 0;
    transform: translateY(24rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
