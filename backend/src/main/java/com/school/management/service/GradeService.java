package com.school.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.school.management.dto.GradeDTO;
import com.school.management.dto.request.GradeCreateRequest;
import com.school.management.dto.request.GradeUpdateRequest;
import com.school.management.dto.vo.GradeStatisticsVO;
import com.school.management.entity.Grade;

import java.util.List;

/**
 * 年级服务接口
 * 年级为全校共享资源，不再绑定特定院系
 *
 * @author system
 * @version 3.1.0
 * @since 2024-12-29
 */
public interface GradeService extends IService<Grade> {

    /**
     * 创建年级
     *
     * @param request 创建请求
     * @return 年级DTO
     */
    GradeDTO createGrade(GradeCreateRequest request);

    /**
     * 更新年级
     *
     * @param request 更新请求
     * @return 年级DTO
     */
    GradeDTO updateGrade(GradeUpdateRequest request);

    /**
     * 删除年级(逻辑删除)
     *
     * @param id 年级ID
     * @return 是否成功
     */
    boolean deleteGrade(Long id);

    /**
     * 分页查询年级列表
     *
     * @param pageNum 页码
     * @param pageSize 页大小
     * @param enrollmentYear 入学年份
     * @param status 状态
     * @param keyword 关键词
     * @return 年级分页
     */
    IPage<GradeDTO> listGrades(Integer pageNum, Integer pageSize,
                                Integer enrollmentYear,
                                Integer status, String keyword);

    /**
     * 根据ID查询年级详情
     *
     * @param id 年级ID
     * @return 年级DTO
     */
    GradeDTO getGradeById(Long id);

    /**
     * 根据年级编码查询年级
     *
     * @param gradeCode 年级编码
     * @return 年级DTO
     */
    GradeDTO getGradeByCode(String gradeCode);

    /**
     * 查询年级主任管理的年级列表
     *
     * @param directorId 年级主任ID
     * @return 年级列表
     */
    List<GradeDTO> listGradesByDirector(Long directorId);

    /**
     * 同步年级统计信息
     *
     * @param gradeId 年级ID,null表示同步所有年级
     * @return 是否成功
     */
    boolean syncGradeStatistics(Long gradeId);

    /**
     * 更新年级主任
     *
     * @param gradeId 年级ID
     * @param directorId 年级主任ID
     * @return 是否成功
     */
    boolean updateGradeDirector(Long gradeId, Long directorId);

    /**
     * 批量更新年级状态
     *
     * @param gradeIds 年级ID列表
     * @param status 状态
     * @return 是否成功
     */
    boolean batchUpdateStatus(List<Long> gradeIds, Integer status);

    /**
     * 获取年级统计信息
     *
     * @param gradeId 年级ID
     * @return 统计VO
     */
    GradeStatisticsVO getGradeStatistics(Long gradeId);

    /**
     * 检查年级是否可以删除
     *
     * @param gradeId 年级ID
     * @return 是否可以删除
     */
    boolean checkCanDelete(Long gradeId);

    /**
     * 实体转DTO
     *
     * @param grade 年级实体
     * @return 年级DTO
     */
    GradeDTO convertToDTO(Grade grade);

    /**
     * 实体列表转DTO列表
     *
     * @param grades 年级实体列表
     * @return 年级DTO列表
     */
    List<GradeDTO> convertToDTOList(List<Grade> grades);
}
