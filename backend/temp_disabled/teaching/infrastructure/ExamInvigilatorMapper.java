package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 监考教师Mapper
 */
@Mapper
public interface ExamInvigilatorMapper extends BaseMapper<ExamInvigilatorPO> {

    @Delete("DELETE FROM exam_invigilators WHERE room_id = #{roomId}")
    void deleteByRoomId(@Param("roomId") Long roomId);
}
