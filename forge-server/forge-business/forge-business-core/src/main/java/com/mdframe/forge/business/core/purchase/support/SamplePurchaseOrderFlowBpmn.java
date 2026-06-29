package com.mdframe.forge.business.core.purchase.support;

import lombok.experimental.UtilityClass;

/**
 * 采购单审批测试流程 BPMN。
 */
@UtilityClass
public class SamplePurchaseOrderFlowBpmn {

    public static String build() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <bpmn:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                  xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                                  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                                  xmlns:flowable="http://flowable.org/bpmn"
                                  id="Definitions_sample_purchase_order_approval"
                                  targetNamespace="http://forge.mdframe.com/bpmn">
                  <bpmn:process id="sample_purchase_order_approval" name="采购单审批测试流程" isExecutable="true">
                    <bpmn:startEvent id="StartEvent_1" name="提交采购单" flowable:initiator="initiator">
                      <bpmn:outgoing>Flow_001</bpmn:outgoing>
                    </bpmn:startEvent>
                    <bpmn:userTask id="dept_leader_approve" name="部门负责人审批" flowable:assignee="${deptLeaderId}" flowable:formKey="sample_purchase_order_approval_form" flowable:formFieldPermissions='[{"field":"arrivalListFileIds","label":"上传清单","readable":true,"writable":true,"required":false},{"field":"deptLeaderRemark","label":"部门负责人意见","readable":true,"writable":true,"required":false}]' flowable:allowApprove="true" flowable:allowReject="true" flowable:allowDelegate="true" flowable:allowReturn="false" flowable:allowTerminate="false" flowable:requireComment="true">
                      <bpmn:incoming>Flow_001</bpmn:incoming>
                      <bpmn:incoming>Flow_012</bpmn:incoming>
                      <bpmn:outgoing>Flow_002</bpmn:outgoing>
                    </bpmn:userTask>
                    <bpmn:exclusiveGateway id="Gateway_dept_result" name="部门负责人审批结果" default="Flow_003">
                      <bpmn:incoming>Flow_002</bpmn:incoming>
                      <bpmn:outgoing>Flow_003</bpmn:outgoing>
                      <bpmn:outgoing>Flow_004</bpmn:outgoing>
                    </bpmn:exclusiveGateway>
                    <bpmn:userTask id="engineering_manager_approve" name="工程部经理审批" flowable:assignee="${engineeringManagerId}" flowable:formKey="sample_purchase_order_approval_form" flowable:formFieldPermissions='[{"field":"engineeringManagerRemark","label":"工程部经理意见","readable":true,"writable":true,"required":false}]' flowable:allowApprove="true" flowable:allowReject="true" flowable:allowDelegate="true" flowable:allowReturn="false" flowable:allowTerminate="false" flowable:requireComment="true">
                      <bpmn:incoming>Flow_003</bpmn:incoming>
                      <bpmn:outgoing>Flow_005</bpmn:outgoing>
                    </bpmn:userTask>
                    <bpmn:exclusiveGateway id="Gateway_engineering_result" name="工程部经理审批结果" default="Flow_006">
                      <bpmn:incoming>Flow_005</bpmn:incoming>
                      <bpmn:outgoing>Flow_006</bpmn:outgoing>
                      <bpmn:outgoing>Flow_007</bpmn:outgoing>
                    </bpmn:exclusiveGateway>
                    <bpmn:userTask id="purchase_countersign" name="采购会签" flowable:assignee="${assignee}" flowable:formKey="sample_purchase_order_approval_form" flowable:formFieldPermissions='[{"field":"countersignRemark","label":"会签意见","readable":true,"writable":true,"required":false}]' flowable:allowApprove="true" flowable:allowReject="true" flowable:allowDelegate="true" flowable:allowReturn="false" flowable:allowTerminate="false" flowable:requireComment="true">
                      <bpmn:incoming>Flow_006</bpmn:incoming>
                      <bpmn:outgoing>Flow_008</bpmn:outgoing>
                      <bpmn:multiInstanceLoopCharacteristics isSequential="false" flowable:collection="${countersignUserList}" flowable:elementVariable="assignee">
                        <bpmn:completionCondition xsi:type="bpmn:tFormalExpression"><![CDATA[${approved == false || nrOfCompletedInstances == nrOfInstances}]]></bpmn:completionCondition>
                      </bpmn:multiInstanceLoopCharacteristics>
                    </bpmn:userTask>
                    <bpmn:exclusiveGateway id="Gateway_countersign_result" name="会签审批结果" default="Flow_009">
                      <bpmn:incoming>Flow_008</bpmn:incoming>
                      <bpmn:outgoing>Flow_009</bpmn:outgoing>
                      <bpmn:outgoing>Flow_010</bpmn:outgoing>
                    </bpmn:exclusiveGateway>
                    <bpmn:userTask id="applicant_modify" name="申请人修改" flowable:assignee="${initiator}" flowable:formKey="sample_purchase_order_approval_form" flowable:formFieldPermissions='[{"field":"title","label":"采购主题","readable":true,"writable":true,"required":true},{"field":"supplierName","label":"供应商","readable":true,"writable":true,"required":true},{"field":"amountCent","label":"采购金额分","readable":true,"writable":true,"required":true},{"field":"purchaseItems","label":"采购明细","readable":true,"writable":true,"required":false},{"field":"needDate","label":"期望到货日期","readable":true,"writable":true,"required":false},{"field":"applicantModifyRemark","label":"申请人修改说明","readable":true,"writable":true,"required":false}]' flowable:allowApprove="true" flowable:allowReject="true" flowable:allowDelegate="false" flowable:allowReturn="false" flowable:allowTerminate="false" flowable:requireComment="true">
                      <bpmn:incoming>Flow_004</bpmn:incoming>
                      <bpmn:incoming>Flow_007</bpmn:incoming>
                      <bpmn:incoming>Flow_010</bpmn:incoming>
                      <bpmn:outgoing>Flow_011</bpmn:outgoing>
                    </bpmn:userTask>
                    <bpmn:exclusiveGateway id="Gateway_modify_result" name="修改处理结果" default="Flow_012">
                      <bpmn:incoming>Flow_011</bpmn:incoming>
                      <bpmn:outgoing>Flow_012</bpmn:outgoing>
                      <bpmn:outgoing>Flow_013</bpmn:outgoing>
                    </bpmn:exclusiveGateway>
                    <bpmn:endEvent id="EndEvent_approved" name="审批通过">
                      <bpmn:incoming>Flow_009</bpmn:incoming>
                    </bpmn:endEvent>
                    <bpmn:endEvent id="EndEvent_rejected" name="申请人终止">
                      <bpmn:incoming>Flow_013</bpmn:incoming>
                    </bpmn:endEvent>
                    <bpmn:sequenceFlow id="Flow_001" sourceRef="StartEvent_1" targetRef="dept_leader_approve" />
                    <bpmn:sequenceFlow id="Flow_002" sourceRef="dept_leader_approve" targetRef="Gateway_dept_result" />
                    <bpmn:sequenceFlow id="Flow_003" name="通过" sourceRef="Gateway_dept_result" targetRef="engineering_manager_approve" />
                    <bpmn:sequenceFlow id="Flow_004" name="驳回修改" sourceRef="Gateway_dept_result" targetRef="applicant_modify">
                      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression"><![CDATA[${approvalResult == 'reject'}]]></bpmn:conditionExpression>
                    </bpmn:sequenceFlow>
                    <bpmn:sequenceFlow id="Flow_005" sourceRef="engineering_manager_approve" targetRef="Gateway_engineering_result" />
                    <bpmn:sequenceFlow id="Flow_006" name="通过" sourceRef="Gateway_engineering_result" targetRef="purchase_countersign" />
                    <bpmn:sequenceFlow id="Flow_007" name="驳回修改" sourceRef="Gateway_engineering_result" targetRef="applicant_modify">
                      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression"><![CDATA[${approvalResult == 'reject'}]]></bpmn:conditionExpression>
                    </bpmn:sequenceFlow>
                    <bpmn:sequenceFlow id="Flow_008" sourceRef="purchase_countersign" targetRef="Gateway_countersign_result" />
                    <bpmn:sequenceFlow id="Flow_009" name="通过" sourceRef="Gateway_countersign_result" targetRef="EndEvent_approved" />
                    <bpmn:sequenceFlow id="Flow_010" name="驳回修改" sourceRef="Gateway_countersign_result" targetRef="applicant_modify">
                      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression"><![CDATA[${approvalResult == 'reject'}]]></bpmn:conditionExpression>
                    </bpmn:sequenceFlow>
                    <bpmn:sequenceFlow id="Flow_011" sourceRef="applicant_modify" targetRef="Gateway_modify_result" />
                    <bpmn:sequenceFlow id="Flow_012" name="重新提交" sourceRef="Gateway_modify_result" targetRef="dept_leader_approve" />
                    <bpmn:sequenceFlow id="Flow_013" name="终止申请" sourceRef="Gateway_modify_result" targetRef="EndEvent_rejected">
                      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression"><![CDATA[${approvalResult == 'reject'}]]></bpmn:conditionExpression>
                    </bpmn:sequenceFlow>
                  </bpmn:process>
                  <bpmndi:BPMNDiagram id="BPMNDiagram_sample_purchase_order_approval">
                    <bpmndi:BPMNPlane id="BPMNPlane_sample_purchase_order_approval" bpmnElement="sample_purchase_order_approval">
                      <bpmndi:BPMNShape id="StartEvent_1_di" bpmnElement="StartEvent_1"><dc:Bounds x="80" y="192" width="36" height="36" /></bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="dept_leader_approve_di" bpmnElement="dept_leader_approve"><dc:Bounds x="220" y="170" width="140" height="80" /></bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="Gateway_dept_result_di" bpmnElement="Gateway_dept_result" isMarkerVisible="true"><dc:Bounds x="420" y="185" width="50" height="50" /></bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="engineering_manager_approve_di" bpmnElement="engineering_manager_approve"><dc:Bounds x="530" y="170" width="140" height="80" /></bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="Gateway_engineering_result_di" bpmnElement="Gateway_engineering_result" isMarkerVisible="true"><dc:Bounds x="730" y="185" width="50" height="50" /></bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="purchase_countersign_di" bpmnElement="purchase_countersign"><dc:Bounds x="840" y="170" width="140" height="80" /></bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="Gateway_countersign_result_di" bpmnElement="Gateway_countersign_result" isMarkerVisible="true"><dc:Bounds x="1040" y="185" width="50" height="50" /></bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="EndEvent_approved_di" bpmnElement="EndEvent_approved"><dc:Bounds x="1160" y="192" width="36" height="36" /></bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="applicant_modify_di" bpmnElement="applicant_modify"><dc:Bounds x="540" y="330" width="140" height="80" /></bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="Gateway_modify_result_di" bpmnElement="Gateway_modify_result" isMarkerVisible="true"><dc:Bounds x="740" y="345" width="50" height="50" /></bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="EndEvent_rejected_di" bpmnElement="EndEvent_rejected"><dc:Bounds x="880" y="352" width="36" height="36" /></bpmndi:BPMNShape>
                      <bpmndi:BPMNEdge id="Flow_001_di" bpmnElement="Flow_001"><di:waypoint x="116" y="210" /><di:waypoint x="220" y="210" /></bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Flow_002_di" bpmnElement="Flow_002"><di:waypoint x="360" y="210" /><di:waypoint x="420" y="210" /></bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Flow_003_di" bpmnElement="Flow_003"><di:waypoint x="470" y="210" /><di:waypoint x="530" y="210" /></bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Flow_004_di" bpmnElement="Flow_004"><di:waypoint x="445" y="235" /><di:waypoint x="445" y="370" /><di:waypoint x="540" y="370" /></bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Flow_005_di" bpmnElement="Flow_005"><di:waypoint x="670" y="210" /><di:waypoint x="730" y="210" /></bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Flow_006_di" bpmnElement="Flow_006"><di:waypoint x="780" y="210" /><di:waypoint x="840" y="210" /></bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Flow_007_di" bpmnElement="Flow_007"><di:waypoint x="755" y="235" /><di:waypoint x="755" y="370" /><di:waypoint x="680" y="370" /></bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Flow_008_di" bpmnElement="Flow_008"><di:waypoint x="980" y="210" /><di:waypoint x="1040" y="210" /></bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Flow_009_di" bpmnElement="Flow_009"><di:waypoint x="1090" y="210" /><di:waypoint x="1160" y="210" /></bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Flow_010_di" bpmnElement="Flow_010"><di:waypoint x="1065" y="235" /><di:waypoint x="1065" y="430" /><di:waypoint x="610" y="430" /><di:waypoint x="610" y="410" /></bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Flow_011_di" bpmnElement="Flow_011"><di:waypoint x="680" y="370" /><di:waypoint x="740" y="370" /></bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Flow_012_di" bpmnElement="Flow_012"><di:waypoint x="765" y="345" /><di:waypoint x="765" y="95" /><di:waypoint x="290" y="95" /><di:waypoint x="290" y="170" /></bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Flow_013_di" bpmnElement="Flow_013"><di:waypoint x="790" y="370" /><di:waypoint x="880" y="370" /></bpmndi:BPMNEdge>
                    </bpmndi:BPMNPlane>
                  </bpmndi:BPMNDiagram>
                </bpmn:definitions>
                """;
    }
}
