package com.school.management.domain.asset.repository;

import com.school.management.domain.asset.model.aggregate.Dormitory;
import com.school.management.domain.asset.model.valueobject.DormitoryStatus;
import com.school.management.domain.asset.model.valueobject.GenderType;
import com.school.management.domain.shared.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 宿舍仓储接口
 */
public interface DormitoryRepository extends Repository<Dormitory, Long> {

    /**
     * 根据宿舍号查询
     */
    Optional<Dormitory> findByDormitoryNo(String dormitoryNo);

    /**
     * 根据楼宇ID查询
     */
    List<Dormitory> findByBuildingId(Long buildingId);

    /**
     * 根据楼宇ID和楼层查询
     */
    List<Dormitory> findByBuildingIdAndFloor(Long buildingId, Integer floorNumber);

    /**
     * 根据部门ID查询
     */
    List<Dormitory> findByDepartmentId(Long departmentId);

    /**
     * 查询有空床位的宿舍
     */
    List<Dormitory> findAvailable(Long buildingId, GenderType genderType);

    /**
     * 检查宿舍号是否存在
     */
    boolean existsByDormitoryNo(String dormitoryNo);

    /**
     * 统计楼宇的宿舍数量
     */
    long countByBuildingId(Long buildingId);

    /**
     * 统计部门的宿舍数量
     */
    long countByDepartmentId(Long departmentId);

    /**
     * 分页查询
     */
    List<Dormitory> findByPage(DormitoryQueryCriteria criteria, int pageNum, int pageSize);

    /**
     * 统计符合条件的数量
     */
    long countByCriteria(DormitoryQueryCriteria criteria);

    /**
     * 查询条件
     */
    class DormitoryQueryCriteria {
        private String keyword;
        private Long buildingId;
        private Long departmentId;
        private Integer floorNumber;
        private GenderType genderType;
        private DormitoryStatus status;
        private Boolean hasAvailableBeds;

        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
        public Long getBuildingId() { return buildingId; }
        public void setBuildingId(Long buildingId) { this.buildingId = buildingId; }
        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
        public Integer getFloorNumber() { return floorNumber; }
        public void setFloorNumber(Integer floorNumber) { this.floorNumber = floorNumber; }
        public GenderType getGenderType() { return genderType; }
        public void setGenderType(GenderType genderType) { this.genderType = genderType; }
        public DormitoryStatus getStatus() { return status; }
        public void setStatus(DormitoryStatus status) { this.status = status; }
        public Boolean getHasAvailableBeds() { return hasAvailableBeds; }
        public void setHasAvailableBeds(Boolean hasAvailableBeds) { this.hasAvailableBeds = hasAvailableBeds; }
    }
}
