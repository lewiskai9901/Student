package com.school.management.infrastructure.persistence.inspection.v7.knowledge;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KnowledgeArticleMapper extends BaseMapper<KnowledgeArticlePO> {

    @Select("SELECT * FROM insp_knowledge_articles WHERE MATCH(title, content) AGAINST(#{keyword} IN BOOLEAN MODE) AND deleted = 0 ORDER BY is_pinned DESC, created_at DESC")
    List<KnowledgeArticlePO> search(@Param("keyword") String keyword);

    @Select("SELECT * FROM insp_knowledge_articles WHERE category = #{category} AND deleted = 0 ORDER BY is_pinned DESC, created_at DESC")
    List<KnowledgeArticlePO> findByCategory(@Param("category") String category);

    @Select("SELECT * FROM insp_knowledge_articles WHERE is_pinned = 1 AND deleted = 0 ORDER BY created_at DESC")
    List<KnowledgeArticlePO> findPinned();
}
