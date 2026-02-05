package com.school.management.domain.scoring.repository;

import com.school.management.domain.scoring.model.entity.FormulaFunction;

import java.util.List;
import java.util.Optional;

/**
 * 内置函数仓储接口
 */
public interface FormulaFunctionRepository {

    /**
     * 根据函数名查询
     */
    Optional<FormulaFunction> findByName(String name);

    /**
     * 查询所有启用的函数
     */
    List<FormulaFunction> findAllEnabled();

    /**
     * 按分类查询
     */
    List<FormulaFunction> findByCategory(String category);

    /**
     * 查询所有
     */
    List<FormulaFunction> findAll();
}
