package com.school.management.infrastructure.persistence.inspection.v7.template;

import com.school.management.domain.inspection.model.v7.template.ResponseSet;
import com.school.management.domain.inspection.repository.v7.ResponseSetRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ResponseSetRepositoryImpl implements ResponseSetRepository {

    private final ResponseSetMapper mapper;

    public ResponseSetRepositoryImpl(ResponseSetMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ResponseSet save(ResponseSet responseSet) {
        ResponseSetPO po = toPO(responseSet);
        if (responseSet.getId() == null) {
            mapper.insert(po);
            responseSet.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return responseSet;
    }

    @Override
    public Optional<ResponseSet> findById(Long id) {
        ResponseSetPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<ResponseSet> findBySetCode(String setCode) {
        ResponseSetPO po = mapper.findBySetCode(setCode);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<ResponseSet> findAll() {
        return mapper.selectList(null).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ResponseSet> findByIsGlobal(Boolean isGlobal) {
        return mapper.findByIsGlobal(isGlobal).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ResponseSet> findPagedWithConditions(int offset, int size, String keyword) {
        return mapper.findPagedWithConditions(offset, size, keyword).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countWithConditions(String keyword) {
        return mapper.countWithConditions(keyword);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private ResponseSetPO toPO(ResponseSet domain) {
        ResponseSetPO po = new ResponseSetPO();
        po.setId(domain.getId());
        po.setTenantId(0L);
        po.setSetCode(domain.getSetCode());
        po.setSetName(domain.getSetName());
        po.setIsGlobal(domain.getIsGlobal());
        po.setIsEnabled(domain.getIsEnabled());
        po.setCreatedBy(domain.getCreatedBy());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedBy(domain.getUpdatedBy());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private ResponseSet toDomain(ResponseSetPO po) {
        return ResponseSet.reconstruct(ResponseSet.builder()
                .id(po.getId())
                .setCode(po.getSetCode())
                .setName(po.getSetName())
                .isGlobal(po.getIsGlobal())
                .isEnabled(po.getIsEnabled())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedBy(po.getUpdatedBy())
                .updatedAt(po.getUpdatedAt()));
    }
}
