package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.template.LibraryItem;

import java.util.List;
import java.util.Optional;

public interface LibraryItemRepository {

    LibraryItem save(LibraryItem item);

    Optional<LibraryItem> findById(Long id);

    Optional<LibraryItem> findByItemCode(String itemCode);

    List<LibraryItem> findAll();

    List<LibraryItem> search(String keyword, String category);

    List<String> findDistinctCategories();

    void deleteById(Long id);
}
