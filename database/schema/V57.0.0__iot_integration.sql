-- V57.0.0 Feature 6.3: IoT传感器集成

CREATE TABLE insp_iot_sensors (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  sensor_code VARCHAR(100) NOT NULL,
  sensor_name VARCHAR(200) NOT NULL,
  sensor_type VARCHAR(50) NOT NULL COMMENT 'TEMPERATURE|HUMIDITY|AIR_QUALITY|NOISE|LIGHT|SMOKE|WATER',
  location_name VARCHAR(200),
  place_id BIGINT DEFAULT NULL,
  mqtt_topic VARCHAR(200) COMMENT 'MQTT订阅主题',
  data_unit VARCHAR(20) COMMENT '数据单位',
  is_active TINYINT DEFAULT 1,
  last_reading DECIMAL(10,2) DEFAULT NULL,
  last_reading_at DATETIME DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_code (tenant_id, sensor_code)
) COMMENT='IoT传感器注册';

CREATE TABLE insp_sensor_readings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  sensor_id BIGINT NOT NULL,
  reading_value DECIMAL(10,2) NOT NULL,
  reading_unit VARCHAR(20),
  recorded_at DATETIME NOT NULL,
  INDEX idx_sensor_time (sensor_id, recorded_at)
) COMMENT='传感器读数';

CREATE TABLE insp_item_sensor_bindings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  template_item_id BIGINT NOT NULL,
  sensor_id BIGINT NOT NULL,
  auto_fill TINYINT DEFAULT 1 COMMENT '是否自动填充',
  auto_score TINYINT DEFAULT 0 COMMENT '是否自动评分',
  scoring_thresholds JSON COMMENT '自动评分阈值配置',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_item (template_item_id)
) COMMENT='检查项-传感器绑定';
