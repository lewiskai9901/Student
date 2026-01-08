package com.school.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.dto.DormitoryCreateRequest;
import com.school.management.dto.DormitoryQueryRequest;
import com.school.management.dto.DormitoryResponse;
import com.school.management.dto.DormitoryUpdateRequest;
import com.school.management.entity.Dormitory;

import java.util.List;

/**
 * 宿舍服务接口
 *
 * @author system
 * @since 1.0.0
 */
public interface DormitoryService {

    /**
     * 创建宿舍
     *
     * @param request 创建请求
     * @return 宿舍ID
     */
    Long createDormitory(DormitoryCreateRequest request);

    /**
     * 更新宿舍
     *
     * @param request 更新请求
     */
    void updateDormitory(DormitoryUpdateRequest request);

    /**
     * 删除宿舍
     *
     * @param id 宿舍ID
     * @param force 是否强制删除(true时会清空宿舍内学生的宿舍信息)
     */
    void deleteDormitory(Long id, boolean force);

    /**
     * 批量删除宿舍
     *
     * @param ids 宿舍ID列表
     */
    void deleteDormitories(List<Long> ids);

    /**
     * 根据ID获取宿舍
     *
     * @param id 宿舍ID
     * @return 宿舍信息
     */
    DormitoryResponse getDormitoryById(Long id);

    /**
     * 根据宿舍编号获取宿舍
     *
     * @param dormitoryNo 宿舍编号
     * @return 宿舍
     */
    Dormitory getDormitoryByNo(String dormitoryNo);

    /**
     * 分页查询宿舍
     *
     * @param request 查询请求
     * @return 分页结果
     */
    IPage<DormitoryResponse> getDormitoryPage(DormitoryQueryRequest request);

    /**
     * 根据楼宇ID查询宿舍列表
     *
     * @param buildingId 楼宇ID
     * @return 宿舍列表
     */
    List<DormitoryResponse> getDormitoriesByBuildingId(Long buildingId);

    /**
     * 根据性别类型查询宿舍列表
     *
     * @param genderType 性别类型
     * @return 宿舍列表
     */
    List<DormitoryResponse> getDormitoriesByGenderType(Integer genderType);

    /**
     * 根据部门ID查询宿舍列表
     *
     * @param departmentId 部门ID
     * @return 宿舍列表
     */
    List<DormitoryResponse> getDormitoriesByDepartmentId(Long departmentId);

    /**
     * 查询有空床位的宿舍
     *
     * @param genderType 性别类型
     * @return 宿舍列表
     */
    List<DormitoryResponse> getAvailableDormitories(Integer genderType);

    /**
     * 根据宿管员ID查询宿舍列表
     *
     * @param supervisorId 宿管员ID
     * @return 宿舍列表
     */
    List<DormitoryResponse> getDormitoriesBySupervisorId(Long supervisorId);

    /**
     * 获取所有正常状态的宿舍
     *
     * @return 宿舍列表
     */
    List<DormitoryResponse> getAllNormalDormitories();

    /**
     * 检查宿舍编号是否存在(在指定楼宇内)
     *
     * @param buildingId 楼宇ID
     * @param dormitoryNo 宿舍编号
     * @param excludeId 排除的ID
     * @return 是否存在
     */
    boolean existsDormitoryNo(Long buildingId, String dormitoryNo, Long excludeId);

    /**
     * 更新宿舍已占用床位数
     *
     * @param dormitoryId 宿舍ID
     * @param occupiedBeds 已占用床位数
     */
    void updateOccupiedBeds(Long dormitoryId, Integer occupiedBeds);

    /**
     * 更新宿舍状态
     *
     * @param id 宿舍ID
     * @param status 状态
     */
    void updateStatus(Long id, Integer status);

    /**
     * 分配床位
     *
     * @param dormitoryId 宿舍ID
     * @param studentId 学生ID
     */
    void assignBed(Long dormitoryId, Long studentId);

    /**
     * 释放床位
     *
     * @param dormitoryId 宿舍ID
     * @param studentId 学生ID
     */
    void releaseBed(Long dormitoryId, Long studentId);

    /**
     * 获取宿舍床位分配情况
     *
     * @param dormitoryId 宿舍ID
     * @return 床位分配列表
     */
    List<com.school.management.dto.BedAllocationResponse> getBedAllocations(Long dormitoryId);

    /**
     * 批量生成宿舍
     *
     * @param request 批量生成请求
     * @return 生成的宿舍数量
     */
    int batchCreateDormitories(com.school.management.dto.DormitoryBatchCreateRequest request);

    /**
     * 批量更新宿舍房间的院系分配
     *
     * @param dormitoryIds 宿舍ID列表
     * @param departmentId 院系ID（null表示取消分配）
     * @return 更新的宿舍数量
     */
    int batchUpdateDepartment(List<Long> dormitoryIds, Long departmentId);

    /**
     * 按楼层批量更新院系分配
     *
     * @param buildingId 楼宇ID
     * @param floor 楼层号
     * @param departmentId 院系ID（null表示取消分配）
     * @return 更新的宿舍数量
     */
    int batchUpdateDepartmentByFloor(Long buildingId, Integer floor, Long departmentId);
}