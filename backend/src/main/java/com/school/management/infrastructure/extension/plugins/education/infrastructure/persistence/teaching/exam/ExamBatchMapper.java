package com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.exam;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.infrastructure.access.DataPermission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DataPermission(module = "exam_batch")
public interface ExamBatchMapper extends BaseMapper<ExamBatchPO> {
}
