package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.platform.ReportTemplate;

import java.util.List;
import java.util.Optional;

public interface ReportTemplateRepository {

    ReportTemplate save(ReportTemplate reportTemplate);

    Optional<ReportTemplate> findById(Long id);

    List<ReportTemplate> findAll();

    List<ReportTemplate> findByReportType(String reportType);

    Optional<ReportTemplate> findDefaultByReportType(String reportType);

    void deleteById(Long id);
}
