package com.school.management.infrastructure.persistence.calendar;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface AcademicYearMapper extends BaseMapper<AcademicYearPO> {

    @Select("SELECT * FROM academic_years WHERE is_current = 1 AND deleted = 0 LIMIT 1")
    AcademicYearPO findCurrent();

    @Select("SELECT * FROM academic_years WHERE deleted = 0 ORDER BY start_date DESC")
    List<AcademicYearPO> findAllActive();

    @Update("UPDATE academic_years SET is_current = 0 WHERE deleted = 0")
    void clearAllCurrentFlags();
}
