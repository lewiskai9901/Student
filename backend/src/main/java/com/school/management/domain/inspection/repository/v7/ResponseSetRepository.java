package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.template.ResponseSet;

import java.util.List;
import java.util.Optional;

public interface ResponseSetRepository {

    ResponseSet save(ResponseSet responseSet);

    Optional<ResponseSet> findById(Long id);

    Optional<ResponseSet> findBySetCode(String setCode);

    List<ResponseSet> findAll();

    List<ResponseSet> findByIsGlobal(Boolean isGlobal);

    List<ResponseSet> findPagedWithConditions(int offset, int size, String keyword);

    long countWithConditions(String keyword);

    void deleteById(Long id);
}
