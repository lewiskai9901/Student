package com.school.management.domain.scoring.repository;

import com.school.management.domain.scoring.model.entity.FormulaVariable;

import java.util.List;
import java.util.Optional;

/**
 * 内置变量仓储接口
 */
public interface FormulaVariableRepository {

    /**
     * 根据变量名查询
     */
    Optional<FormulaVariable> findByName(String name);

    /**
     * 查询所有启用的变量
     */
    List<FormulaVariable> findAllEnabled();

    /**
     * 按分类查询
     */
    List<FormulaVariable> findByCategory(String category);

    /**
     * 查询所有
     */
    List<FormulaVariable> findAll();
}
