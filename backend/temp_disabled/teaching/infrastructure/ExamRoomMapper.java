package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 考试教室Mapper
 */
@Mapper
public interface ExamRoomMapper extends BaseMapper<ExamRoomPO> {

    @Delete("DELETE FROM exam_rooms WHERE arrangement_id = #{arrangementId}")
    void deleteByArrangementId(@Param("arrangementId") Long arrangementId);
}
