package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.template.ResponseSetOption;

import java.util.List;
import java.util.Optional;

public interface ResponseSetOptionRepository {

    ResponseSetOption save(ResponseSetOption option);

    Optional<ResponseSetOption> findById(Long id);

    List<ResponseSetOption> findByResponseSetId(Long responseSetId);

    void deleteById(Long id);

    void deleteByResponseSetId(Long responseSetId);
}
