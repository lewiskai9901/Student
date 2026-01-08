package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.ArchiveRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 归档规则Mapper接口
 *
 * @author system
 * @since 2.0.0
 */
@Mapper
public interface ArchiveRuleMapper extends BaseMapper<ArchiveRule> {

    /**
     * 查询所有启用的归档规则
     *
     * @return 规则列表
     */
    List<ArchiveRule> selectAllEnabled();

    /**
     * 根据表名查询归档规则
     *
     * @param tableName 表名
     * @return 规则
     */
    ArchiveRule selectByTableName(@Param("tableName") String tableName);
}
