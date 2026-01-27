package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.dto.DormitoryQueryRequest;
import com.school.management.dto.DormitoryResponse;
import com.school.management.entity.Dormitory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 宿舍Mapper接口
 *
 * @author system
 * @since 1.0.0
 */
@Mapper
public interface DormitoryMapper extends BaseMapper<Dormitory> {

    /**
     * 分页查询宿舍
     *
     * @param page 分页参数
     * @param request 查询条件
     * @return 宿舍列表
     */
    IPage<DormitoryResponse> selectDormitoryPage(Page<DormitoryResponse> page,
                                                @Param("req") DormitoryQueryRequest request);

    /**
     * 根据ID获取宿舍详情(包含关联数据)
     *
     * @param id 宿舍ID
     * @return 宿舍详情
     */
    DormitoryResponse selectDormitoryDetailById(@Param("id") Long id);

    /**
     * 根据宿舍编号获取宿舍
     *
     * @param dormitoryNo 宿舍编号
     * @return 宿舍
     */
    Dormitory selectByDormitoryNo(@Param("dormitoryNo") String dormitoryNo);

    /**
     * 检查宿舍编号是否存在(在指定楼宇内)
     *
     * @param buildingId 楼宇ID
     * @param dormitoryNo 宿舍编号
     * @param excludeId 排除的ID
     * @return 数量
     */
    Integer countByDormitoryNo(@Param("buildingId") Long buildingId, @Param("dormitoryNo") String dormitoryNo, @Param("excludeId") Long excludeId);

    /**
     * 根据楼栋ID查询宿舍列表
     *
     * @param buildingId 楼栋ID
     * @return 宿舍列表
     */
    List<DormitoryResponse> selectByBuildingId(@Param("buildingId") Long buildingId);

    /**
     * 根据性别类型查询宿舍列表
     *
     * @param genderType 性别类型
     * @return 宿舍列表
     */
    List<DormitoryResponse> selectByGenderType(@Param("genderType") Integer genderType);

    /**
     * 根据组织单元ID查询宿舍列表
     *
     * @param orgUnitId 组织单元ID
     * @return 宿舍列表
     */
    List<DormitoryResponse> selectByOrgUnitId(@Param("orgUnitId") Long orgUnitId);

    /**
     * 查询有空床位的宿舍
     *
     * @param genderType 性别类型
     * @return 宿舍列表
     */
    List<DormitoryResponse> selectAvailableDormitories(@Param("genderType") Integer genderType);

    /**
     * 根据宿管员ID查询宿舍列表
     *
     * @param supervisorId 宿管员ID
     * @return 宿舍列表
     */
    List<DormitoryResponse> selectBySupervisorId(@Param("supervisorId") Long supervisorId);

    /**
     * 获取所有正常状态的宿舍
     *
     * @return 宿舍列表
     */
    List<DormitoryResponse> selectAllNormal();

    /**
     * 更新宿舍已占用床位数
     *
     * @param dormitoryId 宿舍ID
     * @param occupiedBeds 已占用床位数
     * @return 更新数量
     */
    Integer updateOccupiedBeds(@Param("dormitoryId") Long dormitoryId, @Param("occupiedBeds") Integer occupiedBeds);

    /**
     * 统计宿舍楼内学生总数
     *
     * @param buildingId 宿舍楼ID
     * @return 学生总数
     */
    int countStudentsByBuildingId(@Param("buildingId") Long buildingId);

    /**
     * 删除宿舍楼下的所有房间
     *
     * @param buildingId 宿舍楼ID
     */
    void deleteByBuildingId(@Param("buildingId") Long buildingId);
}