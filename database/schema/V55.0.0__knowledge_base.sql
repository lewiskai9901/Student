-- V55.0.0 Feature 5.2: 知识库

CREATE TABLE insp_knowledge_articles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  title VARCHAR(200) NOT NULL,
  content TEXT NOT NULL,
  category VARCHAR(100),
  tags VARCHAR(500),
  related_item_codes JSON COMMENT '关联检查项编码列表',
  source_case_id BIGINT DEFAULT NULL COMMENT '来源纠正案例',
  view_count INT DEFAULT 0,
  helpful_count INT DEFAULT 0,
  is_pinned TINYINT DEFAULT 0,
  created_by BIGINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  FULLTEXT KEY ft_search (title, content, tags)
) COMMENT='知识库文章';
