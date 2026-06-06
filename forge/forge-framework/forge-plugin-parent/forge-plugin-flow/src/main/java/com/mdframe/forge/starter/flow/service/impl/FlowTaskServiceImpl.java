package com.mdframe.forge.starter.flow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.message.service.MessageService;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.service.ISysUserService;
import com.mdframe.forge.starter.flow.dto.ProcessDiagramInfo;
import com.mdframe.forge.starter.flow.dto.ProcessNodeInfo;
import com.mdframe.forge.starter.flow.dto.TaskFormInfo;
import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import com.mdframe.forge.starter.flow.entity.FlowErrorLog;
import com.mdframe.forge.starter.flow.entity.FlowForm;
import com.mdframe.forge.starter.flow.entity.FlowFormInstance;
import com.mdframe.forge.starter.flow.entity.FlowModel;
import com.mdframe.forge.starter.flow.entity.FlowNodeConfig;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper;
import com.mdframe.forge.starter.flow.mapper.FlowFormInstanceMapper;
import com.mdframe.forge.starter.flow.mapper.FlowTaskMapper;
import com.mdframe.forge.starter.flow.service.FlowErrorLogService;
import com.mdframe.forge.starter.flow.service.FlowFormService;
import com.mdframe.forge.starter.flow.service.FlowModelService;
import com.mdframe.forge.starter.flow.service.FlowNodeConfigService;
import com.mdframe.forge.starter.flow.service.FlowOrgIntegrationService;
import com.mdframe.forge.starter.flow.service.FlowTaskService;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.bpmn.model.UserTask;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 流程任务服务实现
 */
@Slf4j
@Service
public class FlowTaskServiceImpl extends ServiceImpl<FlowTaskMapper, FlowTask> implements FlowTaskService {

    private static final String FLOWABLE_NS = "http://flowable.org/bpmn";
    private static final String ACTION_APPROVE = "approve";
    private static final String ACTION_REJECT = "reject";
    private static final String ACTION_DELEGATE = "delegate";
    private static final String ACTION_RETURN = "return";
    private static final String ACTION_TERMINATE = "terminate";

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ProcessEngineConfiguration processEngineConfiguration;

    /**
     * 消息服务（可选注入）
     */
    @Autowired(required = false)
    private MessageService messageService;
    
    /**
     * 组织架构集成服务（可选注入）
     */
    @Autowired(required = false)
    private FlowOrgIntegrationService flowOrgIntegrationService;

    /**
     * 流程模型服务
     */
    @Autowired
    private FlowModelService flowModelService;

    /**
     * 流程节点配置服务
     */
    @Autowired
    private FlowNodeConfigService flowNodeConfigService;

    /**
     * 流程业务Mapper
     */
    @Autowired
    private FlowBusinessMapper flowBusinessMapper;
    
    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private FlowErrorLogService flowErrorLogService;

    @Autowired(required = false)
    private FlowFormService flowFormService;

    @Autowired(required = false)
    private FlowFormInstanceMapper flowFormInstanceMapper;

@Override
    public IPage<FlowTask> todoTasks(Page<FlowTask> page, String userId, String title, String category, Integer status) {
        return this.getBaseMapper().selectTodoTasks(page, userId, title, category, status);
    }

    @Override
    public IPage<FlowTask> doneTasks(Page<FlowTask> page, String userId, String title, String category, Integer status) {
        return this.getBaseMapper().selectDoneTasks(page, userId, title, category, status);
    }

    @Override
    public IPage<FlowTask> startedTasks(Page<FlowTask> page, String userId, String title, String category, Integer status) {
        return this.getBaseMapper().selectStartedTasks(page, userId, title, category, status);
    }

    @Override
    public IPage<FlowTask> candidateTasks(Page<FlowTask> page, String userId, String groupId, String title) {
        // 1. 查询 Flowable 候选任务
        List<Task> flowableTasks = new ArrayList<>();
        
        // 按候选人查询
        if (userId != null && !userId.isEmpty()) {
            List<Task> userCandidateTasks = taskService.createTaskQuery()
                    .taskCandidateUser(userId)
                    .taskUnassigned()
                    .list();
            flowableTasks.addAll(userCandidateTasks);
        }
        
        // 按候选组查询
        if (groupId != null && !groupId.isEmpty()) {
            List<Task> groupCandidateTasks = taskService.createTaskQuery()
                    .taskCandidateGroup(groupId)
                    .taskUnassigned()
                    .list();
            // 去重合并
            Set<String> existingTaskIds = flowableTasks.stream()
                    .map(Task::getId)
                    .collect(Collectors.toSet());
            for (Task task : groupCandidateTasks) {
                if (!existingTaskIds.contains(task.getId())) {
                    flowableTasks.add(task);
                }
            }
        }
        
        // 2. 获取任务ID列表
        if (flowableTasks.isEmpty()) {
            return page; // 返回空页
        }
        
        List<String> taskIds = flowableTasks.stream()
                .map(Task::getId)
                .collect(Collectors.toList());
        
        // 3. 从本地表查询任务详情（带分页）
        LambdaQueryWrapper<FlowTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(FlowTask::getTaskId, taskIds)
                .like(title != null, FlowTask::getTitle, title)
                .orderByDesc(FlowTask::getCreateTime);
        
        return page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claimTask(String taskId, String userId) {
        taskService.claim(taskId, userId);
        
        FlowTask task = new FlowTask();
        task.setTaskId(taskId);
        task.setAssignee(userId);
        task.setStatus(1);
        task.setClaimTime(LocalDateTime.now());
        
        lambdaUpdate().eq(FlowTask::getTaskId, taskId).update(task);
        log.info("签收任务：taskId={}, userId={}", taskId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(String taskId, String userId, String comment, String signature, Map<String, Object> variables) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在或已处理");
        }
        validateTaskAction(task, ACTION_APPROVE, comment, signature);

        try {
            if (comment != null && !comment.isEmpty()) {
                taskService.addComment(taskId, task.getProcessInstanceId(), comment);
            }

            Map<String, Object> completeVariables = mergeActionVariables(variables, true);
            completeTask(task, completeVariables);

            FlowTask flowTask = new FlowTask();
            flowTask.setStatus(2);
            flowTask.setComment(comment);
            flowTask.setSignature(signature);
            flowTask.setCompleteTime(LocalDateTime.now());
            lambdaUpdate().eq(FlowTask::getTaskId, taskId).update(flowTask);

            log.info("审批通过：taskId={}, userId={}", taskId, userId);
        } catch (Exception e) {
            recordTaskError(task.getProcessInstanceId(), taskId, task.getTaskDefinitionKey(),
                    task.getName(), "TASK_APPROVE", e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(String taskId, String userId, String comment, String signature) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在或已处理");
        }
        validateTaskAction(task, ACTION_REJECT, comment, signature);

        try {
            if (comment != null && !comment.isEmpty()) {
                taskService.addComment(taskId, task.getProcessInstanceId(), comment);
            }

            completeTask(task, mergeActionVariables(null, false));

            FlowTask flowTask = new FlowTask();
            flowTask.setStatus(3);
            flowTask.setComment(comment);
            flowTask.setSignature(signature);
            flowTask.setCompleteTime(LocalDateTime.now());
            lambdaUpdate().eq(FlowTask::getTaskId, taskId).update(flowTask);

            log.info("审批驳回：taskId={}, userId={}", taskId, userId);
        } catch (Exception e) {
            recordTaskError(task.getProcessInstanceId(), taskId, task.getTaskDefinitionKey(),
                    task.getName(), "TASK_REJECT", e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delegate(String taskId, String userId, String targetUserId, String comment, String signature) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在或已处理");
        }
        validateTaskAction(task, ACTION_DELEGATE, comment, signature);

        try {
            String owner = task.getAssignee() != null && !task.getAssignee().isEmpty()
                    ? task.getAssignee()
                    : userId;
            if (owner != null && !owner.isEmpty()) {
                taskService.setOwner(taskId, owner);
            }
            taskService.setAssignee(taskId, targetUserId);

            FlowTask flowTask = new FlowTask();
            flowTask.setStatus(0);
            flowTask.setComment(comment);
            flowTask.setSignature(signature);
            flowTask.setAssignee(targetUserId);
            flowTask.setOwner(owner);
            lambdaUpdate().eq(FlowTask::getTaskId, taskId).update(flowTask);

            log.info("转办任务：taskId={}, from={}, to={}", taskId, userId, targetUserId);
        } catch (Exception e) {
            recordTaskError(task.getProcessInstanceId(), taskId, task.getTaskDefinitionKey(),
                    task.getName(), "TASK_DELEGATE", e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnTask(String taskId, String userId, String comment, String signature) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在或已处理");
        }
        validateTaskAction(task, ACTION_RETURN, comment, signature);

        try {
            String targetActivityId = findPreviousUserTaskActivityId(task);
            if (targetActivityId == null || targetActivityId.isEmpty()) {
                throw new RuntimeException("当前任务没有可退回的上一审批节点");
            }

            if (comment != null && !comment.isEmpty()) {
                taskService.addComment(taskId, task.getProcessInstanceId(), "退回：" + comment);
            }

            List<String> currentActivityIds = runtimeService.getActiveActivityIds(task.getProcessInstanceId());
            if (currentActivityIds == null || currentActivityIds.isEmpty()) {
                currentActivityIds = Collections.singletonList(task.getTaskDefinitionKey());
            }
            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(task.getProcessInstanceId())
                    .moveActivityIdsToSingleActivityId(currentActivityIds, targetActivityId)
                    .changeState();

            FlowTask flowTask = new FlowTask();
            flowTask.setStatus(7);
            flowTask.setComment(comment);
            flowTask.setSignature(signature);
            flowTask.setCompleteTime(LocalDateTime.now());
            lambdaUpdate().eq(FlowTask::getTaskId, taskId).update(flowTask);

            log.info("退回任务：taskId={}, userId={}, targetActivityId={}", taskId, userId, targetActivityId);
        } catch (Exception e) {
            recordTaskError(task.getProcessInstanceId(), taskId, task.getTaskDefinitionKey(),
                    task.getName(), "TASK_RETURN", e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void terminateTask(String taskId, String userId, String comment, String signature) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在或已处理");
        }
        validateTaskAction(task, ACTION_TERMINATE, comment, signature);

        try {
            String reason = comment != null && !comment.isBlank() ? comment : "审批人终结流程";
            taskService.addComment(taskId, task.getProcessInstanceId(), "终结流程：" + reason);
            runtimeService.deleteProcessInstance(task.getProcessInstanceId(), reason);

            FlowBusiness business = flowBusinessMapper.selectByProcessInstanceId(task.getProcessInstanceId());
            if (business != null) {
                business.setStatus("terminated");
                business.setEndTime(LocalDateTime.now());
                business.setUpdateTime(LocalDateTime.now());
                flowBusinessMapper.updateById(business);
            }

            FlowTask flowTask = new FlowTask();
            flowTask.setStatus(8);
            flowTask.setComment(comment);
            flowTask.setSignature(signature);
            flowTask.setCompleteTime(LocalDateTime.now());
            lambdaUpdate().eq(FlowTask::getTaskId, taskId).update(flowTask);

            log.info("审批人终结流程：taskId={}, processInstanceId={}, userId={}",
                    taskId, task.getProcessInstanceId(), userId);
        } catch (Exception e) {
            recordTaskError(task.getProcessInstanceId(), taskId, task.getTaskDefinitionKey(),
                    task.getName(), "TASK_TERMINATE", e);
            throw e;
        }
    }

    private Map<String, Object> mergeActionVariables(Map<String, Object> variables, boolean approved) {
        Map<String, Object> completeVariables = variables != null ? new HashMap<>(variables) : new HashMap<>();
        completeVariables.put("approved", approved);
        completeVariables.put("approvalResult", approved ? "approve" : "reject");
        return completeVariables;
    }

    /**
     * Flowable 委派态任务不能直接 complete，需要先 resolve。
     */
    private void completeTask(Task task, Map<String, Object> variables) {
        String taskId = task.getId();
        if (DelegationState.PENDING.equals(task.getDelegationState())) {
            log.info("任务处于委派待解决状态，先 resolve 再 complete：taskId={}, assignee={}, owner={}",
                    taskId, task.getAssignee(), task.getOwner());
            taskService.resolveTask(taskId);
        }

        if (variables != null && !variables.isEmpty()) {
            taskService.complete(taskId, variables);
        } else {
            taskService.complete(taskId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delegateTask(String taskId, String userId, String delegateUserId, String comment) {
        delegate(taskId, userId, delegateUserId, comment, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(String processInstanceId, String userId) {
        try {
            runtimeService.deleteProcessInstance(processInstanceId, "用户撤回");

            FlowTask flowTask = new FlowTask();
            flowTask.setStatus(6);
            flowTask.setCompleteTime(LocalDateTime.now());
            lambdaUpdate().eq(FlowTask::getProcessInstanceId, processInstanceId).update(flowTask);

            log.info("撤回流程：processInstanceId={}, userId={}", processInstanceId, userId);
        } catch (Exception e) {
            FlowErrorLog errorLog = new FlowErrorLog();
            errorLog.setProcessInstanceId(processInstanceId);
            errorLog.setErrorStage("TASK_WITHDRAW");
            flowErrorLogService.recordError(errorLog, e);
            throw e;
        }
    }

    @Override
    public FlowTask getTaskDetail(String taskId) {
        return lambdaQuery().eq(FlowTask::getTaskId, taskId).one();
    }

    @Override
    public byte[] getProcessDiagram(String processInstanceId) {
        try {
            // 1. 获取流程实例
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            
            if (historicProcessInstance == null) {
                log.warn("流程实例不存在：{}", processInstanceId);
                return null;
            }
            
            String processDefinitionId = historicProcessInstance.getProcessDefinitionId();
            log.info("获取流程图：processInstanceId={}, processDefinitionId={}", processInstanceId, processDefinitionId);
            
            // 2. 获取BPMN模型
            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
            if (bpmnModel == null) {
                log.warn("BPMN模型不存在：{}", processDefinitionId);
                return null;
            }
            
            // 打印BPMN模型信息
            log.info("BPMN模型进程数: {}", bpmnModel.getProcesses() != null ? bpmnModel.getProcesses().size() : 0);
            log.info("BPMN模型LocationMap大小: {}", bpmnModel.getLocationMap() != null ? bpmnModel.getLocationMap().size() : 0);
            log.info("BPMN模型FlowLocationMap大小: {}", bpmnModel.getFlowLocationMap() != null ? bpmnModel.getFlowLocationMap().size() : 0);
            
            // 3. 检查BPMN模型是否有图形信息
            if (!hasGraphicInfo(bpmnModel)) {
                log.info("BPMN模型没有图形坐标信息，尝试从部署资源获取原始流程图");
                return getDiagramFromResource(processDefinitionId);
            }
            
            // 4. 获取已完成的历史活动节点
            List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .finished()
                    .orderByHistoricActivityInstanceStartTime()
                    .asc()
                    .list();
            
            // 已完成的节点ID列表
            List<String> completedActivityIds = historicActivityInstances.stream()
                    .map(HistoricActivityInstance::getActivityId)
                    .distinct()
                    .collect(Collectors.toList());
            
            log.info("已完成节点数量: {}, 节点ID: {}", completedActivityIds.size(), completedActivityIds);
            
            // 5. 获取当前活动节点（运行中）
            List<String> currentActivityIds = new ArrayList<>();
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            
            if (processInstance != null) {
                // 流程还在运行中，获取当前活动节点
                currentActivityIds = runtimeService.getActiveActivityIds(processInstanceId);
                log.info("当前活动节点数量: {}, 节点ID: {}", currentActivityIds.size(), currentActivityIds);
            }
            
            // 6. 使用流程图生成器生成图片
            ProcessDiagramGenerator diagramGenerator = processEngineConfiguration.getProcessDiagramGenerator();
            
            if (diagramGenerator == null) {
                log.error("ProcessDiagramGenerator 未配置");
                return null;
            }
            
            // 设置字体（使用系统默认字体，避免字体不存在的问题）
            String activityFontName = "SansSerif";
            String labelFontName = "SansSerif";
            String annotationFontName = "SansSerif";
            
            log.info("开始生成流程图，字体: {}", activityFontName);
            
            // 生成流程图输入流（高亮已完成和当前节点）
            InputStream diagramStream = diagramGenerator.generateDiagram(
                    bpmnModel,
                    "png",
                    completedActivityIds,    // 高亮已完成节点（绿色）
                    currentActivityIds,      // 高亮当前节点（红色）
                    activityFontName,
                    labelFontName,
                    annotationFontName,
                    null,
                    1.0,
                    true
            );
            
            // 7. 转换为字节数组
            if (diagramStream != null) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = diagramStream.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                diagramStream.close();
                log.info("流程图生成成功，大小: {} bytes", output.size());
                return output.toByteArray();
            }
            
            log.warn("流程图生成返回空流");
            return null;
            
        } catch (Exception e) {
            log.error("生成流程图失败：processInstanceId={}, 错误: {}", processInstanceId, e.getMessage(), e);
            throw new RuntimeException("生成流程图失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取流程定义的 BPMN XML
     */
    private String getBpmnXml(ProcessDefinition processDefinition) {
        try {
            // 获取 BpmnModel
            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
            if (bpmnModel == null) {
                log.warn("BPMN模型不存在：{}", processDefinition.getId());
                return null;
            }
            
            // 使用 Flowable 的 XML 导出功能
            byte[] bpmnBytes = new org.flowable.bpmn.converter.BpmnXMLConverter()
                    .convertToXML(bpmnModel);
            
            return new String(bpmnBytes, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("获取BPMN XML失败：processDefinitionId={}", processDefinition.getId(), e);
            return null;
        }
    }
    
    /**
     * 检查BPMN模型是否有图形信息
     */
    private boolean hasGraphicInfo(BpmnModel bpmnModel) {
        if (bpmnModel.getLocationMap() == null || bpmnModel.getLocationMap().isEmpty()) {
            return false;
        }
        // 检查是否有有效的坐标信息
        for (org.flowable.bpmn.model.GraphicInfo graphicInfo : bpmnModel.getLocationMap().values()) {
            if (graphicInfo != null && graphicInfo.getX() >= 0 && graphicInfo.getY() >= 0) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 从资源获取原始流程图（无高亮）
     */
    private byte[] getDiagramFromResource(String processDefinitionId) {
        try {
            // 获取流程定义
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(processDefinitionId)
                    .singleResult();
            
            if (processDefinition == null) {
                log.warn("流程定义不存在：{}", processDefinitionId);
                return null;
            }
            
            log.info("流程定义信息：id={}, name={}, deploymentId={}, diagramResourceName={}",
                    processDefinition.getId(),
                    processDefinition.getName(),
                    processDefinition.getDeploymentId(),
                    processDefinition.getDiagramResourceName());
            
            // 获取流程图资源
            String diagramResourceName = processDefinition.getDiagramResourceName();
            if (diagramResourceName != null && !diagramResourceName.isEmpty()) {
                log.info("尝试从部署资源获取流程图：{}", diagramResourceName);
                try {
                    // 从资源流获取流程图
                    InputStream diagramStream = repositoryService.getResourceAsStream(
                            processDefinition.getDeploymentId(), diagramResourceName);
                    
                    if (diagramStream != null) {
                        // 转换为字节数组
                        ByteArrayOutputStream output = new ByteArrayOutputStream();
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = diagramStream.read(buffer)) != -1) {
                            output.write(buffer, 0, bytesRead);
                        }
                        diagramStream.close();
                        log.info("成功从部署资源获取流程图，大小：{} bytes", output.size());
                        return output.toByteArray();
                    }
                } catch (Exception e) {
                    log.warn("从部署资源获取流程图失败：{}", e.getMessage());
                }
            }
            
            // 如果没有流程图资源，尝试从 BPMN XML 重新生成
            log.info("尝试从 BPMN XML 资源重新生成流程图");
            return generateDiagramFromBpmnXml(processDefinition);
            
        } catch (Exception e) {
            log.error("从资源获取流程图失败：processDefinitionId={}", processDefinitionId, e);
            return null;
        }
    }
    
    /**
     * 从 BPMN XML 资源重新生成流程图
     */
    private byte[] generateDiagramFromBpmnXml(ProcessDefinition processDefinition) {
        try {
            // 获取 BPMN XML 资源名称
            String resourceName = processDefinition.getResourceName();
            log.info("BPMN XML 资源名称：{}", resourceName);
            
            // 获取 BPMN XML 内容
            InputStream bpmnStream = repositoryService.getResourceAsStream(
                    processDefinition.getDeploymentId(), resourceName);
            
            if (bpmnStream == null) {
                log.warn("无法获取 BPMN XML 资源：{}", resourceName);
                return null;
            }
            
            // 读取 BPMN XML 内容
            ByteArrayOutputStream bpmnOutput = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = bpmnStream.read(buffer)) != -1) {
                bpmnOutput.write(buffer, 0, bytesRead);
            }
            bpmnStream.close();
            
            String bpmnXml = bpmnOutput.toString("UTF-8");
            log.info("BPMN XML 内容长度：{}", bpmnXml.length());
            log.info("BPMN XML 是否包含 BPMNDiagram：{}", bpmnXml.contains("BPMNDiagram"));
            
            // 使用 BpmnXMLConverter 解析（第三个参数 true 表示解析图形信息）
            BpmnModel bpmnModel = new org.flowable.bpmn.converter.BpmnXMLConverter()
                    .convertToBpmnModel(
                            new org.flowable.common.engine.impl.util.io.BytesStreamSource(
                                    bpmnXml.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                            false,
                            true);
            
            log.info("重新解析后的 LocationMap 大小：{}",
                    bpmnModel.getLocationMap() != null ? bpmnModel.getLocationMap().size() : 0);
            
            // 检查是否有图形信息
            if (bpmnModel.getLocationMap() == null || bpmnModel.getLocationMap().isEmpty()) {
                log.warn("BPMN XML 中没有图形坐标信息");
                return null;
            }
            
            // 使用流程图生成器生成图片
            ProcessDiagramGenerator diagramGenerator = processEngineConfiguration.getProcessDiagramGenerator();
            
            // 设置中文字体
            String activityFontName = "宋体";
            String labelFontName = "宋体";
            String annotationFontName = "宋体";
            
            // 生成流程图（无高亮）
            InputStream diagramStream = diagramGenerator.generateDiagram(
                    bpmnModel,
                    "png",
                    java.util.Collections.emptyList(),
                    java.util.Collections.emptyList(),
                    activityFontName,
                    labelFontName,
                    annotationFontName,
                    null,
                    1.0,
                    true
            );
            
            if (diagramStream != null) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                buffer = new byte[4096];
                while ((bytesRead = diagramStream.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                diagramStream.close();
                log.info("成功从 BPMN XML 生成流程图，大小：{} bytes", output.size());
                return output.toByteArray();
            }
            
            return null;
        } catch (Exception e) {
            log.error("从 BPMN XML 生成流程图失败", e);
            return null;
        }
    }

    @Override
    public ProcessDiagramInfo getProcessDiagramInfo(String processInstanceId) {
        return getProcessDiagramInfo(processInstanceId, false);
    }

    @Override
    public ProcessDiagramInfo getProcessDiagramInfo(String processInstanceId, boolean includeImage) {
        try {
            log.info("开始获取流程图详情，processInstanceId: {}", processInstanceId);
            
            // 1. 获取流程实例
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            
            if (historicProcessInstance == null) {
                log.warn("流程实例不存在：{}", processInstanceId);
                return null;
            }
            
            String processDefinitionId = historicProcessInstance.getProcessDefinitionId();
            log.info("流程定义ID: {}", processDefinitionId);
            
            // 2. 获取BPMN模型
            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
            if (bpmnModel == null) {
                log.warn("BPMN模型不存在：{}", processDefinitionId);
                return null;
            }
            
            // 3. 构建返回结果
            ProcessDiagramInfo diagramInfo = new ProcessDiagramInfo();
            diagramInfo.setProcessInstanceId(processInstanceId);
            diagramInfo.setProcessDefinitionId(processDefinitionId);
            diagramInfo.setProcessName(historicProcessInstance.getName());
            diagramInfo.setStartUserId(historicProcessInstance.getStartUserId());
            diagramInfo.setStartTime(historicProcessInstance.getStartTime());
            diagramInfo.setEndTime(historicProcessInstance.getEndTime());
            
            // 获取发起人姓名
            String startUserId = historicProcessInstance.getStartUserId();
            if (startUserId != null && flowOrgIntegrationService != null) {
                try {
                    Map<String, Object> userInfo = flowOrgIntegrationService.getUserInfo(startUserId);
                    diagramInfo.setStartUserName((String)userInfo.get("realName"));
                    log.info("发起人姓名: {}", (String)userInfo.get("realName"));
                } catch (Exception e) {
                    log.warn("获取发起人姓名失败: {}", e.getMessage());
                }
            }
            
            // 判断流程状态
            if (historicProcessInstance.getEndTime() == null) {
                diagramInfo.setStatus("running");
            } else if (historicProcessInstance.getDeleteReason() != null) {
                diagramInfo.setStatus("terminated");
            } else {
                diagramInfo.setStatus("completed");
            }
            log.info("流程状态: {}", diagramInfo.getStatus());
            
            // 4. 获取 BPMN XML（用于前端 bpmn-js 渲染）
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(processDefinitionId)
                    .singleResult();
            if (processDefinition != null) {
                String bpmnXml = getBpmnXml(processDefinition);
                diagramInfo.setBpmnXml(bpmnXml);
                log.info("BPMN XML 长度: {}", bpmnXml != null ? bpmnXml.length() : 0);
            }
            
            // 5. 生成流程图图片（备用，默认关闭。BPMN XML 已足够前端渲染，避免每次生成 Base64 PNG 拖慢加载）
            if (includeImage) {
                byte[] diagramBytes = getProcessDiagram(processInstanceId);
                if (diagramBytes != null && diagramBytes.length > 0) {
                    String base64 = Base64.getEncoder().encodeToString(diagramBytes);
                    diagramInfo.setDiagramBase64("data:image/png;base64," + base64);
                    log.info("流程图图片大小: {} bytes, base64长度: {}", diagramBytes.length, base64.length());
                } else {
                    log.warn("未能生成流程图图片");
                }
            }
            
            // 6. 获取节点信息列表
            List<ProcessNodeInfo> nodes = buildNodeInfoList(bpmnModel, processInstanceId);
            diagramInfo.setNodes(nodes);
            log.info("节点数量: {}", nodes != null ? nodes.size() : 0);
            
            // 打印节点详情
            if (nodes != null && !nodes.isEmpty()) {
                for (ProcessNodeInfo node : nodes) {
                    log.info("节点: id={}, name={}, type={}, status={}, x={}, y={}",
                            node.getNodeId(), node.getNodeName(), node.getNodeType(),
                            node.getStatus(), node.getX(), node.getY());
                }
            }
            
            return diagramInfo;
            
        } catch (Exception e) {
            log.error("获取流程图详情失败：processInstanceId={}", processInstanceId, e);
            return null;
        }
    }
    
    /**
     * 构建节点信息列表
     */
    private List<ProcessNodeInfo> buildNodeInfoList(BpmnModel bpmnModel, String processInstanceId) {
        List<ProcessNodeInfo> nodeList = new ArrayList<>();
        
        // 获取流程中的所有节点
        org.flowable.bpmn.model.Process process = bpmnModel.getProcesses().get(0);
        if (process == null) {
            return nodeList;
        }
        
        // 获取已完成的历史活动
        Map<String, HistoricActivityInstance> completedActivityMap = new HashMap<>();
        List<HistoricActivityInstance> historicActivities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .list();
        
        for (HistoricActivityInstance activity : historicActivities) {
            if (activity.getEndTime() != null) {
                // 已完成的活动
                if (!completedActivityMap.containsKey(activity.getActivityId()) ||
                    completedActivityMap.get(activity.getActivityId()).getStartTime().before(activity.getStartTime())) {
                    completedActivityMap.put(activity.getActivityId(), activity);
                }
            }
        }
        
        // 获取当前活动节点
        Set<String> currentActivityIds = new HashSet<>();
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        
        if (processInstance != null) {
            currentActivityIds = new HashSet<>(runtimeService.getActiveActivityIds(processInstanceId));
        }
        
        // 获取历史任务信息（用于获取处理人）
        Map<String, HistoricTaskInstance> taskMap = new HashMap<>();
        List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .list();
        for (HistoricTaskInstance task : historicTasks) {
            if (!taskMap.containsKey(task.getTaskDefinitionKey()) ||
                taskMap.get(task.getTaskDefinitionKey()).getCreateTime().before(task.getCreateTime())) {
                taskMap.put(task.getTaskDefinitionKey(), task);
            }
        }
        
        // 获取当前任务
        Map<String, Task> currentTaskMap = new HashMap<>();
        List<Task> currentTasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();
        for (Task task : currentTasks) {
            currentTaskMap.put(task.getTaskDefinitionKey(), task);
        }
        
        // 遍历所有节点
        for (FlowNode flowNode : process.findFlowElementsOfType(FlowNode.class)) {
            ProcessNodeInfo nodeInfo = new ProcessNodeInfo();
            nodeInfo.setNodeId(flowNode.getId());
            nodeInfo.setNodeName(flowNode.getName());
            nodeInfo.setNodeType(flowNode.getClass().getSimpleName());
            
            // 获取图形信息
            GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(flowNode.getId());
            if (graphicInfo != null) {
                nodeInfo.setX(graphicInfo.getX());
                nodeInfo.setY(graphicInfo.getY());
                nodeInfo.setWidth(graphicInfo.getWidth());
                nodeInfo.setHeight(graphicInfo.getHeight());
            }
            
            // 设置节点状态
            if (currentActivityIds.contains(flowNode.getId())) {
                nodeInfo.setStatus("running");
            } else if (completedActivityMap.containsKey(flowNode.getId())) {
                nodeInfo.setStatus("completed");
                HistoricActivityInstance activity = completedActivityMap.get(flowNode.getId());
                nodeInfo.setStartTime(activity.getStartTime());
                nodeInfo.setEndTime(activity.getEndTime());
                if (activity.getDurationInMillis() != null) {
                    nodeInfo.setDuration(activity.getDurationInMillis());
                }
            } else {
                nodeInfo.setStatus("pending");
            }
            
            // 设置处理人信息（仅用户任务）
            if ("UserTask".equals(nodeInfo.getNodeType())) {
                // 先检查当前任务
                Task currentTask = currentTaskMap.get(flowNode.getId());
                if (currentTask != null) {
                    nodeInfo.setTaskId(currentTask.getId());
                    if (currentTask.getAssignee() != null) {
                        List<String> assigneeIds = Collections.singletonList(currentTask.getAssignee());
                        nodeInfo.setAssigneeIds(assigneeIds);
                        // 获取用户详情
                        fillUserInfo(nodeInfo, assigneeIds);
                    }
                    // 获取候选人信息
                    if (currentTask.getAssignee() == null) {
                        // 任务未签收，获取候选人 - 使用 TaskService 查询
                        List<org.flowable.identitylink.api.IdentityLink> identityLinks = taskService.getIdentityLinksForTask(currentTask.getId());
                        if (identityLinks != null && !identityLinks.isEmpty()) {
                            List<String> candidateUsers = identityLinks.stream()
                                .filter(link -> link.getUserId() != null && "candidate".equals(link.getType()))
                                .map(org.flowable.identitylink.api.IdentityLink::getUserId)
                                .distinct()
                                .collect(Collectors.toList());
                            if (!candidateUsers.isEmpty()) {
                                nodeInfo.setCandidateUserIds(candidateUsers);
                                fillUserInfo(nodeInfo, candidateUsers);
                            }
                        }
                    }
                } else {
                    // 检查历史任务
                    HistoricTaskInstance historicTask = taskMap.get(flowNode.getId());
                    if (historicTask != null) {
                        if (historicTask.getAssignee() != null) {
                            List<String> assigneeIds = Collections.singletonList(historicTask.getAssignee());
                            nodeInfo.setAssigneeIds(assigneeIds);
                            // 获取用户详情
                            fillUserInfo(nodeInfo, assigneeIds);
                        }
                        nodeInfo.setStartTime(historicTask.getCreateTime());
                        nodeInfo.setEndTime(historicTask.getEndTime());
                        if (historicTask.getDurationInMillis() != null) {
                            nodeInfo.setDuration(historicTask.getDurationInMillis());
                        }
                    }
                }
            }
            
            nodeList.add(nodeInfo);
        }
        
        return nodeList;
    }
    
    /**
     * 填充用户信息（姓名、组织）
     */
    private void fillUserInfo(ProcessNodeInfo nodeInfo, List<String> userIds) {
        if (flowOrgIntegrationService == null || userIds == null || userIds.isEmpty()) {
            return;
        }
        
        List<String> names = new ArrayList<>();
        List<String> orgs = new ArrayList<>();
        List<Map<String, Object>> details = new ArrayList<>();
        
        for (String userId : userIds) {
            try {
                Map<String, Object> userInfo = flowOrgIntegrationService.getUserInfo(userId);
                if (userInfo != null) {
                    // 获取用户名
                    String name = (String) userInfo.get("name");
                    if (name == null) {
                        name = (String) userInfo.get("nickname");
                    }
                    if (name == null) {
                        name = (String) userInfo.get("username");
                    }
                    if (name != null) {
                        names.add(name);
                    } else {
                        names.add(userId); // 如果没有名字，显示ID
                    }
                    
                    // 获取组织名称
                    String orgName = (String) userInfo.get("deptName");
                    if (orgName == null) {
                        orgName = (String) userInfo.get("orgName");
                    }
                    if (orgName != null) {
                        orgs.add(orgName);
                    }
                    
                    // 添加详情
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("userId", userId);
                    detail.put("name", name != null ? name : userId);
                    detail.put("orgName", orgName);
                    details.add(detail);
                } else {
                    names.add(userId);
                }
            } catch (Exception e) {
                log.warn("获取用户信息失败: userId={}", userId, e);
                names.add(userId);
            }
        }
        
        nodeInfo.setAssigneeNames(names);
        nodeInfo.setAssigneeOrgs(orgs);
        nodeInfo.setAssigneeDetails(details);
    }

    @Override
    public void remind(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            log.warn("催办失败：任务不存在，taskId={}", taskId);
            return;
        }
        
        log.info("催办任务：taskId={}, taskName={}", taskId, task.getName());
        
        // 发送催办消息通知
        if (messageService != null) {
            try {
                // 获取任务处理人
                String assignee = task.getAssignee();
                if (assignee == null || assignee.isEmpty()) {
                    // 如果任务未签收，尝试获取候选人
                    log.info("任务未签收，跳过消息通知：taskId={}", taskId);
                    return;
                }
                
                // 构建消息
                com.mdframe.forge.plugin.message.domain.dto.MessageSendRequestDTO request =
                    new com.mdframe.forge.plugin.message.domain.dto.MessageSendRequestDTO();
                request.setTitle("流程催办提醒");
                request.setContent(String.format(
                    "您有一个待办任务需要处理：%s，请及时处理。",
                    task.getName()
                ));
                request.setType("SYSTEM");
                request.setChannel("WEB");
                request.setSendScope("USERS");
                
                // 设置接收人
                Set<Long> userIds = new HashSet<>();
                try {
                    userIds.add(Long.parseLong(assignee));
                } catch (NumberFormatException e) {
                    log.warn("无法解析处理人ID：{}", assignee);
                    return;
                }
                request.setUserIds(userIds);
                
                // 发送消息
                messageService.send(request);
                log.info("催办消息发送成功：taskId={}, assignee={}", taskId, assignee);
                
            } catch (Exception e) {
                log.error("发送催办消息失败：taskId={}", taskId, e);
            }
        } else {
            log.warn("消息服务未启用，无法发送催办通知");
        }
    }

    private void validateTaskAction(Task task, String action, String comment, String signature) {
        TaskApprovalPolicy policy = getTaskApprovalPolicy(task);
        if (!policy.isAllowed(action)) {
            throw new RuntimeException("当前节点不允许执行该审批操作");
        }
        if (policy.requireComment && isBlank(comment)) {
            throw new RuntimeException("请输入审批意见");
        }
        if (policy.requireSignature && isBlank(signature)) {
            throw new RuntimeException("请完成审批签名");
        }
    }

    private TaskApprovalPolicy getTaskApprovalPolicy(Task task) {
        TaskApprovalPolicy policy = TaskApprovalPolicy.defaultPolicy();
        FlowNode flowNode = getFlowNode(task);
        if (flowNode != null) {
            applyBpmnPolicy(policy, flowNode);
        }

        FlowModel flowModel = flowModelService.getModelByKey(extractProcessKey(task.getProcessDefinitionId()));
        if (flowModel != null) {
            FlowNodeConfig nodeConfig = flowNodeConfigService.getByModelAndNode(
                    flowModel.getId(), task.getTaskDefinitionKey());
            if (nodeConfig != null) {
                applyNodeConfigPolicy(policy, nodeConfig);
            }
        }
        return policy;
    }

    private FlowNode getFlowNode(Task task) {
        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        if (bpmnModel == null) {
            return null;
        }
        Process process = bpmnModel.getMainProcess();
        if (process == null) {
            return null;
        }
        FlowElement element = process.getFlowElement(task.getTaskDefinitionKey());
        return element instanceof FlowNode ? (FlowNode) element : null;
    }

    private void applyBpmnPolicy(TaskApprovalPolicy policy, FlowNode flowNode) {
        Boolean allowApprove = readBooleanFlowableAttribute(flowNode, "allowApprove");
        if (allowApprove != null) policy.allowApprove = allowApprove;
        Boolean allowReject = readBooleanFlowableAttribute(flowNode, "allowReject");
        if (allowReject != null) policy.allowReject = allowReject;
        Boolean allowDelegate = readBooleanFlowableAttribute(flowNode, "allowDelegate");
        if (allowDelegate != null) policy.allowDelegate = allowDelegate;
        Boolean allowReturn = readBooleanFlowableAttribute(flowNode, "allowReturn");
        if (allowReturn != null) policy.allowReturn = allowReturn;
        Boolean allowTerminate = readBooleanFlowableAttribute(flowNode, "allowTerminate");
        if (allowTerminate != null) policy.allowTerminate = allowTerminate;
        Boolean requireSignature = readBooleanFlowableAttribute(flowNode, "requireSignature");
        if (requireSignature != null) policy.requireSignature = requireSignature;
        Boolean requireComment = readBooleanFlowableAttribute(flowNode, "requireComment");
        if (requireComment != null) policy.requireComment = requireComment;
    }

    private void applyNodeConfigPolicy(TaskApprovalPolicy policy, FlowNodeConfig nodeConfig) {
        if (nodeConfig.getAllowApprove() != null) policy.allowApprove = nodeConfig.getAllowApprove();
        if (nodeConfig.getAllowReject() != null) policy.allowReject = nodeConfig.getAllowReject();
        if (nodeConfig.getAllowDelegate() != null) policy.allowDelegate = nodeConfig.getAllowDelegate();
        if (nodeConfig.getAllowReturn() != null) policy.allowReturn = nodeConfig.getAllowReturn();
        if (nodeConfig.getAllowTerminate() != null) policy.allowTerminate = nodeConfig.getAllowTerminate();
        if (nodeConfig.getRequireSignature() != null) policy.requireSignature = nodeConfig.getRequireSignature();
        if (nodeConfig.getRequireComment() != null) policy.requireComment = nodeConfig.getRequireComment();
    }

    private Boolean readBooleanFlowableAttribute(FlowNode flowNode, String name) {
        String value = flowNode.getAttributeValue(FLOWABLE_NS, name);
        if (isBlank(value)) {
            Map<String, List<ExtensionElement>> extensions = flowNode.getExtensionElements();
            List<ExtensionElement> elements = extensions != null ? extensions.get(name) : null;
            if (elements != null && !elements.isEmpty()) {
                value = elements.get(0).getElementText();
            }
        }
        return parseBooleanValue(value);
    }

    private Boolean parseBooleanValue(String value) {
        if (isBlank(value)) {
            return null;
        }
        String normalized = value.trim();
        if ("true".equalsIgnoreCase(normalized) || "1".equals(normalized)
                || "Y".equalsIgnoreCase(normalized) || "yes".equalsIgnoreCase(normalized)) {
            return true;
        }
        if ("false".equalsIgnoreCase(normalized) || "0".equals(normalized)
                || "N".equalsIgnoreCase(normalized) || "no".equalsIgnoreCase(normalized)) {
            return false;
        }
        return null;
    }

    private String findPreviousUserTaskActivityId(Task task) {
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .activityType("userTask")
                .finished()
                .orderByHistoricActivityInstanceEndTime()
                .desc()
                .list();
        if (activities == null || activities.isEmpty()) {
            return null;
        }
        for (HistoricActivityInstance activity : activities) {
            if (!Objects.equals(activity.getActivityId(), task.getTaskDefinitionKey())) {
                return activity.getActivityId();
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    public TaskFormInfo getTaskFormInfo(String taskId) {
        // 1. 获取任务信息
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在：" + taskId);
        }

        TaskFormInfo formInfo = new TaskFormInfo();
        formInfo.setTaskId(taskId);
        formInfo.setTaskName(task.getName());
        formInfo.setTaskDefKey(task.getTaskDefinitionKey());
        formInfo.setProcessInstanceId(task.getProcessInstanceId());

        // 2. 获取流程定义Key
        String processDefKey = extractProcessKey(task.getProcessDefinitionId());
        formInfo.setProcessDefKey(processDefKey);

        // 3. 获取流程变量
        Map<String, Object> variables = taskService.getVariables(taskId);
        formInfo.setVariables(variables);

        // 4. 获取业务信息
        FlowBusiness business = flowBusinessMapper.selectByProcessInstanceId(task.getProcessInstanceId());
        if (business != null) {
            formInfo.setBusinessKey(business.getBusinessKey());
            formInfo.setTitle(business.getTitle());
            formInfo.setStartUserId(business.getApplyUserId());
            formInfo.setStartUserName(business.getApplyUserName());
            formInfo.setStartDeptId(business.getApplyDeptId());
            formInfo.setStartDeptName(business.getApplyDeptName());
        }
        hydrateFormInstanceSnapshot(formInfo, task.getProcessInstanceId());

        // 5. 获取流程模型信息（全局表单配置）
        FlowModel flowModel = flowModelService.getModelByKey(processDefKey);
        
        // 6. 从BPMN中获取节点级别的表单配置
        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        FlowNode flowNode = null;
        if (bpmnModel != null) {
            Process process = bpmnModel.getMainProcess();
            if (process != null) {
                flowNode = (FlowNode) process.getFlowElement(task.getTaskDefinitionKey());
            }
        }
        
        // 检查节点是否有专属表单配置
        String nodeFormKey = null;
        String nodeFormJson = null;
        String nodeFormUrl = null;
        String nodeFormTarget = null;

        // Flowable BPMN 命名空间（flowable:xxx 属性存放的命名空间）
        final String FLOWABLE_NS = "http://flowable.org/bpmn";

        if (flowNode != null) {
            // 方式1：flowable:formKey 作为标准 UserTask.formKey 属性
            if (flowNode instanceof UserTask) {
                UserTask userTask = (UserTask) flowNode;
                nodeFormKey = userTask.getFormKey();
            }

            // 方式2：flowable:formUrl / formJson / formTarget 以 XML 属性方式写入
            //   例如 <bpmn:userTask flowable:formUrl="/leave/LeaveApproveForm">
            //   Flowable 解析后存入 BaseElement.attributes(namespace -> ExtensionAttribute)
            String attrFormUrl = flowNode.getAttributeValue(FLOWABLE_NS, "formUrl");
            String attrFormJson = flowNode.getAttributeValue(FLOWABLE_NS, "formJson");
            String attrFormTarget = flowNode.getAttributeValue(FLOWABLE_NS, "formTarget");
            if (attrFormUrl != null && !attrFormUrl.isEmpty()) nodeFormUrl = attrFormUrl;
            if (attrFormJson != null && !attrFormJson.isEmpty()) nodeFormJson = attrFormJson;
            if (attrFormTarget != null && !attrFormTarget.isEmpty()) nodeFormTarget = attrFormTarget;

            // 方式3：兼容旧有以子元素方式写入的情况
            //   例如 <flowable:formUrl>/leave/LeaveApproveForm</flowable:formUrl>
            Map<String, List<ExtensionElement>> extensions = flowNode.getExtensionElements();
            if (extensions != null) {
                List<ExtensionElement> formJsonElements = extensions.get("formJson");
                if (nodeFormJson == null && formJsonElements != null && !formJsonElements.isEmpty()) {
                    nodeFormJson = formJsonElements.get(0).getElementText();
                }
                List<ExtensionElement> formUrlElements = extensions.get("formUrl");
                if (nodeFormUrl == null && formUrlElements != null && !formUrlElements.isEmpty()) {
                    nodeFormUrl = formUrlElements.get(0).getElementText();
                }
                List<ExtensionElement> formTargetElements = extensions.get("formTarget");
                if (nodeFormTarget == null && formTargetElements != null && !formTargetElements.isEmpty()) {
                    nodeFormTarget = formTargetElements.get(0).getElementText();
                }
            }
        }
        
        // 确定表单类型和配置
        if (nodeFormUrl != null && !nodeFormUrl.isEmpty()) {
            // 节点配置了外置表单
            formInfo.setFormType("external");
            formInfo.setFormUrl(nodeFormUrl);
            formInfo.setFormTarget(nodeFormTarget != null ? nodeFormTarget : "modal");
        } else if (nodeFormKey != null || nodeFormJson != null) {
            // 节点配置了动态表单
            formInfo.setFormType("dynamic");
            formInfo.setFormKey(nodeFormKey);
            formInfo.setFormJson(resolveFormJson(nodeFormKey, nodeFormJson));
        } else if (flowModel != null) {
            // 使用全局表单配置
            String formType = flowModel.getFormType();
            formInfo.setFormType(formType);

            if ("dynamic".equals(formType)) {
                // 动态表单
                formInfo.setFormKey(flowModel.getFormId());
                formInfo.setFormJson(resolveModelFormJson(flowModel.getFormId(), flowModel.getFormJson()));
            } else if ("external".equals(formType)) {
                // 外部表单
                formInfo.setFormUrl(flowModel.getFormId());
            }
        } else {
            // 无表单
            formInfo.setFormType("none");
        }

        // 7. 获取节点办理配置（BPMN扩展属性 + 节点配置表，配置表优先）
        TaskApprovalPolicy policy = getTaskApprovalPolicy(task);
        formInfo.setAllowApprove(policy.allowApprove);
        formInfo.setAllowReject(policy.allowReject);
        formInfo.setAllowDelegate(policy.allowDelegate);
        formInfo.setAllowReturn(policy.allowReturn);
        formInfo.setAllowTerminate(policy.allowTerminate);
        formInfo.setRequireSignature(policy.requireSignature);
        formInfo.setRequireComment(policy.requireComment);
        formInfo.setAllowRejectToStart(true);

        log.info("获取任务表单信息：taskId={}, formType={}, formKey={}",
                taskId, formInfo.getFormType(), formInfo.getFormKey());

        return formInfo;
    }

    private void hydrateFormInstanceSnapshot(TaskFormInfo formInfo, String processInstanceId) {
        if (flowFormInstanceMapper == null || processInstanceId == null || processInstanceId.isEmpty()) {
            return;
        }
        try {
            FlowFormInstance instance = flowFormInstanceMapper.selectByProcessInstanceId(processInstanceId);
            if (instance == null) {
                return;
            }
            formInfo.setFormInstanceId(instance.getId());
            formInfo.setSchemaSnapshot(instance.getSchemaSnapshot());
            formInfo.setFormData(instance.getFormData());
            formInfo.setDataMode(instance.getDataMode());
            formInfo.setObjectCode(instance.getObjectCode());
            formInfo.setRecordId(instance.getRecordId());
            if (formInfo.getFormJson() == null || formInfo.getFormJson().isEmpty()) {
                formInfo.setFormJson(instance.getSchemaSnapshot());
            }
            if (formInfo.getFormKey() == null || formInfo.getFormKey().isEmpty()) {
                formInfo.setFormKey(instance.getFormKey());
            }
        } catch (Exception e) {
            log.warn("加载流程表单实例快照失败: processInstanceId={}", processInstanceId, e);
        }
    }

    private String resolveFormJson(String formKey, String inlineFormJson) {
        if (inlineFormJson != null && !inlineFormJson.isEmpty()) {
            return inlineFormJson;
        }
        if (formKey == null || formKey.isEmpty() || flowFormService == null) {
            return inlineFormJson;
        }
        try {
            return flowFormService.getFormSchema(formKey);
        } catch (Exception e) {
            log.warn("根据 formKey 获取动态表单失败：formKey={}", formKey, e);
            return inlineFormJson;
        }
    }

    private String resolveModelFormJson(String formId, String inlineFormJson) {
        if (inlineFormJson != null && !inlineFormJson.isEmpty()) {
            return inlineFormJson;
        }
        if (formId == null || formId.isEmpty() || flowFormService == null) {
            return inlineFormJson;
        }
        try {
            FlowForm form = flowFormService.getById(Long.valueOf(formId));
            return form != null ? form.getFormSchema() : inlineFormJson;
        } catch (NumberFormatException e) {
            return resolveFormJson(formId, inlineFormJson);
        } catch (Exception e) {
            log.warn("根据 formId 获取模型动态表单失败：formId={}", formId, e);
            return inlineFormJson;
        }
    }

    /**
     * 获取流程审批时间轴
     */
    @Override
    public List<Map<String, Object>> getProcessHistory(String processInstanceId) {
        // 1. 查询该流程实例的所有任务（按创建时间排序）
        LambdaQueryWrapper<FlowTask> wrapper = new LambdaQueryWrapper<FlowTask>()
                .eq(FlowTask::getProcessInstanceId, processInstanceId)
                .orderByAsc(FlowTask::getCreateTime);
        List<FlowTask> tasks = list(wrapper);

        // 2. 获取业务信息（用于展示发起节点）
        FlowBusiness business = flowBusinessMapper.selectByProcessInstanceId(processInstanceId);

        List<Map<String, Object>> result = new ArrayList<>();

        // 3. 加入“发起”节点
        if (business != null) {
            Map<String, Object> startNode = new HashMap<>();
            startNode.put("taskName", "发起流程");
            startNode.put("assigneeName", business.getApplyUserName());
            startNode.put("assigneeId", business.getApplyUserId());
            startNode.put("action", "start");
            startNode.put("comment", "");
            startNode.put("createTime", business.getApplyTime() != null
                    ? business.getApplyTime().toString() : business.getCreateTime() != null
                    ? business.getCreateTime().toString() : null);
            startNode.put("completeTime", startNode.get("createTime"));
            result.add(startNode);
        }

        // 4. 加入每个任务节点
        // 状态到 action 的映射
        Map<Integer, String> statusActionMap = new HashMap<>();
        statusActionMap.put(0, "pending");
        statusActionMap.put(1, "claim");
        statusActionMap.put(2, "approve");
        statusActionMap.put(3, "reject");
        statusActionMap.put(4, "delegate");
        statusActionMap.put(5, "delegate");
        statusActionMap.put(6, "withdraw");
        statusActionMap.put(7, "return");
        statusActionMap.put(8, "terminate");

        for (FlowTask task : tasks) {
            // 待办且未完成的节点也要展示（表示当前处理中）
            Map<String, Object> node = new HashMap<>();
            node.put("taskId", task.getTaskId());
            node.put("taskName", task.getTaskName());
            // 安全处理：assignee 可能为空
            String assigneeName = "";
            if (task.getAssignee() != null && !task.getAssignee().trim().isEmpty()) {
                try {
                    SysUser sysUser = sysUserService.selectUserById(Long.parseLong(task.getAssignee()));
                    assigneeName = sysUser != null ? sysUser.getRealName() : task.getAssignee();
                } catch (NumberFormatException e) {
                    assigneeName = task.getAssignee();
                }
            }
            node.put("assigneeName", assigneeName);
            node.put("assigneeId", task.getAssignee());
            node.put("action", statusActionMap.getOrDefault(task.getStatus(), "pending"));
            node.put("comment", task.getComment() != null ? task.getComment() : "");
            node.put("signature", task.getSignature());
            node.put("createTime", task.getCreateTime() != null ? task.getCreateTime().toString() : null);
            node.put("completeTime", task.getCompleteTime() != null ? task.getCompleteTime().toString() : null);
            result.add(node);
        }

        return result;
    }

    /**
     * 从流程定义ID提取流程Key
     */
    private String extractProcessKey(String processDefinitionId) {
        if (processDefinitionId == null) {
            return null;
        }
        // 格式：processKey:version:id
        String[] parts = processDefinitionId.split(":");
        return parts.length > 0 ? parts[0] : processDefinitionId;
    }

    private void recordTaskError(String processInstanceId, String taskId, String activityId,
                                  String activityName, String errorStage, Throwable e) {
        FlowErrorLog errorLog = new FlowErrorLog();
        errorLog.setProcessInstanceId(processInstanceId);
        errorLog.setTaskId(taskId);
        errorLog.setActivityId(activityId);
        errorLog.setActivityName(activityName);
        errorLog.setErrorStage(errorStage);
        flowErrorLogService.recordError(errorLog, e);
    }

    private static class TaskApprovalPolicy {
        private boolean allowApprove;
        private boolean allowReject;
        private boolean allowDelegate;
        private boolean allowReturn;
        private boolean allowTerminate;
        private boolean requireSignature;
        private boolean requireComment;

        private static TaskApprovalPolicy defaultPolicy() {
            TaskApprovalPolicy policy = new TaskApprovalPolicy();
            policy.allowApprove = true;
            policy.allowReject = true;
            policy.allowDelegate = true;
            policy.allowReturn = false;
            policy.allowTerminate = false;
            policy.requireSignature = false;
            policy.requireComment = true;
            return policy;
        }

        private boolean isAllowed(String action) {
            switch (action) {
                case ACTION_APPROVE:
                    return allowApprove;
                case ACTION_REJECT:
                    return allowReject;
                case ACTION_DELEGATE:
                    return allowDelegate;
                case ACTION_RETURN:
                    return allowReturn;
                case ACTION_TERMINATE:
                    return allowTerminate;
                default:
                    return false;
            }
        }
    }
}
