package com.school.management.infrastructure.persistence.inspection.v7.platform;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReportTemplateMapper extends BaseMapper<ReportTemplatePO> {

    @Select("SELECT * FROM insp_report_templates WHERE report_type = #{reportType} AND deleted = 0 ORDER BY is_default DESC")
    List<ReportTemplatePO> findByReportType(@Param("reportType") String reportType);

    @Select("SELECT * FROM insp_report_templates WHERE report_type = #{reportType} AND is_default = 1 AND deleted = 0 LIMIT 1")
    ReportTemplatePO findDefaultByReportType(@Param("reportType") String reportType);

    @Select("SELECT * FROM insp_report_templates WHERE deleted = 0 ORDER BY report_type, is_default DESC")
    List<ReportTemplatePO> findAllActive();
}
