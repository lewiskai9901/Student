package com.school.management.infrastructure.persistence.inspection.v7.template;

import com.school.management.domain.inspection.model.v7.template.ResponseSetOption;
import com.school.management.domain.inspection.repository.v7.ResponseSetOptionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ResponseSetOptionRepositoryImpl implements ResponseSetOptionRepository {

    private final ResponseSetOptionMapper mapper;

    public ResponseSetOptionRepositoryImpl(ResponseSetOptionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ResponseSetOption save(ResponseSetOption option) {
        ResponseSetOptionPO po = toPO(option);
        if (option.getId() == null) {
            mapper.insert(po);
            option.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return option;
    }

    @Override
    public Optional<ResponseSetOption> findById(Long id) {
        ResponseSetOptionPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<ResponseSetOption> findByResponseSetId(Long responseSetId) {
        return mapper.findByResponseSetId(responseSetId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteByResponseSetId(Long responseSetId) {
        mapper.softDeleteByResponseSetId(responseSetId);
    }

    private ResponseSetOptionPO toPO(ResponseSetOption domain) {
        ResponseSetOptionPO po = new ResponseSetOptionPO();
        po.setId(domain.getId());
        po.setTenantId(0L);
        po.setResponseSetId(domain.getResponseSetId());
        po.setOptionValue(domain.getOptionValue());
        po.setOptionLabel(domain.getOptionLabel());
        po.setOptionColor(domain.getOptionColor());
        po.setScore(domain.getScore());
        po.setIsFlagged(domain.getIsFlagged());
        po.setSortOrder(domain.getSortOrder());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private ResponseSetOption toDomain(ResponseSetOptionPO po) {
        return ResponseSetOption.reconstruct(ResponseSetOption.builder()
                .id(po.getId())
                .responseSetId(po.getResponseSetId())
                .optionValue(po.getOptionValue())
                .optionLabel(po.getOptionLabel())
                .optionColor(po.getOptionColor())
                .score(po.getScore())
                .isFlagged(po.getIsFlagged())
                .sortOrder(po.getSortOrder())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt()));
    }
}
