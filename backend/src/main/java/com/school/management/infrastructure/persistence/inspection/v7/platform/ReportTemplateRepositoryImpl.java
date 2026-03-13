package com.school.management.infrastructure.persistence.inspection.v7.platform;

import com.school.management.domain.inspection.model.v7.platform.ReportTemplate;
import com.school.management.domain.inspection.repository.v7.ReportTemplateRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ReportTemplateRepositoryImpl implements ReportTemplateRepository {

    private final ReportTemplateMapper mapper;

    public ReportTemplateRepositoryImpl(ReportTemplateMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ReportTemplate save(ReportTemplate template) {
        ReportTemplatePO po = toPO(template);
        if (template.getId() == null) {
            mapper.insert(po);
            template.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return template;
    }

    @Override
    public Optional<ReportTemplate> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<ReportTemplate> findAll() {
        return mapper.findAllActive().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<ReportTemplate> findByReportType(String reportType) {
        return mapper.findByReportType(reportType).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<ReportTemplate> findDefaultByReportType(String reportType) {
        return Optional.ofNullable(mapper.findDefaultByReportType(reportType)).map(this::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private ReportTemplatePO toPO(ReportTemplate d) {
        ReportTemplatePO po = new ReportTemplatePO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId());
        po.setTemplateName(d.getTemplateName());
        po.setTemplateCode(d.getTemplateCode());
        po.setReportType(d.getReportType());
        po.setFormatConfig(d.getFormatConfig());
        po.setHeaderConfig(d.getHeaderConfig());
        po.setIsDefault(d.getIsDefault());
        po.setIsEnabled(d.getIsEnabled());
        po.setCreatedBy(d.getCreatedBy());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private ReportTemplate toDomain(ReportTemplatePO po) {
        return ReportTemplate.reconstruct(ReportTemplate.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .templateName(po.getTemplateName())
                .templateCode(po.getTemplateCode())
                .reportType(po.getReportType())
                .formatConfig(po.getFormatConfig())
                .headerConfig(po.getHeaderConfig())
                .isDefault(po.getIsDefault())
                .isEnabled(po.getIsEnabled())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt()));
    }
}
