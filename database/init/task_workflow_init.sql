-- =====================================================
-- 任务管理模块 - 工作流模板和测试数据初始化
-- 创建日期: 2025-12-27
-- 说明: 创建"开班会任务-两级审批"流程模板
-- =====================================================

SET NAMES utf8mb4;

-- =====================================================
-- 1. 插入流程模板记录
-- =====================================================

INSERT INTO `workflow_templates` (
    `id`,
    `template_name`,
    `template_code`,
    `template_type`,
    `description`,
    `bpmn_xml`,
    `is_default`,
    `status`,
    `version`,
    `sort_order`,
    `created_by`,
    `created_by_name`,
    `created_at`,
    `deleted`
) VALUES (
    1000001,  -- 使用固定ID便于测试
    '开班会任务-两级审批',
    'CLASS_MEETING_TWO_LEVEL',
    'TASK',
    '批量分配给班主任，提交班会照片材料后，系主任和校领导两级审批。支持打回重新提交。',
    '<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xmlns:flowable="http://flowable.org/bpmn"
                  id="Definitions_1"
                  targetNamespace="http://bpmn.io/schema/bpmn"
                  exporter="Camunda Modeler"
                  exporterVersion="5.0.0">
  <bpmn:process id="CLASS_MEETING_TWO_LEVEL" name="开班会任务-两级审批" isExecutable="true">
    <bpmn:startEvent id="StartEvent_1" name="开始">
      <bpmn:outgoing>Flow_1</bpmn:outgoing>
    </bpmn:startEvent>
    <bpmn:userTask id="submitMaterials" name="班主任提交材料" flowable:assignee="${assigneeId}">
      <bpmn:incoming>Flow_1</bpmn:incoming>
      <bpmn:incoming>Flow_Reject1</bpmn:incoming>
      <bpmn:incoming>Flow_Reject2</bpmn:incoming>
      <bpmn:outgoing>Flow_2</bpmn:outgoing>
      <bpmn:multiInstanceLoopCharacteristics isSequential="false" flowable:collection="${assigneeList}" flowable:elementVariable="assigneeId">
        <bpmn:completionCondition>${nrOfCompletedInstances == nrOfInstances}</bpmn:completionCondition>
      </bpmn:multiInstanceLoopCharacteristics>
    </bpmn:userTask>
    <bpmn:userTask id="leader1Approve" name="系主任审批" flowable:assignee="${leader1Id}">
      <bpmn:incoming>Flow_2</bpmn:incoming>
      <bpmn:outgoing>Flow_3</bpmn:outgoing>
    </bpmn:userTask>
    <bpmn:exclusiveGateway id="Gateway_1" name="领导1决策">
      <bpmn:incoming>Flow_3</bpmn:incoming>
      <bpmn:outgoing>Flow_Pass1</bpmn:outgoing>
      <bpmn:outgoing>Flow_Reject1</bpmn:outgoing>
    </bpmn:exclusiveGateway>
    <bpmn:userTask id="leader2Approve" name="校领导审批" flowable:assignee="${leader2Id}">
      <bpmn:incoming>Flow_Pass1</bpmn:incoming>
      <bpmn:outgoing>Flow_4</bpmn:outgoing>
    </bpmn:userTask>
    <bpmn:exclusiveGateway id="Gateway_2" name="领导2决策">
      <bpmn:incoming>Flow_4</bpmn:incoming>
      <bpmn:outgoing>Flow_Pass2</bpmn:outgoing>
      <bpmn:outgoing>Flow_Reject2</bpmn:outgoing>
    </bpmn:exclusiveGateway>
    <bpmn:endEvent id="EndEvent_1" name="结束">
      <bpmn:incoming>Flow_Pass2</bpmn:incoming>
    </bpmn:endEvent>
    <bpmn:sequenceFlow id="Flow_1" sourceRef="StartEvent_1" targetRef="submitMaterials" />
    <bpmn:sequenceFlow id="Flow_2" sourceRef="submitMaterials" targetRef="leader1Approve" />
    <bpmn:sequenceFlow id="Flow_3" sourceRef="leader1Approve" targetRef="Gateway_1" />
    <bpmn:sequenceFlow id="Flow_Pass1" name="通过" sourceRef="Gateway_1" targetRef="leader2Approve">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${leader1Result == ''PASS''}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="Flow_Reject1" name="打回" sourceRef="Gateway_1" targetRef="submitMaterials">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${leader1Result == ''REJECT''}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="Flow_4" sourceRef="leader2Approve" targetRef="Gateway_2" />
    <bpmn:sequenceFlow id="Flow_Pass2" name="通过" sourceRef="Gateway_2" targetRef="EndEvent_1">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${leader2Result == ''PASS''}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="Flow_Reject2" name="打回" sourceRef="Gateway_2" targetRef="submitMaterials">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${leader2Result == ''REJECT''}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="CLASS_MEETING_TWO_LEVEL">
      <bpmndi:BPMNShape id="StartEvent_1_di" bpmnElement="StartEvent_1">
        <dc:Bounds x="152" y="192" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="submitMaterials_di" bpmnElement="submitMaterials">
        <dc:Bounds x="250" y="170" width="100" height="80" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="leader1Approve_di" bpmnElement="leader1Approve">
        <dc:Bounds x="410" y="170" width="100" height="80" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Gateway_1_di" bpmnElement="Gateway_1" isMarkerVisible="true">
        <dc:Bounds x="565" y="185" width="50" height="50" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="leader2Approve_di" bpmnElement="leader2Approve">
        <dc:Bounds x="680" y="170" width="100" height="80" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Gateway_2_di" bpmnElement="Gateway_2" isMarkerVisible="true">
        <dc:Bounds x="835" y="185" width="50" height="50" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="EndEvent_1_di" bpmnElement="EndEvent_1">
        <dc:Bounds x="952" y="192" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="Flow_1_di" bpmnElement="Flow_1">
        <di:waypoint x="188" y="210" />
        <di:waypoint x="250" y="210" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_2_di" bpmnElement="Flow_2">
        <di:waypoint x="350" y="210" />
        <di:waypoint x="410" y="210" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_3_di" bpmnElement="Flow_3">
        <di:waypoint x="510" y="210" />
        <di:waypoint x="565" y="210" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_Pass1_di" bpmnElement="Flow_Pass1">
        <di:waypoint x="615" y="210" />
        <di:waypoint x="680" y="210" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_Reject1_di" bpmnElement="Flow_Reject1">
        <di:waypoint x="590" y="185" />
        <di:waypoint x="590" y="100" />
        <di:waypoint x="300" y="100" />
        <di:waypoint x="300" y="170" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_4_di" bpmnElement="Flow_4">
        <di:waypoint x="780" y="210" />
        <di:waypoint x="835" y="210" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_Pass2_di" bpmnElement="Flow_Pass2">
        <di:waypoint x="885" y="210" />
        <di:waypoint x="952" y="210" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_Reject2_di" bpmnElement="Flow_Reject2">
        <di:waypoint x="860" y="235" />
        <di:waypoint x="860" y="320" />
        <di:waypoint x="300" y="320" />
        <di:waypoint x="300" y="250" />
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>',
    1,  -- 设为默认模板
    1,  -- 启用
    1,  -- 版本号
    10, -- 排序
    1,  -- admin创建
    '系统管理员',
    NOW(),
    0   -- 未删除
) ON DUPLICATE KEY UPDATE
    `bpmn_xml` = VALUES(`bpmn_xml`),
    `updated_at` = NOW();

-- =====================================================
-- 说明:
-- 1. 此模板定义了一个批量任务的两级审批流程
-- 2. 流程变量说明:
--    - assigneeList: 班主任ID列表 (启动时传入)
--    - assigneeId: 当前班主任ID (多实例自动分配)
--    - leader1Id: 领导1(系主任)的用户ID
--    - leader2Id: 领导2(校领导)的用户ID
--    - leader1Result: 领导1审批结果 ('PASS' 或 'REJECT')
--    - leader2Result: 领导2审批结果 ('PASS' 或 'REJECT')
-- 3. 部署流程后会自动生成 process_definition_id
-- =====================================================
