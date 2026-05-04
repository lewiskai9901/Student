package com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.evaluation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评教指标项是评教活动的子表, 通过 evaluation_id 派生权限,
 * 不需要独立 @DataPermission.
 */
@Mapper
public interface EvaluationIndicatorMapper extends BaseMapper<EvaluationIndicatorPO> {
}
