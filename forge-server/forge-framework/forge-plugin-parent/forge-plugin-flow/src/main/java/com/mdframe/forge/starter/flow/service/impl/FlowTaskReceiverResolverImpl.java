package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.flow.entity.FlowTask;
import com.mdframe.forge.starter.flow.service.FlowOrgIntegrationService;
import com.mdframe.forge.starter.flow.service.FlowTaskReceiverResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 默认流程任务消息接收人解析器。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowTaskReceiverResolverImpl implements FlowTaskReceiverResolver {

    private final FlowOrgIntegrationService flowOrgIntegrationService;

    @Override
    public Set<Long> resolveReceivers(FlowTask flowTask) {
        Set<Long> receiverIds = new HashSet<>();
        if (flowTask == null) {
            return receiverIds;
        }
        addUserId(receiverIds, flowTask.getAssignee());
        addUserIds(receiverIds, flowTask.getCandidateUsers());
        if (receiverIds.isEmpty()) {
            addCandidateGroupUsers(receiverIds, flowTask.getCandidateGroups());
        }
        return receiverIds;
    }

    private void addUserIds(Set<Long> receiverIds, String csv) {
        if (csv == null || csv.isBlank()) {
            return;
        }
        for (String item : csv.split(",")) {
            addUserId(receiverIds, item);
        }
    }

    private void addUserId(Set<Long> receiverIds, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        try {
            receiverIds.add(Long.parseLong(raw.trim()));
        } catch (NumberFormatException e) {
            log.warn("流程任务消息接收人不是数值用户ID，已跳过: raw={}", raw);
        }
    }

    private void addCandidateGroupUsers(Set<Long> receiverIds, String candidateGroups) {
        if (candidateGroups == null || candidateGroups.isBlank()) {
            return;
        }
        for (String rawGroup : candidateGroups.split(",")) {
            String group = rawGroup == null ? null : rawGroup.trim();
            if (group == null || group.isEmpty()) {
                continue;
            }
            List<String> userIds = resolveGroupUsers(group);
            for (String userId : userIds) {
                addUserId(receiverIds, userId);
            }
        }
    }

    private List<String> resolveGroupUsers(String group) {
        try {
            List<String> roleUsers = isNumeric(group)
                    ? flowOrgIntegrationService.getUserIdsByRoleId(group)
                    : flowOrgIntegrationService.getUserIdsByRoleCode(group);
            if (roleUsers != null && !roleUsers.isEmpty()) {
                return roleUsers;
            }
            if (isNumeric(group)) {
                List<String> deptUsers = flowOrgIntegrationService.getUserIdsByDeptId(group);
                if (deptUsers != null) {
                    return deptUsers;
                }
            }
        } catch (Exception e) {
            log.warn("解析流程任务候选组用户失败: group={}", group, e);
        }
        return List.of();
    }

    private boolean isNumeric(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
