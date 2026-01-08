package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.common.result.ResultCode;
import com.school.management.dto.DormitoryCreateRequest;
import com.school.management.dto.DormitoryQueryRequest;
import com.school.management.dto.DormitoryResponse;
import com.school.management.dto.DormitoryUpdateRequest;
import com.school.management.entity.*;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.*;
import com.school.management.service.BuildingDormitoryService;
import com.school.management.service.DormitoryService;
import com.school.management.annotation.DataPermission;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 宿舍服务实现类
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DormitoryServiceImpl extends ServiceImpl<DormitoryMapper, Dormitory> implements DormitoryService {

    private final DormitoryMapper dormitoryMapper;
    private final StudentMapper studentMapper;
    private final UserMapper userMapper;
    private final ClassMapper classMapper;
    private final BuildingMapper buildingMapper;
    private final BuildingDormitoryService buildingDormitoryService;
    private final BuildingDepartmentMapper buildingDepartmentMapper;
    private final MajorMapper majorMapper;
    private final UserRoleMapper userRoleMapper;
    private final ClassDormitoryBindingMapper classDormitoryBindingMapper;

    /** 班主任角色ID */
    private static final Long CLASS_TEACHER_ROLE_ID = 5L;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDormitory(DormitoryCreateRequest request) {
        log.info("创建宿舍: {}", request.getDormitoryNo());

        // 检查宿舍编号是否存在(在指定楼宇内)
        if (existsDormitoryNo(request.getBuildingId(), request.getDormitoryNo(), null)) {
            throw new BusinessException(ResultCode.DATA_EXISTS, "该宿舍楼内已存在相同房间号");
        }

        // 1. 验证楼宇是否为宿舍楼
        Building building = buildingMapper.selectById(request.getBuildingId());
        if (building == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "楼宇不存在");
        }
        if (building.getBuildingType() != 2) {
            throw new BusinessException("只能在宿舍楼下创建宿舍");
        }

        // 2. 获取宿舍楼信息并验证
        BuildingDormitory buildingDormitory = buildingDormitoryService.getByBuildingId(request.getBuildingId());
        if (buildingDormitory.getDormitoryType() == null) {
            throw new BusinessException("请先在宿舍楼管理中设置宿舍楼类型");
        }

        // 3. 验证房间用途类型是否符合宿舍楼类型
        validateRoomUsageType(request.getRoomUsageType(), buildingDormitory.getDormitoryType());

        // 4. 根据宿舍楼类型自动确定性别类型(性别类型从宿舍楼继承,前端不传)
        Integer genderType = determineGenderType(buildingDormitory.getDormitoryType());

        // 5. 根据房间用途类型和床位容量确定实际床位数
        Integer bedCount = determineBedCount(request.getRoomUsageType(), request.getBedCapacity());

        // 6. 创建宿舍
        Dormitory dormitory = new Dormitory();
        dormitory.setDormitoryNo(request.getDormitoryNo());
        dormitory.setBuildingId(request.getBuildingId());
        dormitory.setFloorNumber(request.getFloorNumber());
        dormitory.setRoomUsageType(request.getRoomUsageType());
        dormitory.setBedCapacity(request.getBedCapacity());
        dormitory.setBedCount(bedCount);
        dormitory.setOccupiedBeds(0); // 初始已占用床位为0
        dormitory.setGenderType(genderType); // 自动继承
        dormitory.setFacilities(request.getFacilities());
        dormitory.setNotes(request.getNotes());
        dormitory.setStatus(request.getStatus());

        dormitoryMapper.insert(dormitory);

        // 5. 更新宿舍楼房间统计和床位统计
        buildingDormitoryService.incrementRoomCount(request.getBuildingId());
        buildingDormitoryService.incrementBedCount(request.getBuildingId(), bedCount);

        log.info("宿舍创建成功: {} - {}", dormitory.getId(), request.getDormitoryNo());
        return dormitory.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDormitory(DormitoryUpdateRequest request) {
        log.info("更新宿舍: {}", request.getId());

        Dormitory dormitory = dormitoryMapper.selectById(request.getId());
        if (dormitory == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "宿舍不存在");
        }

        // 检查宿舍编号是否重复(在指定楼宇内)
        if (!dormitory.getDormitoryNo().equals(request.getDormitoryNo())
                && existsDormitoryNo(request.getBuildingId(), request.getDormitoryNo(), request.getId())) {
            throw new BusinessException(ResultCode.DATA_EXISTS, "该宿舍楼内已存在相同房间号");
        }

        // 获取宿舍楼信息
        BuildingDormitory buildingDormitory = buildingDormitoryService.getByBuildingId(request.getBuildingId());

        // 验证房间用途类型
        validateRoomUsageType(request.getRoomUsageType(), buildingDormitory.getDormitoryType());

        // 自动确定性别类型
        Integer genderType = determineGenderType(buildingDormitory.getDormitoryType());

        // 确定床位数
        Integer bedCount = determineBedCount(request.getRoomUsageType(), request.getBedCapacity());

        // 更新宿舍信息
        dormitory.setBuildingId(request.getBuildingId());
        dormitory.setDormitoryNo(request.getDormitoryNo());
        dormitory.setFloorNumber(request.getFloorNumber());
        dormitory.setRoomUsageType(request.getRoomUsageType());
        dormitory.setBedCapacity(request.getBedCapacity());
        dormitory.setBedCount(bedCount);
        dormitory.setGenderType(genderType);
        dormitory.setNotes(request.getNotes());
        dormitory.setStatus(request.getStatus());
        dormitory.setUpdatedAt(LocalDateTime.now());

        dormitoryMapper.updateById(dormitory);

        log.info("宿舍更新成功: {}", request.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDormitory(Long id, boolean force) {
        log.info("删除宿舍: {}, 强制删除: {}", id, force);

        Dormitory dormitory = dormitoryMapper.selectById(id);
        if (dormitory == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "宿舍不存在");
        }

        // 检查宿舍下是否有学生
        if (dormitory.getOccupiedBeds() != null && dormitory.getOccupiedBeds() > 0) {
            if (!force) {
                // 不是强制删除,抛出异常
                throw new BusinessException(ResultCode.OPERATION_NOT_ALLOWED,
                    "宿舍内还有 " + dormitory.getOccupiedBeds() + " 名学生，是否强制删除？");
            } else {
                // 强制删除,清空该宿舍内所有学生的宿舍信息
                log.info("强制删除宿舍，清空宿舍内学生的宿舍信息: 宿舍ID={}", id);
                studentMapper.clearDormitoryByDormitoryId(id);
            }
        }

        // 逻辑删除 - 使用removeById方法
        dormitoryMapper.deleteById(dormitory.getId());

        // 更新宿舍楼统计数据(房间数和床位数)
        try {
            buildingDormitoryService.decrementRoomCount(dormitory.getBuildingId());
            buildingDormitoryService.decrementBedCount(dormitory.getBuildingId(), dormitory.getBedCount());
            log.info("已减少宿舍楼 {} 的房间统计数和床位数", dormitory.getBuildingId());
        } catch (Exception e) {
            log.warn("更新宿舍楼统计数据失败: {}", e.getMessage());
        }

        log.info("宿舍删除成功: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDormitories(List<Long> ids) {
        log.info("批量删除宿舍: {}", ids);

        for (Long id : ids) {
            deleteDormitory(id, false);
        }
    }

    @Override
    public DormitoryResponse getDormitoryById(Long id) {
        // 使用mapper查询获取完整数据(包含关联的building信息)
        DormitoryResponse response = dormitoryMapper.selectDormitoryDetailById(id);
        if (response == null) {
            throw new BusinessException("宿舍不存在");
        }

        // 填充额外字段
        enrichDormitoryResponse(response);

        return response;
    }

    private String getRoomTypeName(Integer roomType) {
        if (roomType == null) return "未知";
        switch (roomType) {
            case 1: return "四人间";
            case 2: return "六人间";
            case 3: return "八人间";
            default: return "未知";
        }
    }

    private String getGenderTypeName(Integer genderType) {
        if (genderType == null) return "未知";
        switch (genderType) {
            case 1: return "男生宿舍";
            case 2: return "女生宿舍";
            case 3: return "混合宿舍";
            default: return "未知";
        }
    }

    private String getStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 1: return "正常";
            case 2: return "维修";
            case 3: return "停用";
            default: return "未知";
        }
    }

    private String getDormitoryTypeName(Integer dormitoryType) {
        if (dormitoryType == null) return "学生宿舍";
        switch (dormitoryType) {
            case 1: return "学生宿舍";
            case 2: return "职工宿舍";
            case 3: return "其他宿舍";
            default: return "学生宿舍";
        }
    }

    @Override
    public Dormitory getDormitoryByNo(String dormitoryNo) {
        return dormitoryMapper.selectByDormitoryNo(dormitoryNo);
    }

    @Override
    @DataPermission(module = "dormitory", classField = "classIds")
    public IPage<DormitoryResponse> getDormitoryPage(DormitoryQueryRequest request) {
        // 数据权限过滤由AOP自动处理
        Page<DormitoryResponse> page = new Page<>(request.getPageNum(), request.getPageSize());
        IPage<DormitoryResponse> result = dormitoryMapper.selectDormitoryPage(page, request);

        // 填充额外字段
        result.getRecords().forEach(this::enrichDormitoryResponse);

        return result;
    }

    /**
     * 丰富宿舍响应数据
     */
    private void enrichDormitoryResponse(DormitoryResponse response) {
        // 设置前端需要的字段名
        response.setRoomNo(response.getDormitoryNo());
        response.setFloor(response.getFloorNumber());

        // 设置名称字段
        response.setRoomTypeName(getRoomTypeName(response.getRoomType()));
        response.setGenderTypeName(getGenderTypeName(response.getGenderType()));
        response.setStatusName(getStatusName(response.getStatus()));
        response.setDormitoryTypeName(getDormitoryTypeName(response.getDormitoryType()));

        // 使用实际的床位数(从maxCapacity获取,即数据库的bed_count)
        // 如果maxCapacity不为空,直接使用;否则使用废弃的roomType计算(向后兼容)
        Integer maxOccupancy = response.getMaxCapacity() != null
            ? response.getMaxCapacity()
            : getMaxOccupancyByRoomType(response.getRoomType());
        response.setMaxOccupancy(maxOccupancy);

        // 查询该宿舍的学生列表
        List<com.school.management.entity.Student> students = studentMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.school.management.entity.Student>()
                .eq(com.school.management.entity.Student::getDormitoryId, response.getId())
                .eq(com.school.management.entity.Student::getDeleted, 0)
        );

        // 设置当前入住人数
        response.setCurrentOccupancy(students.size());

        // 构建学生简单信息列表
        List<DormitoryResponse.StudentSimpleInfo> studentInfoList = new java.util.ArrayList<>();
        for (com.school.management.entity.Student student : students) {
            DormitoryResponse.StudentSimpleInfo info = new DormitoryResponse.StudentSimpleInfo();
            info.setId(student.getId());
            info.setStudentNo(student.getStudentNo());
            info.setBedNumber(student.getBedNumber());

            // 获取学生姓名
            com.school.management.entity.User user = userMapper.selectById(student.getUserId());
            if (user != null) {
                info.setRealName(user.getRealName());
            }

            // 获取班级名称
            if (student.getClassId() != null) {
                com.school.management.entity.Class classInfo = classMapper.selectById(student.getClassId());
                if (classInfo != null) {
                    info.setClassName(classInfo.getClassName());
                }
            }

            studentInfoList.add(info);
        }

        response.setStudents(studentInfoList);
    }

    /**
     * 根据房间类型获取最大容纳人数 (已废弃)
     */
    @Deprecated
    private Integer getMaxOccupancyByRoomType(Integer roomType) {
        if (roomType == null) return 4;
        switch (roomType) {
            case 1: return 4;  // 四人间
            case 2: return 6;  // 六人间
            case 3: return 8;  // 八人间
            default: return 4;
        }
    }

    /**
     * 验证房间用途类型是否符合宿舍楼类型
     * @param roomUsageType 房间用途类型: 1学生宿舍 2教职工宿舍 3配电室 4卫生间 5杂物间 6其他
     * @param dormitoryType 宿舍楼类型: 1男生楼 2女生楼 3教职工男生楼 4教职工女生楼 5教职工混合楼
     */
    private void validateRoomUsageType(Integer roomUsageType, Integer dormitoryType) {
        if (roomUsageType == null || dormitoryType == null) {
            throw new BusinessException("房间用途类型和宿舍楼类型不能为空");
        }

        // 根据宿舍楼类型限制房间用途
        switch (dormitoryType) {
            case 1: // 男生宿舍楼
            case 2: // 女生宿舍楼
                // 只允许: 学生宿舍、配电室、卫生间、杂物间、其他
                if (roomUsageType == 2) { // 教职工宿舍
                    throw new BusinessException("学生宿舍楼下不能创建教职工宿舍");
                }
                break;
            case 3: // 教职工男生宿舍楼
            case 4: // 教职工女生宿舍楼
            case 5: // 教职工混合宿舍楼
                // 只允许: 教职工宿舍、配电室、卫生间、杂物间、其他
                if (roomUsageType == 1) { // 学生宿舍
                    throw new BusinessException("教职工宿舍楼下不能创建学生宿舍");
                }
                break;
            default:
                throw new BusinessException("未知的宿舍楼类型");
        }
    }

    /**
     * 根据宿舍楼类型自动确定房间的性别类型
     * @param dormitoryType 宿舍楼类型
     * @return 性别类型: 1男 2女 3混合
     */
    private Integer determineGenderType(Integer dormitoryType) {
        if (dormitoryType == null) {
            return 1; // 默认男生
        }
        switch (dormitoryType) {
            case 1: // 男生宿舍楼
            case 3: // 教职工男生楼
                return 1; // 男
            case 2: // 女生宿舍楼
            case 4: // 教职工女生楼
                return 2; // 女
            case 5: // 教职工混合楼
                return 3; // 混合
            default:
                return 1;
        }
    }

    /**
     * 根据房间用途类型和床位容量确定实际床位数
     * @param roomUsageType 房间用途类型
     * @param bedCapacity 床位容量规格
     * @return 实际床位数
     */
    private Integer determineBedCount(Integer roomUsageType, Integer bedCapacity) {
        if (roomUsageType == null) {
            return 0;
        }
        // 只有学生宿舍和教职工宿舍才有床位
        if (roomUsageType == 1 || roomUsageType == 2) {
            return bedCapacity != null ? bedCapacity : 4;
        }
        // 配电室、卫生间、杂物间等没有床位
        return 0;
    }

    @Override
    public List<DormitoryResponse> getDormitoriesByBuildingId(Long buildingId) {
        return dormitoryMapper.selectByBuildingId(buildingId);
    }

    @Override
    public List<DormitoryResponse> getDormitoriesByGenderType(Integer genderType) {
        return dormitoryMapper.selectByGenderType(genderType);
    }

    @Override
    public List<DormitoryResponse> getDormitoriesByDepartmentId(Long departmentId) {
        log.info("根据部门ID查询宿舍列表: {}", departmentId);
        return dormitoryMapper.selectByDepartmentId(departmentId);
    }

    @Override
    public List<DormitoryResponse> getAvailableDormitories(Integer genderType) {
        return dormitoryMapper.selectAvailableDormitories(genderType);
    }

    @Override
    @Deprecated
    public List<DormitoryResponse> getDormitoriesBySupervisorId(Long supervisorId) {
        // 已废弃: 宿管员管理已移至宿舍楼级别,不再支持房间级别的宿管员
        return new java.util.ArrayList<>();
    }

    @Override
    public List<DormitoryResponse> getAllNormalDormitories() {
        return dormitoryMapper.selectAllNormal();
    }

    @Override
    public boolean existsDormitoryNo(Long buildingId, String dormitoryNo, Long excludeId) {
        return dormitoryMapper.countByDormitoryNo(buildingId, dormitoryNo, excludeId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOccupiedBeds(Long dormitoryId, Integer occupiedBeds) {
        log.info("更新宿舍已占用床位数: {} -> {}", dormitoryId, occupiedBeds);

        Dormitory dormitory = dormitoryMapper.selectById(dormitoryId);
        if (dormitory == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "宿舍不存在");
        }

        dormitoryMapper.updateOccupiedBeds(dormitoryId, occupiedBeds);

        log.info("宿舍已占用床位数更新成功: {}", dormitoryId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        log.info("更新宿舍状态: {} -> {}", id, status);

        Dormitory dormitory = dormitoryMapper.selectById(id);
        if (dormitory == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "宿舍不存在");
        }

        dormitory.setStatus(status);
        dormitory.setUpdatedAt(LocalDateTime.now());
        dormitoryMapper.updateById(dormitory);

        log.info("宿舍状态更新成功: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignBed(Long dormitoryId, Long studentId) {
        log.info("分配床位: 宿舍ID={}, 学生ID={}", dormitoryId, studentId);

        // 1. 验证宿舍是否存在
        Dormitory dormitory = dormitoryMapper.selectById(dormitoryId);
        if (dormitory == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "宿舍不存在");
        }

        // 2. 验证宿舍状态
        if (dormitory.getStatus() != 1) {
            throw new BusinessException(ResultCode.OPERATION_NOT_ALLOWED, "宿舍状态异常，无法分配床位");
        }

        // 3. 验证学生是否存在
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "学生不存在");
        }

        // 3.1 验证班主任权限：班主任只能分配自己班级的学生
        validateClassTeacherPermission(student);

        // 3.2 验证学生班级与宿舍绑定关系：只能将学生分配到其班级绑定的宿舍
        validateClassDormitoryBinding(dormitoryId, student);

        // 4. 验证学生是否已有宿舍
        if (student.getDormitoryId() != null) {
            throw new BusinessException(ResultCode.OPERATION_NOT_ALLOWED, "该学生已分配宿舍，请先释放原床位");
        }

        // 5. 获取学生性别
        User user = userMapper.selectById(student.getUserId());
        if (user == null || user.getGender() == null) {
            throw new BusinessException(ResultCode.DATA_INVALID, "学生信息不完整，缺少性别信息");
        }
        Integer studentGender = user.getGender();

        // 6. 获取楼宇信息
        Building building = buildingMapper.selectById(dormitory.getBuildingId());
        if (building == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "楼宇信息不存在");
        }

        // 7. 验证楼宇必须是宿舍楼 (building_type: 1=教学楼, 2=宿舍楼)
        if (building.getBuildingType() != 2) {
            throw new BusinessException(ResultCode.OPERATION_NOT_ALLOWED, "只能分配学生到宿舍楼");
        }

        // 8. 获取宿舍楼信息
        BuildingDormitory buildingDormitory = buildingDormitoryService.getByBuildingId(building.getId());
        if (buildingDormitory == null || buildingDormitory.getDormitoryType() == null) {
            throw new BusinessException(ResultCode.DATA_INVALID, "宿舍楼类型未设置，请联系管理员");
        }

        // 9. 验证宿舍楼类型：只能分配学生到学生宿舍楼(1=男生宿舍楼, 2=女生宿舍楼)
        Integer dormitoryType = buildingDormitory.getDormitoryType();
        if (dormitoryType != 1 && dormitoryType != 2) {
            String typeName = buildingDormitory.getDormitoryTypeName();
            throw new BusinessException(ResultCode.OPERATION_NOT_ALLOWED,
                "不能将学生分配到" + typeName + "，学生只能分配到男生宿舍楼或女生宿舍楼");
        }

        // 10. 验证学生性别与宿舍楼性别类型是否匹配
        if (dormitoryType == 1 && studentGender != 1) {
            throw new BusinessException(ResultCode.OPERATION_NOT_ALLOWED, "女生不能分配到男生宿舍楼");
        }
        if (dormitoryType == 2 && studentGender != 2) {
            throw new BusinessException(ResultCode.OPERATION_NOT_ALLOWED, "男生不能分配到女生宿舍楼");
        }

        // 11. 验证房间用途类型：只能分配学生到学生宿舍(room_usage_type=1)
        if (dormitory.getRoomUsageType() != 1) {
            String roomUsageName = getRoomUsageTypeName(dormitory.getRoomUsageType());
            throw new BusinessException(ResultCode.OPERATION_NOT_ALLOWED,
                "该房间为" + roomUsageName + "，不能分配学生入住");
        }

        // 12. 验证床位是否已满
        Integer occupiedBeds = dormitory.getOccupiedBeds() != null ? dormitory.getOccupiedBeds() : 0;
        Integer bedCapacity = dormitory.getBedCapacity() != null ? dormitory.getBedCapacity() : 0;
        if (occupiedBeds >= bedCapacity) {
            throw new BusinessException(ResultCode.OPERATION_NOT_ALLOWED, "该宿舍床位已满，无法继续分配");
        }

        // 13. 更新学生的宿舍信息(此处应该调用StudentService,但为了事务一致性直接操作)
        // 注意: 实际应该有bedNumber参数,这里暂时不设置,由前端或其他逻辑补充

        // 14. 增加已占用床位数
        Integer newOccupiedBeds = occupiedBeds + 1;
        dormitoryMapper.updateOccupiedBeds(dormitoryId, newOccupiedBeds);

        log.info("床位分配成功: 宿舍ID={}, 学生ID={}, 楼宇={}, 宿舍楼类型={}, 学生性别={}",
            dormitoryId, studentId, building.getBuildingName(),
            buildingDormitory.getDormitoryTypeName(), studentGender == 1 ? "男" : "女");
    }

    /**
     * 获取房间用途类型名称
     */
    private String getRoomUsageTypeName(Integer roomUsageType) {
        if (roomUsageType == null) {
            return "未知";
        }
        switch (roomUsageType) {
            case 1: return "学生宿舍";
            case 2: return "教职工宿舍";
            case 3: return "配电室";
            case 4: return "卫生间";
            case 5: return "杂物间";
            case 6: return "其他";
            default: return "未知";
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseBed(Long dormitoryId, Long studentId) {
        log.info("释放床位: 宿舍ID={}, 学生ID={}", dormitoryId, studentId);

        Dormitory dormitory = dormitoryMapper.selectById(dormitoryId);
        if (dormitory == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "宿舍不存在");
        }

        if (dormitory.getOccupiedBeds() <= 0) {
            throw new BusinessException(ResultCode.OPERATION_NOT_ALLOWED, "宿舍没有已占用的床位");
        }

        // 减少已占用床位数
        Integer newOccupiedBeds = dormitory.getOccupiedBeds() - 1;
        dormitoryMapper.updateOccupiedBeds(dormitoryId, newOccupiedBeds);

        log.info("床位释放成功: 宿舍ID={}, 学生ID={}", dormitoryId, studentId);
    }

    @Override
    public List<com.school.management.dto.BedAllocationResponse> getBedAllocations(Long dormitoryId) {
        log.info("获取宿舍床位分配情况: {}", dormitoryId);

        // 查询宿舍信息
        Dormitory dormitory = dormitoryMapper.selectById(dormitoryId);
        if (dormitory == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "宿舍不存在");
        }

        // 查询该宿舍的所有学生
        List<com.school.management.entity.Student> students = studentMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.school.management.entity.Student>()
                .eq(com.school.management.entity.Student::getDormitoryId, dormitoryId)
                .eq(com.school.management.entity.Student::getDeleted, 0)
        );

        // 构建床位分配响应列表
        List<com.school.management.dto.BedAllocationResponse> allocations = new java.util.ArrayList<>();

        for (com.school.management.entity.Student student : students) {
            com.school.management.dto.BedAllocationResponse allocation = new com.school.management.dto.BedAllocationResponse();
            allocation.setBedNumber(student.getBedNumber());
            allocation.setStudentId(student.getId());
            allocation.setStudentNo(student.getStudentNo());
            allocation.setIsAssigned(true);

            // 获取学生姓名
            com.school.management.entity.User user = userMapper.selectById(student.getUserId());
            if (user != null) {
                allocation.setStudentName(user.getRealName());
            }

            // 获取班级信息
            if (student.getClassId() != null) {
                com.school.management.entity.Class classInfo = classMapper.selectById(student.getClassId());
                if (classInfo != null) {
                    allocation.setClassId(classInfo.getId());
                    allocation.setClassName(classInfo.getClassName());

                    // 获取班主任信息
                    if (classInfo.getTeacherId() != null) {
                        com.school.management.entity.User teacher = userMapper.selectById(classInfo.getTeacherId());
                        if (teacher != null) {
                            allocation.setTeacherId(teacher.getId());
                            allocation.setTeacherName(teacher.getRealName());
                        }
                    }
                }
            }

            allocations.add(allocation);
        }

        return allocations;
    }

    /**
     * 批量生成宿舍
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchCreateDormitories(com.school.management.dto.DormitoryBatchCreateRequest request) {
        log.info("开始批量生成宿舍: buildingId={}, floors={}-{}, roomsPerFloor={}",
            request.getBuildingId(), request.getStartFloor(), request.getEndFloor(), request.getRoomsPerFloor());

        // 1. 验证楼宇是否存在
        com.school.management.entity.Building building = buildingMapper.selectById(request.getBuildingId());
        if (building == null) {
            throw new BusinessException("楼宇不存在");
        }

        // 2. 计算床位数
        int bedCount;
        switch (request.getRoomType()) {
            case 1: bedCount = 4; break;  // 四人间
            case 2: bedCount = 6; break;  // 六人间
            case 3: bedCount = 8; break;  // 八人间
            default: throw new BusinessException("无效的房间类型");
        }

        // 3. 批量生成宿舍
        int createdCount = 0;
        for (int floor = request.getStartFloor(); floor <= request.getEndFloor(); floor++) {
            for (int roomNum = 1; roomNum <= request.getRoomsPerFloor(); roomNum++) {
                // 生成房间编号
                String dormitoryNo;
                if (request.getNumberFormat() == 1) {
                    // 格式1: 楼层+房间号 (如101, 102, 201, 202)
                    dormitoryNo = String.format("%d%02d", floor, roomNum);
                } else if (request.getNumberFormat() == 2) {
                    // 格式2: 楼层+0+房间号 (如1001, 1002, 2001, 2002)
                    dormitoryNo = String.format("%d%03d", floor, roomNum);
                } else {
                    throw new BusinessException("无效的房间编号格式");
                }

                // 检查房间号是否已存在
                if (existsDormitoryNo(request.getBuildingId(), dormitoryNo, null)) {
                    log.warn("房间号已存在,跳过: {}", dormitoryNo);
                    continue;
                }

                // 创建宿舍实体
                Dormitory dormitory = new Dormitory();
                dormitory.setBuildingId(request.getBuildingId());
                dormitory.setDormitoryNo(dormitoryNo);
                dormitory.setFloorNumber(floor);
                dormitory.setRoomType(request.getRoomType());
                dormitory.setBedCount(bedCount);
                dormitory.setOccupiedBeds(0);
                dormitory.setGenderType(request.getGenderType());
                dormitory.setFacilities(request.getFacilities());
                dormitory.setStatus(request.getStatus());
                dormitory.setDeleted(0);
                dormitory.setCreatedAt(java.time.LocalDateTime.now());
                dormitory.setUpdatedAt(java.time.LocalDateTime.now());

                // 保存到数据库
                dormitoryMapper.insert(dormitory);
                createdCount++;
            }
        }

        log.info("批量生成宿舍完成: 共创建{}个宿舍", createdCount);
        return createdCount;
    }

    /**
     * 验证学生是否可以分配到指定楼宇
     */
    public boolean canAssignStudentToBuilding(Long studentId, Long buildingId) {
        // 1. 获取学生信息
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "学生不存在");
        }

        // 2. 获取班级信息
        com.school.management.entity.Class clazz = classMapper.selectById(student.getClassId());
        if (clazz == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "学生未关联班级");
        }

        // 3. 获取学生的有效部门列表
        List<Long> studentDeptIds = getStudentDepartmentIds(student, clazz);
        if (studentDeptIds.isEmpty()) {
            throw new BusinessException("学生未关联任何部门，无法分配宿舍");
        }

        // 4. 获取楼宇关联的部门列表
        List<Long> buildingDeptIds = buildingDepartmentMapper.selectDepartmentIdsByBuildingId(buildingId);
        if (buildingDeptIds.isEmpty()) {
            throw new BusinessException("该楼宇未关联任何部门，请先配置楼宇部门");
        }

        // 5. 判断是否有交集
        return studentDeptIds.stream().anyMatch(buildingDeptIds::contains);
    }

    /**
     * 获取学生的有效部门ID列表
     * 优先级：班级部门 + 专业部门
     */
    private List<Long> getStudentDepartmentIds(Student student, com.school.management.entity.Class clazz) {
        List<Long> deptIds = new ArrayList<>();

        // 1. 班级部门
        if (clazz.getDepartmentId() != null) {
            deptIds.add(clazz.getDepartmentId());
        }

        // 2. 专业部门
        if (clazz.getMajorId() != null) {
            Major major = majorMapper.selectById(clazz.getMajorId());
            if (major != null && major.getDepartmentId() != null) {
                if (!deptIds.contains(major.getDepartmentId())) {
                    deptIds.add(major.getDepartmentId());
                }
            }
        }

        return deptIds;
    }

    /**
     * 验证班主任权限
     * 如果当前用户是班主任,则只能操作自己班级的学生
     * @param student 学生信息
     */
    private void validateClassTeacherPermission(Student student) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return;
        }

        // 检查当前用户是否为班主任
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.school.management.entity.UserRole> roleWrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        roleWrapper.eq(com.school.management.entity.UserRole::getUserId, currentUserId)
                   .eq(com.school.management.entity.UserRole::getRoleId, CLASS_TEACHER_ROLE_ID);

        long roleCount = userRoleMapper.selectCount(roleWrapper);
        if (roleCount == 0) {
            // 不是班主任,无需验证
            return;
        }

        // 是班主任,验证学生是否在自己管理的班级中
        log.info("检测到班主任用户 {} 正在分配床位,验证学生班级权限", currentUserId);

        if (student.getClassId() == null) {
            throw new BusinessException(ResultCode.OPERATION_NOT_ALLOWED,
                "该学生未分配班级,班主任无法为其分配宿舍");
        }

        // 查询该班级的班主任
        com.school.management.entity.Class clazz = classMapper.selectById(student.getClassId());
        if (clazz == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "学生所在班级不存在");
        }

        if (clazz.getTeacherId() == null) {
            throw new BusinessException(ResultCode.OPERATION_NOT_ALLOWED,
                "该班级未指定班主任,请联系管理员");
        }

        if (!clazz.getTeacherId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.OPERATION_NOT_ALLOWED,
                String.format("您只能为自己班级的学生分配宿舍。该学生属于班级：%s", clazz.getClassName()));
        }

        log.info("班主任权限验证通过: 用户 {} 可以为班级 {} 的学生 {} 分配宿舍",
                currentUserId, clazz.getClassName(), student.getStudentNo());
    }

    /**
     * 验证学生班级与宿舍绑定关系
     * 只能将学生分配到其班级已绑定的宿舍
     * @param dormitoryId 宿舍ID
     * @param student 学生信息
     */
    private void validateClassDormitoryBinding(Long dormitoryId, Student student) {
        // 如果学生没有班级，则无法验证绑定关系
        if (student.getClassId() == null) {
            throw new BusinessException(ResultCode.OPERATION_NOT_ALLOWED,
                "该学生未分配班级，无法分配宿舍");
        }

        // 查询该宿舍绑定的所有班级
        List<ClassDormitoryBinding> bindings = classDormitoryBindingMapper.selectByDormitoryId(dormitoryId);

        // 如果宿舍没有绑定任何班级，不允许分配学生
        if (bindings == null || bindings.isEmpty()) {
            throw new BusinessException(ResultCode.OPERATION_NOT_ALLOWED,
                "该宿舍尚未绑定任何班级，请先在班级管理中为班级分配宿舍");
        }

        // 检查学生所在班级是否在绑定列表中
        boolean isClassBound = bindings.stream()
            .anyMatch(b -> b.getClassId() != null && b.getClassId().equals(student.getClassId()));

        if (!isClassBound) {
            // 获取学生班级名称
            com.school.management.entity.Class studentClass = classMapper.selectById(student.getClassId());
            String className = studentClass != null ? studentClass.getClassName() : "未知班级";

            throw new BusinessException(ResultCode.OPERATION_NOT_ALLOWED,
                String.format("学生所在班级「%s」未绑定到该宿舍，只能将学生分配到其班级绑定的宿舍", className));
        }

        log.info("班级-宿舍绑定验证通过: 学生 {} 所在班级ID {} 已绑定到宿舍 {}",
                student.getStudentNo(), student.getClassId(), dormitoryId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateDepartment(List<Long> dormitoryIds, Long departmentId) {
        if (dormitoryIds == null || dormitoryIds.isEmpty()) {
            return 0;
        }

        log.info("批量更新宿舍院系分配: dormitoryIds={}, departmentId={}", dormitoryIds, departmentId);

        int count = 0;
        for (Long dormitoryId : dormitoryIds) {
            Dormitory dormitory = dormitoryMapper.selectById(dormitoryId);
            if (dormitory != null && dormitory.getDeleted() == 0) {
                dormitory.setDepartmentId(departmentId);
                dormitory.setUpdatedAt(LocalDateTime.now());
                dormitoryMapper.updateById(dormitory);
                count++;
            }
        }

        log.info("批量更新宿舍院系分配完成: 更新数量={}", count);
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateDepartmentByFloor(Long buildingId, Integer floor, Long departmentId) {
        if (buildingId == null || floor == null) {
            return 0;
        }

        log.info("按楼层批量更新院系分配: buildingId={}, floor={}, departmentId={}",
                buildingId, floor, departmentId);

        // 查询该楼层的所有宿舍
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Dormitory> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(Dormitory::getBuildingId, buildingId)
               .eq(Dormitory::getFloorNumber, floor)
               .eq(Dormitory::getDeleted, 0);

        List<Dormitory> dormitories = dormitoryMapper.selectList(wrapper);
        if (dormitories.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (Dormitory dormitory : dormitories) {
            dormitory.setDepartmentId(departmentId);
            dormitory.setUpdatedAt(LocalDateTime.now());
            dormitoryMapper.updateById(dormitory);
            count++;
        }

        log.info("按楼层批量更新院系分配完成: 更新数量={}", count);
        return count;
    }
}