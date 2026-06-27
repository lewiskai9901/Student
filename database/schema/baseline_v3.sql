-- MySQL dump 10.13  Distrib 8.0.43, for Win64 (x86_64)
--
-- Host: localhost    Database: sm_rebuild
-- ------------------------------------------------------
-- Server version	8.0.43

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Temporary view structure for view `_classes_backup_20260408`
--

DROP TABLE IF EXISTS `_classes_backup_20260408`;
/*!50001 DROP VIEW IF EXISTS `_classes_backup_20260408`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `_classes_backup_20260408` AS SELECT 
 1 AS `id`,
 1 AS `class_name`,
 1 AS `class_code`,
 1 AS `org_unit_id`,
 1 AS `grade_level`,
 1 AS `grade_id`,
 1 AS `major_id`,
 1 AS `teacher_id`,
 1 AS `student_count`,
 1 AS `class_type`,
 1 AS `status`,
 1 AS `created_at`,
 1 AS `updated_at`,
 1 AS `deleted`,
 1 AS `tenant_id`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `academic_event`
--

DROP TABLE IF EXISTS `academic_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `academic_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `year_id` bigint NOT NULL COMMENT '学年ID',
  `semester_id` bigint DEFAULT NULL COMMENT '学期ID（可选）',
  `event_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '事件名称',
  `event_type` tinyint NOT NULL DEFAULT '5' COMMENT '事件类型：1-开学, 2-放假, 3-考试, 4-活动, 5-其他',
  `start_date` date NOT NULL COMMENT '开始日期',
  `end_date` date DEFAULT NULL COMMENT '结束日期',
  `all_day` tinyint(1) DEFAULT '1' COMMENT '是否全天事件',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '事件描述',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  `affect_type` tinyint DEFAULT '0' COMMENT '0无影响 1全天停课 2半天停课 3补课日 4考试周',
  `affect_scope` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'all' COMMENT '影响范围: all/grade:N/class:N',
  `substitute_weekday` tinyint DEFAULT NULL COMMENT '补课日按周几上课',
  `affect_slots` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '影响节次范围',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_year_id` (`year_id`),
  KEY `idx_semester_id` (`semester_id`),
  KEY `idx_start_date` (`start_date`),
  KEY `idx_event_type` (`event_type`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='校历事件表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `academic_event`
--

LOCK TABLES `academic_event` WRITE;
/*!40000 ALTER TABLE `academic_event` DISABLE KEYS */;
INSERT INTO `academic_event` VALUES (1,1,2,'开学典礼',1,'2026-02-17','2026-02-17',1,'新学期开学典礼','2026-06-01 22:57:17','2026-06-01 22:57:17',0,0,'all',NULL,NULL,1),(2,1,2,'期中考试',3,'2026-04-13','2026-04-17',1,'期中考试周','2026-06-01 22:57:17','2026-06-01 22:57:17',0,0,'all',NULL,NULL,1),(3,1,2,'五一假期',2,'2026-05-01','2026-05-05',1,'劳动节假期','2026-06-01 22:57:17','2026-06-01 22:57:17',0,0,'all',NULL,NULL,1),(4,1,2,'期末考试',3,'2026-06-22','2026-06-28',1,'期末考试周','2026-06-01 22:57:17','2026-06-01 22:57:17',0,0,'all',NULL,NULL,1),(5,1,2,'暑假开始',2,'2026-07-01','2026-08-31',1,'暑假','2026-06-01 22:57:17','2026-06-01 22:57:17',0,0,'all',NULL,NULL,1),(6,1,2,'开学典礼',1,'2026-02-17','2026-02-17',1,'新学期开学典礼','2026-06-01 23:06:05','2026-06-01 23:06:05',0,0,'all',NULL,NULL,1),(7,1,2,'期中考试',3,'2026-04-13','2026-04-17',1,'期中考试周','2026-06-01 23:06:05','2026-06-01 23:06:05',0,0,'all',NULL,NULL,1),(8,1,2,'五一假期',2,'2026-05-01','2026-05-05',1,'劳动节假期','2026-06-01 23:06:05','2026-06-01 23:06:05',0,0,'all',NULL,NULL,1),(9,1,2,'期末考试',3,'2026-06-22','2026-06-28',1,'期末考试周','2026-06-01 23:06:05','2026-06-01 23:06:05',0,0,'all',NULL,NULL,1),(10,1,2,'暑假开始',2,'2026-07-01','2026-08-31',1,'暑假','2026-06-01 23:06:05','2026-06-01 23:06:05',0,0,'all',NULL,NULL,1);
/*!40000 ALTER TABLE `academic_event` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `academic_warning_rules`
--

DROP TABLE IF EXISTS `academic_warning_rules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `academic_warning_rules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
  `rule_type` varchar(30) NOT NULL COMMENT '规则类型: GRADE_FAIL/ATTENDANCE_LOW/CREDIT_SHORT/CUSTOM',
  `warning_level` tinyint NOT NULL COMMENT '预警级别: 1黄色 2橙色 3红色',
  `condition_params` json NOT NULL COMMENT '条件参数',
  `applicable_grades` varchar(100) DEFAULT NULL COMMENT '适用年级（空=全部）',
  `enabled` tinyint DEFAULT '1',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_type` (`rule_type`),
  KEY `idx_level` (`warning_level`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学业预警规则表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `academic_warning_rules`
--

LOCK TABLES `academic_warning_rules` WRITE;
/*!40000 ALTER TABLE `academic_warning_rules` DISABLE KEYS */;
/*!40000 ALTER TABLE `academic_warning_rules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `academic_warnings`
--

DROP TABLE IF EXISTS `academic_warnings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `academic_warnings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `student_no` varchar(50) DEFAULT NULL COMMENT '学号',
  `student_name` varchar(50) DEFAULT NULL COMMENT '学生姓名',
  `org_unit_id` bigint DEFAULT NULL,
  `class_name` varchar(100) DEFAULT NULL COMMENT '班级名',
  `rule_id` bigint DEFAULT NULL COMMENT '触发的规则ID',
  `rule_name` varchar(100) DEFAULT NULL COMMENT '规则名称',
  `warning_type` varchar(30) NOT NULL COMMENT '预警类型',
  `warning_level` tinyint NOT NULL COMMENT '预警级别: 1黄色 2橙色 3红色',
  `description` varchar(500) NOT NULL COMMENT '预警描述',
  `detail` json DEFAULT NULL COMMENT '详细数据（如挂科列表、出勤明细）',
  `status` tinyint DEFAULT '0' COMMENT '0未处理 1已确认 2已干预 3已解除',
  `handler_id` bigint DEFAULT NULL COMMENT '处理人',
  `handle_note` varchar(500) DEFAULT NULL COMMENT '处理备注',
  `handled_at` datetime DEFAULT NULL COMMENT '处理时间',
  `semester_id` bigint DEFAULT NULL COMMENT '学期ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_student` (`student_id`),
  KEY `idx_class` (`org_unit_id`),
  KEY `idx_level` (`warning_level`),
  KEY `idx_status` (`status`),
  KEY `idx_semester` (`semester_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学业预警记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `academic_warnings`
--

LOCK TABLES `academic_warnings` WRITE;
/*!40000 ALTER TABLE `academic_warnings` DISABLE KEYS */;
/*!40000 ALTER TABLE `academic_warnings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `academic_weeks`
--

DROP TABLE IF EXISTS `academic_weeks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `academic_weeks` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `semester_id` bigint NOT NULL,
  `week_number` int NOT NULL,
  `week_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '如: 第1周',
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `is_current` tinyint DEFAULT '0',
  `status` tinyint DEFAULT '1',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `week_type` int DEFAULT NULL COMMENT '1=教学 2=考试 3=假期',
  PRIMARY KEY (`id`),
  KEY `idx_semester` (`semester_id`),
  KEY `idx_tenant` (`tenant_id`),
  CONSTRAINT `fk_week_semester` FOREIGN KEY (`semester_id`) REFERENCES `semesters` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教学周表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `academic_weeks`
--

LOCK TABLES `academic_weeks` WRITE;
/*!40000 ALTER TABLE `academic_weeks` DISABLE KEYS */;
/*!40000 ALTER TABLE `academic_weeks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `academic_years`
--

DROP TABLE IF EXISTS `academic_years`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `academic_years` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `year_code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学年编码(如: 2024-2025)',
  `year_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学年名称',
  `start_date` date NOT NULL COMMENT '开始日期',
  `end_date` date NOT NULL COMMENT '结束日期',
  `semesters` json DEFAULT NULL COMMENT '学期配置',
  `is_current` tinyint DEFAULT '0' COMMENT '是否当前学年',
  `status` tinyint DEFAULT '1' COMMENT '状态',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_year_code` (`year_code`),
  KEY `idx_current` (`is_current`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学年表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `academic_years`
--

LOCK TABLES `academic_years` WRITE;
/*!40000 ALTER TABLE `academic_years` DISABLE KEYS */;
/*!40000 ALTER TABLE `academic_years` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `access_relations`
--

DROP TABLE IF EXISTS `access_relations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `access_relations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `resource_type` varchar(50) NOT NULL COMMENT '资源类型: place, org_unit, student, class...',
  `resource_id` bigint NOT NULL COMMENT '资源ID',
  `relation` varchar(30) NOT NULL COMMENT '关系: owner, manager, user, member, viewer, responsible, occupant',
  `subject_type` varchar(30) NOT NULL COMMENT '主体类型: org_unit, user',
  `subject_id` bigint NOT NULL COMMENT '主体ID',
  `access_level` varchar(20) NOT NULL DEFAULT 'FULL',
  `valid_from` datetime DEFAULT CURRENT_TIMESTAMP,
  `valid_to` datetime DEFAULT NULL,
  `metadata` json DEFAULT NULL COMMENT '扩展字段（isPrimary, weightRatio, positionTitle 等）',
  `remark` varchar(500) DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT '0',
  `deleted_at` datetime DEFAULT NULL,
  `deleted_by` bigint DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `is_primary` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否主归属',
  `membership_lock_key` bigint GENERATED ALWAYS AS ((case when ((`relation` = _utf8mb4'member') and (`subject_type` = _utf8mb4'user') and (`resource_type` = _utf8mb4'org_unit') and (`deleted` = 0)) then `subject_id` else NULL end)) STORED COMMENT '归属唯一约束键(活跃 user->org member 取 subject_id)',
  `place_belongs_lock_key` bigint GENERATED ALWAYS AS ((case when ((`relation` = _utf8mb4'belongs_to') and (`subject_type` = _utf8mb4'place') and (`resource_type` = _utf8mb4'org_unit') and (`deleted` = 0)) then `subject_id` else NULL end)) STORED COMMENT '场所归属唯一约束键(活跃 place->org belongs_to 取 subject_id)',
  `place_responsible_lock_key` bigint GENERATED ALWAYS AS ((case when ((`relation` = _utf8mb4'responsible_for') and (`subject_type` = _utf8mb4'user') and (`resource_type` = _utf8mb4'place') and (`deleted` = 0)) then `resource_id` else NULL end)) STORED COMMENT '场所责任人唯一约束键(活跃 user->place responsible_for 取 resource_id)',
  `active_uniq` tinyint GENERATED ALWAYS AS ((case when (`deleted` = 0) then 1 else NULL end)) STORED COMMENT '活跃行唯一性豁免列(归档行 NULL 不参与 uk_relation)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_relation` (`resource_type`,`resource_id`,`relation`,`subject_type`,`subject_id`,`active_uniq`),
  UNIQUE KEY `uk_membership_unique` (`membership_lock_key`),
  UNIQUE KEY `uk_place_belongs_unique` (`place_belongs_lock_key`),
  UNIQUE KEY `uk_place_responsible_unique` (`place_responsible_lock_key`),
  KEY `idx_resource` (`resource_type`,`resource_id`,`deleted`),
  KEY `idx_subject` (`subject_type`,`subject_id`,`deleted`),
  KEY `idx_lookup` (`resource_type`,`relation`,`subject_type`,`subject_id`,`deleted`),
  KEY `idx_access_relations_tenant` (`tenant_id`),
  KEY `idx_validity` (`valid_from`,`valid_to`),
  KEY `idx_relation_validity` (`relation`,`valid_to`,`deleted`),
  KEY `idx_expand` (`resource_type`,`resource_id`,`relation`,`deleted`),
  KEY `idx_chain_hop` (`subject_type`,`subject_id`,`relation`,`resource_type`,`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='统一访问关系表 (Zanzibar Simplified)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `access_relations`
--

LOCK TABLES `access_relations` WRITE;
/*!40000 ALTER TABLE `access_relations` DISABLE KEYS */;
INSERT INTO `access_relations` (`id`, `resource_type`, `resource_id`, `relation`, `subject_type`, `subject_id`, `access_level`, `valid_from`, `valid_to`, `metadata`, `remark`, `created_by`, `created_at`, `updated_at`, `deleted`, `deleted_at`, `deleted_by`, `tenant_id`, `is_primary`) VALUES (1,'org_unit',2040636269108707330,'member','user',2041870507300622337,'READ','2026-06-01 23:26:23',NULL,NULL,NULL,NULL,'2026-06-01 23:26:23','2026-06-01 23:26:23',0,NULL,NULL,1,1),(2,'org_unit',2041864691411632129,'member','user',2041870508646993922,'READ','2026-06-01 23:26:23',NULL,NULL,NULL,NULL,'2026-06-01 23:26:23','2026-06-01 23:26:23',0,NULL,NULL,1,1);
/*!40000 ALTER TABLE `access_relations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `access_relations_delegation_ext`
--

DROP TABLE IF EXISTS `access_relations_delegation_ext`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `access_relations_delegation_ext` (
  `relation_id` bigint NOT NULL,
  `delegated_permissions` json DEFAULT NULL COMMENT '委托的具体权限列表(子集)',
  `delegation_reason` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`relation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='委托关系扩展';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `access_relations_delegation_ext`
--

LOCK TABLES `access_relations_delegation_ext` WRITE;
/*!40000 ALTER TABLE `access_relations_delegation_ext` DISABLE KEYS */;
/*!40000 ALTER TABLE `access_relations_delegation_ext` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `access_relations_history`
--

DROP TABLE IF EXISTS `access_relations_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `access_relations_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `original_id` bigint DEFAULT NULL,
  `resource_type` varchar(30) NOT NULL,
  `resource_id` bigint NOT NULL,
  `relation` varchar(50) NOT NULL,
  `subject_type` varchar(30) NOT NULL,
  `subject_id` bigint NOT NULL,
  `access_level` varchar(20) DEFAULT NULL,
  `valid_from` datetime DEFAULT NULL,
  `valid_to` datetime DEFAULT NULL,
  `metadata` json DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `archived_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `archived_reason` varchar(100) DEFAULT NULL,
  `archived_by` bigint DEFAULT NULL,
  `operator_ip` varchar(64) DEFAULT NULL COMMENT '操作者IP (Phase 7 W7.3)',
  `operator_user_agent` varchar(500) DEFAULT NULL COMMENT '操作者UA (Phase 7 W7.3)',
  `operation` varchar(20) NOT NULL DEFAULT 'REVOKE' COMMENT '归档操作类型 REVOKE/EXPIRE (Phase 7 W7.3)',
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `created_by` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_original` (`original_id`),
  KEY `idx_resource_subject` (`resource_type`,`resource_id`,`subject_id`),
  KEY `idx_archived` (`archived_at`),
  KEY `idx_history_archived_at` (`archived_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='关系归档表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `access_relations_history`
--

LOCK TABLES `access_relations_history` WRITE;
/*!40000 ALTER TABLE `access_relations_history` DISABLE KEYS */;
/*!40000 ALTER TABLE `access_relations_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `access_relations_occupancy_ext`
--

DROP TABLE IF EXISTS `access_relations_occupancy_ext`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `access_relations_occupancy_ext` (
  `relation_id` bigint NOT NULL COMMENT 'FK -> access_relations.id',
  `seat_no` varchar(20) DEFAULT NULL,
  `check_in_time` datetime NOT NULL,
  `check_out_time` datetime DEFAULT NULL,
  `notes` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`relation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='占用关系扩展(床位号/时段)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `access_relations_occupancy_ext`
--

LOCK TABLES `access_relations_occupancy_ext` WRITE;
/*!40000 ALTER TABLE `access_relations_occupancy_ext` DISABLE KEYS */;
/*!40000 ALTER TABLE `access_relations_occupancy_ext` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_app_appdef`
--

DROP TABLE IF EXISTS `act_app_appdef`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_app_appdef` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `REV_` int NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `VERSION_` int NOT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `RESOURCE_NAME_` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '',
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_IDX_APP_DEF_UNIQ` (`KEY_`,`VERSION_`,`TENANT_ID_`),
  KEY `ACT_IDX_APP_DEF_DPLY` (`DEPLOYMENT_ID_`),
  CONSTRAINT `ACT_FK_APP_DEF_DPLY` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `act_app_deployment` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_app_appdef`
--

LOCK TABLES `act_app_appdef` WRITE;
/*!40000 ALTER TABLE `act_app_appdef` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_app_appdef` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_app_databasechangelog`
--

DROP TABLE IF EXISTS `act_app_databasechangelog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_app_databasechangelog` (
  `ID` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `AUTHOR` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `FILENAME` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `DATEEXECUTED` datetime NOT NULL,
  `ORDEREXECUTED` int NOT NULL,
  `EXECTYPE` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  `MD5SUM` varchar(35) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DESCRIPTION` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `COMMENTS` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TAG` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `LIQUIBASE` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CONTEXTS` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `LABELS` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOYMENT_ID` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_app_databasechangelog`
--

LOCK TABLES `act_app_databasechangelog` WRITE;
/*!40000 ALTER TABLE `act_app_databasechangelog` DISABLE KEYS */;
INSERT INTO `act_app_databasechangelog` VALUES ('1','flowable','org/flowable/app/db/liquibase/flowable-app-db-changelog.xml','2026-06-01 23:28:41',1,'EXECUTED','9:959783069c0c7ce80320a0617aa48969','createTable tableName=ACT_APP_DEPLOYMENT; createTable tableName=ACT_APP_DEPLOYMENT_RESOURCE; addForeignKeyConstraint baseTableName=ACT_APP_DEPLOYMENT_RESOURCE, constraintName=ACT_FK_APP_RSRC_DPL, referencedTableName=ACT_APP_DEPLOYMENT; createIndex...','',NULL,'4.24.0',NULL,NULL,'0327721664'),('2','flowable','org/flowable/app/db/liquibase/flowable-app-db-changelog.xml','2026-06-01 23:28:42',2,'EXECUTED','9:c75407b1c0e16adf2a6db585c05a94c7','modifyDataType columnName=DEPLOY_TIME_, tableName=ACT_APP_DEPLOYMENT','',NULL,'4.24.0',NULL,NULL,'0327721664'),('3','flowable','org/flowable/app/db/liquibase/flowable-app-db-changelog.xml','2026-06-01 23:28:42',3,'EXECUTED','9:c05b79a3b00e95136533085718361208','createIndex indexName=ACT_IDX_APP_DEF_UNIQ, tableName=ACT_APP_APPDEF','',NULL,'4.24.0',NULL,NULL,'0327721664');
/*!40000 ALTER TABLE `act_app_databasechangelog` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_app_databasechangeloglock`
--

DROP TABLE IF EXISTS `act_app_databasechangeloglock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_app_databasechangeloglock` (
  `ID` int NOT NULL,
  `LOCKED` tinyint(1) NOT NULL,
  `LOCKGRANTED` datetime DEFAULT NULL,
  `LOCKEDBY` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_app_databasechangeloglock`
--

LOCK TABLES `act_app_databasechangeloglock` WRITE;
/*!40000 ALTER TABLE `act_app_databasechangeloglock` DISABLE KEYS */;
INSERT INTO `act_app_databasechangeloglock` VALUES (1,0,NULL,NULL);
/*!40000 ALTER TABLE `act_app_databasechangeloglock` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_app_deployment`
--

DROP TABLE IF EXISTS `act_app_deployment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_app_deployment` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOY_TIME_` datetime(3) DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '',
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_app_deployment`
--

LOCK TABLES `act_app_deployment` WRITE;
/*!40000 ALTER TABLE `act_app_deployment` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_app_deployment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_app_deployment_resource`
--

DROP TABLE IF EXISTS `act_app_deployment_resource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_app_deployment_resource` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `RESOURCE_BYTES_` longblob,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_APP_RSRC_DPL` (`DEPLOYMENT_ID_`),
  CONSTRAINT `ACT_FK_APP_RSRC_DPL` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `act_app_deployment` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_app_deployment_resource`
--

LOCK TABLES `act_app_deployment_resource` WRITE;
/*!40000 ALTER TABLE `act_app_deployment_resource` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_app_deployment_resource` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_cmmn_casedef`
--

DROP TABLE IF EXISTS `act_cmmn_casedef`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_casedef` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `REV_` int NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `VERSION_` int NOT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `RESOURCE_NAME_` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `HAS_GRAPHICAL_NOTATION_` tinyint(1) DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '',
  `DGRM_RESOURCE_NAME_` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `HAS_START_FORM_KEY_` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_IDX_CASE_DEF_UNIQ` (`KEY_`,`VERSION_`,`TENANT_ID_`),
  KEY `ACT_IDX_CASE_DEF_DPLY` (`DEPLOYMENT_ID_`),
  CONSTRAINT `ACT_FK_CASE_DEF_DPLY` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `act_cmmn_deployment` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_cmmn_casedef`
--

LOCK TABLES `act_cmmn_casedef` WRITE;
/*!40000 ALTER TABLE `act_cmmn_casedef` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_cmmn_casedef` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_cmmn_databasechangelog`
--

DROP TABLE IF EXISTS `act_cmmn_databasechangelog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_databasechangelog` (
  `ID` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `AUTHOR` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `FILENAME` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `DATEEXECUTED` datetime NOT NULL,
  `ORDEREXECUTED` int NOT NULL,
  `EXECTYPE` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  `MD5SUM` varchar(35) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DESCRIPTION` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `COMMENTS` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TAG` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `LIQUIBASE` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CONTEXTS` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `LABELS` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOYMENT_ID` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_cmmn_databasechangelog`
--

LOCK TABLES `act_cmmn_databasechangelog` WRITE;
/*!40000 ALTER TABLE `act_cmmn_databasechangelog` DISABLE KEYS */;
INSERT INTO `act_cmmn_databasechangelog` VALUES ('1','flowable','org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml','2026-06-01 23:28:36',1,'EXECUTED','9:d0cc0aaadf0e4ef70c5b412cd05fadc4','createTable tableName=ACT_CMMN_DEPLOYMENT; createTable tableName=ACT_CMMN_DEPLOYMENT_RESOURCE; addForeignKeyConstraint baseTableName=ACT_CMMN_DEPLOYMENT_RESOURCE, constraintName=ACT_FK_CMMN_RSRC_DPL, referencedTableName=ACT_CMMN_DEPLOYMENT; create...','',NULL,'4.24.0',NULL,NULL,'0327715235'),('2','flowable','org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml','2026-06-01 23:28:37',2,'EXECUTED','9:8095a5a8a222a100c2d0310cacbda5e7','addColumn tableName=ACT_CMMN_CASEDEF; addColumn tableName=ACT_CMMN_DEPLOYMENT_RESOURCE; addColumn tableName=ACT_CMMN_RU_CASE_INST; addColumn tableName=ACT_CMMN_RU_PLAN_ITEM_INST','',NULL,'4.24.0',NULL,NULL,'0327715235'),('3','flowable','org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml','2026-06-01 23:28:37',3,'EXECUTED','9:f031b4f0ae67bc5a640736b379049b12','addColumn tableName=ACT_CMMN_RU_PLAN_ITEM_INST; addColumn tableName=ACT_CMMN_RU_CASE_INST; createIndex indexName=ACT_IDX_PLAN_ITEM_STAGE_INST, tableName=ACT_CMMN_RU_PLAN_ITEM_INST; addColumn tableName=ACT_CMMN_RU_PLAN_ITEM_INST; addColumn tableNam...','',NULL,'4.24.0',NULL,NULL,'0327715235'),('4','flowable','org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml','2026-06-01 23:28:37',4,'EXECUTED','9:c484ecfb08719feccac2f80fc962dda9','createTable tableName=ACT_CMMN_HI_PLAN_ITEM_INST; addColumn tableName=ACT_CMMN_RU_MIL_INST; addColumn tableName=ACT_CMMN_HI_MIL_INST','',NULL,'4.24.0',NULL,NULL,'0327715235'),('5','flowable','org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml','2026-06-01 23:28:39',5,'EXECUTED','9:e6a67f8f0d16cd72117900442acfe6e0','modifyDataType columnName=DEPLOY_TIME_, tableName=ACT_CMMN_DEPLOYMENT; modifyDataType columnName=START_TIME_, tableName=ACT_CMMN_RU_CASE_INST; modifyDataType columnName=START_TIME_, tableName=ACT_CMMN_RU_PLAN_ITEM_INST; modifyDataType columnName=T...','',NULL,'4.24.0',NULL,NULL,'0327715235'),('6','flowable','org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml','2026-06-01 23:28:39',6,'EXECUTED','9:7343ab247d959e5add9278b5386de833','createIndex indexName=ACT_IDX_CASE_DEF_UNIQ, tableName=ACT_CMMN_CASEDEF','',NULL,'4.24.0',NULL,NULL,'0327715235'),('7','flowable','org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml','2026-06-01 23:28:39',7,'EXECUTED','9:d73200db684b6cdb748cc03570d5d2e9','renameColumn newColumnName=CREATE_TIME_, oldColumnName=START_TIME_, tableName=ACT_CMMN_RU_PLAN_ITEM_INST; renameColumn newColumnName=CREATE_TIME_, oldColumnName=CREATED_TIME_, tableName=ACT_CMMN_HI_PLAN_ITEM_INST; addColumn tableName=ACT_CMMN_RU_P...','',NULL,'4.24.0',NULL,NULL,'0327715235'),('8','flowable','org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml','2026-06-01 23:28:39',8,'EXECUTED','9:eda5e43816221f2d8554bfcc90f1c37e','addColumn tableName=ACT_CMMN_HI_PLAN_ITEM_INST','',NULL,'4.24.0',NULL,NULL,'0327715235'),('9','flowable','org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml','2026-06-01 23:28:39',9,'EXECUTED','9:c34685611779075a73caf8c380f078ea','addColumn tableName=ACT_CMMN_RU_PLAN_ITEM_INST; addColumn tableName=ACT_CMMN_HI_PLAN_ITEM_INST','',NULL,'4.24.0',NULL,NULL,'0327715235'),('10','flowable','org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml','2026-06-01 23:28:39',10,'EXECUTED','9:368e9472ad2348206205170d6c52d58e','addColumn tableName=ACT_CMMN_RU_CASE_INST; addColumn tableName=ACT_CMMN_RU_CASE_INST; createIndex indexName=ACT_IDX_CASE_INST_REF_ID_, tableName=ACT_CMMN_RU_CASE_INST; addColumn tableName=ACT_CMMN_HI_CASE_INST; addColumn tableName=ACT_CMMN_HI_CASE...','',NULL,'4.24.0',NULL,NULL,'0327715235'),('11','flowable','org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml','2026-06-01 23:28:40',11,'EXECUTED','9:e54b50ceb2bcd5355ae4dfb56d9ff3ad','addColumn tableName=ACT_CMMN_RU_PLAN_ITEM_INST; addColumn tableName=ACT_CMMN_HI_PLAN_ITEM_INST','',NULL,'4.24.0',NULL,NULL,'0327715235'),('12','flowable','org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml','2026-06-01 23:28:40',12,'EXECUTED','9:f53f262768d04e74529f43fcd93429b0','addColumn tableName=ACT_CMMN_RU_CASE_INST','',NULL,'4.24.0',NULL,NULL,'0327715235'),('13','flowable','org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml','2026-06-01 23:28:40',13,'EXECUTED','9:64e7eafbe97997094654e83caea99895','addColumn tableName=ACT_CMMN_RU_PLAN_ITEM_INST; addColumn tableName=ACT_CMMN_HI_PLAN_ITEM_INST','',NULL,'4.24.0',NULL,NULL,'0327715235'),('14','flowable','org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml','2026-06-01 23:28:40',14,'EXECUTED','9:ab7d934abde497eac034701542e0a281','addColumn tableName=ACT_CMMN_RU_CASE_INST; addColumn tableName=ACT_CMMN_HI_CASE_INST','',NULL,'4.24.0',NULL,NULL,'0327715235'),('16','flowable','org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml','2026-06-01 23:28:41',15,'EXECUTED','9:03928d422e510959770e7a9daa5a993f','addColumn tableName=ACT_CMMN_RU_CASE_INST; addColumn tableName=ACT_CMMN_HI_CASE_INST','',NULL,'4.24.0',NULL,NULL,'0327715235'),('17','flowable','org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml','2026-06-01 23:28:41',16,'EXECUTED','9:f30304cf001d6eac78c793ea88cd5781','createIndex indexName=ACT_IDX_HI_CASE_INST_END, tableName=ACT_CMMN_HI_CASE_INST','',NULL,'4.24.0',NULL,NULL,'0327715235'),('18','flowable','org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml','2026-06-01 23:28:41',17,'EXECUTED','9:d782865087d6c0c3dc033ac20e783008','createIndex indexName=ACT_IDX_HI_PLAN_ITEM_INST_CASE, tableName=ACT_CMMN_HI_PLAN_ITEM_INST','',NULL,'4.24.0',NULL,NULL,'0327715235');
/*!40000 ALTER TABLE `act_cmmn_databasechangelog` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_cmmn_databasechangeloglock`
--

DROP TABLE IF EXISTS `act_cmmn_databasechangeloglock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_databasechangeloglock` (
  `ID` int NOT NULL,
  `LOCKED` tinyint(1) NOT NULL,
  `LOCKGRANTED` datetime DEFAULT NULL,
  `LOCKEDBY` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_cmmn_databasechangeloglock`
--

LOCK TABLES `act_cmmn_databasechangeloglock` WRITE;
/*!40000 ALTER TABLE `act_cmmn_databasechangeloglock` DISABLE KEYS */;
INSERT INTO `act_cmmn_databasechangeloglock` VALUES (1,0,NULL,NULL);
/*!40000 ALTER TABLE `act_cmmn_databasechangeloglock` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_cmmn_deployment`
--

DROP TABLE IF EXISTS `act_cmmn_deployment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_deployment` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOY_TIME_` datetime(3) DEFAULT NULL,
  `PARENT_DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '',
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_cmmn_deployment`
--

LOCK TABLES `act_cmmn_deployment` WRITE;
/*!40000 ALTER TABLE `act_cmmn_deployment` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_cmmn_deployment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_cmmn_deployment_resource`
--

DROP TABLE IF EXISTS `act_cmmn_deployment_resource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_deployment_resource` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `RESOURCE_BYTES_` longblob,
  `GENERATED_` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_CMMN_RSRC_DPL` (`DEPLOYMENT_ID_`),
  CONSTRAINT `ACT_FK_CMMN_RSRC_DPL` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `act_cmmn_deployment` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_cmmn_deployment_resource`
--

LOCK TABLES `act_cmmn_deployment_resource` WRITE;
/*!40000 ALTER TABLE `act_cmmn_deployment_resource` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_cmmn_deployment_resource` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_cmmn_hi_case_inst`
--

DROP TABLE IF EXISTS `act_cmmn_hi_case_inst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_hi_case_inst` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `REV_` int NOT NULL,
  `BUSINESS_KEY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `PARENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CASE_DEF_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `STATE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `START_TIME_` datetime(3) DEFAULT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `START_USER_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CALLBACK_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CALLBACK_TYPE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '',
  `REFERENCE_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `REFERENCE_TYPE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `LAST_REACTIVATION_TIME_` datetime(3) DEFAULT NULL,
  `LAST_REACTIVATION_USER_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `BUSINESS_STATUS_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_CASE_INST_END` (`END_TIME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_cmmn_hi_case_inst`
--

LOCK TABLES `act_cmmn_hi_case_inst` WRITE;
/*!40000 ALTER TABLE `act_cmmn_hi_case_inst` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_cmmn_hi_case_inst` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_cmmn_hi_mil_inst`
--

DROP TABLE IF EXISTS `act_cmmn_hi_mil_inst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_hi_mil_inst` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `REV_` int NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `TIME_STAMP_` datetime(3) DEFAULT NULL,
  `CASE_INST_ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `CASE_DEF_ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '',
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_cmmn_hi_mil_inst`
--

LOCK TABLES `act_cmmn_hi_mil_inst` WRITE;
/*!40000 ALTER TABLE `act_cmmn_hi_mil_inst` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_cmmn_hi_mil_inst` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_cmmn_hi_plan_item_inst`
--

DROP TABLE IF EXISTS `act_cmmn_hi_plan_item_inst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_hi_plan_item_inst` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `REV_` int NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `STATE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CASE_DEF_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CASE_INST_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `STAGE_INST_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `IS_STAGE_` tinyint(1) DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ITEM_DEFINITION_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ITEM_DEFINITION_TYPE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `LAST_AVAILABLE_TIME_` datetime(3) DEFAULT NULL,
  `LAST_ENABLED_TIME_` datetime(3) DEFAULT NULL,
  `LAST_DISABLED_TIME_` datetime(3) DEFAULT NULL,
  `LAST_STARTED_TIME_` datetime(3) DEFAULT NULL,
  `LAST_SUSPENDED_TIME_` datetime(3) DEFAULT NULL,
  `COMPLETED_TIME_` datetime(3) DEFAULT NULL,
  `OCCURRED_TIME_` datetime(3) DEFAULT NULL,
  `TERMINATED_TIME_` datetime(3) DEFAULT NULL,
  `EXIT_TIME_` datetime(3) DEFAULT NULL,
  `ENDED_TIME_` datetime(3) DEFAULT NULL,
  `LAST_UPDATED_TIME_` datetime(3) DEFAULT NULL,
  `START_USER_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `REFERENCE_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `REFERENCE_TYPE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '',
  `ENTRY_CRITERION_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `EXIT_CRITERION_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `SHOW_IN_OVERVIEW_` tinyint(1) DEFAULT NULL,
  `EXTRA_VALUE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DERIVED_CASE_DEF_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `LAST_UNAVAILABLE_TIME_` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_PLAN_ITEM_INST_CASE` (`CASE_INST_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_cmmn_hi_plan_item_inst`
--

LOCK TABLES `act_cmmn_hi_plan_item_inst` WRITE;
/*!40000 ALTER TABLE `act_cmmn_hi_plan_item_inst` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_cmmn_hi_plan_item_inst` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_cmmn_ru_case_inst`
--

DROP TABLE IF EXISTS `act_cmmn_ru_case_inst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_ru_case_inst` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `REV_` int NOT NULL,
  `BUSINESS_KEY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `PARENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CASE_DEF_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `STATE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `START_TIME_` datetime(3) DEFAULT NULL,
  `START_USER_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CALLBACK_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CALLBACK_TYPE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '',
  `LOCK_TIME_` datetime(3) DEFAULT NULL,
  `IS_COMPLETEABLE_` tinyint(1) DEFAULT NULL,
  `REFERENCE_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `REFERENCE_TYPE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `LAST_REACTIVATION_TIME_` datetime(3) DEFAULT NULL,
  `LAST_REACTIVATION_USER_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `BUSINESS_STATUS_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_CASE_INST_CASE_DEF` (`CASE_DEF_ID_`),
  KEY `ACT_IDX_CASE_INST_PARENT` (`PARENT_ID_`),
  KEY `ACT_IDX_CASE_INST_REF_ID_` (`REFERENCE_ID_`),
  CONSTRAINT `ACT_FK_CASE_INST_CASE_DEF` FOREIGN KEY (`CASE_DEF_ID_`) REFERENCES `act_cmmn_casedef` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_cmmn_ru_case_inst`
--

LOCK TABLES `act_cmmn_ru_case_inst` WRITE;
/*!40000 ALTER TABLE `act_cmmn_ru_case_inst` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_cmmn_ru_case_inst` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_cmmn_ru_mil_inst`
--

DROP TABLE IF EXISTS `act_cmmn_ru_mil_inst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_ru_mil_inst` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `TIME_STAMP_` datetime(3) DEFAULT NULL,
  `CASE_INST_ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `CASE_DEF_ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_MIL_CASE_DEF` (`CASE_DEF_ID_`),
  KEY `ACT_IDX_MIL_CASE_INST` (`CASE_INST_ID_`),
  CONSTRAINT `ACT_FK_MIL_CASE_DEF` FOREIGN KEY (`CASE_DEF_ID_`) REFERENCES `act_cmmn_casedef` (`ID_`),
  CONSTRAINT `ACT_FK_MIL_CASE_INST` FOREIGN KEY (`CASE_INST_ID_`) REFERENCES `act_cmmn_ru_case_inst` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_cmmn_ru_mil_inst`
--

LOCK TABLES `act_cmmn_ru_mil_inst` WRITE;
/*!40000 ALTER TABLE `act_cmmn_ru_mil_inst` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_cmmn_ru_mil_inst` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_cmmn_ru_plan_item_inst`
--

DROP TABLE IF EXISTS `act_cmmn_ru_plan_item_inst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_ru_plan_item_inst` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `REV_` int NOT NULL,
  `CASE_DEF_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CASE_INST_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `STAGE_INST_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `IS_STAGE_` tinyint(1) DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `STATE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `START_USER_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `REFERENCE_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `REFERENCE_TYPE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '',
  `ITEM_DEFINITION_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ITEM_DEFINITION_TYPE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `IS_COMPLETEABLE_` tinyint(1) DEFAULT NULL,
  `IS_COUNT_ENABLED_` tinyint(1) DEFAULT NULL,
  `VAR_COUNT_` int DEFAULT NULL,
  `SENTRY_PART_INST_COUNT_` int DEFAULT NULL,
  `LAST_AVAILABLE_TIME_` datetime(3) DEFAULT NULL,
  `LAST_ENABLED_TIME_` datetime(3) DEFAULT NULL,
  `LAST_DISABLED_TIME_` datetime(3) DEFAULT NULL,
  `LAST_STARTED_TIME_` datetime(3) DEFAULT NULL,
  `LAST_SUSPENDED_TIME_` datetime(3) DEFAULT NULL,
  `COMPLETED_TIME_` datetime(3) DEFAULT NULL,
  `OCCURRED_TIME_` datetime(3) DEFAULT NULL,
  `TERMINATED_TIME_` datetime(3) DEFAULT NULL,
  `EXIT_TIME_` datetime(3) DEFAULT NULL,
  `ENDED_TIME_` datetime(3) DEFAULT NULL,
  `ENTRY_CRITERION_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `EXIT_CRITERION_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `EXTRA_VALUE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DERIVED_CASE_DEF_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `LAST_UNAVAILABLE_TIME_` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_PLAN_ITEM_CASE_DEF` (`CASE_DEF_ID_`),
  KEY `ACT_IDX_PLAN_ITEM_CASE_INST` (`CASE_INST_ID_`),
  KEY `ACT_IDX_PLAN_ITEM_STAGE_INST` (`STAGE_INST_ID_`),
  CONSTRAINT `ACT_FK_PLAN_ITEM_CASE_DEF` FOREIGN KEY (`CASE_DEF_ID_`) REFERENCES `act_cmmn_casedef` (`ID_`),
  CONSTRAINT `ACT_FK_PLAN_ITEM_CASE_INST` FOREIGN KEY (`CASE_INST_ID_`) REFERENCES `act_cmmn_ru_case_inst` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_cmmn_ru_plan_item_inst`
--

LOCK TABLES `act_cmmn_ru_plan_item_inst` WRITE;
/*!40000 ALTER TABLE `act_cmmn_ru_plan_item_inst` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_cmmn_ru_plan_item_inst` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_cmmn_ru_sentry_part_inst`
--

DROP TABLE IF EXISTS `act_cmmn_ru_sentry_part_inst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_ru_sentry_part_inst` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `REV_` int NOT NULL,
  `CASE_DEF_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CASE_INST_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `PLAN_ITEM_INST_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ON_PART_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `IF_PART_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TIME_STAMP_` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_SENTRY_CASE_DEF` (`CASE_DEF_ID_`),
  KEY `ACT_IDX_SENTRY_CASE_INST` (`CASE_INST_ID_`),
  KEY `ACT_IDX_SENTRY_PLAN_ITEM` (`PLAN_ITEM_INST_ID_`),
  CONSTRAINT `ACT_FK_SENTRY_CASE_DEF` FOREIGN KEY (`CASE_DEF_ID_`) REFERENCES `act_cmmn_casedef` (`ID_`),
  CONSTRAINT `ACT_FK_SENTRY_CASE_INST` FOREIGN KEY (`CASE_INST_ID_`) REFERENCES `act_cmmn_ru_case_inst` (`ID_`),
  CONSTRAINT `ACT_FK_SENTRY_PLAN_ITEM` FOREIGN KEY (`PLAN_ITEM_INST_ID_`) REFERENCES `act_cmmn_ru_plan_item_inst` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_cmmn_ru_sentry_part_inst`
--

LOCK TABLES `act_cmmn_ru_sentry_part_inst` WRITE;
/*!40000 ALTER TABLE `act_cmmn_ru_sentry_part_inst` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_cmmn_ru_sentry_part_inst` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_dmn_databasechangelog`
--

DROP TABLE IF EXISTS `act_dmn_databasechangelog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_dmn_databasechangelog` (
  `ID` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `AUTHOR` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `FILENAME` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `DATEEXECUTED` datetime NOT NULL,
  `ORDEREXECUTED` int NOT NULL,
  `EXECTYPE` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  `MD5SUM` varchar(35) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DESCRIPTION` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `COMMENTS` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TAG` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `LIQUIBASE` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CONTEXTS` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `LABELS` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOYMENT_ID` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_dmn_databasechangelog`
--

LOCK TABLES `act_dmn_databasechangelog` WRITE;
/*!40000 ALTER TABLE `act_dmn_databasechangelog` DISABLE KEYS */;
INSERT INTO `act_dmn_databasechangelog` VALUES ('1','activiti','org/flowable/dmn/db/liquibase/flowable-dmn-db-changelog.xml','2026-06-01 23:28:33',1,'EXECUTED','9:5b36e70aee5a2e42f6e7a62ea5fa681b','createTable tableName=ACT_DMN_DEPLOYMENT; createTable tableName=ACT_DMN_DEPLOYMENT_RESOURCE; createTable tableName=ACT_DMN_DECISION_TABLE','',NULL,'4.24.0',NULL,NULL,'0327713487'),('2','flowable','org/flowable/dmn/db/liquibase/flowable-dmn-db-changelog.xml','2026-06-01 23:28:33',2,'EXECUTED','9:fd13fa3f7af55d2b72f763fc261da30d','createTable tableName=ACT_DMN_HI_DECISION_EXECUTION','',NULL,'4.24.0',NULL,NULL,'0327713487'),('3','flowable','org/flowable/dmn/db/liquibase/flowable-dmn-db-changelog.xml','2026-06-01 23:28:33',3,'EXECUTED','9:9f30e6a3557d4b4c713dbb2dcc141782','addColumn tableName=ACT_DMN_HI_DECISION_EXECUTION','',NULL,'4.24.0',NULL,NULL,'0327713487'),('4','flowable','org/flowable/dmn/db/liquibase/flowable-dmn-db-changelog.xml','2026-06-01 23:28:33',4,'EXECUTED','9:41085fbde807dba96104ee75a2fcc4cc','dropColumn columnName=PARENT_DEPLOYMENT_ID_, tableName=ACT_DMN_DECISION_TABLE','',NULL,'4.24.0',NULL,NULL,'0327713487'),('5','flowable','org/flowable/dmn/db/liquibase/flowable-dmn-db-changelog.xml','2026-06-01 23:28:34',5,'EXECUTED','9:169d906b6503ad6907b7e5cd0d70d004','modifyDataType columnName=DEPLOY_TIME_, tableName=ACT_DMN_DEPLOYMENT; modifyDataType columnName=START_TIME_, tableName=ACT_DMN_HI_DECISION_EXECUTION; modifyDataType columnName=END_TIME_, tableName=ACT_DMN_HI_DECISION_EXECUTION','',NULL,'4.24.0',NULL,NULL,'0327713487'),('6','flowable','org/flowable/dmn/db/liquibase/flowable-dmn-db-changelog.xml','2026-06-01 23:28:34',6,'EXECUTED','9:f00f92f3ef1af3fc1604f0323630f9b1','createIndex indexName=ACT_IDX_DEC_TBL_UNIQ, tableName=ACT_DMN_DECISION_TABLE','',NULL,'4.24.0',NULL,NULL,'0327713487'),('7','flowable','org/flowable/dmn/db/liquibase/flowable-dmn-db-changelog.xml','2026-06-01 23:28:34',7,'EXECUTED','9:d24d4c5f44083b4edf1231a7a682a2cd','dropIndex indexName=ACT_IDX_DEC_TBL_UNIQ, tableName=ACT_DMN_DECISION_TABLE; renameTable newTableName=ACT_DMN_DECISION, oldTableName=ACT_DMN_DECISION_TABLE; createIndex indexName=ACT_IDX_DMN_DEC_UNIQ, tableName=ACT_DMN_DECISION','',NULL,'4.24.0',NULL,NULL,'0327713487'),('8','flowable','org/flowable/dmn/db/liquibase/flowable-dmn-db-changelog.xml','2026-06-01 23:28:34',8,'EXECUTED','9:3998ef0958b46fe9c19458183952d2a0','addColumn tableName=ACT_DMN_DECISION','',NULL,'4.24.0',NULL,NULL,'0327713487'),('9','flowable','org/flowable/dmn/db/liquibase/flowable-dmn-db-changelog.xml','2026-06-01 23:28:34',9,'EXECUTED','9:5c9dc65601456faa1aa12f8d3afe0e9e','createIndex indexName=ACT_IDX_DMN_INSTANCE_ID, tableName=ACT_DMN_HI_DECISION_EXECUTION','',NULL,'4.24.0',NULL,NULL,'0327713487');
/*!40000 ALTER TABLE `act_dmn_databasechangelog` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_dmn_databasechangeloglock`
--

DROP TABLE IF EXISTS `act_dmn_databasechangeloglock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_dmn_databasechangeloglock` (
  `ID` int NOT NULL,
  `LOCKED` tinyint(1) NOT NULL,
  `LOCKGRANTED` datetime DEFAULT NULL,
  `LOCKEDBY` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_dmn_databasechangeloglock`
--

LOCK TABLES `act_dmn_databasechangeloglock` WRITE;
/*!40000 ALTER TABLE `act_dmn_databasechangeloglock` DISABLE KEYS */;
INSERT INTO `act_dmn_databasechangeloglock` VALUES (1,0,NULL,NULL);
/*!40000 ALTER TABLE `act_dmn_databasechangeloglock` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_dmn_decision`
--

DROP TABLE IF EXISTS `act_dmn_decision`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_dmn_decision` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `VERSION_` int DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `RESOURCE_NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DESCRIPTION_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DECISION_TYPE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_IDX_DMN_DEC_UNIQ` (`KEY_`,`VERSION_`,`TENANT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_dmn_decision`
--

LOCK TABLES `act_dmn_decision` WRITE;
/*!40000 ALTER TABLE `act_dmn_decision` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_dmn_decision` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_dmn_deployment`
--

DROP TABLE IF EXISTS `act_dmn_deployment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_dmn_deployment` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOY_TIME_` datetime(3) DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `PARENT_DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_dmn_deployment`
--

LOCK TABLES `act_dmn_deployment` WRITE;
/*!40000 ALTER TABLE `act_dmn_deployment` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_dmn_deployment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_dmn_deployment_resource`
--

DROP TABLE IF EXISTS `act_dmn_deployment_resource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_dmn_deployment_resource` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `RESOURCE_BYTES_` longblob,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_dmn_deployment_resource`
--

LOCK TABLES `act_dmn_deployment_resource` WRITE;
/*!40000 ALTER TABLE `act_dmn_deployment_resource` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_dmn_deployment_resource` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_dmn_hi_decision_execution`
--

DROP TABLE IF EXISTS `act_dmn_hi_decision_execution`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_dmn_hi_decision_execution` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `DECISION_DEFINITION_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `START_TIME_` datetime(3) DEFAULT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `INSTANCE_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `EXECUTION_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ACTIVITY_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `FAILED_` tinyint(1) DEFAULT '0',
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `EXECUTION_JSON_` longtext COLLATE utf8mb4_unicode_ci,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_DMN_INSTANCE_ID` (`INSTANCE_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_dmn_hi_decision_execution`
--

LOCK TABLES `act_dmn_hi_decision_execution` WRITE;
/*!40000 ALTER TABLE `act_dmn_hi_decision_execution` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_dmn_hi_decision_execution` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_evt_log`
--

DROP TABLE IF EXISTS `act_evt_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_evt_log` (
  `LOG_NR_` bigint NOT NULL AUTO_INCREMENT,
  `TYPE_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TIME_STAMP_` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DATA_` longblob,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `LOCK_TIME_` timestamp(3) NULL DEFAULT NULL,
  `IS_PROCESSED_` tinyint DEFAULT '0',
  PRIMARY KEY (`LOG_NR_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_evt_log`
--

LOCK TABLES `act_evt_log` WRITE;
/*!40000 ALTER TABLE `act_evt_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_evt_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ge_bytearray`
--

DROP TABLE IF EXISTS `act_ge_bytearray`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ge_bytearray` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `BYTES_` longblob,
  `GENERATED_` tinyint DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_FK_BYTEARR_DEPL` (`DEPLOYMENT_ID_`),
  CONSTRAINT `ACT_FK_BYTEARR_DEPL` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `act_re_deployment` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ge_bytearray`
--

LOCK TABLES `act_ge_bytearray` WRITE;
/*!40000 ALTER TABLE `act_ge_bytearray` DISABLE KEYS */;
INSERT INTO `act_ge_bytearray` VALUES ('81ae4b55-5dd0-11f1-95ae-bca8a6957225',1,'leave-approval.bpmn20.xml','81ae4b54-5dd0-11f1-95ae-bca8a6957225',_binary '<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\r\n             xmlns:flowable=\"http://flowable.org/bpmn\"\r\n             targetNamespace=\"http://flowable.org/bpmn\"\r\n             id=\"leaveApprovalDefinitions\">\r\n\r\n    <process id=\"leave_approval\" name=\"请假审批\" isExecutable=\"true\">\r\n\r\n        <startEvent id=\"start\" name=\"发起请假\">\r\n            <extensionElements>\r\n                <flowable:formProperty id=\"days\" name=\"请假天数\" type=\"long\" required=\"true\"/>\r\n                <flowable:formProperty id=\"reason\" name=\"请假原因\" type=\"string\"/>\r\n            </extensionElements>\r\n        </startEvent>\r\n\r\n        <sequenceFlow id=\"flow1\" sourceRef=\"start\" targetRef=\"manager_approve\"/>\r\n\r\n        <userTask id=\"manager_approve\" name=\"主管审批\"\r\n                  flowable:assignee=\"${workflowApprover.findFirstApprover(\'admin\', \'org_unit\', orgUnitId, requesterId)}\">\r\n            <documentation>主管审批申请 — assignee 自动找申请人所在 org 的 admin</documentation>\r\n            <extensionElements>\r\n                <flowable:taskListener event=\"create\" delegateExpression=\"${leaveApprovalListener}\"/>\r\n            </extensionElements>\r\n        </userTask>\r\n\r\n        <sequenceFlow id=\"flow2\" sourceRef=\"manager_approve\" targetRef=\"check_days\"/>\r\n\r\n        <exclusiveGateway id=\"check_days\" name=\"天数判断\"/>\r\n\r\n        <sequenceFlow id=\"to_director\" sourceRef=\"check_days\" targetRef=\"director_approve\">\r\n            <conditionExpression xsi:type=\"tFormalExpression\"\r\n                                  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n                ${days &gt; 3}\r\n            </conditionExpression>\r\n        </sequenceFlow>\r\n\r\n        <sequenceFlow id=\"to_end\" sourceRef=\"check_days\" targetRef=\"end\">\r\n            <conditionExpression xsi:type=\"tFormalExpression\"\r\n                                  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n                ${days &lt;= 3}\r\n            </conditionExpression>\r\n        </sequenceFlow>\r\n\r\n        <userTask id=\"director_approve\" name=\"部门主任审批 (>3天)\"\r\n                  flowable:assignee=\"${workflowApprover.findFirstApprover(\'admin\', \'org_unit\', parentOrgUnitId, requesterId)}\"/>\r\n\r\n        <sequenceFlow id=\"flow3\" sourceRef=\"director_approve\" targetRef=\"end\"/>\r\n\r\n        <endEvent id=\"end\" name=\"审批完成\"/>\r\n    </process>\r\n</definitions>\r\n',0),('81cb4938-5dd0-11f1-95ae-bca8a6957225',1,'hello-world.bpmn20.xml','81cb4937-5dd0-11f1-95ae-bca8a6957225',_binary '<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\r\n             xmlns:flowable=\"http://flowable.org/bpmn\"\r\n             targetNamespace=\"http://flowable.org/bpmn\"\r\n             id=\"helloWorldDefinitions\">\r\n\r\n    <process id=\"hello_world\" name=\"Hello World 流程\" isExecutable=\"true\">\r\n\r\n        <startEvent id=\"start\" name=\"开始\"/>\r\n\r\n        <sequenceFlow id=\"flow1\" sourceRef=\"start\" targetRef=\"approve_task\"/>\r\n\r\n        <userTask id=\"approve_task\" name=\"审批 hello world\"\r\n                  flowable:assignee=\"${approver}\"/>\r\n\r\n        <sequenceFlow id=\"flow2\" sourceRef=\"approve_task\" targetRef=\"end\"/>\r\n\r\n        <endEvent id=\"end\" name=\"结束\"/>\r\n    </process>\r\n</definitions>\r\n',0),('81d24e1b-5dd0-11f1-95ae-bca8a6957225',1,'access-relation-approval.bpmn20.xml','81d24e1a-5dd0-11f1-95ae-bca8a6957225',_binary '<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\r\n             xmlns:flowable=\"http://flowable.org/bpmn\"\r\n             xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n             targetNamespace=\"http://flowable.org/bpmn\"\r\n             id=\"accessRelationApprovalDefs\">\r\n\r\n    <process id=\"access_relation_approval\" name=\"关系授权审批\" isExecutable=\"true\">\r\n\r\n        <startEvent id=\"start\" name=\"发起授权申请\">\r\n            <extensionElements>\r\n                <flowable:formProperty id=\"resourceType\" name=\"资源类型\" type=\"string\" required=\"true\"/>\r\n                <flowable:formProperty id=\"resourceId\" name=\"资源ID\" type=\"long\" required=\"true\"/>\r\n                <flowable:formProperty id=\"relation\" name=\"关系码\" type=\"string\" required=\"true\"/>\r\n                <flowable:formProperty id=\"subjectType\" name=\"主体类型\" type=\"string\" required=\"true\"/>\r\n                <flowable:formProperty id=\"subjectId\" name=\"主体ID\" type=\"long\" required=\"true\"/>\r\n                <flowable:formProperty id=\"requesterId\" name=\"申请人ID\" type=\"long\" required=\"true\"/>\r\n            </extensionElements>\r\n        </startEvent>\r\n\r\n        <sequenceFlow id=\"flow1\" sourceRef=\"start\" targetRef=\"approve_task\"/>\r\n\r\n        <userTask id=\"approve_task\" name=\"审批关系授权\"\r\n                  flowable:assignee=\"${workflowApprover.findFirstApprover(relation, resourceType, resourceId, requesterId)}\">\r\n            <documentation>由 ApproverFinder 路由审批人</documentation>\r\n        </userTask>\r\n\r\n        <sequenceFlow id=\"flow2\" sourceRef=\"approve_task\" targetRef=\"check_outcome\"/>\r\n\r\n        <exclusiveGateway id=\"check_outcome\" name=\"审批结果\"/>\r\n\r\n        <sequenceFlow id=\"to_grant\" sourceRef=\"check_outcome\" targetRef=\"grant_relation\">\r\n            <conditionExpression xsi:type=\"tFormalExpression\">${approved == true}</conditionExpression>\r\n        </sequenceFlow>\r\n\r\n        <sequenceFlow id=\"to_reject\" sourceRef=\"check_outcome\" targetRef=\"rejected_end\">\r\n            <conditionExpression xsi:type=\"tFormalExpression\">${approved == false}</conditionExpression>\r\n        </sequenceFlow>\r\n\r\n        <serviceTask id=\"grant_relation\" name=\"授权关系\"\r\n                     flowable:expression=\"${flowableRelationGrantBridge.grant(execution)}\"/>\r\n\r\n        <sequenceFlow id=\"flow3\" sourceRef=\"grant_relation\" targetRef=\"approved_end\"/>\r\n\r\n        <endEvent id=\"approved_end\" name=\"授权成功\"/>\r\n        <endEvent id=\"rejected_end\" name=\"审批拒绝\"/>\r\n    </process>\r\n</definitions>\r\n',0);
/*!40000 ALTER TABLE `act_ge_bytearray` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ge_property`
--

DROP TABLE IF EXISTS `act_ge_property`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ge_property` (
  `NAME_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `VALUE_` varchar(300) COLLATE utf8mb3_bin DEFAULT NULL,
  `REV_` int DEFAULT NULL,
  PRIMARY KEY (`NAME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ge_property`
--

LOCK TABLES `act_ge_property` WRITE;
/*!40000 ALTER TABLE `act_ge_property` DISABLE KEYS */;
INSERT INTO `act_ge_property` VALUES ('batch.schema.version','7.0.1.1',1),('cfg.execution-related-entities-count','true',1),('cfg.task-related-entities-count','true',1),('common.schema.version','7.0.1.1',1),('entitylink.schema.version','7.0.1.1',1),('eventsubscription.schema.version','7.0.1.1',1),('identitylink.schema.version','7.0.1.1',1),('job.schema.version','7.0.1.1',1),('next.dbid','1',1),('schema.history','create(7.0.1.1)',1),('schema.version','7.0.1.1',1),('task.schema.version','7.0.1.1',1),('variable.schema.version','7.0.1.1',1);
/*!40000 ALTER TABLE `act_ge_property` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_actinst`
--

DROP TABLE IF EXISTS `act_hi_actinst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_actinst` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `ACT_ID_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CALL_PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `ASSIGNEE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `TRANSACTION_ORDER_` int DEFAULT NULL,
  `DURATION_` bigint DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_ACT_INST_START` (`START_TIME_`),
  KEY `ACT_IDX_HI_ACT_INST_END` (`END_TIME_`),
  KEY `ACT_IDX_HI_ACT_INST_PROCINST` (`PROC_INST_ID_`,`ACT_ID_`),
  KEY `ACT_IDX_HI_ACT_INST_EXEC` (`EXECUTION_ID_`,`ACT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_actinst`
--

LOCK TABLES `act_hi_actinst` WRITE;
/*!40000 ALTER TABLE `act_hi_actinst` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_hi_actinst` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_attachment`
--

DROP TABLE IF EXISTS `act_hi_attachment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_attachment` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `URL_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CONTENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TIME_` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_attachment`
--

LOCK TABLES `act_hi_attachment` WRITE;
/*!40000 ALTER TABLE `act_hi_attachment` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_hi_attachment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_comment`
--

DROP TABLE IF EXISTS `act_hi_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_comment` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TIME_` datetime(3) NOT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACTION_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `MESSAGE_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `FULL_MSG_` longblob,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_comment`
--

LOCK TABLES `act_hi_comment` WRITE;
/*!40000 ALTER TABLE `act_hi_comment` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_hi_comment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_detail`
--

DROP TABLE IF EXISTS `act_hi_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_detail` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `VAR_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REV_` int DEFAULT NULL,
  `TIME_` datetime(3) NOT NULL,
  `BYTEARRAY_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DOUBLE_` double DEFAULT NULL,
  `LONG_` bigint DEFAULT NULL,
  `TEXT_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TEXT2_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_DETAIL_PROC_INST` (`PROC_INST_ID_`),
  KEY `ACT_IDX_HI_DETAIL_ACT_INST` (`ACT_INST_ID_`),
  KEY `ACT_IDX_HI_DETAIL_TIME` (`TIME_`),
  KEY `ACT_IDX_HI_DETAIL_NAME` (`NAME_`),
  KEY `ACT_IDX_HI_DETAIL_TASK_ID` (`TASK_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_detail`
--

LOCK TABLES `act_hi_detail` WRITE;
/*!40000 ALTER TABLE `act_hi_detail` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_hi_detail` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_entitylink`
--

DROP TABLE IF EXISTS `act_hi_entitylink`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_entitylink` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `LINK_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HIERARCHY_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_ENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_HI_ENT_LNK_REF_SCOPE` (`REF_SCOPE_ID_`,`REF_SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_HI_ENT_LNK_ROOT_SCOPE` (`ROOT_SCOPE_ID_`,`ROOT_SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_HI_ENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_entitylink`
--

LOCK TABLES `act_hi_entitylink` WRITE;
/*!40000 ALTER TABLE `act_hi_entitylink` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_hi_entitylink` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_identitylink`
--

DROP TABLE IF EXISTS `act_hi_identitylink`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_identitylink` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `GROUP_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_IDENT_LNK_USER` (`USER_ID_`),
  KEY `ACT_IDX_HI_IDENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_IDENT_LNK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_IDENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_IDENT_LNK_TASK` (`TASK_ID_`),
  KEY `ACT_IDX_HI_IDENT_LNK_PROCINST` (`PROC_INST_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_identitylink`
--

LOCK TABLES `act_hi_identitylink` WRITE;
/*!40000 ALTER TABLE `act_hi_identitylink` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_hi_identitylink` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_procinst`
--

DROP TABLE IF EXISTS `act_hi_procinst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_procinst` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `BUSINESS_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `DURATION_` bigint DEFAULT NULL,
  `START_USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_ACT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `END_ACT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUPER_PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CALLBACK_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CALLBACK_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REFERENCE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REFERENCE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BUSINESS_STATUS_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `PROC_INST_ID_` (`PROC_INST_ID_`),
  KEY `ACT_IDX_HI_PRO_INST_END` (`END_TIME_`),
  KEY `ACT_IDX_HI_PRO_I_BUSKEY` (`BUSINESS_KEY_`),
  KEY `ACT_IDX_HI_PRO_SUPER_PROCINST` (`SUPER_PROCESS_INSTANCE_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_procinst`
--

LOCK TABLES `act_hi_procinst` WRITE;
/*!40000 ALTER TABLE `act_hi_procinst` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_hi_procinst` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_taskinst`
--

DROP TABLE IF EXISTS `act_hi_taskinst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_taskinst` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_DEF_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `STATE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ASSIGNEE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `IN_PROGRESS_TIME_` datetime(3) DEFAULT NULL,
  `IN_PROGRESS_STARTED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CLAIM_TIME_` datetime(3) DEFAULT NULL,
  `CLAIMED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUSPENDED_TIME_` datetime(3) DEFAULT NULL,
  `SUSPENDED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `COMPLETED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DURATION_` bigint DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `PRIORITY_` int DEFAULT NULL,
  `IN_PROGRESS_DUE_DATE_` datetime(3) DEFAULT NULL,
  `DUE_DATE_` datetime(3) DEFAULT NULL,
  `FORM_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `LAST_UPDATED_TIME_` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_TASK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_TASK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_TASK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_TASK_INST_PROCINST` (`PROC_INST_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_taskinst`
--

LOCK TABLES `act_hi_taskinst` WRITE;
/*!40000 ALTER TABLE `act_hi_taskinst` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_hi_taskinst` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_tsk_log`
--

DROP TABLE IF EXISTS `act_hi_tsk_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_tsk_log` (
  `ID_` bigint NOT NULL AUTO_INCREMENT,
  `TYPE_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `TIME_STAMP_` timestamp(3) NOT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DATA_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_tsk_log`
--

LOCK TABLES `act_hi_tsk_log` WRITE;
/*!40000 ALTER TABLE `act_hi_tsk_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_hi_tsk_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_varinst`
--

DROP TABLE IF EXISTS `act_hi_varinst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_varinst` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `VAR_TYPE_` varchar(100) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BYTEARRAY_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DOUBLE_` double DEFAULT NULL,
  `LONG_` bigint DEFAULT NULL,
  `TEXT_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TEXT2_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `META_INFO_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `LAST_UPDATED_TIME_` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_PROCVAR_NAME_TYPE` (`NAME_`,`VAR_TYPE_`),
  KEY `ACT_IDX_HI_VAR_SCOPE_ID_TYPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_VAR_SUB_ID_TYPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_PROCVAR_PROC_INST` (`PROC_INST_ID_`),
  KEY `ACT_IDX_HI_PROCVAR_TASK_ID` (`TASK_ID_`),
  KEY `ACT_IDX_HI_PROCVAR_EXE` (`EXECUTION_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_varinst`
--

LOCK TABLES `act_hi_varinst` WRITE;
/*!40000 ALTER TABLE `act_hi_varinst` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_hi_varinst` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_bytearray`
--

DROP TABLE IF EXISTS `act_id_bytearray`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_bytearray` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BYTES_` longblob,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_bytearray`
--

LOCK TABLES `act_id_bytearray` WRITE;
/*!40000 ALTER TABLE `act_id_bytearray` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_id_bytearray` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_group`
--

DROP TABLE IF EXISTS `act_id_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_group` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_group`
--

LOCK TABLES `act_id_group` WRITE;
/*!40000 ALTER TABLE `act_id_group` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_id_group` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_info`
--

DROP TABLE IF EXISTS `act_id_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_info` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `USER_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `VALUE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PASSWORD_` longblob,
  `PARENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_info`
--

LOCK TABLES `act_id_info` WRITE;
/*!40000 ALTER TABLE `act_id_info` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_id_info` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_membership`
--

DROP TABLE IF EXISTS `act_id_membership`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_membership` (
  `USER_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `GROUP_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`USER_ID_`,`GROUP_ID_`),
  KEY `ACT_FK_MEMB_GROUP` (`GROUP_ID_`),
  CONSTRAINT `ACT_FK_MEMB_GROUP` FOREIGN KEY (`GROUP_ID_`) REFERENCES `act_id_group` (`ID_`),
  CONSTRAINT `ACT_FK_MEMB_USER` FOREIGN KEY (`USER_ID_`) REFERENCES `act_id_user` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_membership`
--

LOCK TABLES `act_id_membership` WRITE;
/*!40000 ALTER TABLE `act_id_membership` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_id_membership` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_priv`
--

DROP TABLE IF EXISTS `act_id_priv`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_priv` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_UNIQ_PRIV_NAME` (`NAME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_priv`
--

LOCK TABLES `act_id_priv` WRITE;
/*!40000 ALTER TABLE `act_id_priv` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_id_priv` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_priv_mapping`
--

DROP TABLE IF EXISTS `act_id_priv_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_priv_mapping` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `PRIV_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `GROUP_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_FK_PRIV_MAPPING` (`PRIV_ID_`),
  KEY `ACT_IDX_PRIV_USER` (`USER_ID_`),
  KEY `ACT_IDX_PRIV_GROUP` (`GROUP_ID_`),
  CONSTRAINT `ACT_FK_PRIV_MAPPING` FOREIGN KEY (`PRIV_ID_`) REFERENCES `act_id_priv` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_priv_mapping`
--

LOCK TABLES `act_id_priv_mapping` WRITE;
/*!40000 ALTER TABLE `act_id_priv_mapping` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_id_priv_mapping` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_property`
--

DROP TABLE IF EXISTS `act_id_property`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_property` (
  `NAME_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `VALUE_` varchar(300) COLLATE utf8mb3_bin DEFAULT NULL,
  `REV_` int DEFAULT NULL,
  PRIMARY KEY (`NAME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_property`
--

LOCK TABLES `act_id_property` WRITE;
/*!40000 ALTER TABLE `act_id_property` DISABLE KEYS */;
INSERT INTO `act_id_property` VALUES ('schema.version','7.0.1.1',1);
/*!40000 ALTER TABLE `act_id_property` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_token`
--

DROP TABLE IF EXISTS `act_id_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_token` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `TOKEN_VALUE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TOKEN_DATE_` timestamp(3) NULL DEFAULT NULL,
  `IP_ADDRESS_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `USER_AGENT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TOKEN_DATA_` varchar(2000) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_token`
--

LOCK TABLES `act_id_token` WRITE;
/*!40000 ALTER TABLE `act_id_token` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_id_token` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_user`
--

DROP TABLE IF EXISTS `act_id_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_user` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `FIRST_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `LAST_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DISPLAY_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EMAIL_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PWD_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PICTURE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_user`
--

LOCK TABLES `act_id_user` WRITE;
/*!40000 ALTER TABLE `act_id_user` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_id_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_procdef_info`
--

DROP TABLE IF EXISTS `act_procdef_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_procdef_info` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `INFO_JSON_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_UNIQ_INFO_PROCDEF` (`PROC_DEF_ID_`),
  KEY `ACT_IDX_INFO_PROCDEF` (`PROC_DEF_ID_`),
  KEY `ACT_FK_INFO_JSON_BA` (`INFO_JSON_ID_`),
  CONSTRAINT `ACT_FK_INFO_JSON_BA` FOREIGN KEY (`INFO_JSON_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_INFO_PROCDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_procdef_info`
--

LOCK TABLES `act_procdef_info` WRITE;
/*!40000 ALTER TABLE `act_procdef_info` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_procdef_info` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_re_deployment`
--

DROP TABLE IF EXISTS `act_re_deployment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_re_deployment` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `DEPLOY_TIME_` timestamp(3) NULL DEFAULT NULL,
  `DERIVED_FROM_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DERIVED_FROM_ROOT_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ENGINE_VERSION_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_re_deployment`
--

LOCK TABLES `act_re_deployment` WRITE;
/*!40000 ALTER TABLE `act_re_deployment` DISABLE KEYS */;
INSERT INTO `act_re_deployment` VALUES ('81ae4b54-5dd0-11f1-95ae-bca8a6957225','[CORE] 请假审批流程示例',NULL,'CORE:processes/leave-approval.bpmn20.xml','','2026-06-01 15:42:33.546',NULL,NULL,'81ae4b54-5dd0-11f1-95ae-bca8a6957225',NULL),('81cb4937-5dd0-11f1-95ae-bca8a6957225','[CORE] Hello World 测试流程',NULL,'CORE:processes/hello-world.bpmn20.xml','','2026-06-01 15:42:33.736',NULL,NULL,'81cb4937-5dd0-11f1-95ae-bca8a6957225',NULL),('81d24e1a-5dd0-11f1-95ae-bca8a6957225','[CORE] 关系授权审批流程',NULL,'CORE:processes/access-relation-approval.bpmn20.xml','','2026-06-01 15:42:33.780',NULL,NULL,'81d24e1a-5dd0-11f1-95ae-bca8a6957225',NULL);
/*!40000 ALTER TABLE `act_re_deployment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_re_model`
--

DROP TABLE IF EXISTS `act_re_model`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_re_model` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LAST_UPDATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `VERSION_` int DEFAULT NULL,
  `META_INFO_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EDITOR_SOURCE_VALUE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EDITOR_SOURCE_EXTRA_VALUE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_FK_MODEL_SOURCE` (`EDITOR_SOURCE_VALUE_ID_`),
  KEY `ACT_FK_MODEL_SOURCE_EXTRA` (`EDITOR_SOURCE_EXTRA_VALUE_ID_`),
  KEY `ACT_FK_MODEL_DEPLOYMENT` (`DEPLOYMENT_ID_`),
  CONSTRAINT `ACT_FK_MODEL_DEPLOYMENT` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `act_re_deployment` (`ID_`),
  CONSTRAINT `ACT_FK_MODEL_SOURCE` FOREIGN KEY (`EDITOR_SOURCE_VALUE_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_MODEL_SOURCE_EXTRA` FOREIGN KEY (`EDITOR_SOURCE_EXTRA_VALUE_ID_`) REFERENCES `act_ge_bytearray` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_re_model`
--

LOCK TABLES `act_re_model` WRITE;
/*!40000 ALTER TABLE `act_re_model` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_re_model` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_re_procdef`
--

DROP TABLE IF EXISTS `act_re_procdef`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_re_procdef` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `VERSION_` int NOT NULL,
  `DEPLOYMENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `RESOURCE_NAME_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DGRM_RESOURCE_NAME_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `HAS_START_FORM_KEY_` tinyint DEFAULT NULL,
  `HAS_GRAPHICAL_NOTATION_` tinyint DEFAULT NULL,
  `SUSPENSION_STATE_` int DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `ENGINE_VERSION_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DERIVED_FROM_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DERIVED_FROM_ROOT_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DERIVED_VERSION_` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_UNIQ_PROCDEF` (`KEY_`,`VERSION_`,`DERIVED_VERSION_`,`TENANT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_re_procdef`
--

LOCK TABLES `act_re_procdef` WRITE;
/*!40000 ALTER TABLE `act_re_procdef` DISABLE KEYS */;
INSERT INTO `act_re_procdef` VALUES ('access_relation_approval:1:81da646c-5dd0-11f1-95ae-bca8a6957225',1,'http://flowable.org/bpmn','关系授权审批','access_relation_approval',1,'81d24e1a-5dd0-11f1-95ae-bca8a6957225','access-relation-approval.bpmn20.xml',NULL,NULL,0,0,1,'',NULL,NULL,NULL,0),('hello_world:1:81cdba39-5dd0-11f1-95ae-bca8a6957225',1,'http://flowable.org/bpmn','Hello World 流程','hello_world',1,'81cb4937-5dd0-11f1-95ae-bca8a6957225','hello-world.bpmn20.xml',NULL,NULL,0,0,1,'',NULL,NULL,NULL,0),('leave_approval:1:81c66736-5dd0-11f1-95ae-bca8a6957225',1,'http://flowable.org/bpmn','请假审批','leave_approval',1,'81ae4b54-5dd0-11f1-95ae-bca8a6957225','leave-approval.bpmn20.xml',NULL,NULL,0,0,1,'',NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `act_re_procdef` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_actinst`
--

DROP TABLE IF EXISTS `act_ru_actinst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_actinst` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `ACT_ID_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CALL_PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `ASSIGNEE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `DURATION_` bigint DEFAULT NULL,
  `TRANSACTION_ORDER_` int DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_RU_ACTI_START` (`START_TIME_`),
  KEY `ACT_IDX_RU_ACTI_END` (`END_TIME_`),
  KEY `ACT_IDX_RU_ACTI_PROC` (`PROC_INST_ID_`),
  KEY `ACT_IDX_RU_ACTI_PROC_ACT` (`PROC_INST_ID_`,`ACT_ID_`),
  KEY `ACT_IDX_RU_ACTI_EXEC` (`EXECUTION_ID_`),
  KEY `ACT_IDX_RU_ACTI_EXEC_ACT` (`EXECUTION_ID_`,`ACT_ID_`),
  KEY `ACT_IDX_RU_ACTI_TASK` (`TASK_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_actinst`
--

LOCK TABLES `act_ru_actinst` WRITE;
/*!40000 ALTER TABLE `act_ru_actinst` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_actinst` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_deadletter_job`
--

DROP TABLE IF EXISTS `act_ru_deadletter_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_deadletter_job` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_DEADLETTER_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_DEADLETTER_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_DEADLETTER_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_DJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_DJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_DJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_DEADLETTER_JOB_EXECUTION` (`EXECUTION_ID_`),
  KEY `ACT_FK_DEADLETTER_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
  KEY `ACT_FK_DEADLETTER_JOB_PROC_DEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_deadletter_job`
--

LOCK TABLES `act_ru_deadletter_job` WRITE;
/*!40000 ALTER TABLE `act_ru_deadletter_job` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_deadletter_job` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_entitylink`
--

DROP TABLE IF EXISTS `act_ru_entitylink`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_entitylink` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `LINK_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HIERARCHY_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_ENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_ENT_LNK_REF_SCOPE` (`REF_SCOPE_ID_`,`REF_SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_ENT_LNK_ROOT_SCOPE` (`ROOT_SCOPE_ID_`,`ROOT_SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_ENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_entitylink`
--

LOCK TABLES `act_ru_entitylink` WRITE;
/*!40000 ALTER TABLE `act_ru_entitylink` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_entitylink` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_event_subscr`
--

DROP TABLE IF EXISTS `act_ru_event_subscr`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_event_subscr` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `EVENT_TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `EVENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACTIVITY_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CONFIGURATION_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATED_` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `LOCK_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_EVENT_SUBSCR_CONFIG_` (`CONFIGURATION_`),
  KEY `ACT_IDX_EVENT_SUBSCR_SCOPEREF_` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_EVENT_EXEC` (`EXECUTION_ID_`),
  CONSTRAINT `ACT_FK_EVENT_EXEC` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_event_subscr`
--

LOCK TABLES `act_ru_event_subscr` WRITE;
/*!40000 ALTER TABLE `act_ru_event_subscr` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_event_subscr` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_execution`
--

DROP TABLE IF EXISTS `act_ru_execution`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_execution` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `BUSINESS_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUPER_EXEC_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `IS_ACTIVE_` tinyint DEFAULT NULL,
  `IS_CONCURRENT_` tinyint DEFAULT NULL,
  `IS_SCOPE_` tinyint DEFAULT NULL,
  `IS_EVENT_SCOPE_` tinyint DEFAULT NULL,
  `IS_MI_ROOT_` tinyint DEFAULT NULL,
  `SUSPENSION_STATE_` int DEFAULT NULL,
  `CACHED_ENT_STATE_` int DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_ACT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_TIME_` datetime(3) DEFAULT NULL,
  `START_USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `LOCK_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `IS_COUNT_ENABLED_` tinyint DEFAULT NULL,
  `EVT_SUBSCR_COUNT_` int DEFAULT NULL,
  `TASK_COUNT_` int DEFAULT NULL,
  `JOB_COUNT_` int DEFAULT NULL,
  `TIMER_JOB_COUNT_` int DEFAULT NULL,
  `SUSP_JOB_COUNT_` int DEFAULT NULL,
  `DEADLETTER_JOB_COUNT_` int DEFAULT NULL,
  `EXTERNAL_WORKER_JOB_COUNT_` int DEFAULT NULL,
  `VAR_COUNT_` int DEFAULT NULL,
  `ID_LINK_COUNT_` int DEFAULT NULL,
  `CALLBACK_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CALLBACK_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REFERENCE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REFERENCE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BUSINESS_STATUS_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_EXEC_BUSKEY` (`BUSINESS_KEY_`),
  KEY `ACT_IDC_EXEC_ROOT` (`ROOT_PROC_INST_ID_`),
  KEY `ACT_IDX_EXEC_REF_ID_` (`REFERENCE_ID_`),
  KEY `ACT_FK_EXE_PROCINST` (`PROC_INST_ID_`),
  KEY `ACT_FK_EXE_PARENT` (`PARENT_ID_`),
  KEY `ACT_FK_EXE_SUPER` (`SUPER_EXEC_`),
  KEY `ACT_FK_EXE_PROCDEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_EXE_PARENT` FOREIGN KEY (`PARENT_ID_`) REFERENCES `act_ru_execution` (`ID_`) ON DELETE CASCADE,
  CONSTRAINT `ACT_FK_EXE_PROCDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
  CONSTRAINT `ACT_FK_EXE_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `act_ru_execution` (`ID_`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `ACT_FK_EXE_SUPER` FOREIGN KEY (`SUPER_EXEC_`) REFERENCES `act_ru_execution` (`ID_`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_execution`
--

LOCK TABLES `act_ru_execution` WRITE;
/*!40000 ALTER TABLE `act_ru_execution` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_execution` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_external_job`
--

DROP TABLE IF EXISTS `act_ru_external_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_external_job` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_EXTERNAL_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_EXTERNAL_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_EXTERNAL_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_EJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_EJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_EJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  CONSTRAINT `ACT_FK_EXTERNAL_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_EXTERNAL_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `act_ge_bytearray` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_external_job`
--

LOCK TABLES `act_ru_external_job` WRITE;
/*!40000 ALTER TABLE `act_ru_external_job` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_external_job` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_history_job`
--

DROP TABLE IF EXISTS `act_ru_history_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_history_job` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ADV_HANDLER_CFG_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_history_job`
--

LOCK TABLES `act_ru_history_job` WRITE;
/*!40000 ALTER TABLE `act_ru_history_job` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_history_job` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_identitylink`
--

DROP TABLE IF EXISTS `act_ru_identitylink`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_identitylink` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `GROUP_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_IDENT_LNK_USER` (`USER_ID_`),
  KEY `ACT_IDX_IDENT_LNK_GROUP` (`GROUP_ID_`),
  KEY `ACT_IDX_IDENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_IDENT_LNK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_IDENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_ATHRZ_PROCEDEF` (`PROC_DEF_ID_`),
  KEY `ACT_FK_TSKASS_TASK` (`TASK_ID_`),
  KEY `ACT_FK_IDL_PROCINST` (`PROC_INST_ID_`),
  CONSTRAINT `ACT_FK_ATHRZ_PROCEDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
  CONSTRAINT `ACT_FK_IDL_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `act_ru_execution` (`ID_`),
  CONSTRAINT `ACT_FK_TSKASS_TASK` FOREIGN KEY (`TASK_ID_`) REFERENCES `act_ru_task` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_identitylink`
--

LOCK TABLES `act_ru_identitylink` WRITE;
/*!40000 ALTER TABLE `act_ru_identitylink` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_identitylink` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_job`
--

DROP TABLE IF EXISTS `act_ru_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_job` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_JOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_JOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_JOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_JOB_EXECUTION` (`EXECUTION_ID_`),
  KEY `ACT_FK_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
  KEY `ACT_FK_JOB_PROC_DEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`),
  CONSTRAINT `ACT_FK_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
  CONSTRAINT `ACT_FK_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_job`
--

LOCK TABLES `act_ru_job` WRITE;
/*!40000 ALTER TABLE `act_ru_job` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_job` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_suspended_job`
--

DROP TABLE IF EXISTS `act_ru_suspended_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_suspended_job` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_SUSPENDED_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_SUSPENDED_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_SUSPENDED_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_SJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_SJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_SJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_SUSPENDED_JOB_EXECUTION` (`EXECUTION_ID_`),
  KEY `ACT_FK_SUSPENDED_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
  KEY `ACT_FK_SUSPENDED_JOB_PROC_DEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_suspended_job`
--

LOCK TABLES `act_ru_suspended_job` WRITE;
/*!40000 ALTER TABLE `act_ru_suspended_job` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_suspended_job` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_task`
--

DROP TABLE IF EXISTS `act_ru_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_task` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `STATE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_DEF_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ASSIGNEE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DELEGATION_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PRIORITY_` int DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `IN_PROGRESS_TIME_` datetime(3) DEFAULT NULL,
  `IN_PROGRESS_STARTED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CLAIM_TIME_` datetime(3) DEFAULT NULL,
  `CLAIMED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUSPENDED_TIME_` datetime(3) DEFAULT NULL,
  `SUSPENDED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `IN_PROGRESS_DUE_DATE_` datetime(3) DEFAULT NULL,
  `DUE_DATE_` datetime(3) DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUSPENSION_STATE_` int DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `FORM_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `IS_COUNT_ENABLED_` tinyint DEFAULT NULL,
  `VAR_COUNT_` int DEFAULT NULL,
  `ID_LINK_COUNT_` int DEFAULT NULL,
  `SUB_TASK_COUNT_` int DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_TASK_CREATE` (`CREATE_TIME_`),
  KEY `ACT_IDX_TASK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_TASK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_TASK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_TASK_EXE` (`EXECUTION_ID_`),
  KEY `ACT_FK_TASK_PROCINST` (`PROC_INST_ID_`),
  KEY `ACT_FK_TASK_PROCDEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_TASK_EXE` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`),
  CONSTRAINT `ACT_FK_TASK_PROCDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
  CONSTRAINT `ACT_FK_TASK_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_task`
--

LOCK TABLES `act_ru_task` WRITE;
/*!40000 ALTER TABLE `act_ru_task` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_task` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_timer_job`
--

DROP TABLE IF EXISTS `act_ru_timer_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_timer_job` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_TIMER_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_TIMER_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_TIMER_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_TIMER_JOB_DUEDATE` (`DUEDATE_`),
  KEY `ACT_IDX_TJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_TJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_TJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_TIMER_JOB_EXECUTION` (`EXECUTION_ID_`),
  KEY `ACT_FK_TIMER_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
  KEY `ACT_FK_TIMER_JOB_PROC_DEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_timer_job`
--

LOCK TABLES `act_ru_timer_job` WRITE;
/*!40000 ALTER TABLE `act_ru_timer_job` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_timer_job` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_variable`
--

DROP TABLE IF EXISTS `act_ru_variable`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_variable` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BYTEARRAY_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DOUBLE_` double DEFAULT NULL,
  `LONG_` bigint DEFAULT NULL,
  `TEXT_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TEXT2_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `META_INFO_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_RU_VAR_SCOPE_ID_TYPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_RU_VAR_SUB_ID_TYPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_VAR_BYTEARRAY` (`BYTEARRAY_ID_`),
  KEY `ACT_IDX_VARIABLE_TASK_ID` (`TASK_ID_`),
  KEY `ACT_FK_VAR_EXE` (`EXECUTION_ID_`),
  KEY `ACT_FK_VAR_PROCINST` (`PROC_INST_ID_`),
  CONSTRAINT `ACT_FK_VAR_BYTEARRAY` FOREIGN KEY (`BYTEARRAY_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_VAR_EXE` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`),
  CONSTRAINT `ACT_FK_VAR_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_variable`
--

LOCK TABLES `act_ru_variable` WRITE;
/*!40000 ALTER TABLE `act_ru_variable` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_variable` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `activity_events`
--

DROP TABLE IF EXISTS `activity_events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `activity_events` (
  `id` bigint NOT NULL COMMENT 'Snowflake PK',
  `request_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求追踪ID（同一请求产生的多条事件共享）',
  `resource_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ORG_UNIT / PLACE / USER / ROLE / STUDENT / INSPECTION ...',
  `resource_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '资源ID',
  `resource_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '资源显示名（冗余，方便查看）',
  `action` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'CREATE / UPDATE / DELETE / FREEZE / ASSIGN / LOGIN ...',
  `action_label` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '可读描述: "更新组织单元" / "分配负责人"',
  `result` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS / FAILURE / PARTIAL',
  `error_message` text COLLATE utf8mb4_unicode_ci,
  `changed_fields` json DEFAULT NULL COMMENT '[{fieldName, oldValue, newValue}] 字段级 diff',
  `before_snapshot` json DEFAULT NULL COMMENT '变更前快照（可选，关键资源用）',
  `after_snapshot` json DEFAULT NULL COMMENT '变更后快照（可选）',
  `user_id` bigint DEFAULT NULL COMMENT '操作人ID（系统动作为 null）',
  `user_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作人姓名',
  `source_ip` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_agent` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `api_endpoint` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '/api/org-units/123',
  `http_method` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'GET / POST / PUT / DELETE',
  `reason` text COLLATE utf8mb4_unicode_ci COMMENT '操作原因/备注',
  `module` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '模块分组: organization / place / access / inspection / user',
  `tags` json DEFAULT NULL COMMENT '扩展标签 KV',
  `occurred_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_resource` (`resource_type`,`resource_id`,`occurred_at` DESC),
  KEY `idx_user` (`user_id`,`occurred_at` DESC),
  KEY `idx_module_time` (`module`,`occurred_at` DESC),
  KEY `idx_action_time` (`action`,`occurred_at` DESC),
  KEY `idx_occurred_at` (`occurred_at` DESC),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='统一活动事件表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `activity_events`
--

LOCK TABLES `activity_events` WRITE;
/*!40000 ALTER TABLE `activity_events` DISABLE KEYS */;
INSERT INTO `activity_events` VALUES (2061473644219002882,NULL,'AUTH','',NULL,'LOGIN','用户登录','SUCCESS',NULL,NULL,NULL,NULL,NULL,'anonymousUser','0:0:0:0:0:0:0:1','curl/8.15.0','/api/auth/login','POST',NULL,'access',NULL,'2026-06-01 23:43:10.621','2026-06-01 23:43:12',1),(2061473875245461506,NULL,'AUTH','',NULL,'LOGIN','用户登录','SUCCESS',NULL,NULL,NULL,NULL,NULL,'anonymousUser','0:0:0:0:0:0:0:1','curl/8.15.0','/api/auth/login','POST',NULL,'access',NULL,'2026-06-01 23:44:06.715','2026-06-01 23:44:07',1);
/*!40000 ALTER TABLE `activity_events` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `analysis_snapshots`
--

DROP TABLE IF EXISTS `analysis_snapshots`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `analysis_snapshots` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `config_id` bigint NOT NULL COMMENT 'Analysis Config ID',
  `snapshot_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Snapshot Name',
  `snapshot_desc` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Snapshot Description',
  `record_ids` json DEFAULT NULL COMMENT 'Included Record IDs',
  `class_ids` json DEFAULT NULL COMMENT 'Included Class IDs',
  `date_range_start` date DEFAULT NULL COMMENT 'Data Start Date',
  `date_range_end` date DEFAULT NULL COMMENT 'Data End Date',
  `record_count` int DEFAULT '0' COMMENT 'Record Count',
  `class_count` int DEFAULT '0' COMMENT 'Class Count',
  `total_score` decimal(12,2) DEFAULT '0.00' COMMENT 'Total Deductions',
  `avg_score` decimal(10,2) DEFAULT '0.00' COMMENT 'Average Deduction',
  `overview_data` json DEFAULT NULL COMMENT 'Overview Data (JSON)',
  `metrics_data` json DEFAULT NULL COMMENT 'Metrics Results (JSON)',
  `generated_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Generated At',
  `generated_by` bigint DEFAULT NULL COMMENT 'Generated By User ID',
  `generated_by_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Generated By User Name',
  `is_auto` tinyint(1) DEFAULT '0' COMMENT 'Is Auto-Generated (0/1)',
  `version` int DEFAULT '1' COMMENT 'Version Number',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_config_id` (`config_id`),
  KEY `idx_generated_at` (`generated_at`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Analysis Snapshots';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `analysis_snapshots`
--

LOCK TABLES `analysis_snapshots` WRITE;
/*!40000 ALTER TABLE `analysis_snapshots` DISABLE KEYS */;
/*!40000 ALTER TABLE `analysis_snapshots` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `announcements`
--

DROP TABLE IF EXISTS `announcements`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `announcements` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公告标题',
  `content` text COLLATE utf8mb4_unicode_ci COMMENT '公告内容',
  `announcement_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'notice' COMMENT '类型:notice,announcement,warning',
  `priority` tinyint DEFAULT '1' COMMENT '优先级:1普通,2重要,3紧急',
  `publisher_id` bigint DEFAULT NULL COMMENT '发布人ID',
  `publisher_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '发布人姓名',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `start_time` datetime DEFAULT NULL COMMENT '生效开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '生效结束时间',
  `target_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'all' COMMENT '目标类型:all,role,user',
  `target_ids` json DEFAULT NULL COMMENT '目标ID列表',
  `attachment_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '附件URL',
  `is_published` tinyint DEFAULT '0' COMMENT '是否已发布',
  `is_pinned` tinyint DEFAULT '0' COMMENT '是否置顶',
  `view_count` int DEFAULT '0' COMMENT '浏览次数',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_publish_time` (`publish_time`),
  KEY `idx_is_published` (`is_published`),
  KEY `idx_is_pinned` (`is_pinned`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `announcements`
--

LOCK TABLES `announcements` WRITE;
/*!40000 ALTER TABLE `announcements` DISABLE KEYS */;
/*!40000 ALTER TABLE `announcements` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `appeals`
--

DROP TABLE IF EXISTS `appeals`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `appeals` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `inspection_record_id` bigint DEFAULT NULL COMMENT '检查记录ID',
  `deduction_detail_id` bigint DEFAULT NULL COMMENT '扣分明细ID',
  `class_id` bigint DEFAULT NULL COMMENT '班级ID',
  `appeal_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '申诉编码',
  `reason` text COLLATE utf8mb4_unicode_ci COMMENT '申诉原因',
  `attachments` json DEFAULT NULL COMMENT '附件URL列表',
  `original_deduction` decimal(10,2) DEFAULT NULL COMMENT '原扣分',
  `requested_deduction` decimal(10,2) DEFAULT NULL COMMENT '申请扣分',
  `approved_deduction` decimal(10,2) DEFAULT NULL COMMENT '批准扣分',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING' COMMENT '状态',
  `applicant_id` bigint DEFAULT NULL COMMENT '申请人ID',
  `applied_at` datetime DEFAULT NULL COMMENT '申请时间',
  `level1_reviewer_id` bigint DEFAULT NULL COMMENT '一级审核人ID',
  `level1_reviewed_at` datetime DEFAULT NULL COMMENT '一级审核时间',
  `level1_comment` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '一级审核意见',
  `level2_reviewer_id` bigint DEFAULT NULL COMMENT '二级审核人ID',
  `level2_reviewed_at` datetime DEFAULT NULL COMMENT '二级审核时间',
  `level2_comment` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '二级审核意见',
  `effective_at` datetime DEFAULT NULL COMMENT '生效时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_appeal_code` (`appeal_code`),
  KEY `idx_inspection_record_id` (`inspection_record_id`),
  KEY `idx_class_id` (`class_id`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='申诉表(V4)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `appeals`
--

LOCK TABLES `appeals` WRITE;
/*!40000 ALTER TABLE `appeals` DISABLE KEYS */;
/*!40000 ALTER TABLE `appeals` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `asset`
--

DROP TABLE IF EXISTS `asset`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `asset` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `asset_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '资产编码',
  `asset_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '资产名称',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `brand` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '品牌',
  `model` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '型号',
  `unit` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '单位',
  `quantity` int DEFAULT '1' COMMENT '数量',
  `original_value` decimal(12,2) DEFAULT NULL COMMENT '原值',
  `net_value` decimal(12,2) DEFAULT NULL COMMENT '净值',
  `purchase_date` date DEFAULT NULL COMMENT '购入日期',
  `warranty_date` date DEFAULT NULL COMMENT '保修日期',
  `supplier` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商',
  `status` int DEFAULT '1' COMMENT '状态:1正常,2维修,3报废,4借出',
  `management_mode` int DEFAULT '1' COMMENT '管理模式:1固定资产,2耗材',
  `location_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '位置类型',
  `location_id` bigint DEFAULT NULL COMMENT '位置ID',
  `location_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '位置名称',
  `responsible_user_id` bigint DEFAULT NULL COMMENT '责任人ID',
  `responsible_user_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '责任人姓名',
  `space_id` bigint DEFAULT NULL COMMENT '场所ID',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `category_type` int DEFAULT NULL COMMENT '折旧分类类型',
  `depreciation_method` int DEFAULT NULL COMMENT '折旧方法',
  `residual_value` decimal(12,2) DEFAULT NULL COMMENT '残值',
  `accumulated_depreciation` decimal(12,2) DEFAULT '0.00' COMMENT '累计折旧',
  `useful_life` int DEFAULT NULL COMMENT '使用年限(月)',
  `stock_warning_threshold` int DEFAULT NULL COMMENT '库存预警阈值',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_asset_code` (`asset_code`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_location` (`location_type`,`location_id`),
  KEY `idx_responsible_user_id` (`responsible_user_id`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_tenant` (`tenant_id`),
  KEY `idx_space_id` (`space_id`),
  KEY `idx_asset_management_mode` (`management_mode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `asset`
--

LOCK TABLES `asset` WRITE;
/*!40000 ALTER TABLE `asset` DISABLE KEYS */;
/*!40000 ALTER TABLE `asset` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `asset_alert`
--

DROP TABLE IF EXISTS `asset_alert`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `asset_alert` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `alert_type` int NOT NULL COMMENT '预警类型',
  `asset_id` bigint DEFAULT NULL COMMENT '资产ID',
  `asset_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '资产编码',
  `asset_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '资产名称',
  `borrow_id` bigint DEFAULT NULL COMMENT '借用ID',
  `alert_content` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '预警内容',
  `alert_level` int DEFAULT '1' COMMENT '预警级别',
  `is_read` tinyint(1) DEFAULT '0' COMMENT '是否已读',
  `is_handled` tinyint(1) DEFAULT '0' COMMENT '是否已处理',
  `handle_remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '处理备注',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `handler_id` bigint DEFAULT NULL COMMENT '处理人ID',
  `handler_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '处理人姓名',
  `notify_user_id` bigint DEFAULT NULL COMMENT '通知人ID',
  `notify_user_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '通知人姓名',
  `alert_time` datetime DEFAULT NULL COMMENT '预警时间',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_asset_id` (`asset_id`),
  KEY `idx_alert_type` (`alert_type`),
  KEY `idx_is_handled` (`is_handled`),
  KEY `idx_notify_user_id` (`notify_user_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产预警表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `asset_alert`
--

LOCK TABLES `asset_alert` WRITE;
/*!40000 ALTER TABLE `asset_alert` DISABLE KEYS */;
/*!40000 ALTER TABLE `asset_alert` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `asset_approval`
--

DROP TABLE IF EXISTS `asset_approval`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `asset_approval` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `approval_no` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批单号',
  `approval_type` int DEFAULT NULL COMMENT '审批类型',
  `business_id` bigint DEFAULT NULL COMMENT '业务ID',
  `asset_id` bigint DEFAULT NULL COMMENT '资产ID',
  `asset_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '资产名称',
  `applicant_id` bigint DEFAULT NULL COMMENT '申请人ID',
  `applicant_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '申请人姓名',
  `applicant_dept` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '申请人部门',
  `approver_id` bigint DEFAULT NULL COMMENT '审批人ID',
  `approver_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批人姓名',
  `status` int DEFAULT '0' COMMENT '状态',
  `apply_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '申请原因',
  `apply_quantity` int DEFAULT NULL COMMENT '申请数量',
  `apply_amount` decimal(12,2) DEFAULT NULL COMMENT '申请金额',
  `approval_remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批备注',
  `apply_time` datetime DEFAULT NULL COMMENT '申请时间',
  `approval_time` datetime DEFAULT NULL COMMENT '审批时间',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_approval_no` (`approval_no`),
  KEY `idx_asset_id` (`asset_id`),
  KEY `idx_applicant_id` (`applicant_id`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产审批表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `asset_approval`
--

LOCK TABLES `asset_approval` WRITE;
/*!40000 ALTER TABLE `asset_approval` DISABLE KEYS */;
/*!40000 ALTER TABLE `asset_approval` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `asset_borrow`
--

DROP TABLE IF EXISTS `asset_borrow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `asset_borrow` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `borrow_no` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '借用单号',
  `borrow_type` int DEFAULT NULL COMMENT '借用类型',
  `asset_id` bigint NOT NULL COMMENT '资产ID',
  `asset_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '资产编码',
  `asset_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '资产名称',
  `quantity` int DEFAULT '1' COMMENT '借用数量',
  `borrower_id` bigint DEFAULT NULL COMMENT '借用人ID',
  `borrower_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '借用人姓名',
  `borrower_dept` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '借用人部门',
  `borrower_phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '借用人电话',
  `borrow_date` datetime DEFAULT NULL COMMENT '借用日期',
  `expected_return_date` date DEFAULT NULL COMMENT '预计归还日期',
  `actual_return_date` datetime DEFAULT NULL COMMENT '实际归还日期',
  `return_condition` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '归还状况',
  `return_remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '归还备注',
  `returner_id` bigint DEFAULT NULL COMMENT '归还人ID',
  `returner_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '归还人姓名',
  `purpose` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '借用用途',
  `status` int DEFAULT '0' COMMENT '状态',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作人姓名',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_borrow_no` (`borrow_no`),
  KEY `idx_asset_id` (`asset_id`),
  KEY `idx_borrower_id` (`borrower_id`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产借用表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `asset_borrow`
--

LOCK TABLES `asset_borrow` WRITE;
/*!40000 ALTER TABLE `asset_borrow` DISABLE KEYS */;
/*!40000 ALTER TABLE `asset_borrow` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `asset_category`
--

DROP TABLE IF EXISTS `asset_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `asset_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` bigint DEFAULT NULL COMMENT '父分类ID',
  `category_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类编码',
  `category_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类名称',
  `category_type` int DEFAULT NULL COMMENT '分类类型',
  `default_management_mode` int DEFAULT NULL COMMENT '默认管理模式',
  `depreciation_years` int DEFAULT NULL COMMENT '折旧年限',
  `unit` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '单位',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_code` (`category_code`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_tenant` (`tenant_id`),
  KEY `idx_asset_category_default_mode` (`default_management_mode`)
) ENGINE=InnoDB AUTO_INCREMENT=47 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产分类表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `asset_category`
--

LOCK TABLES `asset_category` WRITE;
/*!40000 ALTER TABLE `asset_category` DISABLE KEYS */;
INSERT INTO `asset_category` VALUES (1,NULL,'TEACH','教学设备',1,1,NULL,'台',1,NULL,'2026-06-01 22:57:19','2026-06-01 22:57:20',0,1),(2,NULL,'OFFICE','办公设备',1,1,NULL,'台',2,NULL,'2026-06-01 22:57:19','2026-06-01 22:57:20',0,1),(3,NULL,'DORM','宿舍设施',1,1,NULL,'件',3,NULL,'2026-06-01 22:57:19','2026-06-01 22:57:20',0,1),(4,NULL,'OTHER','其他资产',1,1,NULL,'件',4,NULL,'2026-06-01 22:57:19','2026-06-01 22:57:20',0,1),(5,1,'TEACH-DESK','课桌',1,1,NULL,'张',1,NULL,'2026-06-01 22:57:19','2026-06-01 22:57:20',0,1),(6,1,'TEACH-CHAIR','椅凳',1,1,NULL,'把',2,NULL,'2026-06-01 22:57:19','2026-06-01 22:57:20',0,1),(7,1,'TEACH-PODIUM','讲台',1,1,NULL,'个',3,NULL,'2026-06-01 22:57:19','2026-06-01 22:57:20',0,1),(8,1,'TEACH-BOARD','黑板/白板',1,1,NULL,'块',4,NULL,'2026-06-01 22:57:19','2026-06-01 22:57:20',0,1),(9,1,'TEACH-MULTIMEDIA','多媒体设备',1,1,NULL,'套',5,NULL,'2026-06-01 22:57:19','2026-06-01 22:57:20',0,1),(10,1,'TEACH-LECTERN','讲桌',1,1,NULL,'张',6,NULL,'2026-06-01 22:57:19','2026-06-01 22:57:20',0,1),(11,1,'TEACH-PROJECTOR','投影仪',1,1,NULL,'台',7,NULL,'2026-06-01 22:57:19','2026-06-01 22:57:20',0,1),(12,1,'TEACH-AC','空调',1,1,NULL,'台',8,NULL,'2026-06-01 22:57:19','2026-06-01 22:57:20',0,1),(13,2,'OFFICE-PC','电脑',1,1,NULL,'台',1,NULL,'2026-06-01 22:57:19','2026-06-01 22:57:20',0,1),(14,2,'OFFICE-PRINT','打印机',1,1,NULL,'台',2,NULL,'2026-06-01 22:57:19','2026-06-01 22:57:20',0,1),(15,2,'OFFICE-DESK','办公桌',1,1,NULL,'张',3,NULL,'2026-06-01 22:57:19','2026-06-01 22:57:20',0,1),(16,2,'OFFICE-CHAIR','办公椅',1,1,NULL,'把',4,NULL,'2026-06-01 22:57:19','2026-06-01 22:57:20',0,1),(17,2,'OFFICE-CABINET','文件柜',1,1,NULL,'个',5,NULL,'2026-06-01 22:57:19','2026-06-01 22:57:20',0,1),(18,3,'DORM-BED','床铺',1,1,NULL,'张',1,NULL,'2026-06-01 22:57:19','2026-06-01 22:57:20',0,1),(19,3,'DORM-CABINET','衣柜',1,1,NULL,'个',2,NULL,'2026-06-01 22:57:19','2026-06-01 22:57:20',0,1),(20,3,'DORM-DESK','书桌',1,1,NULL,'张',3,NULL,'2026-06-01 22:57:19','2026-06-01 22:57:20',0,1),(21,3,'DORM-CHAIR','椅子',1,1,NULL,'把',4,NULL,'2026-06-01 22:57:19','2026-06-01 22:57:20',0,1),(22,3,'DORM-FAN','风扇',1,1,NULL,'台',5,NULL,'2026-06-01 22:57:19','2026-06-01 22:57:20',0,1),(23,3,'DORM-AC','空调',1,1,NULL,'台',6,NULL,'2026-06-01 22:57:19','2026-06-01 22:57:20',0,1);
/*!40000 ALTER TABLE `asset_category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `asset_depreciation`
--

DROP TABLE IF EXISTS `asset_depreciation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `asset_depreciation` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `asset_id` bigint NOT NULL COMMENT '资产ID',
  `asset_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '资产编码',
  `depreciation_period` varchar(6) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '折旧期间(YYYYMM)',
  `beginning_value` decimal(12,2) DEFAULT NULL COMMENT '期初原值',
  `beginning_accumulated_depreciation` decimal(12,2) DEFAULT NULL COMMENT '期初累计折旧',
  `beginning_net_value` decimal(12,2) DEFAULT NULL COMMENT '期初净值',
  `depreciation_amount` decimal(12,2) DEFAULT NULL COMMENT '本期折旧额',
  `ending_accumulated_depreciation` decimal(12,2) DEFAULT NULL COMMENT '期末累计折旧',
  `ending_net_value` decimal(12,2) DEFAULT NULL COMMENT '期末净值',
  `used_months` int DEFAULT NULL COMMENT '已使用月数',
  `remaining_months` int DEFAULT NULL COMMENT '剩余月数',
  `depreciation_method` int DEFAULT NULL COMMENT '折旧方法',
  `depreciation_date` date DEFAULT NULL COMMENT '折旧日期',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_asset_id` (`asset_id`),
  KEY `idx_period` (`depreciation_period`),
  KEY `idx_depreciation_date` (`depreciation_date`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产折旧表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `asset_depreciation`
--

LOCK TABLES `asset_depreciation` WRITE;
/*!40000 ALTER TABLE `asset_depreciation` DISABLE KEYS */;
/*!40000 ALTER TABLE `asset_depreciation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `asset_history`
--

DROP TABLE IF EXISTS `asset_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `asset_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `asset_id` bigint NOT NULL COMMENT '资产ID',
  `change_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '变更类型',
  `change_content` text COLLATE utf8mb4_unicode_ci COMMENT '变更内容',
  `old_location_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '原位置类型',
  `old_location_id` bigint DEFAULT NULL COMMENT '原位置ID',
  `old_location_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '原位置名称',
  `new_location_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '新位置类型',
  `new_location_id` bigint DEFAULT NULL COMMENT '新位置ID',
  `new_location_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '新位置名称',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作人姓名',
  `operate_time` datetime DEFAULT NULL COMMENT '操作时间',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_asset_id` (`asset_id`),
  KEY `idx_change_type` (`change_type`),
  KEY `idx_operate_time` (`operate_time`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产历史表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `asset_history`
--

LOCK TABLES `asset_history` WRITE;
/*!40000 ALTER TABLE `asset_history` DISABLE KEYS */;
/*!40000 ALTER TABLE `asset_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `asset_inventory`
--

DROP TABLE IF EXISTS `asset_inventory`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `asset_inventory` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `inventory_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '盘点编码',
  `inventory_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '盘点名称',
  `scope_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '范围类型',
  `scope_value` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '范围值',
  `start_date` date DEFAULT NULL COMMENT '开始日期',
  `end_date` date DEFAULT NULL COMMENT '结束日期',
  `status` int DEFAULT '0' COMMENT '状态',
  `total_count` int DEFAULT '0' COMMENT '总数',
  `checked_count` int DEFAULT '0' COMMENT '已盘点数',
  `profit_count` int DEFAULT '0' COMMENT '盘盈数',
  `loss_count` int DEFAULT '0' COMMENT '盘亏数',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inventory_code` (`inventory_code`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产盘点表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `asset_inventory`
--

LOCK TABLES `asset_inventory` WRITE;
/*!40000 ALTER TABLE `asset_inventory` DISABLE KEYS */;
/*!40000 ALTER TABLE `asset_inventory` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `asset_inventory_detail`
--

DROP TABLE IF EXISTS `asset_inventory_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `asset_inventory_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `inventory_id` bigint NOT NULL COMMENT '盘点ID',
  `asset_id` bigint NOT NULL COMMENT '资产ID',
  `expected_quantity` int DEFAULT NULL COMMENT '预期数量',
  `actual_quantity` int DEFAULT NULL COMMENT '实际数量',
  `difference` int DEFAULT NULL COMMENT '差异',
  `result_type` int DEFAULT NULL COMMENT '结果类型:1正常,2盘盈,3盘亏',
  `check_time` datetime DEFAULT NULL COMMENT '盘点时间',
  `checker_id` bigint DEFAULT NULL COMMENT '盘点人ID',
  `checker_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '盘点人姓名',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_inventory_id` (`inventory_id`),
  KEY `idx_asset_id` (`asset_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产盘点明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `asset_inventory_detail`
--

LOCK TABLES `asset_inventory_detail` WRITE;
/*!40000 ALTER TABLE `asset_inventory_detail` DISABLE KEYS */;
/*!40000 ALTER TABLE `asset_inventory_detail` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `asset_maintenance`
--

DROP TABLE IF EXISTS `asset_maintenance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `asset_maintenance` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `asset_id` bigint NOT NULL COMMENT '资产ID',
  `maintenance_type` int DEFAULT NULL COMMENT '维修类型',
  `fault_desc` text COLLATE utf8mb4_unicode_ci COMMENT '故障描述',
  `start_date` date DEFAULT NULL COMMENT '开始日期',
  `end_date` date DEFAULT NULL COMMENT '结束日期',
  `cost` decimal(10,2) DEFAULT NULL COMMENT '维修费用',
  `maintainer` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '维修人',
  `result` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '维修结果',
  `status` int DEFAULT '0' COMMENT '状态',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_asset_id` (`asset_id`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产维修表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `asset_maintenance`
--

LOCK TABLES `asset_maintenance` WRITE;
/*!40000 ALTER TABLE `asset_maintenance` DISABLE KEYS */;
/*!40000 ALTER TABLE `asset_maintenance` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `attendance_records`
--

DROP TABLE IF EXISTS `attendance_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `semester_id` bigint NOT NULL COMMENT '学期ID',
  `course_id` bigint DEFAULT NULL COMMENT '课程ID（课程考勤时）',
  `org_unit_id` bigint DEFAULT NULL,
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `attendance_date` date NOT NULL COMMENT '考勤日期',
  `period` int DEFAULT NULL COMMENT '节次（课程考勤时）',
  `attendance_type` tinyint DEFAULT '1' COMMENT '1课程考勤 2日常考勤',
  `status` tinyint NOT NULL COMMENT '1出勤 2迟到 3早退 4请假 5旷课',
  `check_in_time` datetime DEFAULT NULL COMMENT '签到时间',
  `check_method` varchar(20) DEFAULT 'MANUAL' COMMENT '签到方式: MANUAL/NFC/QR/FACE',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `recorded_by` bigint DEFAULT NULL COMMENT '记录人ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_student_date` (`student_id`,`attendance_date`),
  KEY `idx_class_date` (`org_unit_id`,`attendance_date`),
  KEY `idx_course_date` (`course_id`,`attendance_date`),
  KEY `idx_semester` (`semester_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='考勤记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `attendance_records`
--

LOCK TABLES `attendance_records` WRITE;
/*!40000 ALTER TABLE `attendance_records` DISABLE KEYS */;
/*!40000 ALTER TABLE `attendance_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `audit_trail`
--

DROP TABLE IF EXISTS `audit_trail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_trail` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `module` varchar(50) NOT NULL COMMENT '模块',
  `action` varchar(30) NOT NULL COMMENT '操作',
  `resource_type` varchar(50) NOT NULL COMMENT '资源类型',
  `resource_id` varchar(50) DEFAULT NULL COMMENT '资源ID',
  `resource_name` varchar(200) DEFAULT NULL COMMENT '资源名称',
  `before_data` json DEFAULT NULL COMMENT '变更前',
  `after_data` json DEFAULT NULL COMMENT '变更后',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) DEFAULT NULL COMMENT '操作人',
  `ip_address` varchar(50) DEFAULT NULL COMMENT 'IP',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_module` (`module`),
  KEY `idx_resource` (`resource_type`,`resource_id`),
  KEY `idx_operator` (`operator_id`),
  KEY `idx_time` (`created_at`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作审计日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `audit_trail`
--

LOCK TABLES `audit_trail` WRITE;
/*!40000 ALTER TABLE `audit_trail` DISABLE KEYS */;
/*!40000 ALTER TABLE `audit_trail` ENABLE KEYS */;
UNLOCK TABLES;


--
-- Table structure for table `buildings`
--

DROP TABLE IF EXISTS `buildings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `buildings` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `building_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '楼栋编码',
  `building_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '楼栋名称',
  `building_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型:DORMITORY,TEACHING,OFFICE',
  `total_floors` int DEFAULT '1' COMMENT '总楼层数',
  `address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '地址',
  `manager_id` bigint DEFAULT NULL COMMENT '管理员ID',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '状态:0禁用,1启用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_building_code` (`building_code`),
  KEY `idx_building_type` (`building_type`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_tenant` (`tenant_id`),
  KEY `idx_buildings_name` (`building_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='楼栋表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `buildings`
--

LOCK TABLES `buildings` WRITE;
/*!40000 ALTER TABLE `buildings` DISABLE KEYS */;
/*!40000 ALTER TABLE `buildings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `calculation_rules`
--

DROP TABLE IF EXISTS `calculation_rules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `calculation_rules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则代码',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则名称',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
  `rule_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则类型: ceiling/floor/veto/progressive/bonus/penalty',
  `condition_formula` text COLLATE utf8mb4_unicode_ci COMMENT '条件公式(JavaScript)，返回boolean',
  `action_formula` text COLLATE utf8mb4_unicode_ci COMMENT '动作公式(JavaScript)，返回调整后的分数',
  `parameters_schema` json DEFAULT NULL COMMENT '规则参数定义',
  `default_parameters` json DEFAULT NULL COMMENT '默认参数',
  `priority` int DEFAULT '0' COMMENT '执行优先级，数字越小越先执行',
  `stop_on_match` tinyint(1) DEFAULT '0' COMMENT '匹配后是否停止后续规则',
  `is_system` tinyint(1) DEFAULT '0',
  `is_enabled` tinyint(1) DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` int DEFAULT '0',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_calc_rule_type` (`rule_type`),
  KEY `idx_calc_rule_priority` (`priority`),
  KEY `idx_calc_rule_deleted` (`deleted`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计算规则定义表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `calculation_rules`
--

LOCK TABLES `calculation_rules` WRITE;
/*!40000 ALTER TABLE `calculation_rules` DISABLE KEYS */;
INSERT INTO `calculation_rules` VALUES (1,'CEILING','分数封顶','限制最高得分','ceiling','ctx.score > ctx.maxScore','ctx.maxScore','{\"maxScore\": {\"type\": \"number\", \"label\": \"最高分\", \"default\": 100}}',NULL,10,0,1,1,'2026-06-01 22:57:01','2026-06-01 22:57:01',0,1),(2,'FLOOR','分数保底','限制最低得分','floor','ctx.score < ctx.minScore','ctx.minScore','{\"minScore\": {\"type\": \"number\", \"label\": \"最低分\", \"default\": 0}}',NULL,20,0,1,1,'2026-06-01 22:57:01','2026-06-01 22:57:01',0,1),(3,'VETO','一票否决','触发条件则直接归零','veto','ctx.hasVetoItem === true','0','{\"vetoItems\": {\"type\": \"array\", \"label\": \"否决项\"}}',NULL,1,0,1,1,'2026-06-01 22:57:01','2026-06-01 22:57:01',0,1),(4,'PROGRESSIVE_PENALTY','累进惩罚','重复违规加重处罚','progressive','ctx.occurrenceCount > 1','ctx.baseDeduction * Math.pow(ctx.multiplier, ctx.occurrenceCount - 1)','{\"multiplier\": {\"type\": \"number\", \"label\": \"递增倍率\", \"default\": 1.5}}',NULL,30,0,1,1,'2026-06-01 22:57:01','2026-06-01 22:57:01',0,1),(5,'CONSECUTIVE_BONUS','连续奖励','连续达标给予奖励','bonus','ctx.consecutiveCount >= ctx.threshold','ctx.score + ctx.bonusPoints','{\"threshold\": {\"type\": \"number\", \"label\": \"连续次数\", \"default\": 3}, \"bonusPoints\": {\"type\": \"number\", \"label\": \"奖励分数\", \"default\": 5}}',NULL,40,0,1,1,'2026-06-01 22:57:01','2026-06-01 22:57:01',0,1),(6,'TIME_PENALTY','超时惩罚','超时则扣分','penalty','ctx.isOverdue === true','ctx.score - ctx.penaltyPoints','{\"penaltyPoints\": {\"type\": \"number\", \"label\": \"超时扣分\", \"default\": 10}}',NULL,50,0,1,1,'2026-06-01 22:57:01','2026-06-01 22:57:01',0,1);
/*!40000 ALTER TABLE `calculation_rules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `casbin_rule`
--

DROP TABLE IF EXISTS `casbin_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `casbin_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ptype` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `v0` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `v1` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `v2` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `v3` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `v4` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `v5` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Casbin 策略规则表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `casbin_rule`
--

LOCK TABLES `casbin_rule` WRITE;
/*!40000 ALTER TABLE `casbin_rule` DISABLE KEYS */;
INSERT INTO `casbin_rule` VALUES (1,'p','role:1','insp:audit','view','','',''),(2,'p','role:2','insp:audit','view','','',''),(3,'p','role:1','insp:audit','view','','',''),(4,'p','role:2','insp:audit','view','','','');
/*!40000 ALTER TABLE `casbin_rule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `channel_deliveries`
--

DROP TABLE IF EXISTS `channel_deliveries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `channel_deliveries` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `notification_id` bigint NOT NULL COMMENT 'FK -> msg_notifications.id',
  `channel` varchar(20) NOT NULL COMMENT 'IN_APP/SMS/WX_MP/EMAIL/APP_PUSH',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SENDING/SENT/DELIVERED/FAILED/RETRYING/DEAD',
  `provider_msg_id` varchar(100) DEFAULT NULL,
  `retry_count` int NOT NULL DEFAULT '0',
  `next_retry_at` datetime DEFAULT NULL,
  `sent_at` datetime DEFAULT NULL,
  `delivered_at` datetime DEFAULT NULL COMMENT '第三方回执',
  `read_at` datetime DEFAULT NULL,
  `clicked_at` datetime DEFAULT NULL,
  `last_error` varchar(500) DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_notification` (`notification_id`),
  KEY `idx_status_retry` (`status`,`next_retry_at`),
  KEY `idx_channel` (`channel`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通道投递记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `channel_deliveries`
--

LOCK TABLES `channel_deliveries` WRITE;
/*!40000 ALTER TABLE `channel_deliveries` DISABLE KEYS */;
/*!40000 ALTER TABLE `channel_deliveries` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `check_plan_period_config`
--

DROP TABLE IF EXISTS `check_plan_period_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `check_plan_period_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `check_plan_id` bigint NOT NULL COMMENT '检查计划ID',
  `week_start_day` tinyint DEFAULT '1' COMMENT '周起始日：1-7（1=周一，7=周日）',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_check_plan` (`check_plan_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查计划周期配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `check_plan_period_config`
--

LOCK TABLES `check_plan_period_config` WRITE;
/*!40000 ALTER TABLE `check_plan_period_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `check_plan_period_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `class_course_assignments`
--

DROP TABLE IF EXISTS `class_course_assignments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `class_course_assignments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `semester_id` bigint NOT NULL COMMENT '学期ID',
  `org_unit_id` bigint DEFAULT NULL,
  `offering_id` bigint NOT NULL COMMENT '学期开课计划ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `weekly_hours` int NOT NULL COMMENT '周课时',
  `student_count` int DEFAULT NULL COMMENT '选课人数',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0待确认 1已确认',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_class_course` (`semester_id`,`org_unit_id`,`course_id`),
  KEY `idx_semester_class` (`semester_id`,`org_unit_id`),
  KEY `idx_tenant` (`tenant_id`),
  KEY `fk_assignment_offering` (`offering_id`),
  CONSTRAINT `fk_assignment_offering` FOREIGN KEY (`offering_id`) REFERENCES `semester_course_offerings` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级开课表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `class_course_assignments`
--

LOCK TABLES `class_course_assignments` WRITE;
/*!40000 ALTER TABLE `class_course_assignments` DISABLE KEYS */;
/*!40000 ALTER TABLE `class_course_assignments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `class_dormitory_bindings`
--

DROP TABLE IF EXISTS `class_dormitory_bindings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `class_dormitory_bindings` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `org_unit_id` bigint DEFAULT NULL,
  `dormitory_id` bigint NOT NULL COMMENT '宿舍ID',
  `student_count` int DEFAULT '0' COMMENT '学生人数',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_class_dormitory` (`org_unit_id`,`dormitory_id`),
  KEY `idx_dormitory_id` (`dormitory_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级宿舍绑定表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `class_dormitory_bindings`
--

LOCK TABLES `class_dormitory_bindings` WRITE;
/*!40000 ALTER TABLE `class_dormitory_bindings` DISABLE KEYS */;
/*!40000 ALTER TABLE `class_dormitory_bindings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `class_size_snapshots`
--

DROP TABLE IF EXISTS `class_size_snapshots`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `class_size_snapshots` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `org_unit_id` bigint DEFAULT NULL,
  `snapshot_date` date NOT NULL COMMENT 'Snapshot Date',
  `student_count` int NOT NULL COMMENT 'Total Student Count',
  `active_count` int DEFAULT NULL COMMENT 'Active Students (excluding leave/suspension)',
  `male_count` int DEFAULT NULL COMMENT 'Male Count',
  `female_count` int DEFAULT NULL COMMENT 'Female Count',
  `snapshot_source` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'AUTO' COMMENT 'Source (AUTO/MANUAL/PUBLISH)',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_class_date` (`org_unit_id`,`snapshot_date`),
  KEY `idx_snapshot_date` (`snapshot_date`),
  KEY `idx_class_id` (`org_unit_id`),
  KEY `idx_class_size_date_range` (`snapshot_date`,`org_unit_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Class Size Snapshots';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `class_size_snapshots`
--

LOCK TABLES `class_size_snapshots` WRITE;
/*!40000 ALTER TABLE `class_size_snapshots` DISABLE KEYS */;
/*!40000 ALTER TABLE `class_size_snapshots` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `class_time_configs`
--

DROP TABLE IF EXISTS `class_time_configs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `class_time_configs` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `config_name` varchar(50) NOT NULL COMMENT '配置名称，如"标准作息"',
  `semester_id` bigint DEFAULT NULL COMMENT '适用学期（空表示通用）',
  `time_slot` int NOT NULL COMMENT '节次（第几节课）',
  `slot_name` varchar(20) DEFAULT NULL COMMENT '节次名称，如"第一节"',
  `start_time` time NOT NULL COMMENT '上课时间',
  `end_time` time NOT NULL COMMENT '下课时间',
  `slot_type` tinyint DEFAULT '1' COMMENT '类型：1上午 2下午 3晚上',
  `is_default` tinyint DEFAULT '0' COMMENT '是否默认配置',
  `status` tinyint DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_slot` (`config_name`,`time_slot`),
  KEY `idx_semester` (`semester_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作息时间配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `class_time_configs`
--

LOCK TABLES `class_time_configs` WRITE;
/*!40000 ALTER TABLE `class_time_configs` DISABLE KEYS */;
INSERT INTO `class_time_configs` VALUES (1,'标准作息',NULL,1,'第一节','08:00:00','08:45:00',1,1,1,'2026-06-01 22:57:18','2026-06-01 22:57:18'),(2,'标准作息',NULL,2,'第二节','08:55:00','09:40:00',1,1,1,'2026-06-01 22:57:18','2026-06-01 22:57:18'),(3,'标准作息',NULL,3,'第三节','10:00:00','10:45:00',1,1,1,'2026-06-01 22:57:18','2026-06-01 22:57:18'),(4,'标准作息',NULL,4,'第四节','10:55:00','11:40:00',1,1,1,'2026-06-01 22:57:18','2026-06-01 22:57:18'),(5,'标准作息',NULL,5,'第五节','14:00:00','14:45:00',2,1,1,'2026-06-01 22:57:18','2026-06-01 22:57:18'),(6,'标准作息',NULL,6,'第六节','14:55:00','15:40:00',2,1,1,'2026-06-01 22:57:18','2026-06-01 22:57:18'),(7,'标准作息',NULL,7,'第七节','16:00:00','16:45:00',2,1,1,'2026-06-01 22:57:18','2026-06-01 22:57:18'),(8,'标准作息',NULL,8,'第八节','16:55:00','17:40:00',2,1,1,'2026-06-01 22:57:18','2026-06-01 22:57:18'),(9,'标准作息',NULL,9,'第九节','19:00:00','19:45:00',3,1,1,'2026-06-01 22:57:18','2026-06-01 22:57:18'),(10,'标准作息',NULL,10,'第十节','19:55:00','20:40:00',3,1,1,'2026-06-01 22:57:18','2026-06-01 22:57:18');
/*!40000 ALTER TABLE `class_time_configs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary view structure for view `classes`
--

DROP TABLE IF EXISTS `classes`;
/*!50001 DROP VIEW IF EXISTS `classes`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `classes` AS SELECT 
 1 AS `id`,
 1 AS `class_name`,
 1 AS `class_code`,
 1 AS `grade_level`,
 1 AS `org_unit_id`,
 1 AS `grade_id`,
 1 AS `major_id`,
 1 AS `major_direction_id`,
 1 AS `class_sequence`,
 1 AS `teacher_id`,
 1 AS `assistant_teacher_id`,
 1 AS `student_count`,
 1 AS `classroom_location`,
 1 AS `enrollment_year`,
 1 AS `education_system`,
 1 AS `skill_level`,
 1 AS `duration`,
 1 AS `graduation_year`,
 1 AS `class_type`,
 1 AS `status`,
 1 AS `is_international`,
 1 AS `is_experimental`,
 1 AS `is_oriented`,
 1 AS `created_at`,
 1 AS `updated_at`,
 1 AS `created_by`,
 1 AS `updated_by`,
 1 AS `deleted`,
 1 AS `tenant_id`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `client_error_logs`
--

DROP TABLE IF EXISTS `client_error_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `client_error_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `user_id` bigint DEFAULT NULL COMMENT '触发错误的用户 ID, 未登录为 NULL',
  `level` varchar(16) NOT NULL DEFAULT 'ERROR' COMMENT 'ERROR | WARN | INFO',
  `source` varchar(32) NOT NULL DEFAULT 'JS' COMMENT 'JS | VUE | HTTP | UNHANDLED',
  `message` varchar(1024) NOT NULL,
  `stack` text,
  `url` varchar(512) DEFAULT NULL COMMENT '触发错误的页面 URL',
  `route_path` varchar(256) DEFAULT NULL COMMENT 'Vue Router 路径',
  `user_agent` varchar(512) DEFAULT NULL,
  `fingerprint` varchar(64) DEFAULT NULL COMMENT '错误指纹 (message hash) 用于去重统计',
  `occurrence_count` int NOT NULL DEFAULT '1' COMMENT '同指纹合并次数',
  `first_occurred_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_occurred_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `resolved` tinyint(1) NOT NULL DEFAULT '0',
  `resolved_by` bigint DEFAULT NULL,
  `resolved_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_cel_user_time` (`user_id`,`last_occurred_at`),
  KEY `idx_cel_fingerprint` (`fingerprint`,`resolved`),
  KEY `idx_cel_level_time` (`level`,`last_occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户端错误日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `client_error_logs`
--

LOCK TABLES `client_error_logs` WRITE;
/*!40000 ALTER TABLE `client_error_logs` DISABLE KEYS */;
/*!40000 ALTER TABLE `client_error_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cohort_semester_mapping`
--

DROP TABLE IF EXISTS `cohort_semester_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cohort_semester_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cohort_id` bigint NOT NULL COMMENT '年级ID (grades表)',
  `semester_id` bigint NOT NULL COMMENT '学期ID (semesters表)',
  `program_semester` tinyint NOT NULL COMMENT '培养方案第几学期 (1-8)',
  `plan_id` bigint DEFAULT NULL COMMENT '关联培养方案',
  `status` tinyint DEFAULT '1' COMMENT '1=正常 0=跳过(如休学学期)',
  `remark` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cohort_semester` (`cohort_id`,`semester_id`),
  KEY `idx_semester` (`semester_id`),
  KEY `idx_plan` (`plan_id`),
  CONSTRAINT `fk_csm_plan` FOREIGN KEY (`plan_id`) REFERENCES `curriculum_plans` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_csm_semester` FOREIGN KEY (`semester_id`) REFERENCES `semesters` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='年级-学期映射表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cohort_semester_mapping`
--

LOCK TABLES `cohort_semester_mapping` WRITE;
/*!40000 ALTER TABLE `cohort_semester_mapping` DISABLE KEYS */;
/*!40000 ALTER TABLE `cohort_semester_mapping` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `course_evaluations`
--

DROP TABLE IF EXISTS `course_evaluations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_evaluations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `evaluation_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `evaluation_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '活动名称',
  `semester_id` bigint NOT NULL,
  `org_unit_id` bigint DEFAULT NULL COMMENT '归属(空=全校)',
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0草稿 1进行中 2已结束',
  `anonymous` tinyint NOT NULL DEFAULT '1' COMMENT '是否匿名',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_eval_code` (`evaluation_code`),
  KEY `idx_semester` (`semester_id`),
  KEY `idx_org_unit` (`org_unit_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评教活动';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `course_evaluations`
--

LOCK TABLES `course_evaluations` WRITE;
/*!40000 ALTER TABLE `course_evaluations` DISABLE KEYS */;
/*!40000 ALTER TABLE `course_evaluations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `course_schedules`
--

DROP TABLE IF EXISTS `course_schedules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_schedules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `semester_id` bigint NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '方案名称',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '方案说明',
  `status` tinyint DEFAULT '0' COMMENT '0=草稿 1=已发布 2=已归档',
  `entry_count` int DEFAULT '0' COMMENT '条目数',
  `generated_at` datetime DEFAULT NULL COMMENT '自动排课时间',
  `published_at` datetime DEFAULT NULL COMMENT '发布时间',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_semester` (`semester_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排课方案表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `course_schedules`
--

LOCK TABLES `course_schedules` WRITE;
/*!40000 ALTER TABLE `course_schedules` DISABLE KEYS */;
/*!40000 ALTER TABLE `course_schedules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `courses`
--

DROP TABLE IF EXISTS `courses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `courses` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `course_code` varchar(50) NOT NULL COMMENT '课程代码',
  `course_name` varchar(100) NOT NULL COMMENT '课程名称',
  `course_name_en` varchar(200) DEFAULT NULL COMMENT '课程英文名',
  `course_category` tinyint DEFAULT '1' COMMENT '课程类别：1公共基础课 2专业基础课 3专业核心课 4专业选修课 5通识选修课 6实践课',
  `course_type` tinyint DEFAULT '1' COMMENT '课程性质：1必修 2限选 3任选',
  `course_nature` tinyint DEFAULT '1' COMMENT '课程属性：1理论课 2实验课 3理论+实验 4实践课',
  `credits` decimal(4,1) DEFAULT '0.0' COMMENT '学分',
  `total_hours` int DEFAULT '0' COMMENT '总学时',
  `theory_hours` int DEFAULT '0' COMMENT '理论学时',
  `practice_hours` int DEFAULT '0' COMMENT '实践/实验学时',
  `weekly_hours` int DEFAULT '2' COMMENT '周学时',
  `assessment_method` tinyint DEFAULT '1' COMMENT '考核方式(1=考试,2=考查,3=技能考试,4=考试+考查)',
  `grade_scale_type` tinyint DEFAULT '1' COMMENT '评分制：1百分制 2五级制 3二级制（通过/不通过）',
  `org_unit_id` bigint DEFAULT NULL COMMENT '开课部门ID',
  `prerequisite_ids` json DEFAULT NULL COMMENT '先修课程ID数组',
  `description` text COMMENT '课程简介',
  `syllabus_url` varchar(500) DEFAULT NULL COMMENT '教学大纲URL',
  `status` tinyint DEFAULT '1' COMMENT '状态：1启用 0停用',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `exam_type` tinyint DEFAULT '1' COMMENT '考核方式',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_code` (`course_code`),
  KEY `idx_category` (`course_category`),
  KEY `idx_org_unit` (`org_unit_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `courses`
--

LOCK TABLES `courses` WRITE;
/*!40000 ALTER TABLE `courses` DISABLE KEYS */;
/*!40000 ALTER TABLE `courses` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `curriculum_plan_courses`
--

DROP TABLE IF EXISTS `curriculum_plan_courses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `curriculum_plan_courses` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `plan_id` bigint NOT NULL COMMENT '培养方案ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `semester_number` tinyint NOT NULL COMMENT '开课学期（第几学期，1-8）',
  `course_category` tinyint DEFAULT NULL COMMENT '在本方案中的课程类别（覆盖课程表默认值）',
  `course_type` tinyint DEFAULT NULL COMMENT '在本方案中的课程性质（必修/选修）',
  `credits` decimal(4,1) DEFAULT NULL COMMENT '学分（覆盖课程表默认值）',
  `total_hours` int DEFAULT NULL COMMENT '总学时',
  `weekly_hours` int DEFAULT NULL COMMENT '周学时',
  `theory_hours` int DEFAULT NULL COMMENT '理论学时',
  `practice_hours` int DEFAULT NULL COMMENT '实践学时',
  `assessment_method` tinyint DEFAULT '1' COMMENT '考核方式(1=考试,2=考查,3=技能考试,4=考试+考查)',
  `is_degree_course` tinyint DEFAULT '0' COMMENT '是否学位课程',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_course` (`plan_id`,`course_id`),
  KEY `idx_plan_semester` (`plan_id`,`semester_number`),
  KEY `fk_plancourse_course` (`course_id`),
  KEY `idx_tenant` (`tenant_id`),
  CONSTRAINT `fk_plancourse_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`),
  CONSTRAINT `fk_plancourse_plan` FOREIGN KEY (`plan_id`) REFERENCES `curriculum_plans` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='培养方案课程表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `curriculum_plan_courses`
--

LOCK TABLES `curriculum_plan_courses` WRITE;
/*!40000 ALTER TABLE `curriculum_plan_courses` DISABLE KEYS */;
/*!40000 ALTER TABLE `curriculum_plan_courses` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `curriculum_plans`
--

DROP TABLE IF EXISTS `curriculum_plans`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `curriculum_plans` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `plan_code` varchar(50) NOT NULL COMMENT '方案代码',
  `plan_name` varchar(100) NOT NULL COMMENT '方案名称，如"2024级计算机应用技术专业培养方案"',
  `major_id` bigint NOT NULL COMMENT '专业ID',
  `major_direction_id` bigint DEFAULT NULL COMMENT '专业方向ID（可选，更精确的绑定）',
  `grade_year` int NOT NULL COMMENT '适用年级（入学年份）',
  `education_level` tinyint DEFAULT '1' COMMENT '培养层次：1中专 2大专 3本科',
  `education_length` tinyint DEFAULT '3' COMMENT '学制（年）',
  `total_credits` decimal(5,1) DEFAULT NULL COMMENT '毕业总学分要求',
  `required_credits` decimal(5,1) DEFAULT NULL COMMENT '必修学分要求',
  `elective_credits` decimal(5,1) DEFAULT NULL COMMENT '选修学分要求',
  `practice_credits` decimal(5,1) DEFAULT NULL COMMENT '实践学分要求',
  `degree_type` varchar(50) DEFAULT NULL COMMENT '授予学位类型',
  `training_objective` text COMMENT '培养目标',
  `graduation_requirement` text COMMENT '毕业要求',
  `version` int DEFAULT '1' COMMENT '版本号',
  `status` tinyint DEFAULT '1' COMMENT '状态：0草稿 1已发布 2已归档',
  `published_at` datetime DEFAULT NULL COMMENT '发布时间',
  `published_by` bigint DEFAULT NULL COMMENT '发布人',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_code` (`plan_code`),
  KEY `idx_major_year` (`major_id`,`grade_year`),
  KEY `idx_major_direction` (`major_direction_id`),
  KEY `idx_tenant` (`tenant_id`),
  CONSTRAINT `fk_plan_major` FOREIGN KEY (`major_id`) REFERENCES `majors` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='培养方案表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `curriculum_plans`
--

LOCK TABLES `curriculum_plans` WRITE;
/*!40000 ALTER TABLE `curriculum_plans` DISABLE KEYS */;
/*!40000 ALTER TABLE `curriculum_plans` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `daily_check_categories`
--

DROP TABLE IF EXISTS `daily_check_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `daily_check_categories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `check_id` bigint NOT NULL COMMENT '日常检查ID',
  `category_id` bigint NOT NULL COMMENT '类别ID(关联deduction_types)',
  `category_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类别名称',
  `participated_rounds` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '1' COMMENT '参与的轮次(逗号分隔)',
  `check_rounds` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '1' COMMENT '检查轮次配置',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `deleted` tinyint DEFAULT '0' COMMENT '是否删除',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_check_id` (`check_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日常检查类别关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `daily_check_categories`
--

LOCK TABLES `daily_check_categories` WRITE;
/*!40000 ALTER TABLE `daily_check_categories` DISABLE KEYS */;
/*!40000 ALTER TABLE `daily_check_categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `daily_check_targets`
--

DROP TABLE IF EXISTS `daily_check_targets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `daily_check_targets` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `check_id` bigint NOT NULL COMMENT '日常检查ID',
  `target_type` int DEFAULT '1' COMMENT '目标类型: 1-班级, 2-宿舍',
  `target_id` bigint NOT NULL COMMENT '目标ID(班级ID或宿舍ID)',
  `target_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '目标名称',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `deleted` tinyint DEFAULT '0' COMMENT '是否删除',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_check_id` (`check_id`),
  KEY `idx_target_id` (`target_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日常检查目标关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `daily_check_targets`
--

LOCK TABLES `daily_check_targets` WRITE;
/*!40000 ALTER TABLE `daily_check_targets` DISABLE KEYS */;
/*!40000 ALTER TABLE `daily_check_targets` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `daily_checks`
--

DROP TABLE IF EXISTS `daily_checks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `daily_checks` (
  `id` bigint NOT NULL,
  `plan_id` bigint DEFAULT NULL COMMENT '检查计划ID',
  `check_date` date NOT NULL COMMENT '检查日期',
  `check_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '检查名称',
  `template_id` bigint DEFAULT NULL COMMENT '模板ID',
  `check_type` int DEFAULT '1' COMMENT '检查类型: 1-日常, 2-周检, 3-月检',
  `total_rounds` int DEFAULT '1' COMMENT '总轮次',
  `round_names` json DEFAULT NULL COMMENT '轮次名称配置',
  `status` int DEFAULT '0' COMMENT '状态: 0-未开始, 1-进行中, 2-已完成',
  `record_generated` tinyint DEFAULT '0' COMMENT '是否已生成记录',
  `record_id` bigint DEFAULT NULL COMMENT '关联的检查记录ID',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '检查描述',
  `excluded_targets` json DEFAULT NULL COMMENT '排除的目标',
  `checker_id` bigint DEFAULT NULL COMMENT '检查员ID',
  `checker_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '检查员姓名',
  `weight_config_id` bigint DEFAULT NULL COMMENT '加权配置ID',
  `enable_weight` tinyint DEFAULT '0' COMMENT '是否启用加权',
  `deleted` tinyint DEFAULT '0' COMMENT '是否删除',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_plan_id` (`plan_id`),
  KEY `idx_check_date` (`check_date`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日常检查表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `daily_checks`
--

LOCK TABLES `daily_checks` WRITE;
/*!40000 ALTER TABLE `daily_checks` DISABLE KEYS */;
/*!40000 ALTER TABLE `daily_checks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `daily_class_summary`
--

DROP TABLE IF EXISTS `daily_class_summary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `daily_class_summary` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `check_plan_id` bigint NOT NULL COMMENT '检查计划ID',
  `org_unit_id` bigint DEFAULT NULL,
  `check_date` date NOT NULL COMMENT '检查日期',
  `total_score` decimal(10,2) DEFAULT '0.00' COMMENT '当天总扣分',
  `weighted_total_score` decimal(10,2) DEFAULT '0.00' COMMENT '当天加权总扣分',
  `category_scores` json DEFAULT NULL COMMENT '各类别扣分',
  `item_scores` json DEFAULT NULL COMMENT '各扣分项扣分',
  `check_count` int DEFAULT '0' COMMENT '当天检查次数',
  `participated` tinyint DEFAULT '1' COMMENT '是否参与检查：0否 1是',
  `is_finalized` tinyint DEFAULT '0' COMMENT '是否已确认（申诉期结束）：0否 1是',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_class_date` (`check_plan_id`,`org_unit_id`,`check_date`),
  KEY `idx_check_date` (`check_date`),
  KEY `idx_finalized` (`is_finalized`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日班级汇总表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `daily_class_summary`
--

LOCK TABLES `daily_class_summary` WRITE;
/*!40000 ALTER TABLE `daily_class_summary` DISABLE KEYS */;
/*!40000 ALTER TABLE `daily_class_summary` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `data_modules`
--

-- data_modules: v3 单轨 data_resources 后的死表, 2026-06-12 起不再创建 (V20260612_1 已 DROP)

--
-- Table structure for table `data_resources`
--

DROP TABLE IF EXISTS `data_resources`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `data_resources` (
  `resource_code` varchar(50) NOT NULL,
  `resource_name` varchar(50) NOT NULL,
  `domain_code` varchar(50) NOT NULL DEFAULT 'CORE',
  `industry` varchar(20) DEFAULT NULL COMMENT '所属行业 CORE/EDU/HEALTH/CARE/CUSTOM',
  `domain_name` varchar(50) DEFAULT NULL,
  `type_field` varchar(50) DEFAULT NULL COMMENT '业务表里指向类型码的字段(类型过滤用)',
  `type_entity` varchar(20) DEFAULT NULL COMMENT '类型选项取自 entity_type_configs 的 entity_type(USER/PLACE/ORG_UNIT)',
  `subject_relation_filterable` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否支持②结果关系过滤(成员型资源, 如 user)',
  `registered_by` varchar(100) NOT NULL DEFAULT 'CORE',
  `sort_order` int DEFAULT '0',
  `enabled` tinyint DEFAULT '1',
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `plugin_enabled` tinyint NOT NULL DEFAULT '1',
  `allowed_scopes` json DEFAULT NULL COMMENT '本模块支持的 scope 代码数组',
  `access_resource_type` varchar(255) DEFAULT NULL,
  `resource_kind` varchar(10) NOT NULL DEFAULT 'PLAIN' COMMENT '资源种类 SUBJECT/PLAIN (统一锚定模型第一根轴: 记录本身是否主体)',
  PRIMARY KEY (`resource_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据资源注册';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `data_resources`
--

LOCK TABLES `data_resources` WRITE;
/*!40000 ALTER TABLE `data_resources` DISABLE KEYS */;
INSERT INTO `data_resources` (`resource_code`,`resource_name`,`domain_code`,`industry`,`domain_name`,`registered_by`,`sort_order`,`enabled`,`tenant_id`,`created_at`,`plugin_enabled`,`allowed_scopes`,`access_resource_type`) VALUES ('attendance','考勤','education','EDU','教育','EducationPlugin',22,1,1,'2026-06-01 22:57:30',1,'[\"ALL\", \"BY_GRADE\", \"BY_CLASS\", \"BY_MAJOR\", \"SELF\", \"CUSTOM\"]',NULL),('dashboard','仪表盘','CORE','CORE','核心','CORE',40,1,1,'2026-06-01 22:57:32',1,'[\"SELF\"]',NULL),('dormitory_building','宿舍楼','education','EDU','教育','EducationPlugin',50,1,1,'2026-06-01 22:57:32',1,'[\"ALL\", \"DEPARTMENT_AND_BELOW\", \"DEPARTMENT\", \"SELF\", \"CUSTOM\"]',NULL),('dormitory_checkin','住宿登记','education','EDU','教育','EducationPlugin',51,1,1,'2026-06-01 22:57:32',1,'[\"ALL\", \"DEPARTMENT_AND_BELOW\", \"DEPARTMENT\", \"SELF\", \"CUSTOM\"]',NULL),('dormitory_room','宿舍房间','education','EDU','教育','EducationPlugin',52,1,1,'2026-06-01 22:57:32',1,'[\"ALL\", \"DEPARTMENT_AND_BELOW\", \"DEPARTMENT\", \"SELF\", \"CUSTOM\"]',NULL),('exam','考试','education','EDU','教育','EducationPlugin',21,1,1,'2026-06-01 22:57:30',1,'[\"ALL\", \"BY_CLASS\", \"SELF\", \"CUSTOM\"]',NULL),('exam_batch','考试批次','education','EDU','教育','EducationPlugin',25,1,1,'2026-06-01 22:57:32',1,'[\"ALL\", \"BY_CLASS\", \"SELF\", \"CUSTOM\"]',NULL),('grade_batch','成绩批次','education','EDU','教育','EducationPlugin',20,1,1,'2026-06-01 22:57:30',1,'[\"ALL\", \"BY_CLASS\", \"SELF\", \"CUSTOM\"]',NULL),('inspection','检查记录','inspection','CORE','检查平台','InspectionPlugin',30,1,1,'2026-06-01 22:57:30',1,'[\"ALL\", \"DEPARTMENT_AND_BELOW\", \"DEPARTMENT\", \"SELF\", \"CUSTOM\"]',NULL),('inspection_alert','预警记录','inspection','CORE','检查平台','InspectionPlugin',39,1,1,'2026-06-01 22:57:39',1,'[\"ALL\", \"DEPARTMENT_AND_BELOW\", \"DEPARTMENT\", \"SELF\", \"CUSTOM\"]',NULL),('inspection_appeal','检查申诉','inspection','CORE','检查平台','InspectionPlugin',31,1,1,'2026-06-01 22:57:32',1,'[\"SELF\"]',NULL),('inspection_audit','检查审计日志','inspection','CORE','检查平台','CORE',0,1,1,'2026-06-01 22:57:37',1,'[\"ALL\", \"DEPARTMENT_AND_BELOW\", \"DEPARTMENT\", \"SELF\", \"CUSTOM\"]',NULL),('inspection_corrective','整改任务','inspection','CORE','检查平台','InspectionPlugin',32,1,1,'2026-06-01 22:57:32',1,'[\"ALL\", \"DEPARTMENT_AND_BELOW\", \"DEPARTMENT\", \"SELF\", \"CUSTOM\"]',NULL),('inspection_observation','评分观察','inspection','CORE','检查平台','InspectionPlugin',41,1,1,'2026-06-01 22:57:39',1,'[\"ALL\", \"DEPARTMENT_AND_BELOW\", \"DEPARTMENT\", \"SELF\", \"CUSTOM\"]',NULL),('inspection_personal','个人评级','inspection','CORE','检查平台','InspectionPlugin',33,1,1,'2026-06-01 22:57:32',1,'[\"SELF\"]',NULL),('inspection_project','检查项目','inspection','CORE','检查平台','InspectionPlugin',34,1,1,'2026-06-01 22:57:32',1,'[\"ALL\", \"DEPARTMENT_AND_BELOW\", \"DEPARTMENT\", \"SELF\", \"CUSTOM\"]',NULL),('inspection_record','检查记录','inspection','CORE','检查平台','InspectionPlugin',35,1,1,'2026-06-01 22:57:32',1,'[\"ALL\", \"DEPARTMENT_AND_BELOW\", \"DEPARTMENT\", \"SELF\", \"CUSTOM\"]',NULL),('inspection_summary','检查汇总','inspection','CORE','检查平台','InspectionPlugin',36,1,1,'2026-06-01 22:57:32',1,'[\"ALL\", \"DEPARTMENT_AND_BELOW\", \"DEPARTMENT\", \"CUSTOM\"]',NULL),('inspection_task','检查任务','inspection','CORE','检查平台','InspectionPlugin',37,1,1,'2026-06-01 22:57:32',1,'[\"ALL\", \"DEPARTMENT_AND_BELOW\", \"DEPARTMENT\", \"SELF\", \"CUSTOM\"]',NULL),('inspection_template','检查模板','inspection','CORE','检查平台','InspectionPlugin',38,1,1,'2026-06-01 22:57:32',1,'[\"ALL\", \"SELF\"]',NULL),('inspection_violation','违规记录','inspection','CORE','检查平台','InspectionPlugin',40,1,1,'2026-06-01 22:57:39',1,'[\"ALL\", \"DEPARTMENT_AND_BELOW\", \"DEPARTMENT\", \"SELF\", \"CUSTOM\"]',NULL),('notification','消息','CORE','CORE','核心','CORE',5,1,1,'2026-06-01 22:57:30',1,'[\"SELF\"]',NULL),('org_unit','组织','CORE','CORE','核心','CORE',2,1,1,'2026-06-01 22:57:30',1,'[\"ALL\", \"DEPARTMENT_AND_BELOW\", \"DEPARTMENT\", \"SELF\", \"CUSTOM\"]',NULL),('place','场所','CORE','CORE','核心','CORE',3,1,1,'2026-06-01 22:57:30',1,'[\"ALL\", \"DEPARTMENT_AND_BELOW\", \"DEPARTMENT\", \"SELF\", \"CUSTOM\"]',NULL),('role','角色','CORE','CORE','核心','CORE',4,1,1,'2026-06-01 22:57:30',1,'[\"ALL\", \"DEPARTMENT_AND_BELOW\", \"DEPARTMENT\", \"SELF\", \"CUSTOM\"]',NULL),('school_class','班级','education','EDU','教育','EducationPlugin',11,1,1,'2026-06-01 22:57:32',1,'[\"ALL\", \"BY_CLASS\", \"BY_MAJOR\", \"SELF\", \"CUSTOM\"]',NULL),('student','学生','education','EDU','教育','EducationPlugin',10,1,1,'2026-06-01 22:57:30',1,'[\"ALL\", \"BY_GRADE\", \"BY_CLASS\", \"BY_MAJOR\", \"SELF\", \"CUSTOM\"]',NULL),('student_grade','学生成绩','education','EDU','教育','EducationPlugin',24,1,1,'2026-06-01 22:57:32',1,'[\"ALL\", \"BY_CLASS\", \"SELF\", \"CUSTOM\"]',NULL),('system_role','系统角色','CORE','CORE','核心','CORE',45,1,1,'2026-06-01 22:57:32',1,'[\"ALL\", \"DEPARTMENT_AND_BELOW\", \"DEPARTMENT\", \"SELF\", \"CUSTOM\"]',NULL),('system_user','系统用户','CORE','CORE','核心','CORE',46,1,1,'2026-06-01 22:57:32',1,'[\"ALL\", \"DEPARTMENT_AND_BELOW\", \"DEPARTMENT\", \"SELF\", \"CUSTOM\"]',NULL),('teaching_task','教学任务','education','EDU','教育','EducationPlugin',23,1,1,'2026-06-01 22:57:32',1,'[\"ALL\", \"BY_CLASS\", \"BY_MAJOR\", \"SELF\", \"CUSTOM\"]',NULL),('user','用户','CORE','CORE','核心','CORE',1,1,1,'2026-06-01 22:57:30',1,'[\"ALL\", \"DEPARTMENT_AND_BELOW\", \"DEPARTMENT\", \"SELF\", \"CUSTOM\"]',NULL);
-- R2.2 前置②: entity_events 独立资源码 (表无 org_unit_id, 锚点照搬 created_by 现状, 字节等价)
INSERT INTO `data_resources` (`resource_code`,`resource_name`,`domain_code`,`industry`,`domain_name`,`registered_by`,`sort_order`,`enabled`,`tenant_id`,`created_at`,`plugin_enabled`,`allowed_scopes`) VALUES ('entity_event','实体事件','inspection','CORE','检查平台','CORE',47,1,1,'2026-06-01 22:57:30',1,'[\"ALL\", \"SELF\"]');
-- R2.2 前置①: 激活 8 个此前无 data_resources 行的教务/排课模块 (注解本意是过滤, 缺行=不过滤越权面)
INSERT INTO `data_resources` (`resource_code`,`resource_name`,`domain_code`,`industry`,`domain_name`,`registered_by`,`sort_order`,`enabled`,`tenant_id`,`created_at`,`plugin_enabled`,`allowed_scopes`) VALUES
 ('teaching_progress','教学进度','education','EDU','教育','EducationPlugin',60,1,1,'2026-06-01 22:57:30',1,'[\"ALL\", \"DEPARTMENT_AND_BELOW\", \"DEPARTMENT\", \"SELF\", \"CUSTOM\"]'),
 ('class_course_assignment','排课分配','education','EDU','教育','EducationPlugin',61,1,1,'2026-06-01 22:57:30',1,'[\"ALL\", \"DEPARTMENT_AND_BELOW\", \"DEPARTMENT\", \"SELF\", \"CUSTOM\"]'),
 ('course_evaluation','课程评教','education','EDU','教育','EducationPlugin',62,1,1,'2026-06-01 22:57:30',1,'[\"ALL\", \"DEPARTMENT_AND_BELOW\", \"DEPARTMENT\", \"SELF\", \"CUSTOM\"]'),
 ('evaluation_response','评教作答','education','EDU','教育','EducationPlugin',63,1,1,'2026-06-01 22:57:30',1,'[\"ALL\", \"DEPARTMENT_AND_BELOW\", \"DEPARTMENT\", \"SELF\", \"CUSTOM\"]'),
 ('scheduling_constraint','排课约束','education','EDU','教育','EducationPlugin',64,1,1,'2026-06-01 22:57:30',1,'[\"ALL\", \"DEPARTMENT_AND_BELOW\", \"DEPARTMENT\", \"SELF\", \"CUSTOM\"]'),
 ('schedule_entry','课表条目','education','EDU','教育','EducationPlugin',65,1,1,'2026-06-01 22:57:30',1,'[\"ALL\", \"DEPARTMENT_AND_BELOW\", \"DEPARTMENT\", \"SELF\", \"CUSTOM\"]'),
 ('schedule_conflict_record','排课冲突','education','EDU','教育','EducationPlugin',66,1,1,'2026-06-01 22:57:30',1,'[\"ALL\", \"DEPARTMENT_AND_BELOW\", \"DEPARTMENT\", \"SELF\", \"CUSTOM\"]'),
 ('teacher_preference','教师偏好','education','EDU','教育','EducationPlugin',67,1,1,'2026-06-01 22:57:30',1,'[\"ALL\", \"SELF\"]');
-- 类型过滤(闸2/2b): user 资源支持按 user_type_code 过滤, 选项取自 entity_type_configs(entity_type=USER)
UPDATE `data_resources` SET `type_field`='user_type_code', `type_entity`='USER' WHERE `resource_code` IN ('user','system_user');
-- 结果关系过滤(轴②): 成员型资源 (user/system_user) 支持按 access_relations 关系过滤
UPDATE `data_resources` SET `subject_relation_filterable`=1 WHERE `resource_code` IN ('user','system_user');
-- 统一锚定模型 (R1): 主体型资源 — 记录本身是 user/org_unit/place, 归属走 access_relations 非列 (org_unit_id 不重建)
UPDATE `data_resources` SET `resource_kind`='SUBJECT' WHERE `resource_code` IN ('user','system_user','org_unit','place','student');
-- 完美重构 (2026-06-27): 检查执行域按表拆资源码。data_resources 须 seed (DataResourceUpserter 只 UPDATE 不 INSERT;
-- resource_relations 由 contribution 启动期写, 无需 baseline)。删共享码 inspection_record (P1-P4 已把 5 mapper
-- 迁出 → 无 @DataPermission 引用; 其 casbin 功能权限码 inspection_record:view 是另一命名空间不在此表); 加 4 拆分码
-- (inspection_task 已在上方 dump)。
DELETE FROM `data_resources` WHERE `resource_code`='inspection_record';
INSERT INTO `data_resources` (`resource_code`,`resource_name`,`domain_code`,`domain_name`,`enabled`,`sort_order`,`registered_by`,`allowed_scopes`,`subject_relation_filterable`,`resource_kind`,`tenant_id`,`plugin_enabled`) VALUES
('inspection_submission','检查提交单','inspection','检查平台',1,38,'CORE','["ALL","DEPARTMENT_AND_BELOW","MANAGED_ORGS_AND_BELOW","DEPARTMENT","MANAGED_ORGS","SELF","CUSTOM"]',0,'PLAIN',1,1),
('inspection_evidence','检查证据','inspection','检查平台',1,39,'CORE','["ALL","DEPARTMENT_AND_BELOW","MANAGED_ORGS_AND_BELOW","DEPARTMENT","MANAGED_ORGS","SELF","CUSTOM"]',0,'PLAIN',1,1),
('inspection_submission_detail','检查提交明细','inspection','检查平台',1,40,'CORE','["ALL","DEPARTMENT_AND_BELOW","MANAGED_ORGS_AND_BELOW","DEPARTMENT","MANAGED_ORGS","SELF","CUSTOM"]',0,'PLAIN',1,1),
('inspection_project_inspector','检查项目成员','inspection','检查平台',1,41,'CORE','["ALL","DEPARTMENT_AND_BELOW","MANAGED_ORGS_AND_BELOW","DEPARTMENT","MANAGED_ORGS","SELF","CUSTOM"]',0,'PLAIN',1,1),
-- B-2: org_unit_scores 拆出独立码 (无 created_by → 无 SELF, 仅 owner_org)。
('inspection_org_unit_score','组织检查得分','inspection','检查平台',1,42,'CORE','["ALL","DEPARTMENT_AND_BELOW","MANAGED_ORGS_AND_BELOW","DEPARTMENT","MANAGED_ORGS","CUSTOM"]',0,'PLAIN',1,1);
-- B-1 守护揪出的既有缺行: EDU dormitory/enrollment 已 dr() 声明却无 data_resources 行 (早于本次重构, 会致 422)。
INSERT INTO `data_resources` (`resource_code`,`resource_name`,`domain_code`,`domain_name`,`enabled`,`sort_order`,`registered_by`,`allowed_scopes`,`subject_relation_filterable`,`resource_kind`,`tenant_id`,`plugin_enabled`) VALUES
('dormitory','宿舍管理','education','教育',1,50,'EducationPlugin','["ALL","DEPARTMENT_AND_BELOW","DEPARTMENT","SELF","CUSTOM"]',0,'PLAIN',1,1),
('enrollment','招生管理','education','教育',1,51,'EducationPlugin','["ALL","DEPARTMENT_AND_BELOW","DEPARTMENT","SELF","CUSTOM"]',0,'PLAIN',1,1);
/*!40000 ALTER TABLE `data_resources` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `record_relations` (统一锚定 R4: 记录↔主体一等表)
-- 扁平、无传递; 与 access_relations 主体图分立。引擎尚未消费 (R4 地基, 无 seed)。
--

DROP TABLE IF EXISTS `record_relations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `record_relations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `resource_code` varchar(50) NOT NULL COMMENT '哪个资源, 对应 data_resources.resource_code',
  `record_id` bigint NOT NULL COMMENT '业务记录 id (非主体)',
  `relation_code` varchar(50) NOT NULL COMMENT '关系码: reviewer / inspected / shared_with ...',
  `subject_type` varchar(20) NOT NULL COMMENT '主体类型: USER/ORG_UNIT/PLACE/ASSET',
  `subject_id` bigint NOT NULL COMMENT '主体 id',
  `access_level` varchar(20) NOT NULL DEFAULT 'READ_ONLY',
  `valid_from` datetime DEFAULT CURRENT_TIMESTAMP,
  `valid_to` datetime DEFAULT NULL,
  `metadata` json DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  `tenant_id` bigint NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_record_relation` (`resource_code`,`record_id`,`relation_code`,`subject_type`,`subject_id`,`tenant_id`,`deleted`),
  KEY `idx_by_subject` (`subject_type`,`subject_id`,`relation_code`,`resource_code`,`deleted`),
  KEY `idx_by_record` (`resource_code`,`record_id`,`relation_code`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记录↔主体关系表 (扁平, 无传递; 与 access_relations 主体图分立)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `resource_relations` (统一锚定模型 R1: 资源关系注册表)
-- 行由 PluginPackage.contribute() 的 ResourceRelationContribution 在启动期 UPSERT, 故无 seed。
--

DROP TABLE IF EXISTS `resource_relations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resource_relations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `resource_code` varchar(50) NOT NULL COMMENT 'FK -> data_resources.resource_code',
  `relation_code` varchar(50) NOT NULL COMMENT '关系码: creator/owner_org/inspector/inspected/reviewer',
  `relation_name` varchar(50) NOT NULL COMMENT '人话显示名(UI)',
  `subject_type` varchar(20) NOT NULL COMMENT '指向主体: USER/ORG_UNIT/PLACE/ASSET/ANY',
  `cardinality` varchar(10) NOT NULL COMMENT 'SINGLE/MULTI',
  `storage_kind` varchar(20) NOT NULL COMMENT 'SUBJECT_GRAPH/COLUMN/RECORD_RELATION (MATERIALIZED 叠加不在此声明)',
  `column_name` varchar(50) DEFAULT NULL COMMENT 'COLUMN: 业务表列名',
  `type_column` varchar(50) DEFAULT NULL COMMENT '多态主体的类型列 (配 column_name)',
  `ar_relation` varchar(30) DEFAULT NULL COMMENT 'SUBJECT_GRAPH/RECORD_RELATION: relation 值',
  `resolver_bean` varchar(100) DEFAULT NULL COMMENT 'PROVIDER: RecordRelationResolver 的 Spring bean 名',
  `enforce_insert_scope` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'R8: owner_org 参与 INSERT 授权 (新行 owner_org∈可写组织, 仅 ownership 语义资源)',
  `auto_fill` tinyint(1) NOT NULL DEFAULT '0' COMMENT '写入是否自动填列',
  `grants_by_default` tinyint(1) NOT NULL DEFAULT '0' COMMENT '无显式授予时默认参与可见性',
  `industry` varchar(20) DEFAULT NULL COMMENT '贡献插件 CORE/EDU/...',
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_resource_relation` (`resource_code`,`relation_code`,`tenant_id`),
  KEY `idx_resource` (`resource_code`,`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资源关系注册表 — 统一锚定模型唯一真相';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `data_scope_dims`
--

DROP TABLE IF EXISTS `data_scope_dims`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `data_scope_dims` (
  `dim_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '维度代码, 如 BY_MAJOR/BY_GRADE',
  `dim_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '中文名',
  `description` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '说明',
  `domain_code` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属业务域',
  `resolver_type` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '范围解析器类全限定名',
  `industry` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属行业包',
  `plugin_class` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '声明插件全限定类名',
  `origin` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '统一来源: PLUGIN:<code>@<ver> / TENANT:CUSTOM#<id>',
  `is_enabled` tinyint DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `plugin_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '插件级启用状态',
  PRIMARY KEY (`dim_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据权限维度字典';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `data_scope_dims`
--

LOCK TABLES `data_scope_dims` WRITE;
/*!40000 ALTER TABLE `data_scope_dims` DISABLE KEYS */;
INSERT INTO `data_scope_dims` VALUES ('BY_CLASS','按班级','仅访问用户所在班级的学生/成绩数据','education','com.school.management.infrastructure.extension.plugins.education.scope.ClassDataScopeResolver','EDU','com.school.management.infrastructure.extension.plugins.education.EducationDataScopePlugin','PLUGIN:EDU@1.0.0',1,'2026-06-01 23:34:29',1),('BY_GRADE','按年级','按照用户所属年级的所有学生数据','education','com.school.management.infrastructure.extension.plugins.education.scope.GradeDataScopeResolver','EDU','com.school.management.infrastructure.extension.plugins.education.EducationDataScopePlugin','PLUGIN:EDU@1.0.0',1,'2026-06-01 23:34:29',1),('BY_MAJOR','按专业','按照用户所属专业的所有学生/课程数据','education','com.school.management.infrastructure.extension.plugins.education.scope.MajorDataScopeResolver','EDU','com.school.management.infrastructure.extension.plugins.education.EducationDataScopePlugin','PLUGIN:EDU@1.0.0',1,'2026-06-01 23:34:29',1);
/*!40000 ALTER TABLE `data_scope_dims` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `domain_events`
--

DROP TABLE IF EXISTS `domain_events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `domain_events` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `event_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Event UUID',
  `event_type` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Event Type (e.g., ClassCreatedEvent)',
  `aggregate_type` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Aggregate Type (e.g., SchoolClass)',
  `aggregate_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Aggregate ID',
  `aggregate_version` int NOT NULL COMMENT 'Aggregate Version',
  `payload` json NOT NULL COMMENT 'Event Payload (JSON)',
  `metadata` json DEFAULT NULL COMMENT 'Event Metadata (JSON)',
  `occurred_at` datetime(3) NOT NULL COMMENT 'When Event Occurred',
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'When Record Created',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_id` (`event_id`),
  KEY `idx_aggregate` (`aggregate_type`,`aggregate_id`),
  KEY `idx_occurred` (`occurred_at`),
  KEY `idx_type` (`event_type`),
  KEY `idx_events_aggregate_version` (`aggregate_type`,`aggregate_id`,`aggregate_version`),
  KEY `idx_events_time_range` (`occurred_at`,`aggregate_type`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Domain Events Store';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `domain_events`
--

LOCK TABLES `domain_events` WRITE;
/*!40000 ALTER TABLE `domain_events` DISABLE KEYS */;
/*!40000 ALTER TABLE `domain_events` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dormitories`
--

DROP TABLE IF EXISTS `dormitories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dormitories` (
  `id` bigint NOT NULL COMMENT 'Primary Key',
  `building_id` bigint NOT NULL COMMENT 'Building ID',
  `org_unit_id` bigint DEFAULT NULL COMMENT '组织单元ID',
  `dormitory_no` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Dormitory Number',
  `floor_number` int DEFAULT NULL COMMENT 'Floor Number',
  `room_usage_type` tinyint DEFAULT '1' COMMENT 'Usage Type: 1-Student, 2-Staff, 3-Guest',
  `bed_capacity` int DEFAULT '4' COMMENT 'Bed Capacity',
  `bed_count` int DEFAULT '0' COMMENT 'Actual Bed Count',
  `occupied_beds` int DEFAULT '0' COMMENT 'Occupied Beds',
  `gender_type` tinyint DEFAULT '0' COMMENT 'Gender Type: 0-Mixed, 1-Male, 2-Female',
  `facilities` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Facilities',
  `notes` text COLLATE utf8mb4_unicode_ci COMMENT 'Notes',
  `status` tinyint DEFAULT '1' COMMENT 'Status: 0-Disabled, 1-Available, 2-Full, 3-Maintenance',
  `deleted` int DEFAULT '0' COMMENT 'Logical Delete Flag',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated At',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dormitory_no` (`building_id`,`dormitory_no`),
  KEY `idx_building_id` (`building_id`),
  KEY `idx_department_id` (`org_unit_id`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_dormitories_floor` (`floor_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Dormitories Table';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dormitories`
--

LOCK TABLES `dormitories` WRITE;
/*!40000 ALTER TABLE `dormitories` DISABLE KEYS */;
/*!40000 ALTER TABLE `dormitories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dormitory_member_snapshots`
--

DROP TABLE IF EXISTS `dormitory_member_snapshots`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dormitory_member_snapshots` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `snapshot_date` date NOT NULL COMMENT 'Snapshot Date',
  `record_id` bigint DEFAULT NULL COMMENT 'Related Check Record ID',
  `dormitory_id` bigint NOT NULL COMMENT 'Dormitory ID',
  `building_id` bigint DEFAULT NULL COMMENT 'Building ID',
  `building_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Building Name',
  `dormitory_no` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Dormitory Number',
  `members` json NOT NULL COMMENT 'Members Array [{studentId,studentNo,studentName,bedNo,isDormLeader}]',
  `member_count` int DEFAULT '0' COMMENT 'Member Count',
  `leader_id` bigint DEFAULT NULL COMMENT 'Dorm Leader ID',
  `leader_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Dorm Leader Name',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',
  PRIMARY KEY (`id`),
  KEY `idx_snapshot_date` (`snapshot_date`),
  KEY `idx_dormitory` (`dormitory_id`),
  KEY `idx_record` (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Dormitory Member Snapshots';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dormitory_member_snapshots`
--

LOCK TABLES `dormitory_member_snapshots` WRITE;
/*!40000 ALTER TABLE `dormitory_member_snapshots` DISABLE KEYS */;
/*!40000 ALTER TABLE `dormitory_member_snapshots` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `enrollment_applications`
--

DROP TABLE IF EXISTS `enrollment_applications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `enrollment_applications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `plan_id` bigint NOT NULL COMMENT '招生计划ID',
  `academic_year` int NOT NULL COMMENT '招生年份',
  `applicant_name` varchar(50) NOT NULL COMMENT '姓名',
  `gender` tinyint DEFAULT NULL COMMENT '1男 2女',
  `id_card` varchar(18) DEFAULT NULL COMMENT '身份证号',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `guardian_name` varchar(50) DEFAULT NULL COMMENT '监护人姓名',
  `guardian_phone` varchar(20) DEFAULT NULL COMMENT '监护人电话',
  `graduate_from` varchar(100) DEFAULT NULL COMMENT '毕业学校',
  `major_id` bigint NOT NULL COMMENT '报考专业',
  `major_direction_id` bigint DEFAULT NULL COMMENT '报考方向',
  `application_date` date DEFAULT NULL COMMENT '报名日期',
  `exam_score` decimal(5,1) DEFAULT NULL COMMENT '入学考试成绩(如有)',
  `status` tinyint DEFAULT '0' COMMENT '0待审核 1已录取 2未录取 3已报到 4已放弃',
  `review_comment` varchar(200) DEFAULT NULL COMMENT '审核意见',
  `reviewer_id` bigint DEFAULT NULL COMMENT '审核人',
  `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间',
  `registered_at` datetime DEFAULT NULL COMMENT '报到时间',
  `assigned_class_id` bigint DEFAULT NULL COMMENT '分配班级ID',
  `assigned_student_id` bigint DEFAULT NULL COMMENT '创建的学生记录ID',
  `remark` varchar(500) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_plan` (`plan_id`),
  KEY `idx_year` (`academic_year`),
  KEY `idx_status` (`status`),
  KEY `idx_id_card` (`id_card`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报名记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `enrollment_applications`
--

LOCK TABLES `enrollment_applications` WRITE;
/*!40000 ALTER TABLE `enrollment_applications` DISABLE KEYS */;
/*!40000 ALTER TABLE `enrollment_applications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `enrollment_plans`
--

DROP TABLE IF EXISTS `enrollment_plans`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `enrollment_plans` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `academic_year` int NOT NULL COMMENT '招生年份',
  `major_id` bigint NOT NULL COMMENT '专业ID',
  `major_direction_id` bigint DEFAULT NULL COMMENT '专业方向ID',
  `org_unit_id` bigint DEFAULT NULL COMMENT '招收系部',
  `planned_count` int NOT NULL COMMENT '计划招生人数',
  `actual_count` int DEFAULT '0' COMMENT '实际录取人数',
  `registered_count` int DEFAULT '0' COMMENT '已报到人数',
  `enrollment_target` varchar(50) DEFAULT '初中毕业生' COMMENT '招生对象',
  `status` tinyint DEFAULT '0' COMMENT '0草稿 1已发布 2招生中 3已结束',
  `remark` varchar(500) DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_year` (`academic_year`),
  KEY `idx_major` (`major_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='招生计划表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `enrollment_plans`
--

LOCK TABLES `enrollment_plans` WRITE;
/*!40000 ALTER TABLE `enrollment_plans` DISABLE KEYS */;
/*!40000 ALTER TABLE `enrollment_plans` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `entity_attribute_values`
--

DROP TABLE IF EXISTS `entity_attribute_values`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `entity_attribute_values` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `entity_type` varchar(30) NOT NULL,
  `entity_id` bigint NOT NULL,
  `field_code` varchar(50) NOT NULL,
  `value_text` varchar(1000) DEFAULT NULL,
  `value_number` decimal(20,6) DEFAULT NULL,
  `value_date` datetime DEFAULT NULL,
  `value_bool` tinyint DEFAULT NULL,
  `value_ref_id` bigint DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_entity_field` (`entity_type`,`entity_id`,`field_code`,`tenant_id`),
  KEY `idx_field_text` (`field_code`,`value_text`(100)),
  KEY `idx_field_num` (`field_code`,`value_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实体属性值(EAV 兜底)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `entity_attribute_values`
--

LOCK TABLES `entity_attribute_values` WRITE;
/*!40000 ALTER TABLE `entity_attribute_values` DISABLE KEYS */;
/*!40000 ALTER TABLE `entity_attribute_values` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `entity_event_relations`
--

DROP TABLE IF EXISTS `entity_event_relations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `entity_event_relations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` bigint NOT NULL,
  `related_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `related_id` bigint NOT NULL,
  `related_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `relation` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_event` (`event_id`),
  KEY `idx_related` (`related_type`,`related_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `entity_event_relations`
--

LOCK TABLES `entity_event_relations` WRITE;
/*!40000 ALTER TABLE `entity_event_relations` DISABLE KEYS */;
/*!40000 ALTER TABLE `entity_event_relations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `entity_event_types`
--

DROP TABLE IF EXISTS `entity_event_types`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `entity_event_types` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `category_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `category_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `has_score` tinyint DEFAULT '0',
  `has_severity` tinyint DEFAULT '0',
  `severity_levels` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'JSON数组',
  `icon` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `color` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `applicable_subjects` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'JSON: ["USER","ORG","PLACE"]',
  `is_system` tinyint DEFAULT '0',
  `is_enabled` tinyint DEFAULT '1',
  `sort_order` int DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  `plugin_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '插件级启用状态',
  `category_polarity` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT 'NEUTRAL' COMMENT '大类极性: POSITIVE/NEGATIVE/NEUTRAL',
  `industry` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属行业包',
  `plugin_class` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '声明插件全限定类名',
  `origin` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '统一来源',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type` (`tenant_id`,`type_code`),
  KEY `idx_entity_event_type_code` (`type_code`,`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=75 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `entity_event_types`
--

LOCK TABLES `entity_event_types` WRITE;
/*!40000 ALTER TABLE `entity_event_types` DISABLE KEYS */;
INSERT INTO `entity_event_types` VALUES (1,0,'INSPECTION','检查','INSP_GRADE','检查评级',1,0,NULL,NULL,NULL,'[\"ORG_UNIT\",\"PLACE\",\"USER\"]',1,1,0,'2026-06-01 22:57:15','2026-06-01 22:57:29',0,1,'NEUTRAL',NULL,NULL,NULL),(2,0,'INSPECTION','检查','INSP_VIOLATION','违规记录',1,0,NULL,NULL,NULL,'[\"USER\",\"ORG_UNIT\",\"PLACE\"]',1,1,0,'2026-06-01 22:57:15','2026-06-01 22:57:29',0,1,'NEUTRAL',NULL,NULL,NULL),(3,0,'INSPECTION','检查','INSP_COMMENDATION','表扬记录',1,0,NULL,NULL,NULL,'[\"USER\",\"ORG_UNIT\",\"PLACE\"]',1,1,0,'2026-06-01 22:57:15','2026-06-01 22:57:29',0,1,'NEUTRAL',NULL,NULL,NULL),(4,0,'INSPECTION','检查','INSP_RATING','综合评级',0,0,NULL,NULL,NULL,'[\"ORG_UNIT\",\"PLACE\",\"USER\"]',1,1,0,'2026-06-01 22:57:15','2026-06-01 22:57:29',0,1,'NEUTRAL',NULL,NULL,NULL),(5,0,'INSPECTION','检查','INSP_NOTE','检查备注',0,0,NULL,NULL,NULL,'[\"ORG_UNIT\",\"PLACE\",\"USER\"]',1,1,0,'2026-06-01 22:57:15','2026-06-01 22:57:29',0,1,'NEUTRAL',NULL,NULL,NULL),(6,0,'ORG','组织','ORG_TRANSFER','组织调动',0,0,NULL,NULL,NULL,'[\"USER\"]',1,1,0,'2026-06-01 22:57:15','2026-06-01 22:57:15',0,1,'NEUTRAL',NULL,NULL,NULL),(7,0,'ORG','组织','ORG_JOIN','加入组织',0,0,NULL,NULL,NULL,'[\"USER\"]',1,1,0,'2026-06-01 22:57:15','2026-06-01 22:57:15',0,1,'NEUTRAL',NULL,NULL,NULL),(8,0,'ORG','组织','ORG_LEAVE','离开组织',0,0,NULL,NULL,NULL,'[\"USER\"]',1,1,0,'2026-06-01 22:57:15','2026-06-01 22:57:15',0,1,'NEUTRAL',NULL,NULL,NULL),(9,0,'PLACE','场所','PLACE_ASSIGN','场所分配',0,0,NULL,NULL,NULL,'[\"USER\",\"ORG_UNIT\"]',1,1,0,'2026-06-01 22:57:15','2026-06-01 22:57:29',0,1,'NEUTRAL',NULL,NULL,NULL),(10,0,'PLACE','场所','PLACE_RELEASE','场所释放',0,0,NULL,NULL,NULL,'[\"USER\",\"ORG_UNIT\"]',1,1,0,'2026-06-01 22:57:15','2026-06-01 22:57:29',0,1,'NEUTRAL',NULL,NULL,NULL),(11,1,'DISCIPLINE','纪律','LATE','迟到',0,0,NULL,'Clock','#ef4444','USER',1,1,1,'2026-06-01 23:05:47','2026-06-01 23:32:05',0,1,'NEGATIVE','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.DisciplineMessagingPlugin','PLUGIN:EDU@1.0.0'),(12,1,'DISCIPLINE','纪律','ABSENCE','缺勤',0,0,NULL,'UserX','#ef4444','USER',1,1,2,'2026-06-01 23:05:47','2026-06-01 23:32:05',0,1,'NEGATIVE','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.DisciplineMessagingPlugin','PLUGIN:EDU@1.0.0'),(13,1,'DISCIPLINE','纪律','EARLY_LEAVE','早退',0,0,NULL,'LogOut','#ef4444','USER',1,1,3,'2026-06-01 23:05:47','2026-06-01 23:32:05',0,1,'NEGATIVE','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.DisciplineMessagingPlugin','PLUGIN:EDU@1.0.0'),(14,1,'DISCIPLINE','纪律','CONTRABAND','违禁品',0,0,NULL,'Ban','#ef4444','USER',1,1,4,'2026-06-01 23:05:47','2026-06-01 23:32:05',0,1,'NEGATIVE','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.DisciplineMessagingPlugin','PLUGIN:EDU@1.0.0'),(15,1,'DISCIPLINE','纪律','DORM_VIOLATION','宿舍违规',0,0,NULL,'Bed','#ef4444','USER,PLACE',1,1,5,'2026-06-01 23:05:47','2026-06-01 23:32:05',0,1,'NEGATIVE','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.DisciplineMessagingPlugin','PLUGIN:EDU@1.0.0'),(16,1,'DISCIPLINE','纪律','HYGIENE_VIOLATION','卫生违规',0,0,NULL,'Trash2','#ef4444','USER,ORG_UNIT,PLACE',1,1,6,'2026-06-01 23:05:47','2026-06-01 23:32:05',0,1,'NEGATIVE','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.DisciplineMessagingPlugin','PLUGIN:EDU@1.0.0'),(17,1,'DISCIPLINE','纪律','SAFETY_VIOLATION','安全违规',0,0,NULL,'ShieldAlert','#ef4444','USER,ORG_UNIT,PLACE',1,1,7,'2026-06-01 23:05:47','2026-06-01 23:32:05',0,1,'NEGATIVE','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.DisciplineMessagingPlugin','PLUGIN:EDU@1.0.0'),(18,1,'DISCIPLINE','纪律','CLASS_VIOLATION','课堂违纪',0,0,NULL,'BookX','#ef4444','USER,ORG_UNIT',1,1,8,'2026-06-01 23:05:47','2026-06-01 23:32:05',0,1,'NEGATIVE','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.DisciplineMessagingPlugin','PLUGIN:EDU@1.0.0'),(19,1,'DISCIPLINE','纪律','FIGHT','打架斗殴',0,0,NULL,'Swords','#ef4444','USER',1,1,9,'2026-06-01 23:05:47','2026-06-01 23:32:05',0,1,'NEGATIVE','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.DisciplineMessagingPlugin','PLUGIN:EDU@1.0.0'),(20,1,'AWARD','奖励','HONOR','荣誉称号',0,0,NULL,'Trophy','#22c55e','USER,ORG_UNIT',1,1,10,'2026-06-01 23:05:47','2026-06-01 23:32:05',0,1,'POSITIVE','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.AwardMessagingPlugin','PLUGIN:EDU@1.0.0'),(21,1,'AWARD','奖励','PRAISE','表扬',0,0,NULL,'ThumbsUp','#22c55e','USER',1,1,11,'2026-06-01 23:05:47','2026-06-01 23:32:05',0,1,'POSITIVE','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.AwardMessagingPlugin','PLUGIN:EDU@1.0.0'),(22,1,'AWARD','奖励','EXEMPLARY','先进个人',0,0,NULL,'Star','#22c55e','USER',1,1,12,'2026-06-01 23:05:47','2026-06-01 23:32:05',0,1,'POSITIVE','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.AwardMessagingPlugin','PLUGIN:EDU@1.0.0'),(23,1,'AWARD','奖励','EXEMPLARY_ORG','先进组织',0,0,NULL,'Award','#22c55e','ORG_UNIT',1,1,13,'2026-06-01 23:05:47','2026-06-01 23:32:05',0,1,'POSITIVE','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.AwardMessagingPlugin','PLUGIN:EDU@1.0.0'),(24,1,'AWARD','奖励','FULL_ATTENDANCE','全勤奖',0,0,NULL,'CheckCircle','#22c55e','USER',1,1,14,'2026-06-01 23:05:47','2026-06-01 23:32:05',0,1,'POSITIVE','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.AwardMessagingPlugin','PLUGIN:EDU@1.0.0'),(25,1,'AWARD','奖励','DISCIPLINE_EXCELLENT','纪律优秀',0,0,NULL,'Shield','#22c55e','USER,ORG_UNIT',1,1,15,'2026-06-01 23:05:47','2026-06-01 23:32:05',0,1,'POSITIVE','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.AwardMessagingPlugin','PLUGIN:EDU@1.0.0'),(26,1,'ACADEMIC','学业','EXAM_EXCELLENT','考试优秀',0,0,NULL,'GraduationCap','#22c55e','USER',1,1,16,'2026-06-01 23:05:47','2026-06-01 23:32:05',0,1,'POSITIVE','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.AcademicMessagingPlugin','PLUGIN:EDU@1.0.0'),(27,1,'ACADEMIC','学业','EXAM_FAIL','考试不及格',0,0,NULL,'AlertTriangle','#ef4444','USER',1,1,17,'2026-06-01 23:05:47','2026-06-01 23:32:05',0,1,'NEGATIVE','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.AcademicMessagingPlugin','PLUGIN:EDU@1.0.0'),(28,1,'ACADEMIC','学业','CREDIT_WARNING','学业预警',0,0,NULL,'AlertCircle','#f59e0b','USER',1,1,18,'2026-06-01 23:05:47','2026-06-01 23:32:05',0,1,'NEGATIVE','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.AcademicMessagingPlugin','PLUGIN:EDU@1.0.0'),(29,1,'PERSONNEL','人事','MEMBER_JOIN','成员加入',0,0,NULL,'UserPlus','#6b7280','USER,ORG_UNIT',1,1,19,'2026-06-01 23:05:47','2026-06-01 23:32:05',0,1,'NEUTRAL','CORE','com.school.management.infrastructure.extension.plugins.core.messaging.PersonnelMessagingPlugin','PLUGIN:CORE@1.0.0'),(30,1,'PERSONNEL','人事','MEMBER_LEAVE','成员离开',0,0,NULL,'UserMinus','#6b7280','USER,ORG_UNIT',1,1,20,'2026-06-01 23:05:47','2026-06-01 23:32:05',0,1,'NEUTRAL','CORE','com.school.management.infrastructure.extension.plugins.core.messaging.PersonnelMessagingPlugin','PLUGIN:CORE@1.0.0'),(31,1,'PERSONNEL','人事','TRANSFER','调动',0,0,NULL,'ArrowRightLeft','#6b7280','USER',1,1,21,'2026-06-01 23:05:47','2026-06-01 23:32:05',0,1,'NEUTRAL','CORE','com.school.management.infrastructure.extension.plugins.core.messaging.PersonnelMessagingPlugin','PLUGIN:CORE@1.0.0'),(32,1,'PERSONNEL','人事','CHECKIN','入住',0,0,NULL,'Home','#6b7280','USER',1,1,22,'2026-06-01 23:05:47','2026-06-01 23:32:05',0,1,'NEUTRAL','CORE','com.school.management.infrastructure.extension.plugins.core.messaging.PersonnelMessagingPlugin','PLUGIN:CORE@1.0.0'),(33,1,'PERSONNEL','人事','CHECKOUT','退宿',0,0,NULL,'DoorOpen','#6b7280','USER',1,1,23,'2026-06-01 23:05:47','2026-06-01 23:32:05',0,1,'NEUTRAL','CORE','com.school.management.infrastructure.extension.plugins.core.messaging.PersonnelMessagingPlugin','PLUGIN:CORE@1.0.0'),(34,1,'PERSONNEL','人事','ENROLLED','入学',0,0,NULL,'School','#6b7280','USER',1,1,24,'2026-06-01 23:05:47','2026-06-01 23:32:05',0,1,'NEUTRAL','CORE','com.school.management.infrastructure.extension.plugins.core.messaging.PersonnelMessagingPlugin','PLUGIN:CORE@1.0.0'),(35,1,'INSPECTION','检查','INSP_PASS','检查合格',0,0,NULL,'CheckSquare','#22c55e','USER,ORG_UNIT,PLACE',1,1,25,'2026-06-01 23:05:47','2026-06-01 23:32:05',0,1,'NEUTRAL','CORE','com.school.management.infrastructure.extension.plugins.core.messaging.InspectionMessagingPlugin','PLUGIN:CORE@1.0.0'),(36,1,'INSPECTION','检查','INSP_FAIL','检查不合格',0,0,NULL,'XSquare','#ef4444','USER,ORG_UNIT,PLACE',1,1,26,'2026-06-01 23:05:47','2026-06-01 23:32:05',0,1,'NEUTRAL','CORE','com.school.management.infrastructure.extension.plugins.core.messaging.InspectionMessagingPlugin','PLUGIN:CORE@1.0.0'),(37,1,'INSPECTION','检查','DISCIPLINE_FAIL','纪律不合格',0,0,NULL,'ShieldX','#ef4444','USER,ORG_UNIT',1,1,27,'2026-06-01 23:05:47','2026-06-01 23:32:05',0,1,'NEUTRAL','CORE','com.school.management.infrastructure.extension.plugins.core.messaging.InspectionMessagingPlugin','PLUGIN:CORE@1.0.0'),(38,1,'INSPECTION','检查','INSP_RECTIFIED','整改完成',0,0,NULL,'CheckCircle2','#3b82f6','USER,ORG_UNIT,PLACE',1,1,28,'2026-06-01 23:05:47','2026-06-01 23:32:05',0,1,'NEUTRAL','CORE','com.school.management.infrastructure.extension.plugins.core.messaging.InspectionMessagingPlugin','PLUGIN:CORE@1.0.0'),(39,1,'NOTIFICATION','通知','ANNOUNCEMENT','公告',0,0,NULL,'Megaphone','#3b82f6','USER,ORG_UNIT',1,1,29,'2026-06-01 23:05:47','2026-06-01 23:32:05',0,1,'NEUTRAL','CORE','com.school.management.infrastructure.extension.plugins.core.messaging.NotificationMessagingPlugin','PLUGIN:CORE@1.0.0'),(40,1,'NOTIFICATION','通知','SCHEDULE_CHANGE','日程变更',0,0,NULL,'Calendar','#3b82f6','USER,ORG_UNIT',1,1,30,'2026-06-01 23:05:47','2026-06-01 23:32:05',0,1,'NEUTRAL','CORE','com.school.management.infrastructure.extension.plugins.core.messaging.NotificationMessagingPlugin','PLUGIN:CORE@1.0.0'),(41,1,'NOTIFICATION','通知','GRADE_RELEASE','成绩发布',0,0,NULL,'FileText','#3b82f6','USER,ORG_UNIT',1,1,31,'2026-06-01 23:05:47','2026-06-01 23:32:05',0,1,'NEUTRAL','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.AcademicMessagingPlugin','PLUGIN:EDU@1.0.0'),(42,1,'NOTIFICATION','通知','SYSTEM_NOTICE','系统通知',0,0,NULL,'Bell','#6b7280','[\"USER\",\"ORG_UNIT\",\"PLACE\"]',0,1,32,'2026-06-01 23:05:47','2026-06-01 23:05:47',0,1,'NEUTRAL',NULL,NULL,NULL),(53,1,'TEACHING','教学','GRADE_SUBMITTED','成绩已录入(旧版)',0,0,NULL,'edit','#f59e0b','ORG_UNIT',1,1,10,'2026-06-01 23:06:11','2026-06-01 23:32:05',0,1,'NEUTRAL','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.TeachingMessagingPlugin','PLUGIN:EDU@1.0.0'),(54,1,'TEACHING','教学','GRADE_APPROVED','成绩审核通过(旧版)',0,0,NULL,'check','#10b981','ORG_UNIT',1,1,11,'2026-06-01 23:06:11','2026-06-01 23:32:05',0,1,'NEUTRAL','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.TeachingMessagingPlugin','PLUGIN:EDU@1.0.0'),(55,1,'TEACHING','教学','GRADE_PUBLISHED','成绩公示(旧版)',0,0,NULL,'megaphone','#2563eb','ORG_UNIT',1,1,12,'2026-06-01 23:06:11','2026-06-01 23:32:05',0,1,'NEUTRAL','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.TeachingMessagingPlugin','PLUGIN:EDU@1.0.0'),(56,1,'TEACHING','教学','EXAM_PUBLISHED','考试发布(旧版)',0,0,NULL,'calendar','#8b5cf6','USER,ORG_UNIT',1,1,13,'2026-06-01 23:06:11','2026-06-01 23:32:05',0,1,'NEUTRAL','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.TeachingMessagingPlugin','PLUGIN:EDU@1.0.0'),(57,1,'TEACHING','教学','GRADE_PUBLISHED_PERSONAL','成绩发放(旧版)',0,0,NULL,'user-check','#2563eb','USER',1,1,13,'2026-06-01 23:06:14','2026-06-01 23:32:06',0,1,'NEUTRAL','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.TeachingMessagingPlugin','PLUGIN:EDU@1.0.0'),(58,1,'PLACE','场所','DORM_CHECKIN_EVT','入住登记',0,0,NULL,'bed-double','#0d9488','USER',1,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,'NEUTRAL','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.DormitoryMessagingPlugin','PLUGIN:EDU@1.0.0'),(59,1,'PLACE','场所','DORM_CHECKOUT_EVT','退宿登记',0,0,NULL,'log-out','#6b7280','USER',1,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,'NEUTRAL','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.DormitoryMessagingPlugin','PLUGIN:EDU@1.0.0'),(60,1,'DISCIPLINE','纪律','INSP_PASSED_EVT','检查通过',0,0,NULL,'check-shield','#10b981','USER,ORG_UNIT,PLACE',1,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,'POSITIVE','CORE','com.school.management.infrastructure.extension.plugins.core.messaging.InspectionMessagingPlugin','PLUGIN:CORE@1.0.0'),(61,1,'DISCIPLINE','纪律','INSP_FAILED_EVT','检查不通过',0,0,NULL,'alert-triangle','#ef4444','USER,ORG_UNIT,PLACE',1,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,'NEGATIVE','CORE','com.school.management.infrastructure.extension.plugins.core.messaging.InspectionMessagingPlugin','PLUGIN:CORE@1.0.0'),(62,1,'PERSONNEL','人事','ORG_UNIT_CREATED_EVT','组织新建',0,0,NULL,'building-plus','#0ea5e9','ORG_UNIT',1,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,'NEUTRAL','CORE','com.school.management.infrastructure.extension.plugins.core.messaging.OrganizationMessagingPlugin','PLUGIN:CORE@1.0.0'),(63,1,'DISCIPLINE','纪律','ATTENDANCE_LATE_EVT','迟到',0,0,NULL,'clock-alert','#f59e0b','USER',1,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,'NEGATIVE','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.AttendanceMessagingPlugin','PLUGIN:EDU@1.0.0'),(64,1,'DISCIPLINE','纪律','ATTENDANCE_ABSENT_EVT','缺勤',0,0,NULL,'x-circle','#ef4444','USER',1,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,'NEGATIVE','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.AttendanceMessagingPlugin','PLUGIN:EDU@1.0.0'),(65,1,'PERSONNEL','人事','ATTENDANCE_LEAVE_EVT','请假',0,0,NULL,'calendar-minus','#6b7280','USER',1,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,'NEUTRAL','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.AttendanceMessagingPlugin','PLUGIN:EDU@1.0.0'),(66,1,'ACADEMIC','学业','ENROLLMENT_ADMITTED_EVT','录取通知',0,0,NULL,'mail-check','#10b981','USER',1,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,'POSITIVE','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.EnrollmentMessagingPlugin','PLUGIN:EDU@1.0.0'),(67,1,'ACADEMIC','学业','ENROLLMENT_REGISTERED_EVT','报到完成',0,0,NULL,'user-check','#10b981','USER',1,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,'POSITIVE','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.EnrollmentMessagingPlugin','PLUGIN:EDU@1.0.0'),(68,1,'ACADEMIC','学业','STUDENT_STATUS_CHANGED_EVT','学籍变更',0,0,NULL,'refresh-cw','#6b7280','USER',1,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,'NEUTRAL','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.EnrollmentMessagingPlugin','PLUGIN:EDU@1.0.0'),(69,1,'ACADEMIC','学业','GRADE_SUBMITTED_EVT','成绩已录入',0,0,NULL,'edit-3','#2563eb','ORG_UNIT',1,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,'NEUTRAL','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.GradeMessagingPlugin','PLUGIN:EDU@1.0.0'),(70,1,'ACADEMIC','学业','GRADE_APPROVED_EVT','成绩已审核',0,0,NULL,'check-circle','#10b981','ORG_UNIT',1,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,'POSITIVE','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.GradeMessagingPlugin','PLUGIN:EDU@1.0.0'),(71,1,'ACADEMIC','学业','GRADE_PUBLISHED_EVT','成绩已公示',0,0,NULL,'megaphone','#f59e0b','ORG_UNIT',1,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,'NEUTRAL','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.GradeMessagingPlugin','PLUGIN:EDU@1.0.0'),(72,1,'ACADEMIC','学业','GRADE_PUBLISHED_PERSONAL_EVT','成绩已发放',0,0,NULL,'mail','#0ea5e9','USER',1,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,'NEUTRAL','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.GradeMessagingPlugin','PLUGIN:EDU@1.0.0'),(73,1,'TEACHING','教学','EXAM_PUBLISHED_EVT','考试发布',0,0,NULL,'calendar-clock','#6366f1','USER,ORG_UNIT',1,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,'NEUTRAL','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.TeachingMessagingPlugin','PLUGIN:EDU@1.0.0'),(74,1,'TEACHING','教学','SCHEDULE_PUBLISHED_EVT','课表发布',0,0,NULL,'calendar','#2563eb','ORG_UNIT',1,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,'NEUTRAL','EDU','com.school.management.infrastructure.extension.plugins.education.messaging.TeachingMessagingPlugin','PLUGIN:EDU@1.0.0');
/*!40000 ALTER TABLE `entity_event_types` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `entity_events`
--

DROP TABLE IF EXISTS `entity_events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `entity_events` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `subject_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `subject_id` bigint NOT NULL,
  `subject_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `event_category` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `event_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `event_label` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `idempotency_key` varchar(160) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业务幂等 key，唯一。拼接格式 {prefix}:{triggerId}:{subjectType}:{subjectId}',
  `payload` text COLLATE utf8mb4_unicode_ci COMMENT 'JSON',
  `source_module` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `source_ref_type` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `source_ref_id` bigint DEFAULT NULL,
  `tags` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'JSON数组',
  `created_by` bigint DEFAULT NULL,
  `created_by_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `occurred_at` datetime NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_entity_events_idempotency` (`idempotency_key`),
  KEY `idx_subject` (`subject_type`,`subject_id`,`occurred_at` DESC),
  KEY `idx_category` (`event_category`,`event_type`,`occurred_at` DESC),
  KEY `idx_source` (`source_ref_type`,`source_ref_id`),
  KEY `idx_occurred` (`occurred_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `entity_events`
--

LOCK TABLES `entity_events` WRITE;
/*!40000 ALTER TABLE `entity_events` DISABLE KEYS */;
/*!40000 ALTER TABLE `entity_events` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `entity_field_definitions`
--

DROP TABLE IF EXISTS `entity_field_definitions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `entity_field_definitions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `entity_type` varchar(30) NOT NULL,
  `type_code` varchar(50) NOT NULL,
  `field_code` varchar(50) NOT NULL,
  `field_name` varchar(50) DEFAULT NULL,
  `field_type` varchar(20) NOT NULL COMMENT 'text/number/date/relation/bool/enum',
  `category` varchar(50) DEFAULT NULL,
  `is_required` tinyint DEFAULT '0',
  `is_system` tinyint DEFAULT '0' COMMENT '1=插件声明,0=管理员自定义',
  `options` json DEFAULT NULL,
  `storage_kind` varchar(20) NOT NULL DEFAULT 'ATTR' COMMENT 'COLUMN/ATTR',
  `storage_table` varchar(100) DEFAULT NULL,
  `storage_column` varchar(100) DEFAULT NULL,
  `sort_order` int DEFAULT '0',
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_field` (`entity_type`,`type_code`,`field_code`,`tenant_id`),
  KEY `idx_storage` (`storage_kind`,`storage_table`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实体字段定义注册';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `entity_field_definitions`
--

LOCK TABLES `entity_field_definitions` WRITE;
/*!40000 ALTER TABLE `entity_field_definitions` DISABLE KEYS */;
/*!40000 ALTER TABLE `entity_field_definitions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `entity_type_configs`
--

DROP TABLE IF EXISTS `entity_type_configs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `entity_type_configs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `entity_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ORG_UNIT/PLACE/USER',
  `type_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '类型描述',
  `icon` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图标名',
  `category` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '大类',
  `parent_type_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `allowed_child_type_codes` json DEFAULT NULL,
  `max_depth` int DEFAULT NULL COMMENT '允许的最大子级深度',
  `default_role_codes` json DEFAULT NULL COMMENT '关联默认角色编码 (user)',
  `default_user_type_codes` json DEFAULT NULL COMMENT '关联默认用户类型编码 (org/place)',
  `default_org_type_codes` json DEFAULT NULL COMMENT '关联默认组织类型编码 (user/place)',
  `default_place_type_codes` json DEFAULT NULL COMMENT '关联默认场所类型编码 (org/user)',
  `metadata_schema` json NOT NULL DEFAULT (json_object(_utf8mb4'fields',json_array())),
  `features` json DEFAULT (json_object()),
  `ui_config` json DEFAULT (json_object()),
  `is_plugin_registered` tinyint DEFAULT '0' COMMENT '是否由插件注册',
  `plugin_class` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '插件实现类全名',
  `origin` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '统一来源: PLUGIN:<code>@<ver> / TENANT:CUSTOM#<id>',
  `is_system` tinyint DEFAULT '0',
  `is_enabled` tinyint DEFAULT '1',
  `sort_order` int DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  `plugin_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '插件级启用状态 (0=所属插件被禁)',
  `overridden_fields` json DEFAULT NULL COMMENT '管理员显式覆写的字段名数组, 如 ["typeName","category"]',
  `industry` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属行业包',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_entity_type` (`tenant_id`,`entity_type`,`type_code`),
  KEY `idx_entity_type_configs_tenant` (`tenant_id`),
  KEY `idx_entity_type_configs_parent` (`parent_type_code`),
  KEY `idx_entity_type_configs_enabled` (`is_enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=90 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='统一实体类型注册表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `entity_type_configs`
--

LOCK TABLES `entity_type_configs` WRITE;
/*!40000 ALTER TABLE `entity_type_configs` DISABLE KEYS */;
INSERT INTO `entity_type_configs` VALUES (1,1,'USER','ADMIN','管理员','系统管理人员',NULL,NULL,NULL,'[]',NULL,'[\"SUPER_ADMIN\"]',NULL,NULL,NULL,'{\"fields\": []}','{\"isStaff\": true, \"canLogin\": true}','{\"icon\": \"shield-check\", \"color\": \"#4338ca\"}',1,'com.school.management.infrastructure.extension.plugins.core.CoreAdminPlugin','PLUGIN:CORE@1.0.0',0,1,0,'2026-06-01 22:57:25','2026-06-01 23:32:05',0,1,NULL,'CORE'),(2,1,'USER','TEACHER','教师','教职工人员',NULL,'STAFF',NULL,'[]',NULL,'[\"TEACHER\"]',NULL,NULL,NULL,'{\"fields\": [{\"key\": \"employeeNo\", \"type\": \"text\", \"group\": \"基本信息\", \"label\": \"工号\", \"config\": {\"maxLength\": 50}, \"system\": true, \"required\": false}, {\"key\": \"subject\", \"type\": \"select\", \"group\": \"教学信息\", \"label\": \"学科\", \"config\": {\"options\": [{\"label\": \"数学\", \"value\": \"MATH\"}, {\"label\": \"语文\", \"value\": \"CHINESE\"}, {\"label\": \"英语\", \"value\": \"ENGLISH\"}, {\"label\": \"物理\", \"value\": \"PHYSICS\"}, {\"label\": \"化学\", \"value\": \"CHEMISTRY\"}, {\"label\": \"生物\", \"value\": \"BIOLOGY\"}, {\"label\": \"历史\", \"value\": \"HISTORY\"}, {\"label\": \"地理\", \"value\": \"GEOGRAPHY\"}, {\"label\": \"体育\", \"value\": \"PE\"}, {\"label\": \"计算机\", \"value\": \"CS\"}, {\"label\": \"其他\", \"value\": \"OTHER\"}]}, \"system\": true, \"required\": false}, {\"key\": \"title\", \"type\": \"select\", \"group\": \"教学信息\", \"label\": \"职称\", \"config\": {\"options\": [{\"label\": \"初级\", \"value\": \"PRIMARY\"}, {\"label\": \"中级\", \"value\": \"INTERMEDIATE\"}, {\"label\": \"高级\", \"value\": \"SENIOR\"}, {\"label\": \"正高级\", \"value\": \"PROFESSOR\"}]}, \"system\": true, \"required\": false}, {\"key\": \"maxWeeklyHours\", \"type\": \"number\", \"group\": \"排课配置\", \"label\": \"周最大课时\", \"config\": {\"max\": 30, \"min\": 0, \"default\": 16}, \"system\": true, \"required\": false}]}','{\"isStaff\": true, \"canLogin\": true, \"canTeach\": true, \"canApproveGrade\": true, \"canBeAdminOfOrg\": true, \"profileEditableBySelf\": true, \"canBeResponsibleForPlace\": true}','{\"icon\": \"user-check\", \"color\": \"#16a34a\"}',1,'com.school.management.infrastructure.extension.plugins.education.TeacherPlugin','PLUGIN:EDU@1.0.0',0,1,0,'2026-06-01 22:57:25','2026-06-01 23:32:05',0,1,NULL,'EDU'),(3,1,'USER','STUDENT','学生','在校学生',NULL,'MEMBER',NULL,'[]',NULL,'[\"STUDENT\"]',NULL,NULL,NULL,'{\"fields\": [{\"key\": \"studentNo\", \"type\": \"text\", \"group\": \"基本信息\", \"label\": \"学号\", \"system\": true, \"required\": true}, {\"key\": \"orgUnitId\", \"type\": \"relation\", \"group\": \"学籍信息\", \"label\": \"班级\", \"config\": {\"target\": \"org_units\", \"labelField\": \"unitName\", \"targetTypeCode\": \"CLASS\"}, \"system\": true, \"required\": true}, {\"key\": \"enrollmentDate\", \"type\": \"date\", \"group\": \"学籍信息\", \"label\": \"入学日期\", \"system\": true, \"required\": false}, {\"key\": \"guardianName\", \"type\": \"text\", \"group\": \"家庭信息\", \"label\": \"监护人姓名\", \"system\": true, \"required\": false}, {\"key\": \"guardianPhone\", \"type\": \"text\", \"group\": \"家庭信息\", \"label\": \"监护人电话\", \"system\": true, \"required\": false}]}','{\"canLogin\": true, \"canEnroll\": true, \"isLearner\": true, \"hasGuardian\": true, \"attendanceTracked\": true, \"canBeAssignedToClass\": true, \"manageableByOrgAdmin\": true, \"receivesPersonalGrade\": true}','{\"icon\": \"graduation-cap\", \"color\": \"#2563eb\"}',1,'com.school.management.infrastructure.extension.plugins.education.StudentPlugin','PLUGIN:EDU@1.0.0',0,1,0,'2026-06-01 22:57:25','2026-06-01 23:32:05',0,1,NULL,'EDU'),(4,1,'USER','EXTERNAL','外部人员','临时或外部人员',NULL,'EXTERNAL',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{}','{}',0,NULL,NULL,0,1,0,'2026-06-01 22:57:25','2026-06-01 22:57:28',0,1,NULL,NULL),(5,1,'USER','SUPER_ADMIN','超级管理员','系统最高管理员',NULL,'ADMIN',NULL,'[]',NULL,'[\"SUPER_ADMIN\"]',NULL,NULL,NULL,'{\"fields\": []}','{}','{\"icon\": \"shield\", \"color\": \"#dc2626\"}',1,'com.school.management.infrastructure.extension.plugins.education.SuperAdminPlugin','PLUGIN:EDU@1.0.0',0,1,0,'2026-06-01 22:57:25','2026-06-01 23:32:05',0,1,NULL,'EDU'),(6,1,'USER','SYSTEM_ADMIN','系统管理员','系统配置管理员',NULL,'ADMIN',NULL,NULL,NULL,'[\"SCHOOL_ADMIN\"]',NULL,NULL,NULL,'{\"fields\": []}','{}','{}',0,NULL,NULL,0,1,0,'2026-06-01 22:57:25','2026-06-01 22:57:28',0,1,NULL,NULL),(7,1,'USER','ORG_ADMIN','组织管理员','组织机构管理员',NULL,'ADMIN',NULL,NULL,NULL,'[\"ACADEMIC_DIRECTOR\", \"SUBJECT_TEACHER\", \"INSPECTOR\", \"DORMITORY_MANAGER\"]',NULL,NULL,NULL,'{\"fields\": []}','{}','{}',0,NULL,NULL,0,1,0,'2026-06-01 22:57:25','2026-06-01 22:57:28',0,1,NULL,NULL),(8,1,'USER','TEACHING_STAFF','教学人员','从事教学工作的教师',NULL,'STAFF',NULL,NULL,NULL,'[\"SUBJECT_TEACHER\"]',NULL,NULL,NULL,'{\"fields\": []}','{}','{}',0,NULL,NULL,0,1,0,'2026-06-01 22:57:25','2026-06-01 22:57:28',0,1,NULL,NULL),(9,1,'USER','CLASS_TEACHER','班主任','班级管理教师',NULL,'STAFF',NULL,NULL,NULL,'[\"CLASS_TEACHER\"]',NULL,NULL,NULL,'{\"fields\": []}','{}','{}',0,NULL,NULL,0,1,0,'2026-06-01 22:57:25','2026-06-01 22:57:28',0,1,NULL,NULL),(10,1,'USER','COUNSELOR','辅导员','学生辅导员',NULL,'STAFF',NULL,'[]',NULL,'[\"GRADE_DIRECTOR\"]',NULL,NULL,NULL,'{\"fields\": [{\"key\": \"managedGrades\", \"type\": \"tags\", \"group\": \"职责范围\", \"label\": \"管理年级\", \"config\": {\"options\": [{\"label\": \"2023级\", \"value\": \"2023\"}, {\"label\": \"2024级\", \"value\": \"2024\"}, {\"label\": \"2025级\", \"value\": \"2025\"}]}, \"system\": true, \"required\": false}, {\"key\": \"officeLocation\", \"type\": \"text\", \"group\": \"联系方式\", \"label\": \"办公室\", \"system\": true, \"required\": false}, {\"key\": \"officePhone\", \"type\": \"text\", \"group\": \"联系方式\", \"label\": \"办公电话\", \"system\": true, \"required\": false}, {\"key\": \"maxStudents\", \"type\": \"number\", \"group\": \"职责范围\", \"label\": \"管理学生上限\", \"config\": {\"max\": 500, \"min\": 0, \"default\": 200}, \"system\": true, \"required\": false}]}','{\"isStaff\": true, \"canLogin\": true, \"canCounsel\": true, \"canBeAdminOfOrg\": true, \"profileEditableBySelf\": true}','{\"icon\": \"user-check\", \"color\": \"#7c3aed\"}',1,'com.school.management.infrastructure.extension.plugins.education.CounselorPlugin','PLUGIN:EDU@1.0.0',0,1,0,'2026-06-01 22:57:25','2026-06-01 23:32:05',0,1,NULL,'EDU'),(11,1,'USER','INSPECTOR','检查员','量化检查专员',NULL,'STAFF',NULL,NULL,NULL,'[\"INSPECTOR\"]',NULL,NULL,NULL,'{\"fields\": []}','{}','{}',0,NULL,NULL,0,1,0,'2026-06-01 22:57:25','2026-06-01 22:57:28',0,1,NULL,NULL),(12,1,'USER','DORM_MANAGER','宿管员','宿舍管理人员',NULL,'STAFF',NULL,NULL,NULL,'[\"DORMITORY_MANAGER\"]',NULL,NULL,NULL,'{\"fields\": []}','{}','{}',0,NULL,NULL,0,1,0,'2026-06-01 22:57:25','2026-06-01 22:57:28',0,1,NULL,NULL),(13,1,'USER','ADMIN_STAFF','行政人员','行政管理人员',NULL,'STAFF',NULL,NULL,NULL,'[\"SCHOOL_ADMIN\"]',NULL,NULL,NULL,'{\"fields\": []}','{}','{}',0,NULL,NULL,0,1,0,'2026-06-01 22:57:25','2026-06-01 22:57:28',0,1,NULL,NULL),(14,1,'USER','UNDERGRADUATE','本科生','本科在读学生',NULL,'MEMBER',NULL,NULL,NULL,'[\"STUDENT\"]',NULL,NULL,NULL,'{\"fields\": []}','{}','{}',0,NULL,NULL,0,1,0,'2026-06-01 22:57:25','2026-06-01 22:57:28',0,1,NULL,NULL),(15,1,'USER','GRADUATE','研究生','研究生在读学生',NULL,'MEMBER',NULL,NULL,NULL,'[\"STUDENT\"]',NULL,NULL,NULL,'{\"fields\": []}','{}','{}',0,NULL,NULL,0,1,0,'2026-06-01 22:57:25','2026-06-01 22:57:28',0,1,NULL,NULL),(16,1,'USER','EXCHANGE','交换生','交换访问学生',NULL,'MEMBER',NULL,NULL,NULL,'[\"STUDENT\"]',NULL,NULL,NULL,'{\"fields\": []}','{}','{}',0,NULL,NULL,0,1,0,'2026-06-01 22:57:25','2026-06-01 22:57:28',0,1,NULL,NULL),(17,1,'USER','VISITOR','访客','临时来访人员',NULL,'EXTERNAL',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{}','{}',0,NULL,NULL,0,1,0,'2026-06-01 22:57:25','2026-06-01 22:57:28',0,1,NULL,NULL),(18,1,'USER','CONTRACTOR','外包人员','外包服务人员',NULL,'EXTERNAL',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{}','{}',0,NULL,NULL,0,1,0,'2026-06-01 22:57:25','2026-06-01 22:57:28',0,1,NULL,NULL),(32,1,'ORG_UNIT','ORGANIZATION','组织',NULL,NULL,'ROOT',NULL,'[\"DIVISION\", \"DEPARTMENT\", \"GRADE\"]',NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{\"attendance\": false, \"scheduling\": false, \"inspectionTarget\": 1, \"memberManagement\": false, \"dataPermissionBoundary\": true}','{}',0,NULL,NULL,1,1,1,'2026-06-01 23:06:03','2026-06-01 23:06:03',0,1,NULL,NULL),(33,1,'ORG_UNIT','DIVISION','事业部',NULL,NULL,'BRANCH','ORGANIZATION','[\"DEPARTMENT\", \"SECTION\"]',NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{\"attendance\": false, \"scheduling\": false, \"inspectionTarget\": 1, \"memberManagement\": false, \"dataPermissionBoundary\": true}','{}',0,NULL,NULL,1,1,2,'2026-06-01 23:06:03','2026-06-01 23:06:03',0,1,NULL,NULL),(34,1,'ORG_UNIT','DEPARTMENT','系部',NULL,NULL,'BRANCH','SCHOOL','[\"GRADE\", \"TEACHING_GROUP\"]',NULL,NULL,NULL,NULL,NULL,'{\"fields\": [{\"key\": \"deanName\", \"type\": \"user\", \"group\": \"人员配置\", \"label\": \"系主任\", \"system\": true, \"required\": false}, {\"key\": \"deptPhone\", \"type\": \"text\", \"group\": \"联系方式\", \"label\": \"办公电话\", \"system\": true, \"required\": false}]}','{}','{\"icon\": \"building\", \"color\": \"#7c3aed\"}',1,'com.school.management.infrastructure.extension.plugins.education.DepartmentPlugin','PLUGIN:EDU@1.0.0',1,1,3,'2026-06-01 23:06:03','2026-06-01 23:32:05',0,1,NULL,'EDU'),(35,1,'ORG_UNIT','SECTION','教研室',NULL,NULL,NULL,NULL,'[]',NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{}','{\"icon\": \"library\", \"color\": \"#16a34a\"}',1,'com.school.management.infrastructure.extension.plugins.education.SectionPlugin','PLUGIN:EDU@1.0.0',1,1,4,'2026-06-01 23:06:03','2026-06-01 23:32:05',0,1,NULL,'EDU'),(36,1,'ORG_UNIT','TEAM','小组',NULL,NULL,'GROUP','SECTION',NULL,NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{\"attendance\": true, \"scheduling\": false, \"inspectionTarget\": 1, \"memberManagement\": true, \"dataPermissionBoundary\": false}','{}',0,NULL,NULL,1,1,5,'2026-06-01 23:06:03','2026-06-01 23:06:03',0,1,NULL,NULL),(37,1,'ORG_UNIT','CLASS','班级',NULL,NULL,'GROUP','GRADE','[]',NULL,NULL,NULL,NULL,NULL,'{\"fields\": [{\"key\": \"enrollmentYear\", \"type\": \"number\", \"group\": \"基本信息\", \"label\": \"入学年份\", \"config\": {\"max\": 2035, \"min\": 2020}, \"system\": true, \"required\": true}, {\"key\": \"majorId\", \"type\": \"relation\", \"group\": \"基本信息\", \"label\": \"专业\", \"config\": {\"target\": \"majors\", \"labelField\": \"majorName\"}, \"system\": true, \"required\": false}, {\"key\": \"headTeacher\", \"type\": \"user\", \"group\": \"人员配置\", \"label\": \"班主任\", \"config\": {\"role\": \"TEACHER\"}, \"system\": true, \"required\": false}, {\"key\": \"assistantTeacher\", \"type\": \"user\", \"group\": \"人员配置\", \"label\": \"副班主任\", \"config\": {\"role\": \"TEACHER\"}, \"system\": true, \"required\": false}, {\"key\": \"duration\", \"type\": \"number\", \"group\": \"基本信息\", \"label\": \"学制(年)\", \"config\": {\"max\": 6, \"min\": 1, \"default\": 3}, \"system\": true, \"required\": false}, {\"key\": \"classType\", \"type\": \"select\", \"group\": \"基本信息\", \"label\": \"班级类型\", \"config\": {\"options\": [{\"label\": \"普通班\", \"value\": 1}, {\"label\": \"重点班\", \"value\": 2}, {\"label\": \"实验班\", \"value\": 3}]}, \"system\": true, \"required\": false}]}','{\"hasStudents\": true, \"hasTimetable\": true, \"hasAttendance\": true}','{\"icon\": \"users\", \"color\": \"#2563eb\"}',1,'com.school.management.infrastructure.extension.plugins.education.ClassPlugin','PLUGIN:EDU@1.0.0',1,1,50,'2026-06-01 23:06:03','2026-06-01 23:32:05',0,1,NULL,'EDU'),(39,1,'PLACE','DORMITORY','宿舍区',NULL,NULL,'SPACE',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{\"hasGender\": true, \"hasCapacity\": true, \"hasOccupancy\": true}','{\"icon\": \"BedDouble\", \"color\": \"teal\"}',0,NULL,NULL,0,1,0,'2026-06-01 23:06:09','2026-06-01 23:06:10',0,1,NULL,NULL),(40,1,'PLACE','TEACHING','教学区',NULL,NULL,'SPACE',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{\"hasGender\": false, \"hasCapacity\": false, \"hasOccupancy\": false}','{\"icon\": \"GraduationCap\", \"color\": \"blue\"}',0,NULL,NULL,0,1,0,'2026-06-01 23:06:09','2026-06-01 23:06:10',0,1,NULL,NULL),(41,1,'PLACE','ACTIVITY','活动区',NULL,NULL,'SPACE',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{}','{}',0,NULL,NULL,0,1,0,'2026-06-01 23:06:09','2026-06-01 23:06:09',0,1,NULL,NULL),(42,1,'PLACE','OFFICE','办公区',NULL,NULL,'SPACE',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{\"hasGender\": false, \"hasCapacity\": true, \"hasOccupancy\": true}','{\"icon\": \"Briefcase\", \"color\": \"slate\"}',0,NULL,NULL,0,1,0,'2026-06-01 23:06:09','2026-06-01 23:06:10',0,1,NULL,NULL),(43,1,'PLACE','DORM_BUILDING','宿舍楼',NULL,NULL,'BUILDING',NULL,'[]',NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{}','{\"icon\": \"building-2\", \"color\": \"#0891b2\"}',1,'com.school.management.infrastructure.extension.plugins.education.EducationDormBuildingPlugin','PLUGIN:EDU@1.0.0',0,1,0,'2026-06-01 23:06:09','2026-06-01 23:32:05',0,1,NULL,'EDU'),(44,1,'PLACE','DORM_FLOOR','宿舍楼层',NULL,NULL,'FLOOR',NULL,'[]',NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{}','{\"icon\": \"layers\", \"color\": \"#0891b2\"}',1,'com.school.management.infrastructure.extension.plugins.education.EducationDormFloorPlugin','PLUGIN:EDU@1.0.0',0,1,0,'2026-06-01 23:06:09','2026-06-01 23:32:05',0,1,NULL,'EDU'),(45,1,'PLACE','DORM_ROOM','宿舍',NULL,NULL,'ROOM','DORM_FLOOR','[]',NULL,NULL,NULL,NULL,NULL,'{\"fields\": [{\"key\": \"floorNumber\", \"type\": \"number\", \"group\": \"位置信息\", \"label\": \"楼层\", \"system\": true, \"required\": false}, {\"key\": \"roomManager\", \"type\": \"user\", \"group\": \"人员配置\", \"label\": \"舍长\", \"config\": {\"role\": \"STUDENT\"}, \"system\": true, \"required\": false}]}','{\"hasGender\": true, \"hasCapacity\": true, \"hasOccupancy\": true}','{\"icon\": \"bed-double\", \"color\": \"#0d9488\"}',1,'com.school.management.infrastructure.extension.plugins.education.DormitoryPlugin','PLUGIN:EDU@1.0.0',0,1,0,'2026-06-01 23:06:09','2026-06-01 23:32:05',0,1,NULL,'EDU'),(46,1,'PLACE','TEACH_BUILDING','教学楼',NULL,NULL,'SPACE','TEACHING',NULL,NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{}','{}',0,NULL,NULL,0,1,0,'2026-06-01 23:06:09','2026-06-01 23:06:09',0,1,NULL,NULL),(47,1,'PLACE','CLASSROOM','教室',NULL,NULL,'ROOM',NULL,'[]',NULL,NULL,NULL,NULL,NULL,'{\"fields\": [{\"key\": \"roomFunction\", \"type\": \"select\", \"group\": \"基本信息\", \"label\": \"教室功能\", \"config\": {\"options\": [{\"label\": \"普通教室\", \"value\": \"NORMAL\"}, {\"label\": \"多媒体教室\", \"value\": \"MULTIMEDIA\"}, {\"label\": \"实验室\", \"value\": \"LAB\"}, {\"label\": \"机房\", \"value\": \"COMPUTER\"}]}, \"system\": true, \"required\": false}, {\"key\": \"hasProjector\", \"type\": \"boolean\", \"group\": \"设施配置\", \"label\": \"有投影仪\", \"system\": true, \"required\": false}, {\"key\": \"hasAC\", \"type\": \"boolean\", \"group\": \"设施配置\", \"label\": \"有空调\", \"system\": true, \"required\": false}, {\"key\": \"examCapacity\", \"type\": \"number\", \"group\": \"考试配置\", \"label\": \"考试容量\", \"config\": {\"max\": 500, \"min\": 0}, \"system\": true, \"required\": false}]}','{}','{\"icon\": \"building\", \"color\": \"#d97706\"}',1,'com.school.management.infrastructure.extension.plugins.education.ClassroomPlugin','PLUGIN:EDU@1.0.0',0,1,0,'2026-06-01 23:06:09','2026-06-01 23:32:05',0,1,NULL,'EDU'),(48,1,'PLACE','LAB','实验室',NULL,NULL,'SPACE','TEACH_BUILDING',NULL,NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{\"hasGender\": false, \"hasCapacity\": true, \"hasOccupancy\": false}','{\"icon\": \"FlaskConical\", \"color\": \"amber\"}',0,NULL,NULL,0,1,0,'2026-06-01 23:06:09','2026-06-01 23:06:10',0,1,NULL,NULL),(49,1,'PLACE','COMPUTER_ROOM','机房',NULL,NULL,'SPACE','TEACH_BUILDING',NULL,NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{}','{}',0,NULL,NULL,0,1,0,'2026-06-01 23:06:09','2026-06-01 23:06:09',0,1,NULL,NULL),(50,1,'PLACE','LIBRARY','图书馆',NULL,NULL,'SPACE','TEACHING',NULL,NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{\"hasGender\": false, \"hasCapacity\": true, \"hasOccupancy\": false}','{\"icon\": \"BookOpen\", \"color\": \"amber\"}',0,NULL,NULL,0,1,0,'2026-06-01 23:06:09','2026-06-01 23:06:10',0,1,NULL,NULL),(51,1,'PLACE','MEETING_ROOM','会议室',NULL,NULL,'SPACE','ACTIVITY',NULL,NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{}','{}',0,NULL,NULL,0,1,0,'2026-06-01 23:06:09','2026-06-01 23:06:09',0,1,NULL,NULL),(52,1,'PLACE','LECTURE_HALL','报告厅',NULL,NULL,'SPACE','ACTIVITY',NULL,NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{}','{}',0,NULL,NULL,0,1,0,'2026-06-01 23:06:09','2026-06-01 23:06:09',0,1,NULL,NULL),(53,1,'PLACE','GYM','体育馆',NULL,NULL,'SPACE','ACTIVITY',NULL,NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{}','{}',0,NULL,NULL,0,1,0,'2026-06-01 23:06:09','2026-06-01 23:06:09',0,1,NULL,NULL),(54,1,'PLACE','PLAYGROUND','运动场',NULL,NULL,'SPACE','ACTIVITY',NULL,NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{}','{}',0,NULL,NULL,0,1,0,'2026-06-01 23:06:09','2026-06-01 23:06:09',0,1,NULL,NULL),(70,1,'PLACE','AREA','区域',NULL,NULL,'AREA',NULL,'[]',NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{}','{\"icon\": \"map\", \"color\": \"#6b7280\"}',1,'com.school.management.infrastructure.extension.plugins.core.CoreAreaPlugin','PLUGIN:CORE@1.0.0',0,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,NULL,'CORE'),(71,1,'PLACE','BUILDING','建筑',NULL,NULL,'BUILDING',NULL,'[]',NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{}','{\"icon\": \"building\", \"color\": \"#6b7280\"}',1,'com.school.management.infrastructure.extension.plugins.core.CoreBuildingPlugin','PLUGIN:CORE@1.0.0',0,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,NULL,'CORE'),(72,1,'PLACE','FLOOR','楼层',NULL,NULL,'FLOOR',NULL,'[]',NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{}','{\"icon\": \"layers\", \"color\": \"#6b7280\"}',1,'com.school.management.infrastructure.extension.plugins.core.CoreFloorPlugin','PLUGIN:CORE@1.0.0',0,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,NULL,'CORE'),(73,1,'USER','GUEST','访客',NULL,NULL,NULL,NULL,'[]',NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{\"canLogin\": false, \"isExternal\": true}','{\"icon\": \"user-circle\", \"color\": \"#94a3b8\"}',1,'com.school.management.infrastructure.extension.plugins.core.CoreGuestPlugin','PLUGIN:CORE@1.0.0',0,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,NULL,'CORE'),(74,1,'PLACE','POINT','点位',NULL,NULL,'POINT',NULL,'[]',NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{}','{\"icon\": \"locate\", \"color\": \"#6b7280\"}',1,'com.school.management.infrastructure.extension.plugins.core.CorePointPlugin','PLUGIN:CORE@1.0.0',0,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,NULL,'CORE'),(75,1,'PLACE','ROOM','房间',NULL,NULL,'ROOM',NULL,'[]',NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{}','{\"icon\": \"door-open\", \"color\": \"#6b7280\"}',1,'com.school.management.infrastructure.extension.plugins.core.CoreRoomPlugin','PLUGIN:CORE@1.0.0',0,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,NULL,'CORE'),(76,1,'PLACE','SITE','场地',NULL,NULL,'AREA',NULL,'[]',NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{}','{\"icon\": \"map-pin\", \"color\": \"#6b7280\"}',1,'com.school.management.infrastructure.extension.plugins.core.CoreSitePlugin','PLUGIN:CORE@1.0.0',0,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,NULL,'CORE'),(77,1,'USER','STAFF','职工',NULL,NULL,NULL,NULL,'[]',NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{\"isStaff\": true, \"canLogin\": true}','{\"icon\": \"user\", \"color\": \"#475569\"}',1,'com.school.management.infrastructure.extension.plugins.core.CoreStaffPlugin','PLUGIN:CORE@1.0.0',0,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,NULL,'CORE'),(78,1,'ORG_UNIT','ADMIN_OFFICE','行政部门',NULL,NULL,NULL,NULL,'[]',NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{}','{\"icon\": \"briefcase\", \"color\": \"#475569\"}',1,'com.school.management.infrastructure.extension.plugins.education.AdminOfficePlugin','PLUGIN:EDU@1.0.0',0,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,NULL,'EDU'),(79,1,'PLACE','TYPE_COMPUTER_LAB','机房',NULL,NULL,'ROOM',NULL,'[]',NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{}','{\"icon\": \"monitor\", \"color\": \"#2563eb\"}',1,'com.school.management.infrastructure.extension.plugins.education.EducationComputerLabPlugin','PLUGIN:EDU@1.0.0',0,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,NULL,'EDU'),(80,1,'PLACE','TYPE_LAB','实验室',NULL,NULL,'ROOM',NULL,'[]',NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{}','{\"icon\": \"flask-conical\", \"color\": \"#7c3aed\"}',1,'com.school.management.infrastructure.extension.plugins.education.EducationLabPlugin','PLUGIN:EDU@1.0.0',0,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,NULL,'EDU'),(81,1,'PLACE','TYPE_TEACH_BLDG','教学楼',NULL,NULL,'BUILDING',NULL,'[]',NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{}','{\"icon\": \"school\", \"color\": \"#d97706\"}',1,'com.school.management.infrastructure.extension.plugins.education.EducationTeachBuildingPlugin','PLUGIN:EDU@1.0.0',0,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,NULL,'EDU'),(82,1,'PLACE','TYPE_TEACH_FLOOR','教学楼层',NULL,NULL,'FLOOR',NULL,'[]',NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{}','{\"icon\": \"layers\", \"color\": \"#d97706\"}',1,'com.school.management.infrastructure.extension.plugins.education.EducationTeachFloorPlugin','PLUGIN:EDU@1.0.0',0,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,NULL,'EDU'),(83,1,'PLACE','TYPE_TRAIN_BLDG','实训楼',NULL,NULL,'BUILDING',NULL,'[]',NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{}','{\"icon\": \"factory\", \"color\": \"#b45309\"}',1,'com.school.management.infrastructure.extension.plugins.education.EducationTrainBuildingPlugin','PLUGIN:EDU@1.0.0',0,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,NULL,'EDU'),(84,1,'PLACE','TYPE_TRAIN_FLOOR','实训楼层',NULL,NULL,'FLOOR',NULL,'[]',NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{}','{\"icon\": \"layers\", \"color\": \"#b45309\"}',1,'com.school.management.infrastructure.extension.plugins.education.EducationTrainFloorPlugin','PLUGIN:EDU@1.0.0',0,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,NULL,'EDU'),(85,1,'PLACE','TYPE_TRAINING','实训室',NULL,NULL,'ROOM',NULL,'[]',NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{}','{\"icon\": \"wrench\", \"color\": \"#b45309\"}',1,'com.school.management.infrastructure.extension.plugins.education.EducationTrainingRoomPlugin','PLUGIN:EDU@1.0.0',0,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,NULL,'EDU'),(86,1,'ORG_UNIT','GRADE','年级',NULL,NULL,'CONTAINER','DEPARTMENT','[\"CLASS\"]',NULL,NULL,NULL,NULL,NULL,'{\"fields\": [{\"key\": \"enrollmentYear\", \"type\": \"number\", \"group\": \"基本信息\", \"label\": \"入学年份\", \"config\": {\"max\": 2035, \"min\": 2020}, \"system\": true, \"required\": true}, {\"key\": \"schoolingYears\", \"type\": \"number\", \"group\": \"基本信息\", \"label\": \"学制(年)\", \"config\": {\"max\": 6, \"min\": 1, \"default\": 3}, \"system\": true, \"required\": false}, {\"key\": \"gradeDirector\", \"type\": \"user\", \"group\": \"人员配置\", \"label\": \"年级主任\", \"config\": {\"role\": \"TEACHER\"}, \"system\": true, \"required\": false}]}','{\"hasExams\": true, \"hasClasses\": true}','{\"icon\": \"layers\", \"color\": \"#7c3aed\"}',1,'com.school.management.infrastructure.extension.plugins.education.GradePlugin','PLUGIN:EDU@1.0.0',0,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,NULL,'EDU'),(87,1,'USER','PARENT','家长',NULL,NULL,NULL,NULL,'[]',NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{\"canLogin\": true, \"isExternal\": true, \"hasGuardian\": false}','{\"icon\": \"users\", \"color\": \"#ea580c\"}',1,'com.school.management.infrastructure.extension.plugins.education.ParentPlugin','PLUGIN:EDU@1.0.0',0,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,NULL,'EDU'),(88,1,'ORG_UNIT','SCHOOL','学校',NULL,NULL,'ROOT',NULL,'[\"DEPARTMENT\", \"ADMIN_OFFICE\"]',NULL,NULL,NULL,NULL,NULL,'{\"fields\": [{\"key\": \"schoolLevel\", \"type\": \"select\", \"group\": \"基本信息\", \"label\": \"办学层次\", \"config\": {\"options\": [{\"label\": \"小学\", \"value\": \"PRIMARY\"}, {\"label\": \"初中\", \"value\": \"JUNIOR\"}, {\"label\": \"高中\", \"value\": \"SENIOR\"}, {\"label\": \"中职\", \"value\": \"VOCATIONAL\"}, {\"label\": \"高职/大专\", \"value\": \"COLLEGE\"}, {\"label\": \"本科及以上\", \"value\": \"UNIVERSITY\"}]}, \"system\": true, \"required\": false}, {\"key\": \"principalName\", \"type\": \"user\", \"group\": \"人员配置\", \"label\": \"校长\", \"system\": true, \"required\": false}, {\"key\": \"address\", \"type\": \"text\", \"group\": \"基本信息\", \"label\": \"学校地址\", \"system\": true, \"required\": false}, {\"key\": \"phone\", \"type\": \"text\", \"group\": \"基本信息\", \"label\": \"联系电话\", \"system\": true, \"required\": false}]}','{}','{\"icon\": \"building-2\", \"color\": \"#111827\"}',1,'com.school.management.infrastructure.extension.plugins.education.SchoolPlugin','PLUGIN:EDU@1.0.0',0,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,NULL,'EDU'),(89,1,'ORG_UNIT','TEACHING_GROUP','教研组',NULL,NULL,NULL,NULL,'[]',NULL,NULL,NULL,NULL,NULL,'{\"fields\": []}','{}','{\"icon\": \"users-round\", \"color\": \"#16a34a\"}',1,'com.school.management.infrastructure.extension.plugins.education.TeachingGroupPlugin','PLUGIN:EDU@1.0.0',0,1,0,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,NULL,'EDU');
/*!40000 ALTER TABLE `entity_type_configs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `evaluation_indicators`
--

DROP TABLE IF EXISTS `evaluation_indicators`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `evaluation_indicators` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `evaluation_id` bigint NOT NULL,
  `indicator_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '指标名(如: 教学态度)',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `weight` int NOT NULL DEFAULT '100' COMMENT '权重(0-100)',
  `max_score` int NOT NULL DEFAULT '5' COMMENT '满分(默认 5 分制)',
  `sort_order` int NOT NULL DEFAULT '0',
  `required` tinyint NOT NULL DEFAULT '1' COMMENT '是否必填',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_eval` (`evaluation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评教指标项';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `evaluation_indicators`
--

LOCK TABLES `evaluation_indicators` WRITE;
/*!40000 ALTER TABLE `evaluation_indicators` DISABLE KEYS */;
/*!40000 ALTER TABLE `evaluation_indicators` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `evaluation_responses`
--

DROP TABLE IF EXISTS `evaluation_responses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `evaluation_responses` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `evaluation_id` bigint NOT NULL,
  `task_id` bigint NOT NULL COMMENT '被评教学任务',
  `teacher_id` bigint NOT NULL COMMENT '被评教师',
  `student_id` bigint NOT NULL COMMENT '提交学生',
  `org_unit_id` bigint DEFAULT NULL COMMENT '学生班级(数据权限)',
  `total_score` decimal(5,2) DEFAULT NULL COMMENT '加权总分',
  `scores_json` json DEFAULT NULL COMMENT '各指标打分: [{indicatorId, score}]',
  `comment` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '主观评语',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0未提交 1已提交',
  `submitted_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_eval_task_student` (`evaluation_id`,`task_id`,`student_id`),
  KEY `idx_teacher` (`teacher_id`),
  KEY `idx_student` (`student_id`),
  KEY `idx_org_unit` (`org_unit_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评教提交';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `evaluation_responses`
--

LOCK TABLES `evaluation_responses` WRITE;
/*!40000 ALTER TABLE `evaluation_responses` DISABLE KEYS */;
/*!40000 ALTER TABLE `evaluation_responses` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `evaluation_result_history`
--

DROP TABLE IF EXISTS `evaluation_result_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `evaluation_result_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `original_id` bigint NOT NULL COMMENT 'Original Result ID',
  `record_id` bigint NOT NULL COMMENT 'Check Record ID',
  `entity_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Entity Type (CLASS/DORMITORY/STUDENT)',
  `entity_id` bigint NOT NULL COMMENT 'Entity ID',
  `entity_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Entity Name',
  `base_score` decimal(10,2) DEFAULT '100.00' COMMENT 'Base Score',
  `deduction_score` decimal(10,2) DEFAULT '0.00' COMMENT 'Deduction Score',
  `final_score` decimal(10,2) DEFAULT '100.00' COMMENT 'Final Score',
  `weighted_score` decimal(10,2) DEFAULT NULL COMMENT 'Weighted Score',
  `rating` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Rating (A/B/C/D/E)',
  `snapshot_reason` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Snapshot Reason',
  `snapshot_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Snapshot Time',
  `snapshot_by` bigint DEFAULT NULL COMMENT 'Snapshot By User ID',
  PRIMARY KEY (`id`),
  KEY `idx_original` (`original_id`),
  KEY `idx_record` (`record_id`),
  KEY `idx_entity` (`entity_type`,`entity_id`),
  KEY `idx_snapshot_at` (`snapshot_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Evaluation Result History';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `evaluation_result_history`
--

LOCK TABLES `evaluation_result_history` WRITE;
/*!40000 ALTER TABLE `evaluation_result_history` DISABLE KEYS */;
/*!40000 ALTER TABLE `evaluation_result_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `event_publications`
--

DROP TABLE IF EXISTS `event_publications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `event_publications` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `event_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Event ID (FK to domain_events)',
  `event_type` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Event Type',
  `status` enum('PENDING','PUBLISHED','FAILED') COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING' COMMENT 'Publication Status',
  `retry_count` int DEFAULT '0' COMMENT 'Retry Count',
  `last_error` text COLLATE utf8mb4_unicode_ci COMMENT 'Last Error Message',
  `target_channel` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Target Channel/Queue',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',
  `published_at` datetime DEFAULT NULL COMMENT 'Published At',
  `next_retry_at` datetime DEFAULT NULL COMMENT 'Next Retry Time',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_id` (`event_id`),
  KEY `idx_status` (`status`),
  KEY `idx_next_retry` (`status`,`next_retry_at`),
  KEY `idx_tenant` (`tenant_id`),
  KEY `idx_evt_pub_pending` (`status`,`next_retry_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Event Publications Outbox';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `event_publications`
--

LOCK TABLES `event_publications` WRITE;
/*!40000 ALTER TABLE `event_publications` DISABLE KEYS */;
/*!40000 ALTER TABLE `event_publications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `event_schemas`
--

DROP TABLE IF EXISTS `event_schemas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `event_schemas` (
  `event_type` varchar(50) NOT NULL,
  `event_category` varchar(30) DEFAULT NULL,
  `required_payload_keys` json NOT NULL COMMENT '["batchId","createdBy"]',
  `optional_payload_keys` json DEFAULT NULL,
  `description` text,
  `registered_by` varchar(100) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`event_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='事件契约注册';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `event_schemas`
--

LOCK TABLES `event_schemas` WRITE;
/*!40000 ALTER TABLE `event_schemas` DISABLE KEYS */;
INSERT INTO `event_schemas` VALUES ('ATTENDANCE_RECORDED','TEACHING','[\"studentId\", \"classId\", \"status\"]',NULL,'考勤记录','EducationPlugin','2026-06-01 22:57:30'),('EXAM_PUBLISHED','TEACHING','[\"examId\", \"orgUnitId\"]',NULL,'考试发布','EducationPlugin','2026-06-01 22:57:30'),('GRADE_APPROVED','TEACHING','[\"batchId\", \"courseId\", \"createdBy\", \"approvedBy\"]',NULL,'成绩审核通过','EducationPlugin','2026-06-01 22:57:30'),('GRADE_PUBLISHED','TEACHING','[\"batchId\", \"courseId\", \"orgUnitId\"]',NULL,'成绩发布','EducationPlugin','2026-06-01 22:57:30'),('GRADE_SUBMITTED','TEACHING','[\"batchId\", \"courseId\", \"createdBy\"]',NULL,'成绩提交,主体=提交教师','EducationPlugin','2026-06-01 22:57:30'),('INSP_ITEM_RESULT','INSPECTION','[\"studentId\", \"classId\", \"itemId\"]',NULL,'检查项结果','InspectionPlugin','2026-06-01 22:57:30');
/*!40000 ALTER TABLE `event_schemas` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `event_triggers`
--

DROP TABLE IF EXISTS `event_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `event_triggers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '触发器名称',
  `trigger_point_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '关联触发点编码',
  `condition_json` json DEFAULT NULL COMMENT '触发条件',
  `event_type_mode` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'FIXED' COMMENT 'FIXED=固定类型/DYNAMIC=从context取',
  `event_type_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '固定事件类型编码(FIXED模式)',
  `event_type_source` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'context字段名(DYNAMIC模式)',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_enabled` tinyint DEFAULT '1',
  `sort_order` int DEFAULT '0',
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  `industry` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属行业包',
  `plugin_class` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '声明插件全限定类名',
  `origin` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '统一来源: PLUGIN:<code>@<ver> / TENANT:CUSTOM#<id>',
  `subjects_json` json DEFAULT NULL COMMENT '主体配置数组 [{type,idSource,nameSource}]',
  `plugin_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '插件级启用状态',
  PRIMARY KEY (`id`),
  KEY `idx_point` (`trigger_point_code`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='事件触发器配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `event_triggers`
--

LOCK TABLES `event_triggers` WRITE;
/*!40000 ALTER TABLE `event_triggers` DISABLE KEYS */;
INSERT INTO `event_triggers` VALUES (1,'检查扣分→学生+班级事件','INSP_ITEM_RESULT','{\"isNegative\": true}','DYNAMIC',NULL,'eventTypeHint','检查项扣分时同时记录到学生和班级',1,0,1,NULL,'2026-06-01 22:56:48','2026-06-01 22:56:49',0,NULL,NULL,NULL,'[{\"type\": \"USER\", \"idSource\": \"studentId\", \"nameSource\": \"studentName\"}, {\"type\": \"ORG_UNIT\", \"idSource\": \"classId\", \"nameSource\": \"className\"}]',1),(2,'检查优秀→班级表扬','INSP_GRADE_RESULT','{\"grade\": {\"$in\": [\"A\"]}}','FIXED','DISCIPLINE_EXCELLENT',NULL,'检查等级为A时给目标(班级)记录纪律优秀',1,0,1,NULL,'2026-06-01 22:56:48','2026-06-01 22:56:48',0,NULL,NULL,NULL,'[{\"type\": \"ORG_UNIT\", \"idSource\": \"targetId\", \"nameSource\": \"targetName\"}]',1),(3,'检查不合格→班级警告','INSP_GRADE_RESULT','{\"grade\": {\"$in\": [\"D\"]}}','FIXED','DISCIPLINE_FAIL',NULL,'检查等级为D时给目标(班级)记录纪律不合格',1,0,1,NULL,'2026-06-01 22:56:48','2026-06-01 22:56:48',0,NULL,NULL,NULL,'[{\"type\": \"ORG_UNIT\", \"idSource\": \"targetId\", \"nameSource\": \"targetName\"}]',1),(4,'考勤异常→学生+班级事件','ATTENDANCE_RECORDED','{\"status\": {\"$in\": [2, 3, 5]}}','DYNAMIC',NULL,'eventTypeHint','迟到/早退/旷课时同时记录到学生和班级',1,0,1,NULL,'2026-06-01 22:56:48','2026-06-01 22:56:48',0,NULL,NULL,NULL,'[{\"type\": \"USER\", \"idSource\": \"studentId\", \"nameSource\": \"studentName\"}, {\"type\": \"ORG_UNIT\", \"idSource\": \"classId\", \"nameSource\": \"className\"}]',1),(5,'成绩提交通知','GRADE_SUBMITTED',NULL,'FIXED','GRADE_SUBMITTED',NULL,NULL,1,10,1,NULL,'2026-06-01 22:57:28','2026-06-01 22:57:28',0,NULL,NULL,NULL,'[{\"type\": \"USER\", \"idSource\": \"createdBy\", \"nameSource\": \"batchName\"}]',1),(6,'成绩审核通过通知','GRADE_APPROVED',NULL,'FIXED','GRADE_APPROVED',NULL,NULL,1,11,1,NULL,'2026-06-01 22:57:28','2026-06-01 22:57:28',0,NULL,NULL,NULL,'[{\"type\": \"USER\", \"idSource\": \"createdBy\", \"nameSource\": \"batchName\"}]',1),(7,'成绩发布通知','GRADE_PUBLISHED',NULL,'FIXED','GRADE_PUBLISHED',NULL,NULL,1,12,1,NULL,'2026-06-01 22:57:28','2026-06-01 22:57:28',0,NULL,NULL,NULL,'[{\"type\": \"ORG_UNIT\", \"idSource\": \"orgUnitId\", \"nameSource\": \"batchName\"}]',1),(8,'考试发布通知','EXAM_PUBLISHED',NULL,'FIXED','EXAM_PUBLISHED',NULL,NULL,1,13,1,NULL,'2026-06-01 22:57:28','2026-06-01 22:57:28',0,NULL,NULL,NULL,'[{\"type\": \"ORG_UNIT\", \"idSource\": \"orgUnitId\", \"nameSource\": \"batchName\"}]',1),(9,'成绩个人通知','GRADE_PUBLISHED_PERSONAL',NULL,'FIXED','GRADE_PUBLISHED_PERSONAL',NULL,'成绩发布 → 学生 + 家长',1,13,1,NULL,'2026-06-01 22:57:33','2026-06-01 22:57:33',0,NULL,NULL,NULL,'[{\"type\": \"USER\", \"idSource\": \"studentId\", \"nameSource\": \"studentName\"}]',1),(10,'成绩提交通知','GRADE_SUBMITTED',NULL,'FIXED','GRADE_SUBMITTED',NULL,NULL,1,10,1,NULL,'2026-06-01 23:06:11','2026-06-01 23:06:11',0,NULL,NULL,NULL,'[{\"type\": \"USER\", \"idSource\": \"createdBy\", \"nameSource\": \"batchName\"}]',1),(11,'成绩审核通过通知','GRADE_APPROVED',NULL,'FIXED','GRADE_APPROVED',NULL,NULL,1,11,1,NULL,'2026-06-01 23:06:11','2026-06-01 23:06:11',0,NULL,NULL,NULL,'[{\"type\": \"USER\", \"idSource\": \"createdBy\", \"nameSource\": \"batchName\"}]',1),(12,'成绩发布通知','GRADE_PUBLISHED',NULL,'FIXED','GRADE_PUBLISHED',NULL,NULL,1,12,1,NULL,'2026-06-01 23:06:11','2026-06-01 23:06:11',0,NULL,NULL,NULL,'[{\"type\": \"ORG_UNIT\", \"idSource\": \"orgUnitId\", \"nameSource\": \"batchName\"}]',1),(13,'考试发布通知','EXAM_PUBLISHED',NULL,'FIXED','EXAM_PUBLISHED',NULL,NULL,1,13,1,NULL,'2026-06-01 23:06:11','2026-06-01 23:06:11',0,NULL,NULL,NULL,'[{\"type\": \"ORG_UNIT\", \"idSource\": \"orgUnitId\", \"nameSource\": \"batchName\"}]',1),(14,'成绩个人通知','GRADE_PUBLISHED_PERSONAL',NULL,'FIXED','GRADE_PUBLISHED_PERSONAL',NULL,'成绩发布 → 学生 + 家长',1,13,1,NULL,'2026-06-01 23:06:14','2026-06-01 23:06:14',0,NULL,NULL,NULL,'[{\"type\": \"USER\", \"idSource\": \"studentId\", \"nameSource\": \"studentName\"}]',1),(15,'默认: GRADE_SUBMITTED → GRADE_SUBMITTED_EVT','GRADE_SUBMITTED',NULL,'FIXED','GRADE_SUBMITTED_EVT',NULL,'由插件 com.school.management.infrastructure.extension.plugins.education.messaging.GradeMessagingPlugin 注册的默认触发器',1,0,1,NULL,'2026-06-01 23:32:06','2026-06-01 23:32:06',0,'EDU','com.school.management.infrastructure.extension.plugins.education.messaging.GradeMessagingPlugin','PLUGIN:EDU@1.0.0',NULL,1),(16,'默认: GRADE_APPROVED → GRADE_APPROVED_EVT','GRADE_APPROVED',NULL,'FIXED','GRADE_APPROVED_EVT',NULL,'由插件 com.school.management.infrastructure.extension.plugins.education.messaging.GradeMessagingPlugin 注册的默认触发器',1,0,1,NULL,'2026-06-01 23:32:06','2026-06-01 23:32:06',0,'EDU','com.school.management.infrastructure.extension.plugins.education.messaging.GradeMessagingPlugin','PLUGIN:EDU@1.0.0',NULL,1),(17,'默认: GRADE_PUBLISHED → GRADE_PUBLISHED_EVT','GRADE_PUBLISHED',NULL,'FIXED','GRADE_PUBLISHED_EVT',NULL,'由插件 com.school.management.infrastructure.extension.plugins.education.messaging.GradeMessagingPlugin 注册的默认触发器',1,0,1,NULL,'2026-06-01 23:32:06','2026-06-01 23:32:06',0,'EDU','com.school.management.infrastructure.extension.plugins.education.messaging.GradeMessagingPlugin','PLUGIN:EDU@1.0.0',NULL,1),(18,'默认: GRADE_PUBLISHED_PERSONAL → GRADE_PUBLISHED_PERSONAL_EVT','GRADE_PUBLISHED_PERSONAL',NULL,'FIXED','GRADE_PUBLISHED_PERSONAL_EVT',NULL,'由插件 com.school.management.infrastructure.extension.plugins.education.messaging.GradeMessagingPlugin 注册的默认触发器',1,0,1,NULL,'2026-06-01 23:32:06','2026-06-01 23:32:06',0,'EDU','com.school.management.infrastructure.extension.plugins.education.messaging.GradeMessagingPlugin','PLUGIN:EDU@1.0.0',NULL,1);
/*!40000 ALTER TABLE `event_triggers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `exam_arrangements`
--

DROP TABLE IF EXISTS `exam_arrangements`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_arrangements` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `batch_id` bigint NOT NULL COMMENT '考试批次ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `exam_date` date NOT NULL COMMENT '考试日期',
  `start_time` time NOT NULL COMMENT '开始时间',
  `end_time` time NOT NULL COMMENT '结束时间',
  `duration` int DEFAULT NULL COMMENT '考试时长（分钟）',
  `exam_form` tinyint DEFAULT '1' COMMENT '考试形式：1闭卷 2开卷 3机考 4口试 5实操',
  `total_students` int DEFAULT '0' COMMENT '应考人数',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `status` tinyint DEFAULT '1' COMMENT '状态：1正常 0取消',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `task_id` bigint DEFAULT NULL COMMENT '关联教学任务ID',
  `class_id` bigint DEFAULT NULL COMMENT '关联班级ID',
  `org_unit_id` bigint DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_batch` (`batch_id`),
  KEY `idx_course` (`course_id`),
  KEY `idx_date` (`exam_date`),
  KEY `idx_task` (`task_id`),
  KEY `idx_tenant` (`tenant_id`),
  CONSTRAINT `fk_arrangement_batch` FOREIGN KEY (`batch_id`) REFERENCES `exam_batches` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_arrangement_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='考试安排表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `exam_arrangements`
--

LOCK TABLES `exam_arrangements` WRITE;
/*!40000 ALTER TABLE `exam_arrangements` DISABLE KEYS */;
/*!40000 ALTER TABLE `exam_arrangements` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `exam_batches`
--

DROP TABLE IF EXISTS `exam_batches`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_batches` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `batch_code` varchar(50) NOT NULL COMMENT '批次代码',
  `batch_name` varchar(100) NOT NULL COMMENT '批次名称，如"2025-2026-1期末考试"',
  `semester_id` bigint NOT NULL COMMENT '学期ID',
  `exam_type` tinyint NOT NULL COMMENT '考试类型：1期中 2期末 3补考 4重修',
  `start_date` date NOT NULL COMMENT '考试开始日期',
  `end_date` date NOT NULL COMMENT '考试结束日期',
  `registration_deadline` datetime DEFAULT NULL COMMENT '报名截止时间（补考/重修用）',
  `status` tinyint DEFAULT '0' COMMENT '状态：0筹备中 1报名中 2已安排 3进行中 4已结束',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_code` (`batch_code`),
  KEY `idx_semester` (`semester_id`),
  KEY `idx_tenant` (`tenant_id`),
  CONSTRAINT `fk_exambatch_semester` FOREIGN KEY (`semester_id`) REFERENCES `semesters` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='考试批次表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `exam_batches`
--

LOCK TABLES `exam_batches` WRITE;
/*!40000 ALTER TABLE `exam_batches` DISABLE KEYS */;
/*!40000 ALTER TABLE `exam_batches` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `exam_invigilators`
--

DROP TABLE IF EXISTS `exam_invigilators`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_invigilators` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `room_id` bigint NOT NULL COMMENT '考场ID',
  `teacher_id` bigint NOT NULL COMMENT '监考教师ID',
  `invigilator_role` tinyint DEFAULT '1' COMMENT '监考角色：1主监考 2副监考 3巡考',
  `status` tinyint DEFAULT '1' COMMENT '状态：1正常 0取消',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_teacher` (`room_id`,`teacher_id`),
  KEY `idx_teacher` (`teacher_id`),
  KEY `idx_tenant` (`tenant_id`),
  CONSTRAINT `fk_invigilator_room` FOREIGN KEY (`room_id`) REFERENCES `exam_rooms` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='监考安排表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `exam_invigilators`
--

LOCK TABLES `exam_invigilators` WRITE;
/*!40000 ALTER TABLE `exam_invigilators` DISABLE KEYS */;
/*!40000 ALTER TABLE `exam_invigilators` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `exam_rooms`
--

DROP TABLE IF EXISTS `exam_rooms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_rooms` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `arrangement_id` bigint NOT NULL COMMENT '考试安排ID',
  `classroom_id` bigint NOT NULL COMMENT '教室ID',
  `room_code` varchar(20) DEFAULT NULL COMMENT '考场编号',
  `seat_count` int NOT NULL COMMENT '座位数',
  `student_count` int DEFAULT '0' COMMENT '安排考生数',
  `seat_layout` json DEFAULT NULL COMMENT '座位安排（学生ID与座位号映射）',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_arrangement` (`arrangement_id`),
  KEY `idx_classroom` (`classroom_id`),
  KEY `idx_tenant` (`tenant_id`),
  CONSTRAINT `fk_room_arrangement` FOREIGN KEY (`arrangement_id`) REFERENCES `exam_arrangements` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='考场安排表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `exam_rooms`
--

LOCK TABLES `exam_rooms` WRITE;
/*!40000 ALTER TABLE `exam_rooms` DISABLE KEYS */;
/*!40000 ALTER TABLE `exam_rooms` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `exam_students`
--

DROP TABLE IF EXISTS `exam_students`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_students` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `arrangement_id` bigint NOT NULL COMMENT '考试安排ID',
  `room_id` bigint DEFAULT NULL COMMENT '考场ID',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `seat_number` varchar(20) DEFAULT NULL COMMENT '座位号',
  `exam_status` tinyint DEFAULT '0' COMMENT '考试状态：0待考 1已考 2缺考 3作弊',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注（如缺考原因）',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_arrangement_student` (`arrangement_id`,`student_id`),
  KEY `idx_student` (`student_id`),
  KEY `idx_room` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='考生安排表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `exam_students`
--

LOCK TABLES `exam_students` WRITE;
/*!40000 ALTER TABLE `exam_students` DISABLE KEYS */;
/*!40000 ALTER TABLE `exam_students` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `export_tasks`
--

DROP TABLE IF EXISTS `export_tasks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `export_tasks` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务编号',
  `export_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '导出类型：realtime_deduction/rating_report/statistics',
  `export_format` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'EXCEL' COMMENT '导出格式：EXCEL/WORD/PDF',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/PROCESSING/COMPLETED/FAILED/CANCELLED',
  `progress` int DEFAULT '0' COMMENT '进度百分比(0-100)',
  `total_count` int DEFAULT '0' COMMENT '总记录数',
  `processed_count` int DEFAULT '0' COMMENT '已处理记录数',
  `file_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件名',
  `file_path` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件存储路径',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小(字节)',
  `download_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '下载链接',
  `expire_time` datetime DEFAULT NULL COMMENT '链接过期时间',
  `request_data` text COLLATE utf8mb4_unicode_ci COMMENT '请求参数(JSON)',
  `error_message` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '错误信息',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_task_code` (`task_code`),
  KEY `idx_created_by` (`created_by`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='导出任务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `export_tasks`
--

LOCK TABLES `export_tasks` WRITE;
/*!40000 ALTER TABLE `export_tasks` DISABLE KEYS */;
/*!40000 ALTER TABLE `export_tasks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flw_channel_definition`
--

DROP TABLE IF EXISTS `flw_channel_definition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flw_channel_definition` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `VERSION_` int DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `RESOURCE_NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DESCRIPTION_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `IMPLEMENTATION_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_IDX_CHANNEL_DEF_UNIQ` (`KEY_`,`VERSION_`,`TENANT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flw_channel_definition`
--

LOCK TABLES `flw_channel_definition` WRITE;
/*!40000 ALTER TABLE `flw_channel_definition` DISABLE KEYS */;
/*!40000 ALTER TABLE `flw_channel_definition` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flw_ev_databasechangelog`
--

DROP TABLE IF EXISTS `flw_ev_databasechangelog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flw_ev_databasechangelog` (
  `ID` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `AUTHOR` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `FILENAME` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `DATEEXECUTED` datetime NOT NULL,
  `ORDEREXECUTED` int NOT NULL,
  `EXECTYPE` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  `MD5SUM` varchar(35) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DESCRIPTION` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `COMMENTS` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TAG` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `LIQUIBASE` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CONTEXTS` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `LABELS` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOYMENT_ID` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flw_ev_databasechangelog`
--

LOCK TABLES `flw_ev_databasechangelog` WRITE;
/*!40000 ALTER TABLE `flw_ev_databasechangelog` DISABLE KEYS */;
INSERT INTO `flw_ev_databasechangelog` VALUES ('1','flowable','org/flowable/eventregistry/db/liquibase/flowable-eventregistry-db-changelog.xml','2026-06-01 23:28:32',1,'EXECUTED','9:63268f536c469325acef35970312551b','createTable tableName=FLW_EVENT_DEPLOYMENT; createTable tableName=FLW_EVENT_RESOURCE; createTable tableName=FLW_EVENT_DEFINITION; createIndex indexName=ACT_IDX_EVENT_DEF_UNIQ, tableName=FLW_EVENT_DEFINITION; createTable tableName=FLW_CHANNEL_DEFIN...','',NULL,'4.24.0',NULL,NULL,'0327712058'),('2','flowable','org/flowable/eventregistry/db/liquibase/flowable-eventregistry-db-changelog.xml','2026-06-01 23:28:32',2,'EXECUTED','9:dcb58b7dfd6dbda66939123a96985536','addColumn tableName=FLW_CHANNEL_DEFINITION; addColumn tableName=FLW_CHANNEL_DEFINITION','',NULL,'4.24.0',NULL,NULL,'0327712058'),('3','flowable','org/flowable/eventregistry/db/liquibase/flowable-eventregistry-db-changelog.xml','2026-06-01 23:28:32',3,'EXECUTED','9:d0c05678d57af23ad93699991e3bf4f6','customChange','',NULL,'4.24.0',NULL,NULL,'0327712058');
/*!40000 ALTER TABLE `flw_ev_databasechangelog` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flw_ev_databasechangeloglock`
--

DROP TABLE IF EXISTS `flw_ev_databasechangeloglock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flw_ev_databasechangeloglock` (
  `ID` int NOT NULL,
  `LOCKED` tinyint(1) NOT NULL,
  `LOCKGRANTED` datetime DEFAULT NULL,
  `LOCKEDBY` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flw_ev_databasechangeloglock`
--

LOCK TABLES `flw_ev_databasechangeloglock` WRITE;
/*!40000 ALTER TABLE `flw_ev_databasechangeloglock` DISABLE KEYS */;
INSERT INTO `flw_ev_databasechangeloglock` VALUES (1,0,NULL,NULL);
/*!40000 ALTER TABLE `flw_ev_databasechangeloglock` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flw_event_definition`
--

DROP TABLE IF EXISTS `flw_event_definition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flw_event_definition` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `VERSION_` int DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `RESOURCE_NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DESCRIPTION_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_IDX_EVENT_DEF_UNIQ` (`KEY_`,`VERSION_`,`TENANT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flw_event_definition`
--

LOCK TABLES `flw_event_definition` WRITE;
/*!40000 ALTER TABLE `flw_event_definition` DISABLE KEYS */;
/*!40000 ALTER TABLE `flw_event_definition` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flw_event_deployment`
--

DROP TABLE IF EXISTS `flw_event_deployment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flw_event_deployment` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOY_TIME_` datetime(3) DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `PARENT_DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flw_event_deployment`
--

LOCK TABLES `flw_event_deployment` WRITE;
/*!40000 ALTER TABLE `flw_event_deployment` DISABLE KEYS */;
/*!40000 ALTER TABLE `flw_event_deployment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flw_event_resource`
--

DROP TABLE IF EXISTS `flw_event_resource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flw_event_resource` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `RESOURCE_BYTES_` longblob,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flw_event_resource`
--

LOCK TABLES `flw_event_resource` WRITE;
/*!40000 ALTER TABLE `flw_event_resource` DISABLE KEYS */;
/*!40000 ALTER TABLE `flw_event_resource` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flw_ru_batch`
--

DROP TABLE IF EXISTS `flw_ru_batch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flw_ru_batch` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `TYPE_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `SEARCH_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SEARCH_KEY2_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) NOT NULL,
  `COMPLETE_TIME_` datetime(3) DEFAULT NULL,
  `STATUS_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BATCH_DOC_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flw_ru_batch`
--

LOCK TABLES `flw_ru_batch` WRITE;
/*!40000 ALTER TABLE `flw_ru_batch` DISABLE KEYS */;
/*!40000 ALTER TABLE `flw_ru_batch` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flw_ru_batch_part`
--

DROP TABLE IF EXISTS `flw_ru_batch_part`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flw_ru_batch_part` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `BATCH_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `SCOPE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SEARCH_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SEARCH_KEY2_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) NOT NULL,
  `COMPLETE_TIME_` datetime(3) DEFAULT NULL,
  `STATUS_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RESULT_DOC_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `FLW_IDX_BATCH_PART` (`BATCH_ID_`),
  CONSTRAINT `FLW_FK_BATCH_PART_PARENT` FOREIGN KEY (`BATCH_ID_`) REFERENCES `flw_ru_batch` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flw_ru_batch_part`
--

LOCK TABLES `flw_ru_batch_part` WRITE;
/*!40000 ALTER TABLE `flw_ru_batch_part` DISABLE KEYS */;
/*!40000 ALTER TABLE `flw_ru_batch_part` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `formula_functions`
--

DROP TABLE IF EXISTS `formula_functions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `formula_functions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '函数名',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '函数描述',
  `category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分类: math/array/logic/date/string',
  `parameters_def` json DEFAULT NULL COMMENT '参数定义: [{name, type, required, description}]',
  `return_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '返回类型',
  `implementation` text COLLATE utf8mb4_unicode_ci COMMENT 'JavaScript实现代码',
  `examples` json DEFAULT NULL COMMENT '使用示例: [{input, output, description}]',
  `is_system` tinyint(1) DEFAULT '1',
  `is_enabled` tinyint(1) DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `deleted` int DEFAULT '0',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  KEY `idx_formula_func_category` (`category`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公式内置函数表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `formula_functions`
--

LOCK TABLES `formula_functions` WRITE;
/*!40000 ALTER TABLE `formula_functions` DISABLE KEYS */;
INSERT INTO `formula_functions` VALUES (1,'sum','求和','math','[{\"name\": \"values\", \"type\": \"array\", \"required\": true, \"description\": \"数值数组\"}]','number','function sum(arr) { return Array.isArray(arr) ? arr.reduce((a, b) => a + (Number(b) || 0), 0) : 0; }','[{\"input\": \"sum([1, 2, 3])\", \"output\": \"6\"}]',1,1,'2026-06-01 22:57:01',0,1),(2,'average','平均值','math','[{\"name\": \"values\", \"type\": \"array\", \"required\": true}]','number','function average(arr) { return arr && arr.length ? arr.reduce((a, b) => a + b, 0) / arr.length : 0; }','[{\"input\": \"average([60, 70, 80])\", \"output\": \"70\"}]',1,1,'2026-06-01 22:57:01',0,1),(3,'max','最大值','math','[{\"name\": \"values\", \"type\": \"array\", \"required\": true}]','number','function max(arr) { return arr && arr.length ? Math.max(...arr) : 0; }','[{\"input\": \"max([1, 5, 3])\", \"output\": \"5\"}]',1,1,'2026-06-01 22:57:01',0,1),(4,'min','最小值','math','[{\"name\": \"values\", \"type\": \"array\", \"required\": true}]','number','function min(arr) { return arr && arr.length ? Math.min(...arr) : 0; }','[{\"input\": \"min([1, 5, 3])\", \"output\": \"1\"}]',1,1,'2026-06-01 22:57:01',0,1),(5,'clamp','限制范围','math','[{\"name\": \"value\", \"type\": \"number\"}, {\"name\": \"min\", \"type\": \"number\"}, {\"name\": \"max\", \"type\": \"number\"}]','number','function clamp(value, min, max) { return Math.min(Math.max(value || 0, min || 0), max || 100); }','[{\"input\": \"clamp(150, 0, 100)\", \"output\": \"100\"}]',1,1,'2026-06-01 22:57:01',0,1),(6,'round','四舍五入','math','[{\"name\": \"value\", \"type\": \"number\"}, {\"name\": \"decimals\", \"type\": \"number\", \"default\": 0}]','number','function round(value, decimals) { const m = Math.pow(10, decimals || 0); return Math.round((value || 0) * m) / m; }','[{\"input\": \"round(3.456, 2)\", \"output\": \"3.46\"}]',1,1,'2026-06-01 22:57:01',0,1),(7,'count','计数','array','[{\"name\": \"values\", \"type\": \"array\"}]','number','function count(arr) { return Array.isArray(arr) ? arr.length : 0; }','[{\"input\": \"count([1, 2, 3])\", \"output\": \"3\"}]',1,1,'2026-06-01 22:57:01',0,1),(8,'filter','过滤','array','[{\"name\": \"values\", \"type\": \"array\"}, {\"name\": \"condition\", \"type\": \"function\"}]','array','function filter(arr, fn) { return Array.isArray(arr) ? arr.filter(fn) : []; }','[{\"input\": \"filter([1, 2, 3], x => x > 1)\", \"output\": \"[2, 3]\"}]',1,1,'2026-06-01 22:57:01',0,1),(9,'iif','条件判断','logic','[{\"name\": \"condition\", \"type\": \"boolean\"}, {\"name\": \"trueValue\", \"type\": \"any\"}, {\"name\": \"falseValue\", \"type\": \"any\"}]','any','function iif(cond, t, f) { return cond ? t : f; }','[{\"input\": \"iif(true, 100, 0)\", \"output\": \"100\"}]',1,1,'2026-06-01 22:57:01',0,1),(10,'lookupStep','阶梯查找','lookup','[{\"name\": \"value\", \"type\": \"number\"}, {\"name\": \"steps\", \"type\": \"array\"}]','number','function lookupStep(value, steps) { if (!steps || !steps.length) return 0; const sorted = [...steps].sort((a,b) => b.threshold - a.threshold); for (let s of sorted) { if (value >= s.threshold) return s.score; } return 0; }','[{\"input\": \"lookupStep(85, [{threshold: 90, score: 100}, {threshold: 80, score: 90}])\", \"output\": \"90\"}]',1,1,'2026-06-01 22:57:01',0,1),(11,'weightedAverage','加权平均','math','[{\"name\": \"values\", \"type\": \"array\"}, {\"name\": \"weights\", \"type\": \"array\"}]','number','function weightedAverage(values, weights) { if (!values || !values.length) return 0; let sum = 0, wSum = 0; for (let i = 0; i < values.length; i++) { const w = weights && weights[i] ? weights[i] : 1; sum += values[i] * w; wSum += w; } return wSum ? sum / wSum : 0; }','[{\"input\": \"weightedAverage([80, 90], [0.4, 0.6])\", \"output\": \"86\"}]',1,1,'2026-06-01 22:57:01',0,1);
/*!40000 ALTER TABLE `formula_functions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `formula_variables`
--

DROP TABLE IF EXISTS `formula_variables`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `formula_variables` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '变量名',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '变量描述',
  `category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分类: score/count/time/context',
  `value_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '值类型: number/string/boolean/array/object',
  `default_value` text COLLATE utf8mb4_unicode_ci COMMENT '默认值',
  `source_description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '值来源说明',
  `is_system` tinyint(1) DEFAULT '1',
  `is_enabled` tinyint(1) DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `deleted` int DEFAULT '0',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  KEY `idx_formula_var_category` (`category`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=47 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公式内置变量表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `formula_variables`
--

LOCK TABLES `formula_variables` WRITE;
/*!40000 ALTER TABLE `formula_variables` DISABLE KEYS */;
INSERT INTO `formula_variables` VALUES (1,'baseScore','基准分','score','number',NULL,'模板设置的基准分数',1,1,'2026-06-01 22:57:01',0,1),(2,'maxScore','最高分','score','number',NULL,'允许的最高分数',1,1,'2026-06-01 22:57:01',0,1),(3,'minScore','最低分','score','number',NULL,'允许的最低分数',1,1,'2026-06-01 22:57:01',0,1),(4,'score','当前得分','score','number',NULL,'计算中的当前分数',1,1,'2026-06-01 22:57:01',0,1),(5,'deductions','扣分列表','score','array',NULL,'所有扣分项的分值数组',1,1,'2026-06-01 22:57:01',0,1),(6,'additions','加分列表','score','array',NULL,'所有加分项的分值数组',1,1,'2026-06-01 22:57:01',0,1),(7,'totalCount','总项数','count','number',NULL,'检查项总数',1,1,'2026-06-01 22:57:01',0,1),(8,'completedCount','完成数','count','number',NULL,'已完成/已检查的项数',1,1,'2026-06-01 22:57:01',0,1),(9,'occurrenceCount','发生次数','count','number',NULL,'同一问题的发生次数',1,1,'2026-06-01 22:57:01',0,1),(10,'consecutiveCount','连续次数','count','number',NULL,'连续达标/违规的次数',1,1,'2026-06-01 22:57:01',0,1),(11,'value','输入值','input','any',NULL,'当前检查项的输入值',1,1,'2026-06-01 22:57:01',0,1),(12,'grade','等级','input','string',NULL,'等级评定值: A/B/C/D',1,1,'2026-06-01 22:57:01',0,1),(13,'stars','星级','input','number',NULL,'星级评分: 1-5',1,1,'2026-06-01 22:57:01',0,1),(14,'passed','是否合格','input','boolean',NULL,'合格判定结果',1,1,'2026-06-01 22:57:01',0,1),(15,'comment','评语','input','string',NULL,'评语: 优/良/中/差',1,1,'2026-06-01 22:57:01',0,1),(16,'currentPeriod','当前周期','time','string',NULL,'当前评分周期',1,1,'2026-06-01 22:57:01',0,1),(17,'periodScores','周期得分','time','array',NULL,'历史周期得分数组',1,1,'2026-06-01 22:57:01',0,1),(18,'previousScore','上期得分','time','number',NULL,'上一周期的得分',1,1,'2026-06-01 22:57:01',0,1),(19,'currentScore','本期得分','time','number',NULL,'当前周期的得分',1,1,'2026-06-01 22:57:01',0,1),(20,'isOverdue','是否超时','time','boolean',NULL,'是否超过截止时间',1,1,'2026-06-01 22:57:01',0,1),(21,'rank','排名','context','number',NULL,'当前排名位置',1,1,'2026-06-01 22:57:01',0,1),(22,'hasVetoItem','有否决项','context','boolean',NULL,'是否触发一票否决',1,1,'2026-06-01 22:57:01',0,1),(23,'multiplier','倍率','context','number',NULL,'当前适用的倍率',1,1,'2026-06-01 22:57:01',0,1);
/*!40000 ALTER TABLE `formula_variables` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `functional_dept_modules`
--

DROP TABLE IF EXISTS `functional_dept_modules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `functional_dept_modules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `org_unit_id` bigint NOT NULL COMMENT '职能部门ID',
  `module_code` varchar(50) NOT NULL COMMENT '管理的模块编码',
  `scope_type` varchar(20) DEFAULT 'all' COMMENT '管理范围: all-全校, custom-自定义',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dept_module` (`org_unit_id`,`module_code`),
  KEY `idx_tenant` (`tenant_id`),
  CONSTRAINT `functional_dept_modules_ibfk_1` FOREIGN KEY (`org_unit_id`) REFERENCES `org_units` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='职能部门管理模块配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `functional_dept_modules`
--

LOCK TABLES `functional_dept_modules` WRITE;
/*!40000 ALTER TABLE `functional_dept_modules` DISABLE KEYS */;
/*!40000 ALTER TABLE `functional_dept_modules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `grade_batches`
--

DROP TABLE IF EXISTS `grade_batches`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `grade_batches` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `batch_code` varchar(50) NOT NULL COMMENT '批次代码',
  `batch_name` varchar(100) NOT NULL COMMENT '批次名称',
  `semester_id` bigint NOT NULL COMMENT '学期ID',
  `course_id` bigint DEFAULT NULL,
  `org_unit_id` bigint DEFAULT NULL,
  `grade_type` tinyint NOT NULL COMMENT '成绩类型：1正常成绩 2补考成绩 3重修成绩',
  `start_time` datetime DEFAULT NULL COMMENT '录入开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '录入截止时间',
  `status` tinyint DEFAULT '0' COMMENT '状态：0未开始 1进行中 2已截止 3已归档',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `exam_batch_id` bigint DEFAULT NULL COMMENT '关联考试批次ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_code` (`batch_code`),
  KEY `idx_semester` (`semester_id`),
  KEY `idx_exam_batch` (`exam_batch_id`),
  KEY `idx_course` (`course_id`),
  KEY `idx_org_unit` (`org_unit_id`),
  KEY `idx_tenant` (`tenant_id`),
  CONSTRAINT `fk_gradebatch_semester` FOREIGN KEY (`semester_id`) REFERENCES `semesters` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成绩录入批次表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `grade_batches`
--

LOCK TABLES `grade_batches` WRITE;
/*!40000 ALTER TABLE `grade_batches` DISABLE KEYS */;
/*!40000 ALTER TABLE `grade_batches` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `grade_change_logs`
--

DROP TABLE IF EXISTS `grade_change_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `grade_change_logs` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `grade_id` bigint NOT NULL COMMENT '学生成绩ID',
  `change_type` tinyint NOT NULL COMMENT '修改类型：1首次录入 2修改 3删除 4恢复',
  `field_name` varchar(50) DEFAULT NULL COMMENT '修改字段',
  `old_value` varchar(100) DEFAULT NULL COMMENT '原值',
  `new_value` varchar(100) DEFAULT NULL COMMENT '新值',
  `change_reason` varchar(500) DEFAULT NULL COMMENT '修改原因',
  `operator_id` bigint NOT NULL COMMENT '操作人ID',
  `operated_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_grade` (`grade_id`),
  KEY `idx_operator` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成绩修改记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `grade_change_logs`
--

LOCK TABLES `grade_change_logs` WRITE;
/*!40000 ALTER TABLE `grade_change_logs` DISABLE KEYS */;
/*!40000 ALTER TABLE `grade_change_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `grade_compositions`
--

DROP TABLE IF EXISTS `grade_compositions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `grade_compositions` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `task_id` bigint NOT NULL COMMENT '教学任务ID',
  `item_name` varchar(50) NOT NULL COMMENT '成绩项名称，如"平时成绩"',
  `item_type` tinyint NOT NULL COMMENT '成绩项类型：1平时 2期中 3期末 4实验 5实践 6其他',
  `weight` decimal(5,2) NOT NULL COMMENT '权重（百分比）',
  `full_score` decimal(5,1) DEFAULT '100.0' COMMENT '满分',
  `is_required` tinyint DEFAULT '1' COMMENT '是否必录',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成绩组成配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `grade_compositions`
--

LOCK TABLES `grade_compositions` WRITE;
/*!40000 ALTER TABLE `grade_compositions` DISABLE KEYS */;
/*!40000 ALTER TABLE `grade_compositions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `grade_directors`
--

DROP TABLE IF EXISTS `grade_directors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `grade_directors` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `org_unit_id` bigint NOT NULL COMMENT '组织单元(系/院)',
  `enrollment_year` int NOT NULL COMMENT '入学年份',
  `director_id` bigint NOT NULL COMMENT '年级主任ID',
  `deputy_director_ids` json DEFAULT NULL COMMENT '副主任ID列表',
  `counselor_ids` json DEFAULT NULL COMMENT '辅导员ID列表',
  `max_class_count` int DEFAULT NULL COMMENT '最大班级数',
  `enrollment_quota` int DEFAULT NULL COMMENT '招生计划数',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_org_year` (`org_unit_id`,`enrollment_year`),
  KEY `idx_director` (`director_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='年级主任配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `grade_directors`
--

LOCK TABLES `grade_directors` WRITE;
/*!40000 ALTER TABLE `grade_directors` DISABLE KEYS */;
/*!40000 ALTER TABLE `grade_directors` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `grade_major_directions`
--

DROP TABLE IF EXISTS `grade_major_directions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `grade_major_directions` (
  `id` bigint NOT NULL,
  `grade_id` bigint NOT NULL COMMENT '年级ID',
  `major_direction_id` bigint NOT NULL,
  `major_id` bigint NOT NULL,
  `major_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `planned_class_count` int DEFAULT '0',
  `planned_student_count` int DEFAULT '0',
  `actual_class_count` int DEFAULT '0',
  `actual_student_count` int DEFAULT '0',
  `remarks` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by` bigint DEFAULT NULL,
  `updated_by` bigint DEFAULT NULL,
  `deleted` tinyint DEFAULT '0',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_grade_direction` (`grade_id`,`major_direction_id`),
  KEY `idx_grade` (`grade_id`),
  KEY `idx_major_direction` (`major_direction_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='年级专业方向映射表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `grade_major_directions`
--

LOCK TABLES `grade_major_directions` WRITE;
/*!40000 ALTER TABLE `grade_major_directions` DISABLE KEYS */;
/*!40000 ALTER TABLE `grade_major_directions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `grade_weight_configs`
--

DROP TABLE IF EXISTS `grade_weight_configs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `grade_weight_configs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `semester_id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `component_type` tinyint NOT NULL COMMENT '1=平时 2=期中 3=期末',
  `weight_percent` int NOT NULL COMMENT '权重百分比，如 20 表示 20%',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_semester_course_component` (`semester_id`,`course_id`,`component_type`),
  KEY `idx_semester_course` (`semester_id`,`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成绩加权配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `grade_weight_configs`
--

LOCK TABLES `grade_weight_configs` WRITE;
/*!40000 ALTER TABLE `grade_weight_configs` DISABLE KEYS */;
/*!40000 ALTER TABLE `grade_weight_configs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `grades`
--

DROP TABLE IF EXISTS `grades`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `grades` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `grade_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '年级编码',
  `grade_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '年级名称',
  `enrollment_year` int NOT NULL COMMENT '入学年份',
  `schooling_years` int DEFAULT '3' COMMENT '学制(年)',
  `expected_graduation_year` int DEFAULT NULL COMMENT '预计毕业年份',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVE' COMMENT '状态:ACTIVE,GRADUATED,DISSOLVED',
  `leader_id` bigint DEFAULT NULL COMMENT '年级主任ID',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `graduation_year` int DEFAULT NULL,
  `grade_director_id` bigint DEFAULT NULL,
  `grade_counselor_id` bigint DEFAULT NULL,
  `total_classes` int DEFAULT NULL,
  `total_students` int DEFAULT NULL,
  `standard_class_size` int DEFAULT NULL,
  `remarks` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_grade_code` (`grade_code`),
  KEY `idx_enrollment_year` (`enrollment_year`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='年级表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `grades`
--

LOCK TABLES `grades` WRITE;
/*!40000 ALTER TABLE `grades` DISABLE KEYS */;
/*!40000 ALTER TABLE `grades` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `indicator_results`
--

DROP TABLE IF EXISTS `indicator_results`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `indicator_results` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `org_unit_id` bigint DEFAULT NULL,
  `indicator_id` bigint NOT NULL,
  `target_id` bigint NOT NULL,
  `target_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `period_key` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '周期键: 2026-W22 / COUNT#7 / MANUAL:2026-05-01_2026-05-31',
  `value` decimal(12,4) DEFAULT NULL,
  `rank_position` int DEFAULT NULL,
  `grade` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT / PUBLISHED / SUPERSEDED',
  `computed_at` datetime NOT NULL,
  `published_at` datetime DEFAULT NULL,
  `revision_of` bigint DEFAULT NULL COMMENT '前一版本 id, 修订链',
  `source_submission_ids` json DEFAULT NULL COMMENT '溯源: 这个评级基于哪些 submissions',
  `source_section_ids` json DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_indicator_target_period` (`indicator_id`,`target_id`,`period_key`),
  KEY `idx_indicator_status` (`indicator_id`,`status`,`deleted`),
  KEY `idx_revision_of` (`revision_of`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级结果实体: 版本化快照 + 修订链';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `indicator_results`
--

LOCK TABLES `indicator_results` WRITE;
/*!40000 ALTER TABLE `indicator_results` DISABLE KEYS */;
/*!40000 ALTER TABLE `indicator_results` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `input_types`
--

DROP TABLE IF EXISTS `input_types`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `input_types` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '打分方式代码',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '打分方式名称',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
  `category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'basic' COMMENT '分类: basic/extended',
  `component_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'UI组件类型: number/select/checkbox/slider/star/grade',
  `component_config` json DEFAULT NULL COMMENT '组件配置参数',
  `value_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'number' COMMENT '值类型: number/string/boolean/array',
  `value_mapping` json DEFAULT NULL COMMENT '值映射规则，如等级到分数的映射',
  `validation_rules` json DEFAULT NULL COMMENT '值验证规则',
  `is_system` tinyint(1) DEFAULT '0' COMMENT '是否系统内置',
  `is_enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `sort_order` int DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` int DEFAULT '0',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_input_type_category` (`category`),
  KEY `idx_input_type_deleted` (`deleted`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='打分方式定义表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `input_types`
--

LOCK TABLES `input_types` WRITE;
/*!40000 ALTER TABLE `input_types` DISABLE KEYS */;
INSERT INTO `input_types` VALUES (1,'NUMERIC','数值输入','直接输入具体分数','basic','number','{\"max\": 100, \"min\": 0, \"step\": 1, \"precision\": 1}','number',NULL,NULL,1,1,1,'2026-06-01 22:57:01','2026-06-01 22:57:01',0,1),(2,'OPTIONS','档位选择','选择预设的分值档位','basic','select','{\"allowCustom\": false}','number',NULL,NULL,1,1,2,'2026-06-01 22:57:01','2026-06-01 22:57:01',0,1),(3,'CHECKBOX','勾选完成','是/否判断','basic','checkbox','{\"checkedValue\": 1, \"uncheckedValue\": 0}','boolean',NULL,NULL,1,1,3,'2026-06-01 22:57:01','2026-06-01 22:57:01',0,1),(4,'COUNT','计数输入','输入次数或数量','extended','number','{\"max\": 999, \"min\": 0, \"step\": 1, \"unit\": \"次\"}','number',NULL,NULL,1,1,10,'2026-06-01 22:57:01','2026-06-01 22:57:01',0,1),(5,'STAR','星级点选','1-5星评分','extended','star','{\"max\": 5, \"allowHalf\": true}','number',NULL,NULL,1,1,11,'2026-06-01 22:57:01','2026-06-01 22:57:01',0,1),(6,'GRADE','等级选择','A/B/C/D等级选择','extended','grade','{\"colors\": [\"#52c41a\", \"#1890ff\", \"#faad14\", \"#ff7a45\", \"#ff4d4f\"], \"grades\": [\"A\", \"B\", \"C\", \"D\", \"E\"]}','string',NULL,NULL,1,1,12,'2026-06-01 22:57:01','2026-06-01 22:57:01',0,1),(7,'SLIDER','滑块选择','滑动选择分值','extended','slider','{\"max\": 100, \"min\": 0, \"step\": 5, \"showStops\": true}','number',NULL,NULL,1,1,13,'2026-06-01 22:57:01','2026-06-01 22:57:01',0,1),(8,'PERCENTAGE','百分比','百分比输入','extended','number','{\"max\": 100, \"min\": 0, \"step\": 1, \"suffix\": \"%\"}','number',NULL,NULL,1,1,14,'2026-06-01 22:57:01','2026-06-01 22:57:01',0,1),(9,'TEXT','文本评价','输入文字评价','extended','textarea','{\"rows\": 3, \"maxLength\": 500}','string',NULL,NULL,1,1,15,'2026-06-01 22:57:01','2026-06-01 22:57:01',0,1);
/*!40000 ALTER TABLE `input_types` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_alert_rules`
--

DROP TABLE IF EXISTS `insp_alert_rules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_alert_rules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `rule_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `metric_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'SCORE_DROP|CONSECUTIVE_FAIL|HIGH_DEVIATION|LOW_COMPLIANCE|OVERDUE_CORRECTION',
  `threshold_config` json NOT NULL COMMENT '阈值配置',
  `severity` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'WARNING' COMMENT 'INFO|WARNING|CRITICAL',
  `notification_channels` json DEFAULT NULL COMMENT '通知渠道配置',
  `is_enabled` tinyint DEFAULT '1',
  `project_id` bigint DEFAULT NULL COMMENT 'NULL=全局',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  `org_unit_id` bigint NOT NULL COMMENT '数据权限边界(MetaObjectHandler 自动填充, NOT NULL by V20260508_1)',
  PRIMARY KEY (`id`),
  KEY `idx_insp_alert_rules_org_unit` (`org_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预警规则';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_alert_rules`
--

LOCK TABLES `insp_alert_rules` WRITE;
/*!40000 ALTER TABLE `insp_alert_rules` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_alert_rules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_alerts`
--

DROP TABLE IF EXISTS `insp_alerts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_alerts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `alert_rule_id` bigint NOT NULL,
  `target_id` bigint DEFAULT NULL,
  `target_type` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `target_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `metric_value` decimal(10,2) DEFAULT NULL COMMENT '触发值',
  `threshold_value` decimal(10,2) DEFAULT NULL COMMENT '阈值',
  `severity` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `message` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'OPEN' COMMENT 'OPEN|ACKNOWLEDGED|RESOLVED|DISMISSED',
  `acknowledged_by` bigint DEFAULT NULL,
  `acknowledged_at` datetime DEFAULT NULL,
  `resolved_at` datetime DEFAULT NULL,
  `triggered_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  `org_unit_id` bigint NOT NULL COMMENT '数据权限边界(MetaObjectHandler 自动填充, NOT NULL by V20260508_1)',
  `created_by` bigint DEFAULT NULL COMMENT '记录创建人(SELF 范围用)',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_triggered` (`triggered_at`),
  KEY `idx_insp_alerts_org_unit` (`org_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预警记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_alerts`
--

LOCK TABLES `insp_alerts` WRITE;
/*!40000 ALTER TABLE `insp_alerts` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_alerts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_audit_trail`
--

DROP TABLE IF EXISTS `insp_audit_trail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_audit_trail` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `user_id` bigint DEFAULT NULL COMMENT '操作用户ID',
  `user_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作用户名',
  `action` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作动作',
  `resource_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '资源类型',
  `resource_id` bigint DEFAULT NULL COMMENT '资源ID',
  `resource_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '资源名称',
  `details` text COLLATE utf8mb4_unicode_ci COMMENT 'JSON 前后快照/明细',
  `ip_address` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户端IP',
  `occurred_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  `org_unit_id` bigint DEFAULT NULL COMMENT '数据权限归属组织',
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_audit_resource` (`resource_type`,`resource_id`),
  KEY `idx_audit_user` (`user_id`),
  KEY `idx_audit_time` (`occurred_at`),
  KEY `idx_audit_org` (`org_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查平台审计日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_audit_trail`
--

LOCK TABLES `insp_audit_trail` WRITE;
/*!40000 ALTER TABLE `insp_audit_trail` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_audit_trail` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_calculation_rules`
--

DROP TABLE IF EXISTS `insp_calculation_rules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_calculation_rules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `scoring_profile_id` bigint NOT NULL,
  `rule_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `rule_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `priority` int NOT NULL DEFAULT '0',
  `rule_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'VETO|PROGRESSIVE|BONUS|CUSTOM',
  `config` json NOT NULL,
  `is_enabled` tinyint NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `scope_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'GLOBAL' COMMENT 'GLOBAL|DIMENSION|CROSS_DIMENSION',
  `target_dimension_ids` json DEFAULT NULL COMMENT '跨维度规则关联的维度ID列表',
  `activation_condition` json DEFAULT NULL COMMENT '激活条件(条件逻辑V2格式)',
  `applies_to` json DEFAULT NULL COMMENT '适用范围: {targetTypes:[], orgUnitIds:[], userTypes:[]}',
  `effective_from` date DEFAULT NULL COMMENT '生效起始日',
  `effective_until` date DEFAULT NULL COMMENT '生效截止日',
  `exclusion_group` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '互斥组名',
  PRIMARY KEY (`id`),
  KEY `idx_profile_priority` (`scoring_profile_id`,`priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V7 计算规则链';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_calculation_rules`
--

LOCK TABLES `insp_calculation_rules` WRITE;
/*!40000 ALTER TABLE `insp_calculation_rules` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_calculation_rules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `place_audit_logs` (G: post-v3 V20260613_2 同步进 baseline — squash 遗漏的场所审计表)
--

DROP TABLE IF EXISTS `place_audit_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `place_audit_logs` (
  `event_id` varchar(64) NOT NULL COMMENT '全局唯一事件ID (UUID)',
  `request_id` varchar(64) NOT NULL COMMENT '请求ID（同一请求的多个操作共享）',
  `resource_type` varchar(50) NOT NULL COMMENT '资源类型：PLACE',
  `resource_id` bigint NOT NULL COMMENT '场所ID',
  `resource_name` varchar(200) DEFAULT NULL COMMENT '场所名称（冗余，便于查询）',
  `event_name` varchar(100) NOT NULL COMMENT '事件名称：CREATE/UPDATE/DELETE/AssignOrganization/ChangeStatus',
  `event_type` varchar(50) NOT NULL COMMENT '事件类型：ApiCall/ConsoleAccess/SystemAction',
  `event_source` varchar(100) NOT NULL COMMENT '事件来源：place-service',
  `event_time` datetime(6) NOT NULL COMMENT '事件时间（微秒精度）',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `user_name` varchar(100) DEFAULT NULL COMMENT '用户名',
  `user_type` varchar(50) DEFAULT NULL COMMENT '用户类型：IAM_USER/ASSUMED_ROLE/SYSTEM',
  `access_key_id` varchar(100) DEFAULT NULL COMMENT 'JWT Token ID',
  `session_id` varchar(100) DEFAULT NULL COMMENT '会话ID',
  `mfa_authenticated` tinyint(1) DEFAULT NULL COMMENT '是否MFA认证',
  `source_ip` varchar(50) NOT NULL COMMENT '来源IP地址',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理（浏览器/客户端）',
  `referer` varchar(500) DEFAULT NULL COMMENT '来源页面URL',
  `api_endpoint` varchar(200) NOT NULL COMMENT 'API端点：/api/v9/places/{id}',
  `http_method` varchar(10) DEFAULT NULL COMMENT 'HTTP方法：GET/POST/PUT/DELETE',
  `request_parameters` json DEFAULT NULL COMMENT '完整请求参数',
  `response_elements` json DEFAULT NULL COMMENT '完整响应内容',
  `before_snapshot` json DEFAULT NULL COMMENT '变更前完整状态快照',
  `after_snapshot` json DEFAULT NULL COMMENT '变更后完整状态快照',
  `changed_fields` json DEFAULT NULL COMMENT '变更字段列表 [{fieldName, oldValue, newValue}]',
  `is_rollback` tinyint(1) DEFAULT '0' COMMENT '是否回滚操作',
  `related_event_id` varchar(64) DEFAULT NULL COMMENT '关联事件ID（回滚时指向原事件）',
  `reason` text COLLATE utf8mb4_unicode_ci COMMENT '变更原因/备注',
  `tags` json DEFAULT NULL COMMENT '自定义标签 {key: value}',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `ttl_expires_at` datetime DEFAULT NULL COMMENT '日志过期时间（合规要求：保留7年）',
  PRIMARY KEY (`event_id`),
  KEY `idx_resource` (`resource_type`,`resource_id`,`event_time` DESC),
  KEY `idx_user` (`user_id`,`event_time` DESC),
  KEY `idx_request` (`request_id`),
  KEY `idx_event_name` (`event_name`,`event_time` DESC),
  KEY `idx_source_ip` (`source_ip`,`event_time` DESC),
  KEY `idx_event_time` (`event_time` DESC),
  KEY `idx_ttl` (`ttl_expires_at`)
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场所审计日志 - 完整5W1H记录（对标AWS CloudTrail）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `insp_corrective_cases`
--

DROP TABLE IF EXISTS `insp_corrective_cases`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_corrective_cases` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `case_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `submission_id` bigint DEFAULT NULL COMMENT '关联提交',
  `detail_id` bigint DEFAULT NULL COMMENT '关联明细项',
  `project_id` bigint DEFAULT NULL COMMENT '关联项目',
  `task_id` bigint DEFAULT NULL COMMENT '关联任务',
  `target_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ORG|PLACE|USER|ASSET',
  `target_id` bigint DEFAULT NULL,
  `target_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `issue_description` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '问题描述',
  `required_action` text COLLATE utf8mb4_unicode_ci COMMENT '要求整改措施',
  `priority` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MEDIUM' COMMENT 'LOW|MEDIUM|HIGH|CRITICAL',
  `deadline` datetime DEFAULT NULL COMMENT '整改截止时间',
  `assignee_id` bigint DEFAULT NULL COMMENT '责任人ID',
  `assignee_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '责任人姓名',
  `escalation_level` int NOT NULL DEFAULT '0' COMMENT '升级层级',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN|ASSIGNED|IN_PROGRESS|SUBMITTED|VERIFIED|REJECTED|CLOSED|ESCALATED',
  `correction_note` text COLLATE utf8mb4_unicode_ci COMMENT '整改说明',
  `correction_evidence_ids` json DEFAULT NULL COMMENT '整改证据ID列表',
  `corrected_at` datetime DEFAULT NULL COMMENT '提交整改时间',
  `verifier_id` bigint DEFAULT NULL COMMENT '验证人ID',
  `verifier_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '验证人姓名',
  `verified_at` datetime DEFAULT NULL COMMENT '验证时间',
  `verification_note` text COLLATE utf8mb4_unicode_ci COMMENT '验证说明',
  `effectiveness_check_date` date DEFAULT NULL COMMENT '效果检查日期 (G: post-v3 同步进 baseline)',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `suggested_by_system` tinyint NOT NULL DEFAULT '0' COMMENT '是否由引擎建议生成 (1=引擎,0=手动)',
  `suggestion_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '建议理由 (可读文本)',
  `severity_score` decimal(4,3) DEFAULT NULL COMMENT '标准化严重度分 0-1, 引擎计算',
  `explain_trace_json` json DEFAULT NULL COMMENT '引擎决策审计链 [{layer,rule,output,...}]',
  `org_unit_id` bigint NOT NULL COMMENT '数据权限边界(MetaObjectHandler 自动填充, NOT NULL by V20260508_1)',
  `issue_category_id` bigint DEFAULT NULL,
  `deficiency_code` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `rca_method` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `rca_data` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `preventive_action` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `effectiveness_status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `effectiveness_note` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_case_code` (`case_code`,`deleted`),
  KEY `idx_submission` (`submission_id`),
  KEY `idx_project` (`project_id`),
  KEY `idx_task` (`task_id`),
  KEY `idx_assignee` (`assignee_id`),
  KEY `idx_status` (`status`),
  KEY `idx_priority` (`priority`),
  KEY `idx_deadline` (`deadline`),
  KEY `idx_insp_corrective_cases_org_unit` (`org_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V7 整改案例';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_corrective_cases`
--

LOCK TABLES `insp_corrective_cases` WRITE;
/*!40000 ALTER TABLE `insp_corrective_cases` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_corrective_cases` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_corrective_subtasks`
--

DROP TABLE IF EXISTS `insp_corrective_subtasks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_corrective_subtasks` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `case_id` bigint NOT NULL COMMENT '所属纠正案例',
  `subtask_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `assignee_id` bigint NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING' COMMENT 'PENDING|IN_PROGRESS|COMPLETED|BLOCKED',
  `priority` int DEFAULT '0',
  `due_date` date DEFAULT NULL,
  `completed_at` datetime DEFAULT NULL,
  `sort_order` int DEFAULT '0',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  `org_unit_id` bigint NOT NULL COMMENT '数据权限边界(MetaObjectHandler 自动填充, NOT NULL by V20260508_1)',
  PRIMARY KEY (`id`),
  KEY `idx_case` (`case_id`),
  KEY `idx_insp_corrective_subtasks_org_unit` (`org_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='纠正案例子任务';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_corrective_subtasks`
--

LOCK TABLES `insp_corrective_subtasks` WRITE;
/*!40000 ALTER TABLE `insp_corrective_subtasks` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_corrective_subtasks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_corrective_summaries`
--

DROP TABLE IF EXISTS `insp_corrective_summaries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_corrective_summaries` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `project_id` bigint NOT NULL,
  `period_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `period_start` date NOT NULL,
  `period_end` date NOT NULL,
  `total_cases` int DEFAULT '0',
  `open_cases` int DEFAULT '0',
  `in_progress_cases` int DEFAULT '0',
  `closed_cases` int DEFAULT '0',
  `overdue_cases` int DEFAULT '0',
  `avg_resolution_hours` decimal(10,2) DEFAULT NULL,
  `compliance_rate` decimal(5,2) DEFAULT NULL,
  `org_unit_id` bigint NOT NULL COMMENT '数据权限边界(MetaObjectHandler 自动填充, NOT NULL by V20260508_1)',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `escalated_cases` int DEFAULT NULL,
  `avg_resolution_days` decimal(10,2) DEFAULT NULL,
  `on_time_rate` decimal(10,2) DEFAULT NULL,
  `effectiveness_confirmed` int DEFAULT NULL,
  `effectiveness_failed` int DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_corrective_period` (`project_id`,`period_type`,`period_start`,`deleted`),
  KEY `idx_org_unit` (`org_unit_id`),
  KEY `idx_insp_corrective_summaries_org_unit` (`org_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='整改汇总';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_corrective_summaries`
--

LOCK TABLES `insp_corrective_summaries` WRITE;
/*!40000 ALTER TABLE `insp_corrective_summaries` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_corrective_summaries` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_daily_summaries`
--

DROP TABLE IF EXISTS `insp_daily_summaries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_daily_summaries` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `project_id` bigint NOT NULL,
  `summary_date` date NOT NULL,
  `target_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ORG|PLACE|USER|ASSET',
  `target_id` bigint NOT NULL,
  `target_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `org_unit_id` bigint DEFAULT NULL,
  `org_unit_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `inspection_count` int NOT NULL DEFAULT '0',
  `avg_score` decimal(8,2) DEFAULT NULL,
  `min_score` decimal(8,2) DEFAULT NULL,
  `max_score` decimal(8,2) DEFAULT NULL,
  `total_deductions` decimal(8,2) NOT NULL DEFAULT '0.00',
  `total_bonuses` decimal(8,2) NOT NULL DEFAULT '0.00',
  `pass_count` int NOT NULL DEFAULT '0',
  `fail_count` int NOT NULL DEFAULT '0',
  `ranking` int DEFAULT NULL,
  `dimension_scores` json DEFAULT NULL COMMENT '各维度分数 {"safety":85,"hygiene":90}',
  `grade` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `percentile` decimal(5,2) DEFAULT NULL COMMENT '百分位(0-100)',
  `rank_in_group` int DEFAULT NULL COMMENT '组内排名',
  `group_total` int DEFAULT NULL COMMENT '组内总数',
  `group_avg` decimal(8,2) DEFAULT NULL COMMENT '组平均分',
  `group_median` decimal(8,2) DEFAULT NULL COMMENT '组中位数',
  `created_by` bigint DEFAULT NULL COMMENT '记录创建人(SELF 范围用)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_daily` (`project_id`,`summary_date`,`target_type`,`target_id`,`deleted`),
  KEY `idx_project_date` (`project_id`,`summary_date`),
  KEY `idx_target` (`target_type`,`target_id`),
  KEY `idx_org_unit` (`org_unit_id`),
  KEY `idx_ranking` (`project_id`,`summary_date`,`ranking`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V7 日汇总';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_daily_summaries`
--

LOCK TABLES `insp_daily_summaries` WRITE;
/*!40000 ALTER TABLE `insp_daily_summaries` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_daily_summaries` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_escalation_policies`
--

DROP TABLE IF EXISTS `insp_escalation_policies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_escalation_policies` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `profile_id` bigint NOT NULL COMMENT '关联评分配置',
  `policy_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `lookup_period_days` int NOT NULL DEFAULT '30' COMMENT '回溯天数',
  `escalation_mode` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MULTIPLY' COMMENT 'MULTIPLY|ADD|FIXED_TABLE',
  `multiplier` decimal(4,2) DEFAULT '2.00' COMMENT '乘数(MULTIPLY模式)',
  `adder` decimal(6,2) DEFAULT NULL COMMENT '增加值(ADD模式)',
  `fixed_table` json DEFAULT NULL COMMENT '固定表: [{occurrence:1,factor:1.0},...]',
  `max_escalation_factor` decimal(4,2) DEFAULT '5.00' COMMENT '最大放大倍数',
  `match_by` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ITEM_CODE' COMMENT 'ITEM_CODE|DIMENSION|SECTION|CATEGORY',
  `is_enabled` tinyint DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_profile` (`profile_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='重复违规递增策略';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_escalation_policies`
--

LOCK TABLES `insp_escalation_policies` WRITE;
/*!40000 ALTER TABLE `insp_escalation_policies` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_escalation_policies` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_evidences`
--

DROP TABLE IF EXISTS `insp_evidences`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_evidences` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `submission_id` bigint NOT NULL,
  `detail_id` bigint DEFAULT NULL COMMENT '关联明细,null=提交级',
  `evidence_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'PHOTO|VIDEO|DOCUMENT|SIGNATURE|GPS_POINT',
  `file_name` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `file_path` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `file_url` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `file_size` bigint DEFAULT NULL,
  `mime_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `thumbnail_url` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `latitude` decimal(10,7) DEFAULT NULL,
  `longitude` decimal(10,7) DEFAULT NULL,
  `captured_at` datetime DEFAULT NULL,
  `metadata` json DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `ai_analysis` json DEFAULT NULL COMMENT 'AI识别结果',
  `ai_confidence` decimal(4,2) DEFAULT NULL,
  `org_unit_id` bigint NOT NULL COMMENT '数据权限边界(MetaObjectHandler 自动填充, NOT NULL by V20260508_1)',
  `created_by` bigint DEFAULT NULL COMMENT '记录创建人(SELF 范围用)',
  PRIMARY KEY (`id`),
  KEY `idx_submission` (`submission_id`),
  KEY `idx_detail` (`detail_id`),
  KEY `idx_insp_evidences_org_unit` (`org_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V7 证据文件';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_evidences`
--

LOCK TABLES `insp_evidences` WRITE;
/*!40000 ALTER TABLE `insp_evidences` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_evidences` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_grade_bands`
--

DROP TABLE IF EXISTS `insp_grade_bands`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_grade_bands` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `scoring_profile_id` bigint NOT NULL,
  `dimension_id` bigint DEFAULT NULL COMMENT 'null=总分等级',
  `grade_code` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  `grade_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `min_score` decimal(10,2) NOT NULL,
  `max_score` decimal(10,2) NOT NULL,
  `color` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `icon` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_profile` (`scoring_profile_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V7 等级映射';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_grade_bands`
--

LOCK TABLES `insp_grade_bands` WRITE;
/*!40000 ALTER TABLE `insp_grade_bands` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_grade_bands` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_grade_definitions`
--

DROP TABLE IF EXISTS `insp_grade_definitions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_grade_definitions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `grade_scheme_id` bigint NOT NULL,
  `code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '等级编码',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '等级名称',
  `min_value` decimal(10,2) NOT NULL,
  `max_value` decimal(10,2) NOT NULL,
  `color` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `icon` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `linked_event_type_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联事件类型编码(达到此等级时触发)',
  PRIMARY KEY (`id`),
  KEY `idx_scheme` (`grade_scheme_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='等级定义';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_grade_definitions`
--

LOCK TABLES `insp_grade_definitions` WRITE;
/*!40000 ALTER TABLE `insp_grade_definitions` DISABLE KEYS */;
INSERT INTO `insp_grade_definitions` VALUES (1,1,'A','优秀',90.00,100.00,'#22C55E',NULL,1,1,NULL),(2,1,'B','良好',80.00,89.99,'#3B82F6',NULL,2,1,NULL),(3,1,'C','中等',70.00,79.99,'#F59E0B',NULL,3,1,NULL),(4,1,'D','及格',60.00,69.99,'#F97316',NULL,4,1,NULL),(5,1,'F','不及格',0.00,59.99,'#EF4444',NULL,5,1,NULL),(6,2,'PASS','合格',60.00,100.00,'#22C55E',NULL,1,1,NULL),(7,2,'FAIL','不合格',0.00,59.99,'#EF4444',NULL,2,1,NULL),(8,3,'RED','红旗',90.00,100.00,'#EF4444','flag',1,1,NULL),(9,3,'BLUE','蓝旗',70.00,89.99,'#3B82F6','flag',2,1,NULL),(10,3,'YELLOW','黄旗',0.00,69.99,'#F59E0B','flag',3,1,NULL),(11,4,'5STAR','五星',90.00,100.00,'#FFD700','star',1,1,NULL),(12,4,'4STAR','四星',80.00,89.99,'#C0C0C0','star',2,1,NULL),(13,4,'3STAR','三星',70.00,79.99,'#CD7F32','star',3,1,NULL),(14,4,'2STAR','二星',60.00,69.99,'#F97316','star',4,1,NULL),(15,4,'1STAR','一星',0.00,59.99,'#808080','star',5,1,NULL),(16,5,'A','优秀',90.00,100.00,'#22C55E',NULL,1,1,NULL),(17,5,'B','良好',80.00,89.99,'#3B82F6',NULL,2,1,NULL),(18,5,'C','中等',70.00,79.99,'#F59E0B',NULL,3,1,NULL),(19,5,'D','及格',60.00,69.99,'#F97316',NULL,4,1,NULL),(20,5,'F','不及格',0.00,59.99,'#EF4444',NULL,5,1,NULL),(21,6,'PASS','合格',60.00,100.00,'#22C55E',NULL,1,1,NULL),(22,6,'FAIL','不合格',0.00,59.99,'#EF4444',NULL,2,1,NULL),(23,7,'RED','红旗',90.00,100.00,'#EF4444','flag',1,1,NULL),(24,7,'BLUE','蓝旗',70.00,89.99,'#3B82F6','flag',2,1,NULL),(25,7,'YELLOW','黄旗',0.00,69.99,'#F59E0B','flag',3,1,NULL),(26,8,'5STAR','五星',90.00,100.00,'#FFD700','star',1,1,NULL),(27,8,'4STAR','四星',80.00,89.99,'#C0C0C0','star',2,1,NULL),(28,8,'3STAR','三星',70.00,79.99,'#CD7F32','star',3,1,NULL),(29,8,'2STAR','二星',60.00,69.99,'#F97316','star',4,1,NULL),(30,8,'1STAR','一星',0.00,59.99,'#808080','star',5,1,NULL);
/*!40000 ALTER TABLE `insp_grade_definitions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_grade_schemes`
--

DROP TABLE IF EXISTS `insp_grade_schemes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_grade_schemes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `display_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '方案显示名称',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '方案描述',
  `scheme_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SCORE_RANGE' COMMENT 'SCORE_RANGE|PERCENT_RANGE',
  `is_system` tinyint(1) NOT NULL DEFAULT '0',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='等级方案';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_grade_schemes`
--

LOCK TABLES `insp_grade_schemes` WRITE;
/*!40000 ALTER TABLE `insp_grade_schemes` DISABLE KEYS */;
INSERT INTO `insp_grade_schemes` VALUES (1,0,'标准五级评定','按百分比划分A/B/C/D/F五个等级','PERCENT_RANGE',1,NULL,'2026-06-01 22:56:31','2026-06-01 22:56:31',0),(2,0,'合格评定','简单的合格/不合格二级评定','PERCENT_RANGE',1,NULL,'2026-06-01 22:56:31','2026-06-01 22:56:31',0),(3,0,'流动红旗','红旗/蓝旗/黄旗三级流动旗评定','PERCENT_RANGE',1,NULL,'2026-06-01 22:56:31','2026-06-01 22:56:31',0),(4,0,'星级评定','五星到一星五级星级评定','PERCENT_RANGE',1,NULL,'2026-06-01 22:56:31','2026-06-01 22:56:31',0),(5,0,'标准五级评定','按百分比划分A/B/C/D/F五个等级','PERCENT_RANGE',1,NULL,'2026-06-01 23:05:35','2026-06-01 23:05:35',0),(6,0,'合格评定','简单的合格/不合格二级评定','PERCENT_RANGE',1,NULL,'2026-06-01 23:05:35','2026-06-01 23:05:35',0),(7,0,'流动红旗','红旗/蓝旗/黄旗三级流动旗评定','PERCENT_RANGE',1,NULL,'2026-06-01 23:05:35','2026-06-01 23:05:35',0),(8,0,'星级评定','五星到一星五级星级评定','PERCENT_RANGE',1,NULL,'2026-06-01 23:05:35','2026-06-01 23:05:35',0);
/*!40000 ALTER TABLE `insp_grade_schemes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_holiday_calendars`
--

DROP TABLE IF EXISTS `insp_holiday_calendars`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_holiday_calendars` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `calendar_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '日历名称',
  `year` int NOT NULL COMMENT '年份',
  `holidays` text COLLATE utf8mb4_unicode_ci COMMENT '节假日 JSON 日期数组',
  `workdays` text COLLATE utf8mb4_unicode_ci COMMENT '调休补班 JSON 日期数组',
  `is_default` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否默认日历',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `org_unit_id` bigint DEFAULT NULL COMMENT '数据权限归属组织',
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_holiday_year` (`year`),
  KEY `idx_holiday_default` (`is_default`),
  KEY `idx_holiday_org` (`org_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查平台假日日历';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_holiday_calendars`
--

LOCK TABLES `insp_holiday_calendars` WRITE;
/*!40000 ALTER TABLE `insp_holiday_calendars` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_holiday_calendars` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_indicator_scores`
--

DROP TABLE IF EXISTS `insp_indicator_scores`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_indicator_scores` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `indicator_id` bigint NOT NULL,
  `target_id` bigint NOT NULL,
  `target_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `target_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `period_start` date NOT NULL,
  `period_end` date NOT NULL,
  `score` decimal(10,2) DEFAULT NULL,
  `grade_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `grade_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `grade_color` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `source_count` int DEFAULT '0',
  `detail` text COLLATE utf8mb4_unicode_ci COMMENT 'JSON',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_indicator_target_period` (`indicator_id`,`target_id`,`period_start`),
  KEY `idx_indicator` (`indicator_id`),
  KEY `idx_target` (`target_id`),
  KEY `idx_period` (`period_start`,`period_end`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='指标得分记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_indicator_scores`
--

LOCK TABLES `insp_indicator_scores` WRITE;
/*!40000 ALTER TABLE `insp_indicator_scores` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_indicator_scores` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_indicators`
--

DROP TABLE IF EXISTS `insp_indicators`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_indicators` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `project_id` bigint NOT NULL,
  `parent_indicator_id` bigint DEFAULT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '指标名称',
  `indicator_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'LEAF' COMMENT 'LEAF|COMPOSITE',
  `source_section_id` bigint DEFAULT NULL COMMENT '数据来源分区ID (LEAF)',
  `source_section_ids` json DEFAULT NULL COMMENT '跨分区组合: 单分区=[id], 多分区=[id,id,...]',
  `source_aggregation` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'AVG' COMMENT 'AVG|MAX|MIN|LATEST|SUM (LEAF)',
  `composite_aggregation` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'WEIGHTED_AVG' COMMENT 'WEIGHTED_AVG|SUM|AVG|MIN|MAX (COMPOSITE)',
  `missing_policy` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'SKIP' COMMENT 'SKIP|CARRY_FORWARD|MARK_INCOMPLETE (COMPOSITE)',
  `evaluation_period` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PER_TASK' COMMENT 'PER_TASK|DAILY|WEEKLY|MONTHLY',
  `grade_scheme_id` bigint DEFAULT NULL COMMENT 'FK -> insp_grade_schemes',
  `sort_order` int NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  `normalization` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'NONE' COMMENT 'NONE|RELATION_COUNT|FIXED_VALUE|PERCENTAGE',
  `normalization_config` text COLLATE utf8mb4_unicode_ci COMMENT 'JSON config for normalization',
  `evaluation_method` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PERCENT_RANGE' COMMENT 'SCORE_RANGE|PERCENT_RANGE|RANK_COUNT|RANK_PERCENT',
  `grade_thresholds` text COLLATE utf8mb4_unicode_ci COMMENT 'JSON: [{gradeCode, value}]',
  `trigger_mode` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'TIME_WINDOW' COMMENT '触发模式: TIME_WINDOW/COUNT/MANUAL',
  `count_threshold` int DEFAULT NULL COMMENT 'COUNT 模式: 累计 N 次触发一次评级',
  `weights_by_section` json DEFAULT NULL COMMENT '跨分区加权: {section_id: weight}, 不填等权',
  `rank_direction` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '排名方向: ASC/DESC, NULL=不排名(值映射等级)',
  `late_policy` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'REVISE_ORIGINAL' COMMENT '逾期补做策略: REVISE_ORIGINAL/CARRY_FORWARD/EXCLUDE',
  `submission_date_field` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'taskDate' COMMENT '归属周期依据: taskDate (计划日, 默认) / completedAt',
  PRIMARY KEY (`id`),
  KEY `idx_project` (`project_id`),
  KEY `idx_parent` (`parent_indicator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价指标';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_indicators`
--

LOCK TABLES `insp_indicators` WRITE;
/*!40000 ALTER TABLE `insp_indicators` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_indicators` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_inspection_plans`
--

DROP TABLE IF EXISTS `insp_inspection_plans`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_inspection_plans` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `project_id` bigint NOT NULL,
  `plan_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `root_section_id` bigint DEFAULT NULL COMMENT 'V66: 该计划使用的模板根分区ID',
  `schedule_mode` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'REGULAR' COMMENT 'REGULAR=定期/ON_DEMAND=手动触发',
  `cycle_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'DAILY' COMMENT 'DAILY/WEEKLY/MONTHLY',
  `frequency` int DEFAULT '1' COMMENT '每周期执行次数',
  `schedule_days` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'JSON: [1,3,5] 周几(WEEKLY时)',
  `time_slots` text COLLATE utf8mb4_unicode_ci COMMENT 'JSON: ["07:00-08:00","12:00-13:00"]',
  `skip_holidays` tinyint DEFAULT '0',
  `is_enabled` tinyint DEFAULT '1',
  `sort_order` int DEFAULT '0',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  `assign_strategy` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OPEN_TO_ALL' COMMENT '检查员指派策略: SPECIFIC=限定到 insp_plan_inspectors / OPEN_TO_ALL=项目全员可领',
  `raters_per_target` int NOT NULL DEFAULT '1' COMMENT '每目标评分人数 1=单人 >1=多人评分',
  `rrule` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'RRULE 周期表达式 (RFC 5545 子集); 非空时优先于 cycleType/frequency/scheduleDays. 支持 FREQ=DAILY|WEEKLY|MONTHLY + BYDAY + INTERVAL.',
  PRIMARY KEY (`id`),
  KEY `idx_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查计划';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_inspection_plans`
--

LOCK TABLES `insp_inspection_plans` WRITE;
/*!40000 ALTER TABLE `insp_inspection_plans` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_inspection_plans` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_inspector_summaries`
--

DROP TABLE IF EXISTS `insp_inspector_summaries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_inspector_summaries` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `project_id` bigint NOT NULL,
  `inspector_id` bigint NOT NULL,
  `inspector_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `period_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `period_start` date NOT NULL,
  `period_end` date NOT NULL,
  `total_tasks` int DEFAULT '0',
  `completed_tasks` int DEFAULT '0',
  `cancelled_tasks` int DEFAULT '0',
  `expired_tasks` int DEFAULT '0',
  `avg_completion_time_minutes` decimal(10,2) DEFAULT NULL,
  `avg_score` decimal(8,2) DEFAULT NULL,
  `total_submissions` int DEFAULT '0',
  `flagged_submissions` int DEFAULT '0',
  `compliance_rate` decimal(5,2) DEFAULT NULL,
  `org_unit_id` bigint NOT NULL COMMENT '数据权限边界(MetaObjectHandler 自动填充, NOT NULL by V20260508_1)',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `created_by` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inspector_period` (`project_id`,`inspector_id`,`period_type`,`period_start`,`deleted`),
  KEY `idx_inspector` (`inspector_id`),
  KEY `idx_org_unit` (`org_unit_id`),
  KEY `idx_insp_inspector_summaries_org_unit` (`org_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查员维度汇总';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_inspector_summaries`
--

LOCK TABLES `insp_inspector_summaries` WRITE;
/*!40000 ALTER TABLE `insp_inspector_summaries` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_inspector_summaries` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_issue_categories`
--

DROP TABLE IF EXISTS `insp_issue_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_issue_categories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `parent_id` bigint DEFAULT NULL COMMENT '父分类ID, NULL=根分类',
  `category_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分类编码',
  `category_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类名称',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分类描述',
  `icon` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图标',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `is_enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `org_unit_id` bigint DEFAULT NULL COMMENT '数据权限归属组织',
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_issue_cat_parent` (`parent_id`,`sort_order`),
  KEY `idx_issue_cat_code` (`category_code`),
  KEY `idx_issue_cat_org` (`org_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查平台问题分类';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_issue_categories`
--

LOCK TABLES `insp_issue_categories` WRITE;
/*!40000 ALTER TABLE `insp_issue_categories` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_issue_categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_item_frequency_summaries`
--

DROP TABLE IF EXISTS `insp_item_frequency_summaries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_item_frequency_summaries` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `project_id` bigint NOT NULL,
  `template_item_id` bigint NOT NULL,
  `item_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `period_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `period_start` date NOT NULL,
  `period_end` date NOT NULL,
  `occurrence_count` int DEFAULT '0',
  `negative_count` int DEFAULT '0',
  `flagged_count` int DEFAULT '0',
  `avg_score` decimal(8,2) DEFAULT NULL,
  `org_unit_id` bigint NOT NULL COMMENT '数据权限边界(MetaObjectHandler 自动填充, NOT NULL by V20260508_1)',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `item_code` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `section_id` bigint DEFAULT NULL,
  `section_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `total_deduction` decimal(10,2) DEFAULT NULL,
  `avg_deduction` decimal(10,2) DEFAULT NULL,
  `cumulative_percentage` decimal(10,2) DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_item_period` (`project_id`,`template_item_id`,`period_type`,`period_start`,`deleted`),
  KEY `idx_org_unit` (`org_unit_id`),
  KEY `idx_insp_item_frequency_summaries_org_unit` (`org_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查项频次汇总';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_item_frequency_summaries`
--

LOCK TABLES `insp_item_frequency_summaries` WRITE;
/*!40000 ALTER TABLE `insp_item_frequency_summaries` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_item_frequency_summaries` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_library_items`
--

DROP TABLE IF EXISTS `insp_library_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_library_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `item_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `item_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `item_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `category` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '库内分类',
  `tags` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标签(逗号分隔)',
  `default_config` json DEFAULT NULL COMMENT '默认配置',
  `default_validation_rules` json DEFAULT NULL,
  `default_scoring_config` json DEFAULT NULL,
  `default_help_content` text COLLATE utf8mb4_unicode_ci,
  `usage_count` int DEFAULT '0' COMMENT '引用次数',
  `is_standard` tinyint DEFAULT '0' COMMENT '是否标准项(不可删除)',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`tenant_id`,`item_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查项库';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_library_items`
--

LOCK TABLES `insp_library_items` WRITE;
/*!40000 ALTER TABLE `insp_library_items` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_library_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_period_summaries`
--

DROP TABLE IF EXISTS `insp_period_summaries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_period_summaries` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `project_id` bigint NOT NULL,
  `period_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'WEEKLY|MONTHLY|QUARTERLY|YEARLY',
  `period_start` date NOT NULL,
  `period_end` date NOT NULL,
  `target_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_id` bigint NOT NULL,
  `target_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `org_unit_id` bigint DEFAULT NULL,
  `org_unit_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `inspection_days` int NOT NULL DEFAULT '0',
  `avg_score` decimal(8,2) DEFAULT NULL,
  `min_score` decimal(8,2) DEFAULT NULL,
  `max_score` decimal(8,2) DEFAULT NULL,
  `score_std_dev` decimal(8,4) DEFAULT NULL,
  `trend_direction` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'UP|DOWN|STABLE',
  `trend_percent` decimal(8,2) DEFAULT NULL,
  `ranking` int DEFAULT NULL,
  `dimension_scores` json DEFAULT NULL,
  `grade` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `corrective_count` int NOT NULL DEFAULT '0',
  `corrective_closed_count` int NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `prev_period_score` decimal(8,2) DEFAULT NULL COMMENT '上期分数',
  `mom_change` decimal(6,2) DEFAULT NULL COMMENT '环比变化(%)',
  `yoy_score` decimal(8,2) DEFAULT NULL COMMENT '去年同期分数',
  `yoy_change` decimal(6,2) DEFAULT NULL COMMENT '同比变化(%)',
  `created_by` bigint DEFAULT NULL COMMENT '记录创建人(SELF 范围用)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_period` (`project_id`,`period_type`,`period_start`,`target_type`,`target_id`,`deleted`),
  KEY `idx_project_period` (`project_id`,`period_type`,`period_start`),
  KEY `idx_target` (`target_type`,`target_id`),
  KEY `idx_org_unit` (`org_unit_id`),
  KEY `idx_ranking` (`project_id`,`period_type`,`period_start`,`ranking`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V7 周期汇总';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_period_summaries`
--

LOCK TABLES `insp_period_summaries` WRITE;
/*!40000 ALTER TABLE `insp_period_summaries` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_period_summaries` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_plan_inspectors`
--

DROP TABLE IF EXISTS `insp_plan_inspectors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_plan_inspectors` (
  `plan_id` bigint NOT NULL COMMENT '调度组 ID',
  `user_id` bigint NOT NULL COMMENT '检查员 user_id (必须属于 project_inspector)',
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `org_unit_id` bigint DEFAULT NULL COMMENT '数据权限边界, 从 plan 继承',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`plan_id`,`user_id`,`deleted`),
  KEY `idx_plan` (`plan_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='调度组检查员关系表 (替代 inspection_plans.inspector_ids JSON)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_plan_inspectors`
--

LOCK TABLES `insp_plan_inspectors` WRITE;
/*!40000 ALTER TABLE `insp_plan_inspectors` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_plan_inspectors` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_plan_sections`
--

DROP TABLE IF EXISTS `insp_plan_sections`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_plan_sections` (
  `plan_id` bigint NOT NULL,
  `section_id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`plan_id`,`section_id`,`deleted`),
  KEY `idx_plan` (`plan_id`),
  KEY `idx_section` (`section_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='调度组-分区关系表 (替代 insp_inspection_plans.section_ids JSON)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_plan_sections`
--

LOCK TABLES `insp_plan_sections` WRITE;
/*!40000 ALTER TABLE `insp_plan_sections` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_plan_sections` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_policy_calc_rules`
--

DROP TABLE IF EXISTS `insp_policy_calc_rules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_policy_calc_rules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `policy_id` bigint NOT NULL COMMENT '所属评分方案ID',
  `rule_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则编码',
  `rule_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则名称',
  `rule_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则类型',
  `priority` int NOT NULL DEFAULT '0' COMMENT '优先级(升序执行)',
  `config` json DEFAULT NULL COMMENT '规则配置 JSON',
  `is_enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `org_unit_id` bigint DEFAULT NULL COMMENT '数据权限归属组织',
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_calc_rule_policy` (`policy_id`,`priority`),
  KEY `idx_calc_rule_org` (`org_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查平台评分方案-计算规则';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_policy_calc_rules`
--

LOCK TABLES `insp_policy_calc_rules` WRITE;
/*!40000 ALTER TABLE `insp_policy_calc_rules` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_policy_calc_rules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_policy_grade_bands`
--

DROP TABLE IF EXISTS `insp_policy_grade_bands`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_policy_grade_bands` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `policy_id` bigint NOT NULL COMMENT '所属评分方案ID',
  `grade_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '等级编码',
  `grade_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '等级名称',
  `min_percent` decimal(5,2) DEFAULT NULL COMMENT '区间下限(百分比, 90=90%)',
  `max_percent` decimal(5,2) DEFAULT NULL COMMENT '区间上限(百分比, 90=90%)',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `org_unit_id` bigint DEFAULT NULL COMMENT '数据权限归属组织',
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_grade_band_policy` (`policy_id`,`sort_order`),
  KEY `idx_grade_band_org` (`org_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查平台评分方案-等级映射';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_policy_grade_bands`
--

LOCK TABLES `insp_policy_grade_bands` WRITE;
/*!40000 ALTER TABLE `insp_policy_grade_bands` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_policy_grade_bands` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_project_corrective_rules`
--

DROP TABLE IF EXISTS `insp_project_corrective_rules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_project_corrective_rules` (
  `id` bigint NOT NULL COMMENT '雪花 ID',
  `project_id` bigint NOT NULL COMMENT '关联项目 ID',
  `scoring_mode` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评分模式: PASS_FAIL/RATING_SCALE/...',
  `rule_json` json NOT NULL COMMENT '规则 JSON, 同 ItemRule schema',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户 ID',
  `org_unit_id` bigint DEFAULT NULL COMMENT '组织 ID (横切关注点)',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by` bigint DEFAULT NULL,
  `updated_by` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_mode` (`project_id`,`scoring_mode`,`deleted`),
  KEY `idx_project` (`project_id`,`deleted`),
  KEY `idx_org` (`org_unit_id`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目级按题型整改规则 (架构 E)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_project_corrective_rules`
--

LOCK TABLES `insp_project_corrective_rules` WRITE;
/*!40000 ALTER TABLE `insp_project_corrective_rules` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_project_corrective_rules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_project_inspectors`
--

DROP TABLE IF EXISTS `insp_project_inspectors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_project_inspectors` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `project_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `user_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'INSPECTOR' COMMENT 'INSPECTOR|REVIEWER|LEAD',
  `is_active` tinyint NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `org_unit_id` bigint NOT NULL COMMENT '数据权限边界(MetaObjectHandler 自动填充, NOT NULL by V20260508_1)',
  `created_by` bigint DEFAULT NULL COMMENT '记录创建人(SELF 范围用)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_user_role` (`project_id`,`user_id`,`role`,`deleted`),
  KEY `idx_user` (`user_id`),
  KEY `idx_insp_project_inspectors_org_unit` (`org_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V7 检查员池';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_project_inspectors`
--

LOCK TABLES `insp_project_inspectors` WRITE;
/*!40000 ALTER TABLE `insp_project_inspectors` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_project_inspectors` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_project_item_overrides`
--

DROP TABLE IF EXISTS `insp_project_item_overrides`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_project_item_overrides` (
  `id` bigint NOT NULL COMMENT '雪花 ID',
  `project_id` bigint NOT NULL COMMENT '关联项目 ID',
  `template_item_id` bigint NOT NULL COMMENT '关联模板检查项 ID',
  `rule_json` json NOT NULL COMMENT '规则 JSON, 同 ItemRule schema',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户 ID',
  `org_unit_id` bigint DEFAULT NULL COMMENT '组织 ID',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by` bigint DEFAULT NULL,
  `updated_by` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_item` (`project_id`,`template_item_id`,`deleted`),
  KEY `idx_project` (`project_id`,`deleted`),
  KEY `idx_item` (`template_item_id`,`deleted`),
  KEY `idx_org` (`org_unit_id`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目级题目个例整改规则 (架构 E)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_project_item_overrides`
--

LOCK TABLES `insp_project_item_overrides` WRITE;
/*!40000 ALTER TABLE `insp_project_item_overrides` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_project_item_overrides` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_project_scores`
--

DROP TABLE IF EXISTS `insp_project_scores`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_project_scores` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `project_id` bigint NOT NULL,
  `cycle_date` date NOT NULL,
  `score` decimal(10,2) DEFAULT NULL,
  `grade` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `target_count` int DEFAULT '0',
  `detail` json DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  `grade_scheme_display_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '等级方案名称快照',
  `grade_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '等级名称快照',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_insp_project_score_cycle` (`tenant_id`,`project_id`,`cycle_date`),
  KEY `idx_project_date` (`project_id`,`cycle_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_project_scores`
--

LOCK TABLES `insp_project_scores` WRITE;
/*!40000 ALTER TABLE `insp_project_scores` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_project_scores` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_projects`
--

DROP TABLE IF EXISTS `insp_projects`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_projects` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `project_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `project_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `template_id` bigint DEFAULT NULL COMMENT '向后兼容保留，可空；新项目通过计划关联模板',
  `template_version_id` bigint DEFAULT NULL COMMENT '锁定的模板版本',
  `scope_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ORG' COMMENT 'ORG|PLACE|USER|CUSTOM',
  `scope_config` json DEFAULT NULL COMMENT '范围配置(组织/场所/用户ID列表)',
  `target_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ORG' COMMENT 'ORG|PLACE|USER|ASSET',
  `start_date` date NOT NULL,
  `end_date` date DEFAULT NULL,
  `cycle_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DAILY' COMMENT 'DAILY|WEEKLY|BIWEEKLY|MONTHLY|QUARTERLY|CUSTOM',
  `cycle_config` json DEFAULT NULL COMMENT '周期自定义配置',
  `time_slots` json DEFAULT NULL COMMENT '时间段配置',
  `skip_holidays` tinyint NOT NULL DEFAULT '0',
  `holiday_calendar_id` bigint DEFAULT NULL,
  `excluded_dates` json DEFAULT NULL COMMENT '排除日期列表',
  `assignment_mode` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ASSIGNED' COMMENT 'FREE|ASSIGNED|HYBRID|ROUND_ROBIN|LOAD_BALANCED',
  `review_required` tinyint NOT NULL DEFAULT '1',
  `auto_publish` tinyint NOT NULL DEFAULT '0',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT|PUBLISHED|PAUSED|COMPLETED|ARCHIVED',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `parent_project_id` bigint DEFAULT NULL,
  `inspection_mode` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PLANNED' COMMENT 'PLANNED/SPOT_CHECK/HYBRID/SELF_AUDIT/EMERGENCY',
  `allow_ad_hoc` tinyint NOT NULL DEFAULT '0' COMMENT '是否允许检查员临时抽查',
  `allow_self_check` tinyint NOT NULL DEFAULT '0' COMMENT '是否允许受检主体自查',
  `ad_hoc_quota_per_inspector` int DEFAULT NULL COMMENT '月度抽查配额, NULL=无限',
  `corrective_strictness` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NORMAL' COMMENT '整改严格度: STRICT/NORMAL/LENIENT/OFF',
  `corrective_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '整改引擎总开关 (取代 OFF strictness)',
  `corrective_strictness_adj` int NOT NULL DEFAULT '0' COMMENT '整体严格度调档 -2~+2 (升降一档)',
  `corrective_auto_create_level` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NONE' COMMENT '自动建单门槛: HIGH/MEDIUM/LOW/NONE',
  `corrective_severity_thresholds` json DEFAULT NULL COMMENT '严重度阈值覆盖, e.g. {"high":0.8,"medium":0.5,"low":0.3}',
  `corrective_default_deadlines` json DEFAULT NULL COMMENT '默认 deadline 天数, e.g. {"high":3,"medium":7,"low":14}',
  `max_reject_count` int DEFAULT NULL COMMENT '任务自动驳回上限, NULL=系统默认 3',
  `max_escalation_level` int DEFAULT NULL COMMENT '整改自动升级上限, NULL=系统默认 3',
  `appeal_window_days` int DEFAULT NULL COMMENT '申诉时效 (从 task 发布起的天数), NULL=系统默认 7',
  `org_unit_id` bigint NOT NULL COMMENT '数据权限边界(MetaObjectHandler 自动填充, NOT NULL by V20260508_1)',
  `scoring_config_snapshot` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_code` (`project_code`,`deleted`),
  KEY `idx_template` (`template_id`),
  KEY `idx_status` (`status`),
  KEY `idx_dates` (`start_date`,`end_date`),
  KEY `idx_parent_project` (`parent_project_id`),
  KEY `idx_insp_projects_org_unit` (`org_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V7 检查项目';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_projects`
--

LOCK TABLES `insp_projects` WRITE;
/*!40000 ALTER TABLE `insp_projects` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_projects` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_rating_dimensions`
--

DROP TABLE IF EXISTS `insp_rating_dimensions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_rating_dimensions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `project_id` bigint NOT NULL,
  `dimension_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `section_ids` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'JSON: 关联的分区ID列表',
  `aggregation` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'WEIGHTED_AVG' COMMENT 'WEIGHTED_AVG/SUM/AVG/MAX/MIN',
  `grade_bands` text COLLATE utf8mb4_unicode_ci COMMENT 'JSON: [{code,name,minScore,maxScore,color}]',
  `award_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '奖项名称',
  `ranking_enabled` tinyint DEFAULT '1',
  `sort_order` int DEFAULT '0',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级维度';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_rating_dimensions`
--

LOCK TABLES `insp_rating_dimensions` WRITE;
/*!40000 ALTER TABLE `insp_rating_dimensions` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_rating_dimensions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_rating_links`
--

DROP TABLE IF EXISTS `insp_rating_links`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_rating_links` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `project_id` bigint NOT NULL COMMENT 'V7 检查项目 ID',
  `rating_config_id` bigint NOT NULL COMMENT '评级配置 ID (rating_configs)',
  `period_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '周期类型: WEEKLY, MONTHLY',
  `auto_calculate` tinyint NOT NULL DEFAULT '1' COMMENT '是否自动计算: 1=是 0=否',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_config` (`project_id`,`rating_config_id`,`deleted`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_rating_config_id` (`rating_config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V7 检查项目-评级配置关联';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_rating_links`
--

LOCK TABLES `insp_rating_links` WRITE;
/*!40000 ALTER TABLE `insp_rating_links` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_rating_links` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_rating_results`
--

DROP TABLE IF EXISTS `insp_rating_results`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_rating_results` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `dimension_id` bigint NOT NULL,
  `target_id` bigint NOT NULL,
  `target_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `target_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ORG/PLACE/USER',
  `cycle_date` date NOT NULL,
  `score` decimal(10,2) DEFAULT NULL,
  `grade` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `rank_no` int DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_dim_date` (`dimension_id`,`cycle_date`),
  KEY `idx_target` (`target_type`,`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级结果';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_rating_results`
--

LOCK TABLES `insp_rating_results` WRITE;
/*!40000 ALTER TABLE `insp_rating_results` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_rating_results` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_response_set_options`
--

DROP TABLE IF EXISTS `insp_response_set_options`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_response_set_options` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `response_set_id` bigint NOT NULL,
  `option_value` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `option_label` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `option_color` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `score` decimal(10,2) DEFAULT NULL,
  `is_flagged` tinyint NOT NULL DEFAULT '0',
  `sort_order` int NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_response_set` (`response_set_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V7 选项集选项';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_response_set_options`
--

LOCK TABLES `insp_response_set_options` WRITE;
/*!40000 ALTER TABLE `insp_response_set_options` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_response_set_options` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_response_sets`
--

DROP TABLE IF EXISTS `insp_response_sets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_response_sets` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `set_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `set_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_global` tinyint NOT NULL DEFAULT '0',
  `is_enabled` tinyint NOT NULL DEFAULT '1',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_code` (`tenant_id`,`set_code`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V7 可复用选项集';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_response_sets`
--

LOCK TABLES `insp_response_sets` WRITE;
/*!40000 ALTER TABLE `insp_response_sets` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_response_sets` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_score_dimensions`
--

DROP TABLE IF EXISTS `insp_score_dimensions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_score_dimensions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `scoring_profile_id` bigint NOT NULL,
  `dimension_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `dimension_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `weight` int NOT NULL DEFAULT '100',
  `base_score` decimal(10,2) NOT NULL DEFAULT '100.00',
  `pass_threshold` decimal(10,2) DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `source_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'SECTION' COMMENT '维度来源: SECTION/MODULE',
  `module_template_id` bigint DEFAULT NULL COMMENT '关联的子模板ID(source_type=MODULE时)',
  PRIMARY KEY (`id`),
  KEY `idx_profile` (`scoring_profile_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V7 评分维度';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_score_dimensions`
--

LOCK TABLES `insp_score_dimensions` WRITE;
/*!40000 ALTER TABLE `insp_score_dimensions` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_score_dimensions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_scoring_policies`
--

DROP TABLE IF EXISTS `insp_scoring_policies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_scoring_policies` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `policy_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '方案编码',
  `policy_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '方案名称',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '方案描述',
  `precision_digits` int NOT NULL DEFAULT '2' COMMENT '小数精度位数',
  `is_system` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否系统内置',
  `is_enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `org_unit_id` bigint DEFAULT NULL COMMENT '数据权限归属组织',
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_policy_code` (`tenant_id`,`policy_code`,`deleted`),
  KEY `idx_scoring_policy_enabled` (`is_enabled`,`sort_order`),
  KEY `idx_scoring_policy_org` (`org_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查平台评分方案';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_scoring_policies`
--

LOCK TABLES `insp_scoring_policies` WRITE;
/*!40000 ALTER TABLE `insp_scoring_policies` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_scoring_policies` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_scoring_presets`
--

DROP TABLE IF EXISTS `insp_scoring_presets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_scoring_presets` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `template_id` bigint NOT NULL,
  `preset_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `preset_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'FULL_PASS|FULL_FAIL|CUSTOM',
  `item_values` json NOT NULL COMMENT '[{itemId:1,value:"合格",score:10},...]',
  `usage_count` int DEFAULT '0',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评分预设';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_scoring_presets`
--

LOCK TABLES `insp_scoring_presets` WRITE;
/*!40000 ALTER TABLE `insp_scoring_presets` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_scoring_presets` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_scoring_profile_versions`
--

DROP TABLE IF EXISTS `insp_scoring_profile_versions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_scoring_profile_versions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `profile_id` bigint NOT NULL,
  `version` int NOT NULL,
  `snapshot` json NOT NULL COMMENT '完整配置快照(dimensions+rules+bands+escalation)',
  `published_at` datetime NOT NULL,
  `published_by` bigint DEFAULT NULL,
  `change_summary` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_profile_version` (`profile_id`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评分配置版本快照';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_scoring_profile_versions`
--

LOCK TABLES `insp_scoring_profile_versions` WRITE;
/*!40000 ALTER TABLE `insp_scoring_profile_versions` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_scoring_profile_versions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_scoring_profiles`
--

DROP TABLE IF EXISTS `insp_scoring_profiles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_scoring_profiles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `section_id` bigint NOT NULL,
  `project_id` bigint NOT NULL COMMENT '项目-owned: profile 必属一项目, 跨项目不共享',
  `max_score` decimal(10,2) NOT NULL DEFAULT '100.00',
  `min_score` decimal(10,2) NOT NULL DEFAULT '0.00',
  `precision_digits` int NOT NULL DEFAULT '2',
  `normalize_by` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NONE' COMMENT '归一化分母维度 NONE|PER_MEMBER|PER_PLACE|PER_SUB_ORG',
  `normalization_mode` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NONE' COMMENT '归一化模式 NONE|PER_CAPITA|SQRT_ADJUSTED',
  `baseline_population` int NOT NULL DEFAULT '1' COMMENT '归一化基准人口/规模',
  `norm_floor` decimal(10,4) DEFAULT NULL COMMENT '归一化后下限(NULL=不限)',
  `norm_cap` decimal(10,4) DEFAULT NULL COMMENT '归一化后上限(NULL=不限)',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `current_version` int DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template` (`section_id`,`deleted`),
  UNIQUE KEY `uk_scoring_profile_project_section` (`project_id`,`section_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V7 评分配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_scoring_profiles`
--

LOCK TABLES `insp_scoring_profiles` WRITE;
/*!40000 ALTER TABLE `insp_scoring_profiles` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_scoring_profiles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_sensor_readings`
--

DROP TABLE IF EXISTS `insp_sensor_readings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_sensor_readings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sensor_id` bigint NOT NULL,
  `reading_value` decimal(10,2) NOT NULL,
  `reading_unit` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `recorded_at` datetime NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_sensor_time` (`sensor_id`,`recorded_at`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='传感器读数';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_sensor_readings`
--

LOCK TABLES `insp_sensor_readings` WRITE;
/*!40000 ALTER TABLE `insp_sensor_readings` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_sensor_readings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_submission_details`
--

DROP TABLE IF EXISTS `insp_submission_details`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_submission_details` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `submission_id` bigint NOT NULL,
  `template_item_id` bigint NOT NULL,
  `item_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `item_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `section_id` bigint DEFAULT NULL,
  `section_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `item_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `response_value` json DEFAULT NULL COMMENT '响应值',
  `scoring_mode` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'DEDUCTION|ADDITION|DIRECT|PASS_FAIL',
  `scoring_config` text COLLATE utf8mb4_unicode_ci,
  `validation_rules` text COLLATE utf8mb4_unicode_ci,
  `condition_logic` text COLLATE utf8mb4_unicode_ci,
  `score` decimal(10,2) DEFAULT NULL,
  `dimensions` json DEFAULT NULL COMMENT '影响的维度',
  `is_flagged` tinyint NOT NULL DEFAULT '0',
  `flag_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` text COLLATE utf8mb4_unicode_ci,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `appeal_adjusted_at` datetime DEFAULT NULL COMMENT '申诉调整幂等标记: 非空表示已应用过申诉调整',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `item_weight` decimal(5,2) DEFAULT '1.00' COMMENT '快照:提交时的项目权重',
  `time_spent_seconds` int DEFAULT NULL COMMENT '该项耗时(秒)',
  `org_unit_id` bigint NOT NULL COMMENT '数据权限边界(MetaObjectHandler 自动填充, NOT NULL by V20260508_1)',
  `created_by` bigint DEFAULT NULL COMMENT '记录创建人(SELF 范围用)',
  PRIMARY KEY (`id`),
  KEY `idx_submission` (`submission_id`),
  KEY `idx_template_item` (`template_item_id`),
  KEY `idx_flagged` (`is_flagged`),
  KEY `idx_insp_submission_details_org_unit` (`org_unit_id`),
  KEY `idx_isd_sub_mode` (`submission_id`,`scoring_mode`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V7 提交明细(每个评分项)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_submission_details`
--

LOCK TABLES `insp_submission_details` WRITE;
/*!40000 ALTER TABLE `insp_submission_details` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_submission_details` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_submission_observations`
--

DROP TABLE IF EXISTS `insp_submission_observations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_submission_observations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `submission_id` bigint NOT NULL COMMENT '所属提交',
  `detail_id` bigint NOT NULL COMMENT '所属检查项明细',
  `project_id` bigint DEFAULT NULL COMMENT '项目ID(冗余)',
  `task_id` bigint DEFAULT NULL COMMENT '任务ID(冗余)',
  `item_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `item_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `item_type` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `section_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `subject_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'USER/ORG_UNIT/PLACE',
  `subject_id` bigint NOT NULL,
  `subject_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `org_unit_id` bigint NOT NULL COMMENT '数据权限边界(MetaObjectHandler 自动填充, NOT NULL by V20260508_1)',
  `org_unit_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '主体所属组织名称(冗余, 通用核心)',
  `score` decimal(10,2) NOT NULL DEFAULT '0.00',
  `is_negative` tinyint NOT NULL DEFAULT '0',
  `severity` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'LOW/MEDIUM/HIGH/CRITICAL',
  `is_flagged` tinyint NOT NULL DEFAULT '0',
  `linked_event_type_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `response_value` text COLLATE utf8mb4_unicode_ci,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `observed_at` datetime NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  `created_by` bigint DEFAULT NULL COMMENT '记录创建人(SELF 范围用)',
  PRIMARY KEY (`id`),
  KEY `idx_obs_submission` (`submission_id`),
  KEY `idx_obs_detail` (`detail_id`),
  KEY `idx_obs_subject` (`subject_type`,`subject_id`),
  KEY `idx_obs_class` (`org_unit_id`),
  KEY `idx_obs_project` (`project_id`),
  KEY `idx_obs_negative` (`is_negative`,`severity`),
  KEY `idx_obs_event_type` (`linked_event_type_code`),
  KEY `idx_obs_observed` (`observed_at`),
  KEY `idx_insp_submission_observations_org_unit` (`org_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评分观察记录(检查平台归一化读模型)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_submission_observations`
--

LOCK TABLES `insp_submission_observations` WRITE;
/*!40000 ALTER TABLE `insp_submission_observations` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_submission_observations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_submissions`
--

DROP TABLE IF EXISTS `insp_submissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_submissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `task_id` bigint NOT NULL,
  `target_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ORG|PLACE|USER|ASSET',
  `target_id` bigint NOT NULL,
  `target_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `org_unit_id` bigint NOT NULL COMMENT '数据权限边界(MetaObjectHandler 自动填充, NOT NULL by V20260508_1)',
  `org_unit_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `weight_ratio` decimal(5,2) NOT NULL DEFAULT '1.00' COMMENT '权重系数',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING|LOCKED|IN_PROGRESS|COMPLETED|SKIPPED',
  `form_data` json DEFAULT NULL COMMENT '完整表单响应',
  `score_breakdown` json DEFAULT NULL COMMENT '各维度分数',
  `base_score` decimal(10,2) DEFAULT NULL,
  `final_score` decimal(10,2) DEFAULT NULL,
  `deduction_total` decimal(10,2) NOT NULL DEFAULT '0.00',
  `bonus_total` decimal(10,2) NOT NULL DEFAULT '0.00',
  `grade` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `passed` tinyint DEFAULT NULL,
  `sync_version` int NOT NULL DEFAULT '1' COMMENT '离线同步版本号',
  `completed_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `scoring_profile_version` int DEFAULT NULL COMMENT '使用的评分配置版本',
  `total_time_seconds` int DEFAULT NULL COMMENT '总耗时(秒)',
  `nfc_tag_uid` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `checkin_time` datetime DEFAULT NULL,
  `section_id` bigint DEFAULT NULL COMMENT '所属一级分区（有targetType的）',
  `root_target_id` bigint DEFAULT NULL COMMENT '根目标ID(用于按部门/系分组)',
  `root_target_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '根目标名称',
  `created_by` bigint DEFAULT NULL COMMENT '记录创建人(SELF 范围用)',
  `closed_at` datetime DEFAULT NULL,
  `closed_reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_task` (`task_id`),
  KEY `idx_target` (`target_type`,`target_id`),
  KEY `idx_org_unit` (`org_unit_id`),
  KEY `idx_status` (`status`),
  KEY `idx_submissions_section` (`section_id`),
  KEY `idx_submissions_root_target` (`root_target_id`),
  KEY `idx_isub_task_status` (`task_id`,`status`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V7 检查提交(每个检查目标)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_submissions`
--

LOCK TABLES `insp_submissions` WRITE;
/*!40000 ALTER TABLE `insp_submissions` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_submissions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_task_section_assignments`
--

DROP TABLE IF EXISTS `insp_task_section_assignments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_task_section_assignments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `task_id` bigint NOT NULL,
  `section_id` bigint NOT NULL,
  `inspector_id` bigint NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING',
  `started_at` datetime DEFAULT NULL,
  `completed_at` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务分区分配';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_task_section_assignments`
--

LOCK TABLES `insp_task_section_assignments` WRITE;
/*!40000 ALTER TABLE `insp_task_section_assignments` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_task_section_assignments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_tasks`
--

DROP TABLE IF EXISTS `insp_tasks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_tasks` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `task_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `project_id` bigint NOT NULL,
  `task_date` date DEFAULT NULL COMMENT '计划检查日期 (SCHEDULED 必填, AD_HOC/SELF_CHECK 可空)',
  `time_slot_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `time_slot_start` time DEFAULT NULL,
  `time_slot_end` time DEFAULT NULL,
  `inspector_id` bigint DEFAULT NULL,
  `inspector_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reviewer_id` bigint DEFAULT NULL,
  `reviewer_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING|CLAIMED|IN_PROGRESS|SUBMITTED|UNDER_REVIEW|REVIEWED|PUBLISHED|CANCELLED|EXPIRED',
  `total_targets` int NOT NULL DEFAULT '0',
  `completed_targets` int NOT NULL DEFAULT '0',
  `skipped_targets` int NOT NULL DEFAULT '0',
  `submitted_at` datetime DEFAULT NULL,
  `late_submission` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否延迟提交 (提交时 today > effective_due_date 即为 1)',
  `late_days` int NOT NULL DEFAULT '0' COMMENT '延迟天数 (0=按时, >0=延迟)',
  `reviewed_at` datetime DEFAULT NULL,
  `published_at` datetime DEFAULT NULL,
  `review_comment` text COLLATE utf8mb4_unicode_ci,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `collaboration_mode` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'SINGLE' COMMENT 'SINGLE|SECTION_SPLIT|FULL_PARALLEL',
  `execution_started_at` datetime DEFAULT NULL,
  `execution_ended_at` datetime DEFAULT NULL,
  `assigned_section_ids` text COLLATE utf8mb4_unicode_ci COMMENT 'JSON: 分配的分区ID（空=全部）',
  `assigned_target_ids` text COLLATE utf8mb4_unicode_ci COMMENT 'JSON: 分配的目标ID（空=全部）',
  `inspection_plan_id` bigint DEFAULT NULL COMMENT '关联的检查计划',
  `task_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SCHEDULED' COMMENT 'SCHEDULED/AD_HOC/TRIGGERED/SELF_CHECK/COMPLAINT/CROSS_AUDIT',
  `source_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'SCHEDULER/MANUAL/EVENT/IMPORT',
  `source_actor_id` bigint DEFAULT NULL COMMENT '触发用户ID',
  `source_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '发起原因/说明',
  `source_ref_type` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '来源单据类型 Appeal/Alert/Complaint',
  `source_ref_id` bigint DEFAULT NULL COMMENT '来源单据ID',
  `deadline_policy` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'STRICT' COMMENT 'STRICT(逾期硬扣)/RELAXED(软提醒)/NONE(永不逾期)',
  `rejection_count` int NOT NULL DEFAULT '0' COMMENT '驳回次数, 超过上限须管理员介入',
  `extended_to` date DEFAULT NULL COMMENT '驳回延期到的有效日期, 为空表示无延期',
  `org_unit_id` bigint NOT NULL COMMENT '数据权限边界(MetaObjectHandler 自动填充, NOT NULL by V20260508_1)',
  `created_by` bigint DEFAULT NULL COMMENT '记录创建人(SELF 范围用)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_code` (`task_code`,`deleted`),
  UNIQUE KEY `uk_insp_task_source_ref` (`source_ref_type`,`source_ref_id`),
  KEY `idx_project` (`project_id`),
  KEY `idx_inspector` (`inspector_id`),
  KEY `idx_task_date` (`task_date`),
  KEY `idx_status` (`status`),
  KEY `idx_tasks_plan` (`inspection_plan_id`),
  KEY `idx_type_status` (`task_type`,`status`),
  KEY `idx_insp_tasks_org_unit` (`org_unit_id`),
  KEY `idx_insp_tasks_late_submission` (`late_submission`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V7 检查任务';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_tasks`
--

LOCK TABLES `insp_tasks` WRITE;
/*!40000 ALTER TABLE `insp_tasks` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_tasks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_template_catalogs`
--

DROP TABLE IF EXISTS `insp_template_catalogs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_template_catalogs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `parent_id` bigint DEFAULT NULL COMMENT '父分类ID，NULL=根分类',
  `catalog_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `catalog_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `icon` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `is_enabled` tinyint NOT NULL DEFAULT '1',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_code` (`tenant_id`,`catalog_code`,`deleted`),
  KEY `idx_tenant_parent` (`tenant_id`,`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V7 模板分类目录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_template_catalogs`
--

LOCK TABLES `insp_template_catalogs` WRITE;
/*!40000 ALTER TABLE `insp_template_catalogs` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_template_catalogs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_template_items`
--

DROP TABLE IF EXISTS `insp_template_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_template_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `section_id` bigint NOT NULL,
  `item_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `item_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `item_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `config` json DEFAULT NULL COMMENT '类型特定配置',
  `validation_rules` json DEFAULT NULL COMMENT '验证规则数组',
  `response_set_id` bigint DEFAULT NULL,
  `scoring_config` json DEFAULT NULL COMMENT '评分+归一化配置',
  `item_rule` json DEFAULT NULL COMMENT '整改规则: {criticality, neverCorrect, baseSeverityMap, deadlineOverrideDays}',
  `dimension_id` bigint DEFAULT NULL COMMENT '所属评分维度ID',
  `help_content` text COLLATE utf8mb4_unicode_ci COMMENT '帮助文本/参考图片URL',
  `is_required` tinyint NOT NULL DEFAULT '0',
  `is_scored` tinyint NOT NULL DEFAULT '0',
  `require_evidence` tinyint NOT NULL DEFAULT '0' COMMENT '非媒体字段是否强制附证据',
  `sort_order` int NOT NULL DEFAULT '0',
  `condition_logic` text COLLATE utf8mb4_unicode_ci,
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `item_weight` decimal(5,2) DEFAULT '1.00' COMMENT '项目权重(维度内)',
  `library_item_id` bigint DEFAULT NULL COMMENT '来源库项目ID',
  `sync_with_library` tinyint DEFAULT '0' COMMENT '是否与库同步',
  `visibility_logic` json DEFAULT NULL COMMENT '显示条件(条件逻辑V2格式)',
  `scoring_logic` json DEFAULT NULL COMMENT '计分条件(条件逻辑V2格式)',
  `input_mode` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT 'INLINE' COMMENT '输入模式: INLINE(默认内嵌) | EVENT_STREAM(事件流快速录入)',
  `linked_event_type_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联事件类型编码(检查扣分时作为eventTypeHint传入触发器)',
  `corrective_override` json DEFAULT NULL COMMENT '检查项级整改覆盖规则, JSON. neverCorrect/forceCorrect/thresholdOverride/deadlineOverride',
  PRIMARY KEY (`id`),
  KEY `idx_section` (`section_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V7 模板字段（22种类型）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_template_items`
--

LOCK TABLES `insp_template_items` WRITE;
/*!40000 ALTER TABLE `insp_template_items` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_template_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_template_sections`
--

DROP TABLE IF EXISTS `insp_template_sections`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_template_sections` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `template_id` bigint DEFAULT NULL,
  `section_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `section_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `weight` int NOT NULL DEFAULT '100',
  `is_repeatable` tinyint NOT NULL DEFAULT '0',
  `condition_logic` text COLLATE utf8mb4_unicode_ci,
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `target_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '检查目标类型: ORG/PLACE/USER (仅一级分区)',
  `description` text COLLATE utf8mb4_unicode_ci,
  `tags` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `catalog_id` bigint DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/DEPRECATED/ARCHIVED',
  `latest_version` int DEFAULT '0',
  `inspection_mode` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'SNAPSHOT' COMMENT 'SNAPSHOT/CONTINUOUS',
  `continuous_start` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '持续模式开始时间 HH:mm',
  `continuous_end` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '持续模式结束时间 HH:mm',
  `target_source_mode` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'INDEPENDENT=根分区从项目范围/PARENT_ASSOCIATED=从父目标派生',
  `target_type_filter` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '类型过滤: org_type=班级, place_category=宿舍, user_type=学生',
  `input_mode` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT 'INLINE' COMMENT '输入模式: INLINE(结构化检查) | EVENT_STREAM(巡查快速记录)',
  `grade_scheme_id` bigint DEFAULT NULL COMMENT 'FK → insp_grade_schemes.id',
  `parent_section_id` bigint DEFAULT NULL,
  `scoring_config` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  KEY `idx_template` (`template_id`),
  KEY `idx_sections_catalog` (`catalog_id`),
  KEY `idx_sections_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V7 模板分区（层级）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_template_sections`
--

LOCK TABLES `insp_template_sections` WRITE;
/*!40000 ALTER TABLE `insp_template_sections` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_template_sections` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_template_versions`
--

DROP TABLE IF EXISTS `insp_template_versions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_template_versions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `template_id` bigint NOT NULL,
  `version` int NOT NULL,
  `structure_snapshot` json NOT NULL COMMENT '完整 sections+items 树',
  `scoring_profile_snapshot` json DEFAULT NULL COMMENT '评分配置快照',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_version` (`template_id`,`version`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V7 模板版本快照（不可变）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_template_versions`
--

LOCK TABLES `insp_template_versions` WRITE;
/*!40000 ALTER TABLE `insp_template_versions` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_template_versions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_templates`
--

DROP TABLE IF EXISTS `insp_templates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_templates` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `template_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `template_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `catalog_id` bigint DEFAULT NULL,
  `tags` json DEFAULT NULL COMMENT '["安全","卫生"]',
  `latest_version` int NOT NULL DEFAULT '0',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT',
  `is_default` tinyint NOT NULL DEFAULT '0',
  `use_count` int NOT NULL DEFAULT '0',
  `last_used_at` datetime DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `target_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'ORG' COMMENT '检查目标类型: ORG/PLACE/USER/ASSET',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_code` (`tenant_id`,`template_code`,`deleted`),
  KEY `idx_tenant_status` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V7 检查模板';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_templates`
--

LOCK TABLES `insp_templates` WRITE;
/*!40000 ALTER TABLE `insp_templates` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_templates` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insp_violation_records`
--

DROP TABLE IF EXISTS `insp_violation_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insp_violation_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `submission_id` bigint NOT NULL,
  `submission_detail_id` bigint NOT NULL,
  `section_id` bigint DEFAULT NULL,
  `item_id` bigint DEFAULT NULL,
  `user_id` bigint NOT NULL COMMENT '违纪人员',
  `user_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `class_info` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '班级/部门信息',
  `occurred_at` datetime NOT NULL,
  `severity` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'MINOR' COMMENT 'MINOR/MODERATE/SEVERE',
  `description` text COLLATE utf8mb4_unicode_ci,
  `evidence_urls` text COLLATE utf8mb4_unicode_ci COMMENT 'JSON: 证据图片URL列表',
  `score` decimal(10,2) DEFAULT NULL COMMENT '扣分值',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  `org_unit_id` bigint NOT NULL COMMENT '数据权限边界(MetaObjectHandler 自动填充, NOT NULL by V20260508_1)',
  PRIMARY KEY (`id`),
  KEY `idx_submission` (`submission_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_date` (`occurred_at`),
  KEY `idx_insp_violation_records_org_unit` (`org_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='违纪记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insp_violation_records`
--

LOCK TABLES `insp_violation_records` WRITE;
/*!40000 ALTER TABLE `insp_violation_records` DISABLE KEYS */;
/*!40000 ALTER TABLE `insp_violation_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inspection_appeals`
--

DROP TABLE IF EXISTS `inspection_appeals`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inspection_appeals` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `org_unit_id` bigint DEFAULT NULL COMMENT '组织单元 ID, 用于数据权限过滤',
  `appeal_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '申诉编号',
  `submission_detail_id` bigint NOT NULL COMMENT '关联的扣分明细 ID',
  `submission_id` bigint DEFAULT NULL COMMENT '冗余: 关联的 submission ID',
  `task_id` bigint DEFAULT NULL COMMENT '冗余: 关联的 task ID',
  `project_id` bigint DEFAULT NULL COMMENT '冗余: 关联的 project ID',
  `subject_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '申诉主体类型 USER/ORG/PLACE/ASSET',
  `subject_id` bigint DEFAULT NULL COMMENT '申诉主体 ID',
  `submitter_user_id` bigint NOT NULL COMMENT '提交申诉的用户 ID',
  `submitter_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reason` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '申诉理由',
  `attachments` json DEFAULT NULL COMMENT '证据附件 URL 列表',
  `expected_adjustment` decimal(10,2) DEFAULT NULL COMMENT '期望分数调整',
  `final_adjustment` decimal(10,2) DEFAULT NULL COMMENT '审核后实际分数调整',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING|APPROVED|REJECTED|WITHDRAWN',
  `reviewer_id` bigint DEFAULT NULL,
  `reviewer_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reviewer_comment` text COLLATE utf8mb4_unicode_ci,
  `reviewed_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `pending_lock_key` bigint GENERATED ALWAYS AS ((case when ((`status` = _utf8mb4'PENDING') and (`deleted` = 0)) then `submission_detail_id` else NULL end)) STORED COMMENT '并发锁键: 同 detail 仅一条 PENDING — 借 UNIQUE+NULL 实现',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_appeal_code` (`appeal_code`,`deleted`),
  UNIQUE KEY `uk_appeal_pending_lock` (`pending_lock_key`),
  KEY `idx_submission_detail` (`submission_detail_id`),
  KEY `idx_submitter` (`submitter_user_id`),
  KEY `idx_status_created` (`status`,`created_at`),
  KEY `idx_org_unit` (`org_unit_id`),
  KEY `idx_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查申诉 (P1#8)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inspection_appeals`
--

LOCK TABLES `inspection_appeals` WRITE;
/*!40000 ALTER TABLE `inspection_appeals` DISABLE KEYS */;
/*!40000 ALTER TABLE `inspection_appeals` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inspection_audit_logs`
--

DROP TABLE IF EXISTS `inspection_audit_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inspection_audit_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `entity_type` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '实体类型: CorrectiveCase / InspAppeal / InspTask / InspProject',
  `entity_id` bigint NOT NULL COMMENT '实体 ID',
  `entity_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业务编号 (caseCode/appealCode/taskCode/projectCode)',
  `action` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '动作: APPEAL_APPROVED/APPEAL_REJECTED/CASE_UNASSIGNED/TASK_REJECTED/TASK_DEADLINE_EXTENDED/PROJECT_TEMPLATE_UPGRADED 等',
  `actor_user_id` bigint DEFAULT NULL COMMENT '操作人 ID (system 自动则 null)',
  `actor_user_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `org_unit_id` bigint DEFAULT NULL COMMENT 'actor 主归属组织 / entity 所在组织 (I4 加, 数据权限收窄用)',
  `reason` text COLLATE utf8mb4_unicode_ci COMMENT '动作原因 / 备注',
  `payload` json DEFAULT NULL COMMENT '动作上下文 (前后值/相关 ID 等)',
  `occurred_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_entity` (`entity_type`,`entity_id`),
  KEY `idx_actor` (`actor_user_id`),
  KEY `idx_action_time` (`action`,`occurred_at`),
  KEY `idx_org_action` (`org_unit_id`,`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查平台高敏感动作审计日志 (review #14)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inspection_audit_logs`
--

LOCK TABLES `inspection_audit_logs` WRITE;
/*!40000 ALTER TABLE `inspection_audit_logs` DISABLE KEYS */;
/*!40000 ALTER TABLE `inspection_audit_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `leave_requests`
--

DROP TABLE IF EXISTS `leave_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `leave_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `leave_type` tinyint NOT NULL COMMENT '1事假 2病假 3公假',
  `start_date` date NOT NULL COMMENT '开始日期',
  `end_date` date NOT NULL COMMENT '结束日期',
  `start_period` int DEFAULT NULL COMMENT '开始节次',
  `end_period` int DEFAULT NULL COMMENT '结束节次',
  `reason` varchar(500) NOT NULL COMMENT '请假原因',
  `attachment_urls` json DEFAULT NULL COMMENT '附件（病假条等）',
  `approval_status` tinyint DEFAULT '0' COMMENT '0待审批 1已通过 2已拒绝',
  `approver_id` bigint DEFAULT NULL COMMENT '审批人',
  `approval_time` datetime DEFAULT NULL COMMENT '审批时间',
  `approval_comment` varchar(200) DEFAULT NULL COMMENT '审批意见',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_student` (`student_id`),
  KEY `idx_date` (`start_date`,`end_date`),
  KEY `idx_status` (`approval_status`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='请假申请表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `leave_requests`
--

LOCK TABLES `leave_requests` WRITE;
/*!40000 ALTER TABLE `leave_requests` DISABLE KEYS */;
/*!40000 ALTER TABLE `leave_requests` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `login_logs`
--

DROP TABLE IF EXISTS `login_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `login_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `user_id` bigint DEFAULT NULL COMMENT 'User ID (NULL if login failed)',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Username',
  `login_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PASSWORD' COMMENT 'Login Type (PASSWORD/WECHAT/SSO)',
  `login_status` tinyint NOT NULL COMMENT 'Status (1=Success,0=Failed)',
  `login_message` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Login Message',
  `ip_address` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'IP Address',
  `user_agent` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'User Agent',
  `browser` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Browser',
  `os` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Operating System',
  `device_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Device Type (PC/MOBILE/TABLET)',
  `location` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Location',
  `session_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Session ID',
  `logout_time` datetime DEFAULT NULL COMMENT 'Logout Time',
  `logout_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Logout Type (MANUAL/TIMEOUT/FORCED)',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Login Time',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_username` (`username`),
  KEY `idx_login_status` (`login_status`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_ip_address` (`ip_address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Login Logs';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `login_logs`
--

LOCK TABLES `login_logs` WRITE;
/*!40000 ALTER TABLE `login_logs` DISABLE KEYS */;
/*!40000 ALTER TABLE `login_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `major_categories`
--

DROP TABLE IF EXISTS `major_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `major_categories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '大类编码',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '大类名称',
  `sort_order` int NOT NULL DEFAULT '0',
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专业大类';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `major_categories`
--

LOCK TABLES `major_categories` WRITE;
/*!40000 ALTER TABLE `major_categories` DISABLE KEYS */;
INSERT INTO `major_categories` VALUES (1,'01','机械类',1,1,'2026-06-01 22:56:36','2026-06-01 22:56:36',0,1),(2,'02','电工电子类',2,1,'2026-06-01 22:56:36','2026-06-01 22:56:36',0,1),(3,'03','信息类',3,1,'2026-06-01 22:56:36','2026-06-01 22:56:36',0,1),(4,'04','交通类',4,1,'2026-06-01 22:56:36','2026-06-01 22:56:36',0,1),(5,'05','服务类',5,1,'2026-06-01 22:56:36','2026-06-01 22:56:36',0,1),(6,'06','财经商贸类',6,1,'2026-06-01 22:56:36','2026-06-01 22:56:36',0,1),(7,'07','文化艺术类',7,1,'2026-06-01 22:56:36','2026-06-01 22:56:36',0,1),(8,'08','医药卫生类',8,1,'2026-06-01 22:56:36','2026-06-01 22:56:36',0,1),(9,'09','化工类',9,1,'2026-06-01 22:56:36','2026-06-01 22:56:36',0,1),(10,'10','轻工类',10,1,'2026-06-01 22:56:36','2026-06-01 22:56:36',0,1),(11,'11','建筑类',11,1,'2026-06-01 22:56:36','2026-06-01 22:56:36',0,1),(12,'12','农业类',12,1,'2026-06-01 22:56:36','2026-06-01 22:56:36',0,1),(13,'13','服装类',13,1,'2026-06-01 22:56:36','2026-06-01 22:56:36',0,1),(14,'14','其他',14,1,'2026-06-01 22:56:36','2026-06-01 22:56:36',0,1);
/*!40000 ALTER TABLE `major_categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `major_directions`
--

DROP TABLE IF EXISTS `major_directions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `major_directions` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `direction_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '方向编码',
  `level` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '培养层次',
  `years` int DEFAULT NULL COMMENT '学制年数',
  `is_segmented` tinyint DEFAULT '0' COMMENT '是否分段培养',
  `phase1_level` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '第一阶段层次',
  `phase1_years` int DEFAULT NULL COMMENT '第一阶段年数',
  `phase2_level` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '第二阶段层次',
  `phase2_years` int DEFAULT NULL COMMENT '第二阶段年数',
  `direction_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '方向名称',
  `major_id` bigint NOT NULL COMMENT '所属专业ID',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '方向描述',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `status` tinyint DEFAULT '1' COMMENT '状态:0禁用,1启用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `remarks` text COLLATE utf8mb4_unicode_ci,
  `enrollment_target` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `education_form` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `certificate_names` json DEFAULT NULL COMMENT 'List<String> stored as JSON',
  `training_standard` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cooperation_enterprise` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `max_enrollment` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_direction_code` (`direction_code`),
  KEY `idx_major_id` (`major_id`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_tenant` (`tenant_id`),
  CONSTRAINT `fk_direction_major` FOREIGN KEY (`major_id`) REFERENCES `majors` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专业方向表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `major_directions`
--

LOCK TABLES `major_directions` WRITE;
/*!40000 ALTER TABLE `major_directions` DISABLE KEYS */;
/*!40000 ALTER TABLE `major_directions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `majors`
--

DROP TABLE IF EXISTS `majors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `majors` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `major_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '专业编码',
  `major_category_code` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属专业大类编码',
  `major_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '专业名称',
  `org_unit_id` bigint NOT NULL COMMENT '所属组织单元ID',
  `schooling_years` int DEFAULT '3' COMMENT '学制(年)',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '专业描述',
  `enrollment_target` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '招生对象(初中毕业生/高中毕业生/等)',
  `education_form` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '办学形式(全日制/非全日制)',
  `lead_teacher_id` bigint DEFAULT NULL COMMENT '专业带头人用户ID',
  `lead_teacher_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '专业带头人姓名(冗余)',
  `approval_year` int DEFAULT NULL COMMENT '批准设置年份',
  `major_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ENROLLING' COMMENT '专业状态(PREPARING/ENROLLING/SUSPENDED/REVOKED)',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '状态:0禁用,1启用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_major_code` (`major_code`),
  KEY `idx_org_unit_id` (`org_unit_id`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专业表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `majors`
--

LOCK TABLES `majors` WRITE;
/*!40000 ALTER TABLE `majors` DISABLE KEYS */;
/*!40000 ALTER TABLE `majors` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `migration_locks`
--

DROP TABLE IF EXISTS `migration_locks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `migration_locks` (
  `lock_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `locked_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`lock_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='一次性数据迁移幂等标记';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `migration_locks`
--

LOCK TABLES `migration_locks` WRITE;
/*!40000 ALTER TABLE `migration_locks` DISABLE KEYS */;
INSERT INTO `migration_locks` VALUES ('evaluation-engine-perfect-2026-05-23','2026-06-01 23:34:29','评级引擎完美架构: GradeBand → Indicator+GradeScheme 派生'),('scoring-profile-project-ownership-2026-05-23','2026-06-01 23:42:33','ScoringProfile 项目-owned 迁移: 克隆 + 切引用 + 删孤儿');
/*!40000 ALTER TABLE `migration_locks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `msg_group_members`
--

DROP TABLE IF EXISTS `msg_group_members`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `msg_group_members` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `group_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `added_by` bigint DEFAULT NULL,
  `added_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_user` (`group_id`,`user_id`,`deleted`),
  KEY `idx_user` (`user_id`),
  KEY `idx_group` (`group_id`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知组成员';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `msg_group_members`
--

LOCK TABLES `msg_group_members` WRITE;
/*!40000 ALTER TABLE `msg_group_members` DISABLE KEYS */;
/*!40000 ALTER TABLE `msg_group_members` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `msg_groups`
--

DROP TABLE IF EXISTS `msg_groups`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `msg_groups` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `group_code` varchar(64) NOT NULL,
  `group_name` varchar(128) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_code` (`tenant_id`,`group_code`,`deleted`),
  KEY `idx_tenant` (`tenant_id`,`enabled`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知组';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `msg_groups`
--

LOCK TABLES `msg_groups` WRITE;
/*!40000 ALTER TABLE `msg_groups` DISABLE KEYS */;
/*!40000 ALTER TABLE `msg_groups` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `msg_notification_failures`
--

DROP TABLE IF EXISTS `msg_notification_failures`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `msg_notification_failures` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` bigint DEFAULT NULL COMMENT '触发此失败的 entity_events.id',
  `rule_id` bigint DEFAULT NULL COMMENT '订阅规则 msg_subscription_rules.id',
  `target_user_id` bigint DEFAULT NULL COMMENT '目标用户',
  `error_message` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '异常 class + message',
  `retry_count` int DEFAULT '0' COMMENT '已重试次数',
  `next_retry_at` datetime DEFAULT NULL COMMENT '下次重试时间; NULL = 不再重试',
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_retry` (`next_retry_at`,`retry_count`),
  KEY `idx_event` (`event_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息派发死信表 (单条 save 异常落此, 供定时重试)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `msg_notification_failures`
--

LOCK TABLES `msg_notification_failures` WRITE;
/*!40000 ALTER TABLE `msg_notification_failures` DISABLE KEYS */;
/*!40000 ALTER TABLE `msg_notification_failures` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `msg_notifications`
--

DROP TABLE IF EXISTS `msg_notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `msg_notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `receiver_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'USER' COMMENT '接收人类型 USER/GROUP/ROLE',
  `receiver_id` bigint NOT NULL DEFAULT '0' COMMENT '接收人 ID (USER=user_id, GROUP=group_id, ROLE=role_id)',
  `user_id` bigint NOT NULL COMMENT '接收人',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci,
  `msg_type` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT 'SYSTEM/EVENT/EVALUATION/MANUAL',
  `source_event_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '来源事件类型',
  `source_ref_type` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `source_ref_id` bigint DEFAULT NULL,
  `subject_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '事件主体类型(USER/ORG_UNIT/PLACE)',
  `subject_id` bigint DEFAULT NULL COMMENT '事件主体ID',
  `subject_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '事件主体名称',
  `event_category` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '事件大类(DISCIPLINE/AWARD/INSPECTION等)',
  `source_module` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '来源模块(inspection/attendance/dormitory等)',
  `is_read` tinyint DEFAULT '0',
  `read_at` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  `event_id` bigint DEFAULT NULL COMMENT '触发此通知的事件ID',
  `send_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SENT' COMMENT '发送状态: PENDING/SENT/FAILED',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '已重试次数',
  `last_error` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最近一次失败原因（用于排查）',
  `sent_at` datetime DEFAULT NULL COMMENT '真实送达时间（站内信=插入时；多通道=通道回调时）',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`,`is_read`,`created_at` DESC),
  KEY `idx_tenant` (`tenant_id`),
  KEY `idx_msg_subject` (`subject_type`,`subject_id`),
  KEY `idx_msg_category` (`event_category`),
  KEY `idx_msg_notif_user_read` (`user_id`,`is_read`,`deleted`,`created_at`),
  KEY `idx_msg_notif_user_list` (`user_id`,`deleted`,`created_at`),
  KEY `idx_msg_notif_event` (`event_id`),
  KEY `idx_receiver` (`receiver_type`,`receiver_id`,`is_read`,`created_at` DESC),
  KEY `idx_send_status` (`send_status`,`retry_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `msg_notifications`
--

LOCK TABLES `msg_notifications` WRITE;
/*!40000 ALTER TABLE `msg_notifications` DISABLE KEYS */;
/*!40000 ALTER TABLE `msg_notifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `msg_subscription_rules`
--

DROP TABLE IF EXISTS `msg_subscription_rules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `msg_subscription_rules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `rule_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `event_category` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '事件大类，NULL=全部',
  `event_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '事件类型，NULL=大类下全部',
  `target_mode` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'BY_ROLE/BY_ORG_ADMIN/BY_USER/BY_RELATED',
  `target_config` text COLLATE utf8mb4_unicode_ci COMMENT 'JSON: 角色编码/组织ID/用户ID列表',
  `channel` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'IN_APP' COMMENT 'IN_APP/EMAIL/WECHAT',
  `template_id` bigint DEFAULT NULL,
  `is_enabled` tinyint DEFAULT '1',
  `sort_order` int DEFAULT '0',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  `plugin_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '插件级启用状态 (级联自订阅的 event_type 的 plugin)',
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`),
  KEY `idx_event` (`event_category`,`event_type`),
  KEY `idx_msg_subscription_event` (`event_category`,`event_type`,`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `msg_subscription_rules`
--

LOCK TABLES `msg_subscription_rules` WRITE;
/*!40000 ALTER TABLE `msg_subscription_rules` DISABLE KEYS */;
INSERT INTO `msg_subscription_rules` VALUES (1,1,'成绩待审核-通知审核人','TEACHING','GRADE_SUBMITTED','BY_ROLE','[\"DEPT_ADMIN\",\"ACADEMIC_ADMIN\",\"SUPER_ADMIN\"]','IN_APP',NULL,1,10,NULL,'2026-06-01 22:57:28','2026-06-01 22:57:28',0,1),(2,1,'成绩审核通过-通知教师','TEACHING','GRADE_APPROVED','BY_SUBJECT','{}','IN_APP',NULL,1,11,NULL,'2026-06-01 22:57:28','2026-06-01 22:57:28',0,1),(3,1,'成绩发布-通知班级用户','TEACHING','GRADE_PUBLISHED','BY_RELATED','{}','IN_APP',NULL,1,12,NULL,'2026-06-01 22:57:28','2026-06-01 22:57:28',0,1),(4,1,'考试发布-通知班级用户','TEACHING','EXAM_PUBLISHED','BY_RELATION','{\"relation\":\"admin\",\"resource_type\":\"org_unit\",\"direction\":\"inward\"}','IN_APP',NULL,1,13,NULL,'2026-06-01 22:57:28','2026-06-01 22:57:32',0,1),(5,1,'成绩发布-通知学生本人',NULL,'GRADE_PUBLISHED','BY_FEATURE','{\"features\":[\"isLearner\",\"receivesPersonalGrade\"]}','IN_APP',NULL,1,0,NULL,'2026-06-01 22:57:32','2026-06-01 22:57:33',1,1),(6,1,'个人成绩-通知学生本人',NULL,'GRADE_PUBLISHED_PERSONAL','BY_SUBJECT','{}','IN_APP',NULL,1,0,NULL,'2026-06-01 22:57:33','2026-06-01 22:57:33',0,1),(7,1,'个人成绩-通知家长',NULL,'GRADE_PUBLISHED_PERSONAL','BY_RELATION','{\"relation\": \"guardian_of\", \"direction\": \"inward\", \"resource_type\": \"user\"}','IN_APP',NULL,1,0,NULL,'2026-06-01 22:57:33','2026-06-01 22:57:33',0,1),(8,0,'管理员通知(全部事件)',NULL,NULL,'BY_ROLE','[\"SUPER_ADMIN\",\"ADMIN\"]','IN_APP',NULL,1,1,NULL,'2026-06-01 22:57:55','2026-06-01 22:57:55',0,1),(9,1,'成绩待审核-通知审核人','TEACHING','GRADE_SUBMITTED','BY_ROLE','[\"DEPT_ADMIN\",\"ACADEMIC_ADMIN\",\"SUPER_ADMIN\"]','IN_APP',NULL,1,10,NULL,'2026-06-01 23:06:11','2026-06-01 23:06:11',0,1),(10,1,'成绩审核通过-通知教师','TEACHING','GRADE_APPROVED','BY_SUBJECT','{}','IN_APP',NULL,1,11,NULL,'2026-06-01 23:06:11','2026-06-01 23:06:11',0,1),(11,1,'成绩发布-通知班级用户','TEACHING','GRADE_PUBLISHED','BY_RELATED','{}','IN_APP',NULL,1,12,NULL,'2026-06-01 23:06:11','2026-06-01 23:06:11',0,1),(12,1,'考试发布-通知班级用户','TEACHING','EXAM_PUBLISHED','BY_RELATED','{}','IN_APP',NULL,1,13,NULL,'2026-06-01 23:06:11','2026-06-01 23:06:11',0,1),(13,1,'个人成绩-通知学生本人',NULL,'GRADE_PUBLISHED_PERSONAL','BY_SUBJECT','{}','IN_APP',NULL,1,0,NULL,'2026-06-01 23:06:14','2026-06-01 23:06:14',0,1),(14,1,'个人成绩-通知家长',NULL,'GRADE_PUBLISHED_PERSONAL','BY_RELATION','{\"relation\": \"guardian_of\", \"direction\": \"inward\", \"resource_type\": \"user\"}','IN_APP',NULL,1,0,NULL,'2026-06-01 23:06:14','2026-06-01 23:06:14',0,1),(15,0,'管理员通知(全部事件)',NULL,NULL,'BY_ROLE','[\"SUPER_ADMIN\",\"ADMIN\"]','IN_APP',NULL,1,1,NULL,'2026-06-01 23:25:08','2026-06-01 23:25:08',0,1);
/*!40000 ALTER TABLE `msg_subscription_rules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `msg_templates`
--

DROP TABLE IF EXISTS `msg_templates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `msg_templates` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `template_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `template_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title_template` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题模板，支持变量 {{subjectName}}',
  `content_template` text COLLATE utf8mb4_unicode_ci COMMENT '内容模板',
  `is_system` tinyint DEFAULT '0',
  `is_enabled` tinyint DEFAULT '1',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`tenant_id`,`template_code`),
  KEY `idx_msg_template_code` (`template_code`,`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `msg_templates`
--

LOCK TABLES `msg_templates` WRITE;
/*!40000 ALTER TABLE `msg_templates` DISABLE KEYS */;
INSERT INTO `msg_templates` VALUES (1,0,'VIOLATION','违规通知','{{subjectName}} 发生违规','{{subjectName}} 于 {{time}} 被记录违规：{{description}}',1,1,NULL,'2026-06-01 22:57:15','2026-06-01 22:57:15',0),(2,0,'GRADE','检查评级','{{subjectName}} 检查评级：{{grade}}','{{subjectName}} 检查得分 {{score}}，评级 {{grade}}',1,1,NULL,'2026-06-01 22:57:15','2026-06-01 22:57:15',0),(3,0,'EVALUATION','评选结果','{{subjectName}} 评选结果：{{levelName}}','{{subjectName}} 在 {{campaignName}} 中获评 {{levelName}}（排名第{{rank}}）',1,1,NULL,'2026-06-01 22:57:15','2026-06-01 22:57:15',0),(4,0,'SYSTEM','系统通知','{{title}}','{{content}}',1,1,NULL,'2026-06-01 22:57:15','2026-06-01 22:57:15',0),(5,0,'PUBLISH','发布通知','{{resourceType}} 已发布','{{operatorName}} 发布了 {{resourceType}}',1,1,NULL,'2026-06-01 22:57:55','2026-06-01 22:57:55',0),(6,0,'APPROVE','审批通知','{{resourceType}} 审批通过','{{operatorName}} 已审批 {{resourceType}}',1,1,NULL,'2026-06-01 22:57:55','2026-06-01 22:57:55',0),(7,0,'REJECT','驳回通知','{{resourceType}} 已驳回','{{operatorName}} 已驳回 {{resourceType}}',1,1,NULL,'2026-06-01 22:57:55','2026-06-01 22:57:55',0),(8,0,'ENROLL','招生通知','新学生报到','{{studentName}} 已完成报到注册',1,1,NULL,'2026-06-01 22:57:55','2026-06-01 22:57:55',0);
/*!40000 ALTER TABLE `msg_templates` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `org_types`
--

DROP TABLE IF EXISTS `org_types`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `org_types` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `type_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型编码（唯一标识）',
  `type_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型名称',
  `parent_type_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '父类型编码（层级关系）',
  `level_order` int NOT NULL DEFAULT '0' COMMENT '层级顺序（0=顶级）',
  `icon` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图标标识',
  `color` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '显示颜色',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '类型描述',
  `can_have_classes` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否可包含班级',
  `can_have_students` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否可直接包含学生',
  `can_be_inspected` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否可被检查',
  `can_have_leader` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否有负责人',
  `is_system` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否系统预置（不可删除）',
  `is_enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序顺序',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_code` (`type_code`),
  KEY `idx_parent_type` (`parent_type_code`),
  KEY `idx_level_order` (`level_order`),
  KEY `idx_is_enabled` (`is_enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织类型配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `org_types`
--

LOCK TABLES `org_types` WRITE;
/*!40000 ALTER TABLE `org_types` DISABLE KEYS */;
INSERT INTO `org_types` VALUES (1,'SCHOOL','学校',NULL,0,'School','#1890ff','学校/教育机构',0,0,0,1,1,1,1,'2026-06-01 23:05:54','2026-06-01 23:05:54',NULL,NULL,0),(2,'COLLEGE','学院','SCHOOL',1,'Building','#52c41a','二级学院/分院',0,0,1,1,1,1,2,'2026-06-01 23:05:54','2026-06-01 23:05:54',NULL,NULL,0),(3,'DEPARTMENT','系部','SCHOOL',1,'Briefcase','#722ed1','教学系部',1,0,1,1,1,1,3,'2026-06-01 23:05:54','2026-06-01 23:05:54',NULL,NULL,0),(4,'MAJOR','专业','COLLEGE',2,'BookOpen','#fa8c16','专业方向',1,0,1,1,1,1,4,'2026-06-01 23:05:54','2026-06-01 23:05:54',NULL,NULL,0),(5,'TEACHING_GROUP','教研室','DEPARTMENT',2,'Users','#13c2c2','教学研究组',0,0,0,1,1,1,5,'2026-06-01 23:05:54','2026-06-01 23:05:54',NULL,NULL,0),(6,'FUNCTIONAL','职能部门','SCHOOL',1,'Settings','#8c8c8c','行政职能部门',0,0,0,1,1,1,10,'2026-06-01 23:05:54','2026-06-01 23:05:54',NULL,NULL,0),(7,'ADMINISTRATIVE','行政单位','SCHOOL',1,'FileText','#595959','行政管理单位',0,0,0,1,1,1,11,'2026-06-01 23:05:54','2026-06-01 23:05:54',NULL,NULL,0);
/*!40000 ALTER TABLE `org_types` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `org_unit_scores`
--

DROP TABLE IF EXISTS `org_unit_scores`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `org_unit_scores` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `project_id` bigint NOT NULL,
  `org_unit_id` bigint NOT NULL,
  `cycle_date` date NOT NULL,
  `score` decimal(8,2) NOT NULL,
  `grade` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `child_count` int NOT NULL DEFAULT '0',
  `source_count` int NOT NULL DEFAULT '0',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_org_score` (`tenant_id`,`project_id`,`org_unit_id`,`cycle_date`),
  KEY `idx_org_score_project_date` (`project_id`,`cycle_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `org_unit_scores`
--

LOCK TABLES `org_unit_scores` WRITE;
/*!40000 ALTER TABLE `org_unit_scores` DISABLE KEYS */;
/*!40000 ALTER TABLE `org_unit_scores` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `org_units`
--

DROP TABLE IF EXISTS `org_units`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `org_units` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `unit_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单元编码',
  `unit_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单元名称',
  `unit_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'department' COMMENT '组织类型',
  `type_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '类型编码(=unit_type 反范式, 视图/seed 用)',
  `unit_category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '组织类别',
  `parent_id` bigint DEFAULT NULL COMMENT '父单元ID',
  `tree_path` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '树形路径',
  `tree_level` int DEFAULT '1' COMMENT '层级',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `deleted` bigint DEFAULT '0',
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `attributes` json DEFAULT NULL COMMENT '扩展属性',
  `version` int DEFAULT '0' COMMENT '乐观锁版本号',
  `headcount` int DEFAULT NULL COMMENT '编制数',
  `merged_into_id` bigint DEFAULT NULL COMMENT '合并到哪个组织',
  `split_from_id` bigint DEFAULT NULL COMMENT '从哪个组织拆分',
  `dissolved_at` datetime DEFAULT NULL COMMENT '撤销时间',
  `dissolved_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '撤销原因',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVE' COMMENT 'DRAFT/ACTIVE/FROZEN/MERGING/DISSOLVED',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_unit_code_deleted` (`unit_code`,`deleted`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_org_units_type_code` (`unit_type`,`unit_code`,`deleted`),
  KEY `idx_org_units_tenant` (`tenant_id`),
  KEY `idx_merged_into` (`merged_into_id`),
  KEY `idx_split_from` (`split_from_id`),
  KEY `idx_unit_category` (`unit_category`),
  KEY `idx_org_units_tree_path` (`tree_path`(100)),
  KEY `idx_org_units_parent_sort` (`parent_id`,`sort_order`),
  KEY `idx_org_units_type` (`unit_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织单元表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `org_units`
--

LOCK TABLES `org_units` WRITE;
/*!40000 ALTER TABLE `org_units` DISABLE KEYS */;
/*!40000 ALTER TABLE `org_units` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pending_relation_approvals`
--

DROP TABLE IF EXISTS `pending_relation_approvals`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pending_relation_approvals` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `resource_type` varchar(50) NOT NULL,
  `resource_id` bigint NOT NULL,
  `relation` varchar(50) NOT NULL,
  `subject_type` varchar(30) NOT NULL,
  `subject_id` bigint NOT NULL,
  `access_level` varchar(20) DEFAULT NULL,
  `valid_from` datetime DEFAULT NULL,
  `valid_to` datetime DEFAULT NULL,
  `metadata` json DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/CANCELLED',
  `requested_by` bigint NOT NULL,
  `requested_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `approver_id` bigint DEFAULT NULL,
  `approved_at` datetime DEFAULT NULL,
  `rejection_reason` varchar(500) DEFAULT NULL,
  `granted_relation_id` bigint DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `idx_pending_status` (`status`,`requested_at`),
  KEY `idx_pending_subject` (`subject_type`,`subject_id`,`status`),
  KEY `idx_pending_resource` (`resource_type`,`resource_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='关系授权审批 pending 队列';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pending_relation_approvals`
--

LOCK TABLES `pending_relation_approvals` WRITE;
/*!40000 ALTER TABLE `pending_relation_approvals` DISABLE KEYS */;
/*!40000 ALTER TABLE `pending_relation_approvals` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `period_configs`
--

DROP TABLE IF EXISTS `period_configs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `period_configs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `semester_id` bigint NOT NULL,
  `config_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `periods_per_day` int NOT NULL DEFAULT '8',
  `schedule_days` json NOT NULL,
  `periods` json NOT NULL COMMENT '[{period,name,startTime,endTime}]',
  `is_default` tinyint DEFAULT '1',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_semester` (`semester_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作息表/节次配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `period_configs`
--

LOCK TABLES `period_configs` WRITE;
/*!40000 ALTER TABLE `period_configs` DISABLE KEYS */;
/*!40000 ALTER TABLE `period_configs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `permissions`
--

DROP TABLE IF EXISTS `permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `permissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `permission_code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限编码',
  `permission_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限名称',
  `parent_id` bigint DEFAULT '0' COMMENT '父权限ID',
  `permission_type` tinyint DEFAULT '1' COMMENT '类型:1目录,2菜单,3按钮',
  `permission_scope` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MANAGEMENT' COMMENT '权限作用域: PUBLIC=公共, SELF=个人空间, MANAGEMENT=管理后台',
  `path` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '路由路径',
  `component` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '组件路径',
  `icon` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图标',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '状态:0禁用,1启用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `plugin_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '插件级启用状态',
  `permission_desc` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `resource_type` int DEFAULT NULL,
  `industry` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `plugin_class` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `origin` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code` (`permission_code`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_status` (`status`),
  KEY `idx_permission_scope` (`permission_scope`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_permissions_tree` (`parent_id`,`permission_type`,`sort_order`),
  KEY `idx_permissions_tenant` (`tenant_id`),
  KEY `idx_perm_plugin_code` (`plugin_enabled`,`permission_code`)
) ENGINE=InnoDB AUTO_INCREMENT=9286 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `permissions`
--

LOCK TABLES `permissions` WRITE;
/*!40000 ALTER TABLE `permissions` DISABLE KEYS */;
INSERT INTO `permissions` VALUES (1,'check:export','导出管理',NULL,0,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 22:56:53','2026-06-01 22:56:53',0,1,1,NULL,NULL,NULL,NULL,NULL),(5001,'task','任务管理',0,1,'MANAGEMENT','/task','Layout','el-icon-s-claim',50,1,'2026-06-01 23:25:08','2026-06-01 23:25:08',0,1,1,'任务管理模块',1,NULL,NULL,NULL),(5002,'task:list','任务列表页',5001,1,'MANAGEMENT','/task/list','task/TaskList','',1,1,'2026-06-01 23:25:08','2026-06-01 23:34:28',0,1,1,'',1,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(5003,'task:my','我的任务页',5001,1,'MANAGEMENT','/task/my','task/MyTask','',2,1,'2026-06-01 23:25:08','2026-06-01 23:34:28',0,1,1,'',1,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(5004,'task:approval','任务审批',5001,1,'MANAGEMENT','/task/approval','task/TaskApproval','',3,1,'2026-06-01 23:25:08','2026-06-01 23:34:28',0,1,1,'',1,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(5005,'task:statistics','任务统计',5001,1,'MANAGEMENT','/task/statistics','task/TaskStatistics','',4,1,'2026-06-01 23:25:08','2026-06-01 23:25:08',0,1,1,'任务统计页面',1,NULL,NULL,NULL),(5010,'task:view','查看任务',5001,1,'MANAGEMENT','','','',10,1,'2026-06-01 23:25:08','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(5011,'task:create','创建任务',5001,1,'MANAGEMENT','','','',11,1,'2026-06-01 23:25:08','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(5012,'task:execute','执行任务',5001,1,'MANAGEMENT','','','',12,1,'2026-06-01 23:25:08','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(5013,'task:approve','审批任务',5001,1,'MANAGEMENT','','','',13,1,'2026-06-01 23:25:08','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(5014,'task:manage','管理任务',5001,1,'MANAGEMENT','','','',14,1,'2026-06-01 23:25:08','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(5100,'workflow','流程管理',0,1,'MANAGEMENT','/workflow','Layout','el-icon-s-operation',51,1,'2026-06-01 23:25:08','2026-06-01 23:25:08',0,1,1,'工作流程管理模块',1,NULL,NULL,NULL),(5101,'workflow:template','流程模板',5100,1,'MANAGEMENT','/workflow/template','workflow/TemplateList','',1,1,'2026-06-01 23:25:08','2026-06-01 23:25:08',0,1,1,'流程模板列表',1,NULL,NULL,NULL),(5102,'workflow:designer','流程设计器',5100,1,'MANAGEMENT','/workflow/designer','workflow/ProcessDesigner','',2,1,'2026-06-01 23:25:08','2026-06-01 23:25:08',0,1,1,'流程设计器',1,NULL,NULL,NULL),(5110,'workflow:view','查看流程',5100,1,'MANAGEMENT','','','',10,1,'2026-06-01 23:25:08','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(5111,'workflow:create','创建流程',5100,1,'MANAGEMENT','','','',11,1,'2026-06-01 23:25:08','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(5112,'workflow:update','修改流程',5100,1,'MANAGEMENT','','','',12,1,'2026-06-01 23:25:08','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(5113,'workflow:delete','删除流程',5100,1,'MANAGEMENT','','','',13,1,'2026-06-01 23:25:08','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(5114,'workflow:deploy','部署流程',5100,1,'MANAGEMENT','','','',14,1,'2026-06-01 23:25:08','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(8101,'teaching:calendar','教学日历管理',0,1,'MANAGEMENT',NULL,NULL,NULL,200,1,'2026-06-01 22:56:35','2026-06-01 22:56:35',0,1,1,NULL,NULL,NULL,NULL,NULL),(8102,'teaching:calendar:view','查看教学日历',8101,3,'MANAGEMENT',NULL,NULL,NULL,1,1,'2026-06-01 22:56:35','2026-06-01 22:56:35',0,1,1,NULL,NULL,NULL,NULL,NULL),(8103,'teaching:calendar:edit','编辑教学日历',8101,3,'MANAGEMENT',NULL,NULL,NULL,2,1,'2026-06-01 22:56:35','2026-06-01 22:56:35',0,1,1,NULL,NULL,NULL,NULL,NULL),(8111,'teaching:course','课程管理',0,1,'MANAGEMENT',NULL,NULL,NULL,201,1,'2026-06-01 22:56:35','2026-06-01 22:56:35',0,1,1,NULL,NULL,NULL,NULL,NULL),(8112,'teaching:course:view','查看课程',8111,3,'MANAGEMENT',NULL,NULL,NULL,1,1,'2026-06-01 22:56:35','2026-06-01 22:56:35',0,1,1,NULL,NULL,NULL,NULL,NULL),(8113,'teaching:course:edit','编辑课程',8111,3,'MANAGEMENT',NULL,NULL,NULL,2,1,'2026-06-01 22:56:35','2026-06-01 22:56:35',0,1,1,NULL,NULL,NULL,NULL,NULL),(8121,'teaching:curriculum','培养方案管理',0,1,'MANAGEMENT',NULL,NULL,NULL,202,1,'2026-06-01 22:56:35','2026-06-01 22:56:35',0,1,1,NULL,NULL,NULL,NULL,NULL),(8122,'teaching:curriculum:view','查看培养方案',8121,3,'MANAGEMENT',NULL,NULL,NULL,1,1,'2026-06-01 22:56:35','2026-06-01 22:56:35',0,1,1,NULL,NULL,NULL,NULL,NULL),(8123,'teaching:curriculum:edit','编辑培养方案',8121,3,'MANAGEMENT',NULL,NULL,NULL,2,1,'2026-06-01 22:56:35','2026-06-01 22:56:35',0,1,1,NULL,NULL,NULL,NULL,NULL),(8131,'teaching:task','教学任务管理',0,1,'MANAGEMENT',NULL,NULL,NULL,203,1,'2026-06-01 22:56:35','2026-06-01 22:56:35',0,1,1,NULL,NULL,NULL,NULL,NULL),(8132,'teaching:task:view','查看教学任务',8131,3,'MANAGEMENT',NULL,NULL,NULL,1,1,'2026-06-01 22:56:35','2026-06-01 23:34:29',0,1,1,'',NULL,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(8133,'teaching:task:edit','编辑教学任务',8131,3,'MANAGEMENT',NULL,NULL,NULL,2,1,'2026-06-01 22:56:35','2026-06-01 23:34:29',0,1,1,'',NULL,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(8141,'teaching:schedule','排课管理',0,1,'MANAGEMENT',NULL,NULL,NULL,204,1,'2026-06-01 22:56:35','2026-06-01 22:56:35',0,1,1,NULL,NULL,NULL,NULL,NULL),(8142,'teaching:schedule:view','查看课表',8141,3,'MANAGEMENT',NULL,NULL,NULL,1,1,'2026-06-01 22:56:35','2026-06-01 23:34:29',0,1,1,'',NULL,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(8143,'teaching:schedule:edit','编辑课表',8141,3,'MANAGEMENT',NULL,NULL,NULL,2,1,'2026-06-01 22:56:35','2026-06-01 23:34:29',0,1,1,'',NULL,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(8151,'teaching:constraint','排课约束管理',0,1,'MANAGEMENT',NULL,NULL,NULL,205,1,'2026-06-01 22:56:35','2026-06-01 22:56:35',0,1,1,NULL,NULL,NULL,NULL,NULL),(8152,'teaching:constraint:view','查看排课约束',8151,3,'MANAGEMENT',NULL,NULL,NULL,1,1,'2026-06-01 22:56:35','2026-06-01 23:34:29',0,1,1,'',NULL,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(8153,'teaching:constraint:edit','编辑排课约束',8151,3,'MANAGEMENT',NULL,NULL,NULL,2,1,'2026-06-01 22:56:35','2026-06-01 23:34:29',0,1,1,'',NULL,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(8161,'teaching:offering','开课管理',0,1,'MANAGEMENT',NULL,NULL,NULL,206,1,'2026-06-01 22:56:35','2026-06-01 22:56:35',0,1,1,NULL,NULL,NULL,NULL,NULL),(8162,'teaching:offering:view','查看开课',8161,3,'MANAGEMENT',NULL,NULL,NULL,1,1,'2026-06-01 22:56:35','2026-06-01 23:34:29',0,1,1,'',NULL,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(8163,'teaching:offering:edit','编辑开课',8161,3,'MANAGEMENT',NULL,NULL,NULL,2,1,'2026-06-01 22:56:35','2026-06-01 23:34:29',0,1,1,'',NULL,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(8171,'teaching:class','教学班管理',0,1,'MANAGEMENT',NULL,NULL,NULL,207,1,'2026-06-01 22:56:35','2026-06-01 22:56:35',0,1,1,NULL,NULL,NULL,NULL,NULL),(8172,'teaching:class:view','查看教学班',8171,3,'MANAGEMENT',NULL,NULL,NULL,1,1,'2026-06-01 22:56:35','2026-06-01 23:34:29',0,1,1,'',NULL,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(8173,'teaching:class:edit','编辑教学班',8171,3,'MANAGEMENT',NULL,NULL,NULL,2,1,'2026-06-01 22:56:35','2026-06-01 23:34:29',0,1,1,'',NULL,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(8181,'teaching:exam','考试管理',0,1,'MANAGEMENT',NULL,NULL,NULL,208,1,'2026-06-01 22:56:35','2026-06-01 22:56:35',0,1,1,NULL,NULL,NULL,NULL,NULL),(8182,'teaching:exam:view','查看考试',8181,3,'MANAGEMENT',NULL,NULL,NULL,1,1,'2026-06-01 22:56:35','2026-06-01 23:34:29',0,1,1,'',NULL,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(8183,'teaching:exam:edit','编辑考试',8181,3,'MANAGEMENT',NULL,NULL,NULL,2,1,'2026-06-01 22:56:35','2026-06-01 23:34:29',0,1,1,'',NULL,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(8191,'teaching:grade','成绩管理',0,1,'MANAGEMENT',NULL,NULL,NULL,209,1,'2026-06-01 22:56:35','2026-06-01 22:56:35',0,1,1,NULL,NULL,NULL,NULL,NULL),(8192,'teaching:grade:view','查看成绩',8191,3,'MANAGEMENT',NULL,NULL,NULL,1,1,'2026-06-01 22:56:35','2026-06-01 23:34:29',0,1,1,'',NULL,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(8193,'teaching:grade:edit','编辑成绩',8191,3,'MANAGEMENT',NULL,NULL,NULL,2,1,'2026-06-01 22:56:35','2026-06-01 23:34:29',0,1,1,'',NULL,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(8201,'student:attendance','考勤管理',0,1,'MANAGEMENT',NULL,NULL,NULL,210,1,'2026-06-01 22:56:35','2026-06-01 22:56:35',0,1,1,NULL,NULL,NULL,NULL,NULL),(8202,'student:attendance:view','查看学生考勤',8201,3,'MANAGEMENT',NULL,NULL,NULL,1,1,'2026-06-01 22:56:35','2026-06-01 23:34:29',0,1,1,'',NULL,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(8203,'student:attendance:edit','编辑学生考勤',8201,3,'MANAGEMENT',NULL,NULL,NULL,2,1,'2026-06-01 22:56:35','2026-06-01 23:34:29',0,1,1,'',NULL,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(8211,'student:warning','学业预警管理',0,1,'MANAGEMENT',NULL,NULL,NULL,211,1,'2026-06-01 22:56:35','2026-06-01 22:56:35',0,1,1,NULL,NULL,NULL,NULL,NULL),(8212,'student:warning:view','查看学业预警',8211,3,'MANAGEMENT',NULL,NULL,NULL,1,1,'2026-06-01 22:56:35','2026-06-01 23:34:29',0,1,1,'',NULL,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(8213,'student:warning:edit','编辑学业预警',8211,3,'MANAGEMENT',NULL,NULL,NULL,2,1,'2026-06-01 22:56:35','2026-06-01 23:34:29',0,1,1,'',NULL,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(8301,'asset:manage','管理资产',0,1,'MANAGEMENT',NULL,NULL,NULL,300,1,'2026-06-01 22:56:35','2026-06-01 23:34:27',0,1,1,'',NULL,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(8302,'asset:manage:view','查看资产管理',8301,3,'MANAGEMENT',NULL,NULL,NULL,1,1,'2026-06-01 22:56:35','2026-06-01 23:34:27',0,1,1,'',NULL,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(8303,'asset:manage:edit','编辑资产管理',8301,3,'MANAGEMENT',NULL,NULL,NULL,2,1,'2026-06-01 22:56:35','2026-06-01 23:34:27',0,1,1,'',NULL,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(8311,'asset:borrow','资产借用管理',0,1,'MANAGEMENT',NULL,NULL,NULL,301,1,'2026-06-01 22:56:35','2026-06-01 22:56:35',0,1,1,NULL,NULL,NULL,NULL,NULL),(8312,'asset:borrow:view','查看借用',8311,3,'MANAGEMENT',NULL,NULL,NULL,1,1,'2026-06-01 22:56:35','2026-06-01 23:34:27',0,1,1,'',NULL,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(8313,'asset:borrow:edit','编辑借用',8311,3,'MANAGEMENT',NULL,NULL,NULL,2,1,'2026-06-01 22:56:35','2026-06-01 23:34:27',0,1,1,'',NULL,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(8321,'asset:inventory','资产盘点管理',0,1,'MANAGEMENT',NULL,NULL,NULL,302,1,'2026-06-01 22:56:35','2026-06-01 22:56:35',0,1,1,NULL,NULL,NULL,NULL,NULL),(8322,'asset:inventory:view','资产盘点查看',8321,3,'MANAGEMENT',NULL,NULL,NULL,1,1,'2026-06-01 22:56:35','2026-06-01 23:34:27',0,1,1,'',NULL,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(8323,'asset:inventory:edit','资产盘点编辑',8321,3,'MANAGEMENT',NULL,NULL,NULL,2,1,'2026-06-01 22:56:35','2026-06-01 23:34:27',0,1,1,'',NULL,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(8331,'asset:approval','资产审批管理',0,1,'MANAGEMENT',NULL,NULL,NULL,303,1,'2026-06-01 22:56:35','2026-06-01 22:56:35',0,1,1,NULL,NULL,NULL,NULL,NULL),(8332,'asset:approval:view','查看资产审批',8331,3,'MANAGEMENT',NULL,NULL,NULL,1,1,'2026-06-01 22:56:35','2026-06-01 23:34:27',0,1,1,'',NULL,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(8333,'asset:approval:edit','编辑资产审批',8331,3,'MANAGEMENT',NULL,NULL,NULL,2,1,'2026-06-01 22:56:35','2026-06-01 23:34:27',0,1,1,'',NULL,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9001,'calendar','校历管理',0,1,'MANAGEMENT',NULL,NULL,NULL,2,1,'2026-06-01 22:56:38','2026-06-01 23:34:29',0,1,1,'',NULL,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9002,'calendar:view','查看校历',9001,3,'MANAGEMENT',NULL,NULL,NULL,1,1,'2026-06-01 22:56:38','2026-06-01 23:34:29',0,1,1,'',NULL,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9003,'calendar:edit','编辑校历',9001,3,'MANAGEMENT',NULL,NULL,NULL,2,1,'2026-06-01 22:56:38','2026-06-01 23:34:29',0,1,1,'',NULL,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9004,'system:admin','系统管理员',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'顶级权限,拥有所有操作',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9005,'system:user:view','查看用户',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'查看用户列表/详情',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9006,'system:user:add','新增用户',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9007,'system:user:edit','编辑用户',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9008,'system:user:delete','删除用户',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9009,'system:role:view','查看角色',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9010,'system:role:add','新增角色',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9011,'system:role:edit','编辑角色',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9012,'system:role:delete','删除角色',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9013,'system:permission:view','查看权限',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9014,'system:permission:add','新增权限',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9015,'system:permission:edit','编辑权限',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9016,'system:permission:delete','删除权限',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9017,'system:permission:manage','权限治理(Casbin sync/refresh)',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9018,'system:config:view','查看配置',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9019,'system:config:add','新增配置',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9020,'system:config:edit','编辑配置',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9021,'system:config:delete','删除配置',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9022,'system:announcement:view','查看公告',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9023,'system:announcement:add','发布公告',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9024,'system:announcement:edit','编辑公告',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9025,'system:announcement:delete','删除公告',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9026,'system:operlog:view','查看操作日志',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9027,'system:operlog:delete','删除操作日志',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9028,'system:operlog:clear','清空操作日志',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9029,'wechat:push:view','查看微信推送记录',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9030,'wechat:push:send','发送微信推送',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9031,'dashboard:view','查看首页仪表盘',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9032,'analytics:view','数据分析',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9033,'asset:borrow:create','创建借用',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9034,'asset:borrow:list','借用管理',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9035,'asset:borrow:return','归还资产',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9036,'asset:category:list','查看分类',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9037,'asset:category:manage','管理分类',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9038,'asset:depreciation:list','查看折旧记录',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9039,'asset:depreciation:manage','折旧管理',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9040,'asset:inventory:create','资产盘点创建',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9041,'asset:inventory:list','查看盘点',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9042,'asset:list','查看资产',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9043,'insp:alert:edit','编辑预警',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9044,'insp:alert:manage','管理预警',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9045,'insp:alert:view','查看预警',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9046,'insp:appeal:view','申诉查看',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9047,'insp:audit:view','审计查看',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9048,'inspection_appeal:create','创建申诉',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9049,'inspection_appeal:review','审核申诉',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:27','2026-06-01 23:34:27',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9050,'inspection_appeal:view','查看申诉',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9051,'system:message:manage','系统消息管理',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9052,'insp:analytics:export','分析导出',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9053,'insp:analytics:manage','管理分析',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9054,'insp:analytics:view','分析查看',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9055,'insp:catalog:create','创建分类',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9056,'insp:catalog:delete','删除分类',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9057,'insp:catalog:edit','编辑分类',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9058,'insp:catalog:manage','分类管理',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9059,'insp:catalog:view','分类查看',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9060,'insp:corrective:create','创建整改',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9061,'insp:corrective:delete','删除整改',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9062,'insp:corrective:execute','执行整改',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9063,'insp:corrective:manage','整改管理',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9064,'insp:corrective:view','整改查看',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9065,'insp:execution:edit','编辑执行',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9066,'insp:execution:view','查看执行',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9067,'insp:iot-sensor:create','创建 IoT 传感器',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9068,'insp:iot-sensor:delete','删除 IoT 传感器',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9069,'insp:iot-sensor:edit','编辑 IoT 传感器',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9070,'insp:iot-sensor:view','查看 IoT 传感器',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9071,'insp:iot-sensor:write-reading','写入 IoT 读数',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9072,'insp:knowledge:create','创建知识库',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9073,'insp:knowledge:manage','管理知识库',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9074,'insp:knowledge:view','查看知识库',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9075,'insp:plan:create','创建计划',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9076,'insp:plan:delete','删除计划',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9077,'insp:plan:edit','编辑计划',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9078,'insp:plan:execute','执行计划',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9079,'insp:plan:view','查看计划',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9080,'insp:platform:manage','检查平台管理',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9081,'insp:platform:view','检查平台查看',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9082,'insp:project:create','项目创建',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9083,'insp:project:delete','删除项目',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9084,'insp:project:edit','项目编辑',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9085,'insp:project:manage','项目管理',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9086,'insp:project:publish','发布项目',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9087,'insp:project:update','项目策略更新',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9088,'insp:received:view','受检主体面查看',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9089,'insp:project:view','项目查看',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9090,'insp:rating:manage','管理评分',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9091,'insp:rating:view','查看评分',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9092,'insp:rating-link:manage','评级链接管理',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9093,'insp:rating-link:view','评级链接查看',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9094,'insp:response-set:create','创建选项集',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9095,'insp:response-set:delete','删除选项集',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9096,'insp:response-set:edit','编辑选项集',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9097,'insp:response-set:manage','选项集管理',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9098,'insp:response-set:view','选项集查看',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9099,'insp:scoring-policy:create','创建评分策略',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9100,'insp:scoring-policy:delete','删除评分策略',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9101,'insp:scoring-policy:edit','编辑评分策略',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9102,'insp:scoring-policy:view','查看评分策略',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9103,'insp:scoring-profile:create','创建评分配置',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9104,'insp:scoring-profile:delete','删除评分配置',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9105,'insp:scoring-profile:edit','评分配置编辑',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9106,'insp:scoring-profile:view','评分配置查看',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9107,'insp:submission:admin','管理提交',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9108,'insp:submission:create','创建提交',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9109,'insp:submission:execute','执行提交',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9110,'insp:submission:view','查看提交',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9111,'insp:sync:access','离线同步',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9112,'insp:task:create','创建任务',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9113,'insp:task:edit','编辑任务',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9114,'insp:task:execute','任务执行',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9115,'insp:task:publish','发布任务',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9116,'insp:task:review','任务审核',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9117,'insp:task:view','任务查看',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9118,'insp:template:create','模板创建',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9119,'insp:template:delete','模板删除',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9120,'insp:template:edit','模板编辑',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9121,'insp:template:publish','模板发布',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9122,'insp:template:update','模板项规则更新',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9123,'insp:template:view','模板查看',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9124,'insp:violation:create','创建违规记录',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9125,'insp:violation:delete','删除违规记录',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9126,'insp:violation:edit','编辑违规记录',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9127,'insp:violation:view','查看违规记录',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9128,'inspection:appeal:create','申诉创建',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9129,'inspection:appeal:review','申诉审核',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9130,'inspection:appeal:view','申诉查看',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9131,'inspection:export:create','创建导出',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9132,'place:add','创建场所',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9133,'place:delete','删除场所',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9134,'place:edit','更新场所',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9135,'place:occupancy','入住管理',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9136,'place:view','查看场所',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9137,'system:announcement','公告管理',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9138,'system:audit','审计日志',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9139,'system:audit:view','查看审计日志',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9140,'system:config','系统配置',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9141,'system:department','部门管理',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9142,'system:department:view','查看部门',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9143,'system:manage','系统管理',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9144,'system:operlog','操作日志',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9145,'system:operlog:export','导出操作日志',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9146,'system:org','组织架构管理',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9147,'system:org:create','创建组织',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9148,'system:org:delete','删除组织',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9149,'system:org:edit','编辑组织类型',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9150,'system:org:update','更新组织',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9151,'system:org:view','查看组织架构',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9152,'system:permission','权限管理',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9153,'system:permission:tree','查看权限树',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9154,'system:place','场所类型管理',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9155,'system:place-type:edit','编辑场所类型',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9156,'system:place-type:view','查看场所类型',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9157,'system:place:add','新增场所类型',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9158,'system:place:delete','删除场所类型',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9159,'system:place:edit','编辑场所类型',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9160,'system:place:view','查看场所类型',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9161,'system:role','角色管理',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9162,'system:user','用户管理',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9163,'system:user:reset','重置密码',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9164,'task:menu','任务管理',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9165,'task:workflow:manage','流程管理权',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9166,'admin:access','管理员访问',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9167,'tenant:create','创建租户',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9168,'tenant:delete','删除租户',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9169,'tenant:update','更新租户',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9170,'tenant:view','查看租户',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9171,'data_module:create','创建数据模块',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9172,'data_module:delete','删除数据模块',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9173,'data_module:update','更新数据模块',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9174,'access:data-permission:view','查看数据权限模块/范围',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9175,'access:relation:view','查看访问关系',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9176,'entity-type-config:view','查看实体类型配置',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9177,'plugin-platform:view','查看插件平台信息',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9178,'entity-event:view','查看实体事件',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9179,'entity-event-type:add','新增事件类型',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9180,'entity-event-type:delete','删除事件类型',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9181,'entity-event-type:edit','编辑事件类型',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9182,'entity-event-type:view','查看事件类型',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9183,'event-trigger:add','新增事件触发器',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9184,'event-trigger:delete','删除事件触发器',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9185,'event-trigger:edit','编辑事件触发器',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9186,'event-trigger:view','查看事件触发器',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9187,'msg-config:create','创建消息配置',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9188,'msg-config:delete','删除消息配置',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9189,'msg-config:edit','编辑消息配置',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9190,'msg-config:view','查看消息配置',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9191,'msg-notification:delete','删除通知',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9192,'msg-notification:edit','编辑通知',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9193,'msg-notification:view','查看通知',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9194,'my:schedule:view','查看我的课表',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9195,'my:substitute:view','查看我的代课',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9196,'my:user_student:view','查看我的学生',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'CORE','com.school.management.infrastructure.extension.plugins.core.CorePermissionProvider','PLUGIN:CORE@1.0.0'),(9197,'academic:major:view','查看专业',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9198,'academic:major:edit','编辑专业',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9199,'academic:course:view','查看课程',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9200,'academic:course:edit','编辑课程',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9201,'academic:curriculum:view','查看培养方案',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9202,'academic:curriculum:edit','编辑培养方案',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9203,'academic:grade-direction:view','查看年级方向',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9204,'academic:grade-direction:edit','编辑年级方向',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9205,'student:info:view','查看学生信息',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9206,'student:info:add','新增学生',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9207,'student:info:edit','编辑学生',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9208,'student:info:delete','删除学生',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9209,'student:info:import','导入学生',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9210,'student:info:export','导出学生',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9211,'student:class:view','查看班级',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9212,'student:class:add','新增班级',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9213,'student:class:edit','编辑班级',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9214,'student:class:delete','删除班级',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9215,'student:department:view','查看院系',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9216,'student:department:add','新增院系',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9217,'student:department:edit','编辑院系',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9218,'student:department:delete','删除院系',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9219,'student:dormitory:view','查看宿舍',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9220,'student:dormitory:add','新增宿舍',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9221,'student:dormitory:edit','编辑宿舍',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9222,'student:dormitory:delete','删除宿舍',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9223,'student:grade:view','查看成绩',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9224,'student:grade:edit','编辑成绩',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9225,'student:grade:delete','删除成绩',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9226,'teaching:classroom:view','查看教室',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9227,'teaching:classroom:list','教室列表',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9228,'teaching:classroom:add','新增教室',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9229,'teaching:classroom:edit','编辑教室',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9230,'teaching:classroom:delete','删除教室',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:28','2026-06-01 23:34:28',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9231,'dormitory:student:assign','分配学生到宿舍',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9232,'system:semester:list','查看学期列表',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9233,'system:semester:query','查询学期',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9234,'system:semester:add','新增学期',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9235,'system:semester:edit','编辑学期',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9236,'system:semester:delete','删除学期',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9237,'system:building:view','查看楼栋',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9238,'system:building:add','新增楼栋',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9239,'system:building:edit','编辑楼栋',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9240,'system:building:delete','删除楼栋',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9241,'system:dormitory_building:view','查看宿舍楼',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9242,'system:dormitory_building:edit','编辑宿舍楼',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9243,'system:dormitory_building:assign_manager','分配宿管员',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9244,'academic','学术管理',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9245,'academic:course','课程管理',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9246,'academic:curriculum','培养方案管理',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9247,'academic:grade-direction','年级专业方向',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9248,'academic:major','专业管理',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9249,'student:class','班级管理',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9250,'student:create','创建学生',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9251,'student:delete','删除学生',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9252,'student:department','学生部门管理',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9253,'student:grade','年级管理',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9254,'student:info','学生信息',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9255,'student:manage','学生管理',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9256,'student:update','更新学生',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9257,'student:view','查看学生',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9258,'student:cohort:edit','编辑学生届',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9259,'student:cohort:view','查看学生届',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9260,'student:myclass:view','查看我的班级',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9261,'dormitory:building:view','楼栋查看',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9262,'dormitory:create','创建宿舍',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9263,'dormitory:manage','宿舍管理',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9264,'dormitory:room:view','宿舍查看',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9265,'dormitory:update','更新宿舍',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9266,'dormitory:view','查看宿舍',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9267,'enrollment','招生管理',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9268,'enrollment:edit','编辑招生',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9269,'enrollment:view','查看招生',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9270,'teacher:profile','教师档案',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9271,'teacher:profile:edit','编辑教师档案',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9272,'teacher:profile:view','查看教师档案',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9273,'teaching:building:list','教学楼栋列表',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9274,'teaching:manage','教学管理',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9275,'teaching:workflow:edit','编辑教学流程',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9276,'teaching:workflow:view','查看教学流程',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9277,'inspection_record:view','查看检查记录',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9278,'schedule:policy:manage','排班策略管理',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9279,'schedule:policy:view','排班管理',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9280,'quantification:check-record:publish','发布检查记录',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9281,'quantification:check-record:review','审核检查记录',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9282,'quantification:config:add','新增量化配置',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9283,'quantification:config:delete','删除量化配置',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9284,'quantification:config:edit','编辑量化配置',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0'),(9285,'quantification:config:view','查看量化配置',0,1,'MANAGEMENT',NULL,NULL,NULL,0,1,'2026-06-01 23:34:29','2026-06-01 23:34:29',0,1,1,'',2,'EDU','com.school.management.infrastructure.extension.plugins.education.EducationPermissionProvider','PLUGIN:EDU@1.0.0');
/*!40000 ALTER TABLE `permissions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `place_batch_job_items`
--

DROP TABLE IF EXISTS `place_batch_job_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `place_batch_job_items` (
  `item_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `job_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '所属任务ID',
  `item_index` int NOT NULL COMMENT '项目索引（处理顺序）',
  `resource_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '资源类型: PLACE/OCCUPANT',
  `resource_id` bigint NOT NULL COMMENT '资源ID',
  `resource_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '资源名称（冗余）',
  `item_status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '项目状态: PENDING/SUCCESS/FAILED/SKIPPED',
  `operation_data` json DEFAULT NULL COMMENT '操作数据（如 {orgUnitId: 123}）',
  `error_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '错误代码',
  `error_message` text COLLATE utf8mb4_unicode_ci COMMENT '错误消息',
  `processed_at` datetime DEFAULT NULL COMMENT '处理时间',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '重试次数',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`item_id`),
  KEY `idx_job_id` (`job_id`,`item_index`) COMMENT '按任务ID查询明细',
  KEY `idx_job_status` (`job_id`,`item_status`) COMMENT '按任务ID+状态查询失败项',
  KEY `idx_tenant` (`tenant_id`),
  CONSTRAINT `place_batch_job_items_ibfk_1` FOREIGN KEY (`job_id`) REFERENCES `place_batch_jobs` (`job_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='批量任务项明细表（断点续传支持）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `place_batch_job_items`
--

LOCK TABLES `place_batch_job_items` WRITE;
/*!40000 ALTER TABLE `place_batch_job_items` DISABLE KEYS */;
/*!40000 ALTER TABLE `place_batch_job_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `place_batch_jobs`
--

DROP TABLE IF EXISTS `place_batch_jobs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `place_batch_jobs` (
  `job_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务ID (UUID)',
  `job_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务类型: BATCH_ASSIGN_ORG/BATCH_CHECK_IN/BATCH_UPDATE_STATUS/BATCH_SET_CAPACITY',
  `job_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '任务名称（用户自定义）',
  `job_status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '任务状态: PENDING/RUNNING/COMPLETED/FAILED/PARTIALLY_COMPLETED/CANCELLED',
  `total_items` int NOT NULL COMMENT '总项目数',
  `processed_items` int NOT NULL DEFAULT '0' COMMENT '已处理项目数',
  `success_count` int NOT NULL DEFAULT '0' COMMENT '成功数量',
  `failure_count` int NOT NULL DEFAULT '0' COMMENT '失败数量',
  `skipped_count` int NOT NULL DEFAULT '0' COMMENT '跳过数量（如重复操作）',
  `failure_details` json DEFAULT NULL COMMENT '失败详情 [{itemId, itemName, errorCode, errorMessage}]',
  `request_parameters` json NOT NULL COMMENT '请求参数（完整输入）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `started_at` datetime DEFAULT NULL COMMENT '开始执行时间',
  `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
  `estimated_completion` datetime DEFAULT NULL COMMENT '预计完成时间',
  `created_by` bigint NOT NULL COMMENT '创建用户ID',
  `created_by_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建用户名（冗余）',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '重试次数',
  `max_retries` int NOT NULL DEFAULT '3' COMMENT '最大重试次数',
  `last_error` text COLLATE utf8mb4_unicode_ci COMMENT '最后一次错误信息',
  `result_summary` json DEFAULT NULL COMMENT '执行摘要 {duration, avgItemTime, peakMemory}',
  `result_file_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '详细结果文件URL（大批量任务）',
  `progress_percentage` decimal(5,2) GENERATED ALWAYS AS ((case when (`total_items` = 0) then 0 else ((`processed_items` * 100.0) / `total_items`) end)) STORED COMMENT '进度百分比（自动计算）',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`job_id`),
  KEY `idx_status_created` (`job_status`,`created_at` DESC) COMMENT '按状态查询待处理任务',
  KEY `idx_created_by` (`created_by`,`created_at` DESC) COMMENT '按创建用户查询任务历史',
  KEY `idx_job_type` (`job_type`,`created_at` DESC) COMMENT '按任务类型查询',
  KEY `idx_created_at` (`created_at` DESC) COMMENT '按创建时间倒序查询',
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场所批量操作任务表（对标AWS Batch Operations）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `place_batch_jobs`
--

LOCK TABLES `place_batch_jobs` WRITE;
/*!40000 ALTER TABLE `place_batch_jobs` DISABLE KEYS */;
INSERT INTO `place_batch_jobs` (`job_id`, `job_type`, `job_name`, `job_status`, `total_items`, `processed_items`, `success_count`, `failure_count`, `skipped_count`, `failure_details`, `request_parameters`, `created_at`, `started_at`, `completed_at`, `estimated_completion`, `created_by`, `created_by_name`, `retry_count`, `max_retries`, `last_error`, `result_summary`, `result_file_url`, `tenant_id`) VALUES ('27b5015b-5dca-11f1-9178-54e1ad81342d','BATCH_ASSIGN_ORG','批量分配宿舍到新部门','COMPLETED',100,100,98,2,0,NULL,'{\"reason\": \"组织架构调整\", \"placeIds\": [1, 2, 3, 4, 5], \"targetOrgUnitId\": 200}','2026-06-01 22:57:05',NULL,NULL,NULL,1,'admin',0,3,NULL,NULL,NULL,1),('64feb654-5dcb-11f1-9178-54e1ad81342d','BATCH_ASSIGN_ORG','批量分配宿舍到新部门','COMPLETED',100,100,98,2,0,NULL,'{\"reason\": \"组织架构调整\", \"placeIds\": [1, 2, 3, 4, 5], \"targetOrgUnitId\": 200}','2026-06-01 23:05:57',NULL,NULL,NULL,1,'admin',0,3,NULL,NULL,NULL,1);
/*!40000 ALTER TABLE `place_batch_jobs` ENABLE KEYS */;
UNLOCK TABLES;


--
-- Table structure for table `place_capacity_stats_mv`
--

DROP TABLE IF EXISTS `place_capacity_stats_mv`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `place_capacity_stats_mv` (
  `place_type_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '场所类型代码',
  `place_type_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '场所类型名称（冗余）',
  `total_places` int NOT NULL COMMENT '场所总数',
  `total_capacity` int NOT NULL COMMENT '总容量',
  `total_occupancy` int NOT NULL COMMENT '总占用',
  `avg_occupancy_rate` decimal(5,2) NOT NULL COMMENT '平均占用率（%）',
  `high_occupancy_count` int NOT NULL COMMENT '高占用场所数（>= 80%）',
  `full_occupancy_count` int NOT NULL COMMENT '满员场所数（100%）',
  `empty_count` int NOT NULL COMMENT '空置场所数（0%）',
  `last_refreshed` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后刷新时间',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`place_type_code`),
  KEY `idx_last_refreshed` (`last_refreshed`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场所容量统计物化视图（定时刷新）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `place_capacity_stats_mv`
--

LOCK TABLES `place_capacity_stats_mv` WRITE;
/*!40000 ALTER TABLE `place_capacity_stats_mv` DISABLE KEYS */;
INSERT INTO `place_capacity_stats_mv` VALUES ('ACTIVITY','活动区',0,0,0,0.00,0,0,0,'2026-06-01 23:05:58',1),('CLASSROOM','教室',0,0,0,0.00,0,0,0,'2026-06-01 23:05:58',1),('COMPUTER_ROOM','机房',0,0,0,0.00,0,0,0,'2026-06-01 23:05:58',1),('DORM_BUILDING','宿舍楼',0,0,0,0.00,0,0,0,'2026-06-01 23:05:58',1),('DORM_FLOOR','宿舍楼层',0,0,0,0.00,0,0,0,'2026-06-01 23:05:58',1),('DORM_ROOM','宿舍房间',0,0,0,0.00,0,0,0,'2026-06-01 23:05:58',1),('DORMITORY','宿舍区',0,0,0,0.00,0,0,0,'2026-06-01 23:05:58',1),('GYM','体育馆',0,0,0,0.00,0,0,0,'2026-06-01 23:05:58',1),('LAB','实验室',0,0,0,0.00,0,0,0,'2026-06-01 23:05:58',1),('LECTURE_HALL','报告厅',0,0,0,0.00,0,0,0,'2026-06-01 23:05:58',1),('LIBRARY','图书馆',0,0,0,0.00,0,0,0,'2026-06-01 23:05:58',1),('MEETING_ROOM','会议室',0,0,0,0.00,0,0,0,'2026-06-01 23:05:58',1),('OFFICE','办公区',0,0,0,0.00,0,0,0,'2026-06-01 23:05:58',1),('PLAYGROUND','运动场',0,0,0,0.00,0,0,0,'2026-06-01 23:05:58',1),('TEACH_BUILDING','教学楼',0,0,0,0.00,0,0,0,'2026-06-01 23:05:58',1),('TEACHING','教学区',0,0,0,0.00,0,0,0,'2026-06-01 23:05:58',1);
/*!40000 ALTER TABLE `place_capacity_stats_mv` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `place_categories`
--

DROP TABLE IF EXISTS `place_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `place_categories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类编码',
  `category_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类名称',
  `apply_to_level` enum('BUILDING','ROOM') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '适用层级：楼栋或房间',
  `icon` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图标',
  `color` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '颜色',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
  `has_capacity` tinyint(1) DEFAULT '0' COMMENT '是否有容量',
  `capacity_unit` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '容量单位：人/床位/座位',
  `default_capacity` int DEFAULT NULL COMMENT '默认容量',
  `bookable` tinyint(1) DEFAULT '0' COMMENT '是否可预订',
  `assignable` tinyint(1) DEFAULT '0' COMMENT '是否可分配给组织/班级',
  `occupiable` tinyint(1) DEFAULT '0' COMMENT '是否可入住',
  `has_gender` tinyint(1) DEFAULT '0' COMMENT '是否区分性别（宿舍用）',
  `is_system` tinyint(1) DEFAULT '0' COMMENT '是否系统预置',
  `is_enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` int DEFAULT '0',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_code` (`category_code`),
  KEY `idx_apply_to_level` (`apply_to_level`),
  KEY `idx_enabled` (`is_enabled`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='空间分类配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `place_categories`
--

LOCK TABLES `place_categories` WRITE;
/*!40000 ALTER TABLE `place_categories` DISABLE KEYS */;
INSERT INTO `place_categories` VALUES (1,'TEACHING_BUILDING','教学楼','BUILDING','School','#52c41a','教学用途楼栋',0,NULL,NULL,0,1,0,0,1,1,1,NULL,'2026-06-01 22:57:02',NULL,'2026-06-01 22:57:02',0,1),(2,'DORMITORY_BUILDING','宿舍楼','BUILDING','Home','#1890ff','学生宿舍楼栋',0,NULL,NULL,0,1,0,0,1,1,2,NULL,'2026-06-01 22:57:02',NULL,'2026-06-01 22:57:02',0,1),(3,'OFFICE_BUILDING','办公楼','BUILDING','OfficeBuilding','#fa8c16','行政办公楼栋',0,NULL,NULL,0,1,0,0,1,1,3,NULL,'2026-06-01 22:57:02',NULL,'2026-06-01 22:57:02',0,1),(4,'COMPLEX_BUILDING','综合楼','BUILDING','Building','#722ed1','多功能综合楼栋',0,NULL,NULL,0,1,0,0,1,1,4,NULL,'2026-06-01 22:57:02',NULL,'2026-06-01 22:57:02',0,1),(5,'LAB_BUILDING','实验楼','BUILDING','Experiment','#13c2c2','实验教学楼栋',0,NULL,NULL,0,1,0,0,1,1,5,NULL,'2026-06-01 22:57:02',NULL,'2026-06-01 22:57:02',0,1),(6,'DORMITORY','学生宿舍','ROOM','Bed','#1890ff','学生住宿房间',1,'床位',6,0,1,1,1,1,1,10,NULL,'2026-06-01 22:57:02',NULL,'2026-06-01 22:57:02',0,1),(7,'CLASSROOM','普通教室','ROOM','Book','#52c41a','普通教学教室',1,'座位',50,1,1,0,0,1,1,11,NULL,'2026-06-01 22:57:02',NULL,'2026-06-01 22:57:02',0,1),(8,'MULTIMEDIA_ROOM','多媒体教室','ROOM','Monitor','#52c41a','配备多媒体设备的教室',1,'座位',60,1,1,0,0,1,1,12,NULL,'2026-06-01 22:57:02',NULL,'2026-06-01 22:57:02',0,1),(9,'LAB','实验室','ROOM','Flask','#13c2c2','实验教学用房',1,'座位',30,1,1,0,0,1,1,13,NULL,'2026-06-01 22:57:02',NULL,'2026-06-01 22:57:02',0,1),(10,'COMPUTER_ROOM','机房','ROOM','Desktop','#13c2c2','计算机教学用房',1,'座位',50,1,1,0,0,1,1,14,NULL,'2026-06-01 22:57:02',NULL,'2026-06-01 22:57:02',0,1),(11,'MEETING_ROOM','会议室','ROOM','Users','#722ed1','会议用房',1,'座位',20,1,0,0,0,1,1,15,NULL,'2026-06-01 22:57:02',NULL,'2026-06-01 22:57:02',0,1),(12,'OFFICE','办公室','ROOM','Briefcase','#fa8c16','行政办公用房',1,'人',4,0,1,1,0,1,1,16,NULL,'2026-06-01 22:57:02',NULL,'2026-06-01 22:57:02',0,1),(13,'WAREHOUSE','仓库','ROOM','Box','#8c8c8c','物资存储用房',0,NULL,NULL,0,1,0,0,1,1,17,NULL,'2026-06-01 22:57:02',NULL,'2026-06-01 22:57:02',0,1),(14,'ACTIVITY_ROOM','活动室','ROOM','Trophy','#eb2f96','学生活动用房',1,'人',30,1,0,0,0,1,1,18,NULL,'2026-06-01 22:57:02',NULL,'2026-06-01 22:57:02',0,1);
/*!40000 ALTER TABLE `place_categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `place_class_assignment`
--

DROP TABLE IF EXISTS `place_class_assignment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `place_class_assignment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `place_id` bigint NOT NULL COMMENT '场所ID',
  `org_unit_id` bigint DEFAULT NULL COMMENT '组织单元ID',
  `assigned_beds` int DEFAULT '0' COMMENT '分配床位数',
  `priority` int DEFAULT '0' COMMENT '优先级',
  `status` int DEFAULT '1' COMMENT '状态',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `assigned_by` bigint DEFAULT NULL COMMENT '分配人',
  `assigned_at` datetime DEFAULT NULL COMMENT '分配时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_place_class` (`place_id`),
  KEY `idx_org_unit_id` (`org_unit_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场所班级分配表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `place_class_assignment`
--

LOCK TABLES `place_class_assignment` WRITE;
/*!40000 ALTER TABLE `place_class_assignment` DISABLE KEYS */;
/*!40000 ALTER TABLE `place_class_assignment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `place_occupants`
--

DROP TABLE IF EXISTS `place_occupants`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `place_occupants` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `place_id` bigint NOT NULL COMMENT '场所ID',
  `occupant_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '占用者类型',
  `occupant_id` bigint NOT NULL COMMENT '占用者ID',
  `occupant_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '占用者名称(冗余)',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户名',
  `org_unit_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '组织名称(冗余)',
  `gender` tinyint DEFAULT NULL COMMENT '性别: 0-未知 1-男 2-女',
  `position_no` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '位置号(床位号/工位号)',
  `check_in_time` datetime NOT NULL COMMENT '入住时间',
  `check_out_time` datetime DEFAULT NULL COMMENT '退出时间',
  `status` tinyint DEFAULT '1' COMMENT '状态: 0-已退出 1-在住',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` int DEFAULT '0',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_space_id` (`place_id`),
  KEY `idx_occupant` (`occupant_type`,`occupant_id`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_occupants_place_status` (`place_id`,`status`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='空间占用记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `place_occupants`
--

LOCK TABLES `place_occupants` WRITE;
/*!40000 ALTER TABLE `place_occupants` DISABLE KEYS */;
/*!40000 ALTER TABLE `place_occupants` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `place_type_config`
--

DROP TABLE IF EXISTS `place_type_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `place_type_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型编码',
  `type_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型名称',
  `type_category` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型分类: BUILDING/ROOM',
  `icon` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图标名称(lucide图标)',
  `color` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '主题色(tailwind类名)',
  `has_capacity` tinyint DEFAULT '1' COMMENT '是否有容量概念',
  `has_occupancy` tinyint DEFAULT '0' COMMENT '是否有入住/使用人员',
  `has_gender` tinyint DEFAULT '0' COMMENT '是否区分性别',
  `default_capacity` int DEFAULT NULL COMMENT '默认容量',
  `attribute_schema` json DEFAULT NULL COMMENT '扩展属性JSON Schema',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `enabled` tinyint DEFAULT '1' COMMENT '是否启用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `type_code` (`type_code`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场所类型配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `place_type_config`
--

LOCK TABLES `place_type_config` WRITE;
/*!40000 ALTER TABLE `place_type_config` DISABLE KEYS */;
INSERT INTO `place_type_config` VALUES (1,'TEACHING','教学楼','BUILDING','GraduationCap','blue',0,0,0,NULL,NULL,1,1,'2026-06-01 22:57:19','2026-06-01 22:57:19',1),(2,'DORMITORY_BUILDING','宿舍楼','BUILDING','Home','teal',0,0,0,NULL,NULL,2,1,'2026-06-01 22:57:19','2026-06-01 22:57:19',1),(3,'OFFICE_BUILDING','办公楼','BUILDING','Building2','gray',0,0,0,NULL,NULL,3,1,'2026-06-01 22:57:19','2026-06-01 22:57:19',1),(4,'MIXED','综合楼','BUILDING','Layers','purple',0,0,0,NULL,NULL,4,1,'2026-06-01 22:57:19','2026-06-01 22:57:19',1),(5,'DORMITORY','学生宿舍','ROOM','BedDouble','teal',1,1,1,6,NULL,10,1,'2026-06-01 22:57:19','2026-06-01 22:57:19',1),(6,'STAFF_DORMITORY','教职工宿舍','ROOM','Bed','cyan',1,1,0,2,NULL,11,1,'2026-06-01 22:57:19','2026-06-01 22:57:19',1),(7,'CLASSROOM','普通教室','ROOM','School','blue',1,0,0,50,NULL,20,1,'2026-06-01 22:57:19','2026-06-01 22:57:19',1),(8,'MULTIMEDIA','多媒体教室','ROOM','Monitor','indigo',1,0,0,60,NULL,21,1,'2026-06-01 22:57:19','2026-06-01 22:57:19',1),(9,'SMART_CLASSROOM','智慧教室','ROOM','Cpu','violet',1,0,0,40,NULL,22,1,'2026-06-01 22:57:19','2026-06-01 22:57:19',1),(10,'LAB','实验室','ROOM','FlaskConical','amber',1,0,0,30,NULL,30,1,'2026-06-01 22:57:19','2026-06-01 22:57:19',1),(11,'COMPUTER_LAB','计算机房','ROOM','Monitor','sky',1,0,0,50,NULL,31,1,'2026-06-01 22:57:19','2026-06-01 22:57:19',1),(12,'TRAINING','实训室','ROOM','Wrench','orange',1,0,0,40,NULL,32,1,'2026-06-01 22:57:19','2026-06-01 22:57:19',1),(13,'OFFICE','办公室','ROOM','Briefcase','slate',1,1,0,4,NULL,40,1,'2026-06-01 22:57:19','2026-06-01 22:57:19',1),(14,'MEETING','会议室','ROOM','Users','emerald',1,0,0,20,NULL,41,1,'2026-06-01 22:57:19','2026-06-01 22:57:19',1),(15,'LIBRARY','图书馆/阅览室','ROOM','BookOpen','amber',1,0,0,100,NULL,50,1,'2026-06-01 22:57:19','2026-06-01 22:57:19',1),(16,'STORAGE','仓库','ROOM','Package','stone',0,0,0,NULL,NULL,60,1,'2026-06-01 22:57:19','2026-06-01 22:57:19',1),(17,'UTILITY','功能房','ROOM','Settings','zinc',0,0,0,NULL,NULL,70,1,'2026-06-01 22:57:19','2026-06-01 22:57:19',1),(18,'BATHROOM','卫生间','ROOM','Bath','gray',0,0,1,NULL,NULL,71,1,'2026-06-01 22:57:19','2026-06-01 22:57:19',1),(19,'POWER_ROOM','配电室','ROOM','Zap','yellow',0,0,0,NULL,NULL,72,1,'2026-06-01 22:57:19','2026-06-01 22:57:19',1);
/*!40000 ALTER TABLE `place_type_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `places`
--

DROP TABLE IF EXISTS `places`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `places` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `place_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '场所编码',
  `place_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '场所名称',
  `type_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '空间类型编码',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
  `parent_id` bigint DEFAULT NULL COMMENT '父级ID',
  `path` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '物化路径',
  `level` int DEFAULT '0' COMMENT '层级深度',
  `capacity` int DEFAULT NULL COMMENT '容量',
  `current_occupancy` int DEFAULT '0' COMMENT '当前占用数',
  `effective_org_unit_id` bigint DEFAULT NULL COMMENT '有效组织ID投影列(解析后含继承; 真相=belongs_to 关系; PlaceOrgProjector 维护, 业务禁直写)',
  `status` tinyint DEFAULT '1' COMMENT '状态: 0-停用 1-正常 2-维护中',
  `attributes` json DEFAULT NULL COMMENT '扩展属性值',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` int DEFAULT '0',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `gender` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '性别限制: MALE/FEMALE/MIXED (NULL=继承父节点)',
  `occupancy_rate` decimal(5,2) GENERATED ALWAYS AS ((case when (`capacity` > 0) then ((`current_occupancy` * 100.0) / `capacity`) else 0 end)) STORED COMMENT '占用率（%）- 自动计算',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_space_code` (`place_code`),
  UNIQUE KEY `uk_parent_place_code` (`parent_id`,`place_code`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_places_path` (`path`(100)),
  KEY `idx_places_parent_id` (`parent_id`),
  KEY `idx_places_type_code` (`type_code`),
  KEY `idx_places_org_unit_id` (`effective_org_unit_id`),
  KEY `idx_places_status` (`status`),
  KEY `idx_places_type_status` (`type_code`,`status`),
  KEY `idx_tenant` (`tenant_id`),
  KEY `idx_places_gender` (`gender`),
  KEY `idx_parent_place_code` (`parent_id`,`place_code`,`deleted`),
  KEY `idx_high_occupancy` (`type_code`,`occupancy_rate` DESC,`id`),
  KEY `idx_capacity_range` (`type_code`,`capacity`,`current_occupancy`),
  KEY `idx_available_capacity` (`type_code`,`capacity`,`current_occupancy`,`id`),
  KEY `idx_parent_inheritance` (`parent_id`,`effective_org_unit_id`) COMMENT '父级+有效组织索引(归属真相在 belongs_to 关系, 本列为投影)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='空间实例表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `places`
--

LOCK TABLES `places` WRITE;
/*!40000 ALTER TABLE `places` DISABLE KEYS */;
/*!40000 ALTER TABLE `places` ENABLE KEYS */;
UNLOCK TABLES;
-- trg_cascade_org_unit_update 触发器已删除 (V20260612_2 场所归属关系化):
-- 归属审计由 PlaceEventHandler 监听 PlaceOrgAssignedEvent 写 place_audit_logs;
-- effective_org_unit_id 是投影列, 触发器若留存会把投影器机械重算记成业务审计。

--
-- Table structure for table `plugin_packages`
--

DROP TABLE IF EXISTS `plugin_packages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `plugin_packages` (
  `industry_code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '行业包代码,如 CORE/EDU/HEALTH',
  `industry_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '中文名,如 通用核心/教育行业',
  `version` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1.0.0' COMMENT 'SemVer 版本号',
  `depends_on` json DEFAULT NULL COMMENT '依赖行业包数组 ["CORE"]',
  `manifest_class` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Manifest 实现类全限定名',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用(1=启用/0=禁用)',
  `uninstall_policy` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SOFT' COMMENT 'SOFT=软删/HARD=硬删',
  `installed_at` datetime DEFAULT NULL COMMENT '首次注册时间',
  `last_started_at` datetime DEFAULT NULL COMMENT '最近一次启动时间',
  `last_disabled_at` datetime DEFAULT NULL COMMENT '最近禁用时间',
  PRIMARY KEY (`industry_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行业插件包元信息注册表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `plugin_packages`
--

LOCK TABLES `plugin_packages` WRITE;
/*!40000 ALTER TABLE `plugin_packages` DISABLE KEYS */;
INSERT INTO `plugin_packages` VALUES ('COMMON_EXT','跨行业通用扩展','1.0.0','[\"CORE\"]','com.school.management.infrastructure.extension.plugins.common.CommonExtManifest',1,'SOFT','2026-06-01 23:28:46','2026-06-01 23:42:31',NULL),('CORE','通用核心','1.0.0','[]','com.school.management.infrastructure.extension.plugins.core.CoreManifest',1,'SOFT','2026-06-01 23:28:46','2026-06-01 23:42:31',NULL),('EDU','教育行业','1.0.0','[\"CORE\"]','com.school.management.infrastructure.extension.plugins.education.EducationManifest',1,'SOFT','2026-06-01 23:28:46','2026-06-01 23:42:31',NULL);
/*!40000 ALTER TABLE `plugin_packages` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `positions`
--

DROP TABLE IF EXISTS `positions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `positions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `position_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '岗位编码',
  `position_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '岗位名称',
  `org_unit_id` bigint NOT NULL COMMENT '所属组织单元',
  `job_level` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '职级(HIGH/MIDDLE/BASE/EXECUTIVE)',
  `headcount` int DEFAULT '1' COMMENT '编制数',
  `reports_to_id` bigint DEFAULT NULL COMMENT '汇报岗位ID',
  `responsibilities` text COLLATE utf8mb4_unicode_ci COMMENT '岗位职责',
  `requirements` text COLLATE utf8mb4_unicode_ci COMMENT '任职要求',
  `sort_order` int DEFAULT '0',
  `is_key_position` tinyint(1) DEFAULT '0' COMMENT '是否关键岗位',
  `enabled` tinyint(1) DEFAULT '1',
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `deleted` tinyint DEFAULT '0',
  `version` int DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_org_unit_id` (`org_unit_id`),
  KEY `idx_reports_to` (`reports_to_id`),
  KEY `idx_tenant` (`tenant_id`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='岗位表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `positions`
--

LOCK TABLES `positions` WRITE;
/*!40000 ALTER TABLE `positions` DISABLE KEYS */;
/*!40000 ALTER TABLE `positions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `quantification_dict_categories`
--

DROP TABLE IF EXISTS `quantification_dict_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quantification_dict_categories` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `category_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类别编码',
  `category_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类别名称',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
  `status` tinyint DEFAULT '1' COMMENT '状态: 1=启用, 0=禁用',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除: 0=未删除, 1=已删除',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_code` (`category_code`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='量化类型字典表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `quantification_dict_categories`
--

LOCK TABLES `quantification_dict_categories` WRITE;
/*!40000 ALTER TABLE `quantification_dict_categories` DISABLE KEYS */;
INSERT INTO `quantification_dict_categories` VALUES (1,'hygiene','卫生检查','宿舍/教室卫生情况检查',1,1,'2026-06-01 22:56:58','2026-06-01 22:56:58',0,1),(2,'discipline','纪律检查','学生日常行为纪律检查',1,2,'2026-06-01 22:56:58','2026-06-01 22:56:58',0,1),(3,'safety','安全检查','消防安全、用电安全等检查',1,3,'2026-06-01 22:56:58','2026-06-01 22:56:58',0,1),(4,'attendance','考勤检查','出勤、迟到、早退等检查',1,4,'2026-06-01 22:56:58','2026-06-01 22:56:58',0,1);
/*!40000 ALTER TABLE `quantification_dict_categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rating_alert_config`
--

DROP TABLE IF EXISTS `rating_alert_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rating_alert_config` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `check_plan_id` bigint NOT NULL COMMENT '检查计划ID',
  `rule_id` bigint NOT NULL COMMENT '评级规则ID',
  `alert_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '预警名称',
  `alert_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '预警类型: CONTINUOUS_POOR连续较差/DECLINING_TREND下降趋势/BELOW_AVERAGE低于平均',
  `trigger_condition` json DEFAULT NULL COMMENT '触发条件配置',
  `alert_level` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '预警级别: WARNING警告/SERIOUS严重/URGENT紧急',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `notify_roles` json DEFAULT NULL COMMENT '通知角色: ["班主任","年级主任","院系领导"]',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_check_plan_id` (`check_plan_id`),
  KEY `idx_rule_id` (`rule_id`),
  KEY `idx_alert_type` (`alert_type`),
  KEY `idx_enabled` (`enabled`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级预警配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rating_alert_config`
--

LOCK TABLES `rating_alert_config` WRITE;
/*!40000 ALTER TABLE `rating_alert_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `rating_alert_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rating_alert_record`
--

DROP TABLE IF EXISTS `rating_alert_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rating_alert_record` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `alert_config_id` bigint NOT NULL COMMENT '预警配置ID',
  `class_id` bigint NOT NULL COMMENT '班级ID',
  `class_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '班级名称（冗余）',
  `triggered_at` datetime NOT NULL COMMENT '触发时间',
  `alert_reason` text COLLATE utf8mb4_unicode_ci COMMENT '预警原因详情',
  `alert_data` json DEFAULT NULL COMMENT '预警数据（如连续记录、趋势数据等）',
  `notification_sent` tinyint(1) DEFAULT '0' COMMENT '是否已发送通知',
  `notification_sent_at` datetime DEFAULT NULL COMMENT '通知发送时间',
  `handled` tinyint(1) DEFAULT '0' COMMENT '是否已处理',
  `handled_at` datetime DEFAULT NULL COMMENT '处理时间',
  `handler_id` bigint DEFAULT NULL COMMENT '处理人ID',
  `handle_remark` text COLLATE utf8mb4_unicode_ci COMMENT '处理备注',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_alert_config_id` (`alert_config_id`),
  KEY `idx_class_id` (`class_id`),
  KEY `idx_triggered_at` (`triggered_at`),
  KEY `idx_handled` (`handled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级预警记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rating_alert_record`
--

LOCK TABLES `rating_alert_record` WRITE;
/*!40000 ALTER TABLE `rating_alert_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `rating_alert_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rating_calculation_detail`
--

DROP TABLE IF EXISTS `rating_calculation_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rating_calculation_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rating_result_id` bigint NOT NULL COMMENT '评级结果ID',
  `source_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '来源类型：CHECK_RECORD（检查记录）/DAILY_SUMMARY（每日汇总）',
  `source_id` bigint NOT NULL COMMENT '来源ID（检查记录ID或每日汇总ID）',
  `check_date` date DEFAULT NULL COMMENT '检查日期',
  `score_contribution` decimal(10,2) DEFAULT NULL COMMENT '对最终分数的贡献',
  `weight_used` decimal(5,4) DEFAULT NULL COMMENT '使用的权重',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_rating_result` (`rating_result_id`),
  KEY `idx_source` (`source_type`,`source_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级计算明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rating_calculation_detail`
--

LOCK TABLES `rating_calculation_detail` WRITE;
/*!40000 ALTER TABLE `rating_calculation_detail` DISABLE KEYS */;
/*!40000 ALTER TABLE `rating_calculation_detail` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rating_change_log`
--

DROP TABLE IF EXISTS `rating_change_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rating_change_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rating_result_id` bigint NOT NULL COMMENT '评级结果ID',
  `org_unit_id` bigint DEFAULT NULL,
  `change_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '变更类型：SCORE_CHANGE（分数变更）/RANKING_CHANGE（排名变更）/STATUS_CHANGE（状态变更）/CREATED（新建）/REVOKED（撤销）',
  `old_score` decimal(10,2) DEFAULT NULL COMMENT '原分数',
  `new_score` decimal(10,2) DEFAULT NULL COMMENT '新分数',
  `old_ranking` int DEFAULT NULL COMMENT '原排名',
  `new_ranking` int DEFAULT NULL COMMENT '新排名',
  `old_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '原状态',
  `new_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '新状态',
  `change_reason` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '变更原因：APPEAL（申诉）/RECALCULATE（重新计算）/MANUAL_ADJUST（手动调整）',
  `related_appeal_id` bigint DEFAULT NULL COMMENT '关联的申诉ID',
  `remark` text COLLATE utf8mb4_unicode_ci COMMENT '备注',
  `changed_by` bigint DEFAULT NULL COMMENT '操作人ID',
  `changed_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_rating_result` (`rating_result_id`),
  KEY `idx_change_type` (`change_type`),
  KEY `idx_changed_at` (`changed_at`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级变更日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rating_change_log`
--

LOCK TABLES `rating_change_log` WRITE;
/*!40000 ALTER TABLE `rating_change_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `rating_change_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rating_comparison_record`
--

DROP TABLE IF EXISTS `rating_comparison_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rating_comparison_record` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `comparison_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '对比名称',
  `comparison_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '对比类型: TIME_PERIOD时间段/RULE规则/DEPARTMENT院系',
  `comparison_config` json NOT NULL COMMENT '对比配置',
  `result_data` json DEFAULT NULL COMMENT '对比结果数据',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_comparison_type` (`comparison_type`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级对比记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rating_comparison_record`
--

LOCK TABLES `rating_comparison_record` WRITE;
/*!40000 ALTER TABLE `rating_comparison_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `rating_comparison_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rating_config`
--

DROP TABLE IF EXISTS `rating_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rating_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `check_plan_id` bigint NOT NULL COMMENT '检查计划ID',
  `rating_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评级名称（如：优秀班级、卫生班级）',
  `rating_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评级周期类型：DAILY/WEEKLY/MONTHLY',
  `icon` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图标',
  `color` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '颜色（十六进制）',
  `priority` int DEFAULT '999' COMMENT '显示优先级（数字越小越靠前）',
  `division_method` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '划分方式：TOP_N（前N名）/TOP_PERCENT（前X%）/BOTTOM_N（后N名）/BOTTOM_PERCENT（后X%）',
  `division_value` decimal(10,2) NOT NULL COMMENT '划分值（3名或10%）',
  `require_approval` tinyint DEFAULT '1' COMMENT '是否需要审核：0否 1是',
  `auto_publish` tinyint DEFAULT '0' COMMENT '审核通过后自动发布：0否 1是',
  `enabled` tinyint DEFAULT '1' COMMENT '是否启用：0否 1是',
  `sort_order` int DEFAULT '0' COMMENT '排序序号',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '规则说明',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除：0未删除 1已删除',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_check_plan` (`check_plan_id`,`deleted`),
  KEY `idx_rating_type` (`rating_type`,`enabled`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rating_config`
--

LOCK TABLES `rating_config` WRITE;
/*!40000 ALTER TABLE `rating_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `rating_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rating_config_version`
--

DROP TABLE IF EXISTS `rating_config_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rating_config_version` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rating_config_id` bigint NOT NULL COMMENT '评级配置ID',
  `version_number` int NOT NULL COMMENT '版本号',
  `effective_from` datetime NOT NULL COMMENT '生效开始时间',
  `effective_to` datetime DEFAULT NULL COMMENT '生效结束时间（NULL表示当前版本）',
  `config_snapshot` json NOT NULL COMMENT '配置快照',
  `change_reason` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '变更原因',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_config` (`rating_config_id`,`version_number`),
  KEY `idx_effective` (`effective_from`,`effective_to`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级配置版本表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rating_config_version`
--

LOCK TABLES `rating_config_version` WRITE;
/*!40000 ALTER TABLE `rating_config_version` DISABLE KEYS */;
/*!40000 ALTER TABLE `rating_config_version` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rating_configs`
--

DROP TABLE IF EXISTS `rating_configs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rating_configs` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `check_plan_id` bigint DEFAULT NULL COMMENT '检查计划ID',
  `rating_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评级名称',
  `period_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '周期类型',
  `division_method` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '划分方式',
  `division_value` decimal(10,2) DEFAULT NULL COMMENT '划分值',
  `icon` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图标',
  `color` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '颜色',
  `priority` int DEFAULT '0' COMMENT '优先级',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
  `require_approval` tinyint(1) DEFAULT '0' COMMENT '是否需要审批',
  `auto_publish` tinyint(1) DEFAULT '0' COMMENT '是否自动发布',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_check_plan_id` (`check_plan_id`),
  KEY `idx_enabled` (`enabled`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rating_configs`
--

LOCK TABLES `rating_configs` WRITE;
/*!40000 ALTER TABLE `rating_configs` DISABLE KEYS */;
/*!40000 ALTER TABLE `rating_configs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rating_levels`
--

DROP TABLE IF EXISTS `rating_levels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rating_levels` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `rule_id` bigint NOT NULL COMMENT '规则ID',
  `level_code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '等级代码',
  `level_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '等级名称',
  `min_score` decimal(10,2) DEFAULT NULL COMMENT '最小分数',
  `max_score` decimal(10,2) DEFAULT NULL COMMENT '最大分数',
  `min_percent` decimal(5,2) DEFAULT NULL COMMENT '最小百分比',
  `max_percent` decimal(5,2) DEFAULT NULL COMMENT '最大百分比',
  `top_n` int DEFAULT NULL COMMENT '前N名',
  `top_percent` decimal(5,2) DEFAULT NULL COMMENT '前N%',
  `color` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '颜色代码',
  `icon` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图标',
  `reward_points` int DEFAULT '0' COMMENT '奖励积分',
  `penalty_points` int DEFAULT '0' COMMENT '惩罚积分',
  `level_order` int NOT NULL COMMENT '等级顺序',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_rule` (`rule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级等级表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rating_levels`
--

LOCK TABLES `rating_levels` WRITE;
/*!40000 ALTER TABLE `rating_levels` DISABLE KEYS */;
/*!40000 ALTER TABLE `rating_levels` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rating_ranking_source`
--

DROP TABLE IF EXISTS `rating_ranking_source`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rating_ranking_source` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rating_config_id` bigint NOT NULL COMMENT '评级配置ID',
  `source_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '数据源类型：TOTAL_SCORE（总分）/CATEGORY（类别）/DEDUCTION_ITEM（扣分项）',
  `source_id` bigint DEFAULT NULL COMMENT '来源ID（类别ID或扣分项ID，TOTAL_SCORE时为NULL）',
  `use_weighted` tinyint DEFAULT '1' COMMENT '是否使用加权分：0否 1是',
  `weight` decimal(5,4) DEFAULT '1.0000' COMMENT '权重（组合排名时使用，总和必须为1）',
  `missing_data_strategy` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'ZERO' COMMENT '缺失数据策略：ZERO（按0分计）/SKIP（跳过该班级）',
  `sort_order` int DEFAULT '0' COMMENT '排序序号',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rating_source` (`rating_config_id`,`source_type`,`source_id`),
  KEY `idx_rating_config` (`rating_config_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级排名数据源配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rating_ranking_source`
--

LOCK TABLES `rating_ranking_source` WRITE;
/*!40000 ALTER TABLE `rating_ranking_source` DISABLE KEYS */;
/*!40000 ALTER TABLE `rating_ranking_source` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rating_result`
--

DROP TABLE IF EXISTS `rating_result`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rating_result` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rating_config_id` bigint NOT NULL COMMENT '评级配置ID',
  `check_plan_id` bigint NOT NULL COMMENT '检查计划ID',
  `org_unit_id` bigint DEFAULT NULL,
  `period_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '周期类型：DAILY/WEEKLY/MONTHLY',
  `period_value` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '周期值：2025-12-22（日）/2025-W51（周）/2025-12（月）',
  `period_start` date NOT NULL COMMENT '周期开始日期',
  `period_end` date NOT NULL COMMENT '周期结束日期',
  `final_score` decimal(10,2) NOT NULL COMMENT '最终得分（扣分）',
  `ranking` int NOT NULL COMMENT '排名',
  `total_classes` int NOT NULL COMMENT '参与评级的总班级数',
  `result_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'DRAFT' COMMENT '结果状态：DRAFT（草稿）/PENDING_APPROVAL（待审核）/APPROVED（已审核）/PUBLISHED（已发布）/ARCHIVED（已归档）',
  `approved_by` bigint DEFAULT NULL COMMENT '审核人ID',
  `approved_at` datetime DEFAULT NULL COMMENT '审核时间',
  `approval_remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审核备注',
  `published_by` bigint DEFAULT NULL COMMENT '发布人ID',
  `published_at` datetime DEFAULT NULL COMMENT '发布时间',
  `version` int DEFAULT '1' COMMENT '版本号（每次重新计算递增）',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除：0未删除 1已删除',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_class_period` (`rating_config_id`,`org_unit_id`,`period_value`,`deleted`),
  KEY `idx_config_period` (`rating_config_id`,`period_value`),
  KEY `idx_class_period` (`org_unit_id`,`period_type`,`period_value`),
  KEY `idx_status` (`result_status`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级结果表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rating_result`
--

LOCK TABLES `rating_result` WRITE;
/*!40000 ALTER TABLE `rating_result` DISABLE KEYS */;
/*!40000 ALTER TABLE `rating_result` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rating_results`
--

DROP TABLE IF EXISTS `rating_results`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rating_results` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `rating_config_id` bigint NOT NULL COMMENT '评级配置ID',
  `check_plan_id` bigint DEFAULT NULL COMMENT '检查计划ID',
  `org_unit_id` bigint DEFAULT NULL,
  `class_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '班级名称',
  `period_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '周期类型',
  `period_start` date DEFAULT NULL COMMENT '周期开始',
  `period_end` date DEFAULT NULL COMMENT '周期结束',
  `ranking` int DEFAULT NULL COMMENT '排名',
  `final_score` decimal(10,2) DEFAULT NULL COMMENT '最终得分',
  `awarded` tinyint(1) DEFAULT '0' COMMENT '是否获奖',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'CALCULATED' COMMENT '状态',
  `calculated_at` datetime DEFAULT NULL COMMENT '计算时间',
  `submitted_at` datetime DEFAULT NULL COMMENT '提交时间',
  `approved_by` bigint DEFAULT NULL COMMENT '审批人',
  `approved_at` datetime DEFAULT NULL COMMENT '审批时间',
  `approval_comment` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批意见',
  `published_by` bigint DEFAULT NULL COMMENT '发布人',
  `published_at` datetime DEFAULT NULL COMMENT '发布时间',
  `revoked_by` bigint DEFAULT NULL COMMENT '撤销人',
  `revoked_at` datetime DEFAULT NULL COMMENT '撤销时间',
  `reject_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '驳回原因',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_rating_config_id` (`rating_config_id`),
  KEY `idx_class_id` (`org_unit_id`),
  KEY `idx_period` (`period_start`,`period_end`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级结果表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rating_results`
--

LOCK TABLES `rating_results` WRITE;
/*!40000 ALTER TABLE `rating_results` DISABLE KEYS */;
/*!40000 ALTER TABLE `rating_results` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rating_rule_template`
--

DROP TABLE IF EXISTS `rating_rule_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rating_rule_template` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `template_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板名称',
  `template_desc` text COLLATE utf8mb4_unicode_ci COMMENT '模板描述',
  `template_config` json NOT NULL COMMENT '模板配置（完整规则配置JSON）',
  `scene_tag` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '场景标签: DAILY_CHECK日常检查/MONTHLY_SUMMARY月度汇总/COMPETITION竞赛评比',
  `usage_count` int DEFAULT '0' COMMENT '使用次数',
  `is_system` tinyint(1) DEFAULT '0' COMMENT '是否系统预设: 0否 1是',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_scene_tag` (`scene_tag`),
  KEY `idx_is_system` (`is_system`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级规则模板表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rating_rule_template`
--

LOCK TABLES `rating_rule_template` WRITE;
/*!40000 ALTER TABLE `rating_rule_template` DISABLE KEYS */;
INSERT INTO `rating_rule_template` VALUES (1,'三等级标准模板','适用于日常检查的标准三等级评级','{\"levels\": [{\"maxScore\": 5, \"minScore\": 0, \"levelName\": \"优秀\", \"levelColor\": \"#10b981\", \"levelOrder\": 1}, {\"maxScore\": 10, \"minScore\": 5, \"levelName\": \"良好\", \"levelColor\": \"#3b82f6\", \"levelOrder\": 2}, {\"maxScore\": 999, \"minScore\": 10, \"levelName\": \"需改进\", \"levelColor\": \"#ef4444\", \"levelOrder\": 3}], \"ruleType\": \"DAILY\", \"scoreSource\": \"TOTAL\", \"divisionMethod\": \"SCORE_RANGE\"}','DAILY_CHECK',0,1,NULL,'2026-06-01 23:05:51','2026-06-01 23:05:51',0),(2,'竞赛式评比模板','适用于月度评比的竞赛式评级','{\"levels\": [{\"levelName\": \"一等奖\", \"rankCount\": 3, \"levelColor\": \"#fbbf24\", \"levelOrder\": 1}, {\"levelName\": \"二等奖\", \"rankCount\": 5, \"levelColor\": \"#c0c0c0\", \"levelOrder\": 2}, {\"levelName\": \"三等奖\", \"rankCount\": 7, \"levelColor\": \"#cd7f32\", \"levelOrder\": 3}], \"ruleType\": \"SUMMARY\", \"scoreSource\": \"TOTAL\", \"summaryMethod\": \"AVERAGE\", \"divisionMethod\": \"RANK_COUNT\"}','COMPETITION',0,1,NULL,'2026-06-01 23:05:51','2026-06-01 23:05:51',0),(3,'百分比稳定模板','适用于学期汇总的百分比评级','{\"levels\": [{\"levelName\": \"A档\", \"levelColor\": \"#10b981\", \"levelOrder\": 1, \"percentage\": 10}, {\"levelName\": \"B档\", \"levelColor\": \"#3b82f6\", \"levelOrder\": 2, \"percentage\": 20}, {\"levelName\": \"C档\", \"levelColor\": \"#f59e0b\", \"levelOrder\": 3, \"percentage\": 30}, {\"levelName\": \"D档\", \"levelColor\": \"#ef4444\", \"levelOrder\": 4, \"percentage\": 40}], \"ruleType\": \"SUMMARY\", \"scoreSource\": \"TOTAL\", \"summaryMethod\": \"AVERAGE\", \"divisionMethod\": \"PERCENTAGE\"}','MONTHLY_SUMMARY',0,1,NULL,'2026-06-01 23:05:51','2026-06-01 23:05:51',0),(4,'纪律标兵模板','适用于周度纪律评选','{\"levels\": [{\"levelName\": \"纪律标兵\", \"rankCount\": 5, \"levelColor\": \"#fbbf24\", \"levelOrder\": 1}, {\"levelName\": \"纪律优秀\", \"rankCount\": 10, \"levelColor\": \"#10b981\", \"levelOrder\": 2}, {\"levelName\": \"纪律合格\", \"rankCount\": 999, \"levelColor\": \"#6b7280\", \"levelOrder\": 3}], \"ruleType\": \"SUMMARY\", \"scoreSource\": \"CATEGORY\", \"categoryName\": \"纪律\", \"summaryMethod\": \"SUM\", \"divisionMethod\": \"RANK_COUNT\"}','COMPETITION',0,1,NULL,'2026-06-01 23:05:51','2026-06-01 23:05:51',0);
/*!40000 ALTER TABLE `rating_rule_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rating_rules`
--

DROP TABLE IF EXISTS `rating_rules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rating_rules` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `template_id` bigint NOT NULL COMMENT '评级模板ID',
  `rule_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则编码',
  `rule_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则名称',
  `rating_basis` enum('TOTAL','SINGLE_CATEGORY','MULTI_CATEGORY','CUSTOM') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评级依据',
  `category_ids` json DEFAULT NULL COMMENT '依据的类别ID列表',
  `score_type` enum('DEDUCTION','WEIGHTED','FINAL') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '使用的分数类型',
  `rating_method` enum('ABSOLUTE','PERCENTAGE','RANKING') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评级方式',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `is_enabled` tinyint DEFAULT '1' COMMENT '是否启用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_rule` (`template_id`,`rule_code`),
  KEY `idx_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级规则表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rating_rules`
--

LOCK TABLES `rating_rules` WRITE;
/*!40000 ALTER TABLE `rating_rules` DISABLE KEYS */;
/*!40000 ALTER TABLE `rating_rules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rating_statistics`
--

DROP TABLE IF EXISTS `rating_statistics`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rating_statistics` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `check_plan_id` bigint NOT NULL COMMENT '检查计划ID',
  `org_unit_id` bigint DEFAULT NULL,
  `rating_config_id` bigint NOT NULL COMMENT '评级配置ID',
  `period_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '统计周期类型：DAILY/WEEKLY/MONTHLY/ALL',
  `period_value` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '周期值',
  `rating_count` int DEFAULT '0' COMMENT '获得该评级的次数',
  `first_rating_date` date DEFAULT NULL COMMENT '首次获得日期',
  `last_rating_date` date DEFAULT NULL COMMENT '最近获得日期',
  `avg_ranking` decimal(10,2) DEFAULT NULL COMMENT '平均排名',
  `best_ranking` int DEFAULT NULL COMMENT '最佳排名',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stats` (`check_plan_id`,`org_unit_id`,`rating_config_id`,`period_type`,`period_value`),
  KEY `idx_class` (`org_unit_id`),
  KEY `idx_rating_config` (`rating_config_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级统计汇总表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rating_statistics`
--

LOCK TABLES `rating_statistics` WRITE;
/*!40000 ALTER TABLE `rating_statistics` DISABLE KEYS */;
/*!40000 ALTER TABLE `rating_statistics` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rating_templates`
--

DROP TABLE IF EXISTS `rating_templates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rating_templates` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `template_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板编码',
  `template_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板名称',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '描述',
  `inspection_template_id` bigint DEFAULT NULL COMMENT '关联的检查模板',
  `is_default` tinyint DEFAULT '0' COMMENT '是否默认',
  `status` tinyint DEFAULT '1' COMMENT '状态',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级模板表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rating_templates`
--

LOCK TABLES `rating_templates` WRITE;
/*!40000 ALTER TABLE `rating_templates` DISABLE KEYS */;
/*!40000 ALTER TABLE `rating_templates` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `relation_teach_ext`
--

DROP TABLE IF EXISTS `relation_teach_ext`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `relation_teach_ext` (
  `relation_id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `semester_id` bigint DEFAULT NULL,
  `teaching_hours` int DEFAULT NULL,
  PRIMARY KEY (`relation_id`),
  KEY `idx_course` (`course_id`),
  KEY `idx_semester` (`semester_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任课关系扩展(教育插件)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `relation_teach_ext`
--

LOCK TABLES `relation_teach_ext` WRITE;
/*!40000 ALTER TABLE `relation_teach_ext` DISABLE KEYS */;
/*!40000 ALTER TABLE `relation_teach_ext` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `relation_types`
--

DROP TABLE IF EXISTS `relation_types`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `relation_types` (
  `relation_code` varchar(50) NOT NULL,
  `from_type` varchar(30) NOT NULL,
  `to_type` varchar(30) NOT NULL,
  `relation_name` varchar(50) NOT NULL,
  `reverse_name` varchar(50) DEFAULT NULL COMMENT '反向读名 (resource→subject 视角, 如 belongs_to 正向"场所属于组织"/反向"组织下辖场所"); 供有向链 UI/反向走跳',
  `is_transitive` tinyint NOT NULL DEFAULT '0',
  `category` varchar(20) NOT NULL COMMENT 'OWNERSHIP/MEMBERSHIP/ASSOCIATION/DELEGATION/SUBSCRIPTION',
  `allowed_from_type_codes` json DEFAULT NULL,
  `allowed_to_type_codes` json DEFAULT NULL,
  `extension_table` varchar(100) DEFAULT NULL,
  `tier` varchar(20) NOT NULL DEFAULT 'CORE' COMMENT 'CORE/COMMON_EXT/DOMAIN',
  `registered_by` varchar(100) NOT NULL DEFAULT 'CORE',
  `description` varchar(200) DEFAULT NULL,
  `is_enabled` tinyint NOT NULL DEFAULT '1',
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `implied_relations` json DEFAULT NULL COMMENT '关系链推导: List<Implied{targetType,relation,discoveryRule}>',
  `metadata_schema` json DEFAULT NULL COMMENT '关系记录 metadata 的 JSON Schema (可选, Phase 5 启用)',
  `approval_required` tinyint NOT NULL DEFAULT '0',
  `plugin_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '插件级启用状态',
  `max_per_resource` int DEFAULT NULL COMMENT '每资源上限(如 admin=1)',
  `capacity_bound` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否受场所容量约束',
  `max_by_subtype` json DEFAULT NULL COMMENT '按子类型的每资源上限覆盖(如 teaches CLASS:10)',
  `max_per_subject` int DEFAULT NULL COMMENT '每个 subject 最多持有该 relation 的 resource 数 (null=无限, 如 member=1 每用户唯一归属)',
  `industry` varchar(20) DEFAULT NULL COMMENT '所属行业包',
  `plugin_class` varchar(200) DEFAULT NULL COMMENT '声明插件全限定类名',
  `origin` varchar(128) DEFAULT NULL COMMENT '统一来源',
  PRIMARY KEY (`relation_code`,`from_type`,`to_type`,`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='关系类型字典';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `relation_types`
--

LOCK TABLES `relation_types` WRITE;
/*!40000 ALTER TABLE `relation_types` DISABLE KEYS */;
INSERT INTO `relation_types` (`relation_code`,`from_type`,`to_type`,`relation_name`,`is_transitive`,`category`,`allowed_from_type_codes`,`allowed_to_type_codes`,`extension_table`,`tier`,`registered_by`,`description`,`is_enabled`,`tenant_id`,`created_at`,`implied_relations`,`metadata_schema`,`approval_required`,`plugin_enabled`,`max_per_resource`,`capacity_bound`,`max_by_subtype`,`max_per_subject`,`industry`,`plugin_class`,`origin`) VALUES('admin','user','org_unit','主管理员',1,'OWNERSHIP','[\"TEACHER\", \"COUNSELOR\", \"STAFF\", \"ADMIN\", \"SUPER_ADMIN\"]',NULL,NULL,'CORE','CORE','组织的主负责人(如班主任/部门主管),沿子组织传递',1,1,'2026-06-01 22:57:30','[{\"relation\": \"viewer\", \"targetType\": \"user\", \"discoveryRule\": \"MEMBERS_OF_ORG\"}, {\"relation\": \"admin\", \"targetType\": \"org_unit\", \"discoveryRule\": \"DESCENDANTS_OF_ORG\"}]',NULL,0,1,1,0,NULL,NULL,'CORE','PluginPackage:CORE','PLUGIN:CORE@1.0.0'),('admin','user','place','场所负责人',0,'OWNERSHIP','[\"TEACHER\", \"STAFF\", \"ADMIN\", \"SUPER_ADMIN\"]',NULL,NULL,'CORE','CORE','场所的主负责人',1,1,'2026-06-01 22:57:30',NULL,NULL,0,1,1,0,NULL,NULL,'CORE','PluginPackage:CORE','PLUGIN:CORE@1.0.0'),('advisor_of','user','org_unit','辅导员',1,'OWNERSHIP','[\"COUNSELOR\"]','[\"GRADE\", \"CLASS\"]',NULL,'DOMAIN','EducationPlugin','辅导员负责年级或班级 [DEPRECATED → admin + metadata.role=ADVISOR]',0,1,'2026-06-01 22:57:30',NULL,NULL,0,1,NULL,0,NULL,NULL,NULL,NULL,NULL),('belongs_to','place','org_unit','归属',0,'ASSOCIATION',NULL,NULL,NULL,'CORE','CORE','场所归属某组织',1,1,'2026-06-01 22:57:30',NULL,NULL,0,1,NULL,0,NULL,1,'CORE','PluginPackage:CORE','PLUGIN:CORE@1.0.0'),('delegated_to','user','user','委托',0,'DELEGATION',NULL,NULL,'access_relations_delegation_ext','CORE','CORE','权限临时委托给另一用户',1,1,'2026-06-01 22:57:30',NULL,NULL,0,1,NULL,0,NULL,NULL,'CORE','PluginPackage:CORE','PLUGIN:CORE@1.0.0'),('deputy','user','org_unit','副管理员',1,'OWNERSHIP','[\"TEACHER\", \"STAFF\", \"ADMIN\"]',NULL,NULL,'CORE','CORE','组织副负责人',1,1,'2026-06-01 22:57:30',NULL,NULL,0,1,NULL,0,NULL,NULL,'CORE','PluginPackage:CORE','PLUGIN:CORE@1.0.0'),('emergency_contact','user','user','紧急联系人',0,'ASSOCIATION',NULL,NULL,NULL,'COMMON_EXT','CommonExtPlugin','通用紧急联系人 — 入院/事故/失联场景',1,1,'2026-06-01 23:32:04',NULL,NULL,0,1,NULL,0,NULL,NULL,NULL,'PluginPackage:CommonExtPlugin',NULL),('family_of','user','user','亲属',0,'ASSOCIATION',NULL,NULL,NULL,'COMMON_EXT','CommonExtPlugin','家属/家长 — 跨学校(家长↔学生)/医院(家属↔病人)/养老 通用. 消息扇出走 BY_RELATION(family_of, inward) 查 resource user 的家属',1,1,'2026-06-01 23:32:04',NULL,NULL,0,1,NULL,0,NULL,NULL,NULL,'PluginPackage:CommonExtPlugin',NULL),('guardian_of','user','user','监护',0,'ASSOCIATION','[\"PARENT\", \"STAFF\"]','[\"STUDENT\"]',NULL,'COMMON_EXT','CommonExtPlugin','家长监护学生/护工监护病人',1,1,'2026-06-01 22:57:30',NULL,NULL,0,1,NULL,0,NULL,NULL,NULL,NULL,NULL),('manages','user','place','场所管理者',0,'OWNERSHIP',NULL,NULL,NULL,'CORE','CORE','非主责管理者 (保洁/物业等)',1,1,'2026-06-01 22:57:30','[{\"relation\": \"viewer\", \"targetType\": \"user\", \"discoveryRule\": \"OCCUPANTS_OF_PLACE\"}]',NULL,0,1,NULL,0,NULL,NULL,'CORE','PluginPackage:CORE','PLUGIN:CORE@1.0.0'),('member','user','org_unit','成员',0,'MEMBERSHIP',NULL,NULL,NULL,'CORE','CORE','用户属于某组织',1,1,'2026-06-01 22:57:30',NULL,NULL,0,1,NULL,0,NULL,1,'CORE','PluginPackage:CORE','PLUGIN:CORE@1.0.0'),('mentor_of','user','user','导师',0,'ASSOCIATION',NULL,NULL,NULL,'DOMAIN','EducationPlugin','导师指导学生',1,1,'2026-06-01 22:57:30',NULL,NULL,0,1,NULL,0,NULL,NULL,'EDU','PluginPackage:EDU','PLUGIN:EDU@1.0.0'),('occupies','user','place','占用',0,'MEMBERSHIP',NULL,NULL,'access_relations_occupancy_ext','CORE','CORE','宿舍入住/工位使用 (含 check_in/check_out 时间)',1,1,'2026-06-01 22:57:30',NULL,NULL,0,1,NULL,1,NULL,NULL,'CORE','PluginPackage:CORE','PLUGIN:CORE@1.0.0'),('responsible_for','user','org_unit','责任人(对组织)',0,'OWNERSHIP',NULL,NULL,NULL,'CORE','CORE','通用责任 — 对某组织负责 (如部门主管,班主任)',1,1,'2026-06-01 23:32:04',NULL,NULL,0,1,NULL,0,NULL,NULL,'CORE','PluginPackage:CORE','PLUGIN:CORE@1.0.0'),('responsible_for','user','place','责任人(对场所)',0,'OWNERSHIP',NULL,NULL,NULL,'CORE','CORE','通用责任 — 对某场所负责 (如设备责任人,场地负责人)',1,1,'2026-06-01 23:32:05',NULL,NULL,0,1,1,0,NULL,NULL,'CORE','PluginPackage:CORE','PLUGIN:CORE@1.0.0'),('responsible_for','user','user','责任人(对人)',0,'OWNERSHIP',NULL,NULL,NULL,'CORE','CORE','通用责任 — 对某用户负责 (如导师对学生,医师对病人)',1,1,'2026-06-01 23:32:04',NULL,NULL,0,1,NULL,0,NULL,NULL,'CORE','PluginPackage:CORE','PLUGIN:CORE@1.0.0'),('supervisor_of','user','user','上级',0,'ASSOCIATION',NULL,NULL,NULL,'COMMON_EXT','CommonExtPlugin','人员上下级关系',1,1,'2026-06-01 22:57:30',NULL,NULL,0,1,NULL,0,NULL,NULL,NULL,NULL,NULL),('teaches','user','org_unit','任课',0,'ASSOCIATION','[\"TEACHER\"]','[\"CLASS\"]','relation_teach_ext','DOMAIN','EducationPlugin','教师任教班级,绑定课程和学期',1,1,'2026-06-01 22:57:30',NULL,NULL,0,1,NULL,0,'{\"CLASS\": 10}',NULL,'EDU','PluginPackage:EDU','PLUGIN:EDU@1.0.0'),('viewer','user','org_unit','查阅者(组织)',0,'ASSOCIATION',NULL,NULL,NULL,'CORE','CORE','通用只读访问 — grant 给某用户对某组织的查阅权',1,1,'2026-06-01 23:32:04',NULL,NULL,0,1,NULL,0,NULL,NULL,'CORE','PluginPackage:CORE','PLUGIN:CORE@1.0.0'),('viewer','user','place','查阅者(场所)',0,'ASSOCIATION',NULL,NULL,NULL,'CORE','CORE','通用只读访问 — grant 给某用户对某场所的查阅权',1,1,'2026-06-01 23:32:04',NULL,NULL,0,1,NULL,0,NULL,NULL,'CORE','PluginPackage:CORE','PLUGIN:CORE@1.0.0'),('viewer','user','user','查阅者(用户)',0,'ASSOCIATION',NULL,NULL,NULL,'CORE','CORE','通用只读访问 — 直接 grant 给某用户对某用户档案的查阅权',1,1,'2026-06-01 23:32:04',NULL,NULL,0,1,NULL,0,NULL,NULL,'CORE','PluginPackage:CORE','PLUGIN:CORE@1.0.0'),('watches','user','org_unit','关注',0,'SUBSCRIPTION',NULL,NULL,NULL,'CORE','CORE','用户订阅某组织的动态',1,1,'2026-06-01 22:57:30',NULL,NULL,0,1,NULL,0,NULL,NULL,'CORE','PluginPackage:CORE','PLUGIN:CORE@1.0.0');
/*!40000 ALTER TABLE `relation_types` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role_data_scopes`
--

DROP TABLE IF EXISTS `role_data_scopes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role_data_scopes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_id` bigint NOT NULL,
  `resource_code` varchar(50) NOT NULL,
  `apply_to` enum('READ','WRITE','BOTH') NOT NULL DEFAULT 'BOTH' COMMENT '读写分离: READ 只读范围 / WRITE 可写范围 / BOTH 两者',
  `type_filter` json DEFAULT NULL COMMENT 'axis③ 类型过滤: 类型码数组, 与组织范围 AND 组合; NULL=不限',
  `subject_rel_include` json DEFAULT NULL COMMENT 'axis② 主体关系白名单(关系码集)',
  `subject_rel_exclude` json DEFAULT NULL COMMENT 'axis② 主体关系黑名单(关系码集)',
  `relation_grants` json DEFAULT NULL COMMENT 'R3 关系授予 [{relation,subject,subjectParam?,subtree?,orgIds?}]; NULL=引擎轴① bridge 派生',
  `priority` int DEFAULT '0',
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_res` (`role_id`,`resource_code`,`apply_to`,`tenant_id`),
  KEY `idx_role` (`role_id`,`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=106 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色数据权限(单轨,替代多代并存)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role_data_scopes`
--

LOCK TABLES `role_data_scopes` WRITE;
/*!40000 ALTER TABLE `role_data_scopes` DISABLE KEYS */;
INSERT INTO `role_data_scopes` (`id`, `role_id`, `resource_code`, `apply_to`, `type_filter`, `subject_rel_include`, `subject_rel_exclude`, `priority`, `tenant_id`, `created_at`, `updated_at`, `deleted`, `relation_grants`) VALUES (1,2022900002094850049,'schedule_entry','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 22:57:41','2026-06-20 00:44:04',0,'[{\"subject\": \"SELF\", \"relation\": \"creator\"}]'),(2,2022900002094850049,'class_course_assignment','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 22:57:41','2026-06-20 00:44:04',0,'[{\"subject\": \"SELF\", \"relation\": \"creator\"}]'),(3,2022900002094850049,'semester_offering','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 22:57:41','2026-06-20 00:44:04',0,'[{\"subject\": \"SELF\", \"relation\": \"creator\"}]'),(4,2022900002094850049,'scheduling_constraint','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 22:57:41','2026-06-20 00:44:04',0,'[{\"subject\": \"SELF\", \"relation\": \"creator\"}]'),(5,2022900002094850049,'schedule_conflict_record','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 22:57:41','2026-06-20 00:44:04',0,'[{\"subject\": \"SELF\", \"relation\": \"creator\"}]'),(6,2022900002094850053,'schedule_entry','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 22:57:41','2026-06-20 00:44:04',0,'[{\"subject\": \"MY_ORG\", \"subtree\": true, \"relation\": \"owner_org\"}]'),(7,2022900002094850053,'class_course_assignment','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 22:57:41','2026-06-20 00:44:04',0,'[{\"subject\": \"MY_ORG\", \"subtree\": true, \"relation\": \"owner_org\"}]'),(8,2022900002094850053,'semester_offering','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 22:57:41','2026-06-20 00:44:04',0,'[{\"subject\": \"MY_ORG\", \"subtree\": true, \"relation\": \"owner_org\"}]'),(9,2022900002094850053,'scheduling_constraint','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 22:57:41','2026-06-20 00:44:04',0,'[{\"subject\": \"MY_ORG\", \"subtree\": true, \"relation\": \"owner_org\"}]'),(10,2022900002094850053,'schedule_conflict_record','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 22:57:41','2026-06-20 00:44:04',0,'[{\"subject\": \"MY_ORG\", \"subtree\": true, \"relation\": \"owner_org\"}]'),(11,2022900002094850049,'teacher_preference','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 22:57:41','2026-06-20 00:44:04',0,'[{\"subject\": \"SELF\", \"relation\": \"creator\"}]'),(12,2022900002094850053,'teacher_preference','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 22:57:41','2026-06-20 00:44:04',0,'[{\"subject\": \"ALL\", \"relation\": \"owner_org\"}]'),(13,2022900002094850049,'teaching_progress','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 22:57:42','2026-06-20 00:44:04',0,'[{\"subject\": \"SELF\", \"relation\": \"creator\"}]'),(14,2022900002094850053,'teaching_progress','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 22:57:42','2026-06-20 00:44:04',0,'[{\"subject\": \"MY_ORG\", \"subtree\": true, \"relation\": \"owner_org\"}]'),(15,2022900002094850049,'course_evaluation','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 22:57:42','2026-06-20 00:44:04',0,'[{\"subject\": \"MY_ORG\", \"subtree\": false, \"relation\": \"owner_org\"}]'),(16,2022900002094850049,'evaluation_response','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 22:57:42','2026-06-20 00:44:04',0,'[{\"subject\": \"SELF\", \"relation\": \"creator\"}]'),(17,2022900002094850053,'course_evaluation','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 22:57:42','2026-06-20 00:44:04',0,'[{\"subject\": \"MY_ORG\", \"subtree\": true, \"relation\": \"owner_org\"}]'),(18,2022900002094850053,'evaluation_response','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 22:57:42','2026-06-20 00:44:04',0,'[{\"subject\": \"MY_ORG\", \"subtree\": true, \"relation\": \"owner_org\"}]'),(37,2022900002094850052,'student','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 23:36:06','2026-06-20 00:44:04',0,'[{\"subject\": \"ALL\", \"relation\": \"owner_org\"}]'),(38,2022900002094850052,'school_class','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 23:36:06','2026-06-20 00:44:04',0,'[{\"subject\": \"ALL\", \"relation\": \"owner_org\"}]'),(39,2022900002094850052,'dashboard','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 23:36:06','2026-06-20 00:44:04',0,'[{\"subject\": \"ALL\", \"relation\": \"owner_org\"}]'),(40,2022900002094850052,'teaching_task','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 23:36:06','2026-06-20 00:44:04',0,'[{\"subject\": \"ALL\", \"relation\": \"owner_org\"}]'),(41,2022900002094850053,'teaching_task','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 23:36:06','2026-06-20 00:44:04',0,'[{\"subject\": \"ALL\", \"relation\": \"owner_org\"}]'),(42,2022900002094850053,'student','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 23:36:06','2026-06-20 00:44:04',0,'[{\"subject\": \"MY_ORG\", \"subtree\": true, \"relation\": \"owner_org\"}]'),(43,2022900002094850053,'school_class','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 23:36:06','2026-06-20 00:44:04',0,'[{\"subject\": \"MY_ORG\", \"subtree\": true, \"relation\": \"owner_org\"}]'),(44,2022900002094850060,'student','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 23:36:06','2026-06-20 00:44:04',0,'[{\"subject\": \"MY_ORG\", \"subtree\": true, \"relation\": \"owner_org\"}]'),(45,2022900002094850060,'school_class','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 23:36:06','2026-06-20 00:44:04',0,'[{\"subject\": \"MY_ORG\", \"subtree\": true, \"relation\": \"owner_org\"}]'),(46,2022900002094850060,'dashboard','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 23:36:06','2026-06-20 00:44:04',0,'[{\"subject\": \"MY_ORG\", \"subtree\": true, \"relation\": \"owner_org\"}]'),(47,2022900002094850054,'student','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 23:36:06','2026-06-20 00:44:04',0,'[{\"subject\": \"PLUGIN_DIM\", \"relation\": \"owner_org\", \"subjectParam\": \"BY_GRADE\"}]'),(48,2022900002094850054,'school_class','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 23:36:06','2026-06-20 00:44:04',0,'[{\"subject\": \"PLUGIN_DIM\", \"relation\": \"owner_org\", \"subjectParam\": \"BY_GRADE\"}]'),(49,2022900002094850055,'student','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 23:36:06','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"PLUGIN_DIM\", \"subtree\": false, \"relation\": \"owner_org\", \"subjectParam\": \"BY_CLASS\"}]'),(50,2022900002094850055,'attendance','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 23:36:06','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"PLUGIN_DIM\", \"subtree\": false, \"relation\": \"owner_org\", \"subjectParam\": \"BY_CLASS\"}]'),(51,2022900002094850055,'student_grade','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 23:36:06','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"PLUGIN_DIM\", \"subtree\": false, \"relation\": \"owner_org\", \"subjectParam\": \"BY_CLASS\"}]'),(52,2022900002094850056,'student','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 23:36:06','2026-06-20 00:44:04',0,'[{\"subject\": \"PLUGIN_DIM\", \"relation\": \"owner_org\", \"subjectParam\": \"BY_CLASS\"}]'),(53,2022900002094850056,'teaching_task','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 23:36:06','2026-06-20 00:44:04',0,'[{\"subject\": \"ALL\", \"relation\": \"owner_org\"}]'),(54,2022900002094850057,'student','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 23:36:06','2026-06-20 00:44:04',0,'[{\"subject\": \"MY_ORG\", \"subtree\": true, \"relation\": \"owner_org\"}]'),(55,2022900002094850061,'student','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 23:36:06','2026-06-20 00:44:04',0,'[{\"subject\": \"SELF\", \"relation\": \"owner_org\"}]'),(56,2022900002094850061,'attendance','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 23:36:06','2026-06-20 00:44:04',0,'[{\"subject\": \"SELF\", \"relation\": \"creator\"}]'),(57,2022900002094850061,'student_grade','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 23:36:06','2026-06-20 00:44:04',0,'[{\"subject\": \"SELF\", \"relation\": \"creator\"}]'),(58,2022900002094850062,'student','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 23:36:06','2026-06-20 00:44:04',0,'[{\"subject\": \"SELF\", \"relation\": \"owner_org\"}]'),(59,2022900002094850062,'attendance','BOTH',NULL,NULL,NULL,0,1,'2026-06-01 23:36:06','2026-06-20 00:44:04',0,'[{\"subject\": \"SELF\", \"relation\": \"creator\"}]'),(497,2022900002094850055,'inspection_audit','BOTH',NULL,NULL,NULL,0,1,'2026-06-05 00:57:33','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]'),(498,2022900002094850055,'user','BOTH',NULL,NULL,NULL,0,1,'2026-06-05 00:57:34','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"owner_org\", \"subjectParam\": null}]'),(499,2022900002094850055,'org_unit','BOTH',NULL,NULL,NULL,0,1,'2026-06-05 00:57:34','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]'),(500,2022900002094850055,'place','BOTH',NULL,NULL,NULL,0,1,'2026-06-05 00:57:34','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]'),(501,2022900002094850055,'role','BOTH',NULL,NULL,NULL,0,1,'2026-06-05 00:57:34','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]'),(502,2022900002094850055,'notification','BOTH',NULL,NULL,NULL,0,1,'2026-06-05 00:57:34','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]'),(503,2022900002094850055,'inspection','BOTH',NULL,NULL,NULL,0,1,'2026-06-05 00:57:34','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]'),(504,2022900002094850055,'inspection_appeal','BOTH',NULL,NULL,NULL,0,1,'2026-06-05 00:57:34','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]'),(505,2022900002094850055,'inspection_corrective','BOTH',NULL,NULL,NULL,0,1,'2026-06-05 00:57:34','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]'),(506,2022900002094850055,'inspection_personal','BOTH',NULL,NULL,NULL,0,1,'2026-06-05 00:57:34','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]'),(507,2022900002094850055,'inspection_project','BOTH',NULL,NULL,NULL,0,1,'2026-06-05 00:57:34','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]'),(508,2022900002094850055,'inspection_record','BOTH',NULL,NULL,NULL,0,1,'2026-06-05 00:57:34','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]'),(509,2022900002094850055,'inspection_summary','BOTH',NULL,NULL,NULL,0,1,'2026-06-05 00:57:34','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"ALL\", \"subtree\": false, \"relation\": \"owner_org\", \"subjectParam\": null}]'),(510,2022900002094850055,'inspection_task','BOTH',NULL,NULL,NULL,0,1,'2026-06-05 00:57:34','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]'),(511,2022900002094850055,'inspection_template','BOTH',NULL,NULL,NULL,0,1,'2026-06-05 00:57:34','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]'),(512,2022900002094850055,'inspection_alert','BOTH',NULL,NULL,NULL,0,1,'2026-06-05 00:57:34','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]'),(513,2022900002094850055,'dashboard','BOTH',NULL,NULL,NULL,0,1,'2026-06-05 00:57:34','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]'),(514,2022900002094850055,'inspection_violation','BOTH',NULL,NULL,NULL,0,1,'2026-06-05 00:57:34','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]'),(515,2022900002094850055,'inspection_observation','BOTH',NULL,NULL,NULL,0,1,'2026-06-05 00:57:34','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]'),(516,2022900002094850055,'system_role','BOTH',NULL,NULL,NULL,0,1,'2026-06-05 00:57:34','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]'),(517,2022900002094850055,'system_user','BOTH',NULL,NULL,NULL,0,1,'2026-06-05 00:57:34','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]'),(519,2022900002094850055,'school_class','BOTH',NULL,NULL,NULL,0,1,'2026-06-05 00:57:34','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]'),(520,2022900002094850055,'grade_batch','BOTH',NULL,NULL,NULL,0,1,'2026-06-05 00:57:34','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"PLUGIN_DIM\", \"subtree\": false, \"relation\": \"owner_org\", \"subjectParam\": \"BY_CLASS\"}]'),(521,2022900002094850055,'exam','BOTH',NULL,NULL,NULL,0,1,'2026-06-05 00:57:34','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"PLUGIN_DIM\", \"subtree\": false, \"relation\": \"owner_org\", \"subjectParam\": \"BY_CLASS\"}]'),(523,2022900002094850055,'teaching_task','BOTH',NULL,NULL,NULL,0,1,'2026-06-05 00:57:34','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]'),(525,2022900002094850055,'exam_batch','BOTH',NULL,NULL,NULL,0,1,'2026-06-05 00:57:34','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]'),(526,2022900002094850055,'dormitory_building','BOTH',NULL,NULL,NULL,0,1,'2026-06-05 00:57:34','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]'),(527,2022900002094850055,'dormitory_checkin','BOTH',NULL,NULL,NULL,0,1,'2026-06-05 00:57:34','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]'),(528,2022900002094850055,'dormitory_room','BOTH',NULL,NULL,NULL,0,1,'2026-06-05 00:57:34','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]'),(9000000000000006105,2022900002094850055,'teacher_preference','BOTH',NULL,NULL,NULL,0,1,'2026-06-19 11:50:11','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]'),(9000000000000006106,2022900002094850056,'teacher_preference','BOTH',NULL,NULL,NULL,0,1,'2026-06-19 11:50:11','2026-06-20 00:44:04',0,'[{\"subject\": \"SELF\", \"relation\": \"creator\"}]'),(9000000000000006107,2022900002094850060,'teacher_preference','BOTH',NULL,NULL,NULL,0,1,'2026-06-19 11:50:11','2026-06-20 00:44:04',0,'[{\"subject\": \"SELF\", \"relation\": \"creator\"}]'),(9000000000000006109,2022900002094850052,'teacher_preference','BOTH',NULL,NULL,NULL,0,1,'2026-06-19 11:50:11','2026-06-20 00:44:04',0,'[{\"subject\": \"ALL\", \"relation\": \"owner_org\"}]'),(9000000000000006400,2022900002094850055,'entity_event','BOTH',NULL,NULL,NULL,0,1,'2026-06-20 01:19:02','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]'),(9000000000000006404,2022900002094850055,'teaching_progress','BOTH',NULL,NULL,NULL,0,1,'2026-06-20 01:19:02','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]'),(9000000000000006405,2022900002094850055,'class_course_assignment','BOTH',NULL,NULL,NULL,0,1,'2026-06-20 01:19:02','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]'),(9000000000000006406,2022900002094850055,'course_evaluation','BOTH',NULL,NULL,NULL,0,1,'2026-06-20 01:19:02','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]'),(9000000000000006407,2022900002094850055,'evaluation_response','BOTH',NULL,NULL,NULL,0,1,'2026-06-20 01:19:02','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]'),(9000000000000006408,2022900002094850055,'scheduling_constraint','BOTH',NULL,NULL,NULL,0,1,'2026-06-20 01:19:02','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]'),(9000000000000006409,2022900002094850055,'schedule_entry','BOTH',NULL,NULL,NULL,0,1,'2026-06-20 01:19:02','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]'),(9000000000000006410,2022900002094850055,'schedule_conflict_record','BOTH',NULL,NULL,NULL,0,1,'2026-06-20 01:19:02','2026-06-20 01:26:38',0,'[{\"orgIds\": null, \"subject\": \"SELF\", \"subtree\": false, \"relation\": \"creator\", \"subjectParam\": null}]');
/*!40000 ALTER TABLE `role_data_scopes` ENABLE KEYS */;
UNLOCK TABLES;

-- R3a-2b: seed 行轴① 用 relation_grants (M1 轴①列 org_anchor/anchor_param/include_subtree/custom_org_ids 已删); 轴②③ = subject_rel_*/type_filter 列。

--
-- Table structure for table `role_permissions`
--

DROP TABLE IF EXISTS `role_permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role_permissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `permission_id` bigint NOT NULL COMMENT '权限ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `tenant_id` bigint NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`,`permission_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_permission_id` (`permission_id`),
  KEY `idx_role_permissions_tenant` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=908334 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role_permissions`
--

LOCK TABLES `role_permissions` WRITE;
/*!40000 ALTER TABLE `role_permissions` DISABLE KEYS */;
INSERT INTO `role_permissions` VALUES (1,1,0,'2026-06-01 22:56:53',1),(908101,1,8101,'2026-06-01 22:56:35',1),(908102,1,8102,'2026-06-01 22:56:35',1),(908103,1,8103,'2026-06-01 22:56:35',1),(908111,1,8111,'2026-06-01 22:56:35',1),(908112,1,8112,'2026-06-01 22:56:35',1),(908113,1,8113,'2026-06-01 22:56:35',1),(908121,1,8121,'2026-06-01 22:56:35',1),(908122,1,8122,'2026-06-01 22:56:35',1),(908123,1,8123,'2026-06-01 22:56:35',1),(908131,1,8131,'2026-06-01 22:56:35',1),(908132,1,8132,'2026-06-01 22:56:35',1),(908133,1,8133,'2026-06-01 22:56:35',1),(908141,1,8141,'2026-06-01 22:56:35',1),(908142,1,8142,'2026-06-01 22:56:35',1),(908143,1,8143,'2026-06-01 22:56:35',1),(908151,1,8151,'2026-06-01 22:56:35',1),(908152,1,8152,'2026-06-01 22:56:35',1),(908153,1,8153,'2026-06-01 22:56:35',1),(908161,1,8161,'2026-06-01 22:56:35',1),(908162,1,8162,'2026-06-01 22:56:35',1),(908163,1,8163,'2026-06-01 22:56:35',1),(908171,1,8171,'2026-06-01 22:56:35',1),(908172,1,8172,'2026-06-01 22:56:35',1),(908173,1,8173,'2026-06-01 22:56:35',1),(908181,1,8181,'2026-06-01 22:56:35',1),(908182,1,8182,'2026-06-01 22:56:35',1),(908183,1,8183,'2026-06-01 22:56:35',1),(908191,1,8191,'2026-06-01 22:56:35',1),(908192,1,8192,'2026-06-01 22:56:35',1),(908193,1,8193,'2026-06-01 22:56:35',1),(908201,1,8201,'2026-06-01 22:56:35',1),(908202,1,8202,'2026-06-01 22:56:35',1),(908203,1,8203,'2026-06-01 22:56:35',1),(908211,1,8211,'2026-06-01 22:56:35',1),(908212,1,8212,'2026-06-01 22:56:35',1),(908213,1,8213,'2026-06-01 22:56:35',1),(908301,1,8301,'2026-06-01 22:56:35',1),(908302,1,8302,'2026-06-01 22:56:35',1),(908303,1,8303,'2026-06-01 22:56:35',1),(908311,1,8311,'2026-06-01 22:56:35',1),(908312,1,8312,'2026-06-01 22:56:35',1),(908313,1,8313,'2026-06-01 22:56:35',1),(908321,1,8321,'2026-06-01 22:56:35',1),(908322,1,8322,'2026-06-01 22:56:35',1),(908323,1,8323,'2026-06-01 22:56:35',1),(908331,1,8331,'2026-06-01 22:56:35',1),(908332,1,8332,'2026-06-01 22:56:35',1),(908333,1,8333,'2026-06-01 22:56:35',1);
/*!40000 ALTER TABLE `role_permissions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色编码',
  `role_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '角色描述',
  `status` tinyint DEFAULT '1' COMMENT '状态:0禁用,1启用',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `role_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'CUSTOM',
  `plugin_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '插件级启用状态',
  `role_desc` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `industry` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `plugin_class` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `origin` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_roles_tenant` (`tenant_id`),
  KEY `idx_roles_active_plugin` (`deleted`,`plugin_enabled`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2022900002094850063 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `roles`
--

LOCK TABLES `roles` WRITE;
/*!40000 ALTER TABLE `roles` DISABLE KEYS */;
INSERT INTO `roles` VALUES (1,'SUPER_ADMIN','超级管理员',NULL,1,1,'2026-02-02 23:59:40',1,'2026-06-01 23:34:29',NULL,0,1,'PRESET',1,'系统超级管理员','CORE','com.school.management.infrastructure.extension.plugins.core.CoreRolePresetPlugin','PLUGIN:CORE@1.0.0'),(2022900002094850049,'TEACHER','教师',NULL,1,100,'2026-02-15 13:05:19',NULL,'2026-06-01 23:34:29',NULL,0,1,'PRESET',1,'教师统一权限角色;职务分类见 teacher_assignments.role_type','EDU','com.school.management.infrastructure.extension.plugins.education.EducationRolePresetPlugin','PLUGIN:EDU@1.0.0'),(2022900002094850050,'TENANT_ADMIN','租户管理员',NULL,1,0,'2026-06-01 23:34:29',NULL,'2026-06-01 23:34:29',NULL,0,1,'PRESET',1,'单租户下的管理员,管理本租户所有业务','CORE','com.school.management.infrastructure.extension.plugins.core.CoreRolePresetPlugin','PLUGIN:CORE@1.0.0'),(2022900002094850051,'GUEST','访客',NULL,1,0,'2026-06-01 23:34:29',NULL,'2026-06-01 23:34:29',NULL,0,1,'PRESET',1,'只读角色,不能创建/修改任何数据','CORE','com.school.management.infrastructure.extension.plugins.core.CoreRolePresetPlugin','PLUGIN:CORE@1.0.0'),(2022900002094850052,'SCHOOL_ADMIN','学校管理员',NULL,1,0,'2026-06-01 23:34:29',NULL,'2026-06-01 23:34:29',NULL,0,1,'PRESET',1,'管理学校所有业务数据,部门/教师/学生/场所等','EDU','com.school.management.infrastructure.extension.plugins.education.EducationRolePresetPlugin','PLUGIN:EDU@1.0.0'),(2022900002094850053,'ACADEMIC_DIRECTOR','教务主任',NULL,1,0,'2026-06-01 23:34:29',NULL,'2026-06-01 23:34:29',NULL,0,1,'PRESET',1,'教务条线最高负责,审核成绩/考试/课表发布','EDU','com.school.management.infrastructure.extension.plugins.education.EducationRolePresetPlugin','PLUGIN:EDU@1.0.0'),(2022900002094850054,'GRADE_DIRECTOR','年级主任',NULL,1,0,'2026-06-01 23:34:29',NULL,'2026-06-01 23:34:29',NULL,0,1,'PRESET',1,'负责某年级下所有班级,跨班管理','EDU','com.school.management.infrastructure.extension.plugins.education.EducationRolePresetPlugin','PLUGIN:EDU@1.0.0'),(2022900002094850055,'CLASS_TEACHER','班主任',NULL,1,0,'2026-06-01 23:34:29',NULL,'2026-06-01 23:34:29',NULL,0,1,'PRESET',1,'负责班级的全方位管理,拥有本班学生完整数据','EDU','com.school.management.infrastructure.extension.plugins.education.EducationRolePresetPlugin','PLUGIN:EDU@1.0.0'),(2022900002094850056,'SUBJECT_TEACHER','任课教师',NULL,1,0,'2026-06-01 23:34:29',NULL,'2026-06-01 23:34:29',NULL,0,1,'PRESET',1,'负责某课程的教学,管理课程下的成绩/考勤','EDU','com.school.management.infrastructure.extension.plugins.education.EducationRolePresetPlugin','PLUGIN:EDU@1.0.0'),(2022900002094850057,'COUNSELOR','辅导员',NULL,1,0,'2026-06-01 23:34:29',NULL,'2026-06-01 23:34:29',NULL,0,1,'PRESET',1,'跨班级辅导,心理/思政等,按分配班级范围工作','EDU','com.school.management.infrastructure.extension.plugins.education.EducationRolePresetPlugin','PLUGIN:EDU@1.0.0'),(2022900002094850058,'DORMITORY_MANAGER','宿管员',NULL,1,0,'2026-06-01 23:34:29',NULL,'2026-06-01 23:34:29',NULL,0,1,'PRESET',1,'管理宿舍入住/退出/调换/卫生检查','EDU','com.school.management.infrastructure.extension.plugins.education.EducationRolePresetPlugin','PLUGIN:EDU@1.0.0'),(2022900002094850059,'INSPECTOR','检查员',NULL,1,0,'2026-06-01 23:34:29',NULL,'2026-06-01 23:34:29',NULL,0,1,'PRESET',1,'执行各类检查任务(卫生/安全/纪律)','EDU','com.school.management.infrastructure.extension.plugins.education.EducationRolePresetPlugin','PLUGIN:EDU@1.0.0'),(2022900002094850060,'DEPT_ADMIN','系部管理员',NULL,1,0,'2026-06-01 23:34:29',NULL,'2026-06-01 23:34:29',NULL,0,1,'PRESET',1,'系/院级管理员角色,可查看本组织及下级数据','EDU','com.school.management.infrastructure.extension.plugins.education.EducationRolePresetPlugin','PLUGIN:EDU@1.0.0'),(2022900002094850061,'STUDENT','学生',NULL,1,0,'2026-06-01 23:34:29',NULL,'2026-06-01 23:34:29',NULL,0,1,'PRESET',1,'学生基本角色,只读自己相关数据','EDU','com.school.management.infrastructure.extension.plugins.education.EducationRolePresetPlugin','PLUGIN:EDU@1.0.0'),(2022900002094850062,'PARENT','家长',NULL,1,0,'2026-06-01 23:34:29',NULL,'2026-06-01 23:34:29',NULL,0,1,'PRESET',1,'接收子女相关通知,只读权限','EDU','com.school.management.infrastructure.extension.plugins.education.EducationRolePresetPlugin','PLUGIN:EDU@1.0.0');
/*!40000 ALTER TABLE `roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `schedule_adjustments`
--

DROP TABLE IF EXISTS `schedule_adjustments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schedule_adjustments` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `adjustment_code` varchar(50) NOT NULL COMMENT '调课单号',
  `semester_id` bigint NOT NULL COMMENT '学期ID',
  `original_entry_id` bigint NOT NULL COMMENT '原排课条目ID',
  `adjustment_type` tinyint NOT NULL COMMENT '调整类型：1调课（换时间）2换教室 3代课（换教师）4停课',
  `original_date` date NOT NULL COMMENT '原上课日期',
  `original_weekday` tinyint DEFAULT NULL COMMENT '原周几',
  `original_slot` int DEFAULT NULL COMMENT '原节次',
  `original_classroom_id` bigint DEFAULT NULL COMMENT '原教室ID',
  `original_teacher_id` bigint DEFAULT NULL COMMENT '原教师ID',
  `new_date` date DEFAULT NULL COMMENT '新上课日期（调课时填）',
  `new_weekday` tinyint DEFAULT NULL COMMENT '新周几',
  `new_slot` int DEFAULT NULL COMMENT '新节次',
  `new_classroom_id` bigint DEFAULT NULL COMMENT '新教室ID',
  `new_teacher_id` bigint DEFAULT NULL COMMENT '代课教师ID',
  `applicant_id` bigint NOT NULL COMMENT '申请人ID',
  `apply_reason` varchar(500) NOT NULL COMMENT '申请原因',
  `apply_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `attachment_urls` json DEFAULT NULL COMMENT '附件（如请假条）',
  `approval_status` tinyint DEFAULT '0' COMMENT '审批状态：0待审批 1已通过 2已拒绝 3已撤回',
  `approver_id` bigint DEFAULT NULL COMMENT '审批人ID',
  `approval_time` datetime DEFAULT NULL COMMENT '审批时间',
  `approval_comment` varchar(500) DEFAULT NULL COMMENT '审批意见',
  `executed` tinyint DEFAULT '0' COMMENT '是否已执行',
  `executed_at` datetime DEFAULT NULL COMMENT '执行时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_adjustment_code` (`adjustment_code`),
  KEY `idx_semester` (`semester_id`),
  KEY `idx_original_entry` (`original_entry_id`),
  KEY `idx_applicant` (`applicant_id`),
  KEY `idx_status` (`approval_status`),
  KEY `idx_tenant` (`tenant_id`),
  CONSTRAINT `fk_adjustment_entry` FOREIGN KEY (`original_entry_id`) REFERENCES `schedule_entries` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='调课申请表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `schedule_adjustments`
--

LOCK TABLES `schedule_adjustments` WRITE;
/*!40000 ALTER TABLE `schedule_adjustments` DISABLE KEYS */;
/*!40000 ALTER TABLE `schedule_adjustments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `schedule_conflict_records`
--

DROP TABLE IF EXISTS `schedule_conflict_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schedule_conflict_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `semester_id` bigint NOT NULL COMMENT '学期ID',
  `detection_batch` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '检测批次号',
  `conflict_category` tinyint NOT NULL COMMENT '1资源冲突 2约束冲突 3软冲突',
  `conflict_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '冲突类型',
  `severity` tinyint NOT NULL COMMENT '1阻塞 2警告 3提示',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '冲突描述',
  `detail` json DEFAULT NULL COMMENT '冲突详情',
  `entry_id_1` bigint DEFAULT NULL COMMENT '排课条目1',
  `entry_id_2` bigint DEFAULT NULL COMMENT '排课条目2',
  `org_unit_id` bigint DEFAULT NULL COMMENT '冲突归属组织单元ID (从 entry_id_1 派生)',
  `created_by` bigint DEFAULT NULL COMMENT '检测创建人ID',
  `constraint_id` bigint DEFAULT NULL COMMENT '关联约束ID',
  `resolution_status` tinyint NOT NULL DEFAULT '0' COMMENT '0未处理 1已解决 2已忽略',
  `resolution_note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '处理说明',
  `resolved_by` bigint DEFAULT NULL COMMENT '处理人',
  `resolved_at` datetime DEFAULT NULL COMMENT '处理时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_semester` (`semester_id`),
  KEY `idx_batch` (`detection_batch`),
  KEY `idx_status` (`resolution_status`),
  KEY `idx_tenant` (`tenant_id`),
  KEY `idx_org_unit` (`org_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排课冲突记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `schedule_conflict_records`
--

LOCK TABLES `schedule_conflict_records` WRITE;
/*!40000 ALTER TABLE `schedule_conflict_records` DISABLE KEYS */;
/*!40000 ALTER TABLE `schedule_conflict_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `schedule_conflicts`
--

DROP TABLE IF EXISTS `schedule_conflicts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schedule_conflicts` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `semester_id` bigint NOT NULL COMMENT '学期ID',
  `entry_id` bigint NOT NULL COMMENT '排课条目ID',
  `conflict_entry_id` bigint DEFAULT NULL COMMENT '冲突的另一条目ID',
  `conflict_type` tinyint NOT NULL COMMENT '冲突类型：1教师冲突 2教室冲突 3班级冲突',
  `conflict_detail` varchar(500) DEFAULT NULL COMMENT '冲突详情描述',
  `resolved` tinyint DEFAULT '0' COMMENT '是否已解决',
  `resolved_by` bigint DEFAULT NULL COMMENT '解决人',
  `resolved_at` datetime DEFAULT NULL COMMENT '解决时间',
  `resolution_note` varchar(500) DEFAULT NULL COMMENT '解决说明',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_entry` (`entry_id`),
  KEY `idx_semester` (`semester_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='排课冲突记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `schedule_conflicts`
--

LOCK TABLES `schedule_conflicts` WRITE;
/*!40000 ALTER TABLE `schedule_conflicts` DISABLE KEYS */;
/*!40000 ALTER TABLE `schedule_conflicts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `schedule_entries`
--

DROP TABLE IF EXISTS `schedule_entries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schedule_entries` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `semester_id` bigint NOT NULL COMMENT '学期ID',
  `schedule_id` bigint DEFAULT NULL COMMENT '关联排课方案',
  `task_id` bigint NOT NULL COMMENT '教学任务ID',
  `teaching_class_id` bigint DEFAULT NULL COMMENT '教学班ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `org_unit_id` bigint DEFAULT NULL,
  `teacher_id` bigint NOT NULL COMMENT '主讲教师ID',
  `classroom_id` bigint DEFAULT NULL COMMENT '教室ID',
  `weekday` tinyint NOT NULL COMMENT '周几（1-7，1=周一）',
  `start_slot` int NOT NULL COMMENT '开始节次',
  `end_slot` int NOT NULL COMMENT '结束节次（支持连堂）',
  `start_week` int DEFAULT '1' COMMENT '起始周次',
  `end_week` int DEFAULT '16' COMMENT '结束周次',
  `week_type` tinyint DEFAULT '0' COMMENT '单双周：0每周 1单周 2双周',
  `consecutive_group` varchar(50) DEFAULT NULL COMMENT '连排分组标识',
  `schedule_type` tinyint DEFAULT '1' COMMENT '课程类型：1正常 2实验 3实践',
  `entry_status` tinyint DEFAULT '1' COMMENT '状态：1正常 2暂停 0删除',
  `conflict_flag` tinyint DEFAULT '0' COMMENT '冲突标记：0无冲突 1有冲突（强制保存）',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_semester` (`semester_id`),
  KEY `idx_class` (`org_unit_id`),
  KEY `idx_teacher` (`teacher_id`),
  KEY `idx_classroom` (`classroom_id`),
  KEY `idx_time` (`weekday`,`start_slot`),
  KEY `idx_task` (`task_id`),
  KEY `idx_schedule` (`schedule_id`),
  KEY `idx_semester_time` (`semester_id`,`weekday`,`start_slot`),
  KEY `idx_teaching_class` (`teaching_class_id`),
  KEY `idx_consecutive_group` (`consecutive_group`),
  KEY `idx_tenant` (`tenant_id`),
  CONSTRAINT `fk_entry_semester` FOREIGN KEY (`semester_id`) REFERENCES `semesters` (`id`),
  CONSTRAINT `fk_entry_task` FOREIGN KEY (`task_id`) REFERENCES `teaching_tasks` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课表条目表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `schedule_entries`
--

LOCK TABLES `schedule_entries` WRITE;
/*!40000 ALTER TABLE `schedule_entries` DISABLE KEYS */;
/*!40000 ALTER TABLE `schedule_entries` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `schedule_executions`
--

DROP TABLE IF EXISTS `schedule_executions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schedule_executions` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `policy_id` bigint NOT NULL COMMENT '策略ID',
  `execution_date` date NOT NULL COMMENT '执行日期',
  `assigned_inspectors` json DEFAULT NULL COMMENT '分配的检查员',
  `session_id` bigint DEFAULT NULL COMMENT '关联会话ID',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING' COMMENT '状态',
  `failure_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '失败原因',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_policy_id` (`policy_id`),
  KEY `idx_execution_date` (`execution_date`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排程执行表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `schedule_executions`
--

LOCK TABLES `schedule_executions` WRITE;
/*!40000 ALTER TABLE `schedule_executions` DISABLE KEYS */;
/*!40000 ALTER TABLE `schedule_executions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `schedule_instances`
--

DROP TABLE IF EXISTS `schedule_instances`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schedule_instances` (
  `id` bigint NOT NULL,
  `entry_id` bigint DEFAULT NULL COMMENT '来源基准条目',
  `semester_id` bigint NOT NULL,
  `actual_date` date NOT NULL,
  `weekday` tinyint NOT NULL COMMENT '1-7',
  `week_number` int DEFAULT NULL COMMENT '教学周',
  `start_slot` int NOT NULL,
  `end_slot` int NOT NULL,
  `course_id` bigint NOT NULL,
  `org_unit_id` bigint DEFAULT NULL,
  `teacher_id` bigint DEFAULT NULL,
  `original_teacher_id` bigint DEFAULT NULL COMMENT '代课时原教师',
  `classroom_id` bigint DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0正常 1已取消 2已调走 3补课 4代课',
  `cancel_reason` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `source_type` tinyint NOT NULL DEFAULT '0' COMMENT '0基准展开 1调课 2补课日 3临时',
  `source_id` bigint DEFAULT NULL COMMENT '来源ID',
  `actual_hours` decimal(3,1) DEFAULT '1.0',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_sem_date` (`semester_id`,`actual_date`),
  KEY `idx_teacher` (`teacher_id`,`actual_date`),
  KEY `idx_class` (`org_unit_id`,`actual_date`),
  KEY `idx_classroom` (`classroom_id`,`actual_date`),
  KEY `idx_entry` (`entry_id`),
  KEY `idx_week` (`semester_id`,`week_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实况课表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `schedule_instances`
--

LOCK TABLES `schedule_instances` WRITE;
/*!40000 ALTER TABLE `schedule_instances` DISABLE KEYS */;
/*!40000 ALTER TABLE `schedule_instances` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `schedule_policies`
--

DROP TABLE IF EXISTS `schedule_policies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schedule_policies` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `policy_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '策略编码',
  `policy_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '策略名称',
  `policy_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '策略类型',
  `rotation_algorithm` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '轮换算法',
  `template_id` bigint DEFAULT NULL COMMENT '关联模板ID',
  `inspector_pool` json DEFAULT NULL COMMENT '检查员池',
  `schedule_config` json DEFAULT NULL COMMENT '排程配置',
  `excluded_dates` json DEFAULT NULL COMMENT '排除日期',
  `is_enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_policy_code` (`policy_code`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排程策略表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `schedule_policies`
--

LOCK TABLES `schedule_policies` WRITE;
/*!40000 ALTER TABLE `schedule_policies` DISABLE KEYS */;
/*!40000 ALTER TABLE `schedule_policies` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `scheduling_constraints`
--

DROP TABLE IF EXISTS `scheduling_constraints`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `scheduling_constraints` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `semester_id` bigint NOT NULL COMMENT '学期ID',
  `constraint_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '约束名称',
  `constraint_level` tinyint NOT NULL COMMENT '1全局 2教师 3班级 4课程',
  `target_id` bigint DEFAULT NULL COMMENT '目标ID',
  `target_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '目标名称',
  `org_unit_id` bigint DEFAULT NULL COMMENT '归属组织单元ID (NULL=全校)',
  `constraint_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '约束类型枚举',
  `is_hard` tinyint NOT NULL DEFAULT '1' COMMENT '1硬约束 0软约束',
  `priority` int NOT NULL DEFAULT '50' COMMENT '优先级权重(1-100)',
  `params` json NOT NULL COMMENT '约束参数',
  `effective_weeks` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生效周次',
  `enabled` tinyint NOT NULL DEFAULT '1',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_semester` (`semester_id`),
  KEY `idx_level_target` (`constraint_level`,`target_id`),
  KEY `idx_type` (`constraint_type`),
  KEY `idx_tenant` (`tenant_id`),
  KEY `idx_org_unit` (`org_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排课约束规则表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `scheduling_constraints`
--

LOCK TABLES `scheduling_constraints` WRITE;
/*!40000 ALTER TABLE `scheduling_constraints` DISABLE KEYS */;
/*!40000 ALTER TABLE `scheduling_constraints` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `school_events`
--

DROP TABLE IF EXISTS `school_events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `school_events` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `semester_id` bigint DEFAULT NULL COMMENT '所属学期ID（跨学期事件可为空）',
  `event_code` varchar(50) DEFAULT NULL COMMENT '事件代码',
  `event_name` varchar(100) NOT NULL COMMENT '事件名称',
  `event_type` tinyint NOT NULL COMMENT '事件类型：1法定节假日 2学校假期 3校级活动 4调休补课 5临时停课',
  `start_date` date NOT NULL COMMENT '开始日期',
  `end_date` date NOT NULL COMMENT '结束日期',
  `start_time` time DEFAULT NULL COMMENT '开始时间（精确到时间的事件）',
  `end_time` time DEFAULT NULL COMMENT '结束时间',
  `all_day` tinyint DEFAULT '1' COMMENT '是否全天事件',
  `affect_schedule` tinyint DEFAULT '0' COMMENT '是否影响正常排课：1影响（停课）0不影响',
  `affected_org_units` json DEFAULT NULL COMMENT '影响的组织单元ID列表（空表示全校）',
  `swap_to_date` date DEFAULT NULL COMMENT '调休：调到哪天补课',
  `swap_weekday` tinyint DEFAULT NULL COMMENT '调休：按周几的课表上课（1-7）',
  `color` varchar(20) DEFAULT '#1890ff' COMMENT '日历显示颜色',
  `priority` int DEFAULT '0' COMMENT '显示优先级',
  `description` text COMMENT '详细描述',
  `attachment_urls` json DEFAULT NULL COMMENT '附件URL列表',
  `status` tinyint DEFAULT '1' COMMENT '状态：1正常 0取消',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_semester` (`semester_id`),
  KEY `idx_date_range` (`start_date`,`end_date`),
  KEY `idx_event_type` (`event_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='校历事件表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `school_events`
--

LOCK TABLES `school_events` WRITE;
/*!40000 ALTER TABLE `school_events` DISABLE KEYS */;
/*!40000 ALTER TABLE `school_events` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `scoring_strategies`
--

DROP TABLE IF EXISTS `scoring_strategies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `scoring_strategies` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '策略代码',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '策略名称',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '策略描述',
  `category` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'basic' COMMENT '策略分类: basic/grade/advanced/time',
  `formula_template` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'JavaScript公式模板',
  `formula_description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '公式说明，如: 得分 = 基准分 - Σ扣分',
  `parameters_schema` json DEFAULT NULL COMMENT '策略参数定义',
  `default_parameters` json DEFAULT NULL COMMENT '默认参数值',
  `supported_input_types` json DEFAULT NULL COMMENT '支持的打分方式代码列表',
  `supported_rule_types` json DEFAULT NULL COMMENT '支持的计算规则类型',
  `is_system` tinyint(1) DEFAULT '0' COMMENT '是否系统内置',
  `is_enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` int DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_scoring_strategy_category` (`category`),
  KEY `idx_scoring_strategy_enabled` (`is_enabled`),
  KEY `idx_scoring_strategy_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计分策略定义表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `scoring_strategies`
--

LOCK TABLES `scoring_strategies` WRITE;
/*!40000 ALTER TABLE `scoring_strategies` DISABLE KEYS */;
INSERT INTO `scoring_strategies` VALUES (1,'DEDUCTION','扣分制','从基准分开始扣除，得分不低于0','basic','Math.max(0, ctx.baseScore - sum(ctx.deductions))','得分 = 基准分 - Σ扣分',NULL,NULL,'[\"NUMERIC\", \"OPTIONS\", \"CHECKBOX\"]',NULL,1,1,1,NULL,'2026-06-01 23:05:55',NULL,'2026-06-01 23:05:55',0),(2,'ADDITION','加分制','从0分开始累加','basic','Math.min(ctx.maxScore || 100, sum(ctx.additions))','得分 = Σ加分',NULL,NULL,'[\"NUMERIC\", \"OPTIONS\"]',NULL,1,1,2,NULL,'2026-06-01 23:05:55',NULL,'2026-06-01 23:05:55',0),(3,'BASE_ADJUST','基准调整制','基准分可加可减','basic','clamp(ctx.baseScore + sum(ctx.additions) - sum(ctx.deductions), 0, ctx.maxScore || 100)','得分 = 基准分 + Σ加分 - Σ扣分',NULL,NULL,'[\"NUMERIC\", \"OPTIONS\"]',NULL,1,1,3,NULL,'2026-06-01 23:05:55',NULL,'2026-06-01 23:05:55',0),(4,'CHECKLIST','清单制','按完成率计算得分','basic','Math.round(ctx.completedCount / ctx.totalCount * 100)','得分 = 完成数 / 总数 × 100',NULL,NULL,'[\"CHECKBOX\"]',NULL,1,1,4,NULL,'2026-06-01 23:05:55',NULL,'2026-06-01 23:05:55',0),(5,'GRADE','等级制','A/B/C/D等级评定','grade','ctx.gradeMapping[ctx.grade] || 0','等级 → 预设分数',NULL,NULL,'[\"GRADE\"]',NULL,1,1,10,NULL,'2026-06-01 23:05:55',NULL,'2026-06-01 23:05:55',0),(6,'STAR_RATING','星级制','1-5星评定','grade','ctx.stars * 20','得分 = 星数 × 20',NULL,NULL,'[\"STAR\"]',NULL,1,1,11,NULL,'2026-06-01 23:05:55',NULL,'2026-06-01 23:05:55',0),(7,'PASS_FAIL','合格制','合格/不合格二元判定','grade','ctx.passed ? 100 : 0','合格=100, 不合格=0',NULL,NULL,'[\"CHECKBOX\", \"OPTIONS\"]',NULL,1,1,12,NULL,'2026-06-01 23:05:55',NULL,'2026-06-01 23:05:55',0),(8,'COMMENT','评语制','优/良/中/差评定','grade','({\"优\": 95, \"良\": 80, \"中\": 65, \"差\": 40})[ctx.comment] || 0','优=95, 良=80, 中=65, 差=40',NULL,NULL,'[\"OPTIONS\", \"GRADE\"]',NULL,1,1,13,NULL,'2026-06-01 23:05:55',NULL,'2026-06-01 23:05:55',0),(9,'STEPPED','阶梯计分','按档位分级扣分','advanced','lookupStep(ctx.value, ctx.steps)','根据值查找对应档位分数',NULL,NULL,'[\"NUMERIC\", \"OPTIONS\"]',NULL,1,1,20,NULL,'2026-06-01 23:05:55',NULL,'2026-06-01 23:05:55',0),(10,'PROGRESSIVE','累进计分','按次数递增扣分','advanced','ctx.baseDeduction * Math.pow(ctx.multiplier || 2, ctx.occurrenceCount - 1)','第n次扣分 = 基础扣分 × 倍率^(n-1)',NULL,NULL,'[\"NUMERIC\", \"COUNT\"]',NULL,1,1,21,NULL,'2026-06-01 23:05:55',NULL,'2026-06-01 23:05:55',0),(11,'MULTIPLIER','倍率计分','特殊时期加倍','advanced','ctx.score * (ctx.multiplier || 1)','实际分 = 基础分 × 倍率',NULL,NULL,'[\"NUMERIC\"]',NULL,1,1,22,NULL,'2026-06-01 23:05:55',NULL,'2026-06-01 23:05:55',0),(12,'RANKING','排名计分','按名次给分','advanced','ctx.rankingScores[ctx.rank - 1] || 0','第1名=100, 第2名=95, ...',NULL,NULL,'[\"NUMERIC\"]',NULL,1,1,23,NULL,'2026-06-01 23:05:55',NULL,'2026-06-01 23:05:55',0),(13,'TIME_DECAY','时间衰减','历史数据权重递减','time','weightedAverage(ctx.periodScores, ctx.weights)','加权平均，近期权重大',NULL,NULL,'[\"NUMERIC\"]',NULL,1,1,30,NULL,'2026-06-01 23:05:55',NULL,'2026-06-01 23:05:55',0),(14,'ROLLING','滚动计算','取最近N期平均','time','average(ctx.periodScores.slice(-ctx.periods))','最近N期平均分',NULL,NULL,'[\"NUMERIC\"]',NULL,1,1,31,NULL,'2026-06-01 23:05:55',NULL,'2026-06-01 23:05:55',0),(15,'TREND_BONUS','趋势加成','进步给予奖励','time','ctx.currentScore + (ctx.currentScore > ctx.previousScore ? ctx.bonusPoints : 0)','环比上升则加分',NULL,NULL,'[\"NUMERIC\"]',NULL,1,1,32,NULL,'2026-06-01 23:05:55',NULL,'2026-06-01 23:05:55',0),(16,'FORCED_DIST','强制分布','按比例限制等级','time','applyForcedDistribution(ctx.scores, ctx.distribution)','A(10%), B(20%), C(40%), D(20%), E(10%)',NULL,NULL,'[\"GRADE\"]',NULL,1,1,33,NULL,'2026-06-01 23:05:55',NULL,'2026-06-01 23:05:55',0);
/*!40000 ALTER TABLE `scoring_strategies` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `semester_course_offerings`
--

DROP TABLE IF EXISTS `semester_course_offerings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `semester_course_offerings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `semester_id` bigint NOT NULL COMMENT '学期ID',
  `plan_id` bigint DEFAULT NULL COMMENT '来源培养方案ID（可选）',
  `plan_course_id` bigint DEFAULT NULL COMMENT '来源方案课程ID（可选）',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `applicable_grade` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '适用年级',
  `org_unit_id` bigint DEFAULT NULL COMMENT '归属组织单元ID (数据权限)',
  `weekly_hours` int NOT NULL COMMENT '周课时数',
  `total_weeks` int DEFAULT NULL COMMENT '总周数',
  `start_week` int NOT NULL DEFAULT '1' COMMENT '起始周',
  `end_week` int DEFAULT NULL COMMENT '结束周',
  `course_category` tinyint DEFAULT NULL COMMENT '课程类别',
  `course_type` tinyint DEFAULT NULL COMMENT '课程性质',
  `allow_combined` tinyint NOT NULL DEFAULT '0' COMMENT '是否允许合堂',
  `max_combined_classes` int NOT NULL DEFAULT '2' COMMENT '最大合堂班数',
  `allow_walking` tinyint NOT NULL DEFAULT '0' COMMENT '是否允许走班',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0草稿 1已确认 2已完成分配',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `week_type` int DEFAULT NULL COMMENT '0=每周 1=单周 2=双周',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_semester_course_grade` (`semester_id`,`course_id`,`applicable_grade`),
  KEY `idx_semester` (`semester_id`),
  KEY `idx_tenant` (`tenant_id`),
  KEY `idx_org_unit` (`org_unit_id`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学期开课计划表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `semester_course_offerings`
--

LOCK TABLES `semester_course_offerings` WRITE;
/*!40000 ALTER TABLE `semester_course_offerings` DISABLE KEYS */;
INSERT INTO `semester_course_offerings` VALUES (14,2042273577180405761,NULL,NULL,1775537914102,'2024级',NULL,3,NULL,1,16,1,1,0,2,0,1,NULL,NULL,'2026-06-01 23:06:00','2026-06-01 23:06:00',0,1,NULL),(15,2042273577180405761,NULL,NULL,1775537914105,'2024级',NULL,3,NULL,1,16,1,1,0,2,0,1,NULL,NULL,'2026-06-01 23:06:00','2026-06-01 23:06:00',0,1,NULL),(16,2042273577180405761,NULL,NULL,1775537914106,'2024级',NULL,2,NULL,1,16,1,1,0,2,0,1,NULL,NULL,'2026-06-01 23:06:00','2026-06-01 23:06:00',0,1,NULL),(17,2042273577180405761,NULL,NULL,1775537914101,'2024级',NULL,4,NULL,1,16,1,1,0,2,0,1,NULL,NULL,'2026-06-01 23:06:00','2026-06-01 23:06:00',0,1,NULL),(18,2042273577180405761,NULL,NULL,1775537914103,'2024级',NULL,3,NULL,1,16,2,1,0,2,0,1,NULL,NULL,'2026-06-01 23:06:00','2026-06-01 23:06:00',0,1,NULL),(19,2042273577180405761,NULL,NULL,1775537914104,'2024级',NULL,2,NULL,1,16,2,1,0,2,0,1,NULL,NULL,'2026-06-01 23:06:00','2026-06-01 23:06:00',0,1,NULL),(20,2042273577180405761,NULL,NULL,2040360775176011777,'2024级',NULL,5,NULL,1,9,2,3,0,2,0,1,NULL,NULL,'2026-06-01 23:06:00','2026-06-01 23:06:00',0,1,NULL),(21,2042273577180405761,NULL,NULL,1775537914102,'2025级',NULL,3,NULL,1,16,1,1,0,2,0,1,NULL,NULL,'2026-06-01 23:06:00','2026-06-01 23:06:00',0,1,NULL),(22,2042273577180405761,NULL,NULL,1775537914105,'2025级',NULL,3,NULL,1,16,1,1,0,2,0,1,NULL,NULL,'2026-06-01 23:06:00','2026-06-01 23:06:00',0,1,NULL),(23,2042273577180405761,NULL,NULL,1775537914106,'2025级',NULL,2,NULL,1,16,1,1,0,2,0,1,NULL,NULL,'2026-06-01 23:06:00','2026-06-01 23:06:00',0,1,NULL),(24,2042273577180405761,NULL,NULL,1775537914101,'2025级',NULL,4,NULL,1,16,1,1,0,2,0,1,NULL,NULL,'2026-06-01 23:06:00','2026-06-01 23:06:00',0,1,NULL),(25,2042273577180405761,NULL,NULL,1775537914103,'2025级',NULL,3,NULL,1,16,2,1,0,2,0,1,NULL,NULL,'2026-06-01 23:06:00','2026-06-01 23:06:00',0,1,NULL),(26,2042273577180405761,NULL,NULL,1775537914104,'2025级',NULL,2,NULL,1,16,2,1,0,2,0,1,NULL,NULL,'2026-06-01 23:06:00','2026-06-01 23:06:00',0,1,NULL);
/*!40000 ALTER TABLE `semester_course_offerings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `semesters`
--

DROP TABLE IF EXISTS `semesters`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `semesters` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `academic_year_id` bigint DEFAULT NULL COMMENT '所属学年ID',
  `semester_code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学期编码:2024-2025-1',
  `semester_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学期名称',
  `academic_year` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学年:2024-2025',
  `semester_type` tinyint NOT NULL COMMENT '学期类型:1第一学期,2第二学期',
  `start_date` date NOT NULL COMMENT '开始日期',
  `end_date` date NOT NULL COMMENT '结束日期',
  `is_current` tinyint DEFAULT '0' COMMENT '是否当前学期',
  `status` tinyint DEFAULT '1' COMMENT '状态:1启用,0禁用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `start_year` int DEFAULT NULL,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_semester_code` (`semester_code`),
  KEY `idx_academic_year` (`academic_year`),
  KEY `idx_current` (`is_current`),
  KEY `idx_tenant` (`tenant_id`),
  KEY `idx_semester_academic_year` (`academic_year_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学期表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `semesters`
--

LOCK TABLES `semesters` WRITE;
/*!40000 ALTER TABLE `semesters` DISABLE KEYS */;
/*!40000 ALTER TABLE `semesters` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `space`
--

DROP TABLE IF EXISTS `space`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `space` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `space_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '场所编码(唯一)',
  `space_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '场所名称',
  `space_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '场所类型: CAMPUS/BUILDING/FLOOR/ROOM',
  `room_type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '房间类型(仅ROOM有): DORMITORY/CLASSROOM/OFFICE...',
  `building_type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '楼宇类型(仅BUILDING有): TEACHING/DORMITORY/OFFICE/MIXED',
  `building_no` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '楼号（如 1, A, 甲）',
  `room_no` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '房间号（如 101, 302）',
  `parent_id` bigint DEFAULT NULL COMMENT '父级ID',
  `path` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '物化路径: /1/2/3/',
  `level` int NOT NULL DEFAULT '0' COMMENT '层级深度(0=根)',
  `campus_id` bigint DEFAULT NULL COMMENT '所属校区ID',
  `building_id` bigint DEFAULT NULL COMMENT '所属楼宇ID',
  `floor_number` int DEFAULT NULL COMMENT '楼层号',
  `capacity` int DEFAULT NULL COMMENT '容量/座位数/床位数',
  `current_occupancy` int DEFAULT '0' COMMENT '当前占用数',
  `org_unit_id` bigint DEFAULT NULL COMMENT '所属组织单元',
  `class_id` bigint DEFAULT NULL COMMENT '归属班级ID（班主任管理）',
  `gender_type` tinyint DEFAULT '0' COMMENT '性别类型：0-不限/混合，1-男，2-女',
  `responsible_user_id` bigint DEFAULT NULL COMMENT '负责人ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0停用 1正常 2维修中',
  `attributes` json DEFAULT NULL COMMENT '扩展属性',
  `description` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述/备注',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_space_code` (`space_code`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_space_type` (`space_type`),
  KEY `idx_room_type` (`room_type`),
  KEY `idx_building_type` (`building_type`),
  KEY `idx_building_id` (`building_id`),
  KEY `idx_floor_number` (`floor_number`),
  KEY `idx_org_unit_id` (`org_unit_id`),
  KEY `idx_status` (`status`),
  KEY `idx_path` (`path`(255)),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_space_class` (`class_id`),
  KEY `idx_space_gender_type` (`gender_type`),
  KEY `idx_space_building_no` (`building_no`),
  KEY `idx_space_room_no` (`room_no`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场所统一表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `space`
--

LOCK TABLES `space` WRITE;
/*!40000 ALTER TABLE `space` DISABLE KEYS */;
INSERT INTO `space` VALUES (1,'CAMPUS_MAIN','主校区','CAMPUS',NULL,NULL,NULL,NULL,NULL,'/1/',0,NULL,NULL,NULL,NULL,0,NULL,NULL,0,NULL,1,NULL,NULL,NULL,'2026-06-01 23:06:07',NULL,'2026-06-01 23:06:07',0);
/*!40000 ALTER TABLE `space` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `space_assignments`
--

DROP TABLE IF EXISTS `space_assignments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `space_assignments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `space_id` bigint NOT NULL COMMENT '空间ID',
  `org_unit_id` bigint NOT NULL COMMENT '组织单元ID',
  `assignment_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PRIMARY' COMMENT '分配类型: PRIMARY-主要 SHARED-共享',
  `allocated_capacity` int DEFAULT NULL COMMENT '分配的容量',
  `priority` int DEFAULT '0' COMMENT '优先级',
  `is_enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` int DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_space_org` (`space_id`,`org_unit_id`,`deleted`),
  KEY `idx_space_id` (`space_id`),
  KEY `idx_org_unit` (`org_unit_id`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='空间分配记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `space_assignments`
--

LOCK TABLES `space_assignments` WRITE;
/*!40000 ALTER TABLE `space_assignments` DISABLE KEYS */;
/*!40000 ALTER TABLE `space_assignments` ENABLE KEYS */;
UNLOCK TABLES;


--
-- Table structure for table `space_categories`
--

DROP TABLE IF EXISTS `space_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `space_categories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类编码',
  `category_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类名称',
  `apply_to_level` enum('BUILDING','ROOM') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '适用层级：楼栋或房间',
  `icon` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图标',
  `color` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '颜色',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
  `has_capacity` tinyint(1) DEFAULT '0' COMMENT '是否有容量',
  `capacity_unit` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '容量单位：人/床位/座位',
  `default_capacity` int DEFAULT NULL COMMENT '默认容量',
  `bookable` tinyint(1) DEFAULT '0' COMMENT '是否可预订',
  `assignable` tinyint(1) DEFAULT '0' COMMENT '是否可分配给组织/班级',
  `occupiable` tinyint(1) DEFAULT '0' COMMENT '是否可入住',
  `has_gender` tinyint(1) DEFAULT '0' COMMENT '是否区分性别（宿舍用）',
  `is_system` tinyint(1) DEFAULT '0' COMMENT '是否系统预置',
  `is_enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` int DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_code` (`category_code`),
  KEY `idx_apply_to_level` (`apply_to_level`),
  KEY `idx_enabled` (`is_enabled`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='空间分类配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `space_categories`
--

LOCK TABLES `space_categories` WRITE;
/*!40000 ALTER TABLE `space_categories` DISABLE KEYS */;
INSERT INTO `space_categories` VALUES (1,'TEACHING_BUILDING','教学楼','BUILDING','School','#52c41a','教学用途楼栋',0,NULL,NULL,0,1,0,0,1,1,1,NULL,'2026-06-01 23:05:56',NULL,'2026-06-01 23:05:56',0),(2,'DORMITORY_BUILDING','宿舍楼','BUILDING','Home','#1890ff','学生宿舍楼栋',0,NULL,NULL,0,1,0,0,1,1,2,NULL,'2026-06-01 23:05:56',NULL,'2026-06-01 23:05:56',0),(3,'OFFICE_BUILDING','办公楼','BUILDING','OfficeBuilding','#fa8c16','行政办公楼栋',0,NULL,NULL,0,1,0,0,1,1,3,NULL,'2026-06-01 23:05:56',NULL,'2026-06-01 23:05:56',0),(4,'COMPLEX_BUILDING','综合楼','BUILDING','Building','#722ed1','多功能综合楼栋',0,NULL,NULL,0,1,0,0,1,1,4,NULL,'2026-06-01 23:05:56',NULL,'2026-06-01 23:05:56',0),(5,'LAB_BUILDING','实验楼','BUILDING','Experiment','#13c2c2','实验教学楼栋',0,NULL,NULL,0,1,0,0,1,1,5,NULL,'2026-06-01 23:05:56',NULL,'2026-06-01 23:05:56',0),(6,'DORMITORY','学生宿舍','ROOM','Bed','#1890ff','学生住宿房间',1,'床位',6,0,1,1,1,1,1,10,NULL,'2026-06-01 23:05:56',NULL,'2026-06-01 23:05:56',0),(7,'CLASSROOM','普通教室','ROOM','Book','#52c41a','普通教学教室',1,'座位',50,1,1,0,0,1,1,11,NULL,'2026-06-01 23:05:56',NULL,'2026-06-01 23:05:56',0),(8,'MULTIMEDIA_ROOM','多媒体教室','ROOM','Monitor','#52c41a','配备多媒体设备的教室',1,'座位',60,1,1,0,0,1,1,12,NULL,'2026-06-01 23:05:56',NULL,'2026-06-01 23:05:56',0),(9,'LAB','实验室','ROOM','Flask','#13c2c2','实验教学用房',1,'座位',30,1,1,0,0,1,1,13,NULL,'2026-06-01 23:05:56',NULL,'2026-06-01 23:05:56',0),(10,'COMPUTER_ROOM','机房','ROOM','Desktop','#13c2c2','计算机教学用房',1,'座位',50,1,1,0,0,1,1,14,NULL,'2026-06-01 23:05:56',NULL,'2026-06-01 23:05:56',0),(11,'MEETING_ROOM','会议室','ROOM','Users','#722ed1','会议用房',1,'座位',20,1,0,0,0,1,1,15,NULL,'2026-06-01 23:05:56',NULL,'2026-06-01 23:05:56',0),(12,'OFFICE','办公室','ROOM','Briefcase','#fa8c16','行政办公用房',1,'人',4,0,1,1,0,1,1,16,NULL,'2026-06-01 23:05:56',NULL,'2026-06-01 23:05:56',0),(13,'WAREHOUSE','仓库','ROOM','Box','#8c8c8c','物资存储用房',0,NULL,NULL,0,1,0,0,1,1,17,NULL,'2026-06-01 23:05:56',NULL,'2026-06-01 23:05:56',0),(14,'ACTIVITY_ROOM','活动室','ROOM','Trophy','#eb2f96','学生活动用房',1,'人',30,1,0,0,0,1,1,18,NULL,'2026-06-01 23:05:56',NULL,'2026-06-01 23:05:56',0);
/*!40000 ALTER TABLE `space_categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `space_class_assignment`
--

DROP TABLE IF EXISTS `space_class_assignment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `space_class_assignment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `place_id` bigint NOT NULL COMMENT '场所ID',
  `class_id` bigint NOT NULL COMMENT '班级ID',
  `org_unit_id` bigint NOT NULL COMMENT '所属部门ID（冗余，便于权限控制）',
  `assigned_beds` int DEFAULT NULL COMMENT '分配床位数（NULL表示不限）',
  `priority` int DEFAULT '0' COMMENT '优先级（数值越大越优先）',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `assigned_by` bigint DEFAULT NULL COMMENT '分配人ID',
  `assigned_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '分配时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_space_class` (`place_id`,`class_id`),
  KEY `idx_class_id` (`class_id`),
  KEY `idx_org_unit_id` (`org_unit_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='场所-班级分配表（部门分配给班级）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `space_class_assignment`
--

LOCK TABLES `space_class_assignment` WRITE;
/*!40000 ALTER TABLE `space_class_assignment` DISABLE KEYS */;
/*!40000 ALTER TABLE `space_class_assignment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `space_classroom_ext`
--

DROP TABLE IF EXISTS `space_classroom_ext`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `space_classroom_ext` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `space_id` bigint NOT NULL COMMENT '场所ID',
  `classroom_category` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '教室分类: NORMAL/MULTIMEDIA/SMART',
  `assigned_class_id` bigint DEFAULT NULL COMMENT '固定使用班级ID',
  `has_projector` tinyint DEFAULT '0' COMMENT '是否有投影仪',
  `has_air_conditioner` tinyint DEFAULT '0' COMMENT '是否有空调',
  `has_computer` tinyint DEFAULT '0' COMMENT '是否有电脑',
  `equipment_info` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '设备配置说明',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `space_id` (`space_id`),
  KEY `idx_space_id` (`space_id`),
  KEY `idx_assigned_class_id` (`assigned_class_id`),
  KEY `idx_classroom_category` (`classroom_category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教室扩展属性表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `space_classroom_ext`
--

LOCK TABLES `space_classroom_ext` WRITE;
/*!40000 ALTER TABLE `space_classroom_ext` DISABLE KEYS */;
/*!40000 ALTER TABLE `space_classroom_ext` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `space_dormitory_ext`
--

DROP TABLE IF EXISTS `space_dormitory_ext`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `space_dormitory_ext` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `space_id` bigint NOT NULL COMMENT '场所ID',
  `gender_type` tinyint NOT NULL DEFAULT '1' COMMENT '性别类型: 1男 2女 3混合',
  `bed_count` int DEFAULT NULL COMMENT '床位数(可能与capacity不同)',
  `facilities` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '设施配置(空调/热水器/独卫等)',
  `assigned_class_ids` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '指定班级ID列表(逗号分隔)',
  `supervisor_id` bigint DEFAULT NULL COMMENT '宿管员ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `space_id` (`space_id`),
  KEY `idx_space_id` (`space_id`),
  KEY `idx_gender_type` (`gender_type`),
  KEY `idx_supervisor_id` (`supervisor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宿舍扩展属性表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `space_dormitory_ext`
--

LOCK TABLES `space_dormitory_ext` WRITE;
/*!40000 ALTER TABLE `space_dormitory_ext` DISABLE KEYS */;
/*!40000 ALTER TABLE `space_dormitory_ext` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `space_lab_ext`
--

DROP TABLE IF EXISTS `space_lab_ext`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `space_lab_ext` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `space_id` bigint NOT NULL COMMENT '场所ID',
  `lab_category` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '实验室分类: PHYSICS/CHEMISTRY/COMPUTER/TRAINING',
  `safety_level` tinyint DEFAULT '1' COMMENT '安全等级: 1普通 2中等 3高',
  `major_id` bigint DEFAULT NULL COMMENT '所属专业ID',
  `equipment_list` text COLLATE utf8mb4_unicode_ci COMMENT '设备清单(JSON)',
  `safety_notice` text COLLATE utf8mb4_unicode_ci COMMENT '安全须知',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `space_id` (`space_id`),
  KEY `idx_space_id` (`space_id`),
  KEY `idx_major_id` (`major_id`),
  KEY `idx_lab_category` (`lab_category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实验室/实训室扩展属性表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `space_lab_ext`
--

LOCK TABLES `space_lab_ext` WRITE;
/*!40000 ALTER TABLE `space_lab_ext` DISABLE KEYS */;
/*!40000 ALTER TABLE `space_lab_ext` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `space_occupant`
--

DROP TABLE IF EXISTS `space_occupant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `space_occupant` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `space_id` bigint NOT NULL COMMENT '场所ID(宿舍/办公室)',
  `occupant_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '占用者类型: STUDENT/TEACHER/STAFF',
  `occupant_id` bigint NOT NULL COMMENT '占用者ID',
  `position_no` int DEFAULT NULL COMMENT '位置编号(床位号/工位号)',
  `check_in_date` date DEFAULT NULL COMMENT '入住日期',
  `check_out_date` date DEFAULT NULL COMMENT '退出日期',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0已退出 1在住',
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_space_position_active` (`space_id`,`position_no`,`status`),
  KEY `idx_space_id` (`space_id`),
  KEY `idx_occupant` (`occupant_type`,`occupant_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场所占用表(学生入住/工位分配)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `space_occupant`
--

LOCK TABLES `space_occupant` WRITE;
/*!40000 ALTER TABLE `space_occupant` DISABLE KEYS */;
/*!40000 ALTER TABLE `space_occupant` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `space_occupants`
--

DROP TABLE IF EXISTS `space_occupants`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `space_occupants` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `space_id` bigint NOT NULL COMMENT '空间ID',
  `occupant_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '占用者类型',
  `occupant_id` bigint NOT NULL COMMENT '占用者ID',
  `occupant_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '占用者名称(冗余)',
  `position_no` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '位置号(床位号/工位号)',
  `check_in_time` datetime NOT NULL COMMENT '入住时间',
  `check_out_time` datetime DEFAULT NULL COMMENT '退出时间',
  `status` tinyint DEFAULT '1' COMMENT '状态: 0-已退出 1-在住',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` int DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_space_id` (`space_id`),
  KEY `idx_occupant` (`occupant_type`,`occupant_id`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='空间占用记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `space_occupants`
--

LOCK TABLES `space_occupants` WRITE;
/*!40000 ALTER TABLE `space_occupants` DISABLE KEYS */;
/*!40000 ALTER TABLE `space_occupants` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `space_office_ext`
--

DROP TABLE IF EXISTS `space_office_ext`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `space_office_ext` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `space_id` bigint NOT NULL COMMENT '场所ID',
  `office_type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '办公室类型: PRIVATE/SHARED/OPEN',
  `department_id` bigint DEFAULT NULL COMMENT '所属部门ID',
  `workstation_count` int DEFAULT NULL COMMENT '工位数量',
  `phone_number` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '办公电话',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `space_id` (`space_id`),
  KEY `idx_space_id` (`space_id`),
  KEY `idx_department_id` (`department_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='办公室扩展属性表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `space_office_ext`
--

LOCK TABLES `space_office_ext` WRITE;
/*!40000 ALTER TABLE `space_office_ext` DISABLE KEYS */;
/*!40000 ALTER TABLE `space_office_ext` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `space_org_assignment`
--

DROP TABLE IF EXISTS `space_org_assignment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `space_org_assignment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `space_id` bigint NOT NULL COMMENT '场所ID(通常是楼宇)',
  `org_unit_id` bigint NOT NULL COMMENT '组织单元ID',
  `floor_start` int DEFAULT NULL COMMENT '起始楼层',
  `floor_end` int DEFAULT NULL COMMENT '结束楼层',
  `assignment_type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'EXCLUSIVE' COMMENT '分配类型: EXCLUSIVE独占/SHARED共享',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0无效 1有效',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_space_org` (`space_id`,`org_unit_id`),
  KEY `idx_space_id` (`space_id`),
  KEY `idx_org_unit_id` (`org_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场所-组织单元分配表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `space_org_assignment`
--

LOCK TABLES `space_org_assignment` WRITE;
/*!40000 ALTER TABLE `space_org_assignment` DISABLE KEYS */;
/*!40000 ALTER TABLE `space_org_assignment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `space_org_relation_history`
--

DROP TABLE IF EXISTS `space_org_relation_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `space_org_relation_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `relation_id` bigint NOT NULL COMMENT '关系ID',
  `place_id` bigint NOT NULL COMMENT '场所ID',
  `org_unit_id` bigint NOT NULL COMMENT '组织单元ID',
  `relation_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '关系类型',
  `action` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作类型: CREATE, UPDATE, DELETE',
  `old_values` json DEFAULT NULL COMMENT '旧值',
  `new_values` json DEFAULT NULL COMMENT '新值',
  `operated_by` bigint DEFAULT NULL COMMENT '操作人',
  `operated_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_relation_id` (`relation_id`),
  KEY `idx_space_id` (`place_id`),
  KEY `idx_org_unit_id` (`org_unit_id`),
  KEY `idx_operated_at` (`operated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场所-组织关系历史表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `space_org_relation_history`
--

LOCK TABLES `space_org_relation_history` WRITE;
/*!40000 ALTER TABLE `space_org_relation_history` DISABLE KEYS */;
/*!40000 ALTER TABLE `space_org_relation_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `space_org_relations`
--

DROP TABLE IF EXISTS `space_org_relations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `space_org_relations` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `place_id` bigint NOT NULL COMMENT '场所ID',
  `org_unit_id` bigint NOT NULL COMMENT '组织单元ID',
  `relation_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PRIMARY' COMMENT '关系类型: PRIMARY-主归属, SHARED-共用, MANAGED-托管',
  `is_primary` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否主归属',
  `priority_level` int DEFAULT '1' COMMENT '优先级（多组织共用时的优先顺序）',
  `can_use` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否有使用权',
  `can_manage` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否有管理权',
  `can_assign` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否有分配权（可分配座位/床位）',
  `can_inspect` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否有检查权',
  `use_schedule` json DEFAULT NULL COMMENT '使用时间安排（周几、时段等）',
  `start_date` date DEFAULT NULL COMMENT '开始日期',
  `end_date` date DEFAULT NULL COMMENT '结束日期',
  `allocated_capacity` int DEFAULT NULL COMMENT '分配的容量（座位/床位数）',
  `weight_ratio` decimal(5,2) DEFAULT '100.00' COMMENT '权重比例',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int DEFAULT '0' COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_space_org_type` (`place_id`,`org_unit_id`,`relation_type`,`deleted`),
  KEY `idx_space_id` (`place_id`),
  KEY `idx_org_unit_id` (`org_unit_id`),
  KEY `idx_relation_type` (`relation_type`),
  KEY `idx_is_primary` (`is_primary`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场所-组织关系表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `space_org_relations`
--

LOCK TABLES `space_org_relations` WRITE;
/*!40000 ALTER TABLE `space_org_relations` DISABLE KEYS */;
/*!40000 ALTER TABLE `space_org_relations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `space_type_config`
--

DROP TABLE IF EXISTS `space_type_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `space_type_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型编码',
  `type_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型名称',
  `type_category` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型分类: BUILDING/ROOM',
  `icon` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图标名称(lucide图标)',
  `color` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '主题色(tailwind类名)',
  `has_capacity` tinyint DEFAULT '1' COMMENT '是否有容量概念',
  `has_occupancy` tinyint DEFAULT '0' COMMENT '是否有入住/使用人员',
  `has_gender` tinyint DEFAULT '0' COMMENT '是否区分性别',
  `default_capacity` int DEFAULT NULL COMMENT '默认容量',
  `attribute_schema` json DEFAULT NULL COMMENT '扩展属性JSON Schema',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `enabled` tinyint DEFAULT '1' COMMENT '是否启用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `type_code` (`type_code`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场所类型配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `space_type_config`
--

LOCK TABLES `space_type_config` WRITE;
/*!40000 ALTER TABLE `space_type_config` DISABLE KEYS */;
INSERT INTO `space_type_config` VALUES (1,'TEACHING','教学楼','BUILDING','GraduationCap','blue',0,0,0,NULL,NULL,1,1,'2026-06-01 23:06:07','2026-06-01 23:06:07'),(2,'DORMITORY_BUILDING','宿舍楼','BUILDING','Home','teal',0,0,0,NULL,NULL,2,1,'2026-06-01 23:06:07','2026-06-01 23:06:07'),(3,'OFFICE_BUILDING','办公楼','BUILDING','Building2','gray',0,0,0,NULL,NULL,3,1,'2026-06-01 23:06:07','2026-06-01 23:06:07'),(4,'MIXED','综合楼','BUILDING','Layers','purple',0,0,0,NULL,NULL,4,1,'2026-06-01 23:06:07','2026-06-01 23:06:07'),(5,'DORMITORY','学生宿舍','ROOM','BedDouble','teal',1,1,1,6,NULL,10,1,'2026-06-01 23:06:07','2026-06-01 23:06:07'),(6,'STAFF_DORMITORY','教职工宿舍','ROOM','Bed','cyan',1,1,0,2,NULL,11,1,'2026-06-01 23:06:07','2026-06-01 23:06:07'),(7,'CLASSROOM','普通教室','ROOM','School','blue',1,0,0,50,NULL,20,1,'2026-06-01 23:06:07','2026-06-01 23:06:07'),(8,'MULTIMEDIA','多媒体教室','ROOM','Monitor','indigo',1,0,0,60,NULL,21,1,'2026-06-01 23:06:07','2026-06-01 23:06:07'),(9,'SMART_CLASSROOM','智慧教室','ROOM','Cpu','violet',1,0,0,40,NULL,22,1,'2026-06-01 23:06:07','2026-06-01 23:06:07'),(10,'LAB','实验室','ROOM','FlaskConical','amber',1,0,0,30,NULL,30,1,'2026-06-01 23:06:07','2026-06-01 23:06:07'),(11,'COMPUTER_LAB','计算机房','ROOM','Monitor','sky',1,0,0,50,NULL,31,1,'2026-06-01 23:06:07','2026-06-01 23:06:07'),(12,'TRAINING','实训室','ROOM','Wrench','orange',1,0,0,40,NULL,32,1,'2026-06-01 23:06:07','2026-06-01 23:06:07'),(13,'OFFICE','办公室','ROOM','Briefcase','slate',1,1,0,4,NULL,40,1,'2026-06-01 23:06:07','2026-06-01 23:06:07'),(14,'MEETING','会议室','ROOM','Users','emerald',1,0,0,20,NULL,41,1,'2026-06-01 23:06:07','2026-06-01 23:06:07'),(15,'LIBRARY','图书馆/阅览室','ROOM','BookOpen','amber',1,0,0,100,NULL,50,1,'2026-06-01 23:06:07','2026-06-01 23:06:07'),(16,'STORAGE','仓库','ROOM','Package','stone',0,0,0,NULL,NULL,60,1,'2026-06-01 23:06:07','2026-06-01 23:06:07'),(17,'UTILITY','功能房','ROOM','Settings','zinc',0,0,0,NULL,NULL,70,1,'2026-06-01 23:06:07','2026-06-01 23:06:07'),(18,'BATHROOM','卫生间','ROOM','Bath','gray',0,0,1,NULL,NULL,71,1,'2026-06-01 23:06:07','2026-06-01 23:06:07'),(19,'POWER_ROOM','配电室','ROOM','Zap','yellow',0,0,0,NULL,NULL,72,1,'2026-06-01 23:06:07','2026-06-01 23:06:07');
/*!40000 ALTER TABLE `space_type_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `space_types`
--

DROP TABLE IF EXISTS `space_types`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `space_types` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `type_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型编码（唯一标识）',
  `type_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型名称',
  `parent_type_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '父类型编码',
  `level_order` int NOT NULL DEFAULT '0' COMMENT '层级顺序（0=顶级）',
  `icon` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图标名称',
  `color` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '颜色代码',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '类型描述',
  `can_have_beds` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否可有床位（宿舍类）',
  `can_have_seats` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否可有座位（教室类）',
  `can_be_inspected` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否可被检查',
  `can_be_borrowed` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否可借用',
  `requires_approval` tinyint(1) NOT NULL DEFAULT '0' COMMENT '借用是否需审批',
  `default_capacity` int DEFAULT NULL COMMENT '默认容量',
  `capacity_unit` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '容量单位（人/床/座）',
  `is_system` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否系统预置',
  `is_enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_code` (`type_code`),
  KEY `idx_parent_type_code` (`parent_type_code`),
  KEY `idx_is_enabled` (`is_enabled`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场所类型配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `space_types`
--

LOCK TABLES `space_types` WRITE;
/*!40000 ALTER TABLE `space_types` DISABLE KEYS */;
INSERT INTO `space_types` VALUES (1,'DORMITORY','宿舍区',NULL,0,'Home','#1890ff','学生宿舍区域',0,0,1,0,0,NULL,NULL,1,1,1,'2026-06-01 23:05:54','2026-06-01 23:05:54',0),(2,'TEACHING','教学区',NULL,0,'School','#52c41a','教学设施区域',0,0,1,0,0,NULL,NULL,1,1,2,'2026-06-01 23:05:54','2026-06-01 23:05:54',0),(3,'ACTIVITY','活动区',NULL,0,'Trophy','#722ed1','活动场所区域',0,0,1,1,1,NULL,NULL,1,1,3,'2026-06-01 23:05:54','2026-06-01 23:05:54',0),(4,'OFFICE','办公区',NULL,0,'Briefcase','#fa8c16','办公场所区域',0,0,0,0,0,NULL,NULL,1,1,4,'2026-06-01 23:05:54','2026-06-01 23:05:54',0),(5,'DORM_BUILDING','宿舍楼','DORMITORY',1,'Building','#1890ff','宿舍楼栋',0,0,1,0,0,NULL,NULL,1,1,10,'2026-06-01 23:05:54','2026-06-01 23:05:54',0),(6,'DORM_FLOOR','宿舍楼层','DORM_BUILDING',2,'Layers','#1890ff','宿舍楼层',0,0,1,0,0,NULL,NULL,1,1,11,'2026-06-01 23:05:54','2026-06-01 23:05:54',0),(7,'DORM_ROOM','宿舍房间','DORM_FLOOR',3,'Home','#1890ff','学生宿舍房间',1,0,1,0,0,6,'床',1,1,12,'2026-06-01 23:05:54','2026-06-01 23:05:54',0),(8,'TEACH_BUILDING','教学楼','TEACHING',1,'Building','#52c41a','教学楼栋',0,0,1,0,0,NULL,NULL,1,1,20,'2026-06-01 23:05:54','2026-06-01 23:05:54',0),(9,'CLASSROOM','教室','TEACH_BUILDING',2,'Book','#52c41a','普通教室',0,1,1,1,0,50,'座',1,1,21,'2026-06-01 23:05:54','2026-06-01 23:05:54',0),(10,'LAB','实验室','TEACH_BUILDING',2,'Flask','#52c41a','实验教学室',0,1,1,1,1,30,'座',1,1,22,'2026-06-01 23:05:54','2026-06-01 23:05:54',0),(11,'COMPUTER_ROOM','机房','TEACH_BUILDING',2,'Monitor','#52c41a','计算机教室',0,1,1,1,1,60,'座',1,1,23,'2026-06-01 23:05:54','2026-06-01 23:05:54',0),(12,'LIBRARY','图书馆','TEACHING',1,'BookOpen','#52c41a','图书馆阅览室',0,1,0,0,0,200,'座',1,1,24,'2026-06-01 23:05:54','2026-06-01 23:05:54',0),(13,'MEETING_ROOM','会议室','ACTIVITY',1,'Users','#722ed1','会议活动室',0,1,0,1,1,20,'座',1,1,30,'2026-06-01 23:05:54','2026-06-01 23:05:54',0),(14,'LECTURE_HALL','报告厅','ACTIVITY',1,'Mic','#722ed1','大型报告厅',0,1,0,1,1,300,'座',1,1,31,'2026-06-01 23:05:54','2026-06-01 23:05:54',0),(15,'GYM','体育馆','ACTIVITY',1,'Activity','#722ed1','室内体育馆',0,0,0,1,1,NULL,NULL,1,1,32,'2026-06-01 23:05:54','2026-06-01 23:05:54',0),(16,'PLAYGROUND','运动场','ACTIVITY',1,'Sun','#722ed1','室外运动场地',0,0,0,1,0,NULL,NULL,1,1,33,'2026-06-01 23:05:54','2026-06-01 23:05:54',0);
/*!40000 ALTER TABLE `space_types` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `spaces`
--

DROP TABLE IF EXISTS `spaces`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `spaces` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `space_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '空间编码',
  `space_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '空间名称',
  `type_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '空间类型编码',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
  `parent_id` bigint DEFAULT NULL COMMENT '父级ID',
  `path` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '物化路径',
  `level` int DEFAULT '0' COMMENT '层级深度',
  `capacity` int DEFAULT NULL COMMENT '容量',
  `current_occupancy` int DEFAULT '0' COMMENT '当前占用数',
  `org_unit_id` bigint DEFAULT NULL COMMENT '所属组织单元ID',
  `responsible_user_id` bigint DEFAULT NULL COMMENT '负责人ID',
  `status` tinyint DEFAULT '1' COMMENT '状态: 0-停用 1-正常 2-维护中',
  `attributes` json DEFAULT NULL COMMENT '扩展属性值',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` int DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_space_code` (`space_code`),
  KEY `idx_type_code` (`type_code`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_path` (`path`(255)),
  KEY `idx_org_unit` (`org_unit_id`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='空间实例表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `spaces`
--

LOCK TABLES `spaces` WRITE;
/*!40000 ALTER TABLE `spaces` DISABLE KEYS */;
/*!40000 ALTER TABLE `spaces` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `spaces_v10`
--

DROP TABLE IF EXISTS `spaces_v10`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `spaces_v10` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `space_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '空间编码（自动生成）',
  `space_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '空间名称',
  `space_level` enum('CAMPUS','BUILDING','FLOOR','ROOM') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '空间层级',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID（楼栋和房间需要）',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
  `parent_id` bigint DEFAULT NULL COMMENT '父级空间ID',
  `path` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '物化路径，如 /1/2/3/',
  `level` int DEFAULT '0' COMMENT '层级深度（0=校区，1=楼，2=层，3=房间）',
  `address` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '地址',
  `building_no` int DEFAULT NULL COMMENT '楼栋编号（数字）',
  `floor_count` int DEFAULT NULL COMMENT '楼层数',
  `floor_number` int DEFAULT NULL COMMENT '楼层号（数字，负数为地下层）',
  `room_no` int DEFAULT NULL COMMENT '房间编号（数字）',
  `capacity` int DEFAULT NULL COMMENT '容量（床位数/座位数）',
  `current_occupancy` int DEFAULT '0' COMMENT '当前占用数',
  `gender_type` tinyint DEFAULT '0' COMMENT '性别限制：0不限/1男/2女',
  `org_unit_id` bigint DEFAULT NULL COMMENT '归属组织单元ID',
  `class_id` bigint DEFAULT NULL COMMENT '归属班级ID（宿舍分配）',
  `responsible_user_id` bigint DEFAULT NULL COMMENT '负责人ID',
  `status` tinyint DEFAULT '1' COMMENT '状态：0停用/1正常/2维护中',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` int DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_space_code` (`space_code`),
  KEY `idx_space_level` (`space_level`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_path` (`path`(255)),
  KEY `idx_building_no` (`building_no`),
  KEY `idx_floor_number` (`floor_number`),
  KEY `idx_room_no` (`room_no`),
  KEY `idx_org_unit_id` (`org_unit_id`),
  KEY `idx_class_id` (`class_id`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`),
  CONSTRAINT `fk_space_category` FOREIGN KEY (`category_id`) REFERENCES `place_categories` (`id`),
  CONSTRAINT `fk_space_parent` FOREIGN KEY (`parent_id`) REFERENCES `spaces_v10` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='空间表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `spaces_v10`
--

LOCK TABLES `spaces_v10` WRITE;
/*!40000 ALTER TABLE `spaces_v10` DISABLE KEYS */;
/*!40000 ALTER TABLE `spaces_v10` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `student_dormitory_assignments`
--

DROP TABLE IF EXISTS `student_dormitory_assignments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_dormitory_assignments` (
  `id` bigint NOT NULL COMMENT 'Primary Key',
  `student_id` bigint NOT NULL COMMENT 'Student ID',
  `dormitory_id` bigint NOT NULL COMMENT 'Dormitory ID',
  `bed_number` int NOT NULL COMMENT 'Bed Number',
  `check_in_date` date NOT NULL COMMENT 'Check In Date',
  `check_out_date` date DEFAULT NULL COMMENT 'Check Out Date',
  `status` tinyint DEFAULT '1' COMMENT 'Status: 1-Active, 0-Inactive',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Remark',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated At',
  `deleted` int DEFAULT '0' COMMENT 'Logical Delete Flag',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_active_assignment` (`student_id`,`status`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_dormitory_id` (`dormitory_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Student Dormitory Assignments Table';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_dormitory_assignments`
--

LOCK TABLES `student_dormitory_assignments` WRITE;
/*!40000 ALTER TABLE `student_dormitory_assignments` DISABLE KEYS */;
/*!40000 ALTER TABLE `student_dormitory_assignments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `student_grade_items`
--

DROP TABLE IF EXISTS `student_grade_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_grade_items` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `grade_id` bigint NOT NULL COMMENT '学生成绩ID',
  `composition_id` bigint NOT NULL COMMENT '成绩组成配置ID',
  `item_name` varchar(50) DEFAULT NULL COMMENT '成绩项名称',
  `score` decimal(5,1) DEFAULT NULL COMMENT '得分',
  `full_score` decimal(5,1) DEFAULT NULL COMMENT '满分',
  `weight` decimal(5,2) DEFAULT NULL COMMENT '权重',
  `weighted_score` decimal(5,2) DEFAULT NULL COMMENT '加权得分',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `input_time` datetime DEFAULT NULL COMMENT '录入时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_grade` (`grade_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成绩明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_grade_items`
--

LOCK TABLES `student_grade_items` WRITE;
/*!40000 ALTER TABLE `student_grade_items` DISABLE KEYS */;
/*!40000 ALTER TABLE `student_grade_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `student_grades`
--

DROP TABLE IF EXISTS `student_grades`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_grades` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `batch_id` bigint DEFAULT NULL COMMENT '录入批次ID',
  `semester_id` bigint NOT NULL COMMENT '学期ID',
  `task_id` bigint NOT NULL COMMENT '教学任务ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `org_unit_id` bigint DEFAULT NULL,
  `grade_scale_type` tinyint DEFAULT '1' COMMENT '评分制：1百分制 2五级制 3二级制',
  `regular_score` decimal(5,1) DEFAULT NULL COMMENT '平时成绩',
  `midterm_score` decimal(5,1) DEFAULT NULL COMMENT '期中成绩',
  `final_score` decimal(5,1) DEFAULT NULL COMMENT '期末成绩',
  `experiment_score` decimal(5,1) DEFAULT NULL COMMENT '实验成绩',
  `total_score` decimal(5,1) DEFAULT NULL COMMENT '总评成绩',
  `grade_level` varchar(10) DEFAULT NULL COMMENT '等级（五级制：A/B/C/D/F）',
  `grade_point` decimal(3,1) DEFAULT NULL COMMENT '绩点',
  `passed` tinyint DEFAULT NULL COMMENT '是否通过：1是 0否',
  `credits_earned` decimal(4,1) DEFAULT NULL COMMENT '获得学分',
  `grade_status` tinyint DEFAULT '0' COMMENT '状态：0未录入 1已录入 2已确认 3已发布 4有异议',
  `is_makeup` tinyint DEFAULT '0' COMMENT '是否补考成绩',
  `is_retake` tinyint DEFAULT '0' COMMENT '是否重修成绩',
  `makeup_count` int DEFAULT '0' COMMENT '补考次数',
  `input_teacher_id` bigint DEFAULT NULL COMMENT '录入教师ID',
  `input_time` datetime DEFAULT NULL COMMENT '录入时间',
  `confirm_time` datetime DEFAULT NULL COMMENT '确认时间',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_student` (`task_id`,`student_id`),
  KEY `idx_semester` (`semester_id`),
  KEY `idx_student` (`student_id`),
  KEY `idx_course` (`course_id`),
  KEY `idx_class` (`org_unit_id`),
  KEY `idx_status` (`grade_status`),
  KEY `idx_tenant` (`tenant_id`),
  CONSTRAINT `fk_grade_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`),
  CONSTRAINT `fk_grade_task` FOREIGN KEY (`task_id`) REFERENCES `teaching_tasks` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生成绩表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_grades`
--

LOCK TABLES `student_grades` WRITE;
/*!40000 ALTER TABLE `student_grades` DISABLE KEYS */;
/*!40000 ALTER TABLE `student_grades` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `student_relationship_snapshots`
--

DROP TABLE IF EXISTS `student_relationship_snapshots`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_relationship_snapshots` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `snapshot_date` date NOT NULL COMMENT 'Snapshot Date',
  `snapshot_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DAILY' COMMENT 'Type (DAILY/SEMESTER_START/SEMESTER_END)',
  `student_id` bigint NOT NULL COMMENT 'Student ID',
  `student_no` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Student Number',
  `student_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Student Name',
  `class_id` bigint DEFAULT NULL COMMENT 'Class ID',
  `class_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Class Name',
  `grade_id` bigint DEFAULT NULL COMMENT 'Grade ID',
  `grade_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Grade Name',
  `department_id` bigint DEFAULT NULL COMMENT 'Department ID',
  `department_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Department Name',
  `dormitory_id` bigint DEFAULT NULL COMMENT 'Dormitory ID',
  `building_id` bigint DEFAULT NULL COMMENT 'Building ID',
  `building_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Building Name',
  `dormitory_no` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Dormitory Number',
  `bed_no` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Bed Number',
  `is_dorm_leader` tinyint DEFAULT '0' COMMENT 'Is Dorm Leader (0/1)',
  `student_status` tinyint DEFAULT NULL COMMENT 'Status (1=Active,2=Suspended,3=Withdrawn,4=Graduated)',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_date_student` (`snapshot_date`,`student_id`),
  KEY `idx_snapshot_date` (`snapshot_date`),
  KEY `idx_student` (`student_id`),
  KEY `idx_class` (`class_id`),
  KEY `idx_dormitory` (`dormitory_id`),
  KEY `idx_student_rel_date` (`snapshot_date`,`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Student Relationship Snapshots';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_relationship_snapshots`
--

LOCK TABLES `student_relationship_snapshots` WRITE;
/*!40000 ALTER TABLE `student_relationship_snapshots` DISABLE KEYS */;
/*!40000 ALTER TABLE `student_relationship_snapshots` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `student_status_changes`
--

DROP TABLE IF EXISTS `student_status_changes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_status_changes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `student_no` varchar(50) DEFAULT NULL COMMENT '学号',
  `student_name` varchar(50) DEFAULT NULL COMMENT '学生姓名',
  `change_type` varchar(30) NOT NULL COMMENT '异动类型: ENROLL/SUSPEND/RESUME/GRADUATE/WITHDRAW/EXPEL/TRANSFER_CLASS/TRANSFER_MAJOR',
  `from_status` varchar(20) DEFAULT NULL COMMENT '原状态',
  `to_status` varchar(20) DEFAULT NULL COMMENT '新状态',
  `from_class_id` bigint DEFAULT NULL COMMENT '原班级ID（转班时）',
  `from_class_name` varchar(100) DEFAULT NULL COMMENT '原班级名',
  `to_class_id` bigint DEFAULT NULL COMMENT '新班级ID（转班时）',
  `to_class_name` varchar(100) DEFAULT NULL COMMENT '新班级名',
  `reason` varchar(500) DEFAULT NULL COMMENT '异动原因',
  `attachment_urls` json DEFAULT NULL COMMENT '附件URL',
  `effective_date` date DEFAULT NULL COMMENT '生效日期',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) DEFAULT NULL COMMENT '操作人姓名',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_student` (`student_id`),
  KEY `idx_type` (`change_type`),
  KEY `idx_date` (`effective_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生学籍异动记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_status_changes`
--

LOCK TABLES `student_status_changes` WRITE;
/*!40000 ALTER TABLE `student_status_changes` DISABLE KEYS */;
/*!40000 ALTER TABLE `student_status_changes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `system_configs`
--

DROP TABLE IF EXISTS `system_configs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_configs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置键',
  `config_value` text COLLATE utf8mb4_unicode_ci COMMENT '配置值',
  `config_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'STRING' COMMENT '值类型:STRING,NUMBER,BOOLEAN,JSON',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '配置说明',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `config_group` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `config_label` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `config_desc` text COLLATE utf8mb4_unicode_ci,
  `is_system` int DEFAULT NULL,
  `sort_order` int DEFAULT NULL,
  `status` int DEFAULT NULL,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `system_configs`
--

LOCK TABLES `system_configs` WRITE;
/*!40000 ALTER TABLE `system_configs` DISABLE KEYS */;
/*!40000 ALTER TABLE `system_configs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `system_messages`
--

DROP TABLE IF EXISTS `system_messages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_messages` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `message_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息类型: TASK_ASSIGN-任务分配, TASK_REMIND-任务提醒, TASK_APPROVE-审批通知, SYSTEM-系统通知',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息标题',
  `content` text COLLATE utf8mb4_unicode_ci COMMENT '消息内容',
  `sender_id` bigint DEFAULT NULL COMMENT '发送人ID(系统消息为空)',
  `sender_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '发送人姓名',
  `receiver_id` bigint NOT NULL COMMENT '接收人ID',
  `receiver_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '接收人姓名',
  `is_read` tinyint DEFAULT '0' COMMENT '是否已读: 0-未读, 1-已读',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `business_type` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业务类型: TASK-任务',
  `business_id` bigint DEFAULT NULL COMMENT '业务ID',
  `extra_data` json DEFAULT NULL COMMENT '额外数据',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` int DEFAULT '0' COMMENT '逻辑删除',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_receiver_id` (`receiver_id`),
  KEY `idx_message_type` (`message_type`),
  KEY `idx_is_read` (`is_read`),
  KEY `idx_business` (`business_type`,`business_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内消息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `system_messages`
--

LOCK TABLES `system_messages` WRITE;
/*!40000 ALTER TABLE `system_messages` DISABLE KEYS */;
/*!40000 ALTER TABLE `system_messages` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `system_modules`
--

DROP TABLE IF EXISTS `system_modules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_modules` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `module_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模块编码',
  `module_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模块名称',
  `module_desc` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '模块描述',
  `parent_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '父模块编码',
  `icon` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图标',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `status` int DEFAULT '1' COMMENT '状态:0禁用,1启用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_module_code` (`module_code`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统模块表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `system_modules`
--

LOCK TABLES `system_modules` WRITE;
/*!40000 ALTER TABLE `system_modules` DISABLE KEYS */;
INSERT INTO `system_modules` VALUES (0,'organization','组织管理','组织架构与人员管理',NULL,NULL,1,1,'2026-06-01 22:57:16','2026-06-01 22:57:16',1);
/*!40000 ALTER TABLE `system_modules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `task_approval_configs`
--

DROP TABLE IF EXISTS `task_approval_configs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_approval_configs` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `org_unit_id` bigint NOT NULL COMMENT '组织单元ID',
  `department_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '部门名称',
  `approval_level` tinyint NOT NULL COMMENT '审批级别',
  `approver_id` bigint NOT NULL COMMENT '审批人ID',
  `approver_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '审批人姓名',
  `approver_role` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批人角色',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_dept_level` (`task_id`,`org_unit_id`,`approval_level`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_department_id` (`org_unit_id`),
  KEY `idx_approver_id` (`approver_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务审批配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `task_approval_configs`
--

LOCK TABLES `task_approval_configs` WRITE;
/*!40000 ALTER TABLE `task_approval_configs` DISABLE KEYS */;
/*!40000 ALTER TABLE `task_approval_configs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `task_approval_records`
--

DROP TABLE IF EXISTS `task_approval_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_approval_records` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `submission_id` bigint NOT NULL COMMENT '提交记录ID',
  `process_instance_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Flowable流程实例ID',
  `task_definition_key` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Flowable任务定义Key',
  `flowable_task_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Flowable任务ID',
  `node_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批节点名称',
  `node_order` int DEFAULT '1' COMMENT '审批顺序(第几级)',
  `approver_id` bigint DEFAULT NULL COMMENT '审批人ID',
  `approver_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批人姓名',
  `approver_role` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批人角色',
  `approval_status` tinyint DEFAULT '0' COMMENT '审批状态: 0-待审批, 1-通过, 2-打回, 3-转交',
  `approval_comment` text COLLATE utf8mb4_unicode_ci COMMENT '审批意见',
  `approval_time` datetime DEFAULT NULL COMMENT '审批时间',
  `reject_to_node` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '打回到的节点',
  `reject_reason` text COLLATE utf8mb4_unicode_ci COMMENT '打回原因',
  `transfer_to_id` bigint DEFAULT NULL COMMENT '转交给谁',
  `transfer_to_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '转交人姓名',
  `transfer_reason` text COLLATE utf8mb4_unicode_ci COMMENT '转交原因',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int DEFAULT '0' COMMENT '逻辑删除',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_submission_id` (`submission_id`),
  KEY `idx_approver_id` (`approver_id`),
  KEY `idx_approval_status` (`approval_status`),
  KEY `idx_process_instance_id` (`process_instance_id`),
  KEY `idx_flowable_task_id` (`flowable_task_id`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务审批记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `task_approval_records`
--

LOCK TABLES `task_approval_records` WRITE;
/*!40000 ALTER TABLE `task_approval_records` DISABLE KEYS */;
/*!40000 ALTER TABLE `task_approval_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `task_assignees`
--

DROP TABLE IF EXISTS `task_assignees`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_assignees` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `assignee_id` bigint NOT NULL COMMENT '执行人ID',
  `assignee_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '执行人姓名',
  `status` tinyint DEFAULT '0' COMMENT '状态: 0-待接收, 1-进行中, 2-待审核, 3-已完成, 4-已打回',
  `accepted_at` datetime DEFAULT NULL COMMENT '接收时间',
  `submitted_at` datetime DEFAULT NULL COMMENT '提交时间',
  `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
  `process_instance_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '该执行人的流程实例ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int DEFAULT '0' COMMENT '逻辑删除',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_assignee` (`task_id`,`assignee_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_assignee_id` (`assignee_id`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务执行人表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `task_assignees`
--

LOCK TABLES `task_assignees` WRITE;
/*!40000 ALTER TABLE `task_assignees` DISABLE KEYS */;
/*!40000 ALTER TABLE `task_assignees` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `task_logs`
--

DROP TABLE IF EXISTS `task_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_logs` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `operator_id` bigint NOT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作人姓名',
  `action` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作类型: CREATE/ASSIGN/ACCEPT/SUBMIT/APPROVE/REJECT/CANCEL/TRANSFER',
  `action_desc` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作描述',
  `old_status` tinyint DEFAULT NULL COMMENT '操作前状态',
  `new_status` tinyint DEFAULT NULL COMMENT '操作后状态',
  `remark` text COLLATE utf8mb4_unicode_ci COMMENT '备注',
  `extra_data` json DEFAULT NULL COMMENT '额外数据',
  `ip_address` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'IP地址',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_action` (`action`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务操作日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `task_logs`
--

LOCK TABLES `task_logs` WRITE;
/*!40000 ALTER TABLE `task_logs` DISABLE KEYS */;
/*!40000 ALTER TABLE `task_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `task_submissions`
--

DROP TABLE IF EXISTS `task_submissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_submissions` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `task_assignee_id` bigint DEFAULT NULL COMMENT '任务执行人记录ID',
  `submitter_id` bigint NOT NULL COMMENT '提交人ID',
  `submitter_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '提交人姓名',
  `content` text COLLATE utf8mb4_unicode_ci COMMENT '完成情况说明',
  `attachment_ids` json DEFAULT NULL COMMENT '附件ID列表(文件/照片)',
  `submitted_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `review_status` tinyint DEFAULT '0' COMMENT '审核状态: 0-待审核, 1-审核中, 2-通过, 3-打回',
  `final_reviewer_id` bigint DEFAULT NULL COMMENT '最终审核人ID',
  `final_reviewer_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最终审核人姓名',
  `final_review_comment` text COLLATE utf8mb4_unicode_ci COMMENT '最终审核意见',
  `final_reviewed_at` datetime DEFAULT NULL COMMENT '最终审核时间',
  `reject_count` int DEFAULT '0' COMMENT '被打回次数',
  `reject_to_node` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '打回到的节点',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int DEFAULT '0' COMMENT '逻辑删除',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_submitter_id` (`submitter_id`),
  KEY `idx_review_status` (`review_status`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务提交记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `task_submissions`
--

LOCK TABLES `task_submissions` WRITE;
/*!40000 ALTER TABLE `task_submissions` DISABLE KEYS */;
/*!40000 ALTER TABLE `task_submissions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tasks`
--

DROP TABLE IF EXISTS `tasks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tasks` (
  `id` bigint NOT NULL COMMENT '任务ID',
  `task_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务编号(如: TASK-20251227-0001)',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务标题',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '任务描述',
  `priority` tinyint DEFAULT '2' COMMENT '优先级: 1-紧急, 2-普通, 3-低',
  `status` tinyint DEFAULT '0' COMMENT '状态: 0-待接收, 1-进行中, 2-待审核, 3-已完成, 4-已打回, 5-已取消, 6-审批中',
  `assigner_id` bigint NOT NULL COMMENT '分配人ID(创建任务的领导)',
  `assigner_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分配人姓名',
  `assign_type` tinyint DEFAULT '1' COMMENT '分配类型: 1-指定个人, 2-批量分配',
  `assignee_id` bigint DEFAULT NULL COMMENT '执行人ID(班主任)',
  `assignee_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '执行人姓名',
  `department_id` bigint DEFAULT NULL COMMENT '部门ID(批量分配时)',
  `department_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '部门名称',
  `target_ids` json DEFAULT NULL COMMENT '目标ID列表(批量分配时的执行人ID数组)',
  `due_date` datetime DEFAULT NULL COMMENT '截止时间',
  `accepted_at` datetime DEFAULT NULL COMMENT '接收时间',
  `submitted_at` datetime DEFAULT NULL COMMENT '提交时间',
  `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
  `workflow_template_id` bigint DEFAULT NULL COMMENT '使用的流程模板ID',
  `process_instance_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Flowable流程实例ID',
  `current_node` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '当前审批节点',
  `current_approvers` json DEFAULT NULL COMMENT '当前待审批人ID列表',
  `attachment_ids` json DEFAULT NULL COMMENT '任务附件ID列表',
  `version` int DEFAULT '0' COMMENT 'Optimistic Lock Version',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int DEFAULT '0' COMMENT '逻辑删除: 0-未删除, 1-已删除',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_code` (`task_code`),
  KEY `idx_assigner_id` (`assigner_id`),
  KEY `idx_assignee_id` (`assignee_id`),
  KEY `idx_department_id` (`department_id`),
  KEY `idx_status` (`status`),
  KEY `idx_priority` (`priority`),
  KEY `idx_due_date` (`due_date`),
  KEY `idx_process_instance_id` (`process_instance_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tasks`
--

LOCK TABLES `tasks` WRITE;
/*!40000 ALTER TABLE `tasks` DISABLE KEYS */;
/*!40000 ALTER TABLE `tasks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teacher_assignments`
--

DROP TABLE IF EXISTS `teacher_assignments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teacher_assignments` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `org_unit_id` bigint DEFAULT NULL,
  `teacher_id` bigint NOT NULL COMMENT '教师ID',
  `teacher_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '教师姓名',
  `role_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色类型:HEAD_TEACHER,DEPUTY_HEAD_TEACHER,SUBJECT_TEACHER',
  `subject_id` bigint DEFAULT NULL COMMENT '科目ID(科任教师)',
  `start_date` date NOT NULL COMMENT '任职开始日期',
  `end_date` date DEFAULT NULL COMMENT '任职结束日期',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVE' COMMENT '状态:ACTIVE,TRANSFERRED,RESIGNED,EXPIRED',
  `transfer_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '变更原因',
  `handover_teacher_id` bigint DEFAULT NULL COMMENT '交接教师ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `role` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_current` tinyint(1) DEFAULT NULL,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_class_id` (`org_unit_id`),
  KEY `idx_teacher_id` (`teacher_id`),
  KEY `idx_role_type` (`role_type`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师任职表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teacher_assignments`
--

LOCK TABLES `teacher_assignments` WRITE;
/*!40000 ALTER TABLE `teacher_assignments` DISABLE KEYS */;
/*!40000 ALTER TABLE `teacher_assignments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teacher_course_qualifications`
--

DROP TABLE IF EXISTS `teacher_course_qualifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teacher_course_qualifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `teacher_profile_id` bigint NOT NULL COMMENT '教师档案ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `qualification_level` tinyint DEFAULT '1' COMMENT '资质等级：1初级 2中级 3高级',
  `remark` varchar(200) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_teacher_course` (`teacher_profile_id`,`course_id`),
  KEY `idx_course` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教师可授课程表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teacher_course_qualifications`
--

LOCK TABLES `teacher_course_qualifications` WRITE;
/*!40000 ALTER TABLE `teacher_course_qualifications` DISABLE KEYS */;
/*!40000 ALTER TABLE `teacher_course_qualifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teacher_preferences`
--

DROP TABLE IF EXISTS `teacher_preferences`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teacher_preferences` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `teacher_id` bigint NOT NULL COMMENT '教师ID',
  `semester_id` bigint NOT NULL COMMENT '学期ID',
  `preference_type` tinyint NOT NULL COMMENT '偏好类型：1不可用时间 2偏好时间 3偏好教室',
  `weekday` tinyint DEFAULT NULL COMMENT '周几（1-7）',
  `time_slot` int DEFAULT NULL COMMENT '节次',
  `classroom_id` bigint DEFAULT NULL COMMENT '偏好教室ID',
  `priority` int DEFAULT '0' COMMENT '优先级（数字越大越优先）',
  `reason` varchar(200) DEFAULT NULL COMMENT '原因说明',
  `status` tinyint DEFAULT '1' COMMENT '状态',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_teacher_semester` (`teacher_id`,`semester_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教师偏好设置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teacher_preferences`
--

LOCK TABLES `teacher_preferences` WRITE;
/*!40000 ALTER TABLE `teacher_preferences` DISABLE KEYS */;
/*!40000 ALTER TABLE `teacher_preferences` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teaching_class_members`
--

DROP TABLE IF EXISTS `teaching_class_members`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teaching_class_members` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `teaching_class_id` bigint NOT NULL COMMENT '教学班ID',
  `member_type` tinyint NOT NULL COMMENT '1整班 2个人',
  `admin_class_id` bigint DEFAULT NULL COMMENT '行政班ID',
  `student_id` bigint DEFAULT NULL COMMENT '学生ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_teaching_class` (`teaching_class_id`),
  KEY `idx_admin_class` (`admin_class_id`),
  KEY `idx_tenant` (`tenant_id`),
  CONSTRAINT `fk_member_class` FOREIGN KEY (`teaching_class_id`) REFERENCES `teaching_classes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教学班成员表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teaching_class_members`
--

LOCK TABLES `teaching_class_members` WRITE;
/*!40000 ALTER TABLE `teaching_class_members` DISABLE KEYS */;
/*!40000 ALTER TABLE `teaching_class_members` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teaching_classes`
--

DROP TABLE IF EXISTS `teaching_classes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teaching_classes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `semester_id` bigint NOT NULL COMMENT '学期ID',
  `class_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '教学班名称',
  `class_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '教学班编号',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `class_type` tinyint NOT NULL DEFAULT '1' COMMENT '1普通 2合堂 3走班',
  `weekly_hours` int NOT NULL COMMENT '周课时数',
  `student_count` int NOT NULL DEFAULT '0' COMMENT '学生数',
  `required_room_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '教室类型要求',
  `required_capacity` int DEFAULT NULL COMMENT '教室容量要求',
  `start_week` int NOT NULL DEFAULT '1' COMMENT '起始周',
  `end_week` int DEFAULT NULL COMMENT '结束周',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1有效 0无效',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_semester` (`semester_id`),
  KEY `idx_course` (`course_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教学班表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teaching_classes`
--

LOCK TABLES `teaching_classes` WRITE;
/*!40000 ALTER TABLE `teaching_classes` DISABLE KEYS */;
/*!40000 ALTER TABLE `teaching_classes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teaching_progress`
--

DROP TABLE IF EXISTS `teaching_progress`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teaching_progress` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `task_id` bigint NOT NULL COMMENT '教学任务ID',
  `entry_id` bigint DEFAULT NULL COMMENT '排课条目ID(可选)',
  `semester_id` bigint NOT NULL COMMENT '学期ID',
  `org_unit_id` bigint DEFAULT NULL COMMENT '归属组织(数据权限)',
  `week_number` int NOT NULL COMMENT '教学周次',
  `lesson_no` int NOT NULL DEFAULT '1' COMMENT '本周第几节(从1起)',
  `planned_topic` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '计划讲授章节/主题',
  `actual_topic` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '实际讲授章节/主题',
  `chapter` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '章节编号',
  `progress_status` tinyint NOT NULL DEFAULT '0' COMMENT '0待授课 1已完成 2未完成 3调课',
  `attendance_count` int DEFAULT NULL COMMENT '实到人数',
  `total_students` int DEFAULT NULL COMMENT '应到人数',
  `note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注/学情记录',
  `recorded_at` datetime DEFAULT NULL COMMENT '授课实际记录时间',
  `recorded_by` bigint DEFAULT NULL COMMENT '记录人(教师)',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_week_lesson` (`task_id`,`week_number`,`lesson_no`),
  KEY `idx_semester_org` (`semester_id`,`org_unit_id`),
  KEY `idx_task` (`task_id`),
  KEY `idx_recorded_by` (`recorded_by`),
  KEY `idx_status` (`progress_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教学进度跟踪表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teaching_progress`
--

LOCK TABLES `teaching_progress` WRITE;
/*!40000 ALTER TABLE `teaching_progress` DISABLE KEYS */;
/*!40000 ALTER TABLE `teaching_progress` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teaching_task_teachers`
--

DROP TABLE IF EXISTS `teaching_task_teachers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teaching_task_teachers` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `task_id` bigint NOT NULL COMMENT '教学任务ID',
  `teacher_id` bigint NOT NULL COMMENT '教师ID',
  `teacher_role` tinyint DEFAULT '1' COMMENT '教师角色：1主讲 2辅讲 3助教',
  `weekly_hours` int DEFAULT NULL COMMENT '该教师承担的周课时数',
  `workload_ratio` decimal(3,2) DEFAULT '1.00' COMMENT '工作量比例',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_teacher` (`task_id`,`teacher_id`),
  KEY `idx_teacher` (`teacher_id`),
  KEY `idx_tenant` (`tenant_id`),
  CONSTRAINT `fk_taskteacher_task` FOREIGN KEY (`task_id`) REFERENCES `teaching_tasks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教学任务教师分配表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teaching_task_teachers`
--

LOCK TABLES `teaching_task_teachers` WRITE;
/*!40000 ALTER TABLE `teaching_task_teachers` DISABLE KEYS */;
/*!40000 ALTER TABLE `teaching_task_teachers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teaching_tasks`
--

DROP TABLE IF EXISTS `teaching_tasks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teaching_tasks` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `task_code` varchar(50) NOT NULL COMMENT '任务编号',
  `semester_id` bigint NOT NULL COMMENT '学期ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `org_unit_id` bigint DEFAULT NULL COMMENT '开课部门ID',
  `student_count` int DEFAULT '0' COMMENT '学生人数',
  `weekly_hours` int NOT NULL COMMENT '周学时',
  `total_hours` int NOT NULL COMMENT '总学时',
  `start_week` int DEFAULT '1' COMMENT '起始周',
  `end_week` int DEFAULT '16' COMMENT '结束周',
  `room_type_required` varchar(50) DEFAULT NULL COMMENT '需要的教室类型(关联场所type_code)',
  `consecutive_periods` int DEFAULT '2' COMMENT '连排节数(1=不连排,2=2节连排,3=3节连排,4=4节连排)',
  `course_nature` tinyint DEFAULT '1' COMMENT '课程性质(1=理论,2=实验,3=实践,4=理论+实验)',
  `scheduling_status` tinyint DEFAULT '0' COMMENT '排课状态：0未排 1部分排 2已排完',
  `task_status` tinyint DEFAULT '0' COMMENT '0=待落实 1=已分配教师 2=已排课 3=进行中 4=已结束 9=已取消',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  `offering_id` bigint DEFAULT NULL COMMENT '关联开课计划ID',
  `teaching_class_id` bigint DEFAULT NULL COMMENT '关联教学班ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_code` (`task_code`),
  KEY `idx_semester` (`semester_id`),
  KEY `idx_course` (`course_id`),
  KEY `idx_offering` (`offering_id`),
  KEY `idx_teaching_class` (`teaching_class_id`),
  KEY `idx_org_unit_semester` (`org_unit_id`,`semester_id`),
  KEY `idx_tenant` (`tenant_id`),
  CONSTRAINT `fk_task_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`),
  CONSTRAINT `fk_task_semester` FOREIGN KEY (`semester_id`) REFERENCES `semesters` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教学任务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teaching_tasks`
--

LOCK TABLES `teaching_tasks` WRITE;
/*!40000 ALTER TABLE `teaching_tasks` DISABLE KEYS */;
/*!40000 ALTER TABLE `teaching_tasks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teaching_weeks`
--

DROP TABLE IF EXISTS `teaching_weeks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teaching_weeks` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `semester_id` bigint NOT NULL COMMENT '所属学期ID',
  `week_number` int NOT NULL COMMENT '周次（第几周）',
  `start_date` date NOT NULL COMMENT '本周开始日期（周一）',
  `end_date` date NOT NULL COMMENT '本周结束日期（周日）',
  `week_type` tinyint DEFAULT '1' COMMENT '周类型：1正常教学周 2考试周 3假期周 4实践周',
  `week_label` varchar(50) DEFAULT NULL COMMENT '周标签，如"国庆假期"',
  `is_active` tinyint DEFAULT '1' COMMENT '是否有效（停课则为0）',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_semester_week` (`semester_id`,`week_number`),
  KEY `idx_date_range` (`start_date`,`end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教学周表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teaching_weeks`
--

LOCK TABLES `teaching_weeks` WRITE;
/*!40000 ALTER TABLE `teaching_weeks` DISABLE KEYS */;
/*!40000 ALTER TABLE `teaching_weeks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tenant_plugin_enablement`
--

DROP TABLE IF EXISTS `tenant_plugin_enablement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant_plugin_enablement` (
  `tenant_id` bigint NOT NULL COMMENT '租户 ID',
  `plugin_code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '插件码, 对应 plugin_packages.industry_code',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '此租户是否启用 (1=启用/0=禁用)',
  `config_json` json DEFAULT NULL COMMENT '租户级插件配置 (如 EDU 插件的学校代码)',
  `enabled_by` bigint DEFAULT NULL COMMENT '启用操作者 user_id',
  `enabled_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '最近变更时间',
  `notes` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`tenant_id`,`plugin_code`),
  KEY `idx_plugin_code` (`plugin_code`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户级插件启用表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tenant_plugin_enablement`
--

LOCK TABLES `tenant_plugin_enablement` WRITE;
/*!40000 ALTER TABLE `tenant_plugin_enablement` DISABLE KEYS */;
/*!40000 ALTER TABLE `tenant_plugin_enablement` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tenants`
--

DROP TABLE IF EXISTS `tenants`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenants` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tenant_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `domain` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '绑定域名',
  `config` json DEFAULT NULL COMMENT '租户配置',
  `enabled` tinyint(1) DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `tenant_code` (`tenant_code`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tenants`
--

LOCK TABLES `tenants` WRITE;
/*!40000 ALTER TABLE `tenants` DISABLE KEYS */;
INSERT INTO `tenants` VALUES (1,'default','默认租户',NULL,NULL,1,'2026-06-01 22:57:07','2026-06-01 22:57:07');
/*!40000 ALTER TABLE `tenants` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `trigger_points`
--

DROP TABLE IF EXISTS `trigger_points`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trigger_points` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `module_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模块编码',
  `module_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模块名称',
  `point_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '触发点编码(全局唯一)',
  `point_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '触发点名称',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '说明',
  `context_schema` json DEFAULT NULL COMMENT '上下文字段定义',
  `is_enabled` tinyint DEFAULT '1',
  `sort_order` int DEFAULT '0',
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  `plugin_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '插件级启用状态',
  `industry` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属行业包',
  `plugin_class` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '声明插件全限定类名',
  `origin` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '统一来源',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_point_code` (`point_code`,`tenant_id`),
  KEY `idx_module` (`module_code`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=63 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='触发点注册表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `trigger_points`
--

LOCK TABLES `trigger_points` WRITE;
/*!40000 ALTER TABLE `trigger_points` DISABLE KEYS */;
INSERT INTO `trigger_points` VALUES (1,'inspection','检查','INSP_ITEM_RESULT','单项检查结果','单个检查项评分完成','{\"score\": \"BigDecimal\", \"itemId\": \"Long\", \"passed\": \"Boolean\", \"targetId\": \"Long\", \"submissionId\": \"Long\"}',1,0,1,'2026-06-01 22:56:48','2026-06-01 23:32:05',0,1,'CORE','com.school.management.infrastructure.extension.plugins.core.messaging.InspectionMessagingPlugin','PLUGIN:CORE@1.0.0'),(2,'inspection','检查','INSP_GRADE_RESULT','等级评定结果','按分数段评级完成','{\"gradeLevel\": \"String\", \"totalScore\": \"BigDecimal\", \"submissionId\": \"Long\"}',1,0,1,'2026-06-01 22:56:48','2026-06-01 23:32:05',0,1,'CORE','com.school.management.infrastructure.extension.plugins.core.messaging.InspectionMessagingPlugin','PLUGIN:CORE@1.0.0'),(3,'inspection','检查','INSP_RECORD_COMPLETE','检查记录完成','整条检查提交完成,可触发汇总/整改','{\"taskId\": \"Long\", \"projectId\": \"Long\", \"submissionId\": \"Long\"}',1,0,1,'2026-06-01 22:56:48','2026-06-01 23:32:05',0,1,'CORE','com.school.management.infrastructure.extension.plugins.core.messaging.InspectionMessagingPlugin','PLUGIN:CORE@1.0.0'),(4,'attendance','考勤','ATTENDANCE_RECORDED','考勤记录','学生/员工考勤记录(到/迟/缺/请假)','{\"userId\": \"Long\", \"orgUnitId\": \"Long\", \"attendanceDate\": \"String\", \"attendanceType\": \"String\"}',1,0,1,'2026-06-01 22:56:48','2026-06-01 23:32:05',0,1,'EDU','com.school.management.infrastructure.extension.plugins.education.messaging.AttendanceMessagingPlugin','PLUGIN:EDU@1.0.0'),(5,'attendance','考勤管理','LEAVE_REQUEST_SUBMITTED','请假申请','学生提交请假申请','{\"endDate\": {\"type\": \"String\", \"label\": \"结束\"}, \"leaveType\": {\"type\": \"String\", \"label\": \"请假类型\"}, \"startDate\": {\"type\": \"String\", \"label\": \"开始\"}, \"studentId\": {\"role\": \"id\", \"type\": \"Long\", \"label\": \"学生ID\", \"subject\": \"USER\"}, \"studentName\": {\"role\": \"name\", \"type\": \"String\", \"label\": \"学生姓名\", \"subject\": \"USER\"}}',1,0,1,'2026-06-01 22:56:48','2026-06-01 22:56:48',0,1,NULL,NULL,NULL),(6,'dormitory','宿舍管理','DORM_CHECKIN','入住','学生入住宿舍','{\"placeId\": {\"role\": \"id\", \"type\": \"Long\", \"label\": \"房间ID\", \"subject\": \"PLACE\"}, \"placeName\": {\"role\": \"name\", \"type\": \"String\", \"label\": \"房间\", \"subject\": \"PLACE\"}, \"occupantId\": {\"role\": \"id\", \"type\": \"Long\", \"label\": \"入住人ID\", \"subject\": \"USER\"}, \"occupantName\": {\"role\": \"name\", \"type\": \"String\", \"label\": \"入住人\", \"subject\": \"USER\"}}',1,0,1,'2026-06-01 22:56:48','2026-06-01 22:56:48',0,1,NULL,NULL,NULL),(7,'dormitory','宿舍管理','DORM_CHECKOUT','退宿','学生退出宿舍','{\"reason\": {\"type\": \"String\", \"label\": \"原因\"}, \"placeId\": {\"role\": \"id\", \"type\": \"Long\", \"label\": \"房间ID\", \"subject\": \"PLACE\"}, \"placeName\": {\"role\": \"name\", \"type\": \"String\", \"label\": \"房间\", \"subject\": \"PLACE\"}, \"occupantId\": {\"role\": \"id\", \"type\": \"Long\", \"label\": \"退宿人ID\", \"subject\": \"USER\"}, \"occupantName\": {\"role\": \"name\", \"type\": \"String\", \"label\": \"退宿人\", \"subject\": \"USER\"}}',1,0,1,'2026-06-01 22:56:48','2026-06-01 22:56:48',0,1,NULL,NULL,NULL),(8,'organization','组织管理','ORG_MEMBER_JOIN','人员加入','人员加入组织','{\"userId\": {\"role\": \"id\", \"type\": \"Long\", \"label\": \"用户ID\", \"subject\": \"USER\"}, \"userName\": {\"role\": \"name\", \"type\": \"String\", \"label\": \"用户姓名\", \"subject\": \"USER\"}, \"orgUnitId\": {\"role\": \"id\", \"type\": \"Long\", \"label\": \"组织ID\", \"subject\": \"ORG_UNIT\"}, \"orgUnitName\": {\"role\": \"name\", \"type\": \"String\", \"label\": \"组织名称\", \"subject\": \"ORG_UNIT\"}}',1,0,1,'2026-06-01 22:56:48','2026-06-01 22:56:48',0,1,NULL,NULL,NULL),(9,'organization','组织管理','ORG_MEMBER_LEAVE','人员离开','人员离开组织','{\"reason\": {\"type\": \"String\", \"label\": \"原因\"}, \"userId\": {\"role\": \"id\", \"type\": \"Long\", \"label\": \"用户ID\", \"subject\": \"USER\"}, \"userName\": {\"role\": \"name\", \"type\": \"String\", \"label\": \"用户姓名\", \"subject\": \"USER\"}, \"orgUnitId\": {\"role\": \"id\", \"type\": \"Long\", \"label\": \"组织ID\", \"subject\": \"ORG_UNIT\"}, \"orgUnitName\": {\"role\": \"name\", \"type\": \"String\", \"label\": \"组织名称\", \"subject\": \"ORG_UNIT\"}}',1,0,1,'2026-06-01 22:56:48','2026-06-01 22:56:48',0,1,NULL,NULL,NULL),(10,'student','学生管理','STUDENT_STATUS_CHANGE','学籍变更','学生状态变更','{\"toStatus\": {\"type\": \"String\", \"label\": \"新状态\"}, \"studentId\": {\"role\": \"id\", \"type\": \"Long\", \"label\": \"学生ID\", \"subject\": \"USER\"}, \"changeType\": {\"type\": \"String\", \"label\": \"变更类型\"}, \"fromStatus\": {\"type\": \"String\", \"label\": \"原状态\"}, \"studentName\": {\"role\": \"name\", \"type\": \"String\", \"label\": \"学生姓名\", \"subject\": \"USER\"}}',1,0,1,'2026-06-01 22:56:48','2026-06-01 22:56:48',0,1,NULL,NULL,NULL),(11,'enrollment','招生与学籍','STUDENT_ENROLLED','学籍注册','学生学籍正式建立','{\"classId\": \"Long\", \"studentId\": \"Long\"}',1,0,1,'2026-06-01 22:56:48','2026-06-01 23:32:05',0,1,'EDU','com.school.management.infrastructure.extension.plugins.education.messaging.EnrollmentMessagingPlugin','PLUGIN:EDU@1.0.0'),(12,'enrollment','招生与学籍','ENROLLMENT_ADMITTED','录取确认','招生录取确认','{\"classId\": \"Long\", \"applicantId\": \"Long\"}',1,0,1,'2026-06-01 22:56:48','2026-06-01 23:32:05',0,1,'EDU','com.school.management.infrastructure.extension.plugins.education.messaging.EnrollmentMessagingPlugin','PLUGIN:EDU@1.0.0'),(13,'enrollment','招生与学籍','ENROLLMENT_REGISTERED','新生报到','新生报到注册','{\"classId\": \"Long\", \"studentId\": \"Long\"}',1,0,1,'2026-06-01 22:56:48','2026-06-01 23:32:05',0,1,'EDU','com.school.management.infrastructure.extension.plugins.education.messaging.EnrollmentMessagingPlugin','PLUGIN:EDU@1.0.0'),(14,'teaching','教务','SCHEDULE_PUBLISHED','课程表发布','学期课程表对外发布','{\"orgUnitId\": \"Long\", \"scheduleId\": \"Long\", \"semesterId\": \"Long\"}',1,0,1,'2026-06-01 22:56:48','2026-06-01 23:32:05',0,1,'EDU','com.school.management.infrastructure.extension.plugins.education.messaging.TeachingMessagingPlugin','PLUGIN:EDU@1.0.0'),(15,'grade','成绩','GRADE_PUBLISHED','成绩公示(班级)','成绩对外公示,班级级事件','{\"batchId\": \"Long\", \"courseId\": \"Long\", \"batchName\": \"String\", \"orgUnitId\": \"Long\", \"semesterId\": \"Long\"}',1,0,1,'2026-06-01 22:56:48','2026-06-01 23:32:05',0,1,'EDU','com.school.management.infrastructure.extension.plugins.education.messaging.GradeMessagingPlugin','PLUGIN:EDU@1.0.0'),(16,'asset','资产管理','ASSET_CHECK_RESULT','资产巡检结果','资产巡检','{\"result\": {\"type\": \"String\", \"label\": \"结果\"}, \"assetId\": {\"type\": \"Long\", \"label\": \"资产ID\"}, \"placeId\": {\"role\": \"id\", \"type\": \"Long\", \"label\": \"场所ID\", \"subject\": \"PLACE\"}, \"assetName\": {\"type\": \"String\", \"label\": \"资产名称\"}, \"placeName\": {\"role\": \"name\", \"type\": \"String\", \"label\": \"场所名称\", \"subject\": \"PLACE\"}}',1,0,1,'2026-06-01 22:56:48','2026-06-01 22:56:48',0,1,NULL,NULL,NULL),(17,'asset','资产管理','ASSET_DAMAGE_FOUND','资产损坏','发现资产损坏','{\"assetId\": {\"type\": \"Long\", \"label\": \"资产ID\"}, \"assetName\": {\"type\": \"String\", \"label\": \"资产名称\"}, \"description\": {\"type\": \"String\", \"label\": \"损坏描述\"}}',1,0,1,'2026-06-01 22:56:48','2026-06-01 22:56:48',0,1,NULL,NULL,NULL),(18,'grade','成绩','GRADE_SUBMITTED','成绩录入','教师录入成绩后提交','{\"batchId\": \"Long\", \"courseId\": \"Long\", \"batchName\": \"String\", \"orgUnitId\": \"Long\", \"semesterId\": \"Long\"}',1,20,1,'2026-06-01 22:57:28','2026-06-01 23:32:05',0,1,'EDU','com.school.management.infrastructure.extension.plugins.education.messaging.GradeMessagingPlugin','PLUGIN:EDU@1.0.0'),(19,'grade','成绩','GRADE_APPROVED','成绩审核通过','教务审核通过成绩批次','{\"batchId\": \"Long\", \"courseId\": \"Long\", \"batchName\": \"String\", \"orgUnitId\": \"Long\", \"semesterId\": \"Long\"}',1,21,1,'2026-06-01 22:57:28','2026-06-01 23:32:05',0,1,'EDU','com.school.management.infrastructure.extension.plugins.education.messaging.GradeMessagingPlugin','PLUGIN:EDU@1.0.0'),(20,'teaching','教务','EXAM_PUBLISHED','考试发布','考试安排对外发布','{\"examId\": \"Long\", \"courseId\": \"Long\", \"examDate\": \"String\", \"examName\": \"String\"}',1,22,1,'2026-06-01 22:57:28','2026-06-01 23:32:05',0,1,'EDU','com.school.management.infrastructure.extension.plugins.education.messaging.TeachingMessagingPlugin','PLUGIN:EDU@1.0.0'),(21,'grade','成绩','GRADE_PUBLISHED_PERSONAL','成绩发放(个人)','成绩定向发送给学生本人及其家长','{\"batchId\": \"Long\", \"batchName\": \"String\", \"studentId\": \"Long\", \"studentName\": \"String\"}',1,0,1,'2026-06-01 22:57:33','2026-06-01 23:32:05',0,1,'EDU','com.school.management.infrastructure.extension.plugins.education.messaging.GradeMessagingPlugin','PLUGIN:EDU@1.0.0'),(22,'policy','Policy 违规','POLICY_WARNING','Policy 违规事件','Policy SPI 产生的 WARN/INFO 级违规, 业务规则软约束触发, 可配置订阅通知','{\"phase\": {\"type\": \"String\", \"label\": \"阶段\"}, \"message\": {\"type\": \"String\", \"label\": \"描述\"}, \"severity\": {\"type\": \"String\", \"label\": \"严重度\"}, \"entityType\": {\"type\": \"String\", \"label\": \"实体类型\"}, \"policyCode\": {\"type\": \"String\", \"label\": \"策略码\"}, \"subjectType\": {\"type\": \"String\", \"label\": \"主体类型\"}}',1,0,1,'2026-06-01 22:57:34','2026-06-01 22:57:34',0,1,NULL,NULL,NULL),(23,'inspection','检查平台','INSP_CORRECTIVE_CREATED','整改单创建','整改案例创建时触发, 通知当事人 (subject) 有新整改要求.','{\"caseId\": {\"type\": \"Long\", \"label\": \"整改单ID\"}, \"caseCode\": {\"type\": \"String\", \"label\": \"整改单号\"}, \"deadline\": {\"type\": \"String\", \"label\": \"整改期限\"}, \"priority\": {\"type\": \"String\", \"label\": \"优先级\"}, \"subjectId\": {\"type\": \"Long\", \"label\": \"主体ID\"}, \"subjectName\": {\"type\": \"String\", \"label\": \"主体名称\"}, \"subjectType\": {\"type\": \"String\", \"label\": \"主体类型\"}, \"issueDescription\": {\"type\": \"String\", \"label\": \"问题描述\"}}',1,0,1,'2026-06-01 22:57:35','2026-06-01 22:57:35',0,1,NULL,NULL,NULL),(24,'inspection','检查平台','INSP_CORRECTIVE_ASSIGNED','整改责任人分配','整改案例指派责任人时触发, 通知 assignee.','{\"caseId\": {\"type\": \"Long\", \"label\": \"整改单ID\"}, \"caseCode\": {\"type\": \"String\", \"label\": \"整改单号\"}, \"deadline\": {\"type\": \"String\", \"label\": \"整改期限\"}, \"assigneeId\": {\"type\": \"Long\", \"label\": \"责任人ID\"}, \"assigneeName\": {\"type\": \"String\", \"label\": \"责任人姓名\"}}',1,0,1,'2026-06-01 22:57:35','2026-06-01 22:57:35',0,1,NULL,NULL,NULL),(25,'inspection','检查平台','INSP_CORRECTIVE_VERIFIED','整改验证通过','整改案例验证通过时触发, 通知当事人和责任人案例闭环.','{\"caseId\": {\"type\": \"Long\", \"label\": \"整改单ID\"}, \"caseCode\": {\"type\": \"String\", \"label\": \"整改单号\"}, \"subjectId\": {\"type\": \"Long\", \"label\": \"主体ID\"}, \"assigneeId\": {\"type\": \"Long\", \"label\": \"责任人ID\"}, \"subjectType\": {\"type\": \"String\", \"label\": \"主体类型\"}, \"verifierComment\": {\"type\": \"String\", \"label\": \"验证备注\"}, \"verificationResult\": {\"type\": \"String\", \"label\": \"验证结果\"}}',1,0,1,'2026-06-01 22:57:35','2026-06-01 22:57:36',0,1,NULL,NULL,NULL),(26,'inspection','检查平台','INSP_APPEAL_SUBMITTED','申诉提交','申诉提交时触发, 通知申诉人 (确认收到) 和审核员 (有待审).','{\"reason\": {\"type\": \"String\", \"label\": \"申诉理由\"}, \"appealId\": {\"type\": \"Long\", \"label\": \"申诉ID\"}, \"appealCode\": {\"type\": \"String\", \"label\": \"申诉编号\"}, \"submitterName\": {\"type\": \"String\", \"label\": \"提交人姓名\"}, \"submitterUserId\": {\"type\": \"Long\", \"label\": \"提交人ID\"}, \"submissionDetailId\": {\"type\": \"Long\", \"label\": \"扣分项ID\"}}',1,0,1,'2026-06-01 22:57:35','2026-06-01 22:57:35',0,1,NULL,NULL,NULL),(27,'inspection','检查平台','INSP_APPEAL_REVIEWED','申诉审核完成','申诉审核完成 (APPROVED/REJECTED) 时触发, 通知申诉人.','{\"appealId\": {\"type\": \"Long\", \"label\": \"申诉ID\"}, \"appealCode\": {\"type\": \"String\", \"label\": \"申诉编号\"}, \"reviewerId\": {\"type\": \"Long\", \"label\": \"审核员ID\"}, \"reviewResult\": {\"type\": \"String\", \"label\": \"审核结果\"}, \"finalAdjustment\": {\"type\": \"Decimal\", \"label\": \"实际调整\"}, \"reviewerComment\": {\"type\": \"String\", \"label\": \"审核备注\"}, \"submitterUserId\": {\"type\": \"Long\", \"label\": \"提交人ID\"}}',1,0,1,'2026-06-01 22:57:35','2026-06-01 22:57:35',0,1,NULL,NULL,NULL),(28,'inspection','检查平台','INSP_TASK_REJECTED','任务驳回','任务被审核驳回时触发, 通知检查员重新提交.','{\"taskId\": {\"type\": \"Long\", \"label\": \"任务ID\"}, \"comment\": {\"type\": \"String\", \"label\": \"驳回原因\"}, \"taskCode\": {\"type\": \"String\", \"label\": \"任务编号\"}, \"extendedTo\": {\"type\": \"String\", \"label\": \"延期到的日期\"}, \"inspectorId\": {\"type\": \"Long\", \"label\": \"检查员ID\"}, \"rejectionCount\": {\"type\": \"Long\", \"label\": \"累计驳回次数\"}}',1,0,1,'2026-06-01 22:57:35','2026-06-01 22:57:35',0,1,NULL,NULL,NULL),(29,'inspection','检查平台','INSP_CORRECTIVE_REJECTED','整改验证驳回','整改案例验证不通过 (验证驳回) 时触发, 通知当事人和责任人重新整改.','{\"caseId\": {\"type\": \"Long\", \"label\": \"整改单ID\"}, \"caseCode\": {\"type\": \"String\", \"label\": \"整改单号\"}, \"subjectId\": {\"type\": \"Long\", \"label\": \"主体ID\"}, \"assigneeId\": {\"type\": \"Long\", \"label\": \"责任人ID\"}, \"subjectType\": {\"type\": \"String\", \"label\": \"主体类型\"}, \"verifierComment\": {\"type\": \"String\", \"label\": \"驳回理由\"}, \"verificationResult\": {\"type\": \"String\", \"label\": \"验证结果\"}}',1,0,1,'2026-06-01 22:57:36','2026-06-01 22:57:36',0,1,NULL,NULL,NULL),(59,'organization','组织','ORG_UNIT_CREATED','组织创建','新组织单元建立','{\"parentId\": \"Long\", \"unitName\": \"String\", \"unitType\": \"String\", \"orgUnitId\": \"Long\"}',1,0,1,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,'CORE','com.school.management.infrastructure.extension.plugins.core.messaging.OrganizationMessagingPlugin','PLUGIN:CORE@1.0.0'),(60,'place','场所','PLACE_OCCUPIED','场所占用','用户入住/占用场所(宿舍/工位/酒店通用)','{\"placeId\": \"Long\", \"placeName\": \"String\", \"occupantId\": \"Long\", \"occupantName\": \"String\", \"placeTypeCode\": \"String\"}',1,0,1,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,'CORE','com.school.management.infrastructure.extension.plugins.core.messaging.PlaceMessagingPlugin','PLUGIN:CORE@1.0.0'),(61,'place','场所','PLACE_VACATED','场所退出','用户退出场所占用','{\"placeId\": \"Long\", \"placeName\": \"String\", \"occupantId\": \"Long\", \"occupantName\": \"String\", \"placeTypeCode\": \"String\"}',1,0,1,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,'CORE','com.school.management.infrastructure.extension.plugins.core.messaging.PlaceMessagingPlugin','PLUGIN:CORE@1.0.0'),(62,'enrollment','招生与学籍','STUDENT_STATUS_CHANGED','学籍状态变更','休学/复学/毕业/退学','{\"newStatus\": \"String\", \"oldStatus\": \"String\", \"studentId\": \"Long\"}',1,0,1,'2026-06-01 23:32:05','2026-06-01 23:32:05',0,1,'EDU','com.school.management.infrastructure.extension.plugins.education.messaging.EnrollmentMessagingPlugin','PLUGIN:EDU@1.0.0');
/*!40000 ALTER TABLE `trigger_points` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_msg_preferences`
--

DROP TABLE IF EXISTS `user_msg_preferences`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_msg_preferences` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `user_id` bigint NOT NULL,
  `event_type_code` varchar(64) DEFAULT NULL,
  `channels` varchar(255) NOT NULL DEFAULT '["IN_APP"]',
  `quiet_hours_start` varchar(5) DEFAULT NULL,
  `quiet_hours_end` varchar(5) DEFAULT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_event` (`tenant_id`,`user_id`,`event_type_code`,`deleted`),
  KEY `idx_user` (`tenant_id`,`user_id`),
  KEY `idx_event` (`event_type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户消息偏好';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_msg_preferences`
--

LOCK TABLES `user_msg_preferences` WRITE;
/*!40000 ALTER TABLE `user_msg_preferences` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_msg_preferences` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_roles`
--

DROP TABLE IF EXISTS `user_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_roles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `tenant_id` bigint DEFAULT '1' COMMENT '租户ID',
  `scope_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ALL' COMMENT '作用域类型: ALL=全局, ORG_UNIT=组织节点',
  `scope_id` bigint NOT NULL DEFAULT '0' COMMENT '作用域ID: ALL时=0, ORG_UNIT时=orgUnitId',
  `assigned_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '分配时间',
  `assigned_by` bigint DEFAULT NULL COMMENT '分配人ID',
  `expires_at` datetime DEFAULT NULL COMMENT '过期时间（空=永久）',
  `is_active` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否激活',
  `reason` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '授权原因',
  `granted_by` bigint DEFAULT NULL COMMENT '授权人ID',
  `granted_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role_scope` (`user_id`,`role_id`,`scope_type`,`scope_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_user_roles_scope` (`scope_type`,`scope_id`),
  KEY `idx_user_roles_active` (`is_active`,`expires_at`),
  KEY `idx_user_roles_tenant` (`tenant_id`),
  KEY `idx_ur_user_active` (`user_id`,`is_active`,`expires_at`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表(支持作用域)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_roles`
--

LOCK TABLES `user_roles` WRITE;
/*!40000 ALTER TABLE `user_roles` DISABLE KEYS */;
INSERT INTO `user_roles` VALUES (1,1,1,1,'ALL',0,'2026-06-01 23:43:41',NULL,NULL,1,NULL,NULL,'2026-06-01 23:43:41','2026-06-01 23:43:41');
/*!40000 ALTER TABLE `user_roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_space_relation_history`
--

DROP TABLE IF EXISTS `user_space_relation_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_space_relation_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `relation_id` bigint NOT NULL COMMENT '关系ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `place_id` bigint NOT NULL COMMENT '场所ID',
  `relation_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '关系类型',
  `action` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作类型: CREATE, UPDATE, DELETE, CHECK_IN, CHECK_OUT',
  `old_values` json DEFAULT NULL COMMENT '旧值',
  `new_values` json DEFAULT NULL COMMENT '新值',
  `operated_by` bigint DEFAULT NULL COMMENT '操作人',
  `operated_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_relation_id` (`relation_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_space_id` (`place_id`),
  KEY `idx_operated_at` (`operated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-场所关系历史表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_space_relation_history`
--

LOCK TABLES `user_space_relation_history` WRITE;
/*!40000 ALTER TABLE `user_space_relation_history` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_space_relation_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_space_relations`
--

DROP TABLE IF EXISTS `user_space_relations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_space_relations` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `place_id` bigint NOT NULL COMMENT '场所ID',
  `relation_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ASSIGNED' COMMENT '关系类型: ASSIGNED-分配, MANAGED-管理, TEMPORARY-临时',
  `position_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '位置编码（如床位号、座位号）',
  `position_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '位置名称',
  `is_primary` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否主要场所',
  `can_use` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否有使用权',
  `can_manage` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否有管理权',
  `start_date` date DEFAULT NULL COMMENT '开始日期',
  `end_date` date DEFAULT NULL COMMENT '结束日期',
  `fee_amount` decimal(10,2) DEFAULT NULL COMMENT '费用金额',
  `fee_paid` tinyint(1) DEFAULT '0' COMMENT '是否已缴费',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int DEFAULT '0' COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_space_type` (`user_id`,`place_id`,`relation_type`,`deleted`),
  UNIQUE KEY `uk_space_position` (`place_id`,`position_code`,`deleted`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_space_id` (`place_id`),
  KEY `idx_relation_type` (`relation_type`),
  KEY `idx_position_code` (`position_code`),
  KEY `idx_is_primary` (`is_primary`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-场所关系表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_space_relations`
--

LOCK TABLES `user_space_relations` WRITE;
/*!40000 ALTER TABLE `user_space_relations` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_space_relations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_student`
--

DROP TABLE IF EXISTS `user_student`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_student` (
  `id` bigint NOT NULL COMMENT 'Primary Key',
  `student_no` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Student Number',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Student Name',
  `gender` tinyint DEFAULT '1' COMMENT 'Gender: 1-Male, 2-Female',
  `id_card` varchar(18) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ID Card Number',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Phone Number',
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Email',
  `birth_date` date DEFAULT NULL COMMENT 'Birth Date',
  `enrollment_date` date DEFAULT NULL COMMENT 'Enrollment Date',
  `expected_graduation_date` date DEFAULT NULL COMMENT 'Expected Graduation Date',
  `dormitory_id` bigint DEFAULT NULL COMMENT 'Dormitory ID',
  `bed_number` int DEFAULT NULL COMMENT 'Bed Number',
  `status` tinyint DEFAULT '1' COMMENT 'Status: 1-Studying, 2-Suspended, 3-Withdrawn, 4-Graduated, 5-Expelled',
  `avatar_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Avatar URL',
  `home_address` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Home Address',
  `emergency_contact` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Emergency Contact',
  `emergency_phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Emergency Phone',
  `remark` text COLLATE utf8mb4_unicode_ci COMMENT 'Remark',
  `deleted` int DEFAULT '0' COMMENT 'Logical Delete Flag',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated At',
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `user_id` bigint DEFAULT NULL,
  `admission_date` date DEFAULT NULL,
  `graduation_date` date DEFAULT NULL,
  `student_status` int DEFAULT NULL,
  `special_notes` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_no` (`student_no`),
  KEY `idx_dormitory_id` (`dormitory_id`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_students_name` (`name`),
  KEY `idx_students_enrollment_date` (`enrollment_date`),
  KEY `idx_students_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Students Table';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_student`
--

LOCK TABLES `user_student` WRITE;
/*!40000 ALTER TABLE `user_student` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_student` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_teacher`
--

DROP TABLE IF EXISTS `user_teacher`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_teacher` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '关联用户ID',
  `employee_no` varchar(50) DEFAULT NULL COMMENT '工号',
  `title` varchar(30) DEFAULT NULL COMMENT '职称：教授/副教授/讲师/助教/实训指导',
  `title_level` varchar(20) DEFAULT NULL COMMENT '职称等级：正高/副高/中级/初级',
  `teaching_group` varchar(100) DEFAULT NULL COMMENT '所属教研室/教学组',
  `max_weekly_hours` int DEFAULT '20' COMMENT '每周最大课时',
  `qualification` text COMMENT '教学资质描述',
  `specialties` json DEFAULT NULL COMMENT '擅长领域/可授课程类别(JSON数组)',
  `hire_date` date DEFAULT NULL COMMENT '入职日期',
  `status` tinyint DEFAULT '1' COMMENT '1在职 2离职 3退休',
  `remark` varchar(500) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教师档案表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_teacher`
--

LOCK TABLES `user_teacher` WRITE;
/*!40000 ALTER TABLE `user_teacher` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_teacher` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码(BCrypt)',
  `real_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像URL',
  `gender` tinyint DEFAULT '0' COMMENT '性别:0未知,1男,2女',
  `birth_date` date DEFAULT NULL COMMENT '出生日期',
  `identity_card` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '身份证号',
  `user_type_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户类型编码',
  `status` tinyint DEFAULT '1' COMMENT '状态:0禁用,1启用',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最后登录IP',
  `password_changed_at` datetime DEFAULT NULL COMMENT '密码修改时间',
  `wechat_openid` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '微信OpenID',
  `allow_multiple_devices` tinyint DEFAULT '0' COMMENT '是否允许多设备登录:0否,1是',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `deleted` bigint DEFAULT '0' COMMENT '逻辑删除:0未删除,删除时存id',
  `attributes` json DEFAULT NULL COMMENT '扩展属性',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username_deleted` (`username`,`deleted`),
  KEY `idx_phone` (`phone`),
  KEY `idx_status` (`status`),
  KEY `idx_user_type_code` (`user_type_code`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_users_login_cover` (`username`,`password`,`status`,`deleted`),
  KEY `idx_users_search` (`real_name`,`phone`,`status`),
  KEY `idx_users_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'admin','$2a$10$NBILuC13J0f71JBhs8sr5uiB02XDpR4BF9uhki3dDsXEaIGanmR6e','超级管理员','13800000000',NULL,NULL,1,NULL,NULL,'SUPER_ADMIN',1,'2026-06-01 23:44:07','0:0:0:0:0:0:0:1','2026-04-04 19:25:03',NULL,0,1,'2025-11-05 22:49:34',1,'2026-06-01 23:44:07',NULL,0,NULL),(2041870507300622337,'teacher01','$2a$10$NBILuC13J0f71JBhs8sr5uiB02XDpR4BF9uhki3dDsXEaIGanmR6e','张明','13810000001',NULL,NULL,1,NULL,NULL,'TEACHER',1,NULL,NULL,'2026-04-08 21:27:20',NULL,0,1,'2026-04-08 21:27:20',NULL,'2026-04-08 21:27:20',NULL,0,NULL),(2041870508646993922,'teacher02','$2a$10$x7vfQ9F.3zCV1WOM8QlxJuGmIm.cNiN3wt1aE4dSN/hE2/bYC3BTC','李华','13810000002',NULL,NULL,1,NULL,NULL,'TEACHER',1,NULL,NULL,'2026-04-08 21:27:20',NULL,0,1,'2026-04-08 21:27:20',NULL,'2026-04-08 21:27:20',NULL,0,NULL);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary view structure for view `v_batch_job_dashboard`
--

DROP TABLE IF EXISTS `v_batch_job_dashboard`;
/*!50001 DROP VIEW IF EXISTS `v_batch_job_dashboard`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_batch_job_dashboard` AS SELECT 
 1 AS `job_id`,
 1 AS `job_type`,
 1 AS `job_name`,
 1 AS `job_status`,
 1 AS `total_items`,
 1 AS `processed_items`,
 1 AS `success_count`,
 1 AS `failure_count`,
 1 AS `progress_percentage`,
 1 AS `created_by_name`,
 1 AS `created_at`,
 1 AS `started_at`,
 1 AS `completed_at`,
 1 AS `duration_seconds`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `v_buildings`
--

DROP TABLE IF EXISTS `v_buildings`;
/*!50001 DROP VIEW IF EXISTS `v_buildings`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_buildings` AS SELECT 
 1 AS `id`,
 1 AS `building_no`,
 1 AS `building_name`,
 1 AS `building_type`,
 1 AS `status`,
 1 AS `description`,
 1 AS `created_at`,
 1 AS `updated_at`,
 1 AS `total_floors`,
 1 AS `total_rooms`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `v_classrooms`
--

DROP TABLE IF EXISTS `v_classrooms`;
/*!50001 DROP VIEW IF EXISTS `v_classrooms`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_classrooms` AS SELECT 
 1 AS `id`,
 1 AS `classroom_no`,
 1 AS `classroom_name`,
 1 AS `building_id`,
 1 AS `building_name`,
 1 AS `building_no`,
 1 AS `floor`,
 1 AS `capacity`,
 1 AS `classroom_type`,
 1 AS `status`,
 1 AS `classroom_category`,
 1 AS `class_id`,
 1 AS `has_projector`,
 1 AS `has_air_conditioner`,
 1 AS `has_computer`,
 1 AS `equipment_info`,
 1 AS `created_at`,
 1 AS `updated_at`*/;
SET character_set_client = @saved_cs_client;



--
-- Temporary view structure for view `v_space_detail`
--

DROP TABLE IF EXISTS `v_space_detail`;
/*!50001 DROP VIEW IF EXISTS `v_space_detail`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_space_detail` AS SELECT 
 1 AS `id`,
 1 AS `space_code`,
 1 AS `space_name`,
 1 AS `space_type`,
 1 AS `room_type`,
 1 AS `building_type`,
 1 AS `parent_id`,
 1 AS `path`,
 1 AS `level`,
 1 AS `campus_id`,
 1 AS `building_id`,
 1 AS `floor_number`,
 1 AS `capacity`,
 1 AS `current_occupancy`,
 1 AS `org_unit_id`,
 1 AS `class_id`,
 1 AS `gender_type`,
 1 AS `responsible_user_id`,
 1 AS `status`,
 1 AS `attributes`,
 1 AS `description`,
 1 AS `created_by`,
 1 AS `created_at`,
 1 AS `updated_by`,
 1 AS `updated_at`,
 1 AS `deleted`,
 1 AS `org_unit_name`,
 1 AS `assigned_class_name`,
 1 AS `class_teacher_id`,
 1 AS `teacher_name`,
 1 AS `teacher_phone`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `wechat_push_record`
--

DROP TABLE IF EXISTS `wechat_push_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wechat_push_record` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `business_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务类型: ANNOUNCEMENT-公告, NOTIFICATION-通报',
  `business_id` bigint NOT NULL COMMENT '业务ID',
  `user_id` bigint NOT NULL COMMENT '目标用户ID',
  `openid` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '微信openid',
  `template_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板ID',
  `content` text COLLATE utf8mb4_unicode_ci COMMENT '发送内容JSON',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING-待发送, SUCCESS-成功, FAILED-失败',
  `error_code` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '错误码',
  `error_msg` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '错误信息',
  `msg_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '微信返回的消息ID',
  `send_time` datetime DEFAULT NULL COMMENT '发送时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int DEFAULT '0' COMMENT '逻辑删除标志',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_business` (`business_type`,`business_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='微信推送记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wechat_push_record`
--

LOCK TABLES `wechat_push_record` WRITE;
/*!40000 ALTER TABLE `wechat_push_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `wechat_push_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `workflow_templates`
--

DROP TABLE IF EXISTS `workflow_templates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `workflow_templates` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `template_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板名称',
  `template_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板编码(唯一)',
  `template_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'TASK' COMMENT '模板类型: TASK-任务审批, LEAVE-请假, OTHER-其他',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '模板描述',
  `process_definition_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Flowable流程定义ID',
  `process_definition_key` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Flowable流程定义Key',
  `bpmn_xml` longtext COLLATE utf8mb4_unicode_ci COMMENT 'BPMN流程定义XML',
  `form_config` json DEFAULT NULL COMMENT '表单配置(JSON格式)',
  `node_config` json DEFAULT NULL COMMENT '节点配置(审批人规则等)',
  `is_default` tinyint DEFAULT '0' COMMENT '是否默认模板: 0-否, 1-是',
  `status` tinyint DEFAULT '1' COMMENT '状态: 0-禁用, 1-启用',
  `version` int DEFAULT '1' COMMENT '版本号',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_by_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人姓名',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int DEFAULT '0' COMMENT '逻辑删除: 0-未删除, 1-已删除',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`template_code`),
  KEY `idx_template_type` (`template_type`),
  KEY `idx_status` (`status`),
  KEY `idx_is_default` (`is_default`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程模板表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `workflow_templates`
--

LOCK TABLES `workflow_templates` WRITE;
/*!40000 ALTER TABLE `workflow_templates` DISABLE KEYS */;
INSERT INTO `workflow_templates` VALUES (1,'简单审批(单人)','SIMPLE_APPROVE','TASK','任务创建者直接审批，适用于简单任务',NULL,NULL,NULL,NULL,NULL,0,1,1,1,1,'系统','2026-06-01 23:05:21',NULL,'2026-06-01 23:05:21',0,1),(2,'两级审批','TWO_LEVEL_APPROVE','TASK','部门负责人审批后，由分管领导终审',NULL,NULL,NULL,NULL,NULL,1,1,1,2,1,'系统','2026-06-01 23:05:21',NULL,'2026-06-01 23:05:21',0,1),(3,'三级审批','THREE_LEVEL_APPROVE','TASK','部门负责人→分管领导→校长三级审批',NULL,NULL,NULL,NULL,NULL,0,1,1,3,1,'系统','2026-06-01 23:05:21',NULL,'2026-06-01 23:05:21',0,1),(1000001,'开班会任务-两级审批','CLASS_MEETING_TWO_LEVEL','TASK','批量分配给班主任，提交班会照片材料后，系主任和校领导两级审批。支持打回重新提交。',NULL,NULL,'<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n                  xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\"\n                  xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\"\n                  xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\"\n                  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n                  xmlns:flowable=\"http://flowable.org/bpmn\"\n                  id=\"Definitions_1\"\n                  targetNamespace=\"http://bpmn.io/schema/bpmn\"\n                  exporter=\"Camunda Modeler\"\n                  exporterVersion=\"5.0.0\">\n  <bpmn:process id=\"CLASS_MEETING_TWO_LEVEL\" name=\"开班会任务-两级审批\" isExecutable=\"true\">\n    <bpmn:startEvent id=\"StartEvent_1\" name=\"开始\">\n      <bpmn:outgoing>Flow_1</bpmn:outgoing>\n    </bpmn:startEvent>\n    <bpmn:userTask id=\"submitMaterials\" name=\"班主任提交材料\" flowable:assignee=\"${assigneeId}\">\n      <bpmn:incoming>Flow_1</bpmn:incoming>\n      <bpmn:incoming>Flow_Reject1</bpmn:incoming>\n      <bpmn:incoming>Flow_Reject2</bpmn:incoming>\n      <bpmn:outgoing>Flow_2</bpmn:outgoing>\n      <bpmn:multiInstanceLoopCharacteristics isSequential=\"false\" flowable:collection=\"${assigneeList}\" flowable:elementVariable=\"assigneeId\">\n        <bpmn:completionCondition>${nrOfCompletedInstances == nrOfInstances}</bpmn:completionCondition>\n      </bpmn:multiInstanceLoopCharacteristics>\n    </bpmn:userTask>\n    <bpmn:userTask id=\"leader1Approve\" name=\"系主任审批\" flowable:assignee=\"${leader1Id}\">\n      <bpmn:incoming>Flow_2</bpmn:incoming>\n      <bpmn:outgoing>Flow_3</bpmn:outgoing>\n    </bpmn:userTask>\n    <bpmn:exclusiveGateway id=\"Gateway_1\" name=\"领导1决策\">\n      <bpmn:incoming>Flow_3</bpmn:incoming>\n      <bpmn:outgoing>Flow_Pass1</bpmn:outgoing>\n      <bpmn:outgoing>Flow_Reject1</bpmn:outgoing>\n    </bpmn:exclusiveGateway>\n    <bpmn:userTask id=\"leader2Approve\" name=\"校领导审批\" flowable:assignee=\"${leader2Id}\">\n      <bpmn:incoming>Flow_Pass1</bpmn:incoming>\n      <bpmn:outgoing>Flow_4</bpmn:outgoing>\n    </bpmn:userTask>\n    <bpmn:exclusiveGateway id=\"Gateway_2\" name=\"领导2决策\">\n      <bpmn:incoming>Flow_4</bpmn:incoming>\n      <bpmn:outgoing>Flow_Pass2</bpmn:outgoing>\n      <bpmn:outgoing>Flow_Reject2</bpmn:outgoing>\n    </bpmn:exclusiveGateway>\n    <bpmn:endEvent id=\"EndEvent_1\" name=\"结束\">\n      <bpmn:incoming>Flow_Pass2</bpmn:incoming>\n    </bpmn:endEvent>\n    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"submitMaterials\" />\n    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"submitMaterials\" targetRef=\"leader1Approve\" />\n    <bpmn:sequenceFlow id=\"Flow_3\" sourceRef=\"leader1Approve\" targetRef=\"Gateway_1\" />\n    <bpmn:sequenceFlow id=\"Flow_Pass1\" name=\"通过\" sourceRef=\"Gateway_1\" targetRef=\"leader2Approve\">\n      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">${leader1Result == \'PASS\'}</bpmn:conditionExpression>\n    </bpmn:sequenceFlow>\n    <bpmn:sequenceFlow id=\"Flow_Reject1\" name=\"打回\" sourceRef=\"Gateway_1\" targetRef=\"submitMaterials\">\n      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">${leader1Result == \'REJECT\'}</bpmn:conditionExpression>\n    </bpmn:sequenceFlow>\n    <bpmn:sequenceFlow id=\"Flow_4\" sourceRef=\"leader2Approve\" targetRef=\"Gateway_2\" />\n    <bpmn:sequenceFlow id=\"Flow_Pass2\" name=\"通过\" sourceRef=\"Gateway_2\" targetRef=\"EndEvent_1\">\n      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">${leader2Result == \'PASS\'}</bpmn:conditionExpression>\n    </bpmn:sequenceFlow>\n    <bpmn:sequenceFlow id=\"Flow_Reject2\" name=\"打回\" sourceRef=\"Gateway_2\" targetRef=\"submitMaterials\">\n      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">${leader2Result == \'REJECT\'}</bpmn:conditionExpression>\n    </bpmn:sequenceFlow>\n  </bpmn:process>\n  <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\">\n    <bpmndi:BPMNPlane id=\"BPMNPlane_1\" bpmnElement=\"CLASS_MEETING_TWO_LEVEL\">\n      <bpmndi:BPMNShape id=\"StartEvent_1_di\" bpmnElement=\"StartEvent_1\">\n        <dc:Bounds x=\"152\" y=\"192\" width=\"36\" height=\"36\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"submitMaterials_di\" bpmnElement=\"submitMaterials\">\n        <dc:Bounds x=\"250\" y=\"170\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"leader1Approve_di\" bpmnElement=\"leader1Approve\">\n        <dc:Bounds x=\"410\" y=\"170\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Gateway_1_di\" bpmnElement=\"Gateway_1\" isMarkerVisible=\"true\">\n        <dc:Bounds x=\"565\" y=\"185\" width=\"50\" height=\"50\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"leader2Approve_di\" bpmnElement=\"leader2Approve\">\n        <dc:Bounds x=\"680\" y=\"170\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Gateway_2_di\" bpmnElement=\"Gateway_2\" isMarkerVisible=\"true\">\n        <dc:Bounds x=\"835\" y=\"185\" width=\"50\" height=\"50\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"EndEvent_1_di\" bpmnElement=\"EndEvent_1\">\n        <dc:Bounds x=\"952\" y=\"192\" width=\"36\" height=\"36\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNEdge id=\"Flow_1_di\" bpmnElement=\"Flow_1\">\n        <di:waypoint x=\"188\" y=\"210\" />\n        <di:waypoint x=\"250\" y=\"210\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_2_di\" bpmnElement=\"Flow_2\">\n        <di:waypoint x=\"350\" y=\"210\" />\n        <di:waypoint x=\"410\" y=\"210\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_3_di\" bpmnElement=\"Flow_3\">\n        <di:waypoint x=\"510\" y=\"210\" />\n        <di:waypoint x=\"565\" y=\"210\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_Pass1_di\" bpmnElement=\"Flow_Pass1\">\n        <di:waypoint x=\"615\" y=\"210\" />\n        <di:waypoint x=\"680\" y=\"210\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_Reject1_di\" bpmnElement=\"Flow_Reject1\">\n        <di:waypoint x=\"590\" y=\"185\" />\n        <di:waypoint x=\"590\" y=\"100\" />\n        <di:waypoint x=\"300\" y=\"100\" />\n        <di:waypoint x=\"300\" y=\"170\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_4_di\" bpmnElement=\"Flow_4\">\n        <di:waypoint x=\"780\" y=\"210\" />\n        <di:waypoint x=\"835\" y=\"210\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_Pass2_di\" bpmnElement=\"Flow_Pass2\">\n        <di:waypoint x=\"885\" y=\"210\" />\n        <di:waypoint x=\"952\" y=\"210\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_Reject2_di\" bpmnElement=\"Flow_Reject2\">\n        <di:waypoint x=\"860\" y=\"235\" />\n        <di:waypoint x=\"860\" y=\"320\" />\n        <di:waypoint x=\"300\" y=\"320\" />\n        <di:waypoint x=\"300\" y=\"250\" />\n      </bpmndi:BPMNEdge>\n    </bpmndi:BPMNPlane>\n  </bpmndi:BPMNDiagram>\n</bpmn:definitions>',NULL,NULL,1,1,1,10,1,'系统管理员','2026-06-01 23:25:08',NULL,'2026-06-01 23:25:08',0,1);
/*!40000 ALTER TABLE `workflow_templates` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'sm_rebuild'
--

--
-- Dumping routines for database 'sm_rebuild'
--
-- get_effective_org_unit_id 函数已删除 (V20260612_2 关系化: 有效组织即 places.effective_org_unit_id 投影列)
/*!50003 DROP PROCEDURE IF EXISTS `atomic_update_occupancy` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_unicode_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `atomic_update_occupancy`(
  IN p_place_id BIGINT,
  IN p_increment INT,
  IN p_user_id BIGINT,
  IN p_reason VARCHAR(200),
  OUT p_result VARCHAR(20),
  OUT p_message VARCHAR(500)
)
BEGIN
  DECLARE current_cap INT;
  DECLARE current_occ INT;
  DECLARE new_occ INT;
  DECLARE place_name_val VARCHAR(200);

  
  START TRANSACTION;

  
  SELECT capacity, current_occupancy, place_name
  INTO current_cap, current_occ, place_name_val
  FROM places
  WHERE id = p_place_id
  FOR UPDATE;

  
  IF current_cap IS NULL THEN
    SET p_result = 'NOT_FOUND';
    SET p_message = CONCAT('场所不存在: ', p_place_id);
    ROLLBACK;
  ELSE
    
    SET new_occ = current_occ + p_increment;

    
    IF new_occ < 0 THEN
      SET p_result = 'INVALID_VALUE';
      SET p_message = CONCAT('占用人数不能为负数: ', new_occ);
      ROLLBACK;
    ELSEIF new_occ > current_cap THEN
      SET p_result = 'OVER_CAPACITY';
      SET p_message = CONCAT('超出容量限制: 当前容量=', current_cap, ', 尝试入住=', new_occ);
      ROLLBACK;
    ELSE
      
      UPDATE places
      SET
        current_occupancy = new_occ,
        updated_at = NOW()
      WHERE id = p_place_id
        AND current_occupancy = current_occ;  

      
      IF ROW_COUNT() = 1 THEN
        SET p_result = 'SUCCESS';
        SET p_message = CONCAT('更新成功: ', current_occ, ' → ', new_occ);

        
        INSERT INTO place_audit_logs (
          event_id,
          request_id,
          resource_type,
          resource_id,
          resource_name,
          event_name,
          event_type,
          event_source,
          event_time,
          user_id,
          source_ip,
          api_endpoint,
          changed_fields,
          reason
        ) VALUES (
          UUID(),
          UUID(),
          'PLACE',
          p_place_id,
          place_name_val,
          'UpdateOccupancy',
          'SystemAction',
          'capacity-service',
          NOW(6),
          p_user_id,
          '127.0.0.1',
          '/api/v9/places/capacity',
          JSON_ARRAY(
            JSON_OBJECT(
              'fieldName', 'current_occupancy',
              'oldValue', current_occ,
              'newValue', new_occ
            )
          ),
          p_reason
        );

        COMMIT;
      ELSE
        SET p_result = 'CONFLICT';
        SET p_message = '并发冲突，请重试';
        ROLLBACK;
      END IF;
    END IF;
  END IF;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `cleanup_expired_audit_logs` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `cleanup_expired_audit_logs`()
BEGIN
  DECLARE deleted_count INT DEFAULT 0;

  
  DELETE FROM place_audit_logs
  WHERE ttl_expires_at IS NOT NULL
    AND ttl_expires_at < NOW();

  SET deleted_count = ROW_COUNT();

  
  INSERT INTO place_audit_logs (
    event_id,
    request_id,
    resource_type,
    resource_id,
    event_name,
    event_type,
    event_source,
    event_time,
    user_type,
    source_ip,
    api_endpoint,
    reason
  ) VALUES (
    UUID(),
    UUID(),
    'SYSTEM',
    0,
    'CleanupExpiredAuditLogs',
    'SystemAction',
    'audit-cleanup-job',
    NOW(6),
    'SYSTEM',
    '127.0.0.1',
    '/system/audit/cleanup',
    CONCAT('清理过期审计日志：', deleted_count, '条')
  );

  SELECT CONCAT('已清理 ', deleted_count, ' 条过期审计日志') AS result;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `cleanup_expired_jobs` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `cleanup_expired_jobs`()
BEGIN
  DECLARE deleted_count INT DEFAULT 0;

  
  DELETE FROM place_batch_jobs
  WHERE job_status IN ('COMPLETED', 'FAILED', 'CANCELLED')
    AND completed_at < DATE_SUB(NOW(), INTERVAL 30 DAY);

  SET deleted_count = ROW_COUNT();

  SELECT CONCAT('已清理 ', deleted_count, ' 个过期批量任务') AS result;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
-- get_affected_children 过程已删除 (V20260612_2 关系化: 子树重算走 PlaceOrgProjector, 代码零消费)
/*!50003 DROP PROCEDURE IF EXISTS `refresh_capacity_stats` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_unicode_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `refresh_capacity_stats`()
    DETERMINISTIC
    COMMENT '刷新容量统计物化视图'
BEGIN
  
  TRUNCATE TABLE place_capacity_stats_mv;

  
  INSERT INTO place_capacity_stats_mv (
    place_type_code,
    place_type_name,
    total_places,
    total_capacity,
    total_occupancy,
    avg_occupancy_rate,
    high_occupancy_count,
    full_occupancy_count,
    empty_count
  )
  SELECT
    pt.type_code AS place_type_code,
    pt.type_name AS place_type_name,
    COUNT(p.id) AS total_places,
    COALESCE(SUM(p.capacity), 0) AS total_capacity,
    COALESCE(SUM(p.current_occupancy), 0) AS total_occupancy,
    CASE
      WHEN SUM(p.capacity) > 0 THEN (SUM(p.current_occupancy) * 100.0 / SUM(p.capacity))
      ELSE 0
    END AS avg_occupancy_rate,
    SUM(CASE WHEN p.occupancy_rate >= 80.0 THEN 1 ELSE 0 END) AS high_occupancy_count,
    SUM(CASE WHEN p.occupancy_rate = 100.0 THEN 1 ELSE 0 END) AS full_occupancy_count,
    SUM(CASE WHEN p.occupancy_rate = 0 THEN 1 ELSE 0 END) AS empty_count
  FROM place_types pt
  LEFT JOIN places p ON p.type_code = pt.type_code AND p.deleted = 0
  GROUP BY pt.type_code, pt.type_name;

  
  INSERT INTO place_audit_logs (
    event_id,
    request_id,
    resource_type,
    resource_id,
    event_name,
    event_type,
    event_source,
    event_time,
    user_type,
    source_ip,
    api_endpoint,
    reason
  ) VALUES (
    UUID(),
    UUID(),
    'SYSTEM',
    0,
    'RefreshCapacityStats',
    'SystemAction',
    'capacity-stats-job',
    NOW(6),
    'SYSTEM',
    '127.0.0.1',
    '/system/capacity/refresh',
    CONCAT('刷新容量统计物化视图，共 ', ROW_COUNT(), ' 个场所类型')
  );
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `submit_batch_job` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `submit_batch_job`(
  IN p_job_type VARCHAR(50),
  IN p_job_name VARCHAR(200),
  IN p_total_items INT,
  IN p_request_parameters JSON,
  IN p_created_by BIGINT,
  IN p_created_by_name VARCHAR(100),
  OUT p_job_id VARCHAR(64)
)
BEGIN
  
  SET p_job_id = UUID();

  
  INSERT INTO place_batch_jobs (
    job_id,
    job_type,
    job_name,
    job_status,
    total_items,
    request_parameters,
    created_by,
    created_by_name
  ) VALUES (
    p_job_id,
    p_job_type,
    p_job_name,
    'PENDING',
    p_total_items,
    p_request_parameters,
    p_created_by,
    p_created_by_name
  );

  
  INSERT INTO place_audit_logs (
    event_id,
    request_id,
    resource_type,
    resource_id,
    event_name,
    event_type,
    event_source,
    event_time,
    user_id,
    user_name,
    source_ip,
    api_endpoint,
    request_parameters
  ) VALUES (
    UUID(),
    p_job_id,
    'BATCH_JOB',
    0,
    'SubmitBatchJob',
    'ApiCall',
    'place-batch-service',
    NOW(6),
    p_created_by,
    p_created_by_name,
    '127.0.0.1',
    '/api/v9/places/batch-jobs',
    JSON_OBJECT('jobId', p_job_id, 'jobType', p_job_type)
  );
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `update_job_progress` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `update_job_progress`(
  IN p_job_id VARCHAR(64),
  IN p_processed_increment INT,
  IN p_success_increment INT,
  IN p_failure_increment INT
)
BEGIN
  DECLARE current_status VARCHAR(30);
  DECLARE current_processed INT;
  DECLARE current_total INT;

  
  UPDATE place_batch_jobs
  SET
    processed_items = processed_items + p_processed_increment,
    success_count = success_count + p_success_increment,
    failure_count = failure_count + p_failure_increment,
    started_at = COALESCE(started_at, NOW()),
    job_status = CASE
      WHEN job_status = 'PENDING' THEN 'RUNNING'
      ELSE job_status
    END
  WHERE job_id = p_job_id;

  
  SELECT job_status, processed_items, total_items
  INTO current_status, current_processed, current_total
  FROM place_batch_jobs
  WHERE job_id = p_job_id;

  
  IF current_processed >= current_total AND current_status = 'RUNNING' THEN
    UPDATE place_batch_jobs
    SET
      job_status = CASE
        WHEN failure_count = 0 THEN 'COMPLETED'
        WHEN failure_count < total_items THEN 'PARTIALLY_COMPLETED'
        ELSE 'FAILED'
      END,
      completed_at = NOW()
    WHERE job_id = p_job_id;
  END IF;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Final view structure for view `_classes_backup_20260408`
--

/*!50001 DROP VIEW IF EXISTS `_classes_backup_20260408`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `_classes_backup_20260408` AS select `o`.`id` AS `id`,`o`.`unit_name` AS `class_name`,`o`.`unit_code` AS `class_code`,`o`.`id` AS `org_unit_id`,coalesce(cast(json_unquote(json_extract(`o`.`attributes`,'$.gradeLevel')) as unsigned),1) AS `grade_level`,cast(json_unquote(json_extract(`o`.`attributes`,'$.gradeId')) as unsigned) AS `grade_id`,cast(json_unquote(json_extract(`o`.`attributes`,'$.majorId')) as unsigned) AS `major_id`,cast(json_unquote(json_extract(`o`.`attributes`,'$.headTeacher')) as unsigned) AS `teacher_id`,coalesce(cast(json_unquote(json_extract(`o`.`attributes`,'$.studentCount')) as unsigned),0) AS `student_count`,coalesce(cast(json_unquote(json_extract(`o`.`attributes`,'$.classType')) as unsigned),1) AS `class_type`,(case `o`.`status` when 'ACTIVE' then 1 else 0 end) AS `status`,`o`.`created_at` AS `created_at`,`o`.`updated_at` AS `updated_at`,`o`.`deleted` AS `deleted`,`o`.`tenant_id` AS `tenant_id` from `org_units` `o` where (`o`.`unit_type` = 'CLASS') */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `classes`
--

/*!50001 DROP VIEW IF EXISTS `classes`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `classes` AS select `o`.`id` AS `id`,`o`.`unit_name` AS `class_name`,`o`.`unit_code` AS `class_code`,coalesce(cast(json_unquote(json_extract(`o`.`attributes`,'$.gradeLevel')) as unsigned),1) AS `grade_level`,`o`.`parent_id` AS `org_unit_id`,cast(json_unquote(json_extract(`o`.`attributes`,'$.gradeId')) as unsigned) AS `grade_id`,cast(json_unquote(json_extract(`o`.`attributes`,'$.majorId')) as unsigned) AS `major_id`,cast(json_unquote(json_extract(`o`.`attributes`,'$.majorDirectionId')) as unsigned) AS `major_direction_id`,NULL AS `class_sequence`,cast(json_unquote(json_extract(`o`.`attributes`,'$.headTeacher')) as unsigned) AS `teacher_id`,cast(json_unquote(json_extract(`o`.`attributes`,'$.assistantTeacher')) as unsigned) AS `assistant_teacher_id`,coalesce(cast(json_unquote(json_extract(`o`.`attributes`,'$.studentCount')) as unsigned),0) AS `student_count`,json_unquote(json_extract(`o`.`attributes`,'$.classroomLocation')) AS `classroom_location`,year(now()) AS `enrollment_year`,json_unquote(json_extract(`o`.`attributes`,'$.educationSystem')) AS `education_system`,NULL AS `skill_level`,cast(json_unquote(json_extract(`o`.`attributes`,'$.duration')) as unsigned) AS `duration`,cast(json_unquote(json_extract(`o`.`attributes`,'$.graduationYear')) as unsigned) AS `graduation_year`,coalesce(cast(json_unquote(json_extract(`o`.`attributes`,'$.classType')) as unsigned),1) AS `class_type`,(case `o`.`status` when 'ACTIVE' then 1 when 'FROZEN' then 0 else 1 end) AS `status`,0 AS `is_international`,0 AS `is_experimental`,0 AS `is_oriented`,`o`.`created_at` AS `created_at`,`o`.`updated_at` AS `updated_at`,`o`.`created_by` AS `created_by`,`o`.`updated_by` AS `updated_by`,`o`.`deleted` AS `deleted`,`o`.`tenant_id` AS `tenant_id` from `org_units` `o` where (`o`.`type_code` = 'CLASS') */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `v_batch_job_dashboard`
--

/*!50001 DROP VIEW IF EXISTS `v_batch_job_dashboard`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `v_batch_job_dashboard` AS select `place_batch_jobs`.`job_id` AS `job_id`,`place_batch_jobs`.`job_type` AS `job_type`,`place_batch_jobs`.`job_name` AS `job_name`,`place_batch_jobs`.`job_status` AS `job_status`,`place_batch_jobs`.`total_items` AS `total_items`,`place_batch_jobs`.`processed_items` AS `processed_items`,`place_batch_jobs`.`success_count` AS `success_count`,`place_batch_jobs`.`failure_count` AS `failure_count`,`place_batch_jobs`.`progress_percentage` AS `progress_percentage`,`place_batch_jobs`.`created_by_name` AS `created_by_name`,`place_batch_jobs`.`created_at` AS `created_at`,`place_batch_jobs`.`started_at` AS `started_at`,`place_batch_jobs`.`completed_at` AS `completed_at`,timestampdiff(SECOND,`place_batch_jobs`.`started_at`,coalesce(`place_batch_jobs`.`completed_at`,now())) AS `duration_seconds` from `place_batch_jobs` where (`place_batch_jobs`.`created_at` >= (now() - interval 7 day)) order by `place_batch_jobs`.`created_at` desc */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `v_buildings`
--

/*!50001 DROP VIEW IF EXISTS `v_buildings`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `v_buildings` AS select `s`.`id` AS `id`,`s`.`space_code` AS `building_no`,`s`.`space_name` AS `building_name`,`s`.`building_type` AS `building_type`,`s`.`status` AS `status`,`s`.`description` AS `description`,`s`.`created_at` AS `created_at`,`s`.`updated_at` AS `updated_at`,(select count(0) from `space` `c` where ((`c`.`parent_id` = `s`.`id`) and (`c`.`space_type` = 'FLOOR') and (`c`.`deleted` = 0))) AS `total_floors`,(select count(0) from `space` `r` where ((`r`.`building_id` = `s`.`id`) and (`r`.`space_type` = 'ROOM') and (`r`.`deleted` = 0))) AS `total_rooms` from `space` `s` where ((`s`.`space_type` = 'BUILDING') and (`s`.`deleted` = 0)) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `v_classrooms`
--

/*!50001 DROP VIEW IF EXISTS `v_classrooms`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `v_classrooms` AS select `s`.`id` AS `id`,`s`.`space_code` AS `classroom_no`,`s`.`space_name` AS `classroom_name`,`s`.`building_id` AS `building_id`,(select `space`.`space_name` from `space` where (`space`.`id` = `s`.`building_id`)) AS `building_name`,(select `space`.`space_code` from `space` where (`space`.`id` = `s`.`building_id`)) AS `building_no`,`s`.`floor_number` AS `floor`,`s`.`capacity` AS `capacity`,`s`.`room_type` AS `classroom_type`,`s`.`status` AS `status`,`e`.`classroom_category` AS `classroom_category`,`e`.`assigned_class_id` AS `class_id`,`e`.`has_projector` AS `has_projector`,`e`.`has_air_conditioner` AS `has_air_conditioner`,`e`.`has_computer` AS `has_computer`,`e`.`equipment_info` AS `equipment_info`,`s`.`created_at` AS `created_at`,`s`.`updated_at` AS `updated_at` from (`space` `s` left join `space_classroom_ext` `e` on((`s`.`id` = `e`.`space_id`))) where ((`s`.`space_type` = 'ROOM') and (`s`.`room_type` in ('CLASSROOM','MULTIMEDIA','SMART_CLASSROOM')) and (`s`.`deleted` = 0)) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;



--
-- Final view structure for view `v_space_detail`
--

/*!50001 DROP VIEW IF EXISTS `v_space_detail`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `v_space_detail` AS select `s`.`id` AS `id`,`s`.`space_code` AS `space_code`,`s`.`space_name` AS `space_name`,`s`.`space_type` AS `space_type`,`s`.`room_type` AS `room_type`,`s`.`building_type` AS `building_type`,`s`.`parent_id` AS `parent_id`,`s`.`path` AS `path`,`s`.`level` AS `level`,`s`.`campus_id` AS `campus_id`,`s`.`building_id` AS `building_id`,`s`.`floor_number` AS `floor_number`,`s`.`capacity` AS `capacity`,`s`.`current_occupancy` AS `current_occupancy`,`s`.`org_unit_id` AS `org_unit_id`,`s`.`class_id` AS `class_id`,`s`.`gender_type` AS `gender_type`,`s`.`responsible_user_id` AS `responsible_user_id`,`s`.`status` AS `status`,`s`.`attributes` AS `attributes`,`s`.`description` AS `description`,`s`.`created_by` AS `created_by`,`s`.`created_at` AS `created_at`,`s`.`updated_by` AS `updated_by`,`s`.`updated_at` AS `updated_at`,`s`.`deleted` AS `deleted`,`ou`.`unit_name` AS `org_unit_name`,`c`.`class_name` AS `assigned_class_name`,`c`.`teacher_id` AS `class_teacher_id`,`t`.`real_name` AS `teacher_name`,`t`.`phone` AS `teacher_phone` from (((`space` `s` left join `org_units` `ou` on((`s`.`org_unit_id` = `ou`.`id`))) left join `classes` `c` on((`s`.`class_id` = `c`.`id`))) left join `users` `t` on((`c`.`teacher_id` = `t`.`id`))) where (`s`.`deleted` = 0) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-01 23:48:03
