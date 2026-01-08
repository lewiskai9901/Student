package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.entity.GradeMajorDirection;
import com.school.management.entity.MajorDirection;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.GradeMajorDirectionMapper;
import com.school.management.mapper.MajorDirectionMapper;
import com.school.management.service.GradeMajorDirectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 学年专业方向关联服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GradeMajorDirectionServiceImpl implements GradeMajorDirectionService {

    private final GradeMajorDirectionMapper gradeMajorDirectionMapper;
    private final MajorDirectionMapper majorDirectionMapper;

    @Override
    public IPage<GradeMajorDirection> getGradeMajorDirectionPage(int pageNum, int pageSize, GradeMajorDirection params) {
        Page<GradeMajorDirection> page = new Page<>(pageNum, pageSize);
        return gradeMajorDirectionMapper.selectGradeMajorDirectionPage(page, params);
    }

    @Override
    public List<GradeMajorDirection> getByAcademicYear(Integer academicYear) {
        if (academicYear == null) {
            throw new BusinessException("学年不能为空");
        }
        return gradeMajorDirectionMapper.selectByAcademicYear(academicYear);
    }

    @Override
    public List<GradeMajorDirection> getByMajorDirectionId(Long majorDirectionId) {
        if (majorDirectionId == null) {
            throw new BusinessException("专业方向ID不能为空");
        }
        return gradeMajorDirectionMapper.selectByMajorDirectionId(majorDirectionId);
    }

    @Override
    public GradeMajorDirection getDetailById(Long id) {
        if (id == null) {
            throw new BusinessException("ID不能为空");
        }
        GradeMajorDirection detail = gradeMajorDirectionMapper.selectDetailById(id);
        if (detail == null) {
            throw new BusinessException("学年专业方向关联不存在");
        }
        return detail;
    }

    @Override
    public GradeMajorDirection getByYearAndDirection(Integer academicYear, Long majorDirectionId) {
        if (academicYear == null || majorDirectionId == null) {
            throw new BusinessException("学年和专业方向ID不能为空");
        }
        return gradeMajorDirectionMapper.selectByYearAndDirection(academicYear, majorDirectionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GradeMajorDirection addDirectionToYear(GradeMajorDirection gradeMajorDirection) {
        // 参数校验
        validateGradeMajorDirection(gradeMajorDirection);

        // 检查是否已存在
        GradeMajorDirection existing = gradeMajorDirectionMapper.selectByYearAndDirection(
            gradeMajorDirection.getAcademicYear(),
            gradeMajorDirection.getMajorDirectionId()
        );
        if (existing != null) {
            throw new BusinessException("该学年已添加此专业方向");
        }

        // 验证专业方向是否存在
        MajorDirection direction = majorDirectionMapper.selectDirectionById(gradeMajorDirection.getMajorDirectionId());
        if (direction == null) {
            throw new BusinessException("专业方向不存在");
        }

        gradeMajorDirectionMapper.insert(gradeMajorDirection);
        log.info("为学年添加专业方向成功: academicYear={}, directionId={}",
            gradeMajorDirection.getAcademicYear(), gradeMajorDirection.getMajorDirectionId());

        return gradeMajorDirection;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAddDirectionsToYear(Integer academicYear, List<Long> majorDirectionIds) {
        if (academicYear == null) {
            throw new BusinessException("学年不能为空");
        }
        if (majorDirectionIds == null || majorDirectionIds.isEmpty()) {
            throw new BusinessException("专业方向ID列表不能为空");
        }

        for (Long directionId : majorDirectionIds) {
            // 检查是否已存在
            GradeMajorDirection existing = gradeMajorDirectionMapper.selectByYearAndDirection(academicYear, directionId);
            if (existing != null) {
                log.warn("学年{}已存在专业方向{},跳过", academicYear, directionId);
                continue;
            }

            GradeMajorDirection gmd = new GradeMajorDirection();
            gmd.setAcademicYear(academicYear);
            gmd.setMajorDirectionId(directionId);
            addDirectionToYear(gmd);
        }

        log.info("批量为学年添加专业方向成功: academicYear={}, count={}", academicYear, majorDirectionIds.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGradeMajorDirection(Long id, GradeMajorDirection gradeMajorDirection) {
        if (id == null) {
            throw new BusinessException("ID不能为空");
        }

        GradeMajorDirection existing = gradeMajorDirectionMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("学年专业方向关联不存在");
        }

        gradeMajorDirection.setId(id);
        gradeMajorDirectionMapper.updateById(gradeMajorDirection);
        log.info("更新学年专业方向配置成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGradeMajorDirection(Long id) {
        if (id == null) {
            throw new BusinessException("ID不能为空");
        }

        GradeMajorDirection gmd = gradeMajorDirectionMapper.selectById(id);
        if (gmd == null) {
            throw new BusinessException("学年专业方向关联不存在");
        }

        gradeMajorDirectionMapper.deleteById(id);
        log.info("删除学年专业方向关联成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteGradeMajorDirections(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("ID列表不能为空");
        }

        for (Long id : ids) {
            deleteGradeMajorDirection(id);
        }
        log.info("批量删除学年专业方向关联成功: count={}", ids.size());
    }

    /**
     * 校验学年专业方向关联数据
     */
    private void validateGradeMajorDirection(GradeMajorDirection gmd) {
        if (gmd == null) {
            throw new BusinessException("学年专业方向关联数据不能为空");
        }
        if (gmd.getAcademicYear() == null) {
            throw new BusinessException("学年不能为空");
        }
        if (gmd.getMajorDirectionId() == null) {
            throw new BusinessException("专业方向ID不能为空");
        }
    }
}
