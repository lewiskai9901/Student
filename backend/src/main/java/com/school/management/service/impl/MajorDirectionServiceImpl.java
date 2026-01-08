package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.entity.GradeMajorDirection;
import com.school.management.entity.MajorDirection;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.ClassMapper;
import com.school.management.mapper.GradeMajorDirectionMapper;
import com.school.management.mapper.MajorDirectionMapper;
import com.school.management.service.MajorDirectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 专业方向服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MajorDirectionServiceImpl implements MajorDirectionService {

    private final MajorDirectionMapper majorDirectionMapper;
    private final GradeMajorDirectionMapper gradeMajorDirectionMapper;
    private final ClassMapper classMapper;

    @Override
    public IPage<MajorDirection> getDirectionPage(int pageNum, int pageSize, MajorDirection params) {
        Page<MajorDirection> page = new Page<>(pageNum, pageSize);
        return majorDirectionMapper.selectDirectionPage(page, params);
    }

    @Override
    public List<MajorDirection> getDirectionsByMajorId(Long majorId) {
        if (majorId == null) {
            throw new BusinessException("专业ID不能为空");
        }
        return majorDirectionMapper.selectByMajorId(majorId);
    }

    @Override
    public List<MajorDirection> getAllDirections() {
        return majorDirectionMapper.selectAllDirections();
    }

    @Override
    public MajorDirection getDirectionById(Long id) {
        if (id == null) {
            throw new BusinessException("专业方向ID不能为空");
        }
        MajorDirection direction = majorDirectionMapper.selectDirectionById(id);
        if (direction == null) {
            throw new BusinessException("专业方向不存在");
        }
        return direction;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MajorDirection createDirection(MajorDirection direction) {
        // 参数校验
        validateDirection(direction);

        // 检查方向名称是否重复
        if (existsDirectionByName(direction.getMajorId(), direction.getDirectionName(), null)) {
            throw new BusinessException("该专业下已存在同名方向");
        }

        // 检查方向编码是否重复
        if (StringUtils.hasText(direction.getDirectionCode()) &&
            existsDirectionByCode(direction.getDirectionCode(), null)) {
            throw new BusinessException("方向编码已存在");
        }

        majorDirectionMapper.insert(direction);
        log.info("创建专业方向成功: {}", direction.getDirectionName());
        return direction;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDirection(Long id, MajorDirection direction) {
        if (id == null) {
            throw new BusinessException("专业方向ID不能为空");
        }

        MajorDirection existingDirection = majorDirectionMapper.selectById(id);
        if (existingDirection == null) {
            throw new BusinessException("专业方向不存在");
        }

        // 检查方向名称是否重复
        if (StringUtils.hasText(direction.getDirectionName()) &&
            !direction.getDirectionName().equals(existingDirection.getDirectionName())) {
            Long majorId = direction.getMajorId() != null ? direction.getMajorId() : existingDirection.getMajorId();
            if (existsDirectionByName(majorId, direction.getDirectionName(), id)) {
                throw new BusinessException("该专业下已存在同名方向");
            }
        }

        // 检查方向编码是否重复
        if (StringUtils.hasText(direction.getDirectionCode()) &&
            !direction.getDirectionCode().equals(existingDirection.getDirectionCode())) {
            if (existsDirectionByCode(direction.getDirectionCode(), id)) {
                throw new BusinessException("方向编码已存在");
            }
        }

        direction.setId(id);
        majorDirectionMapper.updateById(direction);
        log.info("更新专业方向成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDirection(Long id) {
        if (id == null) {
            throw new BusinessException("专业方向ID不能为空");
        }

        MajorDirection direction = majorDirectionMapper.selectById(id);
        if (direction == null) {
            throw new BusinessException("专业方向不存在");
        }

        // 检查是否有关联的年级
        List<GradeMajorDirection> gradeRelations = gradeMajorDirectionMapper.selectByMajorDirectionId(id);
        if (gradeRelations != null && !gradeRelations.isEmpty()) {
            throw new BusinessException("该专业方向已被" + gradeRelations.size() + "个学年使用,无法删除");
        }

        majorDirectionMapper.deleteById(id);
        log.info("删除专业方向成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteDirections(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("专业方向ID列表不能为空");
        }

        for (Long id : ids) {
            deleteDirection(id);
        }
        log.info("批量删除专业方向成功: count={}", ids.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDirectionsByMajorId(Long majorId) {
        if (majorId == null) {
            throw new BusinessException("专业ID不能为空");
        }

        // 检查是否有关联的年级使用这些方向
        List<MajorDirection> directions = majorDirectionMapper.selectByMajorId(majorId);
        for (MajorDirection direction : directions) {
            List<GradeMajorDirection> gradeRelations = gradeMajorDirectionMapper.selectByMajorDirectionId(direction.getId());
            if (gradeRelations != null && !gradeRelations.isEmpty()) {
                throw new BusinessException("专业方向[" + direction.getDirectionName() + "]已被学年使用,无法删除");
            }
        }

        majorDirectionMapper.deleteByMajorId(majorId);
        log.info("删除专业下所有方向成功: majorId={}", majorId);
    }

    /**
     * 检查方向名称是否存在
     */
    private boolean existsDirectionByName(Long majorId, String directionName, Long excludeId) {
        LambdaQueryWrapper<MajorDirection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MajorDirection::getMajorId, majorId)
               .eq(MajorDirection::getDirectionName, directionName)
               .eq(MajorDirection::getDeleted, 0);
        if (excludeId != null) {
            wrapper.ne(MajorDirection::getId, excludeId);
        }
        return majorDirectionMapper.selectCount(wrapper) > 0;
    }

    /**
     * 检查方向编码是否存在
     */
    private boolean existsDirectionByCode(String directionCode, Long excludeId) {
        LambdaQueryWrapper<MajorDirection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MajorDirection::getDirectionCode, directionCode)
               .eq(MajorDirection::getDeleted, 0);
        if (excludeId != null) {
            wrapper.ne(MajorDirection::getId, excludeId);
        }
        return majorDirectionMapper.selectCount(wrapper) > 0;
    }

    /**
     * 校验专业方向数据
     */
    private void validateDirection(MajorDirection direction) {
        if (direction == null) {
            throw new BusinessException("专业方向数据不能为空");
        }
        if (direction.getMajorId() == null) {
            throw new BusinessException("所属专业不能为空");
        }
        if (!StringUtils.hasText(direction.getDirectionName())) {
            throw new BusinessException("方向名称不能为空");
        }

        // 验证分段注册逻辑
        if (direction.getIsSegmented() != null && direction.getIsSegmented() == 1) {
            // 分段注册模式
            if (!StringUtils.hasText(direction.getPhase1Level())) {
                throw new BusinessException("分段注册需要设置第一阶段层次");
            }
            if (direction.getPhase1Years() == null || direction.getPhase1Years() <= 0) {
                throw new BusinessException("分段注册需要设置第一阶段学制");
            }
            if (!StringUtils.hasText(direction.getPhase2Level())) {
                throw new BusinessException("分段注册需要设置第二阶段层次");
            }
            if (direction.getPhase2Years() == null || direction.getPhase2Years() <= 0) {
                throw new BusinessException("分段注册需要设置第二阶段学制");
            }
        } else {
            // 非分段注册模式
            if (!StringUtils.hasText(direction.getLevel())) {
                throw new BusinessException("层次不能为空");
            }
            if (direction.getYears() == null || direction.getYears() <= 0) {
                throw new BusinessException("学制必须大于0");
            }
        }
    }
}
