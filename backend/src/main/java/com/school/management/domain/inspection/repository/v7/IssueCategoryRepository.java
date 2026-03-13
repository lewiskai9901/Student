package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.corrective.IssueCategory;

import java.util.List;
import java.util.Optional;

public interface IssueCategoryRepository {

    IssueCategory save(IssueCategory category);

    Optional<IssueCategory> findById(Long id);

    List<IssueCategory> findAll();

    List<IssueCategory> findByParentId(Long parentId);

    List<IssueCategory> findRoots();

    void deleteById(Long id);
}
