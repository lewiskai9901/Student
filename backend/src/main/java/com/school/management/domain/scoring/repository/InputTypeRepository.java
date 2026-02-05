package com.school.management.domain.scoring.repository;

import com.school.management.domain.scoring.model.aggregate.InputType;

import java.util.List;
import java.util.Optional;

/**
 * 打分方式仓储接口
 */
public interface InputTypeRepository {

    /**
     * 保存打分方式
     */
    InputType save(InputType inputType);

    /**
     * 根据ID查询
     */
    Optional<InputType> findById(Long id);

    /**
     * 根据代码查询
     */
    Optional<InputType> findByCode(String code);

    /**
     * 查询所有启用的
     */
    List<InputType> findAllEnabled();

    /**
     * 按分类查询
     */
    List<InputType> findByCategory(String category);

    /**
     * 查询所有
     */
    List<InputType> findAll();

    /**
     * 删除
     */
    void delete(Long id);

    /**
     * 检查代码是否存在
     */
    boolean existsByCode(String code);
}
